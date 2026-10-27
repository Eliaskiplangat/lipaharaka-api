package com.lipaharaka.api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InitiatePaymentRequest(
        @Schema(description = "Payer phone number to receive the STK push", example = "+254712345678")
        @NotBlank @Pattern(regexp = "^(?:\\+?254|0)[17]\\d{8}$", message = "Phone number must be a valid Kenyan MSISDN")
        String payerPhoneNumber
) {
}
