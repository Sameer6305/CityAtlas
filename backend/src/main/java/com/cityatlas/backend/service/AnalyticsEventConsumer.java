package com.cityatlas.backend.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.cityatlas.backend.config.KafkaTopics;
import com.cityatlas.backend.dto.event.AnalyticsEventPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ANALYTICS EVENT CONSUMER - Kafka Message Consumer Service
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Responsible for consuming analytics events from Kafka topics and delegating
 * processing to the appropriate service layer.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY KAFKA FOR EVENT CONSUMPTION?
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. ASYNCHRONOUS PROCESSING:
 *    - User actions don't block on analytics processing
 *    - Events are processed in background with guaranteed delivery
 *    - Frontend remains responsive under high load
 * 
 * 2. CONSUMER GROUP SCALABILITY:
 *    - Multiple instances can consume from same topics
 *    - Kafka balances partitions across consumers
 *    - Easy horizontal scaling for high-volume events
 * 
 * 3. EXACTLY-ONCE SEMANTICS (with proper config):
 *    - No duplicate processing with idempotent consumers
 *    - Offset management ensures no lost events
 *    - Transactional writes to database
 * 
 * 4. REPLAY CAPABILITY:
 *    - Can reprocess historical events by resetting offset
 *    - Useful for fixing bugs or migrating data
 *    - Supports backfilling analytics after schema changes
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * ANALYTICS PIPELINE INTEGRATION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This consumer is part of a larger analytics pipeline:
 * 
 *   [Frontend] → [REST API] → [Kafka Producer] → [Kafka Topics]
 *                                                       ↓
 *                                              ┌────────────────┐
 *                                              │  THIS CONSUMER │
 *                                              │  (CityAtlas)   │
 *                                              └───────┬────────┘
 *                                                      ↓
 *                                              ┌────────────────┐
 *                                              │  PostgreSQL    │
 *                                              │  (analytics_   │
 *                                              │   events table)│
 *                                              └────────────────┘
 * 
 * Future additions can consume from the same topics:
 * - Real-time dashboards (Kafka Streams)
 * - ML feature extraction (Spark Streaming)
 * - Data warehouse sync (Kafka Connect)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONSUMER CONFIGURATION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * - Group ID: cityatlas-analytics-consumer
 * - Auto-commit: Enabled (commits after successful processing)
 * - Concurrency: Can be increased via application.properties
 * - Error Handling: Logs and continues (doesn't halt consumer)
 * 
 * Thread Safety: Each listener method runs in its own thread pool
 * Scaling: Multiple instances can consume from the same topics (consumer group)
 * 
 * @see AnalyticsEventProducer
 * @see AnalyticsEventService
 * @see KafkaEventLogger
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AnalyticsEventConsumer {
    
    /**
     * Service layer that handles the business logic for analytics events
     * This keeps the consumer thin and focused on message consumption only
     */
    private final AnalyticsEventService analyticsEventService;
    
    /**
     * Structured JSON logger for Kafka pipeline observability
     * Logs all events in a format suitable for log aggregators and demos
     */
    private final KafkaEventLogger kafkaEventLogger;
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * CITY_SEARCHED Event Consumer
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Triggered when users search for cities in the application.
     * Records search queries for popularity tracking and search optimization.
     * 
     * EVENT SCHEMA:
     * {
     *   "eventType": "CITY_SEARCHED",
     *   "searchQuery": "tech cities california",
     *   "citySlug": "san-francisco",     // null if no city clicked
     *   "sessionId": "sess_abc123",
     *   "userId": "user_xyz789",         // null for anonymous
     *   "timestamp": "2026-01-08T14:30:00",
     *   "referrer": "google",
     *   "deviceType": "desktop"
     * }
     * 
     * ANALYTICS USE CASES:
     * - Track popular search terms for SEO optimization
     * - Identify cities users want but don't exist in system
     * - Feed search autocomplete ML model
     * - Calculate search-to-view conversion rate
     * 
     * Kafka Configuration:
     * - Topic: cityatlas.analytics.city-searched
     * - Group: cityatlas-analytics-consumer
     * - Auto-offset reset: earliest (process all messages)
     * 
     * @param event The deserialized analytics event payload
     * @param partition The Kafka partition this message came from
     * @param offset The message offset in the partition
     */
    @KafkaListener(
        topics = KafkaTopics.CITY_SEARCHED,
        groupId = "${spring.kafka.consumer.group-id:cityatlas-analytics-consumer}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCitySearched(
            @Payload AnalyticsEventPayload event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        // Log structured event consumption
        kafkaEventLogger.logEventConsumed(KafkaTopics.CITY_SEARCHED, partition, offset, event);
        
        log.info("Received CITY_SEARCHED event from partition {}, offset {}: searchQuery='{}', citySlug='{}'",
            partition, offset, event.getSearchQuery(), event.getCitySlug());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Delegate to service layer for business logic
            analyticsEventService.processCitySearchedEvent(event);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Successfully processed CITY_SEARCHED event at offset {} in {}ms", offset, processingTime);
            
        } catch (Exception ex) {
            // Log structured failure
            kafkaEventLogger.logEventFailed(KafkaTopics.CITY_SEARCHED, partition, offset, event, "PROCESSING", ex);
            
            // Log error but don't rethrow - prevents consumer from stopping
            // Consider implementing dead letter queue (DLQ) for failed messages
            log.error("Failed to process CITY_SEARCHED event at partition {}, offset {}: {}",
                partition, offset, event, ex);
        }
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * SECTION_VIEWED Event Consumer
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Triggered when users navigate to specific city sections.
     * Tracks which sections are most popular for content optimization.
     * 
     * EVENT SCHEMA:
     * {
     *   "eventType": "SECTION_VIEWED",
     *   "citySlug": "new-york",
     *   "section": "economy",            // economy, culture, environment, etc.
     *   "sessionId": "sess_abc123",
     *   "userId": "user_xyz789",
     *   "timestamp": "2026-01-08T14:31:00",
     *   "referrer": "internal",
     *   "deviceType": "mobile"
     * }
     * 
     * ANALYTICS USE CASES:
     * - Track most popular sections per city
     * - Optimize section ordering based on engagement
     * - Personalize homepage section recommendations
     * - A/B test section layouts
     * 
     * Kafka Configuration:
     * - Topic: cityatlas.analytics.section-viewed
     * - Group: cityatlas-analytics-consumer
     * - Partition ordering: Maintained per citySlug (used as partition key)
     * 
     * @param event The deserialized analytics event payload
     * @param partition The Kafka partition this message came from
     * @param offset The message offset in the partition
     */
    @KafkaListener(
        topics = KafkaTopics.SECTION_VIEWED,
        groupId = "${spring.kafka.consumer.group-id:cityatlas-analytics-consumer}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSectionViewed(
            @Payload AnalyticsEventPayload event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        // Log structured event consumption
        kafkaEventLogger.logEventConsumed(KafkaTopics.SECTION_VIEWED, partition, offset, event);
        
        log.info("Received SECTION_VIEWED event from partition {}, offset {}: citySlug='{}', section='{}'",
            partition, offset, event.getCitySlug(), event.getSection());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Delegate to service layer for business logic
            analyticsEventService.processSectionViewedEvent(event);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Successfully processed SECTION_VIEWED event at offset {} in {}ms", offset, processingTime);
            
        } catch (Exception ex) {
            // Log structured failure
            kafkaEventLogger.logEventFailed(KafkaTopics.SECTION_VIEWED, partition, offset, event, "PROCESSING", ex);
            
            log.error("Failed to process SECTION_VIEWED event at partition {}, offset {}: {}",
                partition, offset, event, ex);
        }
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * TIME_SPENT_ON_SECTION Event Consumer
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Triggered when users leave a section, recording engagement duration.
     * Critical for measuring content quality and user engagement metrics.
     * 
     * EVENT SCHEMA:
     * {
     *   "eventType": "TIME_SPENT_ON_SECTION",
     *   "citySlug": "tokyo",
     *   "section": "culture",
     *   "durationInSeconds": 245,        // Time spent on section
     *   "sessionId": "sess_abc123",
     *   "userId": "user_xyz789",
     *   "timestamp": "2026-01-08T14:35:00",
     *   "deviceType": "desktop"
     * }
     * 
     * ANALYTICS USE CASES:
     * - Measure content engagement quality (avg time per section)
     * - Identify high-value content (long read times)
     * - Detect content issues (very short times = bounce)
     * - Calculate session depth and engagement scores
     * - Feed recommendation engine with engagement signals
     * 
     * ENGAGEMENT BENCHMARKS:
     * - < 10 seconds: Bounce (potential content issue)
     * - 10-60 seconds: Quick scan
     * - 60-180 seconds: Normal engagement
     * - > 180 seconds: Deep engagement (high-value content)
     * 
     * Kafka Configuration:
     * - Topic: cityatlas.analytics.time-spent-on-section
     * - Group: cityatlas-analytics-consumer
     * - Retention: 90 days (longer than other analytics topics)
     * 
     * @param event The deserialized analytics event payload
     * @param partition The Kafka partition this message came from
     * @param offset The message offset in the partition
     */
    @KafkaListener(
        topics = KafkaTopics.TIME_SPENT_ON_SECTION,
        groupId = "${spring.kafka.consumer.group-id:cityatlas-analytics-consumer}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTimeSpentOnSection(
            @Payload AnalyticsEventPayload event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        // Log structured event consumption
        kafkaEventLogger.logEventConsumed(KafkaTopics.TIME_SPENT_ON_SECTION, partition, offset, event);
        
        log.info("Received TIME_SPENT_ON_SECTION event from partition {}, offset {}: citySlug='{}', section='{}', duration={}s",
            partition, offset, event.getCitySlug(), event.getSection(), event.getDurationInSeconds());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Delegate to service layer for business logic
            analyticsEventService.processTimeSpentOnSectionEvent(event);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Successfully processed TIME_SPENT_ON_SECTION event at offset {} in {}ms", offset, processingTime);
            
        } catch (Exception ex) {
            // Log structured failure
            kafkaEventLogger.logEventFailed(KafkaTopics.TIME_SPENT_ON_SECTION, partition, offset, event, "PROCESSING", ex);
            
            log.error("Failed to process TIME_SPENT_ON_SECTION event at partition {}, offset {}: {}",
                partition, offset, event, ex);
        }
    }
    
    /**
     * Health check method to verify consumer is running
     * 
     * Can be used by monitoring systems to check if the consumer is functional.
     * Note: This only checks if the service is instantiated, not if Kafka is reachable.
     * 
     * @return true if consumer service is ready
     */
    public boolean isHealthy() {
        return analyticsEventService != null;
    }
}
