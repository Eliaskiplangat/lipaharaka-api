package com.lipaharaka.api.advance;

import com.lipaharaka.api.advance.lender.LenderApiClient;
import com.lipaharaka.api.advance.lender.LenderDecision;
import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.business.KycStatus;
import com.lipaharaka.api.common.exception.ConflictException;
import com.lipaharaka.api.common.exception.ForbiddenException;
import com.lipaharaka.api.common.exception.ValidationException;
import com.lipaharaka.api.invoice.Invoice;
import com.lipaharaka.api.invoice.InvoiceService;
import com.lipaharaka.api.invoice.InvoiceStatus;
import com.lipaharaka.api.payment.TransactionRepository;
import com.lipaharaka.api.payment.mpesa.B2CResult;
import com.lipaharaka.api.payment.mpesa.MpesaClient;
import com.lipaharaka.api.risk.RiskScore;
import com.lipaharaka.api.risk.RiskScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AdvanceServiceTest {

    @Mock private AdvanceRepository advanceRepository;
    @Mock private LenderPartnerRepository lenderPartnerRepository;
    @Mock private InvoiceService invoiceService;
    @Mock private RiskScoringService riskScoringService;
    @Mock private LenderApiClient lenderApiClient;
    @Mock private MpesaClient mpesaClient;
    @Mock private TransactionRepository transactionRepository;

    private AdvanceService advanceService;

    @BeforeEach
    void setUp() {
        AdvanceProperties properties = new AdvanceProperties(new BigDecimal("0.03"), new BigDecimal("0.90"));
        advanceService = new AdvanceService(advanceRepository, lenderPartnerRepository, invoiceService,
                riskScoringService, lenderApiClient, mpesaClient, transactionRepository, properties);
    }

    @Test
    void quote_appliesMaxAdvancePercentageAndFee_toOutstandingBalance() {
        Invoice invoice = sentInvoiceWithOutstanding(new BigDecimal("10000.00"));
        when(invoiceService.getOwnedInvoiceOrThrow(invoice.getId())).thenReturn(invoice);

        var quote = advanceService.quote(invoice.getId());

        // 90% of 10000 = 9000 requested; 3% fee of 9000 = 270; net = 8730
        assertThat(quote.requestedAmount()).isEqualByComparingTo("9000.00");
        assertThat(quote.feeAmount()).isEqualByComparingTo("270.00");
        assertThat(quote.netPayout()).isEqualByComparingTo("8730.00");
    }

    @Test
    void quote_rejects_whenInvoiceNotSentOrOverdue() {
        Invoice invoice = sentInvoiceWithOutstanding(new BigDecimal("5000.00"));
        invoice.setStatus(InvoiceStatus.DRAFT);
        when(invoiceService.getOwnedInvoiceOrThrow(invoice.getId())).thenReturn(invoice);

        assertThatThrownBy(() -> advanceService.quote(invoice.getId())).isInstanceOf(ValidationException.class);
    }

    @Test
    void quote_rejects_whenBusinessKycNotApproved() {
        Invoice invoice = sentInvoiceWithOutstanding(new BigDecimal("5000.00"));
        invoice.getBusiness().setKycStatus(KycStatus.PENDING);
        when(invoiceService.getOwnedInvoiceOrThrow(invoice.getId())).thenReturn(invoice);

        assertThatThrownBy(() -> advanceService.quote(invoice.getId())).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requestAdvance_rejects_whenAdvanceAlreadyExistsForInvoice() {
        Invoice invoice = sentInvoiceWithOutstanding(new BigDecimal("5000.00"));
        when(invoiceService.getOwnedInvoiceOrThrow(invoice.getId())).thenReturn(invoice);
        when(advanceRepository.findByInvoiceId(invoice.getId())).thenReturn(Optional.of(new Advance()));

        assertThatThrownBy(() -> advanceService.requestAdvance(invoice.getId())).isInstanceOf(ConflictException.class);
        verify(lenderApiClient, never()).submitAdvanceRequest(any(), any(), any(), anyInt());
    }

    @Test
    void requestAdvance_disburses_whenLenderApproves() {
        Invoice invoice = sentInvoiceWithOutstanding(new BigDecimal("10000.00"));
        Business business = invoice.getBusiness();
        when(invoiceService.getOwnedInvoiceOrThrow(invoice.getId())).thenReturn(invoice);
        when(advanceRepository.findByInvoiceId(invoice.getId())).thenReturn(Optional.empty());
        when(advanceRepository.save(any(Advance.class))).thenAnswer(inv -> inv.getArgument(0));

        RiskScore riskScore = new RiskScore();
        riskScore.setScore(75);
        when(riskScoringService.getLatestOrCompute(business)).thenReturn(riskScore);

        when(lenderApiClient.submitAdvanceRequest(any(), any(), any(), anyInt()))
                .thenReturn(new LenderDecision(true, "EXT-123", null));
        when(mpesaClient.disburseToBusiness(any(), any(), any()))
                .thenReturn(new B2CResult("conv-1", "orig-1", "Accepted"));

        Advance advance = advanceService.requestAdvance(invoice.getId());

        assertThat(advance.getStatus()).isEqualTo(AdvanceStatus.DISBURSED);
        verify(mpesaClient).disburseToBusiness(any(), eq(advance.getNetPayout()), any());
    }

    private Invoice sentInvoiceWithOutstanding(BigDecimal outstanding) {
        Business business = new Business();
        business.setId(UUID.randomUUID());
        business.setKycStatus(KycStatus.APPROVED);
        business.setMpesaShortcode("174379");

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setBusiness(business);
        invoice.setInvoiceNumber("INV-202601-0001");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setTotalAmount(outstanding);
        invoice.setAmountPaid(BigDecimal.ZERO);
        return invoice;
    }
}
