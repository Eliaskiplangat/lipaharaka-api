package com.lipaharaka.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "^\\+254[17]\\d{8}$", message = "Phone number must be a valid Kenyan MSISDN, e.g. +254712345678")
        String phoneNumber,

        @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit code")
        String otpCode,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {
}
