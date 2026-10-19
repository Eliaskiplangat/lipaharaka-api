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

        String normalized = rawPhoneNumber.trim().replace(" ", "");
        if (normalized.startsWith("+254")) {
            return "254" + normalized.substring(4);
        }
        if (normalized.startsWith("254")) {
            return normalized;
        }
        if (normalized.startsWith("0") && normalized.length() == 10) {
            return "254" + normalized.substring(1);
        }
        return normalized;
    }

    /**
     * Handles the Daraja STK callback. Idempotent on mpesa_receipt_number: Safaricom retries
     * callbacks on timeout, so the same receipt arriving twice must not double-credit the
     * invoice — the unique index on transactions.mpesa_receipt_number is the hard backstop,
     * this existence check is the fast path that avoids a doomed insert or a double-reconcile.
     */
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
            // No matching PENDING transaction: either a very late/duplicate retry after we
            // already reconciled it, or a callback for a request this instance never issued
            // (e.g. after a redeploy). Logged for manual reconciliation rather than silently
            // dropped, since real money moved on Safaricom's side either way.
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

    /**
     * FR-4.2/FR-5.4: applies the confirmed payment to the invoice and, if that invoice had a
     * disbursed advance against it, offsets the advance in the same transaction so the two can
     * never be left inconsistent (payment applied but advance never marked repaid, or vice versa).
     */
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
