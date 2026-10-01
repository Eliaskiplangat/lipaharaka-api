package com.lipaharaka.api.business.dto;

import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.business.KycStatus;

import java.time.Instant;
import java.util.UUID;

public record BusinessResponse(
        UUID id, String name, String kraPin, String sector, String mpesaShortcode,
        KycStatus kycStatus, String kycReviewReason, Instant createdAt
) {
    public static BusinessResponse from(Business b) {
        return new BusinessResponse(b.getId(), b.getName(), b.getKraPin(), b.getSector(),
                b.getMpesaShortcode(), b.getKycStatus(), b.getKycReviewReason(), b.getCreatedAt());
    }
}
