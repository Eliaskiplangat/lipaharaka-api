package com.lipaharaka.api.advance.dto;

import com.lipaharaka.api.advance.Advance;
import com.lipaharaka.api.advance.AdvanceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdvanceResponse(UUID id, UUID invoiceId, String invoiceNumber, BigDecimal requestedAmount,
                               BigDecimal feeAmount, BigDecimal netPayout, AdvanceStatus status, Instant requestedAt) {
    public static AdvanceResponse from(Advance a) {
        return new AdvanceResponse(a.getId(), a.getInvoice().getId(), a.getInvoice().getInvoiceNumber(),
                a.getRequestedAmount(), a.getFeeAmount(), a.getNetPayout(), a.getStatus(), a.getRequestedAt());
    }
}
