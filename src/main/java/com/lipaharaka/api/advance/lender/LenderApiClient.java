package com.lipaharaka.api.advance.lender;

import java.math.BigDecimal;
import java.util.UUID;

public interface LenderApiClient {
    LenderDecision submitAdvanceRequest(UUID businessId, UUID invoiceId, BigDecimal requestedAmount, int riskScore);
}
