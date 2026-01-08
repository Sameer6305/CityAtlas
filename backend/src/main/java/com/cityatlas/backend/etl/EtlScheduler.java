package com.cityatlas.backend.etl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cityatlas.backend.entity.AnalyticsEvent;
import com.cityatlas.backend.entity.Metrics;
import com.cityatlas.backend.entity.analytics.DimCity;
import com.cityatlas.backend.entity.analytics.FactCityMetrics;
import com.cityatlas.backend.entity.analytics.FactUserEventsDaily;
import com.cityatlas.backend.etl.DataCleaningService.CleaningResult;
import com.cityatlas.backend.etl.MetricNormalizationService.NormalizedMetric;
import com.cityatlas.backend.repository.AnalyticsEventRepository;
import com.cityatlas.backend.repository.MetricsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ============================================================================
 *                            BATCH PATH
 * ============================================================================
 * 
 * ETL SCHEDULER - Orchestrates Scheduled Batch ETL Jobs
 * 
 * This class implements the BATCH PATH of the CityAtlas Lambda Architecture.
 * It runs scheduled jobs that aggregate data into the star schema (dim + fact)
 * tables for historical analysis.
 * 
 * ============================================================================
 *                    DATA FLOW (BATCH PATH)
 * ============================================================================
 * 
 *   +---------------+     +------------------+     +-------------------+
 *   | OLTP Tables   | --> | EtlScheduler     | --> | Star Schema       |
 *   | (cities,      |     | (This Class)     |     | (dim + fact)      |
 *   |  metrics,     |     |                  |     |                   |
 *   |  events)      |     |                  |     |                   |
 *   +---------------+     +------------------+     +-------------------+
 * 
 * OUTPUT TABLES:
 *   - dim_city              : City dimension (slowly changing)
 *   - fact_city_metrics     : Daily city metrics aggregates
 *   - fact_user_events_daily: Daily user events aggregates
 * 
 * ============================================================================
 *                    BATCH PATH vs STREAMING PATH
 * ============================================================================
 * 
 *   BATCH PATH (This Class)                STREAMING PATH (EtlKafkaListener)
 *   -----------------------                --------------------------------
 *   Latency: Minutes to hours              Latency: Seconds
 *   Trigger: Cron schedule                 Trigger: Kafka events
 *   Output: Aggregated facts               Output: Raw events
 *   Use case: Historical analysis          Use case: Real-time counters
 * 
 * ============================================================================
 * SCHEDULING STRATEGY
 * ============================================================================
 * 
 *   +-------------------------+-----------------+-----------------------------+
 *   | JOB                     | SCHEDULE        | TARGET TABLES               |
 *   +-------------------------+-----------------+-----------------------------+
 *   | Dimension Refresh       | Daily 2:00 AM   | dim_city                    |
 *   | Metrics Snapshot        | Hourly :00      | fact_city_metrics           |
 *   | Events Aggregation      | Every 15 min    | fact_user_events_daily      |
 *   | Full Rebuild            | Weekly Sun 3AM  | All star schema tables      |
 *   +-------------------------+-----------------+-----------------------------+
 * 
 * ============================================================================
 * CRON EXPRESSION REFERENCE
 * ============================================================================
 * 
 *   Format: "second minute hour day month weekday"
 *   
 *   Examples:
 *   - "0 0 2 * * *"    = Every day at 2:00 AM
 *   - "0 0 * * * *"    = Every hour at :00
 *   - "0 star/15 * * * *" = Every 15 minutes (star = asterisk)
 *   - "0 0 3 * * SUN"  = Every Sunday at 3:00 AM
 */
@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class EtlScheduler {
    
    // ==========================================================================
    // DEPENDENCIES
    // ==========================================================================
    
    private final DataCleaningService cleaningService;
    private final MetricNormalizationService normalizationService;
    private final DataAggregationService aggregationService;
    private final DimensionLoaderService dimensionLoaderService;
    
    // Source data repositories
    private final MetricsRepository metricsRepository;
    private final AnalyticsEventRepository eventRepository;
    
    // Note: In production, add repositories for analytics tables
    // private final DimCityRepository dimCityRepository;
    // private final FactCityMetricsRepository factMetricsRepository;
    // private final FactUserEventsDailyRepository factEventsRepository;
    
    // ==========================================================================
    // BATCH ID GENERATION
    // ==========================================================================
    
    private static final DateTimeFormatter BATCH_ID_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private String generateBatchId() {
        return "ETL_" + LocalDateTime.now().format(BATCH_ID_FORMAT);
    }
    
    // ==========================================================================
    // SCHEDULED JOB: Dimension Refresh (Daily)
    // ==========================================================================
    
    /**
     * Refresh dimension tables daily at 2:00 AM.
     * 
     * WHAT IT DOES:
     * 1. Loads current dim_city rows
     * 2. Compares with OLTP cities table
     * 3. Applies SCD Type 2 for any changes
     * 
     * WHY 2 AM:
     * - Low user traffic (minimal impact on OLTP)
     * - Before business hours (data ready for morning)
     * - Allows time to detect issues before peak usage
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    @Transactional
    public void refreshDimensions() {
        String batchId = generateBatchId();
        log.info("[ETL-SCHED] === Starting Dimension Refresh Job === Batch: {}", batchId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // -----------------------------------------------------------------
            // STEP 1: Build fresh dimension data from OLTP
            // -----------------------------------------------------------------
            
            List<DimCity> freshDimCities = dimensionLoaderService.buildInitialDimCityLoad();
            
            // -----------------------------------------------------------------
            // STEP 2: In production, compare with existing and apply SCD Type 2
            // -----------------------------------------------------------------
            
            // List<DimCity> currentDims = dimCityRepository.findByIsCurrentTrue();
            // DimensionChangeSet changes = dimensionLoaderService.detectChanges(currentDims);
            // 
            // if (changes.hasChanges()) {
            //     dimCityRepository.saveAll(changes.expirations);
            //     dimCityRepository.saveAll(changes.inserts);
            // }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[ETL-SCHED] === Dimension Refresh Complete === Duration: {}ms, Cities: {}",
                     duration, freshDimCities.size());
            
        } catch (Exception e) {
            log.error("[ETL-SCHED] === Dimension Refresh FAILED === Batch: {}", batchId, e);
            // In production: Send alert, update monitoring dashboard
        }
    }
    
    // ==========================================================================
    // SCHEDULED JOB: Metrics Snapshot (Hourly)
    // ==========================================================================
    
    /**
     * Snapshot city metrics every hour at :00.
     * 
     * WHAT IT DOES:
     * 1. Extract metrics recorded in the last hour
     * 2. Clean and validate
     * 3. Normalize values
     * 4. Aggregate to daily grain
     * 5. Load into fact_city_metrics
     * 
     * WHY HOURLY:
     * - Balances freshness vs processing overhead
     * - Captures intraday metric updates
     * - Provides near-real-time dashboard data
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at :00
    @Transactional
    public void snapshotMetrics() {
        String batchId = generateBatchId();
        log.info("[ETL-SCHED] === Starting Metrics Snapshot Job === Batch: {}", batchId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            LocalDateTime windowEnd = LocalDateTime.now();
            LocalDateTime windowStart = windowEnd.minusHours(1);
            
            // -----------------------------------------------------------------
            // EXTRACT: Get metrics from the last hour
            // -----------------------------------------------------------------
            
            List<Metrics> rawMetrics = metricsRepository
                .findByRecordedAtBetween(windowStart, windowEnd);
            
            log.debug("[ETL-SCHED] Extracted {} metrics from {} to {}", 
                     rawMetrics.size(), windowStart, windowEnd);
            
            if (rawMetrics.isEmpty()) {
                log.info("[ETL-SCHED] No new metrics in window. Skipping.");
                return;
            }
            
            // -----------------------------------------------------------------
            // CLEAN: Validate and filter
            // -----------------------------------------------------------------
            
            CleaningResult<Metrics> cleanResult = cleaningService.cleanMetrics(rawMetrics);
            
            log.debug("[ETL-SCHED] Cleaning result: {} clean, {} rejected",
                     cleanResult.cleanCount(), cleanResult.rejectedCount());
            
            // -----------------------------------------------------------------
            // TRANSFORM: Normalize values
            // -----------------------------------------------------------------
            
            List<NormalizedMetric> normalized = normalizationService
                .normalizeMetrics(cleanResult.cleanRecords());
            
            // -----------------------------------------------------------------
            // AGGREGATE: Build fact rows
            // -----------------------------------------------------------------
            
            // In production: Load dim_city lookup and previous day metrics
            Map<Long, DimCity> dimCityLookup = new HashMap<>();
            Map<String, Double> previousDayMetrics = new HashMap<>();
            
            List<FactCityMetrics> facts = aggregationService.aggregateCityMetrics(
                normalized, dimCityLookup, previousDayMetrics, batchId
            );
            
            // -----------------------------------------------------------------
            // LOAD: Upsert into fact table
            // -----------------------------------------------------------------
            
            // In production:
            // factMetricsRepository.saveAll(facts);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[ETL-SCHED] === Metrics Snapshot Complete === Duration: {}ms, Facts: {}",
                     duration, facts.size());
            
        } catch (Exception e) {
            log.error("[ETL-SCHED] === Metrics Snapshot FAILED === Batch: {}", batchId, e);
        }
    }
    
    // ==========================================================================
    // SCHEDULED JOB: Events Aggregation (Every 15 minutes)
    // ==========================================================================
    
    /**
     * Aggregate user events every 15 minutes.
     * 
     * WHAT IT DOES:
     * 1. Extract events from the last 15 minutes
     * 2. Clean and deduplicate
     * 3. Aggregate to daily grain (running totals)
     * 4. Upsert into fact_user_events_daily
     * 
     * WHY 15 MINUTES:
     * - Near-real-time dashboard updates
     * - Low enough frequency to batch efficiently
     * - High enough for timely analytics
     */
    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    @Transactional
    public void aggregateEvents() {
        String batchId = generateBatchId();
        log.info("[ETL-SCHED] === Starting Events Aggregation Job === Batch: {}", batchId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            LocalDateTime windowEnd = LocalDateTime.now();
            LocalDateTime windowStart = windowEnd.minusMinutes(15);
            
            // -----------------------------------------------------------------
            // EXTRACT: Get events from the last 15 minutes
            // -----------------------------------------------------------------
            
            List<AnalyticsEvent> rawEvents = eventRepository
                .findByEventTimestampBetween(windowStart, windowEnd);
            
            log.debug("[ETL-SCHED] Extracted {} events from {} to {}", 
                     rawEvents.size(), windowStart, windowEnd);
            
            if (rawEvents.isEmpty()) {
                log.debug("[ETL-SCHED] No new events in window. Skipping.");
                return;
            }
            
            // -----------------------------------------------------------------
            // CLEAN: Validate and deduplicate
            // -----------------------------------------------------------------
            
            CleaningResult<AnalyticsEvent> cleanResult = cleaningService
                .cleanAnalyticsEvents(rawEvents);
            
            // -----------------------------------------------------------------
            // AGGREGATE: Build daily rollup facts
            // -----------------------------------------------------------------
            
            Map<Long, DimCity> dimCityLookup = new HashMap<>(); // Load in production
            
            List<FactUserEventsDaily> facts = aggregationService.aggregateUserEventsDaily(
                cleanResult.cleanRecords(), dimCityLookup, batchId
            );
            
            // -----------------------------------------------------------------
            // LOAD: Upsert into fact table (accumulate into today's row)
            // -----------------------------------------------------------------
            
            // In production: Use upsert with accumulation logic
            // factEventsRepository.upsertDailyAggregates(facts);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[ETL-SCHED] === Events Aggregation Complete === Duration: {}ms, Facts: {}",
                     duration, facts.size());
            
        } catch (Exception e) {
            log.error("[ETL-SCHED] === Events Aggregation FAILED === Batch: {}", batchId, e);
        }
    }
    
    // ==========================================================================
    // MANUAL TRIGGER METHODS (for testing/admin)
    // ==========================================================================
    
    /**
     * Manually trigger dimension refresh.
     * Exposed for admin/testing purposes.
     */
    public void triggerDimensionRefresh() {
        log.info("[ETL-SCHED] Manual dimension refresh triggered");
        refreshDimensions();
    }
    
    /**
     * Manually trigger metrics snapshot.
     */
    public void triggerMetricsSnapshot() {
        log.info("[ETL-SCHED] Manual metrics snapshot triggered");
        snapshotMetrics();
    }
    
    /**
     * Manually trigger events aggregation.
     */
    public void triggerEventsAggregation() {
        log.info("[ETL-SCHED] Manual events aggregation triggered");
        aggregateEvents();
    }
    
    /**
     * Run full ETL pipeline (all jobs in sequence).
     * Use for initial setup or recovery.
     */
    public void runFullPipeline() {
        log.info("[ETL-SCHED] =============================================");
        log.info("[ETL-SCHED] Starting Full ETL Pipeline");
        log.info("[ETL-SCHED] =============================================");
        
        refreshDimensions();
        snapshotMetrics();
        aggregateEvents();
        
        log.info("[ETL-SCHED] =============================================");
        log.info("[ETL-SCHED] Full ETL Pipeline Complete");
        log.info("[ETL-SCHED] =============================================");
    }
}
