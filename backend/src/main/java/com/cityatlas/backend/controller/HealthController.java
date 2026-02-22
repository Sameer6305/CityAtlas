package com.cityatlas.backend.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.dto.response.HealthResponse;

/**
 * Health Check Controller
 * 
 * Provides simple health check endpoint for monitoring and deployment verification.
 * Used by load balancers, monitoring tools, and CI/CD pipelines.
 * 
 * Endpoints:
 * - GET /api/health - Basic health status
 * 
 * TODO: Add detailed health checks:
 * - Database connectivity
 * - External API availability
 * - Disk space
 * - Memory usage
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    
    /**
     * Basic Health Check Endpoint
     * 
     * Endpoint: GET /api/health
     * 
     * Returns simple status to verify:
     * - Application is running
     * - Server is responding
     * - Basic connectivity works
     * 
     * Use Cases:
     * - Load balancer health checks
     * - Kubernetes liveness/readiness probes
     * - Monitoring tools (Prometheus, Datadog, etc.)
     * - CI/CD deployment verification
     * - Manual testing during development
     * 
     * @return HealthResponse with application status
     * 
     * Response Codes:
     * - 200 OK: Application is healthy and responding
     * 
     * TODO: Add database ping to verify full stack health
     * TODO: Consider using Spring Boot Actuator for advanced health checks
     */
    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse health = HealthResponse.builder()
                .application("CityAtlas Backend API")
                .status("UP")
                .timestamp(LocalDateTime.now())
                .version("0.0.1-SNAPSHOT")
                .message("Service is running normally")
                .build();
        
        return ResponseEntity.ok(health);
    }
    
    // ============================================
    // Future Health Check Enhancements
    // ============================================
    
    /*
     * TODO: Add detailed health endpoint
     * 
     * @GetMapping("/detailed")
     * public ResponseEntity<DetailedHealthResponse> getDetailedHealth() {
     *     return ResponseEntity.ok(DetailedHealthResponse.builder()
     *         .database(checkDatabaseHealth())
     *         .diskSpace(checkDiskSpace())
     *         .memory(checkMemoryUsage())
     *         .build());
     * }
     * 
     * Consider using Spring Boot Actuator:
     * - /actuator/health - Built-in health checks
     * - /actuator/info - Application information
     * - /actuator/metrics - Performance metrics
     */
}
