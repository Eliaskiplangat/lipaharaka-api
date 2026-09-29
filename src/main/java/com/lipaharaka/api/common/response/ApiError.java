package com.lipaharaka.api.common.response;

import java.util.List;

public record ApiError(String code, String message, List<FieldViolation> violations) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
