package com.cityatlas.backend.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.dto.event.AnalyticsEventPayload;
import com.cityatlas.backend.dto.response.CityResponse;
import com.cityatlas.backend.service.AnalyticsEventProducer;

import lombok.extern.slf4j.Slf4j;

/**
 * City Controller - City Profile Endpoints
 * 
 * Handles requests for city information and overview data.
 * Base path: /api/cities
 * 
 * Endpoints:
 * - GET /api/cities/{slug} - Get city details by slug
 * 
 * Analytics Integration:
 * - Publishes CITY_SEARCHED events to Kafka asynchronously
 * - Events are fire-and-forget (don't block API responses)
 * - Failed event publishing is logged but doesn't affect API response
 * 
 * TODO: Connect to CityService for database operations
 */
@RestController
@RequestMapping("/api/cities")
@Slf4j
public class CityController {
    
    /**
     * Analytics event producer for Kafka integration (Optional)
     * Used to publish user behavior events asynchronously when Kafka is enabled
     */
    @Autowired(required = false)
    private AnalyticsEventProducer analyticsEventProducer;
    
    /**
     * Get City Details by Slug
     * 
     * Endpoint: GET /api/cities/{slug}
     * Example: GET /api/cities/san-francisco
     * 
     * Returns comprehensive city information including:
     * - Basic info (name, location, population)
     * - Economic indicators (GDP per capita, unemployment)
     * - Cost of living metrics
     * 
     * Analytics Integration:
     * - Publishes CITY_SEARCHED event to Kafka (async)
     * - Event includes citySlug, sessionId, and timestamp
     * - Publishing happens AFTER response is prepared (non-blocking)
     * - Failed event publishing does NOT affect API response
     * 
     * @param slug URL-friendly city identifier (e.g., "san-francisco", "new-york")
     * @param sessionId Optional session ID for tracking (from frontend)
     * @param userId Optional user ID if authenticated
     * @return CityResponse with city details
     * 
     * Response Codes:
     * - 200 OK: City found and returned
     * - 404 NOT FOUND: City with given slug does not exist (TODO)
     * 
     * TODO: Replace mock data with service call
     * TODO: Add error handling for non-existent cities
     * TODO: Add caching for frequently accessed cities
     */
    @GetMapping("/{slug}")
    public ResponseEntity<CityResponse> getCityBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String userId) {
        // ============================================
        // MOCK DATA - Replace with service call
        // TODO: CityResponse city = cityService.getCityBySlug(slug);
        // ============================================
        
        CityResponse city = createMockCityResponse(slug);
        
        // ============================================
        // ANALYTICS EVENT PUBLISHING (Non-blocking)
        // ============================================
        // Publish CITY_SEARCHED event to Kafka asynchronously
        // This tracks when users view city profiles
        // 
        // IMPORTANT: This is fire-and-forget - the API response
        // is NOT blocked or affected by Kafka publishing.
        // 
        // Event Flow: API -> Kafka -> Consumer -> Database
        // ============================================
        try {
            AnalyticsEventPayload event = AnalyticsEventPayload.builder()
                    .eventType("CITY_SEARCHED")
                    .citySlug(slug)
                    .sessionId(sessionId)
                    .userId(userId)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            // Async publishing - returns CompletableFuture but we don't wait
            // Only publish if Kafka is enabled (analyticsEventProducer is available)
            if (analyticsEventProducer != null) {
                analyticsEventProducer.publishCitySearched(event);
                log.debug("CITY_SEARCHED event published for citySlug: {}", slug);
            } else {
                log.trace("Kafka disabled - skipping CITY_SEARCHED event for citySlug: {}", slug);
            }
            
        } catch (Exception ex) {
            // Log error but don't fail the API request
            // Analytics failures should never affect user experience
            log.error("Failed to publish CITY_SEARCHED event for slug: {}", slug, ex);
        }
        
        return ResponseEntity.ok(city);
    }
    
    // ============================================
    // Mock Data Generation - TEMPORARY
    // ============================================
    
    /**
     * Creates mock city data for development/testing
     * 
     * TODO: Remove this method once database integration is complete
     */
    private CityResponse createMockCityResponse(String slug) {
        // Convert slug to display name
        String displayName = convertSlugToName(slug);
        
        return CityResponse.builder()
            .id(1L)
            .slug(slug)
            .name(displayName)
            .state("California")
            .country("United States")
            .population(873_965L)
            .gdpPerCapita(85_000.0)
            .latitude(37.7749)
            .longitude(-122.4194)
            .costOfLivingIndex(158)
            .unemploymentRate(3.8)
            .bannerImageUrl("https://images.unsplash.com/photo-1501594907352-04cda38ebc29")
            .description("Global technology hub and cultural center on the West Coast")
            .lastUpdated(LocalDateTime.now())
            .build();
    }
    
    /**
     * Helper: Convert URL slug to display name
     * Example: "san-francisco" -> "San Francisco"
     * 
     * TODO: Move to utility class
     */
    private String convertSlugToName(String slug) {
        String[] words = slug.split("-");
        StringBuilder name = new StringBuilder();
        
        for (String word : words) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(word.substring(0, 1).toUpperCase())
                .append(word.substring(1).toLowerCase());
        }
        
        return name.toString();
    }
}
