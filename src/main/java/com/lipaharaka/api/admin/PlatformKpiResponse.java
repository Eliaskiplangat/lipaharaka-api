package com.lipaharaka.api.admin;

import java.math.BigDecimal;

public record PlatformKpiResponse(
        BigDecimal totalInvoiced,
        BigDecimal totalCollected,
        BigDecimal totalAdvanced,
        long totalInvoiceCount,
        long overdueInvoiceCount,
        double overdueRatePercent,
        double averageRiskScore,
        long lowRiskBusinessCount,
        long moderateRiskBusinessCount,
        long highRiskBusinessCount,
        long criticalRiskBusinessCount
) {
}
