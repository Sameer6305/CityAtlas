package com.cityatlas.backend.entity.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.cityatlas.backend.entity.MetricType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * FACT_CITY_METRICS - City Metrics Fact Table (Star Schema)
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * FACT TABLE ROLE:
 * This is the central fact table for city performance analytics.
 * It stores daily snapshots of city metrics with linkage to dimension tables.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * GRAIN DEFINITION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * GRAIN: One row per city per metric type per day
 * 
 * The grain is the most important decision in fact table design:
 * 
 *   ┌────────────────────────────────────────────────────────────────────────┐
 *   │  Primary Key (Composite Business Key)                                  │
 *   │                                                                         │
 *   │  dim_city_id + metric_type + metric_date = UNIQUE ROW                  │
 *   │                                                                         │
 *   │  Example:                                                               │
 *   │  ┌───────────┬────────────┬────────────┬─────────────┐                │
 *   │  │ dim_city  │ metric_type│ metric_date│ metric_value│                │
 *   │  ├───────────┼────────────┼────────────┼─────────────┤                │
 *   │  │ 1 (NYC)   │ AQI        │ 2026-01-01 │ 45.2        │                │
 *   │  │ 1 (NYC)   │ AQI        │ 2026-01-02 │ 52.1        │                │
 *   │  │ 1 (NYC)   │ GDP        │ 2026-01-01 │ 85000       │                │
 *   │  │ 2 (LA)    │ AQI        │ 2026-01-01 │ 78.5        │                │
 *   │  └───────────┴────────────┴────────────┴─────────────┘                │
 *   └────────────────────────────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * FACT TABLE DESIGN PRINCIPLES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. ADDITIVE MEASURES
 *    Metrics that can be summed across dimensions:
 *    - metric_value (SUM across time for totals)
 *    - metric_value_delta (SUM for net change)
 * 
 * 2. SEMI-ADDITIVE MEASURES
 *    Metrics that can only be summed across some dimensions:
 *    - population (sum across cities, but NOT across time)
 *    - percentile_rank (average makes sense, sum doesn't)
 * 
 * 3. NON-ADDITIVE MEASURES
 *    Metrics that cannot be summed:
 *    - ratios, percentages (use weighted averages instead)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * RELATIONSHIP TO OLTP TABLES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   OLTP (Transactional)              OLAP (Analytical)
 *   ────────────────────              ─────────────────
 *   
 *   ┌─────────────┐                   ┌─────────────────────┐
 *   │   cities    │                   │      dim_city       │
 *   │ (current)   │───── ETL ────────▶│   (historical)      │
 *   └─────────────┘                   └──────────┬──────────┘
 *                                                │
 *   ┌─────────────┐                              │ FK
 *   │   metrics   │                              ▼
 *   │ (raw data)  │───── ETL ────────▶┌─────────────────────┐
 *   └─────────────┘                   │  fact_city_metrics  │
 *                                     │  (daily snapshots)  │
 *                                     └─────────────────────┘
 * 
 * The OLTP 'metrics' table contains raw measurements.
 * This fact table contains daily aggregated snapshots with analytics context.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * EXAMPLE ANALYTICS QUERIES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * -- 1. AQI trend for a city over 30 days
 * SELECT metric_date, metric_value, metric_value_previous,
 *        metric_value - metric_value_previous as daily_change
 * FROM fact_city_metrics f
 * JOIN dim_city dc ON f.dim_city_id = dc.id AND dc.is_current = true
 * WHERE dc.city_slug = 'new-york'
 *   AND f.metric_type = 'AQI'
 *   AND f.metric_date >= CURRENT_DATE - INTERVAL '30 days'
 * ORDER BY f.metric_date;
 * 
 * -- 2. Compare cities by GDP percentile
 * SELECT dc.city_name, f.metric_value as gdp,
 *        f.percentile_rank as gdp_percentile
 * FROM fact_city_metrics f
 * JOIN dim_city dc ON f.dim_city_id = dc.id AND dc.is_current = true
 * WHERE f.metric_type = 'GDP_PER_CAPITA'
 *   AND f.metric_date = CURRENT_DATE
 * ORDER BY f.percentile_rank DESC;
 * 
 * -- 3. Month-over-month metric change by region
 * SELECT dc.region, f.metric_type,
 *        SUM(CASE WHEN f.metric_date >= DATE_TRUNC('month', CURRENT_DATE)
 *                 THEN f.metric_value END) as current_month,
 *        SUM(CASE WHEN f.metric_date < DATE_TRUNC('month', CURRENT_DATE)
 *                 THEN f.metric_value END) as previous_month
 * FROM fact_city_metrics f
 * JOIN dim_city dc ON f.dim_city_id = dc.id AND dc.is_current = true
 * WHERE f.metric_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 month'
 * GROUP BY dc.region, f.metric_type;
 * 
 * @see DimCity
 * @see com.cityatlas.backend.entity.Metrics
 */
@Entity
@Table(name = "fact_city_metrics",
    indexes = {
        // ═══════════════════════════════════════════════════════════════════════
        // PRIMARY ACCESS PATTERNS
        // ═══════════════════════════════════════════════════════════════════════
        
        // Time-series query: Get metric history for a city
        // SELECT * FROM fact_city_metrics WHERE dim_city_id = ? AND metric_type = ? ORDER BY metric_date
        @Index(name = "idx_fact_metrics_city_type_date", 
               columnList = "dim_city_id, metric_type, metric_date"),
        
        // Dashboard query: Get all metrics for a city on a date
        // SELECT * FROM fact_city_metrics WHERE dim_city_id = ? AND metric_date = ?
        @Index(name = "idx_fact_metrics_city_date", 
               columnList = "dim_city_id, metric_date"),
        
        // Cross-city comparison: Get metric for all cities on a date
        // SELECT * FROM fact_city_metrics WHERE metric_type = ? AND metric_date = ?
        @Index(name = "idx_fact_metrics_type_date", 
               columnList = "metric_type, metric_date"),
        
        // ═══════════════════════════════════════════════════════════════════════
        // AGGREGATION PATTERNS
        // ═══════════════════════════════════════════════════════════════════════
        
        // Date-based aggregations and partitioning
        @Index(name = "idx_fact_metrics_date", 
               columnList = "metric_date"),
        
        // Ranking and percentile queries
        @Index(name = "idx_fact_metrics_percentile", 
               columnList = "metric_type, percentile_rank")
    },
    uniqueConstraints = {
        // Enforce grain: one row per city per metric per day
        @UniqueConstraint(name = "uq_fact_metrics_grain",
                         columnNames = {"dim_city_id", "metric_type", "metric_date"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "dimCity")
public class FactCityMetrics {
    
    /**
     * Surrogate primary key.
     * Fact tables can have large row counts, so use BIGINT (Long) for headroom.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIMENSION FOREIGN KEYS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Foreign key to dim_city dimension table.
     * 
     * IMPORTANT: Join to dim_city, NOT to the OLTP 'cities' table.
     * This allows historical queries using SCD Type 2 versioning.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dim_city_id", nullable = false)
    private DimCity dimCity;
    
    /**
     * Date dimension - the day this metric was recorded.
     * 
     * In a full data warehouse, this would be a FK to a dim_date table
     * containing day-of-week, quarter, fiscal year, holidays, etc.
     * For simplicity, we use a date field with the grain enforced.
     */
    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEGENERATE DIMENSION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Type of metric being measured.
     * 
     * This is a "degenerate dimension" - a dimension attribute stored directly
     * in the fact table rather than in a separate dimension table.
     * Used when the dimension has no additional attributes worth storing.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private MetricType metricType;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MEASURES (The actual values we're tracking)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * PRIMARY MEASURE: Current metric value
     * 
     * The main measurement for this fact row.
     * Additive across cities, semi-additive across time.
     */
    @Column(name = "metric_value", nullable = false)
    private Double metricValue;
    
    /**
     * Previous day's metric value.
     * 
     * Pre-computed for common delta calculations:
     *   daily_change = metric_value - metric_value_previous
     *   pct_change = (metric_value - metric_value_previous) / metric_value_previous
     * 
     * Storing this avoids expensive self-joins in queries.
     */
    @Column(name = "metric_value_previous")
    private Double metricValuePrevious;
    
    /**
     * Pre-computed change from previous day.
     * 
     * metric_value_delta = metric_value - metric_value_previous
     * 
     * Stored for query convenience - allows SUM(metric_value_delta) for net change.
     */
    @Column(name = "metric_value_delta")
    private Double metricValueDelta;
    
    /**
     * Percentile rank among all cities for this metric on this date.
     * 
     * Range: 0.0 to 1.0 (0 = lowest, 1 = highest)
     * 
     * Pre-computed during ETL to enable fast ranking queries:
     *   "Show me cities in the top 10% for AQI"
     *   WHERE percentile_rank >= 0.90
     */
    @Column(name = "percentile_rank")
    private Double percentileRank;
    
    /**
     * Unit of measurement for context.
     * Examples: "index", "USD", "percentage", "count"
     */
    @Column(length = 50)
    private String unit;
    
    /**
     * Data quality score (0-100).
     * 
     * Indicates confidence in the metric value:
     * - 100: Direct measurement from verified source
     * - 75: Interpolated from recent data
     * - 50: Estimated/modeled value
     * - 25: Stale data (>7 days old)
     * 
     * Allows filtering: WHERE data_quality_score >= 75
     */
    @Column(name = "data_quality_score")
    private Integer dataQualityScore;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ETL METADATA
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Original source of this metric data.
     * Examples: "OpenAQ", "Census Bureau", "World Bank"
     */
    @Column(name = "data_source", length = 200)
    private String dataSource;
    
    /**
     * Timestamp when this fact row was loaded/created by ETL.
     */
    @CreationTimestamp
    @Column(name = "etl_loaded_at", nullable = false, updatable = false)
    private LocalDateTime etlLoadedAt;
    
    /**
     * ETL batch identifier for tracking and debugging.
     * Format: "YYYYMMDD_HHMMSS" or UUID
     */
    @Column(name = "etl_batch_id", length = 50)
    private String etlBatchId;
}
