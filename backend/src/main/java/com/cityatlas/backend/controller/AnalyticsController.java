package com.cityatlas.backend.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.dto.event.AnalyticsEventPayload;
import com.cityatlas.backend.dto.response.AnalyticsResponse;
import com.cityatlas.backend.dto.response.AnalyticsResponse.AQIDataPoint;
import com.cityatlas.backend.dto.response.AnalyticsResponse.CostOfLivingData;
import com.cityatlas.backend.dto.response.AnalyticsResponse.JobSectorData;
import com.cityatlas.backend.dto.response.AnalyticsResponse.PopulationDataPoint;
import com.cityatlas.backend.service.AnalyticsEventProducer;

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
     * Used to publish user behavior events asynchronously when Kafka is enabled
     */
    @Autowired(required = false)
    private AnalyticsEventProducer analyticsEventProducer;
    
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
        // MOCK DATA - Replace with service call
        // TODO: AnalyticsResponse analytics = analyticsService.getAnalyticsBySlug(slug);
        // ============================================
        
        AnalyticsResponse analytics = createMockAnalyticsResponse(slug);
        
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
    
    // ============================================
    // Mock Data Generation - TEMPORARY
    // ============================================
    
    /**
     * Creates mock analytics data for development/testing
     * 
     * Matches the data structure expected by frontend Recharts components.
     * 
     * TODO: Remove this method once database integration is complete
     * TODO: In production, this data comes from:
     * - Environmental monitoring APIs (OpenAQ, IQAir)
     * - Labor statistics databases
     * - Census data
     * - Economic indices
     */
    private AnalyticsResponse createMockAnalyticsResponse(String slug) {
        String cityName = convertSlugToName(slug);
        
        return AnalyticsResponse.builder()
            .citySlug(slug)
            .cityName(cityName)
            .aqiTrend(createMockAQIData())
            .jobSectors(createMockJobSectorData())
            .costOfLiving(createMockCostOfLivingData())
            .populationTrend(createMockPopulationData())
            .build();
    }
    
    /**
     * Mock AQI (Air Quality Index) Trend - 12 months
     * Lower values = better air quality
     * 0-50: Good | 51-100: Moderate | 101-150: Unhealthy | 151+: Hazardous
     */
    private List<AQIDataPoint> createMockAQIData() {
        return Arrays.asList(
            AQIDataPoint.builder().month("Jan").aqi(52).category("Moderate").build(),
            AQIDataPoint.builder().month("Feb").aqi(48).category("Good").build(),
            AQIDataPoint.builder().month("Mar").aqi(45).category("Good").build(),
            AQIDataPoint.builder().month("Apr").aqi(41).category("Good").build(),
            AQIDataPoint.builder().month("May").aqi(38).category("Good").build(),
            AQIDataPoint.builder().month("Jun").aqi(42).category("Good").build(),
            AQIDataPoint.builder().month("Jul").aqi(47).category("Good").build(),
            AQIDataPoint.builder().month("Aug").aqi(44).category("Good").build(),
            AQIDataPoint.builder().month("Sep").aqi(40).category("Good").build(),
            AQIDataPoint.builder().month("Oct").aqi(43).category("Good").build(),
            AQIDataPoint.builder().month("Nov").aqi(46).category("Good").build(),
            AQIDataPoint.builder().month("Dec").aqi(45).category("Good").build()
        );
    }
    
    /**
     * Mock Employment Distribution by Sector
     * Represents current workforce breakdown
     */
    private List<JobSectorData> createMockJobSectorData() {
        return Arrays.asList(
            JobSectorData.builder()
                .sector("Technology")
                .employees(185000)
                .percentage(28.5)
                .growthRate(5.2)
                .build(),
            JobSectorData.builder()
                .sector("Healthcare")
                .employees(142000)
                .percentage(21.8)
                .growthRate(3.1)
                .build(),
            JobSectorData.builder()
                .sector("Finance")
                .employees(98000)
                .percentage(15.1)
                .growthRate(2.4)
                .build(),
            JobSectorData.builder()
                .sector("Education")
                .employees(87000)
                .percentage(13.4)
                .growthRate(1.8)
                .build(),
            JobSectorData.builder()
                .sector("Retail")
                .employees(76000)
                .percentage(11.7)
                .growthRate(-0.5)
                .build(),
            JobSectorData.builder()
                .sector("Manufacturing")
                .employees(62000)
                .percentage(9.5)
                .growthRate(-1.2)
                .build()
        );
    }
    
    /**
     * Mock Cost of Living Index by Category
     * 100 = national average
     * Values above 100 indicate higher than average cost
     */
    private List<CostOfLivingData> createMockCostOfLivingData() {
        return Arrays.asList(
            CostOfLivingData.builder()
                .category("Housing")
                .index(195)
                .nationalAverage(100)
                .monthlyAvg(2850)
                .build(),
            CostOfLivingData.builder()
                .category("Food")
                .index(142)
                .nationalAverage(100)
                .monthlyAvg(680)
                .build(),
            CostOfLivingData.builder()
                .category("Transportation")
                .index(128)
                .nationalAverage(100)
                .monthlyAvg(420)
                .build(),
            CostOfLivingData.builder()
                .category("Healthcare")
                .index(135)
                .nationalAverage(100)
                .monthlyAvg(580)
                .build(),
            CostOfLivingData.builder()
                .category("Education")
                .index(168)
                .nationalAverage(100)
                .monthlyAvg(1250)
                .build(),
            CostOfLivingData.builder()
                .category("Utilities")
                .index(115)
                .nationalAverage(100)
                .monthlyAvg(185)
                .build()
        );
    }
    
    /**
     * Mock Population Growth Trend - 10 years
     * Population in millions, growth rate as percentage
     */
    private List<PopulationDataPoint> createMockPopulationData() {
        return Arrays.asList(
            PopulationDataPoint.builder().year("2015").population(7.8).growthRate(1.2).build(),
            PopulationDataPoint.builder().year("2016").population(7.9).growthRate(1.3).build(),
            PopulationDataPoint.builder().year("2017").population(8.0).growthRate(1.3).build(),
            PopulationDataPoint.builder().year("2018").population(8.1).growthRate(1.2).build(),
            PopulationDataPoint.builder().year("2019").population(8.2).growthRate(1.2).build(),
            PopulationDataPoint.builder().year("2020").population(8.1).growthRate(-1.2).build(),
            PopulationDataPoint.builder().year("2021").population(8.2).growthRate(1.2).build(),
            PopulationDataPoint.builder().year("2022").population(8.25).growthRate(0.6).build(),
            PopulationDataPoint.builder().year("2023").population(8.3).growthRate(0.6).build(),
            PopulationDataPoint.builder().year("2024").population(8.35).growthRate(0.6).build()
        );
    }
    
    /**
     * Helper: Convert URL slug to display name
     * Example: "san-francisco" -> "San Francisco"
     * 
     * TODO: Move to utility class (duplicated in CityController)
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
