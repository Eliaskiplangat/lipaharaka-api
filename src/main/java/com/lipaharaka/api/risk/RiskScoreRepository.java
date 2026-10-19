package com.lipaharaka.api.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskScoreRepository extends JpaRepository<RiskScore, UUID> {
    Optional<RiskScore> findTopByBusinessIdOrderByComputedAtDesc(UUID businessId);
    List<RiskScore> findAllByOrderByComputedAtDesc();
}
