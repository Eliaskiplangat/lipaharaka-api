package com.lipaharaka.api.advance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface AdvanceRepository extends JpaRepository<Advance, UUID> {

    Optional<Advance> findByInvoiceId(UUID invoiceId);

    Page<Advance> findByBusinessId(UUID businessId, Pageable pageable);

    Page<Advance> findByStatus(AdvanceStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Advance a where a.invoice.id = :invoiceId and a.status = 'DISBURSED'")
    Optional<Advance> findDisbursedAdvanceForInvoiceForUpdate(@Param("invoiceId") UUID invoiceId);

    @Query("select coalesce(sum(a.netPayout), 0) from Advance a where a.status in ('DISBURSED', 'REPAID')")
    java.math.BigDecimal sumTotalAdvanced();
}
