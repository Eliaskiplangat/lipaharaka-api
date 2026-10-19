package com.lipaharaka.api.business.dto;

import jakarta.validation.constraints.NotNull;

public record KycReviewRequest(
        @NotNull Boolean approve,
        String reason // required by service layer when approve == false
) {
}
