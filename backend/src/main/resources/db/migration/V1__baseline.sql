-- FIXED: Baseline schema so Flyway can bootstrap cleanly on a fresh database.

CREATE TABLE IF NOT EXISTS cities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    state VARCHAR(100),
    country VARCHAR(100) NOT NULL,
    population BIGINT NOT NULL,
    gdp_per_capita DOUBLE PRECISION,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    cost_of_living_index INTEGER,
    unemployment_rate DOUBLE PRECISION,
    banner_image_url VARCHAR(500),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_synced_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_city_slug ON cities(slug);
CREATE INDEX IF NOT EXISTS idx_city_country ON cities(country);
CREATE INDEX IF NOT EXISTS idx_city_country_state ON cities(country, state);
CREATE INDEX IF NOT EXISTS idx_city_population ON cities(population);
CREATE INDEX IF NOT EXISTS idx_city_state ON cities(state);

CREATE TABLE IF NOT EXISTS metrics (
    id BIGSERIAL PRIMARY KEY,
    city_id BIGINT NOT NULL REFERENCES cities(id),
    metric_type VARCHAR(50) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(50),
    data_source VARCHAR(200),
    notes TEXT,
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_metrics_city_type_time ON metrics(city_id, metric_type, recorded_at);
CREATE INDEX IF NOT EXISTS idx_metrics_city_id ON metrics(city_id);
CREATE INDEX IF NOT EXISTS idx_metrics_type_time ON metrics(metric_type, recorded_at);
CREATE INDEX IF NOT EXISTS idx_metrics_recorded_at ON metrics(recorded_at);

CREATE TABLE IF NOT EXISTS analytics_events (
    id BIGSERIAL PRIMARY KEY,
    city_id BIGINT REFERENCES cities(id),
    event_type VARCHAR(50) NOT NULL,
    event_value DOUBLE PRECISION,
    metadata TEXT,
    user_id VARCHAR(100),
    session_id VARCHAR(100),
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_city_type_time ON analytics_events(city_id, event_type, event_timestamp);
CREATE INDEX IF NOT EXISTS idx_event_city_id ON analytics_events(city_id);
CREATE INDEX IF NOT EXISTS idx_event_type_time ON analytics_events(event_type, event_timestamp);
CREATE INDEX IF NOT EXISTS idx_event_timestamp ON analytics_events(event_timestamp);
CREATE INDEX IF NOT EXISTS idx_event_user_id ON analytics_events(user_id);
CREATE INDEX IF NOT EXISTS idx_event_session_id ON analytics_events(session_id);

CREATE TABLE IF NOT EXISTS ai_summaries (
    id BIGSERIAL PRIMARY KEY,
    city_id BIGINT NOT NULL UNIQUE REFERENCES cities(id),
    summary_text TEXT NOT NULL,
    summary_length VARCHAR(20),
    ai_model VARCHAR(50),
    confidence_score DOUBLE PRECISION,
    data_points_count INTEGER,
    generated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_summary_generated_at ON ai_summaries(generated_at);
CREATE INDEX IF NOT EXISTS idx_ai_summary_model ON ai_summaries(ai_model);
CREATE INDEX IF NOT EXISTS idx_ai_summary_length ON ai_summaries(summary_length);

CREATE TABLE IF NOT EXISTS city_sections (
    id BIGSERIAL PRIMARY KEY,
    city_id BIGINT NOT NULL REFERENCES cities(id),
    section_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    display_order INTEGER,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_section_city_type UNIQUE (city_id, section_type)
);

CREATE INDEX IF NOT EXISTS idx_section_city_type ON city_sections(city_id, section_type);
CREATE INDEX IF NOT EXISTS idx_section_city_id ON city_sections(city_id);
CREATE INDEX IF NOT EXISTS idx_section_city_published ON city_sections(city_id, published);
CREATE INDEX IF NOT EXISTS idx_section_type ON city_sections(section_type);
