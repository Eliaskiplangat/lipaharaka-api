package com.lipaharaka.api.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 150)
        String fullName,

        @Size(min = 7, max = 20)
        String phoneNumber
) {}
