package com.cityatlas.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cityatlas.backend.dto.event.AnalyticsEventPayload;
import com.cityatlas.backend.entity.AnalyticsEvent;
import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.entity.EventType;
import com.cityatlas.backend.repository.AnalyticsEventRepository;
import com.cityatlas.backend.repository.CityRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Analytics Event Service
 * 
 * Business logic layer for processing analytics events consumed from Kafka.
 * This service handles the actual work of persisting and processing events,
 * keeping the consumer layer thin and focused on message consumption.
 * 
 * EVENT FLOW:
 * 1. API: Frontend sends analytics data to REST endpoint
 * 2. Producer: Controller publishes AnalyticsEventPayload to Kafka topic
 * 3. Kafka: Message is stored in topic partition (async, reliable)
 * 4. Consumer: @KafkaListener receives message and calls this service
 * 5. Service: Maps DTO -> Entity, resolves relationships, validates
 * 6. Database: AnalyticsEvent persisted to PostgreSQL via repository
 * 
 * Responsibilities:
 * - Map AnalyticsEventPayload (DTO) to AnalyticsEvent (entity)
 * - Resolve City entity by slug (handle missing cities gracefully)
 * - Validate event data (secondary validation after Kafka deserialization)
 * - Persist events to the database via repository
 * - Handle database errors without crashing consumer
 * 
 * Future Enhancements:
 * - Real-time analytics aggregation (Redis counters)
 * - Event batching for improved database performance
 * - Cache popular queries and sections
 * - ML pipeline integration for recommendations
 * 
 * @see AnalyticsEventConsumer
 * @see AnalyticsEventProducer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventService {
    
    private final AnalyticsEventRepository analyticsEventRepository;
    private final CityRepository cityRepository;
    
    /**
     * Process CITY_SEARCHED event
     * 
     * EVENT FLOW: API -> Kafka (city-searched topic) -> This method -> Database
     * 
     * Business Logic:
     * 1. Look up city by slug (if user clicked a search result)
     * 2. Map AnalyticsEventPayload to AnalyticsEvent entity
     * 3. Store search query in metadata field for trending analysis
     * 4. Persist to analytics_events table
     * 
     * Graceful Handling:
     * - If citySlug is null: Event is saved without city reference (global search)
     * - If citySlug is invalid: Logs warning but still saves event (data integrity)
     * - If database fails: Exception propagates to consumer error handler
     * 
     * @param event The city searched event from Kafka
     */
    @Transactional
    public void processCitySearchedEvent(AnalyticsEventPayload event) {
        log.debug("Processing CITY_SEARCHED event: {}", event);
        
        // Resolve city reference if citySlug is provided
        City city = null;
        if (event.getCitySlug() != null && !event.getCitySlug().isBlank()) {
            city = resolveCitySafely(event.getCitySlug());
        }
        
        // Build metadata JSON with search context
        String metadata = buildSearchMetadata(event);
        
        // Map DTO to Entity
        AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                .city(city) // Nullable - some searches don't result in city selection
                .eventType(EventType.SEARCH)
                .value(null) // No numeric value for searches
                .metadata(metadata)
                .userId(event.getUserId())
                .sessionId(event.getSessionId())
                .eventTimestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build();
        
        // Persist to database
        analyticsEventRepository.save(analyticsEvent);
        
        log.info("CITY_SEARCHED event persisted: searchQuery='{}', citySlug='{}', city={}",
            event.getSearchQuery(), event.getCitySlug(), city != null ? "resolved" : "null");
    }
    
    /**
     * Process SECTION_VIEWED event
     * 
     * EVENT FLOW: API -> Kafka (section-viewed topic) -> This method -> Database
     * 
     * Business Logic:
     * 1. Validate required fields (citySlug and section must be present)
     * 2. Look up city entity by slug
     * 3. Map AnalyticsEventPayload to AnalyticsEvent entity
     * 4. Store section name in metadata for analytics queries
     * 5. Persist to analytics_events table
     * 
     * Graceful Handling:
     * - If citySlug is missing: Throws exception (invalid event)
     * - If city not found: Logs warning and saves event without city reference
     * - Section name stored in metadata for flexible querying
     * 
     * @param event The section viewed event from Kafka
     */
    @Transactional
    public void processSectionViewedEvent(AnalyticsEventPayload event) {
        log.debug("Processing SECTION_VIEWED event: {}", event);
        
        // Validate required fields
        if (event.getCitySlug() == null || event.getCitySlug().isBlank()) {
            log.error("SECTION_VIEWED event missing citySlug: {}", event);
            throw new IllegalArgumentException("citySlug is required for SECTION_VIEWED events");
        }
        
        if (event.getSection() == null || event.getSection().isBlank()) {
            log.error("SECTION_VIEWED event missing section: {}", event);
            throw new IllegalArgumentException("section is required for SECTION_VIEWED events");
        }
        
        // Resolve city reference
        City city = resolveCitySafely(event.getCitySlug());
        
        // Build metadata JSON with section context
        String metadata = buildSectionMetadata(event);
        
        // Map DTO to Entity
        AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                .city(city) // May be null if city not found
                .eventType(EventType.PAGE_VIEW) // Section view is a page view
                .value(null) // No numeric value for section views
                .metadata(metadata)
                .userId(event.getUserId())
                .sessionId(event.getSessionId())
                .eventTimestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build();
        
        // Persist to database
        analyticsEventRepository.save(analyticsEvent);
        
        log.info("SECTION_VIEWED event persisted: citySlug='{}', section='{}', city={}",
            event.getCitySlug(), event.getSection(), city != null ? "resolved" : "null");
    }
    
    /**
     * Process TIME_SPENT_ON_SECTION event
     * 
     * EVENT FLOW: API -> Kafka (time-spent topic) -> This method -> Database
     * 
     * Business Logic:
     * 1. Validate required fields (citySlug, section, durationInSeconds)
     * 2. Look up city entity by slug
     * 3. Map AnalyticsEventPayload to AnalyticsEvent entity
     * 4. Store duration as the event value (in seconds)
     * 5. Store section name in metadata for analytics queries
     * 6. Persist to analytics_events table
     * 
     * Graceful Handling:
     * - If required fields missing: Throws exception (invalid event)
     * - If city not found: Logs warning and saves event without city reference
     * - If duration is zero or negative: Logs warning but still saves (data quality)
     * - Very low engagement (<10s) logged for content review
     * 
     * @param event The time spent event from Kafka
     */
    @Transactional
    public void processTimeSpentOnSectionEvent(AnalyticsEventPayload event) {
        log.debug("Processing TIME_SPENT_ON_SECTION event: {}", event);
        
        // Validate required fields
        if (event.getCitySlug() == null || event.getCitySlug().isBlank()) {
            log.error("TIME_SPENT_ON_SECTION event missing citySlug: {}", event);
            throw new IllegalArgumentException("citySlug is required for TIME_SPENT_ON_SECTION events");
        }
        
        if (event.getSection() == null || event.getSection().isBlank()) {
            log.error("TIME_SPENT_ON_SECTION event missing section: {}", event);
            throw new IllegalArgumentException("section is required for TIME_SPENT_ON_SECTION events");
        }
        
        if (event.getDurationInSeconds() == null) {
            log.error("TIME_SPENT_ON_SECTION event missing durationInSeconds: {}", event);
            throw new IllegalArgumentException("durationInSeconds is required for TIME_SPENT_ON_SECTION events");
        }
        
        // Log data quality issues (but continue processing)
        if (event.getDurationInSeconds() <= 0) {
            log.warn("TIME_SPENT_ON_SECTION event has zero or negative duration: {} seconds", 
                event.getDurationInSeconds());
        }
        
        // Flag very low engagement for content review
        if (event.getDurationInSeconds() < 10) {
            log.info("Low engagement detected: citySlug='{}', section='{}', duration={}s (consider content review)",
                event.getCitySlug(), event.getSection(), event.getDurationInSeconds());
        }
        
        // Resolve city reference
        City city = resolveCitySafely(event.getCitySlug());
        
        // Build metadata JSON with section and duration context
        String metadata = buildEngagementMetadata(event);
        
        // Map DTO to Entity
        // Store duration as the numeric value field
        AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                .city(city) // May be null if city not found
                .eventType(EventType.CITY_VIEW) // Generic city view event with duration metric
                .value(event.getDurationInSeconds().doubleValue()) // Duration in seconds
                .metadata(metadata)
                .userId(event.getUserId())
                .sessionId(event.getSessionId())
                .eventTimestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build();
        
        // Persist to database
        analyticsEventRepository.save(analyticsEvent);
        
        log.info("TIME_SPENT_ON_SECTION event persisted: citySlug='{}', section='{}', duration={}s, city={}",
            event.getCitySlug(), event.getSection(), event.getDurationInSeconds(), 
            city != null ? "resolved" : "null");
    }
    
    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================
    
    /**
     * Resolve city entity by slug, handling missing cities gracefully
     * 
     * This method never throws exceptions - it returns null if city not found.
     * This ensures that analytics events are still captured even if the city
     * was deleted or the slug is invalid (important for data integrity).
     * 
     * @param citySlug The city slug to look up
     * @return City entity if found, null otherwise
     */
    private City resolveCitySafely(String citySlug) {
        Optional<City> cityOpt = cityRepository.findBySlug(citySlug);
        
        if (cityOpt.isEmpty()) {
            log.warn("City not found for slug '{}' - event will be saved without city reference", citySlug);
            return null;
        }
        
        return cityOpt.get();
    }
    
    /**
     * Build metadata JSON for search events
     * 
     * Includes:
     * - Search query
     * - Referrer
     * - Device type
     * 
     * @param event The analytics event payload
     * @return JSON string for metadata column
     */
    private String buildSearchMetadata(AnalyticsEventPayload event) {
        // Using simple string concatenation for now
        // TODO: Consider using Jackson ObjectMapper for proper JSON serialization
        StringBuilder json = new StringBuilder("{");
        
        if (event.getSearchQuery() != null) {
            json.append("\"searchQuery\":\"").append(escapeJson(event.getSearchQuery())).append("\",");
        }
        
        if (event.getReferrer() != null) {
            json.append("\"referrer\":\"").append(escapeJson(event.getReferrer())).append("\",");
        }
        
        if (event.getDeviceType() != null) {
            json.append("\"deviceType\":\"").append(escapeJson(event.getDeviceType())).append("\",");
        }
        
        // Remove trailing comma if present
        if (json.charAt(json.length() - 1) == ',') {
            json.setLength(json.length() - 1);
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Build metadata JSON for section view events
     * 
     * Includes:
     * - Section name
     * - Referrer
     * - Device type
     * 
     * @param event The analytics event payload
     * @return JSON string for metadata column
     */
    private String buildSectionMetadata(AnalyticsEventPayload event) {
        StringBuilder json = new StringBuilder("{");
        
        json.append("\"section\":\"").append(escapeJson(event.getSection())).append("\",");
        
        if (event.getReferrer() != null) {
            json.append("\"referrer\":\"").append(escapeJson(event.getReferrer())).append("\",");
        }
        
        if (event.getDeviceType() != null) {
            json.append("\"deviceType\":\"").append(escapeJson(event.getDeviceType())).append("\",");
        }
        
        // Remove trailing comma
        if (json.charAt(json.length() - 1) == ',') {
            json.setLength(json.length() - 1);
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Build metadata JSON for engagement (time spent) events
     * 
     * Includes:
     * - Section name
     * - Duration in seconds (also stored in value field for easy querying)
     * - Device type
     * 
     * @param event The analytics event payload
     * @return JSON string for metadata column
     */
    private String buildEngagementMetadata(AnalyticsEventPayload event) {
        StringBuilder json = new StringBuilder("{");
        
        json.append("\"section\":\"").append(escapeJson(event.getSection())).append("\",");
        json.append("\"durationInSeconds\":").append(event.getDurationInSeconds()).append(",");
        
        if (event.getDeviceType() != null) {
            json.append("\"deviceType\":\"").append(escapeJson(event.getDeviceType())).append("\",");
        }
        
        // Remove trailing comma
        if (json.charAt(json.length() - 1) == ',') {
            json.setLength(json.length() - 1);
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape special characters for JSON strings
     * 
     * Handles: quotes, backslashes, newlines, tabs
     * 
     * @param str The string to escape
     * @return Escaped string safe for JSON
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
