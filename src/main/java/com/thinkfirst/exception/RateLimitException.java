package com.thinkfirst.exception;

public class RateLimitException extends RuntimeException {
    private final String provider;
    
    public RateLimitException(String provider, String message) {
        super(message);
        this.provider = provider;
    }
    
    public RateLimitException(String provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }
    
    public String getProvider() {
        return provider;
    }
}

