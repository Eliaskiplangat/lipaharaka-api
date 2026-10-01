package com.lipaharaka.api.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {
    Optional<Business> findByOwnerId(UUID ownerId);
    Page<Business> findByKycStatus(KycStatus status, Pageable pageable);
    boolean existsByKraPin(String kraPin);
}
