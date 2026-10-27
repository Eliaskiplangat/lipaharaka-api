package com.lipaharaka.api.notification;

public interface EmailGateway {
    void send(String toAddress, String subject, String body);
}
