package com.cityatlas.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.cityatlas.backend.dto.event.AnalyticsEventPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * KAFKA EVENT LOGGER - Structured JSON Logging for Analytics Pipeline
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * This component provides structured JSON logging for all Kafka events in CityAtlas.
 * It serves as the observability layer for the event streaming pipeline, making
 * event flow visible for debugging, monitoring, and demonstration purposes.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY KAFKA IS USED IN CITYATLAS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. DECOUPLING: Frontend doesn't wait for analytics processing
 *    - User actions are captured instantly
 *    - Analytics processing happens asynchronously
 *    - UI remains responsive regardless of backend load
 * 
 * 2. RELIABILITY: Events survive system failures
 *    - Kafka persists messages to disk
 *    - Consumer can replay from any offset
 *    - No data loss during deployments or crashes
 * 
 * 3. SCALABILITY: Horizontal scaling via partitioning
 *    - Multiple consumers can process in parallel
 *    - Partitioning by citySlug ensures ordered processing per city
 *    - Can handle millions of events per day
 * 
 * 4. ANALYTICS PIPELINE INTEGRATION:
 *    - Events can be consumed by multiple systems:
 *      • Real-time dashboards (Kafka Streams)
 *      • Data warehouse (Kafka Connect → PostgreSQL/BigQuery)
 *      • ML pipelines (feature extraction for recommendations)
 *      • Alerting systems (anomaly detection)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * HOW THIS SUPPORTS ANALYTICS PIPELINES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * EVENT FLOW:
 * 
 *   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
 *   │   Frontend      │────▶│   REST API      │────▶│   Kafka         │
 *   │   (User Action) │     │   (Producer)    │     │   (Message Bus) │
 *   └─────────────────┘     └─────────────────┘     └────────┬────────┘
 *                                                            │
 *              ┌─────────────────────────────────────────────┼─────────────────────────┐
 *              │                                             │                         │
 *              ▼                                             ▼                         ▼
 *   ┌─────────────────┐                          ┌─────────────────┐      ┌─────────────────┐
 *   │   Consumer      │                          │   Kafka Connect │      │   Kafka Streams │
 *   │   (This App)    │                          │   (Data Sink)   │      │   (Real-time)   │
 *   │   → PostgreSQL  │                          │   → BigQuery    │      │   → Dashboards  │
 *   └─────────────────┘                          └─────────────────┘      └─────────────────┘
 * 
 * ANALYTICS USE CASES:
 * 
 * 1. CITY_SEARCHED Events:
 *    - Track popular search terms
 *    - Identify cities with high demand but no data
 *    - Optimize search ranking
 *    - Feed ML models for search autocomplete
 * 
 * 2. SECTION_VIEWED Events:
 *    - Track which city sections are most viewed
 *    - A/B test section ordering
 *    - Personalize homepage based on user preferences
 *    - Identify content gaps (low-viewed sections)
 * 
 * 3. TIME_SPENT Events:
 *    - Measure content engagement quality
 *    - Identify high-value vs low-value content
 *    - Calculate session metrics
 *    - Feed recommendation engine
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * LOG OUTPUT FORMAT
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * All logs are structured JSON for easy parsing by log aggregators:
 * 
 * {
 *   "eventId": "evt_20260108_143052_abc123",
 *   "stage": "PRODUCED|CONSUMED|PROCESSED|STORED",
 *   "topic": "cityatlas.analytics.city-searched",
 *   "partition": 0,
 *   "offset": 12345,
 *   "timestamp": "2026-01-08T14:30:52.123",
 *   "processingTimeMs": 5,
 *   "event": {
 *     "eventType": "CITY_SEARCHED",
 *     "citySlug": "san-francisco",
 *     "searchQuery": "tech cities",
 *     "sessionId": "sess_abc123",
 *     "timestamp": "2026-01-08T14:30:52.000"
 *   },
 *   "status": "SUCCESS|FAILED",
 *   "error": null
 * }
 * 
 * @see AnalyticsEventProducer
 * @see AnalyticsEventConsumer
 * @see AnalyticsEventService
 */
@Component
@Slf4j
public class KafkaEventLogger {
    
    private static final String LOG_PREFIX = "[KAFKA-PIPELINE]";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final ObjectMapper objectMapper;
    
    public KafkaEventLogger() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Log when an event is produced (sent to Kafka)
     * 
     * Called by: AnalyticsEventProducer after successful send
     * Stage: PRODUCED
     */
    public void logEventProduced(
            String topic, 
            int partition, 
            long offset, 
            AnalyticsEventPayload event) {
        
        KafkaLogEntry logEntry = KafkaLogEntry.builder()
            .eventId(generateEventId(event))
            .stage("PRODUCED")
            .topic(topic)
            .partition(partition)
            .offset(offset)
            .timestamp(LocalDateTime.now())
            .processingTimeMs(0L)
            .eventType(event.getEventType())
            .citySlug(event.getCitySlug())
            .section(event.getSection())
            .sessionId(event.getSessionId())
            .status("SUCCESS")
            .build();
        
        logStructured(logEntry, event);
    }
    
    /**
     * Log when an event is consumed (received from Kafka)
     * 
     * Called by: AnalyticsEventConsumer on message receipt
     * Stage: CONSUMED
     */
    public void logEventConsumed(
            String topic, 
            int partition, 
            long offset, 
            AnalyticsEventPayload event) {
        
        KafkaLogEntry logEntry = KafkaLogEntry.builder()
            .eventId(generateEventId(event))
            .stage("CONSUMED")
            .topic(topic)
            .partition(partition)
            .offset(offset)
            .timestamp(LocalDateTime.now())
            .processingTimeMs(0L)
            .eventType(event.getEventType())
            .citySlug(event.getCitySlug())
            .section(event.getSection())
            .sessionId(event.getSessionId())
            .status("SUCCESS")
            .build();
        
        logStructured(logEntry, event);
    }
    
    /**
     * Log when an event is processed and stored to database
     * 
     * Called by: AnalyticsEventService after database persist
     * Stage: STORED
     */
    public void logEventStored(
            AnalyticsEventPayload event, 
            long processingTimeMs,
            boolean cityResolved) {
        
        KafkaLogEntry logEntry = KafkaLogEntry.builder()
            .eventId(generateEventId(event))
            .stage("STORED")
            .topic(getTopicForEventType(event.getEventType()))
            .partition(-1) // Not available at this stage
            .offset(-1)    // Not available at this stage
            .timestamp(LocalDateTime.now())
            .processingTimeMs(processingTimeMs)
            .eventType(event.getEventType())
            .citySlug(event.getCitySlug())
            .section(event.getSection())
            .sessionId(event.getSessionId())
            .status("SUCCESS")
            .metadata("cityResolved=" + cityResolved)
            .build();
        
        logStructured(logEntry, event);
    }
    
    /**
     * Log when event processing fails
     * 
     * Called by: AnalyticsEventConsumer or AnalyticsEventService on error
     * Stage: FAILED
     */
    public void logEventFailed(
            String topic,
            int partition,
            long offset,
            AnalyticsEventPayload event,
            String stage,
            Throwable error) {
        
        KafkaLogEntry logEntry = KafkaLogEntry.builder()
            .eventId(generateEventId(event))
            .stage(stage + "_FAILED")
            .topic(topic)
            .partition(partition)
            .offset(offset)
            .timestamp(LocalDateTime.now())
            .processingTimeMs(0L)
            .eventType(event != null ? event.getEventType() : "UNKNOWN")
            .citySlug(event != null ? event.getCitySlug() : null)
            .section(event != null ? event.getSection() : null)
            .sessionId(event != null ? event.getSessionId() : null)
            .status("FAILED")
            .error(error.getMessage())
            .build();
        
        logStructuredError(logEntry, event, error);
    }
    
    /**
     * Log pipeline metrics summary (for periodic reporting)
     * 
     * Called by: Scheduled task or health check
     */
    public void logPipelineSummary(
            long eventsProduced,
            long eventsConsumed,
            long eventsStored,
            long eventsFailed,
            long avgProcessingTimeMs) {
        
        log.info("""
            {} ════════════════════════════════════════════════════════════════
            {} KAFKA PIPELINE SUMMARY
            {} ════════════════════════════════════════════════════════════════
            {} Events Produced:     {}
            {} Events Consumed:     {}
            {} Events Stored:       {}
            {} Events Failed:       {}
            {} Avg Processing Time: {} ms
            {} ════════════════════════════════════════════════════════════════
            """,
            LOG_PREFIX, LOG_PREFIX, LOG_PREFIX,
            LOG_PREFIX, eventsProduced,
            LOG_PREFIX, eventsConsumed,
            LOG_PREFIX, eventsStored,
            LOG_PREFIX, eventsFailed,
            LOG_PREFIX, avgProcessingTimeMs,
            LOG_PREFIX);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void logStructured(KafkaLogEntry logEntry, AnalyticsEventPayload event) {
        try {
            String logJson = objectMapper.writeValueAsString(logEntry);
            
            log.info("""
                {} ──────────────────────────────────────────────────────────────
                {} Stage: {} | Event: {} | City: {}
                {} Topic: {} | Partition: {} | Offset: {}
                {} ──────────────────────────────────────────────────────────────
                {} Structured Log:
                {}
                {} ──────────────────────────────────────────────────────────────
                """,
                LOG_PREFIX,
                LOG_PREFIX, logEntry.getStage(), logEntry.getEventType(), logEntry.getCitySlug(),
                LOG_PREFIX, logEntry.getTopic(), logEntry.getPartition(), logEntry.getOffset(),
                LOG_PREFIX,
                LOG_PREFIX, logJson,
                LOG_PREFIX);
                
        } catch (JsonProcessingException e) {
            log.warn("{} Failed to serialize log entry: {}", LOG_PREFIX, e.getMessage());
        }
    }
    
    private void logStructuredError(KafkaLogEntry logEntry, AnalyticsEventPayload event, Throwable error) {
        try {
            String logJson = objectMapper.writeValueAsString(logEntry);
            
            log.error("""
                {} ══════════════════════════════════════════════════════════════
                {} ⚠️  EVENT PROCESSING FAILED
                {} ══════════════════════════════════════════════════════════════
                {} Stage: {} | Event: {} | City: {}
                {} Topic: {} | Partition: {} | Offset: {}
                {} Error: {}
                {} ══════════════════════════════════════════════════════════════
                {} Structured Log:
                {}
                {} ══════════════════════════════════════════════════════════════
                """,
                LOG_PREFIX, LOG_PREFIX, LOG_PREFIX,
                LOG_PREFIX, logEntry.getStage(), logEntry.getEventType(), logEntry.getCitySlug(),
                LOG_PREFIX, logEntry.getTopic(), logEntry.getPartition(), logEntry.getOffset(),
                LOG_PREFIX, error.getMessage(),
                LOG_PREFIX,
                LOG_PREFIX, logJson,
                LOG_PREFIX,
                error);
                
        } catch (JsonProcessingException e) {
            log.error("{} Failed to serialize error log entry: {}", LOG_PREFIX, e.getMessage());
        }
    }
    
    private String generateEventId(AnalyticsEventPayload event) {
        if (event == null) {
            return "evt_" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "_unknown";
        }
        
        String timestamp = event.getTimestamp() != null 
            ? event.getTimestamp().format(TIMESTAMP_FORMAT)
            : LocalDateTime.now().format(TIMESTAMP_FORMAT);
            
        String suffix = event.getSessionId() != null 
            ? event.getSessionId().substring(0, Math.min(6, event.getSessionId().length()))
            : String.valueOf(System.nanoTime() % 100000);
            
        return "evt_" + timestamp + "_" + suffix;
    }
    
    private String getTopicForEventType(String eventType) {
        if (eventType == null) return "unknown";
        
        return switch (eventType) {
            case "CITY_SEARCHED" -> "cityatlas.analytics.city-searched";
            case "SECTION_VIEWED" -> "cityatlas.analytics.section-viewed";
            case "TIME_SPENT_ON_SECTION" -> "cityatlas.analytics.time-spent-on-section";
            default -> "cityatlas.analytics.unknown";
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STRUCTURED LOG ENTRY (for JSON serialization)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class KafkaLogEntry {
        private String eventId;
        private String stage;
        private String topic;
        private int partition;
        private long offset;
        private LocalDateTime timestamp;
        private long processingTimeMs;
        private String eventType;
        private String citySlug;
        private String section;
        private String sessionId;
        private String status;
        private String error;
        private String metadata;
    }
}
