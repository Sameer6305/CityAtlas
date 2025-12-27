package com.cityatlas.backend.service;

import com.cityatlas.backend.config.KafkaTopics;
import com.cityatlas.backend.dto.event.AnalyticsEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Analytics Event Producer Service
 * 
 * Responsible for publishing analytics events to Kafka topics.
 * This service acts as the single entry point for all analytics event publishing,
 * ensuring consistent error handling and logging across the application.
 * 
 * Responsibilities:
 * - Route events to appropriate Kafka topics based on event type
 * - Serialize event payloads to JSON (handled by KafkaTemplate)
 * - Log successful publishes for monitoring and debugging
 * - Log failures with full context for troubleshooting
 * - Provide async publishing with CompletableFuture for non-blocking operations
 * 
 * Thread Safety: This service is thread-safe (KafkaTemplate is thread-safe)
 * Error Handling: Failed publishes are logged but don't throw exceptions (fire-and-forget)
 * 
 * Usage:
 * <pre>
 * analyticsEventProducer.publishEvent(event)
 *     .thenAccept(result -> log.info("Event published successfully"))
 *     .exceptionally(ex -> { log.error("Failed to publish", ex); return null; });
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
     * Publish an analytics event to the appropriate Kafka topic
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
        
        // Add success callback
        future.thenAccept(result -> {
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
     * Publish a city searched event
     * 
     * Convenience method for CITY_SEARCHED events.
     * Validates event type and delegates to publishEvent().
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
     * Publish a section viewed event
     * 
     * Convenience method for SECTION_VIEWED events.
     * Validates event type and delegates to publishEvent().
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
     * Publish a time spent on section event
     * 
     * Convenience method for TIME_SPENT_ON_SECTION events.
     * Validates event type and delegates to publishEvent().
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
