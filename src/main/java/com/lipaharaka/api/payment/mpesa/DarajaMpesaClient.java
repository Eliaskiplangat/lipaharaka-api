package com.lipaharaka.api.payment.mpesa;

import com.lipaharaka.api.common.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;


@Component
@Profile("!dev")
@EnableConfigurationProperties(MpesaProperties.class)
public class DarajaMpesaClient implements MpesaClient {

    private static final Logger log = LoggerFactory.getLogger(DarajaMpesaClient.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RestTemplate restTemplate;
    private final MpesaProperties properties;
    private volatile String cachedAccessToken;
    private volatile Instant accessTokenExpiry = Instant.EPOCH;

    public DarajaMpesaClient(org.springframework.web.client.RestTemplate mpesaRestTemplate, MpesaProperties properties) {
        this.restTemplate = mpesaRestTemplate;
        this.properties = properties;
    }

    @Override
    @CircuitBreaker(name = "mpesa", fallbackMethod = "stkPushFallback")
    @Retry(name = "mpesa")
    public StkPushResult initiateStkPush(String phoneNumber, BigDecimal amount, String accountReference, String description) {
        validateRequiredConfiguration();

        String accessToken = fetchAccessToken();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String password = Base64.getEncoder().encodeToString(
                (properties.shortcode() + properties.passkey() + timestamp).getBytes());

        String callbackUrl = properties.callbackBaseUrl().replaceAll("/+$", "") + "/api/v1/payments/mpesa/callback/stk";
        String maskedPhone = phoneNumber.length() > 4 ? "******" + phoneNumber.substring(phoneNumber.length() - 4) : phoneNumber;
        log.info("Initiating M-Pesa STK push for account {} to phone {} with callback {}", accountReference, maskedPhone, callbackUrl);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("BusinessShortCode", properties.shortcode());
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("TransactionType", "CustomerPayBillOnline");
        body.put("Amount", amount.intValue());
        body.put("PartyA", phoneNumber);
        body.put("PartyB", properties.shortcode());
        body.put("PhoneNumber", phoneNumber);
        body.put("CallBackURL", callbackUrl);
        body.put("AccountReference", accountReference);
        body.put("TransactionDesc", description);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = restTemplate.postForEntity(
                properties.baseUrl() + "/mpesa/stkpush/v1/processrequest",
                new HttpEntity<>(body, headers), Map.class);

        Map<?, ?> resp = response.getBody();
        if (resp == null) {
            throw new ExternalServiceException("M-Pesa", "Daraja STK response body was empty",
                    new IllegalStateException("Daraja STK response body was empty"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = (Map<String, Object>) resp;

        Object responseCode = respMap.get("ResponseCode");
        if (responseCode != null && !"0".equals(String.valueOf(responseCode))) {
            String errorDescription = String.valueOf(respMap.getOrDefault("ResponseDescription", "unknown"));
            throw new ExternalServiceException("M-Pesa",
                    "Daraja rejected the STK push request: " + errorDescription,
                    new IllegalStateException("Daraja STK push rejected: " + responseCode));
        }

        return new StkPushResult(
                String.valueOf(respMap.get("MerchantRequestID")),
                String.valueOf(respMap.get("CheckoutRequestID")),
                String.valueOf(respMap.get("ResponseDescription")));
    }

    @SuppressWarnings("unused")
    private StkPushResult stkPushFallback(String phoneNumber, BigDecimal amount, String accountReference,
                                          String description, Throwable t) {
        log.error("M-Pesa STK push unavailable for {}", accountReference, t);
        throw new ExternalServiceException("M-Pesa", "Unable to initiate payment prompt right now.", t);
    }

    @Override
    @CircuitBreaker(name = "mpesa", fallbackMethod = "b2cFallback")
    @Retry(name = "mpesa")
    public B2CResult disburseToBusiness(String phoneNumber, BigDecimal amount, String remarks) {
        String accessToken = fetchAccessToken();

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("InitiatorName", "lipaharaka_api");
        body.put("SecurityCredential", "PLACEHOLDER_ENCRYPTED_CREDENTIAL");
        body.put("CommandID", "BusinessPayment");
        body.put("Amount", amount.intValue());
        body.put("PartyA", properties.shortcode());
        body.put("PartyB", phoneNumber);
        body.put("Remarks", remarks);
        body.put("QueueTimeOutURL", properties.callbackBaseUrl() + "/api/v1/payments/mpesa/callback/b2c/timeout");
        body.put("ResultURL", properties.callbackBaseUrl() + "/api/v1/payments/mpesa/callback/b2c/result");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = restTemplate.postForEntity(
                properties.baseUrl() + "/mpesa/b2c/v1/paymentrequest",
                new HttpEntity<>(body, headers), Map.class);

        Map<?, ?> resp = response.getBody();
        return new B2CResult(
                String.valueOf(resp.get("ConversationID")),
                String.valueOf(resp.get("OriginatorConversationID")),
                String.valueOf(resp.get("ResponseDescription")));
    }

    @SuppressWarnings("unused")
    private B2CResult b2cFallback(String phoneNumber, BigDecimal amount, String remarks, Throwable t) {
        log.error("M-Pesa B2C disbursement unavailable for {}", phoneNumber, t);
        throw new ExternalServiceException("M-Pesa", "Unable to disburse funds right now.", t);
    }

    private void validateRequiredConfiguration() {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new IllegalStateException("MPESA_BASE_URL is required.");
        }
        if (properties.consumerKey() == null || properties.consumerKey().isBlank()) {
            throw new IllegalStateException("MPESA_CONSUMER_KEY is required.");
        }
        if (properties.consumerSecret() == null || properties.consumerSecret().isBlank()) {
            throw new IllegalStateException("MPESA_CONSUMER_SECRET is required.");
        }
        if (properties.shortcode() == null || properties.shortcode().isBlank()) {
            throw new IllegalStateException("MPESA_SHORTCODE is required.");
        }
        if (properties.passkey() == null || properties.passkey().isBlank()) {
            throw new IllegalStateException("MPESA_PASSKEY is required.");
        }
        if (properties.callbackBaseUrl() == null || properties.callbackBaseUrl().isBlank()) {
            throw new IllegalStateException("MPESA_CALLBACK_BASE_URL is required.");
        }
    }

    private String fetchAccessToken() {
        validateRequiredConfiguration();

        Instant now = Instant.now();
        if (cachedAccessToken != null && now.isBefore(accessTokenExpiry.minusSeconds(30L))) {
            return cachedAccessToken;
        }

        String credentials = Base64.getEncoder().encodeToString(
                (properties.consumerKey() + ":" + properties.consumerSecret()).getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);

        var response = restTemplate.exchange(
                properties.baseUrl() + "/oauth/v1/generate?grant_type=client_credentials",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        Map<?, ?> body = response.getBody();
        if (body == null || body.get("access_token") == null) {
            throw new ExternalServiceException("M-Pesa", "Daraja access token response missing access_token",
                    new IllegalStateException("Missing Daraja access token in response"));
        }

        String accessToken = String.valueOf(body.get("access_token"));
        Object expiresIn = body.get("expires_in");
        long ttlSeconds = expiresIn instanceof Number number ? Math.max(60L, number.longValue()) : 3600L;

        synchronized (this) {
            cachedAccessToken = accessToken;
            accessTokenExpiry = Instant.now().plusSeconds(Math.max(60L, ttlSeconds - 30L));
        }

        return accessToken;
    }
}