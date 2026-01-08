-- ═══════════════════════════════════════════════════════════════════════════════
-- V3: AI Feature Engineering - Computed Features Table
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- PURPOSE:
-- Stores pre-computed city scores for fast retrieval and reuse.
-- Eliminates redundant computation of deterministic features.
--
-- GRAIN: One row per city per computation date
--
-- SCORES (0-100 scale):
--   - economy_score: GDP per capita (40%) + Unemployment rate (60%)
--   - livability_score: Cost of living (35%) + AQI (35%) + Population (30%)
--   - sustainability_score: AQI (100%), future: carbon, green space
--   - growth_score: Population growth (50%) + GDP growth (50%)
--   - overall_score: Economy (30%) + Livability (35%) + Sustain. (20%) + Growth (15%)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS city_computed_features (
    id BIGSERIAL PRIMARY KEY,
    
    -- ═══════════════════════════════════════════════════════════════════════════
    -- DIMENSION RELATIONSHIP
    -- ═══════════════════════════════════════════════════════════════════════════
    
    -- Reference to city dimension table (nullable for flexibility during development)
    city_key BIGINT REFERENCES dim_city(city_key),
    
    -- Date when features were computed (forms unique key with city_key)
    computation_date DATE NOT NULL,
    
    -- ═══════════════════════════════════════════════════════════════════════════
    -- COMPUTED SCORES (0-100 scale, nullable if insufficient data)
    -- ═══════════════════════════════════════════════════════════════════════════
    
    -- Economy Score: GDP (40%) + Unemployment (60%)
    economy_score DOUBLE PRECISION,
    
    -- Livability Score: Cost (35%) + AQI (35%) + Population (30%)
    livability_score DOUBLE PRECISION,
    
    -- Sustainability Score: AQI (100%)
    sustainability_score DOUBLE PRECISION,
    
    -- Growth Score: Population growth (50%) + GDP growth (50%)
    growth_score DOUBLE PRECISION,
    
    -- Overall Score: Economy (30%) + Livability (35%) + Sustain (20%) + Growth (15%)
    overall_score DOUBLE PRECISION,
    
    -- ═══════════════════════════════════════════════════════════════════════════
    -- SCORE EXPLANATIONS (Human-readable)
    -- ═══════════════════════════════════════════════════════════════════════════
    
    economy_explanation TEXT,
    livability_explanation TEXT,
    sustainability_explanation TEXT,
    growth_explanation TEXT,
    overall_explanation TEXT,
    
    -- ═══════════════════════════════════════════════════════════════════════════
    -- DATA QUALITY METADATA
    -- ═══════════════════════════════════════════════════════════════════════════
    
    -- Percentage of input data available (0-100)
    data_completeness DOUBLE PRECISION NOT NULL DEFAULT 0,
    
    -- Confidence score (0-1) based on data quality
    confidence_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    
    -- Comma-separated list of missing input data
    missing_data TEXT,
    
    -- ═══════════════════════════════════════════════════════════════════════════
    -- RAW INPUT VALUES (Auditing and debugging)
    -- ═══════════════════════════════════════════════════════════════════════════
    
    input_gdp_per_capita DOUBLE PRECISION,
    input_unemployment_rate DOUBLE PRECISION,
    input_cost_of_living INTEGER,
    input_aqi INTEGER,
    input_population BIGINT,
    input_population_growth DOUBLE PRECISION,
    input_gdp_growth DOUBLE PRECISION,
    
    -- ═══════════════════════════════════════════════════════════════════════════
    -- ETL METADATA
    -- ═══════════════════════════════════════════════════════════════════════════
    
    etl_batch_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- ═══════════════════════════════════════════════════════════════════════════
    -- CONSTRAINTS
    -- ═══════════════════════════════════════════════════════════════════════════
    
    -- One row per city per day
    CONSTRAINT uk_features_city_date UNIQUE (city_key, computation_date)
);

-- ═══════════════════════════════════════════════════════════════════════════════
-- INDEXES FOR COMMON QUERY PATTERNS
-- ═══════════════════════════════════════════════════════════════════════════════

-- Cache lookup by city
CREATE INDEX IF NOT EXISTS idx_features_city_key 
    ON city_computed_features(city_key);

-- Daily batch queries
CREATE INDEX IF NOT EXISTS idx_features_date 
    ON city_computed_features(computation_date);

-- Ranking queries (sorted by score descending)
CREATE INDEX IF NOT EXISTS idx_features_overall 
    ON city_computed_features(overall_score DESC NULLS LAST);
    
CREATE INDEX IF NOT EXISTS idx_features_economy 
    ON city_computed_features(economy_score DESC NULLS LAST);
    
CREATE INDEX IF NOT EXISTS idx_features_livability 
    ON city_computed_features(livability_score DESC NULLS LAST);
    
CREATE INDEX IF NOT EXISTS idx_features_growth 
    ON city_computed_features(growth_score DESC NULLS LAST);
    
CREATE INDEX IF NOT EXISTS idx_features_sustainability 
    ON city_computed_features(sustainability_score DESC NULLS LAST);

-- ═══════════════════════════════════════════════════════════════════════════════
-- COMMENTS FOR DOCUMENTATION
-- ═══════════════════════════════════════════════════════════════════════════════

COMMENT ON TABLE city_computed_features IS 
    'Pre-computed city scores for AI feature engineering. Stores deterministic scores derived from city metrics.';

COMMENT ON COLUMN city_computed_features.economy_score IS 
    'Economic health score (0-100). Formula: GDP (40%) + Unemployment (60%). Higher = better economy.';

COMMENT ON COLUMN city_computed_features.livability_score IS 
    'Quality of life score (0-100). Formula: Cost (35%) + AQI (35%) + Size (30%). Higher = more livable.';

COMMENT ON COLUMN city_computed_features.sustainability_score IS 
    'Environmental health score (0-100). Currently based on AQI. Higher = more sustainable.';

COMMENT ON COLUMN city_computed_features.growth_score IS 
    'Growth trajectory score (0-100). Formula: Pop growth (50%) + GDP growth (50%). Higher = faster growth.';

COMMENT ON COLUMN city_computed_features.overall_score IS 
    'Composite score (0-100). Weighted: Economy (30%) + Livability (35%) + Sustain (20%) + Growth (15%).';

COMMENT ON COLUMN city_computed_features.data_completeness IS 
    'Percentage (0-100) of input data that was available for computation.';

COMMENT ON COLUMN city_computed_features.confidence_score IS 
    'Confidence level (0-1) based on data quality, recency, and completeness.';
