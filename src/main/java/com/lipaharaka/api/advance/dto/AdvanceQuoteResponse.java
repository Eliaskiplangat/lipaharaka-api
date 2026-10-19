package com.lipaharaka.api.advance.dto;

import java.math.BigDecimal;

public record AdvanceQuoteResponse(BigDecimal requestedAmount, BigDecimal feeAmount, BigDecimal netPayout,
                                    BigDecimal feePercentage) {
}
