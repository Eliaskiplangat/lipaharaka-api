package com.lipaharaka.api.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lipaharaka.otp")
public record OtpProperties(int ttlMinutes, int length) {
}
