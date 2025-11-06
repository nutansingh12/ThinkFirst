package com.thinkfirst.exception;

public class AIProviderException extends RuntimeException {
    private final String provider;
    
    public AIProviderException(String provider, String message) {
        super(message);
        this.provider = provider;
    }
    
    public AIProviderException(String provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }
    
    public String getProvider() {
        return provider;
    }
}

