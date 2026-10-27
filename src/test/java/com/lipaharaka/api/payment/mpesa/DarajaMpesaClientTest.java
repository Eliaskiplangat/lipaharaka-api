package com.lipaharaka.api.payment.mpesa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DarajaMpesaClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void initiateStkPush_reusesCachedAccessTokenDuringValidityWindow() {
        MpesaProperties properties = new MpesaProperties(
                "https://sandbox.safaricom.co.ke",
                "consumer-key",
                "consumer-secret",
                "174379",
                "passkey",
                "https://api.lipaharaka.co.ke"
        );
        DarajaMpesaClient client = new DarajaMpesaClient(restTemplate, properties);

        when(restTemplate.exchange(
                eq("https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(Map.of("access_token", "cached-token", "expires_in", 3600), HttpStatus.OK));

        when(restTemplate.postForEntity(
                eq("https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(Map.of(
                "MerchantRequestID", "MR-001",
                "CheckoutRequestID", "CK-001",
                "ResponseDescription", "Success"
        ), HttpStatus.OK));

        client.initiateStkPush("254712345678", new BigDecimal("1000"), "INV-1001", "Test invoice");
        client.initiateStkPush("254712345678", new BigDecimal("1000"), "INV-1002", "Test invoice");

        verify(restTemplate, times(1)).exchange(
                eq("https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }
}
