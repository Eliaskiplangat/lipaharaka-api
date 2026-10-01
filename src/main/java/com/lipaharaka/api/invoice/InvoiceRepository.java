package com.lipaharaka.api.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByBusinessId(UUID businessId, Pageable pageable);

    Optional<Invoice> findByShareableToken(String token);

    boolean existsByBusinessIdAndInvoiceNumber(UUID businessId, String invoiceNumber);

    @Query("select i from Invoice i where i.status = 'SENT' and i.dueDate < :today")
    List<Invoice> findOverdueCandidates(@Param("today") LocalDate today);

    @Query("select i from Invoice i where i.status in ('SENT','OVERDUE') and i.dueDate between :from and :to")
    List<Invoice> findDueForReminderWindow(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Pessimistic lock for the M-Pesa reconciliation path: two callbacks for the same invoice
    // must not both apply their amount concurrently and double-count payment.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") UUID id);

    // FR-6.1: platform-wide KPIs for the admin dashboard.
    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i")
    java.math.BigDecimal sumTotalInvoiced();

    @Query("select coalesce(sum(i.amountPaid), 0) from Invoice i")
    java.math.BigDecimal sumTotalCollected();

    long countByStatus(InvoiceStatus status);

    long count();
}
