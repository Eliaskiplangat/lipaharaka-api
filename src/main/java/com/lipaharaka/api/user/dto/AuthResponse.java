package com.lipaharaka.api.user.dto;

import java.util.UUID;

public record AuthResponse(String accessToken, UUID userId, String fullName, String role) {
}
