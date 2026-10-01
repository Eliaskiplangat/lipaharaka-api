package com.lipaharaka.api.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String role) {
}
