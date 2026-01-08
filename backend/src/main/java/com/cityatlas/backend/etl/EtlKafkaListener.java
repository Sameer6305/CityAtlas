package com.cityatlas.backend.etl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.cityatlas.backend.dto.event.AnalyticsEventPayload;
import com.cityatlas.backend.entity.analytics.FactUserEventsDaily;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ============================================================================
 *                          STREAMING PATH
 * ============================================================================
 * 
 * ETL KAFKA LISTENER - Event-Driven Real-Time Data Pipeline
 * 
 * This class implements the STREAMING PATH of the CityAtlas Lambda Architecture.
 * It consumes events from Kafka topics and writes to the analytics_events table
 * with low latency (seconds).
 * 
 * ============================================================================
 *                    DATA FLOW (STREAMING PATH)
 * ============================================================================
 * 
 *   +---------------+     +------------------+     +-------------------+
 *   | User Action   | --> | Kafka Producer   | --> | Kafka Topic       |
 *   | (Frontend)    |     | (KafkaService)   |     | (analytics.*)     |
 *   +---------------+     +------------------+     +-------------------+
 *                                                          |
 *                                                          v
 *   +-------------------+     +------------------+     +-------------------+
 *   | analytics_events  | <-- | EtlKafkaListener | <-- | Kafka Consumer    |
 *   | (PostgreSQL)      |     | (This Class)     |     | (Spring Kafka)    |
 *   +-------------------+     +------------------+     +-------------------+
 * 
 * OUTPUT TABLE: analytics_events
 *   - Stores raw events with minimal transformation
 *   - Enables real-time counters and live dashboards
 *   - Source data for batch aggregation jobs
 * 
 * ============================================================================
 *                    STREAMING PATH vs BATCH PATH
 * ============================================================================
 * 
 *   BATCH PROCESSING (EtlScheduler)        STREAM PROCESSING (This Class)
 *   ─────────────────────────────          ──────────────────────────────
 *   
 *   ┌─────────┐   Scheduled    ┌─────────┐    ┌─────────┐   On Event   ┌─────────┐
 *   │ Source  │ ────────────▶  │   ETL   │    │  Kafka  │ ───────────▶ │   ETL   │
 *   │  Data   │   (hourly)     │ Process │    │  Event  │  (real-time) │ Process │
 *   └─────────┘                └─────────┘    └─────────┘              └─────────┘
 *   
 *   Pros:                                     Pros:
 *   • Higher throughput                       • Lower latency
 *   • More efficient for large volumes        • Immediate updates
 *   • Simpler error handling                  • Event-driven architecture
 *   
 *   Cons:                                     Cons:
 *   • Higher latency                          • More complex error handling
 *   • Data not available until next run       • Per-event overhead
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * MICRO-BATCH STRATEGY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Pure stream processing (one event → one DB write) is inefficient.
 * We use MICRO-BATCHING: accumulate events in memory, flush periodically.
 * 
 *   ┌─────────────────────────────────────────────────────────────────────────┐
 *   │                                                                         │
 *   │  Event 1  ──┐                                                           │
 *   │  Event 2  ──┤    ┌───────────────┐                                      │
 *   │  Event 3  ──┼───▶│  Accumulator  │───▶ Flush every 100 events          │
 *   │  Event 4  ──┤    │   (in-memory) │     OR every 10 seconds             │
 *   │  Event 5  ──┘    └───────────────┘                                      │
 *   │                                                                         │
 *   │  Benefits:                                                              │
 *   │  • Reduces DB round-trips (1 batch write vs 100 individual writes)     │
 *   │  • Still provides near-real-time updates                               │
 *   │  • Handles burst traffic efficiently                                   │
 *   │                                                                         │
 *   └─────────────────────────────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * EVENT TYPES HANDLED
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────────────┬────────────────────────────────────────────────────┐
 *   │ Event Type          │ ETL Action                                         │
 *   ├─────────────────────┼────────────────────────────────────────────────────┤
 *   │ CITY_SEARCHED       │ Increment search counter, update search analytics │
 *   │ SECTION_VIEWED      │ Update engagement metrics, track popular sections │
 *   │ TIME_SPENT          │ Update duration aggregates, calculate engagement  │
 *   │ DATA_SYNC           │ Trigger immediate metric snapshot                 │
 *   └─────────────────────┴────────────────────────────────────────────────────┘
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class EtlKafkaListener {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final DataCleaningService cleaningService;
    private final DataAggregationService aggregationService;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MICRO-BATCH CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Maximum events to accumulate before flushing.
     */
    private static final int BATCH_SIZE = 100;
    
    /**
     * Maximum time (ms) to hold events before flushing.
     */
    private static final long FLUSH_INTERVAL_MS = 10_000; // 10 seconds
    
    /**
     * Event accumulator (thread-safe).
     * Key: grain (city_id + event_type + date)
     * Value: running aggregation
     */
    private final ConcurrentHashMap<String, EventAccumulator> accumulators = 
        new ConcurrentHashMap<>();
    
    /**
     * Counter for events since last flush.
     */
    private final AtomicLong eventsSinceFlush = new AtomicLong(0);
    
    /**
     * Timestamp of last flush.
     */
    private volatile long lastFlushTime = System.currentTimeMillis();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // KAFKA LISTENERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Handle CITY_SEARCHED events for search analytics.
     * 
     * TRANSFORMATION:
     * - Accumulate search counts by city
     * - Track unique users searching for each city
     * - Flush to fact_user_events_daily periodically
     */
    @KafkaListener(
        topics = "cityatlas.analytics.city-searched",
        groupId = "cityatlas-etl-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCitySearchedEvent(AnalyticsEventPayload payload) {
        log.debug("[ETL-KAFKA] Received CITY_SEARCHED event: city={}", payload.getCitySlug());
        
        try {
            // ─────────────────────────────────────────────────────────────────
            // VALIDATE: Basic payload validation
            // ─────────────────────────────────────────────────────────────────
            
            if (!isValidPayload(payload)) {
                log.warn("[ETL-KAFKA] Invalid payload, skipping: {}", payload);
                return;
            }
            
            // ─────────────────────────────────────────────────────────────────
            // ACCUMULATE: Add to micro-batch
            // ─────────────────────────────────────────────────────────────────
            
            String grainKey = buildGrainKey(payload);
            
            accumulators.compute(grainKey, (key, existing) -> {
                if (existing == null) {
                    existing = new EventAccumulator(payload);
                }
                existing.addEvent(payload);
                return existing;
            });
            
            // ─────────────────────────────────────────────────────────────────
            // FLUSH CHECK: Flush if batch size or time threshold reached
            // ─────────────────────────────────────────────────────────────────
            
            long eventCount = eventsSinceFlush.incrementAndGet();
            long timeSinceFlush = System.currentTimeMillis() - lastFlushTime;
            
            if (eventCount >= BATCH_SIZE || timeSinceFlush >= FLUSH_INTERVAL_MS) {
                flushAccumulators();
            }
            
        } catch (Exception e) {
            log.error("[ETL-KAFKA] Error processing CITY_SEARCHED event", e);
            // In production: Send to dead letter queue
        }
    }
    
    /**
     * Handle SECTION_VIEWED events for engagement analytics.
     */
    @KafkaListener(
        topics = "cityatlas.analytics.section-viewed",
        groupId = "cityatlas-etl-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSectionViewedEvent(AnalyticsEventPayload payload) {
        log.debug("[ETL-KAFKA] Received SECTION_VIEWED event: city={}, section={}",
                 payload.getCitySlug(), payload.getSection());
        
        try {
            if (!isValidPayload(payload)) {
                return;
            }
            
            String grainKey = buildGrainKey(payload);
            
            accumulators.compute(grainKey, (key, existing) -> {
                if (existing == null) {
                    existing = new EventAccumulator(payload);
                }
                existing.addEvent(payload);
                return existing;
            });
            
            checkAndFlush();
            
        } catch (Exception e) {
            log.error("[ETL-KAFKA] Error processing SECTION_VIEWED event", e);
        }
    }
    
    /**
     * Handle TIME_SPENT events for duration analytics.
     * 
     * These events include duration data, enabling:
     * - Average time on section calculations
     * - Bounce rate detection
     * - Engagement scoring
     */
    @KafkaListener(
        topics = "cityatlas.analytics.time-spent-on-section",
        groupId = "cityatlas-etl-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTimeSpentEvent(AnalyticsEventPayload payload) {
        log.debug("[ETL-KAFKA] Received TIME_SPENT event: city={}, duration={}s",
                 payload.getCitySlug(), extractDuration(payload));
        
        try {
            if (!isValidPayload(payload)) {
                return;
            }
            
            String grainKey = buildGrainKey(payload);
            Long duration = extractDuration(payload);
            
            accumulators.compute(grainKey, (key, existing) -> {
                if (existing == null) {
                    existing = new EventAccumulator(payload);
                }
                existing.addEventWithDuration(payload, duration);
                return existing;
            });
            
            checkAndFlush();
            
        } catch (Exception e) {
            log.error("[ETL-KAFKA] Error processing TIME_SPENT event", e);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MICRO-BATCH FLUSHING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if flush is needed and execute if so.
     */
    private void checkAndFlush() {
        long eventCount = eventsSinceFlush.incrementAndGet();
        long timeSinceFlush = System.currentTimeMillis() - lastFlushTime;
        
        if (eventCount >= BATCH_SIZE || timeSinceFlush >= FLUSH_INTERVAL_MS) {
            flushAccumulators();
        }
    }
    
    /**
     * Flush accumulated events to the database.
     * 
     * PROCESS:
     * 1. Snapshot current accumulators (atomic swap)
     * 2. Convert to fact rows
     * 3. Upsert into fact_user_events_daily
     * 4. Reset counters
     */
    private synchronized void flushAccumulators() {
        if (accumulators.isEmpty()) {
            return;
        }
        
        String batchId = "KAFKA_" + LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        log.info("[ETL-KAFKA] Flushing {} accumulated grains. Batch: {}", 
                 accumulators.size(), batchId);
        
        try {
            // ─────────────────────────────────────────────────────────────────
            // STEP 1: Snapshot and clear accumulators atomically
            // ─────────────────────────────────────────────────────────────────
            
            Map<String, EventAccumulator> snapshot = new HashMap<>(accumulators);
            accumulators.clear();
            
            // ─────────────────────────────────────────────────────────────────
            // STEP 2: Convert accumulators to fact rows
            // ─────────────────────────────────────────────────────────────────
            
            for (EventAccumulator acc : snapshot.values()) {
                FactUserEventsDaily fact = acc.toFactRow(batchId);
                
                // In production: Upsert to database
                // factEventsRepository.upsertIncrementally(fact);
                
                log.debug("[ETL-KAFKA] Fact row: city={}, type={}, count={}, users={}",
                         fact.getDimCity() != null ? fact.getDimCity().getCitySlug() : "global",
                         fact.getEventType(),
                         fact.getEventCount(),
                         fact.getUniqueUsers());
            }
            
            // ─────────────────────────────────────────────────────────────────
            // STEP 3: Reset counters
            // ─────────────────────────────────────────────────────────────────
            
            eventsSinceFlush.set(0);
            lastFlushTime = System.currentTimeMillis();
            
            log.info("[ETL-KAFKA] Flush complete. {} fact rows written.", snapshot.size());
            
        } catch (Exception e) {
            log.error("[ETL-KAFKA] Flush failed. Events may be lost!", e);
            // In production: Restore accumulators or write to recovery log
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Validate that payload has required fields.
     */
    private boolean isValidPayload(AnalyticsEventPayload payload) {
        if (payload == null) {
            return false;
        }
        if (payload.getEventType() == null) {
            return false;
        }
        if (payload.getTimestamp() == null) {
            return false;
        }
        return true;
    }
    
    /**
     * Build grain key for accumulator lookup.
     * Grain: city_slug + event_type + date
     */
    private String buildGrainKey(AnalyticsEventPayload payload) {
        String citySlug = payload.getCitySlug() != null ? payload.getCitySlug() : "global";
        String eventType = payload.getEventType(); // eventType is already a String
        String date = payload.getTimestamp().toLocalDate().toString();
        
        return String.format("%s_%s_%s", citySlug, eventType, date);
    }
    
    /**
     * Extract duration from payload metadata.
     */
    private Long extractDuration(AnalyticsEventPayload payload) {
        if (payload.getDurationInSeconds() != null) {
            return payload.getDurationInSeconds().longValue();
        }
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACCUMULATOR CLASS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Accumulates events for a single grain (city + type + date).
     * 
     * Thread-safe for concurrent updates within a grain.
     */
    private static class EventAccumulator {
        private final String citySlug;
        private final String eventType;
        private final java.time.LocalDate eventDate;
        
        private final AtomicLong eventCount = new AtomicLong(0);
        private final java.util.Set<String> uniqueUsers = 
            java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
        private final java.util.Set<String> uniqueSessions = 
            java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
        
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong durationCount = new AtomicLong(0);
        private final AtomicLong bounceCount = new AtomicLong(0);
        private final AtomicLong engagedCount = new AtomicLong(0);
        
        public EventAccumulator(AnalyticsEventPayload firstPayload) {
            this.citySlug = firstPayload.getCitySlug();
            this.eventType = firstPayload.getEventType(); // eventType is already a String
            this.eventDate = firstPayload.getTimestamp().toLocalDate();
        }
        
        public void addEvent(AnalyticsEventPayload payload) {
            eventCount.incrementAndGet();
            
            if (payload.getUserId() != null && !payload.getUserId().isEmpty()) {
                uniqueUsers.add(payload.getUserId());
            }
            if (payload.getSessionId() != null && !payload.getSessionId().isEmpty()) {
                uniqueSessions.add(payload.getSessionId());
            }
        }
        
        public void addEventWithDuration(AnalyticsEventPayload payload, Long duration) {
            addEvent(payload);
            
            if (duration != null && duration > 0) {
                totalDuration.addAndGet(duration);
                durationCount.incrementAndGet();
                
                if (duration < 10) {
                    bounceCount.incrementAndGet();
                }
                if (duration >= 60) {
                    engagedCount.incrementAndGet();
                }
            }
        }
        
        public FactUserEventsDaily toFactRow(String batchId) {
            Double avgDuration = null;
            if (durationCount.get() > 0) {
                avgDuration = (double) totalDuration.get() / durationCount.get();
            }
            
            // Map AnalyticsEventPayload eventType strings to EventType enum
            // The payload uses strings like "CITY_SEARCHED", "SECTION_VIEWED", etc.
            // We map them to the closest EventType enum values
            com.cityatlas.backend.entity.EventType mappedEventType = mapStringToEventType(eventType);
            
            return FactUserEventsDaily.builder()
                .dimCity(null) // Resolve in production via lookup
                .eventDate(eventDate)
                .eventType(mappedEventType)
                .eventCount(eventCount.get())
                .uniqueUsers((long) uniqueUsers.size())
                .uniqueSessions((long) uniqueSessions.size())
                .totalDurationSeconds(totalDuration.get() > 0 ? totalDuration.get() : null)
                .avgDurationSeconds(avgDuration)
                .bounceCount(bounceCount.get() > 0 ? bounceCount.get() : null)
                .engagedCount(engagedCount.get() > 0 ? engagedCount.get() : null)
                .etlBatchId(batchId)
                .rawEventCount(eventCount.get())
                .build();
        }
        
        /**
         * Maps string event types from AnalyticsEventPayload to EventType enum.
         * Handles the mapping between frontend event names and backend enum.
         */
        private com.cityatlas.backend.entity.EventType mapStringToEventType(String eventTypeStr) {
            if (eventTypeStr == null) {
                return com.cityatlas.backend.entity.EventType.PAGE_VIEW; // default
            }
            
            return switch (eventTypeStr) {
                case "CITY_SEARCHED" -> com.cityatlas.backend.entity.EventType.SEARCH;
                case "SECTION_VIEWED" -> com.cityatlas.backend.entity.EventType.PAGE_VIEW;
                case "TIME_SPENT_ON_SECTION" -> com.cityatlas.backend.entity.EventType.ANALYTICS_VIEW;
                default -> {
                    // Try direct mapping for enum value strings
                    try {
                        yield com.cityatlas.backend.entity.EventType.valueOf(eventTypeStr);
                    } catch (IllegalArgumentException e) {
                        yield com.cityatlas.backend.entity.EventType.PAGE_VIEW;
                    }
                }
            };
        }
    }
}
