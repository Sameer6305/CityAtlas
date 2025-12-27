package com.cityatlas.backend.service;

import com.cityatlas.backend.config.KafkaTopics;
import com.cityatlas.backend.dto.event.AnalyticsEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Analytics Event Consumer Service
 * 
 * Responsible for consuming analytics events from Kafka topics and delegating
 * processing to the appropriate service layer.
 * 
 * Responsibilities:
 * - Listen to Kafka topics for incoming analytics events
 * - Deserialize JSON payloads to AnalyticsEventPayload objects
 * - Log received events for monitoring and debugging
 * - Delegate business logic to AnalyticsEventService
 * - Handle consumer errors gracefully (with logging)
 * 
 * NOT Responsible For:
 * - Business logic (that's in AnalyticsEventService)
 * - Data validation (events are already validated by producer)
 * - Database operations (delegated to service layer)
 * 
 * Consumer Configuration:
 * - Group ID: cityatlas-analytics-consumer
 * - Auto-commit: Enabled (commits after successful processing)
 * - Concurrency: Can be increased via application.properties
 * - Error Handling: Logs and continues (doesn't halt consumer)
 * 
 * Thread Safety: Each listener method runs in its own thread pool
 * Scaling: Multiple instances can consume from the same topics (consumer group)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventConsumer {
    
    /**
     * Service layer that handles the business logic for analytics events
     * This keeps the consumer thin and focused on message consumption only
     */
    private final AnalyticsEventService analyticsEventService;
    
    /**
     * Consume CITY_SEARCHED events
     * 
     * Triggered when users search for cities in the application.
     * Records search queries for popularity tracking and search optimization.
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
        
        log.info("Received CITY_SEARCHED event from partition {}, offset {}: searchQuery='{}', citySlug='{}'",
            partition, offset, event.getSearchQuery(), event.getCitySlug());
        
        try {
            // Delegate to service layer for business logic
            analyticsEventService.processCitySearchedEvent(event);
            
            log.debug("Successfully processed CITY_SEARCHED event at offset {}", offset);
            
        } catch (Exception ex) {
            // Log error but don't rethrow - prevents consumer from stopping
            // Consider implementing dead letter queue (DLQ) for failed messages
            log.error("Failed to process CITY_SEARCHED event at partition {}, offset {}: {}",
                partition, offset, event, ex);
        }
    }
    
    /**
     * Consume SECTION_VIEWED events
     * 
     * Triggered when users navigate to specific city sections.
     * Tracks which sections are most popular for content optimization.
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
        
        log.info("Received SECTION_VIEWED event from partition {}, offset {}: citySlug='{}', section='{}'",
            partition, offset, event.getCitySlug(), event.getSection());
        
        try {
            // Delegate to service layer for business logic
            analyticsEventService.processSectionViewedEvent(event);
            
            log.debug("Successfully processed SECTION_VIEWED event at offset {}", offset);
            
        } catch (Exception ex) {
            log.error("Failed to process SECTION_VIEWED event at partition {}, offset {}: {}",
                partition, offset, event, ex);
        }
    }
    
    /**
     * Consume TIME_SPENT_ON_SECTION events
     * 
     * Triggered when users leave a section, recording engagement duration.
     * Critical for measuring content quality and user engagement metrics.
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
        
        log.info("Received TIME_SPENT_ON_SECTION event from partition {}, offset {}: citySlug='{}', section='{}', duration={}s",
            partition, offset, event.getCitySlug(), event.getSection(), event.getDurationInSeconds());
        
        try {
            // Delegate to service layer for business logic
            analyticsEventService.processTimeSpentOnSectionEvent(event);
            
            log.debug("Successfully processed TIME_SPENT_ON_SECTION event at offset {}", offset);
            
        } catch (Exception ex) {
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
