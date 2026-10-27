package com.lipaharaka.api.notification;

public interface SmsGateway {
    void send(String phoneNumber, String message);
}
