package com.lipaharaka.api.advance.lender;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lipaharaka.lender")
public record LenderApiProperties(String baseUrl, String apiKey) {
}
