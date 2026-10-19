package com.lipaharaka.api.advance;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "lipaharaka.advance")
public record AdvanceProperties(BigDecimal feePercentage, BigDecimal maxAdvancePercentage) {
}
