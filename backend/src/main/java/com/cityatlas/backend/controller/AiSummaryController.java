package com.cityatlas.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.dto.response.AiCitySummaryDTO;
import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.exception.ResourceNotFoundException;
import com.cityatlas.backend.repository.CityRepository;
import com.cityatlas.backend.service.AiCitySummaryService;
import com.cityatlas.backend.service.external.AirQualityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API Controller for AI City Summary
 * 
 * Provides explainable, rule-based city personality insights and recommendations.
 * 
 * This endpoint generates summaries on-demand by analyzing city metrics including:
 * - Economic indicators (GDP, unemployment, cost of living)
 * - Environmental data (air quality)
 * - Population demographics
 * - User engagement analytics
 * 
 * Design Notes:
 * - No caching: Ensures real-time data freshness (can be added later if needed)
 * - No authentication: Public data accessible to all users
 * - Graceful degradation: Returns summary even with partial data
 * 
 * @see AiCitySummaryService
 * @see AiCitySummaryDTO
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow frontend access from any origin
public class AiSummaryController {
    
    private final CityRepository cityRepository;
    private final AiCitySummaryService aiSummaryService;
    private final AirQualityService airQualityService;
    
    /**
     * Get AI-generated city summary with personality, strengths, weaknesses, and ideal audience
     * 
     * Endpoint: GET /api/ai/summary/{citySlug}
     * 
     * Flow:
     * 1. Fetch city entity by slug (throw 404 if not found)
     * 2. Retrieve current air quality index (null if unavailable)
     * 3. Calculate popularity score from city data (defaults to 50 if not available)
     * 4. Generate AI summary using rule-based logic
     * 5. Return structured JSON response
     * 
     * Error Handling:
     * - 404: City not found (slug doesn't match any city in database)
     * - 200: Success, even with partial data (some metrics may be null)
     * 
     * Example Request:
     * GET /api/ai/summary/new-york
     * 
     * Example Response:
     * {
     *   "personality": "A major metropolitan hub with a thriving, prosperous economy...",
     *   "strengths": ["Strong, prosperous economy", "Major metropolitan amenities"],
     *   "weaknesses": ["High cost of living"],
     *   "bestSuitedFor": ["High-earning professionals", "Urban enthusiasts"]
     * }
     * 
     * @param citySlug URL-friendly city identifier (e.g., "new-york", "san-francisco")
     * @return AI-generated city summary with personality insights
     * @throws ResourceNotFoundException if city with given slug doesn't exist
     */
    @GetMapping("/summary/{citySlug}")
    public ResponseEntity<AiCitySummaryDTO> getCitySummary(@PathVariable String citySlug) {
        log.info("AI summary requested for city: {}", citySlug);
        
        // Step 1: Fetch city by slug
        // Why: Slug is the primary identifier in URLs (e.g., /cities/new-york)
        // Throws 404 if city doesn't exist - clear feedback to frontend
        City city = cityRepository.findBySlug(citySlug)
                .orElseThrow(() -> {
                    log.warn("City not found for slug: {}", citySlug);
                    return new ResourceNotFoundException("City not found with slug: " + citySlug);
                });
        
        log.debug("Found city: {} (ID: {})", city.getName(), city.getId());
        
        // Step 2: Fetch current air quality index
        // Why: AQI is a key quality-of-life metric that changes frequently
        // Graceful degradation: If AQI service fails or returns empty, we pass null
        // The AI service will still generate a summary without environmental insights
        Integer currentAqi = null;
        try {
            currentAqi = airQualityService.fetchAirQualityByCoordinates(
                    city.getLatitude(), 
                    city.getLongitude(), 
                    25) // 25km radius
                    .map(aqDto -> aqDto.getAqi())
                    .block(); // Block to convert Mono to synchronous value
            log.debug("Retrieved AQI for {}: {}", citySlug, currentAqi);
        } catch (Exception e) {
            // Don't fail the entire request if AQI is unavailable
            // This is expected if API keys are missing or service is down
            log.warn("Could not retrieve AQI for {}: {}", citySlug, e.getMessage());
        }
        
        // Step 3: Calculate popularity score
        // Why: Popularity indicates user interest and can signal desirability
        // Note: This is a simplified calculation - in production, you might:
        //   - Query analytics events table for view counts
        //   - Calculate relative popularity compared to other cities
        //   - Use time-weighted engagement metrics
        // For now: Default to moderate popularity (50/100) if no data available
        Integer popularityScore = calculatePopularityScore(city);
        log.debug("Calculated popularity score for {}: {}", citySlug, popularityScore);
        
        // Step 4: Generate AI summary
        // Why: This is the core value-add - translating raw metrics into insights
        // The service uses rule-based logic to ensure explainability
        AiCitySummaryDTO summary = aiSummaryService.generateSummary(city, currentAqi, popularityScore);
        
        log.info("Successfully generated AI summary for {}", citySlug);
        
        // Step 5: Return 200 OK with summary
        // Why: Even with partial data, we can provide value to users
        // The frontend can display available insights and handle missing fields
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Calculate popularity score for a city
     * 
     * Logic:
     * - This is a placeholder implementation
     * - In production, this would query the analytics_events table
     * - Could count views, searches, saves over the last 30 days
     * - Normalize to 0-100 scale by comparing to other cities
     * 
     * Current Implementation:
     * - Returns 50 (moderate popularity) as default
     * - Ensures the AI summary generator has a reasonable baseline
     * 
     * Future Enhancements:
     * - Inject AnalyticsEventRepository
     * - Query: SELECT COUNT(*) FROM analytics_events WHERE city_slug = ? AND timestamp > NOW() - INTERVAL '30 days'
     * - Normalize against city population size (per-capita popularity)
     * 
     * Why this matters: Popularity can indicate hidden gems or oversaturated markets
     * 
     * @param city The city entity (could use analytics data in future)
     * @return Popularity score 0-100 (currently defaults to 50)
     */
    private Integer calculatePopularityScore(City city) {
        // TODO: Implement real popularity calculation from analytics_events table
        // For now, return moderate popularity for all cities
        // This prevents the AI summary from making assumptions about popularity
        return 50;
    }
}
