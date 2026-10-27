package com.lipaharaka.api.invoice;

import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.business.BusinessService;
import com.lipaharaka.api.invoice.dto.CreateInvoiceRequest;
import com.lipaharaka.api.invoice.dto.InvoiceLineItemRequest;
import com.lipaharaka.api.common.exception.ForbiddenException;
import com.lipaharaka.api.notification.EmailGateway;
import com.lipaharaka.api.notification.SmsGateway;
import com.lipaharaka.api.security.AuthenticatedUser;
import com.lipaharaka.api.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private BuyerRepository buyerRepository;
    @Mock private BusinessService businessService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private SmsGateway smsGateway;
    @Mock private EmailGateway emailGateway;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(invoiceRepository, buyerRepository, businessService,
                currentUserProvider, smsGateway, emailGateway);
    }

    @Test
    void create_calculatesSubtotalTaxAndTotal_acrossMultipleLineItems() {
        Business business = approvedBusiness();
        when(businessService.getForCurrentUser()).thenReturn(business);
        when(invoiceRepository.existsByBusinessIdAndInvoiceNumber(any(), any())).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new CreateInvoiceRequest(
                "Acme Buyers Ltd", "+254712345678", "buyer@acme.co.ke", LocalDate.now().plusDays(14),
                List.of(
                        new InvoiceLineItemRequest("Consulting hours", new BigDecimal("10"), new BigDecimal("1000"), new BigDecimal("0.16")),
                        new InvoiceLineItemRequest("Delivery", new BigDecimal("1"), new BigDecimal("500"), BigDecimal.ZERO)
                ));

        Invoice invoice = invoiceService.create(request);

        // 10 * 1000 = 10000 subtotal, 16% tax = 1600; + 500 flat, no tax
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("10500.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("1600.00");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("12100.00");
        assertThat(invoice.getLineItems()).hasSize(2);
        assertThat(invoice.getShareableToken()).isNotBlank();
    }

    @Test
    void create_rejectsInvoiceIssuance_whenBusinessKycNotApproved() {
        Business business = new Business();
        business.setKycStatus(com.lipaharaka.api.business.KycStatus.PENDING);
        when(businessService.getForCurrentUser()).thenReturn(business);

        var request = new CreateInvoiceRequest("Buyer", null, null, LocalDate.now().plusDays(7),
                List.of(new InvoiceLineItemRequest("Item", BigDecimal.ONE, BigDecimal.TEN, null)));

        assertThatThrownBy(() -> invoiceService.create(request)).isInstanceOf(ForbiddenException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void applyPayment_marksInvoicePaid_onceOutstandingBalanceReachesZero() {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.setStatus(InvoiceStatus.SENT);

        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice updated = invoiceService.applyPayment(invoiceId, new BigDecimal("1000.00"));

        assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(updated.outstandingBalance()).isEqualByComparingTo("0.00");
    }

    private Business approvedBusiness() {
        Business business = new Business();
        business.setId(UUID.randomUUID());
        business.setName("Test SME");
        business.setKycStatus(com.lipaharaka.api.business.KycStatus.APPROVED);
        return business;
    }
}
