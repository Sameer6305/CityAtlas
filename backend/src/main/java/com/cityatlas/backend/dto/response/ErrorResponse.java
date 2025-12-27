package com.cityatlas.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized Error Response DTO
 * 
 * Provides consistent error structure across all API endpoints.
 * Used by GlobalExceptionHandler to return uniform error messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Timestamp when error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code (400, 404, 500, etc.)
     */
    private Integer status;
    
    /**
     * HTTP status text (Bad Request, Not Found, etc.)
     */
    private String error;
    
    /**
     * User-friendly error message
     */
    private String message;
    
    /**
     * API endpoint path where error occurred
     */
    private String path;
    
    /**
     * Validation errors (for 400 Bad Request with validation failures)
     */
    private List<ValidationError> validationErrors;
    
    /**
     * Nested class for validation error details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        /**
         * Field name that failed validation
         */
        private String field;
        
        /**
         * Validation error message
         */
        private String message;
        
        /**
         * Rejected value (optional)
         */
        private Object rejectedValue;
    }
}
