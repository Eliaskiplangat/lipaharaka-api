package com.lipaharaka.api.payment.mpesa;

import java.math.BigDecimal;


public interface MpesaClient {


    StkPushResult initiateStkPush(String phoneNumber, BigDecimal amount, String accountReference, String description);

    B2CResult disburseToBusiness(String phoneNumber, BigDecimal amount, String remarks);
}
