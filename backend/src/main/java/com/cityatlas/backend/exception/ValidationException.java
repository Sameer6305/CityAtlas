package com.cityatlas.backend.exception;

/**
 * Business Validation Exception
 * 
 * Thrown when business logic validation fails (beyond basic field validation).
 * Maps to HTTP 400 Bad Request.
 * 
 * Examples:
 * - Invalid date range
 * - Duplicate resource
 * - Business rule violation
 * 
 * Usage:
 * throw new ValidationException("City slug must contain only lowercase letters and hyphens");
 */
public class ValidationException extends RuntimeException {
    
    /**
     * Constructor with validation message
     */
    public ValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructor with message and cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
