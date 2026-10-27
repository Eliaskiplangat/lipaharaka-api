package com.lipaharaka.api.risk;

import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.invoice.Invoice;
import com.lipaharaka.api.invoice.InvoiceRepository;
import com.lipaharaka.api.invoice.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskScoringServiceTest {

    @Mock
    private RiskScoreRepository riskScoreRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        riskScoringService = new RiskScoringService(riskScoreRepository, invoiceRepository);
    }

    @Test
    void computeAndPersist_includesAgingAndLatePaymentMetrics() {
        Business business = new Business();
        business.setId(UUID.randomUUID());
        business.setCreatedAt(LocalDate.now().minusMonths(16).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));

        Invoice onTimeInvoice = new Invoice();
        onTimeInvoice.setStatus(InvoiceStatus.PAID);
        onTimeInvoice.setTotalAmount(new BigDecimal("1000.00"));
        onTimeInvoice.setAmountPaid(new BigDecimal("1000.00"));
        onTimeInvoice.setIssueDate(LocalDate.now().minusDays(30));
        onTimeInvoice.setDueDate(LocalDate.now().minusDays(10));
        onTimeInvoice.setPaidAt(Instant.now().minus(8, ChronoUnit.DAYS));

        Invoice lateInvoice = new Invoice();
        lateInvoice.setStatus(InvoiceStatus.PAID);
        lateInvoice.setTotalAmount(new BigDecimal("800.00"));
        lateInvoice.setAmountPaid(new BigDecimal("800.00"));
        lateInvoice.setIssueDate(LocalDate.now().minusDays(35));
        lateInvoice.setDueDate(LocalDate.now().minusDays(20));
        lateInvoice.setPaidAt(Instant.now().minus(5, ChronoUnit.DAYS));

        Invoice overdueInvoice = new Invoice();
        overdueInvoice.setStatus(InvoiceStatus.OVERDUE);
        overdueInvoice.setTotalAmount(new BigDecimal("1200.00"));
        overdueInvoice.setAmountPaid(BigDecimal.ZERO);
        overdueInvoice.setIssueDate(LocalDate.now().minusDays(45));
        overdueInvoice.setDueDate(LocalDate.now().minusDays(12));

        when(invoiceRepository.findByBusinessId(eq(business.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(onTimeInvoice, lateInvoice, overdueInvoice)));
        when(riskScoreRepository.save(any(RiskScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RiskScore riskScore = riskScoringService.computeAndPersist(business);

        assertThat(riskScore.getScore()).isBetween(0, 100);
        Map<String, Object> factors = riskScore.getFactors();
        assertThat(factors).containsKeys(
                "overdueFrequency",
                "averageInvoiceAgingDays",
                "averagePaymentDelayDays",
                "repeatedLateSettlementRate",
                "defaultTrendRate",
                "onTimePaymentRatio"
        );
        assertThat((double) factors.get("overdueFrequency")).isGreaterThan(0.0);
        assertThat((double) factors.get("averagePaymentDelayDays")).isGreaterThan(0.0);
        assertThat((double) factors.get("repeatedLateSettlementRate")).isGreaterThan(0.0);
        assertThat((double) factors.get("defaultTrendRate")).isGreaterThanOrEqualTo(0.0);
    }
}
