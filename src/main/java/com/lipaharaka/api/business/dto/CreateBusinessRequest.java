package com.lipaharaka.api.business.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBusinessRequest(
        @NotBlank String name,
        @NotBlank String kraPin,
        String sector,
        String mpesaShortcode
) {
}
