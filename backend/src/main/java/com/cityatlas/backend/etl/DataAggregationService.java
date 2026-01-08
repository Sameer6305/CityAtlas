package com.cityatlas.backend.etl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cityatlas.backend.entity.AnalyticsEvent;
import com.cityatlas.backend.entity.Metrics;
import com.cityatlas.backend.entity.analytics.DimCity;
import com.cityatlas.backend.entity.analytics.FactCityMetrics;
import com.cityatlas.backend.entity.analytics.FactUserEventsDaily;
import com.cityatlas.backend.etl.MetricNormalizationService.NormalizedMetric;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * DATA AGGREGATION SERVICE - Third Stage of ETL Pipeline
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Aggregates cleaned and normalized data into analytics-ready formats.
 * Transforms row-level data into summary tables for fast dashboard queries.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * AGGREGATION CONCEPTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   RAW DATA (OLTP)                   AGGREGATED DATA (OLAP)
 *   ───────────────                   ─────────────────────
 *   
 *   ┌─────────────────────────────┐   ┌─────────────────────────────┐
 *   │ analytics_events            │   │ fact_user_events_daily      │
 *   │ ─────────────────────────── │   │ ─────────────────────────── │
 *   │ 10,000 rows per day         │   │ ~500 rows per day           │
 *   │ • One row per event         │ → │ • One row per city/type/day │
 *   │ • 50ms query time           │   │ • 5ms query time            │
 *   └─────────────────────────────┘   └─────────────────────────────┘
 *   
 *   Compression: ~20:1 (varies by cardinality)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * AGGREGATION FUNCTIONS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────────┬────────────────────────────────────────────────────────┐
 *   │ Function        │ Usage                                                  │
 *   ├─────────────────┼────────────────────────────────────────────────────────┤
 *   │ COUNT(*)        │ Total events, total views                              │
 *   │ COUNT(DISTINCT) │ Unique users, unique sessions                          │
 *   │ SUM()           │ Total duration, total value                            │
 *   │ AVG()           │ Average engagement time, average metric value          │
 *   │ MIN() / MAX()   │ Range detection, outlier context                       │
 *   │ PERCENTILE()    │ Ranking cities, identifying top performers             │
 *   └─────────────────┴────────────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * AGGREGATION GRAIN
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * The "grain" defines what one row represents in the output:
 * 
 *   ┌────────────────────────────────┬────────────────────────────────────────┐
 *   │ Output Table                   │ Grain (One row per...)                 │
 *   ├────────────────────────────────┼────────────────────────────────────────┤
 *   │ fact_city_metrics              │ city + metric_type + date              │
 *   │ fact_user_events_daily         │ city + event_type + date               │
 *   └────────────────────────────────┴────────────────────────────────────────┘
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataAggregationService {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Duration threshold (seconds) below which an event is a "bounce".
     * Users who leave within this time didn't really engage.
     */
    private static final long BOUNCE_THRESHOLD_SECONDS = 10;
    
    /**
     * Duration threshold (seconds) above which an event is "engaged".
     * Users who stay this long are genuinely interested.
     */
    private static final long ENGAGED_THRESHOLD_SECONDS = 60;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CITY METRICS AGGREGATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Aggregate normalized metrics into fact_city_metrics format.
     * 
     * INPUT: Normalized metrics from MetricNormalizationService
     * OUTPUT: Daily fact rows ready for database insert
     * 
     * AGGREGATION LOGIC:
     * - Group by city + metric_type + date
     * - Use most recent value if multiple per day
     * - Include percentile rank for cross-city comparison
     * - Calculate delta from previous day (if provided)
     * 
     * @param normalizedMetrics Metrics with normalized values and percentiles
     * @param dimCityLookup Map of city_id → DimCity for FK resolution
     * @param previousDayMetrics Previous day's metrics for delta calculation
     * @param batchId ETL batch identifier for tracking
     * @return List of fact rows ready for insert
     */
    public List<FactCityMetrics> aggregateCityMetrics(
            List<NormalizedMetric> normalizedMetrics,
            Map<Long, DimCity> dimCityLookup,
            Map<String, Double> previousDayMetrics,
            String batchId
    ) {
        log.info("[ETL-AGG] Starting city metrics aggregation. Input: {} normalized metrics", 
                 normalizedMetrics.size());
        
        List<FactCityMetrics> results = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 1: Group by grain (city + type + date)
        // ─────────────────────────────────────────────────────────────────────
        // Multiple measurements per day are reduced to one per grain
        
        Map<String, List<NormalizedMetric>> byGrain = normalizedMetrics.stream()
            .collect(Collectors.groupingBy(nm -> {
                Metrics m = nm.originalMetric();
                return String.format("%d_%s_%s",
                    m.getCity().getId(),
                    m.getMetricType().name(),
                    m.getRecordedAt().toLocalDate()
                );
            }));
        
        log.debug("[ETL-AGG] Grouped into {} unique grains", byGrain.size());
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 2: Aggregate each grain into a fact row
        // ─────────────────────────────────────────────────────────────────────
        
        for (Map.Entry<String, List<NormalizedMetric>> entry : byGrain.entrySet()) {
            List<NormalizedMetric> grainMetrics = entry.getValue();
            
            // Use most recent measurement for this grain
            NormalizedMetric latest = grainMetrics.stream()
                .max((a, b) -> a.originalMetric().getRecordedAt()
                              .compareTo(b.originalMetric().getRecordedAt()))
                .orElseThrow();
            
            Metrics source = latest.originalMetric();
            Long cityId = source.getCity().getId();
            DimCity dimCity = dimCityLookup.get(cityId);
            
            if (dimCity == null) {
                log.warn("[ETL-AGG] No dim_city found for city_id={}. Skipping.", cityId);
                continue;
            }
            
            // ─────────────────────────────────────────────────────────────────
            // STEP 3: Calculate delta from previous day
            // ─────────────────────────────────────────────────────────────────
            // Delta = today's value - yesterday's value
            // Useful for trend analysis without self-joins
            
            String prevKey = String.format("%d_%s", cityId, source.getMetricType().name());
            Double previousValue = previousDayMetrics.get(prevKey);
            Double delta = null;
            
            if (previousValue != null) {
                delta = source.getValue() - previousValue;
            }
            
            // ─────────────────────────────────────────────────────────────────
            // STEP 4: Build fact row
            // ─────────────────────────────────────────────────────────────────
            
            FactCityMetrics fact = FactCityMetrics.builder()
                .dimCity(dimCity)
                .metricDate(source.getRecordedAt().toLocalDate())
                .metricType(source.getMetricType())
                .metricValue(source.getValue())
                .metricValuePrevious(previousValue)
                .metricValueDelta(delta)
                .percentileRank(latest.percentileRank())
                .unit(source.getUnit())
                .dataQualityScore(100) // From cleaning service in full implementation
                .dataSource(source.getDataSource())
                .etlBatchId(batchId)
                .build();
            
            results.add(fact);
        }
        
        log.info("[ETL-AGG] City metrics aggregation complete. Output: {} fact rows", results.size());
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // USER EVENTS AGGREGATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Aggregate analytics events into daily summary rows.
     * 
     * INPUT: Raw analytics events for a date range
     * OUTPUT: Daily rollup rows for fact_user_events_daily
     * 
     * AGGREGATION LOGIC:
     * - Group by city + event_type + date
     * - COUNT total events
     * - COUNT DISTINCT users and sessions
     * - SUM/AVG/MIN/MAX durations (from metadata)
     * - COUNT bounces and engaged users
     * 
     * @param events Clean analytics events
     * @param dimCityLookup Map of city_id → DimCity
     * @param batchId ETL batch identifier
     * @return List of daily aggregated rows
     */
    public List<FactUserEventsDaily> aggregateUserEventsDaily(
            List<AnalyticsEvent> events,
            Map<Long, DimCity> dimCityLookup,
            String batchId
    ) {
        log.info("[ETL-AGG] Starting user events aggregation. Input: {} events", events.size());
        
        List<FactUserEventsDaily> results = new ArrayList<>();
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 1: Group by grain (city + event_type + date)
        // ─────────────────────────────────────────────────────────────────────
        
        Map<String, List<AnalyticsEvent>> byGrain = events.stream()
            .collect(Collectors.groupingBy(e -> {
                Long cityId = e.getCity() != null ? e.getCity().getId() : 0L;
                return String.format("%d_%s_%s",
                    cityId,
                    e.getEventType().name(),
                    e.getEventTimestamp().toLocalDate()
                );
            }));
        
        log.debug("[ETL-AGG] Grouped into {} unique event grains", byGrain.size());
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 2: Aggregate each grain
        // ─────────────────────────────────────────────────────────────────────
        
        for (Map.Entry<String, List<AnalyticsEvent>> entry : byGrain.entrySet()) {
            List<AnalyticsEvent> grainEvents = entry.getValue();
            AnalyticsEvent sample = grainEvents.get(0);
            
            // Resolve dimension reference
            Long cityId = sample.getCity() != null ? sample.getCity().getId() : null;
            DimCity dimCity = cityId != null ? dimCityLookup.get(cityId) : null;
            
            // ─────────────────────────────────────────────────────────────────
            // STEP 3: Calculate COUNT aggregations
            // ─────────────────────────────────────────────────────────────────
            
            long eventCount = grainEvents.size();
            
            // COUNT DISTINCT user_id
            long uniqueUsers = grainEvents.stream()
                .map(AnalyticsEvent::getUserId)
                .filter(uid -> uid != null && !uid.isEmpty())
                .distinct()
                .count();
            
            // COUNT DISTINCT session_id  
            long uniqueSessions = grainEvents.stream()
                .map(AnalyticsEvent::getSessionId)
                .filter(sid -> sid != null && !sid.isEmpty())
                .distinct()
                .count();
            
            // ─────────────────────────────────────────────────────────────────
            // STEP 4: Calculate duration aggregations (from metadata)
            // ─────────────────────────────────────────────────────────────────
            
            List<Long> durations = grainEvents.stream()
                .map(e -> extractDuration(e.getMetadata()))
                .filter(d -> d != null && d > 0)
                .collect(Collectors.toList());
            
            Long totalDuration = null;
            Double avgDuration = null;
            Long maxDuration = null;
            Long minDuration = null;
            Long bounceCount = null;
            Long engagedCount = null;
            
            if (!durations.isEmpty()) {
                totalDuration = durations.stream().mapToLong(Long::longValue).sum();
                avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0);
                maxDuration = durations.stream().mapToLong(Long::longValue).max().orElse(0);
                minDuration = durations.stream().mapToLong(Long::longValue).min().orElse(0);
                
                // Count bounces (duration < threshold)
                bounceCount = durations.stream()
                    .filter(d -> d < BOUNCE_THRESHOLD_SECONDS)
                    .count();
                
                // Count engaged (duration >= threshold)
                engagedCount = durations.stream()
                    .filter(d -> d >= ENGAGED_THRESHOLD_SECONDS)
                    .count();
            }
            
            // ─────────────────────────────────────────────────────────────────
            // STEP 5: Build fact row
            // ─────────────────────────────────────────────────────────────────
            
            FactUserEventsDaily fact = FactUserEventsDaily.builder()
                .dimCity(dimCity)
                .eventDate(sample.getEventTimestamp().toLocalDate())
                .eventType(sample.getEventType())
                .eventCount(eventCount)
                .uniqueUsers(uniqueUsers)
                .uniqueSessions(uniqueSessions)
                .totalDurationSeconds(totalDuration)
                .avgDurationSeconds(avgDuration)
                .maxDurationSeconds(maxDuration)
                .minDurationSeconds(minDuration)
                .bounceCount(bounceCount)
                .engagedCount(engagedCount)
                .etlBatchId(batchId)
                .rawEventCount(eventCount)
                .build();
            
            results.add(fact);
        }
        
        log.info("[ETL-AGG] User events aggregation complete. Output: {} fact rows", results.size());
        return results;
    }
    
    /**
     * Extract duration value from event metadata JSON.
     * 
     * Expected format: {"duration": 123}
     * 
     * @param metadata JSON metadata string
     * @return Duration in seconds, or null if not present
     */
    private Long extractDuration(String metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        try {
            // Simple JSON extraction without full parser
            // Look for "duration": or "duration" :
            int idx = metadata.indexOf("\"duration\"");
            if (idx == -1) {
                return null;
            }
            
            // Find the colon after "duration"
            int colonIdx = metadata.indexOf(':', idx);
            if (colonIdx == -1) {
                return null;
            }
            
            // Extract numeric value after colon
            StringBuilder numBuilder = new StringBuilder();
            for (int i = colonIdx + 1; i < metadata.length(); i++) {
                char c = metadata.charAt(i);
                if (Character.isDigit(c)) {
                    numBuilder.append(c);
                } else if (numBuilder.length() > 0) {
                    break; // End of number
                }
            }
            
            if (numBuilder.length() > 0) {
                return Long.parseLong(numBuilder.toString());
            }
        } catch (NumberFormatException e) {
            log.debug("[ETL-AGG] Could not parse duration from metadata: {}", metadata);
        }
        
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SUMMARY AGGREGATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generate summary statistics for a batch of aggregated facts.
     * Useful for logging and monitoring ETL health.
     */
    public AggregationSummary summarize(
            List<FactCityMetrics> metricsFacts,
            List<FactUserEventsDaily> eventsFacts
    ) {
        return new AggregationSummary(
            metricsFacts.size(),
            eventsFacts.size(),
            metricsFacts.stream()
                .map(f -> f.getDimCity() != null ? f.getDimCity().getCitySlug() : "unknown")
                .distinct()
                .count(),
            eventsFacts.stream()
                .mapToLong(FactUserEventsDaily::getEventCount)
                .sum(),
            eventsFacts.stream()
                .mapToLong(FactUserEventsDaily::getUniqueUsers)
                .sum()
        );
    }
    
    /**
     * Summary of aggregation results for logging.
     */
    public record AggregationSummary(
        int metricFactCount,
        int eventFactCount,
        long distinctCities,
        long totalEventsAggregated,
        long totalUniqueUsers
    ) {
        @Override
        public String toString() {
            return String.format(
                "AggregationSummary[metrics=%d, events=%d, cities=%d, rawEvents=%d, users=%d]",
                metricFactCount, eventFactCount, distinctCities, 
                totalEventsAggregated, totalUniqueUsers
            );
        }
    }
}
