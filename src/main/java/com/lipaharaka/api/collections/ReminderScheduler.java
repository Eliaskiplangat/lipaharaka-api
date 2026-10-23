package com.lipaharaka.api.collections;

import com.lipaharaka.api.invoice.Invoice;
import com.lipaharaka.api.invoice.InvoiceRepository;
import com.lipaharaka.api.invoice.InvoiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;


@Component
@EnableConfigurationProperties(ReminderProperties.class)
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final InvoiceRepository invoiceRepository;
    private final ReminderService reminderService;
    private final ReminderProperties properties;

    public ReminderScheduler(InvoiceRepository invoiceRepository, ReminderService reminderService,
                              ReminderProperties properties) {
        this.invoiceRepository = invoiceRepository;
        this.reminderService = reminderService;
        this.properties = properties;
    }

    @Async("taskExecutor")
    @Scheduled(cron = "0 0 7 * * *") // 07:00 daily server time
    public void runDailyReminderSweep() {
        markOverdueInvoices();
        dispatchPreDueReminders();
        dispatchPostDueReminders();
    }

    @Transactional
    void markOverdueInvoices() {
        List<Invoice> candidates = invoiceRepository.findOverdueCandidates(LocalDate.now());
        for (Invoice invoice : candidates) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
        }
    }

    private void dispatchPreDueReminders() {
        for (int daysBefore : properties.intervalsDaysBeforeDue()) {
            LocalDate targetDate = LocalDate.now().plusDays(daysBefore);
            processWindow(targetDate);
        }
    }

    private void dispatchPostDueReminders() {
        for (int daysAfter : properties.intervalsDaysAfterDue()) {
            LocalDate targetDate = LocalDate.now().minusDays(daysAfter);
            processWindow(targetDate);
        }
    }

    private void processWindow(LocalDate targetDueDate) {
        List<Invoice> invoices = invoiceRepository.findDueForReminderWindow(targetDueDate, targetDueDate);
        Instant scheduledFor = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        for (Invoice invoice : invoices) {
            try {
                reminderService.sendReminderIfNotAlreadySent(invoice, scheduledFor);
            } catch (Exception ex) {
                log.error("Reminder dispatch failed for invoice {}", invoice.getId(), ex);
            }
        }
    }
}
