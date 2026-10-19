package com.lipaharaka.api.common.response;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, ApiError error, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static ApiResponse<Void> error(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }
}
