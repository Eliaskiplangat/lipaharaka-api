package com.lipaharaka.api.advance.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RequestAdvanceRequest(@NotNull UUID invoiceId) {
}
