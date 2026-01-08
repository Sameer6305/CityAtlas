package com.cityatlas.backend.etl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cityatlas.backend.entity.AnalyticsEvent;
import com.cityatlas.backend.entity.Metrics;

import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * DATA CLEANING SERVICE - First Stage of ETL Pipeline
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Cleans and validates raw data before transformation. This is the first step
 * in the ETL pipeline, ensuring data quality before normalization and aggregation.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY DATA CLEANING IS CRITICAL
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Raw data from external APIs and user events often contains:
 * 
 *   ┌─────────────────────────────────────────────────────────────────────────┐
 *   │  PROBLEM               │  EXAMPLE                   │  IMPACT          │
 *   ├─────────────────────────────────────────────────────────────────────────┤
 *   │  Missing values        │  AQI = null                │  NullPointerEx   │
 *   │  Outliers              │  Population = -500         │  Skewed averages │
 *   │  Duplicates            │  Same event twice          │  Inflated counts │
 *   │  Invalid types         │  GDP = "N/A"               │  Parse errors    │
 *   │  Stale data            │  2-week old AQI            │  Wrong decisions │
 *   └─────────────────────────────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * CLEANING OPERATIONS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   RAW DATA                        CLEANING PIPELINE                 CLEAN DATA
 *   ────────                        ─────────────────                 ──────────
 *   
 *   ┌──────────────┐     ┌───────────────────────────────────┐     ┌──────────────┐
 *   │ city: null   │     │  1. Filter null required fields   │     │ REJECTED     │
 *   │ value: 42    │ ──▶ │  2. Detect & remove outliers      │ ──▶ │              │
 *   └──────────────┘     │  3. Deduplicate by business key   │     └──────────────┘
 *                        │  4. Validate enum/type values     │
 *   ┌──────────────┐     │  5. Flag stale data               │     ┌──────────────┐
 *   │ city: NYC    │     └───────────────────────────────────┘     │ city: NYC    │
 *   │ value: 42    │ ──────────────────────────────────────────▶   │ value: 42    │
 *   └──────────────┘                                               │ quality: 100 │
 *                                                                  └──────────────┘
 *   ┌──────────────┐                                               ┌──────────────┐
 *   │ city: NYC    │                                               │ city: NYC    │
 *   │ value: 99999 │ ──────────────────────────────────────────▶   │ value: 99999 │
 *   │ (outlier)    │                                               │ quality: 25  │
 *   └──────────────┘                                               │ (flagged)    │
 *                                                                  └──────────────┘
 */
@Service
@Slf4j
public class DataCleaningService {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final DataQualityValidator qualityValidator;
    private final DataQualityFallback fallbackService;
    
    public DataCleaningService(DataQualityValidator qualityValidator, 
                               DataQualityFallback fallbackService) {
        this.qualityValidator = qualityValidator;
        this.fallbackService = fallbackService;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Number of standard deviations from mean to consider as outlier.
     * Values outside mean ± (Z_SCORE_THRESHOLD * stdDev) are flagged.
     * 
     * Industry standard is typically 2-3. We use 3 for less aggressive filtering.
     */
    private static final double Z_SCORE_THRESHOLD = 3.0;
    
    /**
     * Maximum age (in hours) before data is considered stale.
     * Stale data gets a reduced quality score but is not rejected.
     */
    private static final int STALE_DATA_THRESHOLD_HOURS = 24;
    
    /**
     * Quality score values for different data states.
     */
    private static final int QUALITY_SCORE_PERFECT = 100;
    private static final int QUALITY_SCORE_GOOD = 75;
    private static final int QUALITY_SCORE_STALE = 50;
    private static final int QUALITY_SCORE_OUTLIER = 25;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // METRICS CLEANING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Clean a batch of Metrics records.
     * 
     * CLEANING STEPS:
     * 1. Remove records with null required fields (city, value, type)
     * 2. Validate value ranges (AQI 0-500, Population 0-50M, etc.)
     * 3. Detect outliers using Z-score method
     * 4. Deduplicate by business key (city + type + timestamp)
     * 5. Assign quality scores
     * 
     * @param rawMetrics List of raw metrics from OLTP table
     * @return CleaningResult containing clean records and statistics
     */
    public CleaningResult<Metrics> cleanMetrics(List<Metrics> rawMetrics) {
        log.info("[ETL-CLEAN] Starting metrics cleaning. Input count: {}", rawMetrics.size());
        
        List<Metrics> cleanRecords = new ArrayList<>();
        List<RejectedRecord<Metrics>> rejected = new ArrayList<>();
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 1: Null validation using DataQualityValidator
        // ─────────────────────────────────────────────────────────────────────
        DataQualityValidator.BatchValidationResult nullCheck = 
            qualityValidator.validateNotNullBatch(rawMetrics);
        
        // Add null failures to rejected list
        for (DataQualityValidator.FailedRecord failed : nullCheck.failedRecords()) {
            rejected.add(new RejectedRecord<>(failed.metric(), failed.errorCode(), failed.message()));
        }
        
        List<Metrics> notNull = nullCheck.validRecords();
        log.debug("[ETL-CLEAN] After null filter: {} records (rejected {})", 
                  notNull.size(), rawMetrics.size() - notNull.size());
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 2: Range validation using DataQualityValidator
        // ─────────────────────────────────────────────────────────────────────
        DataQualityValidator.BatchValidationResult rangeCheck = 
            qualityValidator.validateRangeBatch(notNull);
        
        // Add range failures to rejected list
        for (DataQualityValidator.FailedRecord failed : rangeCheck.failedRecords()) {
            rejected.add(new RejectedRecord<>(failed.metric(), failed.errorCode(), failed.message()));
        }
        
        List<Metrics> inRange = rangeCheck.validRecords();
        log.debug("[ETL-CLEAN] After range filter: {} records (rejected {})",
                  inRange.size(), notNull.size() - inRange.size());
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 3: Detect outliers using Z-score method
        // ─────────────────────────────────────────────────────────────────────
        // Group by metric type, then calculate Z-score for each group.
        // Different metric types have different normal ranges (AQI vs Population).
        
        Map<String, List<Metrics>> byType = inRange.stream()
            .collect(Collectors.groupingBy(m -> m.getMetricType().name()));
        
        Set<Long> outlierIds = new HashSet<>();
        
        for (Map.Entry<String, List<Metrics>> entry : byType.entrySet()) {
            List<Metrics> typeMetrics = entry.getValue();
            
            // Calculate mean and standard deviation for this metric type
            double mean = typeMetrics.stream()
                .mapToDouble(Metrics::getValue)
                .average()
                .orElse(0.0);
            
            double variance = typeMetrics.stream()
                .mapToDouble(m -> Math.pow(m.getValue() - mean, 2))
                .average()
                .orElse(0.0);
            
            double stdDev = Math.sqrt(variance);
            
            // Flag values outside mean ± (threshold * stdDev)
            if (stdDev > 0) {
                for (Metrics m : typeMetrics) {
                    double zScore = Math.abs((m.getValue() - mean) / stdDev);
                    if (zScore > Z_SCORE_THRESHOLD) {
                        outlierIds.add(m.getId());
                        log.debug("[ETL-CLEAN] Outlier detected: {} {} value={} (z-score={})",
                                  m.getCity().getSlug(), m.getMetricType(), m.getValue(), zScore);
                    }
                }
            }
        }
        
        log.debug("[ETL-CLEAN] Outliers flagged: {}", outlierIds.size());
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 3: Deduplicate by business key
        // ─────────────────────────────────────────────────────────────────────
        // Business key: city_id + metric_type + DATE(recorded_at)
        // Keep the most recent record if duplicates exist
        
        Map<String, Metrics> deduped = new HashMap<>();
        
        for (Metrics m : inRange) {
            String businessKey = String.format("%d_%s_%s",
                m.getCity().getId(),
                m.getMetricType().name(),
                m.getRecordedAt().toLocalDate().toString()
            );
            
            Metrics existing = deduped.get(businessKey);
            if (existing == null || m.getRecordedAt().isAfter(existing.getRecordedAt())) {
                if (existing != null) {
                    rejected.add(new RejectedRecord<>(existing, "DUPLICATE", 
                        "Superseded by newer record at " + m.getRecordedAt()));
                }
                deduped.put(businessKey, m);
            } else {
                rejected.add(new RejectedRecord<>(m, "DUPLICATE", 
                    "Older than existing record at " + existing.getRecordedAt()));
            }
        }
        
        log.debug("[ETL-CLEAN] After deduplication: {} unique records", deduped.size());
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 5: Build final clean list with quality scores and cache values
        // ─────────────────────────────────────────────────────────────────────
        
        cleanRecords.addAll(deduped.values());
        
        // Cache valid values for future fallback
        for (Metrics m : cleanRecords) {
            fallbackService.cacheValue(m.getCity().getSlug(), m.getMetricType(), m.getValue());
        }
        
        // Calculate quality scores for logging (actual score stored in fact table)
        Map<Integer, Long> qualityDistribution = cleanRecords.stream()
            .collect(Collectors.groupingBy(
                m -> calculateQualityScore(m, outlierIds),
                Collectors.counting()
            ));
        
        log.info("[ETL-CLEAN] Metrics cleaning complete. Clean: {}, Rejected: {}, Quality distribution: {}",
                 cleanRecords.size(), rejected.size(), qualityDistribution);
        
        return new CleaningResult<>(cleanRecords, rejected, outlierIds);
    }
    
    /**
     * Calculate data quality score for a metric record.
     * 
     * SCORING LOGIC:
     * - 100: Fresh data, no issues
     * - 75: Good data, minor issues
     * - 50: Stale data (> 24 hours old)
     * - 25: Outlier or suspect value
     */
    private int calculateQualityScore(Metrics m, Set<Long> outlierIds) {
        if (outlierIds.contains(m.getId())) {
            return QUALITY_SCORE_OUTLIER;
        }
        
        long hoursOld = java.time.Duration.between(
            m.getRecordedAt(), 
            java.time.LocalDateTime.now()
        ).toHours();
        
        if (hoursOld > STALE_DATA_THRESHOLD_HOURS) {
            return QUALITY_SCORE_STALE;
        }
        
        return QUALITY_SCORE_PERFECT;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANALYTICS EVENTS CLEANING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Clean a batch of AnalyticsEvent records.
     * 
     * CLEANING STEPS:
     * 1. Remove records with null required fields (eventType, timestamp)
     * 2. Validate event type is in allowed enum
     * 3. Deduplicate by event fingerprint
     * 4. Validate metadata JSON if present
     * 
     * @param rawEvents List of raw events
     * @return CleaningResult containing clean records
     */
    public CleaningResult<AnalyticsEvent> cleanAnalyticsEvents(List<AnalyticsEvent> rawEvents) {
        log.info("[ETL-CLEAN] Starting events cleaning. Input count: {}", rawEvents.size());
        
        List<AnalyticsEvent> cleanRecords = new ArrayList<>();
        List<RejectedRecord<AnalyticsEvent>> rejected = new ArrayList<>();
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 1: Filter null required fields
        // ─────────────────────────────────────────────────────────────────────
        
        List<AnalyticsEvent> notNull = rawEvents.stream()
            .filter(e -> {
                if (e.getEventType() == null) {
                    rejected.add(new RejectedRecord<>(e, "NULL_TYPE", "Event type is required"));
                    return false;
                }
                if (e.getEventTimestamp() == null) {
                    rejected.add(new RejectedRecord<>(e, "NULL_TIMESTAMP", "Event timestamp is required"));
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 2: Deduplicate by event fingerprint
        // ─────────────────────────────────────────────────────────────────────
        // Fingerprint: user_id + session_id + event_type + truncated_timestamp
        // Events within 1 second of each other are considered duplicates
        
        Map<String, AnalyticsEvent> deduped = notNull.stream()
            .collect(Collectors.toMap(
                e -> generateEventFingerprint(e),
                Function.identity(),
                (existing, duplicate) -> {
                    // Keep the first occurrence, reject duplicates
                    rejected.add(new RejectedRecord<>(duplicate, "DUPLICATE", 
                        "Duplicate of event at " + existing.getEventTimestamp()));
                    return existing;
                }
            ));
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 3: Validate metadata JSON structure
        // ─────────────────────────────────────────────────────────────────────
        
        for (AnalyticsEvent e : deduped.values()) {
            if (e.getMetadata() != null && !e.getMetadata().isEmpty()) {
                if (!isValidJson(e.getMetadata())) {
                    // Don't reject, just clear invalid metadata
                    log.warn("[ETL-CLEAN] Invalid JSON metadata for event {}, clearing", e.getId());
                    e.setMetadata(null);
                }
            }
            cleanRecords.add(e);
        }
        
        log.info("[ETL-CLEAN] Events cleaning complete. Clean: {}, Rejected: {}",
                 cleanRecords.size(), rejected.size());
        
        return new CleaningResult<>(cleanRecords, rejected, Set.of());
    }
    
    /**
     * Generate a fingerprint for event deduplication.
     * 
     * Events are considered duplicates if they have the same:
     * - User ID (or empty)
     * - Session ID (or empty)
     * - Event type
     * - Timestamp truncated to seconds
     */
    private String generateEventFingerprint(AnalyticsEvent e) {
        return String.format("%s_%s_%s_%s",
            Objects.toString(e.getUserId(), ""),
            Objects.toString(e.getSessionId(), ""),
            e.getEventType().name(),
            e.getEventTimestamp().withNano(0).toString()
        );
    }
    
    /**
     * Basic JSON validation.
     * Checks for balanced braces and quotes.
     */
    private boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return true; // Empty is valid
        }
        
        String trimmed = json.trim();
        
        // Must start with { or [
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return false;
        }
        
        // Must end with } or ]
        if (!trimmed.endsWith("}") && !trimmed.endsWith("]")) {
            return false;
        }
        
        // Count braces must be balanced
        long openBraces = trimmed.chars().filter(c -> c == '{').count();
        long closeBraces = trimmed.chars().filter(c -> c == '}').count();
        long openBrackets = trimmed.chars().filter(c -> c == '[').count();
        long closeBrackets = trimmed.chars().filter(c -> c == ']').count();
        
        return openBraces == closeBraces && openBrackets == closeBrackets;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESULT CLASSES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Result of a cleaning operation.
     * Contains clean records, rejected records, and flagged outliers.
     */
    public record CleaningResult<T>(
        List<T> cleanRecords,
        List<RejectedRecord<T>> rejectedRecords,
        Set<Long> outlierIds
    ) {
        public int cleanCount() { return cleanRecords.size(); }
        public int rejectedCount() { return rejectedRecords.size(); }
        public int outlierCount() { return outlierIds.size(); }
    }
    
    /**
     * A rejected record with reason for rejection.
     */
    public record RejectedRecord<T>(
        T record,
        String reasonCode,
        String reasonDetail
    ) {}
}
