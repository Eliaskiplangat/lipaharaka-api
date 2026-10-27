package com.lipaharaka.api.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Component
@Profile("!dev")
public class NoopSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(NoopSmsGateway.class);
    private static final RestTemplate restTemplate = new RestTemplate();

    private final boolean enabled;
    private final String provider;
    private final String twilioAccountSid;
    private final String twilioAuthToken;
    private final String twilioFrom;
    private final String africaUsername;
    private final String africaApiKey;
    private final String africaFrom;

    public NoopSmsGateway(
            @Value("${lipaharaka.notification.sms.enabled:false}") boolean enabled,
            @Value("${lipaharaka.notification.sms.provider:twilio}") String provider,
            @Value("${lipaharaka.notification.sms.twilio.account-sid:}") String twilioAccountSid,
            @Value("${lipaharaka.notification.sms.twilio.auth-token:}") String twilioAuthToken,
            @Value("${lipaharaka.notification.sms.twilio.from:}") String twilioFrom,
            @Value("${lipaharaka.notification.sms.africastalking.username:}") String africaUsername,
            @Value("${lipaharaka.notification.sms.africastalking.api-key:}") String africaApiKey,
            @Value("${lipaharaka.notification.sms.africastalking.from:}") String africaFrom) {
        this.enabled = enabled;
        this.provider = provider;
        this.twilioAccountSid = twilioAccountSid;
        this.twilioAuthToken = twilioAuthToken;
        this.twilioFrom = twilioFrom;
        this.africaUsername = africaUsername;
        this.africaApiKey = africaApiKey;
        this.africaFrom = africaFrom;
    }

    @Override
    public void send(String phoneNumber, String message) {
        if (!enabled) {
            log.warn("SMS delivery is disabled. OTP for {} was not sent.", phoneNumber);
            return;
        }

        String normalized = normalizePhone(phoneNumber);
        if (normalized == null) {
            throw new IllegalArgumentException("Phone number is required for SMS delivery.");
        }

        try {
            if ("twilio".equalsIgnoreCase(provider)) {
                sendViaTwilio(normalized, message);
                return;
            }
            if ("africastalking".equalsIgnoreCase(provider)) {
                sendViaAfricaTalking(normalized, message);
                return;
            }
            throw new IllegalStateException("Unsupported SMS provider: " + provider);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send SMS to " + normalized, ex);
        }
    }

    private void sendViaTwilio(String phoneNumber, String message) {
        if (twilioAccountSid == null || twilioAccountSid.isBlank() || twilioAuthToken == null || twilioAuthToken.isBlank() || twilioFrom == null || twilioFrom.isBlank()) {
            throw new IllegalStateException("Twilio SMS credentials are not configured.");
        }

        String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String credentials = Base64.getEncoder().encodeToString((twilioAccountSid + ":" + twilioAuthToken).getBytes());
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", phoneNumber);
        body.add("From", twilioFrom);
        body.add("Body", message);

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        log.info("Sent SMS via Twilio to {}", phoneNumber);
    }

    private void sendViaAfricaTalking(String phoneNumber, String message) {
        if (africaUsername == null || africaUsername.isBlank() || africaApiKey == null || africaApiKey.isBlank()) {
            throw new IllegalStateException("Africa's Talking credentials are not configured.");
        }

        String url = "https://api.sandbox.africastalking.com/version1/messaging";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("apiKey", africaApiKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", africaUsername);
        body.add("to", phoneNumber);
        body.add("message", message);
        if (africaFrom != null && !africaFrom.isBlank()) {
            body.add("from", africaFrom);
        }

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        log.info("Sent SMS via Africa's Talking to {}", phoneNumber);
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        String normalized = phoneNumber.trim().replace(" ", "").replace("-", "");
        if (normalized.startsWith("+254")) {
            return normalized;
        }
        if (normalized.startsWith("254")) {
            return "+" + normalized;
        }
        if (normalized.startsWith("0") && normalized.length() == 10) {
            return "+254" + normalized.substring(1);
        }
        return normalized;
    }
}
