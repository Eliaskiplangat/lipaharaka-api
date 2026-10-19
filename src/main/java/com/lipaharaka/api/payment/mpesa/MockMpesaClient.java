package com.lipaharaka.api.payment.mpesa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;


@Component
@Profile("dev")
public class MockMpesaClient implements MpesaClient {

    private static final Logger log = LoggerFactory.getLogger(MockMpesaClient.class);

    @Override
    public StkPushResult initiateStkPush(String phoneNumber, BigDecimal amount, String accountReference, String description) {
        String checkoutRequestId = "MOCK-CKO-" + UUID.randomUUID();
        log.info("[MOCK M-PESA] STK push simulated: phone={} amount={} ref={} checkoutRequestId={}",
                phoneNumber, amount, accountReference, checkoutRequestId);
        return new StkPushResult("MOCK-MERCHANT-" + UUID.randomUUID(), checkoutRequestId,
                "Success. Request accepted for processing (mock, dev profile)");
    }

    @Override
    public B2CResult disburseToBusiness(String phoneNumber, BigDecimal amount, String remarks) {
        String conversationId = "MOCK-CONV-" + UUID.randomUUID();
        log.info("[MOCK M-PESA] B2C disbursement simulated: phone={} amount={} remarks={} conversationId={}",
                phoneNumber, amount, remarks, conversationId);
        return new B2CResult(conversationId, "MOCK-ORIG-" + UUID.randomUUID(),
                "Accept the service request successfully (mock, dev profile)");
    }
}
