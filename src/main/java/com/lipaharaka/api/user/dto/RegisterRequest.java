package com.lipaharaka.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^\\+254[17]\\d{8}$")
        String phoneNumber,

        @NotBlank @Size(min = 2, max = 150)
        String fullName,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank @Size(min = 6, max = 6)
        String otpCode
) {
}
