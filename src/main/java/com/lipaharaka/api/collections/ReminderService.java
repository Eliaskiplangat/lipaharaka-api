package com.lipaharaka.api.collections;

import com.lipaharaka.api.invoice.Invoice;
import com.lipaharaka.api.notification.EmailGateway;
import com.lipaharaka.api.notification.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderRepository reminderRepository;
    private final SmsGateway smsGateway;
    private final EmailGateway emailGateway;

    public ReminderService(ReminderRepository reminderRepository, SmsGateway smsGateway, EmailGateway emailGateway) {
        this.reminderRepository = reminderRepository;
        this.smsGateway = smsGateway;
        this.emailGateway = emailGateway;
    }


    @Transactional
    public void sendReminderIfNotAlreadySent(Invoice invoice, Instant scheduledFor) {
        boolean alreadySent = reminderRepository.existsByInvoiceIdAndChannelAndScheduledFor(
                invoice.getId(), ReminderChannel.SMS, scheduledFor);
        if (alreadySent) {
            return;
        }

        String message = buildMessage(invoice);
        sendOnChannel(invoice, ReminderChannel.SMS, scheduledFor, message);
        if (invoice.getBuyer().getEmail() != null) {
            sendOnChannel(invoice, ReminderChannel.EMAIL, scheduledFor, message);
        }
    }

    private void sendOnChannel(Invoice invoice, ReminderChannel channel, Instant scheduledFor, String message) {
        Reminder reminder = new Reminder();
        reminder.setInvoice(invoice);
        reminder.setChannel(channel);
        reminder.setScheduledFor(scheduledFor);

        try {
            if (channel == ReminderChannel.SMS && invoice.getBuyer().getPhoneNumber() != null) {
                smsGateway.send(invoice.getBuyer().getPhoneNumber(), message);
            } else if (channel == ReminderChannel.EMAIL) {
                emailGateway.send(invoice.getBuyer().getEmail(), "Payment reminder", message);
            }
            reminder.setStatus(ReminderStatus.SENT);
            reminder.setSentAt(Instant.now());
        } catch (Exception ex) {

            log.warn("Failed to send {} reminder for invoice {}", channel, invoice.getId(), ex);
            reminder.setStatus(ReminderStatus.FAILED);
            reminder.setFailureReason(ex.getMessage());
        }
        reminderRepository.save(reminder);
    }

    private String buildMessage(Invoice invoice) {
        boolean overdue = invoice.getDueDate().isBefore(java.time.LocalDate.now());
        return (overdue ? "Reminder: invoice " : "Upcoming: invoice ") + invoice.getInvoiceNumber()
                + " for KES " + invoice.outstandingBalance() + " from " + invoice.getBusiness().getName()
                + (overdue ? " is now overdue." : " is due " + invoice.getDueDate() + ".");
    }
}
