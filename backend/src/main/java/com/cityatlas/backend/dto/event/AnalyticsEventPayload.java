package com.cityatlas.backend.dto.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ANALYTICS EVENT PAYLOAD - Kafka Message Schema
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Standard payload structure for all analytics events published to Kafka.
 * This DTO is serialized to JSON and sent to various analytics topics.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY STRUCTURED EVENTS?
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. SCHEMA CONSISTENCY:
 *    - All events follow the same structure
 *    - Easy to parse in any downstream system
 *    - Schema evolution with backward compatibility
 * 
 * 2. TYPE SAFETY:
 *    - Compile-time validation in Java
 *    - Runtime validation via isValid() methods
 *    - JSON schema can be generated for documentation
 * 
 * 3. ANALYTICS PIPELINE COMPATIBILITY:
 *    - Works with Kafka Connect transformations
 *    - Easy to query in BigQuery/Snowflake
 *    - ML feature extraction is straightforward
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * EVENT TYPE SCHEMAS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. CITY_SEARCHED - User searched for cities
 * ────────────────────────────────────────────────────────────────────────────────
 * {
 *   "eventType": "CITY_SEARCHED",
 *   "searchQuery": "best tech cities",         // REQUIRED: Search term
 *   "citySlug": "san-francisco",              // OPTIONAL: If user clicked result
 *   "sessionId": "sess_abc123",               // REQUIRED: Session tracking
 *   "userId": "user_xyz789",                  // OPTIONAL: If authenticated
 *   "timestamp": "2026-01-08T14:30:00",       // REQUIRED: When event occurred
 *   "referrer": "google",                     // OPTIONAL: Traffic source
 *   "deviceType": "desktop"                   // OPTIONAL: Device category
 * }
 * 
 * Analytics Use Cases:
 * - Track popular search terms
 * - Identify cities users want but don't exist
 * - Calculate search-to-click conversion rate
 * - Feed search autocomplete ML model
 * 
 * ────────────────────────────────────────────────────────────────────────────────
 * 2. SECTION_VIEWED - User viewed a city section
 * ────────────────────────────────────────────────────────────────────────────────
 * {
 *   "eventType": "SECTION_VIEWED",
 *   "citySlug": "new-york",                   // REQUIRED: Which city
 *   "section": "economy",                     // REQUIRED: Which section
 *   "sessionId": "sess_abc123",               // REQUIRED: Session tracking
 *   "userId": "user_xyz789",                  // OPTIONAL: If authenticated
 *   "timestamp": "2026-01-08T14:31:00",       // REQUIRED: When event occurred
 *   "referrer": "internal",                   // OPTIONAL: Previous page
 *   "deviceType": "mobile"                    // OPTIONAL: Device category
 * }
 * 
 * Analytics Use Cases:
 * - Track section popularity per city
 * - Optimize section ordering
 * - Personalize homepage recommendations
 * - A/B test section layouts
 * 
 * ────────────────────────────────────────────────────────────────────────────────
 * 3. TIME_SPENT_ON_SECTION - User engagement duration
 * ────────────────────────────────────────────────────────────────────────────────
 * {
 *   "eventType": "TIME_SPENT_ON_SECTION",
 *   "citySlug": "tokyo",                      // REQUIRED: Which city
 *   "section": "culture",                     // REQUIRED: Which section
 *   "durationInSeconds": 245,                 // REQUIRED: Time spent
 *   "sessionId": "sess_abc123",               // REQUIRED: Session tracking
 *   "userId": "user_xyz789",                  // OPTIONAL: If authenticated
 *   "timestamp": "2026-01-08T14:35:00",       // REQUIRED: When user left
 *   "deviceType": "desktop"                   // OPTIONAL: Device category
 * }
 * 
 * Analytics Use Cases:
 * - Measure content quality (avg time = engagement)
 * - Identify high-value content (long read times)
 * - Detect content issues (very short times = bounce)
 * - Feed recommendation engine with engagement signals
 * 
 * Engagement Benchmarks:
 * - < 10s: Bounce (potential content issue)
 * - 10-60s: Quick scan
 * - 60-180s: Normal engagement
 * - > 180s: Deep engagement (high-value content)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Thread Safety: This class is immutable when used with Lombok's @Builder
 * Serialization: Uses Jackson for JSON serialization to Kafka
 * 
 * @see com.cityatlas.backend.service.AnalyticsEventProducer
 * @see com.cityatlas.backend.service.AnalyticsEventConsumer
 * @see com.cityatlas.backend.service.KafkaEventLogger
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null fields from JSON
public class AnalyticsEventPayload {
    
    /**
     * City slug identifier (URL-friendly name)
     * 
     * Examples: "san-francisco", "new-york", "tokyo"
     * 
     * Required for: SECTION_VIEWED, TIME_SPENT_ON_SECTION
     * Optional for: CITY_SEARCHED (null if user searched but didn't click a city)
     */
    private String citySlug;
    
    /**
     * Type of analytics event
     * 
     * Possible values:
     * - CITY_SEARCHED: User performed a search
     * - SECTION_VIEWED: User navigated to a section
     * - TIME_SPENT_ON_SECTION: Duration measurement captured
     * 
     * This field determines which other fields are required/populated.
     */
    private String eventType;
    
    /**
     * Section identifier (for section-specific events)
     * 
     * Possible values:
     * - "overview"
     * - "economy"
     * - "infrastructure"
     * - "environment"
     * - "education"
     * - "culture"
     * - "analytics"
     * 
     * Required for: SECTION_VIEWED, TIME_SPENT_ON_SECTION
     * Null for: CITY_SEARCHED
     */
    private String section;
    
    /**
     * Time spent on the section in seconds
     * 
     * Calculation Notes:
     * - Measured from section load to section exit
     * - Cap at reasonable maximum (e.g., 3600 seconds / 1 hour)
     * - Exclude idle time if possible (tab not focused)
     * 
     * Required for: TIME_SPENT_ON_SECTION
     * Null for: CITY_SEARCHED, SECTION_VIEWED
     * 
     * Example values:
     * - 15 seconds: Quick scan
     * - 120 seconds: Moderate engagement
     * - 300+ seconds: Deep reading
     */
    private Integer durationInSeconds;
    
    /**
     * Event timestamp (when event occurred)
     * 
     * Generated on the frontend when the user action happens.
     * Used for:
     * - Event ordering in analytics
     * - Time-series analysis
     * - Session reconstruction
     * 
     * Format: ISO-8601 LocalDateTime
     * Example: 2025-12-27T14:30:00
     */
    private LocalDateTime timestamp;
    
    /**
     * Optional: User session ID
     * Groups related events from the same browsing session
     */
    private String sessionId;
    
    /**
     * Optional: User ID (if authenticated)
     * Null for anonymous users
     */
    private String userId;
    
    /**
     * Optional: Search query (for CITY_SEARCHED events)
     * The actual search term entered by the user
     * Example: "best cities for tech jobs"
     */
    private String searchQuery;
    
    /**
     * Optional: Referrer source
     * Where the user came from (direct, google, social, etc.)
     */
    private String referrer;
    
    /**
     * Optional: Device type
     * Examples: "desktop", "mobile", "tablet"
     */
    private String deviceType;
    
    // ============================================
    // VALIDATION HELPERS
    // ============================================
    
    /**
     * Validate if this payload is complete for CITY_SEARCHED event
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValidCitySearchedEvent() {
        return "CITY_SEARCHED".equals(eventType) 
            && timestamp != null
            && searchQuery != null;
    }
    
    /**
     * Validate if this payload is complete for SECTION_VIEWED event
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValidSectionViewedEvent() {
        return "SECTION_VIEWED".equals(eventType)
            && citySlug != null
            && section != null
            && timestamp != null;
    }
    
    /**
     * Validate if this payload is complete for TIME_SPENT_ON_SECTION event
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValidTimeSpentEvent() {
        return "TIME_SPENT_ON_SECTION".equals(eventType)
            && citySlug != null
            && section != null
            && durationInSeconds != null
            && durationInSeconds > 0
            && timestamp != null;
    }
    
    /**
     * Validate if this payload is valid for any event type
     * 
     * @return true if valid for its declared eventType
     */
    public boolean isValid() {
        if (eventType == null) {
            return false;
        }
        
        return switch (eventType) {
            case "CITY_SEARCHED" -> isValidCitySearchedEvent();
            case "SECTION_VIEWED" -> isValidSectionViewedEvent();
            case "TIME_SPENT_ON_SECTION" -> isValidTimeSpentEvent();
            default -> false;
        };
    }
}
