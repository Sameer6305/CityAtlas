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
import com.cityatlas.backend.dto.response.AnalyticsResponse;
import com.cityatlas.backend.service.AnalyticsEventProducer;
import com.cityatlas.backend.service.CityDataAggregator;

import lombok.extern.slf4j.Slf4j;

/**
 * Analytics Controller - City Analytics & Metrics Endpoints
 * 
 * Handles requests for city analytics data including:
 * - Environmental quality (AQI trends)
 * - Economic indicators (employment, cost of living)
 * - Demographics (population growth)
 * 
 * Base path: /api/cities/{slug}/analytics
 * 
 * Analytics Integration:
 * - Publishes SECTION_VIEWED events to Kafka asynchronously
 * - Events track when users view the analytics section
 * - Events are fire-and-forget (don't block API responses)
 * 
 * TODO: Connect to AnalyticsService for database operations
 */
@RestController
@RequestMapping("/cities/{slug}/analytics")
@Slf4j
public class AnalyticsController {
    
    /**
     * Analytics event producer for Kafka integration (Optional)
     */
    @Autowired(required = false)
    private AnalyticsEventProducer analyticsEventProducer;

    @Autowired
    private CityDataAggregator cityDataAggregator;
    
    /**
     * Get Comprehensive Analytics for a City
     * 
     * Endpoint: GET /api/cities/{slug}/analytics
     * Example: GET /api/cities/san-francisco/analytics
     * 
     * Returns complete analytics dashboard data:
     * - 12-month AQI trend (environmental quality)
     * - Employment distribution by sector
     * - Cost of living breakdown by category
     * - 10-year population growth trend
     * 
     * This data powers the analytics dashboard charts in the frontend.
     * 
     * Analytics Integration:
     * - Publishes SECTION_VIEWED event to Kafka (async)
     * - Event includes citySlug, section="analytics", sessionId
     * - Publishing happens AFTER response is prepared (non-blocking)
     * - Failed event publishing does NOT affect API response
     * 
     * @param slug URL-friendly city identifier
     * @param sessionId Optional session ID for tracking (from frontend)
     * @param userId Optional user ID if authenticated
     * @return AnalyticsResponse with comprehensive metrics
     * 
     * Response Codes:
     * - 200 OK: Analytics data found and returned
     * - 404 NOT FOUND: City does not exist (TODO)
     * 
     * TODO: Replace mock data with service call
     * TODO: Add query parameters for date ranges (e.g., ?from=2023-01&to=2024-01)
     * TODO: Add caching with TTL (data updates daily/weekly)
     * TODO: Consider pagination for large datasets
     */
    @GetMapping
    public ResponseEntity<AnalyticsResponse> getCityAnalytics(
            @PathVariable String slug,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String userId) {
        // ============================================
        // REAL DATA — fetched from World Bank + OpenAQ APIs
        // Job sectors and cost of living REMOVED (no free source)
        // ============================================
        
        AnalyticsResponse analytics = cityDataAggregator.buildAnalyticsResponse(slug);
        
        // ============================================
        // ANALYTICS EVENT PUBLISHING (Non-blocking)
        // ============================================
        // Publish SECTION_VIEWED event to Kafka asynchronously
        // This tracks when users view the analytics section
        // 
        // IMPORTANT: This is fire-and-forget - the API response
        // is NOT blocked or affected by Kafka publishing.
        // 
        // Event Flow: API -> Kafka -> Consumer -> Database
        // ============================================
        try {
            AnalyticsEventPayload event = AnalyticsEventPayload.builder()
                    .eventType("SECTION_VIEWED")
                    .citySlug(slug)
                    .section("analytics") // This is the analytics section
                    .sessionId(sessionId)
                    .userId(userId)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            // Async publishing - returns CompletableFuture but we don't wait
            // Only publish if Kafka is enabled (analyticsEventProducer is available)
            if (analyticsEventProducer != null) {
                analyticsEventProducer.publishSectionViewed(event);
                log.debug("SECTION_VIEWED event published for citySlug: {}, section: analytics", slug);
            } else {
                log.trace("Kafka disabled - skipping SECTION_VIEWED event for citySlug: {}", slug);
            }
            
        } catch (Exception ex) {
            // Log error but don't fail the API request
            // Analytics failures should never affect user experience
            log.error("Failed to publish SECTION_VIEWED event for slug: {}, section: analytics", slug, ex);
        }
        
        return ResponseEntity.ok(analytics);
    }
}
