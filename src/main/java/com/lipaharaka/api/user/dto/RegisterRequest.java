package com.lipaharaka.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(description = "Kenyan phone number in +254 format", example = "+254712345678")
        @NotBlank @Pattern(regexp = "^\\+254[17]\\d{8}$")
        String phoneNumber,

        @Schema(description = "Full name of the business owner", example = "Jane Njeri")
        @NotBlank @Size(min = 2, max = 150)
        String fullName,

        @Schema(description = "Secure password for the account", example = "StrongPass123")
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Schema(description = "One-time password received by SMS", example = "123456")
        @NotBlank @Size(min = 6, max = 6)
        String otpCode
) {
}
