package com.lipaharaka.api.advance.lender;

import com.lipaharaka.api.common.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;


@Component
@Profile("!dev")
@EnableConfigurationProperties(LenderApiProperties.class)
public class HttpLenderApiClient implements LenderApiClient {

    private static final Logger log = LoggerFactory.getLogger(HttpLenderApiClient.class);

    private final RestTemplate restTemplate;
    private final LenderApiProperties properties;

    public HttpLenderApiClient(org.springframework.web.client.RestTemplate lenderRestTemplate, LenderApiProperties properties) {
        this.restTemplate = lenderRestTemplate;
        this.properties = properties;
    }

    @Override
    @CircuitBreaker(name = "lender", fallbackMethod = "fallback")
    @Retry(name = "lender")
    public LenderDecision submitAdvanceRequest(UUID businessId, UUID invoiceId, BigDecimal requestedAmount, int riskScore) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + properties.apiKey());

        Map<String, Object> body = Map.of(
                "businessId", businessId.toString(),
                "invoiceId", invoiceId.toString(),
                "requestedAmount", requestedAmount,
                "riskScore", riskScore
        );

        var response = restTemplate.postForEntity(properties.baseUrl() + "/v1/advance-requests",
                new HttpEntity<>(body, headers), Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = response.getBody();
        return new LenderDecision(
                Boolean.TRUE.equals(resp.get("approved")),
                String.valueOf(resp.get("referenceId")),
                String.valueOf(resp.getOrDefault("reason", "")));
    }

    @SuppressWarnings("unused")
    private LenderDecision fallback(UUID businessId, UUID invoiceId, BigDecimal requestedAmount, int riskScore, Throwable t) {
        log.error("Lender API unavailable for business {}", businessId, t);
        throw new ExternalServiceException("PartnerLender", "Unable to reach the partner lender right now.", t);
    }
}