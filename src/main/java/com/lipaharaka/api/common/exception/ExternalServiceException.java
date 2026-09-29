package com.lipaharaka.api.common.exception;

public class ExternalServiceException extends RuntimeException {
    private final String serviceName;

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
