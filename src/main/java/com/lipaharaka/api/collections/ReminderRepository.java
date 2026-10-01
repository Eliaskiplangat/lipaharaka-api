package com.lipaharaka.api.collections;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    List<Reminder> findByInvoiceId(UUID invoiceId);
    boolean existsByInvoiceIdAndChannelAndScheduledFor(UUID invoiceId, ReminderChannel channel, java.time.Instant scheduledFor);
}
