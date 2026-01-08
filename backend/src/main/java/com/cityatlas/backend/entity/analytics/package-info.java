/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ANALYTICS SCHEMA - Star Schema Design for CityAtlas Data Warehouse
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This package contains JPA entities implementing a STAR SCHEMA design pattern
 * for analytical queries. These tables complement the OLTP (transactional) tables
 * in the parent entity package without modifying them.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHAT IS A STAR SCHEMA?
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * A star schema is a database design pattern optimized for:
 * - Fast analytical queries (aggregations, GROUP BY, filtering)
 * - Business intelligence and reporting
 * - Data warehousing workloads
 * 
 * It gets its name from the shape when visualized:
 * 
 *                           ┌─────────────┐
 *                           │  dim_time   │
 *                           │ (Dimension) │
 *                           └──────┬──────┘
 *                                  │
 *        ┌─────────────┐    ┌──────┴──────┐    ┌─────────────┐
 *        │  dim_city   │────│ fact_metrics│────│dim_geography│
 *        │ (Dimension) │    │   (FACT)    │    │ (Dimension) │
 *        └─────────────┘    └──────┬──────┘    └─────────────┘
 *                                  │
 *                           ┌──────┴──────┐
 *                           │ dim_category│
 *                           │ (Dimension) │
 *                           └─────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * DIMENSION TABLES vs FACT TABLES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * DIMENSION TABLES (dim_*)
 * ─────────────────────────
 * • Store descriptive attributes (WHO, WHAT, WHERE, WHEN)
 * • Low row count, wide columns (many attributes)
 * • Slowly changing (city name, country don't change often)
 * • Denormalized for query performance
 * • Examples: dim_city, dim_time, dim_geography
 * 
 * FACT TABLES (fact_*)
 * ─────────────────────
 * • Store measurable events and metrics (HOW MUCH, HOW MANY)
 * • High row count, narrow columns (mostly foreign keys + measures)
 * • Append-only or aggregated (never updated in place)
 * • Linked to dimensions via foreign keys
 * • Examples: fact_city_metrics, fact_user_events_daily
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY SEPARATE ANALYTICS TABLES?
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. PERFORMANCE ISOLATION
 *    - Heavy analytics queries don't slow down transactional operations
 *    - Aggregations run on pre-computed data
 *    - Indexes optimized for different access patterns
 * 
 * 2. DENORMALIZATION FOR SPEED
 *    - dim_city contains flattened data from multiple OLTP tables
 *    - Avoids costly JOINs at query time
 *    - Trade-off: Storage space vs Query speed
 * 
 * 3. HISTORICAL TRACKING
 *    - Fact tables preserve historical snapshots
 *    - Can query "What was the AQI last month?" even if OLTP data changed
 *    - Supports time-series analysis and trending
 * 
 * 4. AGGREGATION SUPPORT
 *    - Daily aggregates (fact_user_events_daily) reduce query complexity
 *    - Pre-computed rollups for common time granularities
 *    - Enables real-time dashboards without scanning raw events
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * TABLES IN THIS PACKAGE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. DimCity (dim_city)
 *    ─────────────────────────────────────────────────────────────────────────
 *    Dimension table for city attributes.
 *    Denormalized view of city data optimized for analytical JOINs.
 *    
 *    Key Features:
 *    • Surrogate key (analytics_city_id) separate from OLTP primary key
 *    • Valid date ranges for slowly changing dimension (SCD Type 2)
 *    • Includes derived attributes (city_size_category, region)
 *    • Links to OLTP city via city_id foreign key
 * 
 * 2. FactCityMetrics (fact_city_metrics)
 *    ─────────────────────────────────────────────────────────────────────────
 *    Fact table storing daily metric snapshots for each city.
 *    Central table for city performance analytics.
 *    
 *    Grain: One row per city per metric type per day
 *    
 *    Measures:
 *    • metric_value (current value)
 *    • metric_value_previous (for delta calculations)
 *    • percentile_rank (relative ranking among cities)
 * 
 * 3. FactUserEventsDaily (fact_user_events_daily)
 *    ─────────────────────────────────────────────────────────────────────────
 *    Pre-aggregated daily rollup of user events from analytics_events table.
 *    Enables fast dashboard queries without scanning millions of raw events.
 *    
 *    Grain: One row per city per event type per day
 *    
 *    Measures:
 *    • event_count (total events)
 *    • unique_users (distinct user count)
 *    • unique_sessions (distinct session count)
 *    • total_duration_seconds (for time-spent events)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * EXAMPLE ANALYTICS QUERIES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * -- Top 10 most viewed cities this month
 * SELECT dc.city_name, SUM(f.event_count) as total_views
 * FROM fact_user_events_daily f
 * JOIN dim_city dc ON f.dim_city_id = dc.id
 * WHERE f.event_type = 'CITY_VIEW'
 *   AND f.event_date >= DATE_TRUNC('month', CURRENT_DATE)
 * GROUP BY dc.city_name
 * ORDER BY total_views DESC
 * LIMIT 10;
 * 
 * -- AQI trend comparison: Large vs Small cities
 * SELECT dc.city_size_category,
 *        f.metric_date,
 *        AVG(f.metric_value) as avg_aqi
 * FROM fact_city_metrics f
 * JOIN dim_city dc ON f.dim_city_id = dc.id
 * WHERE f.metric_type = 'AQI'
 *   AND f.metric_date >= CURRENT_DATE - INTERVAL '30 days'
 * GROUP BY dc.city_size_category, f.metric_date
 * ORDER BY f.metric_date;
 * 
 * -- User engagement by region
 * SELECT dc.region,
 *        SUM(f.event_count) as total_events,
 *        SUM(f.unique_users) as total_users,
 *        SUM(f.total_duration_seconds) / 60.0 as total_minutes
 * FROM fact_user_events_daily f
 * JOIN dim_city dc ON f.dim_city_id = dc.id
 * WHERE f.event_date >= CURRENT_DATE - INTERVAL '7 days'
 * GROUP BY dc.region
 * ORDER BY total_events DESC;
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * ETL PIPELINE (Data Loading)
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * These tables are populated by scheduled ETL jobs:
 * 
 * 1. dim_city: Refreshed daily from cities table
 *    - INSERT new cities as new dimension rows
 *    - UPDATE existing rows only for SCD Type 2 changes
 * 
 * 2. fact_city_metrics: Refreshed daily
 *    - Snapshot of current metric values
 *    - Compute percentile ranks
 *    - Link to dim_city
 * 
 * 3. fact_user_events_daily: Aggregated nightly from analytics_events
 *    - GROUP BY city_id, event_type, DATE(event_timestamp)
 *    - COUNT events, COUNT DISTINCT users/sessions
 *    - SUM duration values
 * 
 * @see com.cityatlas.backend.entity.City
 * @see com.cityatlas.backend.entity.AnalyticsEvent
 * @see com.cityatlas.backend.entity.Metrics
 */
package com.cityatlas.backend.entity.analytics;
