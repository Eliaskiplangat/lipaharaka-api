package com.lipaharaka.api.payment;

import com.lipaharaka.api.advance.AdvanceService;
import com.lipaharaka.api.invoice.Invoice;
import com.lipaharaka.api.invoice.InvoiceService;
import com.lipaharaka.api.payment.mpesa.MpesaClient;
import com.lipaharaka.api.payment.mpesa.StkPushResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private MpesaClient mpesaClient;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AdvanceService advanceService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(mpesaClient, invoiceService, transactionRepository, advanceService);
    }

    @Test
    void initiateInvoicePayment_normalizesPhoneNumberForDaraja() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-2026-0001");
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setAmountPaid(BigDecimal.ZERO);

        when(invoiceService.getByShareableTokenOrThrow("token-123")).thenReturn(invoice);
        when(mpesaClient.initiateStkPush(eq("254712345678"), eq(new BigDecimal("1000.00")), eq("INV-2026-0001"), eq("Payment for invoice INV-2026-0001")))
                .thenReturn(new StkPushResult("merchant-1", "checkout-1", "Success"));

        paymentService.initiateInvoicePayment("token-123", "+254712345678");

        verify(mpesaClient).initiateStkPush(eq("254712345678"), eq(new BigDecimal("1000.00")), eq("INV-2026-0001"), eq("Payment for invoice INV-2026-0001"));
    }

    @Test
    void initiateInvoicePayment_rejectsInvalidKenyanPhoneNumber() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-2026-0002");
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setAmountPaid(BigDecimal.ZERO);

        when(invoiceService.getByShareableTokenOrThrow("token-invalid")).thenReturn(invoice);

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.initiateInvoicePayment("token-invalid", "12345"));
    }
}
