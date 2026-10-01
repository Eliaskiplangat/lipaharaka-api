package com.lipaharaka.api.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    Optional<OtpCode> findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(String phoneNumber, OtpPurpose purpose);
}
