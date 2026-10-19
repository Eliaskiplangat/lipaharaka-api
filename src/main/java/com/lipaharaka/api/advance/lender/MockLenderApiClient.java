package com.lipaharaka.api.advance.lender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;


@Component
@Profile("dev")
public class MockLenderApiClient implements LenderApiClient {

    private static final Logger log = LoggerFactory.getLogger(MockLenderApiClient.class);

    @Override
    public LenderDecision submitAdvanceRequest(UUID businessId, UUID invoiceId, BigDecimal requestedAmount, int riskScore) {
        log.info("[MOCK LENDER] business={} invoice={} amount={} riskScore={} -> APPROVED (mock always approves)",
                businessId, invoiceId, requestedAmount, riskScore);
        return new LenderDecision(true, "MOCK-" + UUID.randomUUID(), null);
    }
}