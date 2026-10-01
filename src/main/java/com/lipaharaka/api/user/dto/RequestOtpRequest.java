package com.lipaharaka.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RequestOtpRequest(
        @NotBlank @Pattern(regexp = "^\\+254[17]\\d{8}$", message = "Phone number must be a valid Kenyan MSISDN, e.g. +254712345678")
        String phoneNumber
) {
}
