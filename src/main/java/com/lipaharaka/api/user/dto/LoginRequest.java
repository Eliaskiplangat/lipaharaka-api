package com.lipaharaka.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Registered phone number", example = "+254712345678")
        @NotBlank String phoneNumber,

        @Schema(description = "Account password", example = "StrongPass123")
        @NotBlank String password
) {
}
