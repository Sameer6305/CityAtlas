-- ═══════════════════════════════════════════════════════════════════════════════
-- CITYATLAS ANALYTICS SCHEMA - Star Schema for Data Warehouse
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- This migration creates analytics-focused tables following star schema design.
-- These tables complement the existing OLTP tables without modifying them.
--
-- SCHEMA OVERVIEW:
--
--                              ┌─────────────────────┐
--                              │      dim_city       │
--                              │   (City Dimension)  │
--                              └──────────┬──────────┘
--                                         │
--               ┌─────────────────────────┼─────────────────────────┐
--               │                         │                         │
--               ▼                         ▼                         ▼
--   ┌───────────────────────┐  ┌───────────────────────┐  ┌───────────────────┐
--   │  fact_city_metrics    │  │ fact_user_events_daily│  │   (Future Facts)  │
--   │  (Metric Snapshots)   │  │  (Aggregated Events)  │  │                   │
--   └───────────────────────┘  └───────────────────────┘  └───────────────────┘
--
-- ═══════════════════════════════════════════════════════════════════════════════
-- MIGRATION SAFETY:
-- - This script is ADDITIVE ONLY - no existing tables are modified or dropped
-- - Uses IF NOT EXISTS for idempotent execution
-- - All new tables are in the default 'public' schema
-- ═══════════════════════════════════════════════════════════════════════════════


-- ═══════════════════════════════════════════════════════════════════════════════
-- DIM_CITY - City Dimension Table
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- PURPOSE: Denormalized city attributes for fast analytical joins
--
-- STAR SCHEMA CONCEPT - DIMENSION TABLE:
-- • Stores descriptive attributes (WHO, WHAT, WHERE)
-- • Relatively low row count, but wide (many columns)
-- • Implements SCD Type 2 for historical tracking
-- • Linked from fact tables via foreign key
--

CREATE TABLE IF NOT EXISTS dim_city (
    -- Surrogate key (analytics-specific, not the OLTP city.id)
    id                    BIGSERIAL PRIMARY KEY,
    
    -- Foreign key to OLTP table (for ETL sync, not for analytics queries)
    source_city_id        BIGINT REFERENCES cities(id),
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- CITY ATTRIBUTES (Denormalized from OLTP)
    -- ═══════════════════════════════════════════════════════════════════════
    city_slug             VARCHAR(100) NOT NULL,      -- URL-friendly identifier
    city_name             VARCHAR(100) NOT NULL,      -- Display name
    state                 VARCHAR(100),               -- State/Province
    country               VARCHAR(100) NOT NULL,      -- Country
    population            BIGINT NOT NULL,            -- Population at snapshot time
    gdp_per_capita        DOUBLE PRECISION,           -- GDP per capita (USD)
    latitude              DOUBLE PRECISION,           -- Geo coordinate
    longitude             DOUBLE PRECISION,           -- Geo coordinate
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- DERIVED ATTRIBUTES (Pre-computed for query performance)
    -- ═══════════════════════════════════════════════════════════════════════
    -- These avoid CASE statements in every analytics query
    
    city_size_category    VARCHAR(20),  -- SMALL, MEDIUM, LARGE, MEGA
    region                VARCHAR(50),  -- Geographic region (e.g., "North America")
    gdp_tier              VARCHAR(20),  -- LOW, MEDIUM, HIGH
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- SCD TYPE 2 FIELDS (Slowly Changing Dimension)
    -- ═══════════════════════════════════════════════════════════════════════
    -- Allows historical queries: "What was this city's data as of date X?"
    
    valid_from            DATE NOT NULL,              -- When this version became active
    valid_to              DATE NOT NULL,              -- When this version expires (9999-12-31 = current)
    is_current            BOOLEAN NOT NULL,           -- Flag for current version (faster than date check)
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- AUDIT FIELDS
    -- ═══════════════════════════════════════════════════════════════════════
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes optimized for analytics access patterns
CREATE INDEX IF NOT EXISTS idx_dim_city_slug ON dim_city(city_slug);
CREATE INDEX IF NOT EXISTS idx_dim_city_current ON dim_city(is_current);
CREATE INDEX IF NOT EXISTS idx_dim_city_slug_current ON dim_city(city_slug, is_current);
CREATE INDEX IF NOT EXISTS idx_dim_city_size ON dim_city(city_size_category);
CREATE INDEX IF NOT EXISTS idx_dim_city_region ON dim_city(region);
CREATE INDEX IF NOT EXISTS idx_dim_city_country ON dim_city(country);
CREATE INDEX IF NOT EXISTS idx_dim_city_gdp_tier ON dim_city(gdp_tier);
CREATE INDEX IF NOT EXISTS idx_dim_city_valid_range ON dim_city(valid_from, valid_to);
CREATE INDEX IF NOT EXISTS idx_dim_city_source_id ON dim_city(source_city_id);

-- Ensure only one current version per city
CREATE UNIQUE INDEX IF NOT EXISTS uq_dim_city_slug_current 
    ON dim_city(city_slug) WHERE is_current = true;

COMMENT ON TABLE dim_city IS 'City dimension table (star schema) - denormalized city attributes for analytics';
COMMENT ON COLUMN dim_city.is_current IS 'SCD Type 2: TRUE for current version, FALSE for historical';
COMMENT ON COLUMN dim_city.city_size_category IS 'Derived: SMALL (<100K), MEDIUM (100K-1M), LARGE (1M-10M), MEGA (>10M)';


-- ═══════════════════════════════════════════════════════════════════════════════
-- FACT_CITY_METRICS - City Metrics Fact Table
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- PURPOSE: Daily snapshots of city metrics for trend analysis
--
-- STAR SCHEMA CONCEPT - FACT TABLE:
-- • Stores measurable events/metrics (HOW MUCH, HOW MANY)
-- • High row count, narrow columns (FKs + measures)
-- • Append-only (historical data preserved)
-- • Grain: One row per city per metric type per day
--

CREATE TABLE IF NOT EXISTS fact_city_metrics (
    -- Surrogate primary key
    id                      BIGSERIAL PRIMARY KEY,
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- DIMENSION FOREIGN KEYS
    -- ═══════════════════════════════════════════════════════════════════════
    dim_city_id             BIGINT NOT NULL REFERENCES dim_city(id),
    metric_date             DATE NOT NULL,            -- Date of this measurement
    
    -- Degenerate dimension (attribute without separate dim table)
    metric_type             VARCHAR(50) NOT NULL,     -- AQI, GDP_PER_CAPITA, etc.
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- MEASURES
    -- ═══════════════════════════════════════════════════════════════════════
    -- These are the actual values being tracked
    
    metric_value            DOUBLE PRECISION NOT NULL,  -- Current value
    metric_value_previous   DOUBLE PRECISION,           -- Previous day's value
    metric_value_delta      DOUBLE PRECISION,           -- Change from previous
    percentile_rank         DOUBLE PRECISION,           -- 0.0-1.0 rank among cities
    
    unit                    VARCHAR(50),                -- Unit of measurement
    data_quality_score      INTEGER,                    -- 0-100 confidence score
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- ETL METADATA
    -- ═══════════════════════════════════════════════════════════════════════
    data_source             VARCHAR(200),               -- Origin of data
    etl_loaded_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    etl_batch_id            VARCHAR(50),                -- Batch tracking
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- GRAIN CONSTRAINT
    -- ═══════════════════════════════════════════════════════════════════════
    CONSTRAINT uq_fact_metrics_grain UNIQUE (dim_city_id, metric_type, metric_date)
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_fact_metrics_city_type_date 
    ON fact_city_metrics(dim_city_id, metric_type, metric_date);
CREATE INDEX IF NOT EXISTS idx_fact_metrics_city_date 
    ON fact_city_metrics(dim_city_id, metric_date);
CREATE INDEX IF NOT EXISTS idx_fact_metrics_type_date 
    ON fact_city_metrics(metric_type, metric_date);
CREATE INDEX IF NOT EXISTS idx_fact_metrics_date 
    ON fact_city_metrics(metric_date);
CREATE INDEX IF NOT EXISTS idx_fact_metrics_percentile 
    ON fact_city_metrics(metric_type, percentile_rank);

COMMENT ON TABLE fact_city_metrics IS 'City metrics fact table (star schema) - daily metric snapshots';
COMMENT ON COLUMN fact_city_metrics.percentile_rank IS 'Pre-computed ranking (0.0-1.0) among all cities for this metric';


-- ═══════════════════════════════════════════════════════════════════════════════
-- FACT_USER_EVENTS_DAILY - Aggregated User Events Fact Table
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- PURPOSE: Pre-aggregated daily rollups of analytics_events for fast dashboards
--
-- AGGREGATION PATTERN:
-- Raw events (analytics_events) are rolled up nightly into daily summaries.
-- This reduces query complexity from scanning millions of rows to thousands.
--
-- Grain: One row per city per event type per day
--

CREATE TABLE IF NOT EXISTS fact_user_events_daily (
    -- Surrogate primary key
    id                      BIGSERIAL PRIMARY KEY,
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- DIMENSION FOREIGN KEYS
    -- ═══════════════════════════════════════════════════════════════════════
    dim_city_id             BIGINT REFERENCES dim_city(id),  -- Nullable for global events
    event_date              DATE NOT NULL,
    
    -- Degenerate dimension
    event_type              VARCHAR(50) NOT NULL,     -- CITY_VIEW, SEARCH, etc.
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- COUNT MEASURES (Additive)
    -- ═══════════════════════════════════════════════════════════════════════
    event_count             BIGINT NOT NULL,          -- COUNT(*)
    unique_users            BIGINT NOT NULL,          -- COUNT(DISTINCT user_id)
    unique_sessions         BIGINT NOT NULL,          -- COUNT(DISTINCT session_id)
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- DURATION MEASURES (For time-tracking events)
    -- ═══════════════════════════════════════════════════════════════════════
    total_duration_seconds  BIGINT,                   -- SUM(duration)
    avg_duration_seconds    DOUBLE PRECISION,         -- AVG(duration)
    max_duration_seconds    BIGINT,                   -- MAX(duration)
    min_duration_seconds    BIGINT,                   -- MIN(duration)
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- ENGAGEMENT QUALITY MEASURES
    -- ═══════════════════════════════════════════════════════════════════════
    bounce_count            BIGINT,                   -- COUNT WHERE duration < 10s
    engaged_count           BIGINT,                   -- COUNT WHERE duration >= 60s
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- ETL METADATA
    -- ═══════════════════════════════════════════════════════════════════════
    etl_loaded_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    etl_batch_id            VARCHAR(50),
    raw_event_count         BIGINT,                   -- Validation: should equal event_count
    
    -- ═══════════════════════════════════════════════════════════════════════
    -- GRAIN CONSTRAINT
    -- ═══════════════════════════════════════════════════════════════════════
    CONSTRAINT uq_fact_events_grain UNIQUE (dim_city_id, event_type, event_date)
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_fact_events_city_date 
    ON fact_user_events_daily(dim_city_id, event_date);
CREATE INDEX IF NOT EXISTS idx_fact_events_type_date 
    ON fact_user_events_daily(event_type, event_date);
CREATE INDEX IF NOT EXISTS idx_fact_events_date 
    ON fact_user_events_daily(event_date);
CREATE INDEX IF NOT EXISTS idx_fact_events_city_type_date 
    ON fact_user_events_daily(dim_city_id, event_type, event_date);
CREATE INDEX IF NOT EXISTS idx_fact_events_unique_users 
    ON fact_user_events_daily(event_date, unique_users);

COMMENT ON TABLE fact_user_events_daily IS 'Aggregated user events fact table (star schema) - daily event rollups';
COMMENT ON COLUMN fact_user_events_daily.unique_users IS 'Semi-additive: cannot be summed across cities';


-- ═══════════════════════════════════════════════════════════════════════════════
-- EXAMPLE ETL QUERIES
-- ═══════════════════════════════════════════════════════════════════════════════
-- These queries would be run by scheduled ETL jobs

-- 1. Populate dim_city from cities table (initial load)
/*
INSERT INTO dim_city (
    source_city_id, city_slug, city_name, state, country,
    population, gdp_per_capita, latitude, longitude,
    city_size_category, region, gdp_tier,
    valid_from, valid_to, is_current
)
SELECT 
    id,
    slug,
    name,
    state,
    country,
    population,
    gdp_per_capita,
    latitude,
    longitude,
    CASE 
        WHEN population < 100000 THEN 'SMALL'
        WHEN population < 1000000 THEN 'MEDIUM'
        WHEN population < 10000000 THEN 'LARGE'
        ELSE 'MEGA'
    END,
    CASE 
        WHEN country IN ('United States', 'Canada', 'Mexico') THEN 'North America'
        WHEN country IN ('United Kingdom', 'Germany', 'France') THEN 'Western Europe'
        ELSE 'Other'
    END,
    CASE 
        WHEN gdp_per_capita < 20000 THEN 'LOW'
        WHEN gdp_per_capita < 50000 THEN 'MEDIUM'
        ELSE 'HIGH'
    END,
    CURRENT_DATE,
    '9999-12-31'::DATE,
    true
FROM cities;
*/

-- 2. Daily aggregation from analytics_events to fact_user_events_daily
/*
INSERT INTO fact_user_events_daily (
    dim_city_id, event_type, event_date,
    event_count, unique_users, unique_sessions,
    total_duration_seconds, avg_duration_seconds,
    bounce_count, engaged_count
)
SELECT 
    dc.id,
    ae.event_type::VARCHAR,
    DATE(ae.event_timestamp),
    COUNT(*),
    COUNT(DISTINCT ae.user_id),
    COUNT(DISTINCT ae.session_id),
    SUM(COALESCE((ae.metadata::JSONB->>'duration')::BIGINT, 0)),
    AVG(COALESCE((ae.metadata::JSONB->>'duration')::NUMERIC, 0)),
    COUNT(*) FILTER (WHERE COALESCE((ae.metadata::JSONB->>'duration')::BIGINT, 0) < 10),
    COUNT(*) FILTER (WHERE COALESCE((ae.metadata::JSONB->>'duration')::BIGINT, 0) >= 60)
FROM analytics_events ae
LEFT JOIN dim_city dc ON dc.source_city_id = ae.city_id AND dc.is_current = true
WHERE DATE(ae.event_timestamp) = CURRENT_DATE - INTERVAL '1 day'
GROUP BY dc.id, ae.event_type, DATE(ae.event_timestamp)
ON CONFLICT ON CONSTRAINT uq_fact_events_grain 
DO UPDATE SET
    event_count = EXCLUDED.event_count,
    unique_users = EXCLUDED.unique_users,
    unique_sessions = EXCLUDED.unique_sessions;
*/


-- ═══════════════════════════════════════════════════════════════════════════════
-- EXAMPLE ANALYTICS QUERIES
-- ═══════════════════════════════════════════════════════════════════════════════

-- 1. Top 10 most viewed cities this month
/*
SELECT dc.city_name, SUM(f.event_count) as total_views
FROM fact_user_events_daily f
JOIN dim_city dc ON f.dim_city_id = dc.id AND dc.is_current = true
WHERE f.event_type = 'CITY_VIEW'
  AND f.event_date >= DATE_TRUNC('month', CURRENT_DATE)
GROUP BY dc.city_name
ORDER BY total_views DESC
LIMIT 10;
*/

-- 2. AQI trend by city size category
/*
SELECT dc.city_size_category,
       f.metric_date,
       AVG(f.metric_value) as avg_aqi
FROM fact_city_metrics f
JOIN dim_city dc ON f.dim_city_id = dc.id AND dc.is_current = true
WHERE f.metric_type = 'AQI'
  AND f.metric_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY dc.city_size_category, f.metric_date
ORDER BY f.metric_date;
*/

-- 3. Daily engagement trend
/*
SELECT f.event_date,
       SUM(f.event_count) as total_events,
       SUM(f.unique_users) as daily_active_users,
       SUM(f.total_duration_seconds) / 60.0 as total_minutes,
       SUM(f.bounce_count)::FLOAT / NULLIF(SUM(f.event_count), 0) as bounce_rate
FROM fact_user_events_daily f
WHERE f.event_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY f.event_date
ORDER BY f.event_date;
*/
