package com.cityatlas.backend.exception;

/**
 * Resource Not Found Exception
 * 
 * Thrown when a requested resource (city, analytics data, etc.) does not exist.
 * Maps to HTTP 404 Not Found.
 * 
 * Usage:
 * throw new ResourceNotFoundException("City", "slug", citySlug);
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;
    
    /**
     * Constructor with resource details
     * 
     * @param resourceName Type of resource (e.g., "City", "Analytics")
     * @param fieldName Field used to identify resource (e.g., "slug", "id")
     * @param fieldValue Value that was not found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    /**
     * Simple constructor with custom message
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }
    
    public String getResourceName() {
        return resourceName;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getFieldValue() {
        return fieldValue;
    }
}
