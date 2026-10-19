package com.lipaharaka.api.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InitiatePaymentRequest(
        @NotBlank @Pattern(regexp = "^(?:\\+?254|0)[17]\\d{8}$", message = "Phone number must be a valid Kenyan MSISDN")
        String payerPhoneNumber
) {
}
