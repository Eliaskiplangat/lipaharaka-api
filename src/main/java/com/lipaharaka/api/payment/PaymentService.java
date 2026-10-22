package com.lipaharaka.api.payment;

import com.lipaharaka.api.advance.AdvanceService;
import com.lipaharaka.api.common.exception.NotFoundException;
import com.lipaharaka.api.invoice.Invoice;
import com.lipaharaka.api.invoice.InvoiceService;
import com.lipaharaka.api.payment.dto.InitiatePaymentResponse;
import com.lipaharaka.api.payment.mpesa.MpesaClient;
import com.lipaharaka.api.payment.mpesa.StkPushResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final MpesaClient mpesaClient;
    private final InvoiceService invoiceService;
    private final TransactionRepository transactionRepository;
    private final AdvanceService advanceService;

    public PaymentService(MpesaClient mpesaClient, InvoiceService invoiceService,
                           TransactionRepository transactionRepository, AdvanceService advanceService) {
        this.mpesaClient = mpesaClient;
        this.invoiceService = invoiceService;
        this.transactionRepository = transactionRepository;
        this.advanceService = advanceService;
    }

    @Transactional
    public InitiatePaymentResponse initiateInvoicePayment(String invoiceToken, String payerPhoneNumber) {
        Invoice invoice = invoiceService.getByShareableTokenOrThrow(invoiceToken);
        String normalizedPhoneNumber = normalizePhoneNumberForDaraja(payerPhoneNumber);

        StkPushResult result = mpesaClient.initiateStkPush(
                normalizedPhoneNumber, invoice.outstandingBalance(), invoice.getInvoiceNumber(),
                "Payment for invoice " + invoice.getInvoiceNumber());

        Transaction txn = new Transaction();
        txn.setInvoiceId(invoice.getId());
        txn.setType(TransactionType.INVOICE_PAYMENT);
        txn.setStatus(TransactionStatus.PENDING);
        txn.setAmount(invoice.outstandingBalance());
        txn.setCounterpartyPhone(normalizedPhoneNumber);
        txn.setMpesaCheckoutRequestId(result.checkoutRequestId());
        transactionRepository.save(txn);

        return new InitiatePaymentResponse(result.checkoutRequestId(), result.responseDescription());
    }

    private String normalizePhoneNumberForDaraja(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            throw new IllegalArgumentException("Phone number is required.");
        }

        String normalized = rawPhoneNumber.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Phone number is required.");
        }

        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }

        if (normalized.matches("^0\\d{9}$")) {
            return "254" + normalized.substring(1);
        }
        if (normalized.matches("^254\\d{9}$")) {
            return normalized;
        }
        if (normalized.matches("^7\\d{8}$")) {
            return "254" + normalized;
        }
        if (normalized.matches("^2547\\d{8}$")) {
            return normalized;
        }
        if (normalized.matches("^\\d{12}$")) {
            return normalized;
        }

        throw new IllegalArgumentException("Phone number must be a valid Kenyan mobile number.");
    }


    @Transactional
    public void handleStkCallback(Map<String, Object> callbackPayload) {
        StkCallbackData data = StkCallbackData.parse(callbackPayload);
        if (data == null) {
            log.warn("Received unparseable or failed STK callback: {}", callbackPayload);
            return;
        }
        if (data.receiptNumber() != null && transactionRepository.existsByMpesaReceiptNumber(data.receiptNumber())) {
            log.info("Duplicate M-Pesa callback for receipt {} ignored.", data.receiptNumber());
            return;
        }

        Optional<Transaction> pending = transactionRepository
                .findByMpesaCheckoutRequestIdAndStatus(data.checkoutRequestId(), TransactionStatus.PENDING);

        if (pending.isEmpty()) {

            log.warn("No pending transaction found for checkoutRequestId={}; payload={}",
                    data.checkoutRequestId(), callbackPayload);
            return;
        }

        Transaction txn = pending.get();
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setMpesaReceiptNumber(data.receiptNumber());
        txn.setRawCallbackPayload(callbackPayload);
        txn.setSettledAt(Instant.now());
        if (data.amount() != null) {
            txn.setAmount(data.amount()); // trust Safaricom's confirmed amount over our estimate
        }
        transactionRepository.save(txn);

        if (txn.getInvoiceId() != null) {
            reconcileInvoicePayment(txn.getInvoiceId(), txn.getAmount());
        }
        log.info("Reconciled M-Pesa payment receipt {} for KES {} against invoice {}",
                data.receiptNumber(), txn.getAmount(), txn.getInvoiceId());
    }


    @Transactional
    public void reconcileInvoicePayment(UUID invoiceId, BigDecimal amount) {
        invoiceService.applyPayment(invoiceId, amount);
        advanceService.offsetAgainstInvoicePayment(invoiceId);
    }

    private record StkCallbackData(String checkoutRequestId, String receiptNumber, BigDecimal amount, String phoneNumber) {
        @SuppressWarnings("unchecked")
        static StkCallbackData parse(Map<String, Object> payload) {
            try {
                Map<String, Object> body = (Map<String, Object>) payload.get("Body");
                Map<String, Object> stkCallback = (Map<String, Object>) body.get("stkCallback");
                String checkoutRequestId = String.valueOf(stkCallback.get("CheckoutRequestID"));
                int resultCode = (int) stkCallback.get("ResultCode");
                if (resultCode != 0) {
                    return null; // buyer cancelled or payment failed on Safaricom's side
                }
                var items = (java.util.List<Map<String, Object>>) ((Map<String, Object>) stkCallback.get("CallbackMetadata")).get("Item");
                String receipt = null, phone = null;
                BigDecimal amount = null;
                for (Map<String, Object> item : items) {
                    switch (String.valueOf(item.get("Name"))) {
                        case "MpesaReceiptNumber" -> receipt = String.valueOf(item.get("Value"));
                        case "PhoneNumber" -> phone = String.valueOf(item.get("Value"));
                        case "Amount" -> amount = new BigDecimal(String.valueOf(item.get("Value")));
                        default -> { /* ignore other fields (TransactionDate, etc.) */ }
                    }
                }
                return new StkCallbackData(checkoutRequestId, receipt, amount, phone);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
