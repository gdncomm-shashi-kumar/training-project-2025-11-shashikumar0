package com.blibli.gdn.gateway.exception;

import lombok.Getter;

@Getter
public class ServiceUnavailableException extends RuntimeException {
    
    private final String serviceName;
    
    public ServiceUnavailableException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
    }
    
    public ServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }
}
