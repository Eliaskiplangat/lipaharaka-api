package com.lipaharaka.api.risk;

public enum RiskBand {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL;

    public static RiskBand fromScore(int score) {
        if (score >= 80) return CRITICAL;
        if (score >= 60) return HIGH;
        if (score >= 35) return MODERATE;
        return LOW;
    }
}
