package com.lipaharaka.api.notification;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class NoopEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(NoopEmailGateway.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;

    public NoopEmailGateway(JavaMailSender mailSender,
                            @Value("${lipaharaka.notification.email.enabled:false}") boolean enabled,
                            @Value("${lipaharaka.notification.email.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String toAddress, String subject, String body) {
        if (!enabled || fromAddress == null || fromAddress.isBlank()) {
            log.warn("Email delivery is disabled or SMTP is not configured. Email for {} was not sent.", toAddress);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress));
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Sent invoice email to {} with subject {}", toAddress, subject);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send email to " + toAddress, ex);
        }
    }
}
