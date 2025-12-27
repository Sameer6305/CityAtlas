package com.cityatlas.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Generic API Exception
 * 
 * Base exception for custom API errors with HTTP status.
 * Use when you need to return a specific HTTP status code.
 * 
 * Usage:
 * throw new ApiException("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
 */
public class ApiException extends RuntimeException {
    
    private final HttpStatus status;
    
    /**
     * Constructor with message and HTTP status
     */
    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    /**
     * Constructor with message, cause, and HTTP status
     */
    public ApiException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
    
    /**
     * Default to 500 Internal Server Error
     */
    public ApiException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
}
