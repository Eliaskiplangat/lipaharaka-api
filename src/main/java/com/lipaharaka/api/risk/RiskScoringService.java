package com.lipaharaka.api.risk;

import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.invoice.Invoice;
import com.lipaharaka.api.invoice.InvoiceRepository;
import com.lipaharaka.api.invoice.InvoiceStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskScoringService {

    private final RiskScoreRepository riskScoreRepository;
    private final InvoiceRepository invoiceRepository;

    public RiskScoringService(RiskScoreRepository riskScoreRepository, InvoiceRepository invoiceRepository) {
        this.riskScoreRepository = riskScoreRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public RiskScore computeAndPersist(Business business) {
        List<Invoice> recentInvoices = invoiceRepository.findByBusinessId(business.getId(), PageRequest.of(0, 100)).getContent();
        LocalDate today = LocalDate.now();

        long totalCount = recentInvoices.size();
        long paidCount = recentInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
        long paidOnTimeCount = recentInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID && i.getPaidAt() != null)
                .filter(i -> !toLocalDate(i.getPaidAt()).isAfter(i.getDueDate()))
                .count();

        long overdueCount = recentInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.OVERDUE || (!i.isFullyPaid() && i.getDueDate().isBefore(today)))
                .count();

        long latePaidCount = recentInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID && i.getPaidAt() != null)
                .filter(i -> toLocalDate(i.getPaidAt()).isAfter(i.getDueDate()))
                .count();

        double overdueFrequency = totalCount == 0 ? 0.0 : (double) overdueCount / totalCount;
        double onTimePaymentRatio = paidCount == 0 ? 0.5 : (double) paidOnTimeCount / paidCount;
        double repeatedLateSettlementRate = paidCount == 0 ? 0.0 : (double) latePaidCount / paidCount;
        double defaultTrendRate = totalCount == 0 ? 0.0 : (double) overdueCount / totalCount;

        double averageInvoiceAgingDays = recentInvoices.stream()
                .mapToDouble(invoice -> daysBetween(invoice.getIssueDate(),
                        invoice.getStatus() == InvoiceStatus.PAID && invoice.getPaidAt() != null
                                ? toLocalDate(invoice.getPaidAt())
                                : today))
                .average()
                .orElse(0.0);

        double averagePaymentDelayDays = recentInvoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.PAID && invoice.getPaidAt() != null)
                .mapToDouble(invoice -> Math.max(0, daysBetween(invoice.getDueDate(), toLocalDate(invoice.getPaidAt()))))
                .average()
                .orElse(0.0);

        int tenureMonths = business.getCreatedAt() == null ? 0 :
                Math.min((int) java.time.temporal.ChronoUnit.MONTHS.between(
                        business.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate(), today), 24);

        double riskScoreValue = Math.min(100.0,
                overdueFrequency * 35.0
                        + Math.min(averageInvoiceAgingDays / 45.0, 1.0) * 15.0
                        + Math.min(averagePaymentDelayDays / 20.0, 1.0) * 15.0
                        + repeatedLateSettlementRate * 20.0
                        + defaultTrendRate * 20.0
                        + (1.0 - onTimePaymentRatio) * 20.0
                        + Math.max(0, (12 - tenureMonths) * 0.25));

        int score = (int) Math.round(Math.max(0.0, Math.min(100.0, riskScoreValue)));

        Map<String, Object> factors = new LinkedHashMap<>();
        factors.put("overdueFrequency", overdueFrequency);
        factors.put("averageInvoiceAgingDays", roundToTwoDecimals(averageInvoiceAgingDays));
        factors.put("averagePaymentDelayDays", roundToTwoDecimals(averagePaymentDelayDays));
        factors.put("repeatedLateSettlementRate", repeatedLateSettlementRate);
        factors.put("defaultTrendRate", defaultTrendRate);
        factors.put("onTimePaymentRatio", onTimePaymentRatio);
        factors.put("invoiceCountConsidered", totalCount);
        factors.put("businessTenureMonths", tenureMonths);
        factors.put("riskBand", RiskBand.fromScore(score).name());

        RiskScore riskScore = new RiskScore();
        riskScore.setBusiness(business);
        riskScore.setScore(score);
        riskScore.setFactors(factors);
        return riskScoreRepository.save(riskScore);
    }

    public RiskScore getLatestOrCompute(Business business) {
        return riskScoreRepository.findTopByBusinessIdOrderByComputedAtDesc(business.getId())
                .orElseGet(() -> computeAndPersist(business));
    }

    private double daysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return 0.0;
        }
        return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(from, to));
    }

    private LocalDate toLocalDate(java.time.Instant instant) {
        return instant == null ? LocalDate.now() : instant.atZone(java.time.ZoneOffset.UTC).toLocalDate();
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
