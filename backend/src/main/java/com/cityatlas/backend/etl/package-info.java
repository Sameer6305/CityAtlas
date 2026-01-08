/**
 * ==============================================================================
 * ETL PACKAGE - Data Pipeline Architecture for CityAtlas Analytics
 * ==============================================================================
 *
 * This package implements the data pipeline that transforms raw operational
 * data into analytics-ready formats for the star schema.
 *
 * ==============================================================================
 *                    DUAL-PATH ARCHITECTURE
 * ==============================================================================
 *
 * CityAtlas uses a LAMBDA-INSPIRED architecture with two distinct data paths:
 *
 *   +=========================================================================+
 *   |                                                                         |
 *   |  +-------------------------------------------------------------------+  |
 *   |  |                     STREAMING PATH (Hot)                          |  |
 *   |  |                                                                   |  |
 *   |  |    Kafka Events --> EtlKafkaListener --> analytics_events table   |  |
 *   |  |                                                                   |  |
 *   |  |  Characteristics:                                                 |  |
 *   |  |  - Latency: Seconds (micro-batch every 10s or 100 events)         |  |
 *   |  |  - Volume: Individual events                                      |  |
 *   |  |  - Use case: Real-time dashboards, live counters                  |  |
 *   |  |  - Trigger: Event-driven (Kafka consumer)                         |  |
 *   |  +-------------------------------------------------------------------+  |
 *   |                              |                                          |
 *   |                              v                                          |
 *   |                   +-------------------+                                 |
 *   |                   |  analytics_events |  <-- Raw event storage          |
 *   |                   +-------------------+                                 |
 *   |                                                                         |
 *   +=========================================================================+
 *
 *   +=========================================================================+
 *   |                                                                         |
 *   |  +-------------------------------------------------------------------+  |
 *   |  |                      BATCH PATH (Cold)                            |  |
 *   |  |                                                                   |  |
 *   |  |    OLTP Tables --> EtlScheduler --> Star Schema (dim + fact)      |  |
 *   |  |                                                                   |  |
 *   |  |  Characteristics:                                                 |  |
 *   |  |  - Latency: Minutes to hours                                      |  |
 *   |  |  - Volume: Large datasets processed together                      |  |
 *   |  |  - Use case: Historical analysis, trend reports, aggregations     |  |
 *   |  |  - Trigger: Cron schedule (@Scheduled)                            |  |
 *   |  +-------------------------------------------------------------------+  |
 *   |                              |                                          |
 *   |                              v                                          |
 *   |     +-------------+  +------------------+  +---------------------+      |
 *   |     |  dim_city   |  | fact_city_metrics|  | fact_user_events    |      |
 *   |     | (Dimension) |  |   (Daily Agg)    |  |    (Daily Agg)      |      |
 *   |     +-------------+  +------------------+  +---------------------+      |
 *   |                                                                         |
 *   +=========================================================================+
 *
 * ==============================================================================
 *                    STREAMING PATH DETAILS
 * ==============================================================================
 *
 * PURPOSE: Capture user interactions with minimal latency for real-time analytics.
 *
 * DATA FLOW:
 * 
 *   +---------------+     +------------------+     +-------------------+
 *   | User Action   | --> | Kafka Producer   | --> | Kafka Topic       |
 *   | (Frontend)    |     | (KafkaService)   |     | (analytics.*)     |
 *   +---------------+     +------------------+     +-------------------+
 *                                                          |
 *                                                          v
 *   +-------------------+     +------------------+     +-------------------+
 *   | analytics_events  | <-- | EtlKafkaListener | <-- | Kafka Consumer    |
 *   | (PostgreSQL)      |     | (Micro-batch)    |     | (Spring Kafka)    |
 *   +-------------------+     +------------------+     +-------------------+
 *
 * TOPICS CONSUMED:
 *   - cityatlas.analytics.city-searched    --> Search analytics
 *   - cityatlas.analytics.section-viewed   --> Page engagement
 *   - cityatlas.analytics.time-spent       --> Session duration
 *
 * MICRO-BATCH STRATEGY:
 *   - Accumulate events in memory (ConcurrentHashMap)
 *   - Flush to database when: 100 events OR 10 seconds elapsed
 *   - Benefits: Reduced DB round-trips, handles burst traffic
 *
 * OUTPUT TABLE: analytics_events
 *   - Stores raw events with minimal transformation
 *   - Enables real-time queries and live counters
 *   - Source for batch aggregation jobs
 *
 * ==============================================================================
 *                    BATCH PATH DETAILS
 * ==============================================================================
 *
 * PURPOSE: Transform raw data into pre-aggregated analytics for fast querying.
 *
 * DATA FLOW:
 *
 *   +---------------+     +------------------+     +-------------------+
 *   | OLTP Tables   | --> | EtlScheduler     | --> | Star Schema       |
 *   | (cities,      |     | (Spring @Sched)  |     | (dim + fact)      |
 *   |  metrics,     |     |                  |     |                   |
 *   |  events)      |     |                  |     |                   |
 *   +---------------+     +------------------+     +-------------------+
 *
 * SCHEDULED JOBS:
 *
 *   +-------------------------+------------------+---------------------------+
 *   | Job                     | Schedule         | Target Tables             |
 *   +-------------------------+------------------+---------------------------+
 *   | Dimension Refresh       | Daily 2:00 AM    | dim_city                  |
 *   | Metrics Aggregation     | Hourly :00       | fact_city_metrics         |
 *   | Events Aggregation      | Every 15 min     | fact_user_events_daily    |
 *   +-------------------------+------------------+---------------------------+
 *
 * TRANSFORMATION PIPELINE:
 *
 *   1. EXTRACT
 *      - Read from source tables with time window
 *      - Apply watermarks for incremental processing
 *
 *   2. CLEAN (DataCleaningService)
 *      - Filter null/invalid records
 *      - Detect and handle outliers (Z-score > 3)
 *      - Deduplicate by natural key
 *
 *   3. TRANSFORM
 *      - Normalize metrics (MetricNormalizationService)
 *      - Aggregate to daily grain (DataAggregationService)
 *      - Compute derived fields (percentiles, deltas)
 *
 *   4. LOAD
 *      - Upsert into dimension tables (SCD Type 2)
 *      - Insert/update fact tables
 *      - Track batch lineage (etl_batch_id)
 *
 * ==============================================================================
 *                    WHEN TO USE EACH PATH
 * ==============================================================================
 *
 *   USE STREAMING PATH WHEN:
 *   - Need real-time counters (e.g., "X users online now")
 *   - Tracking live user sessions
 *   - Immediate event logging for debugging
 *   - Latency requirement: seconds
 *
 *   USE BATCH PATH WHEN:
 *   - Computing daily/weekly/monthly aggregates
 *   - Historical trend analysis
 *   - Cross-city comparisons
 *   - Complex transformations (joins, window functions)
 *   - Latency tolerance: minutes to hours
 *
 * ==============================================================================
 *                    PACKAGE STRUCTURE
 * ==============================================================================
 *
 *   etl/
 *   +-- package-info.java          # This documentation
 *   |
 *   +-- STREAMING PATH:
 *   |   +-- EtlKafkaListener.java  # Kafka consumer with micro-batching
 *   |
 *   +-- BATCH PATH:
 *   |   +-- EtlScheduler.java      # Spring @Scheduled orchestrator
 *   |
 *   +-- SHARED SERVICES:
 *       +-- DataCleaningService.java       # Validation, outlier detection
 *       +-- MetricNormalizationService.java # Scaling, percentiles
 *       +-- DataAggregationService.java    # Grouping, rollups
 *       +-- DimensionLoaderService.java    # SCD Type 2 handling
 *
 * ==============================================================================
 *                    CONFIGURATION
 * ==============================================================================
 *
 * Streaming path (application.properties):
 *   kafka.enabled=true
 *   kafka.bootstrap-servers=localhost:9092
 *   kafka.consumer.group-id=cityatlas-etl-consumer
 *
 * Batch path (application.properties):
 *   etl.batch.enabled=true
 *   etl.batch.dimension-refresh-cron=0 0 2 * * *
 *   etl.batch.metrics-snapshot-cron=0 0 * * * *
 *   etl.batch.events-aggregation-cron=0 * /15 * * * *
 *
 * ==============================================================================
 *                    MONITORING
 * ==============================================================================
 *
 * Both paths emit structured logs with prefixes for filtering:
 *
 *   STREAMING: [ETL-KAFKA] ...
 *   BATCH:     [ETL-SCHED] ...
 *
 * Key metrics to monitor:
 *   - Streaming: events/second, flush frequency, accumulator size
 *   - Batch: job duration, records processed, error rate
 *
 * @see EtlKafkaListener   Streaming path implementation
 * @see EtlScheduler       Batch path implementation
 * @see DataCleaningService       Shared transformation service
 * @see DataAggregationService    Shared aggregation service
 */
package com.cityatlas.backend.etl;
