package com.lipaharaka.api.payment.mpesa;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lipaharaka.mpesa")
public record MpesaProperties(String baseUrl, String consumerKey, String consumerSecret,
                               String shortcode, String passkey, String callbackBaseUrl) {
}
