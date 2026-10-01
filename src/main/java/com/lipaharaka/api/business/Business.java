package com.lipaharaka.api.business;

import com.lipaharaka.api.common.BaseEntity;
import com.lipaharaka.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
public class Business extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "kra_pin", nullable = false, length = 20)
    private String kraPin;

    @Column(length = 100)
    private String sector;

    @Column(name = "mpesa_shortcode", length = 20)
    private String mpesaShortcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "kyc_reviewed_by")
    private java.util.UUID kycReviewedBy;

    @Column(name = "kyc_review_reason", columnDefinition = "TEXT")
    private String kycReviewReason;

    private Instant kycReviewedAt;

    public boolean isApproved() {
        return kycStatus == KycStatus.APPROVED;
    }
}
