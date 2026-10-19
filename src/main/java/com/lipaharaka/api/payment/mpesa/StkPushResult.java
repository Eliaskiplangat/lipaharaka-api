package com.lipaharaka.api.payment.mpesa;

public record StkPushResult(String merchantRequestId, String checkoutRequestId, String responseDescription) {
}
