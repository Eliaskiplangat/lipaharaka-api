package com.lipaharaka.api.admin;

import com.lipaharaka.api.advance.AdvanceRepository;
import com.lipaharaka.api.audit.AuditLog;
import com.lipaharaka.api.audit.AuditLogRepository;
import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.business.BusinessService;
import com.lipaharaka.api.invoice.InvoiceRepository;
import com.lipaharaka.api.invoice.InvoiceStatus;
import com.lipaharaka.api.risk.RiskBand;
import com.lipaharaka.api.risk.RiskScore;
import com.lipaharaka.api.risk.RiskScoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private final InvoiceRepository invoiceRepository;
    private final AdvanceRepository advanceRepository;
    private final BusinessService businessService;
    private final AuditLogRepository auditLogRepository;
    private final RiskScoreRepository riskScoreRepository;

    public AdminService(InvoiceRepository invoiceRepository, AdvanceRepository advanceRepository,
                         BusinessService businessService, AuditLogRepository auditLogRepository,
                         RiskScoreRepository riskScoreRepository) {
        this.invoiceRepository = invoiceRepository;
        this.advanceRepository = advanceRepository;
        this.businessService = businessService;
        this.auditLogRepository = auditLogRepository;
        this.riskScoreRepository = riskScoreRepository;
    }

    public PlatformKpiResponse getPlatformKpis() {
        long totalInvoices = invoiceRepository.count();
        long overdueCount = invoiceRepository.countByStatus(InvoiceStatus.OVERDUE);
        double overdueRate = totalInvoices == 0 ? 0.0 : (overdueCount * 100.0) / totalInvoices;

        var latestRiskScores = getLatestRiskScoresByBusiness();
        double avgRiskScore = latestRiskScores.isEmpty() ? 0.0 : latestRiskScores.values().stream().mapToDouble(Integer::doubleValue).average().orElse(0.0);
        long lowRisk = latestRiskScores.values().stream().filter(score -> RiskBand.fromScore(score) == RiskBand.LOW).count();
        long moderateRisk = latestRiskScores.values().stream().filter(score -> RiskBand.fromScore(score) == RiskBand.MODERATE).count();
        long highRisk = latestRiskScores.values().stream().filter(score -> RiskBand.fromScore(score) == RiskBand.HIGH).count();
        long criticalRisk = latestRiskScores.values().stream().filter(score -> RiskBand.fromScore(score) == RiskBand.CRITICAL).count();

        return new PlatformKpiResponse(
                invoiceRepository.sumTotalInvoiced(),
                invoiceRepository.sumTotalCollected(),
                advanceRepository.sumTotalAdvanced(),
                totalInvoices,
                overdueCount,
                Math.round(overdueRate * 100.0) / 100.0,
                Math.round(avgRiskScore * 100.0) / 100.0,
                lowRisk,
                moderateRisk,
                highRisk,
                criticalRisk
        );
    }

    public Page<Business> listPendingKyc(Pageable pageable) {
        return businessService.listPendingKyc(pageable);
    }

    public Page<Business> listBusinesses(Pageable pageable) {
        return businessService.listAllForAdmin(pageable);
    }

    public Business reviewKyc(UUID businessId, boolean approve, String reason, UUID reviewerId) {
        return businessService.reviewKyc(businessId, approve, reason, reviewerId);
    }

    public void deactivateBusiness(UUID businessId) {
        businessService.deactivateBusiness(businessId);
    }

    public Page<AuditLog> getAuditTrail(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    private Map<UUID, Integer> getLatestRiskScoresByBusiness() {
        Map<UUID, Integer> latestByBusiness = new LinkedHashMap<>();
        for (RiskScore riskScore : riskScoreRepository.findAllByOrderByComputedAtDesc()) {
            latestByBusiness.putIfAbsent(riskScore.getBusiness().getId(), riskScore.getScore());
        }
        return latestByBusiness;
    }
}
