package com.lipaharaka.api.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Standard API error payload")
public record ApiError(
        @Schema(description = "Error code", example = "VALIDATION_ERROR") String code,
        @Schema(description = "Human readable error message", example = "Invalid invoice request") String message,
        @Schema(description = "Detailed field-level validation issues") List<FieldViolation> violations
) {

    @Schema(description = "Field-level validation issue")
    public record FieldViolation(
            @Schema(description = "Field name with the validation problem", example = "buyerEmail") String field,
            @Schema(description = "Validation problem description", example = "must be a valid email") String message
    ) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
