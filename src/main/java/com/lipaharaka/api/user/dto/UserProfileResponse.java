package com.lipaharaka.api.user.dto;

import com.lipaharaka.api.user.Role;
import com.lipaharaka.api.user.User;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String phoneNumber,
        Role role,
        boolean active
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isActive()
        );
    }
}
