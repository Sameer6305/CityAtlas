package com.cityatlas.backend.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.cityatlas.backend.config.KafkaTopics;
import com.cityatlas.backend.dto.event.AnalyticsEventPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ANALYTICS EVENT PRODUCER - Kafka Message Publisher Service
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Responsible for publishing analytics events to Kafka topics.
 * This service acts as the single entry point for all analytics event publishing,
 * ensuring consistent error handling and logging across the application.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY KAFKA FOR EVENT PRODUCTION?
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. FIRE-AND-FORGET SEMANTICS:
 *    - API response doesn't wait for analytics processing
 *    - User experience remains fast regardless of analytics load
 *    - Events are durably stored in Kafka for later processing
 * 
 * 2. GUARANTEED DELIVERY:
 *    - Kafka acknowledgments ensure message persistence
 *    - Replication protects against broker failures
 *    - No lost events even during backend deployments
 * 
 * 3. BACKPRESSURE HANDLING:
 *    - Kafka absorbs traffic spikes
 *    - Consumers process at their own pace
 *    - No dropped events during high load
 * 
 * 4. MULTI-CONSUMER ARCHITECTURE:
 *    - Same event can be consumed by multiple systems
 *    - Real-time dashboards, data warehouses, ML pipelines
 *    - Add new consumers without changing producer code
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * HOW THIS SUPPORTS ANALYTICS PIPELINES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PRODUCER FLOW:
 * 
 *   [User Action]    →    [REST Controller]    →    [This Producer]
 *        ↓                       ↓                        ↓
 *   Click/Search         Validate Request          Publish to Kafka
 *                                                        ↓
 *                                              ┌─────────────────────┐
 *                                              │   KAFKA TOPICS      │
 *                                              │  • city-searched    │
 *                                              │  • section-viewed   │
 *                                              │  • time-spent       │
 *                                              └─────────────────────┘
 *                                                        ↓
 *                              ┌──────────────────────────┼──────────────────────────┐
 *                              ↓                          ↓                          ↓
 *                      [Consumer: DB]           [Consumer: Stream]        [Consumer: ML]
 *                      PostgreSQL store          Real-time metrics         Recommendations
 * 
 * PARTITIONING STRATEGY:
 * - Key: citySlug (ensures all events for a city go to same partition)
 * - Benefit: Ordered processing per city (important for session analysis)
 * - Fallback: "global" key for events without citySlug (round-robin)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONFIGURATION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Enabled via: kafka.enabled=true (disabled by default for local dev)
 * 
 * Thread Safety: This service is thread-safe (KafkaTemplate is thread-safe)
 * Error Handling: Failed publishes are logged but don't throw exceptions
 * 
 * @see AnalyticsEventConsumer
 * @see AnalyticsEventService
 * @see KafkaEventLogger
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AnalyticsEventProducer {
    
    /**
     * KafkaTemplate for publishing events
     * 
     * Spring Boot auto-configures this bean based on application.properties.
     * Key: String (used for partitioning, typically citySlug)
     * Value: AnalyticsEventPayload (serialized to JSON)
     */
    private final KafkaTemplate<String, AnalyticsEventPayload> kafkaTemplate;
    
    /**
     * Structured JSON logger for Kafka pipeline observability
     */
    private final KafkaEventLogger kafkaEventLogger;
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * PUBLISH EVENT - Core Publishing Method
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Publishes an analytics event to the appropriate Kafka topic.
     * 
     * Topic Selection:
     * - Automatically routes to correct topic based on event.eventType
     * - Validates event before publishing
     * - Uses citySlug as partition key (ensures ordered processing per city)
     * 
     * Async Behavior:
     * - Returns immediately without blocking
     * - CompletableFuture resolves when Kafka acknowledges
     * 
     * @param event The analytics event to publish
     * @return CompletableFuture that completes when message is acknowledged
     */
    public CompletableFuture<SendResult<String, AnalyticsEventPayload>> publishEvent(AnalyticsEventPayload event) {
        // Validate event before publishing
        if (event == null || !event.isValid()) {
            log.warn("Attempted to publish invalid analytics event: {}", event);
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Invalid analytics event payload")
            );
        }
        
        // Determine target topic based on event type
        String topic = determineTopicForEvent(event);
        
        // Use citySlug as partition key (null-safe, falls back to round-robin)
        String partitionKey = event.getCitySlug() != null ? event.getCitySlug() : "global";
        
        // Log publishing attempt
        log.debug("Publishing {} event to topic '{}' with key '{}': {}", 
            event.getEventType(), topic, partitionKey, event);
        
        // Publish to Kafka asynchronously
        CompletableFuture<SendResult<String, AnalyticsEventPayload>> future = 
            kafkaTemplate.send(topic, partitionKey, event);
        
        // Add success callback with structured logging
        future.thenAccept(result -> {
            // Log structured event production
            kafkaEventLogger.logEventProduced(
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset(),
                event
            );
            
            log.info("Successfully published {} event to topic '{}', partition {}, offset {}", 
                event.getEventType(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
        });
        
        // Add failure callback
        future.exceptionally(ex -> {
            log.error("Failed to publish {} event to topic '{}' with key '{}': {}", 
                event.getEventType(), topic, partitionKey, event, ex);
            return null;
        });
        
        return future;
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * PUBLISH CITY_SEARCHED Event
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Convenience method for CITY_SEARCHED events.
     * 
     * EVENT SCHEMA:
     * {
     *   "eventType": "CITY_SEARCHED",
     *   "searchQuery": "tech cities california",
     *   "citySlug": "san-francisco",     // null if no city clicked
     *   "sessionId": "sess_abc123",
     *   "timestamp": "2026-01-08T14:30:00"
     * }
     * 
     * @param event The city searched event payload
     * @return CompletableFuture that completes when published
     */
    public CompletableFuture<SendResult<String, AnalyticsEventPayload>> publishCitySearched(AnalyticsEventPayload event) {
        if (!"CITY_SEARCHED".equals(event.getEventType())) {
            log.warn("publishCitySearched called with wrong event type: {}", event.getEventType());
        }
        return publishEvent(event);
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * PUBLISH SECTION_VIEWED Event
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Convenience method for SECTION_VIEWED events.
     * 
     * EVENT SCHEMA:
     * {
     *   "eventType": "SECTION_VIEWED",
     *   "citySlug": "new-york",
     *   "section": "economy",
     *   "sessionId": "sess_abc123",
     *   "timestamp": "2026-01-08T14:31:00"
     * }
     * 
     * @param event The section viewed event payload
     * @return CompletableFuture that completes when published
     */
    public CompletableFuture<SendResult<String, AnalyticsEventPayload>> publishSectionViewed(AnalyticsEventPayload event) {
        if (!"SECTION_VIEWED".equals(event.getEventType())) {
            log.warn("publishSectionViewed called with wrong event type: {}", event.getEventType());
        }
        return publishEvent(event);
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * PUBLISH TIME_SPENT_ON_SECTION Event
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Convenience method for TIME_SPENT_ON_SECTION events.
     * 
     * EVENT SCHEMA:
     * {
     *   "eventType": "TIME_SPENT_ON_SECTION",
     *   "citySlug": "tokyo",
     *   "section": "culture",
     *   "durationInSeconds": 245,
     *   "sessionId": "sess_abc123",
     *   "timestamp": "2026-01-08T14:35:00"
     * }
     * 
     * @param event The time spent event payload
     * @return CompletableFuture that completes when published
     */
    public CompletableFuture<SendResult<String, AnalyticsEventPayload>> publishTimeSpentOnSection(AnalyticsEventPayload event) {
        if (!"TIME_SPENT_ON_SECTION".equals(event.getEventType())) {
            log.warn("publishTimeSpentOnSection called with wrong event type: {}", event.getEventType());
        }
        return publishEvent(event);
    }
    
    /**
     * Determine which Kafka topic to use based on event type
     * 
     * Routing Logic:
     * - CITY_SEARCHED -> cityatlas.analytics.city-searched
     * - SECTION_VIEWED -> cityatlas.analytics.section-viewed
     * - TIME_SPENT_ON_SECTION -> cityatlas.analytics.time-spent-on-section
     * - Unknown types -> throws exception (fail fast)
     * 
     * @param event The analytics event
     * @return The Kafka topic name
     * @throws IllegalArgumentException if event type is unknown
     */
    private String determineTopicForEvent(AnalyticsEventPayload event) {
        return switch (event.getEventType()) {
            case "CITY_SEARCHED" -> KafkaTopics.CITY_SEARCHED;
            case "SECTION_VIEWED" -> KafkaTopics.SECTION_VIEWED;
            case "TIME_SPENT_ON_SECTION" -> KafkaTopics.TIME_SPENT_ON_SECTION;
            default -> {
                log.error("Unknown event type: {}", event.getEventType());
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
            }
        };
    }
    
    /**
     * Health check method to verify Kafka connectivity
     * 
     * Can be called from a health check endpoint to ensure producer is functional.
     * Note: This doesn't actually send a message, just checks if KafkaTemplate is initialized.
     * 
     * @return true if producer is ready
     */
    public boolean isHealthy() {
        return kafkaTemplate != null;
    }
}
