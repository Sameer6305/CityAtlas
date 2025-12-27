package com.cityatlas.backend.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Health Check Response DTO
 * 
 * Simple health status response for monitoring endpoints.
 * Used to verify application is running and responsive.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {
    
    /**
     * Application name
     */
    private String application;
    
    /**
     * Health status (UP, DOWN)
     */
    private String status;
    
    /**
     * Current server timestamp
     */
    private LocalDateTime timestamp;
    
    /**
     * Application version (optional)
     */
    private String version;
    
    /**
     * Additional information (optional)
     */
    private String message;
}
