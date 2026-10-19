package com.lipaharaka.api.advance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LenderPartnerRepository extends JpaRepository<LenderPartner, UUID> {
    List<LenderPartner> findByActiveTrue();
}
