package com.cityatlas.backend.exception;

import com.cityatlas.backend.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Global Exception Handler
 * 
 * Centralized exception handling for all REST controllers.
 * Catches exceptions and returns standardized ErrorResponse.
 * 
 * Handles:
 * - ResourceNotFoundException (404)
 * - ValidationException (400)
 * - MethodArgumentNotValidException (400) - Bean validation errors
 * - MethodArgumentTypeMismatchException (400) - Path variable type errors
 * - ApiException (custom status)
 * - Generic Exception (500)
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle ResourceNotFoundException
     * Returns 404 Not Found
     * 
     * Triggered when: City not found, Analytics data not available, etc.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle ValidationException
     * Returns 400 Bad Request
     * 
     * Triggered when: Business logic validation fails
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle MethodArgumentNotValidException
     * Returns 400 Bad Request with validation details
     * 
     * Triggered when: @Valid annotation fails on request body
     * (e.g., @NotNull, @NotBlank, @Email violations)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        
        // Extract field validation errors
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            Object rejectedValue = ((FieldError) error).getRejectedValue();
            
            validationErrors.add(ErrorResponse.ValidationError.builder()
                    .field(fieldName)
                    .message(message)
                    .rejectedValue(rejectedValue)
                    .build());
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed for one or more fields")
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle MethodArgumentTypeMismatchException
     * Returns 400 Bad Request
     * 
     * Triggered when: Path variable or query param has wrong type
     * (e.g., expecting Integer but got String)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle ApiException
     * Returns status code specified in exception
     * 
     * Triggered when: Custom API exception with specific HTTP status
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request) {
        
        HttpStatus status = ex.getStatus();
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    /**
     * Handle Generic Exception
     * Returns 500 Internal Server Error
     * 
     * Triggered when: Unexpected error occurs (last resort handler)
     * Logs full stack trace for debugging
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        // Log the exception for debugging
        System.err.println("Unhandled exception occurred:");
        ex.printStackTrace();
        
        // Don't expose internal error details to client in production
        String message = "An unexpected error occurred. Please try again later.";
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
