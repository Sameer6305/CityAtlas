package com.cityatlas.backend.entity.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.cityatlas.backend.entity.City;

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
 * DIM_CITY - City Dimension Table (Star Schema)
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * DIMENSION TABLE ROLE:
 * This is a dimension table that provides descriptive context for fact tables.
 * It answers the WHO/WHAT/WHERE questions in analytics queries.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY A SEPARATE DIMENSION TABLE?
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * The existing 'cities' table (OLTP) is normalized for transactional operations.
 * This dimension table is DENORMALIZED for analytics performance:
 * 
 *   ┌──────────────────────────────────────────────────────────────────────┐
 *   │                    OLTP vs OLAP Comparison                          │
 *   ├────────────────────┬────────────────────────────────────────────────┤
 *   │ OLTP (cities)      │ OLAP (dim_city)                                │
 *   ├────────────────────┼────────────────────────────────────────────────┤
 *   │ Normalized (3NF)   │ Denormalized (flat)                            │
 *   │ Current state only │ Historical versions (SCD Type 2)               │
 *   │ Minimal columns    │ Derived attributes included                    │
 *   │ Frequent updates   │ Batch updates (ETL)                            │
 *   │ ACID transactions  │ Append-mostly                                  │
 *   └────────────────────┴────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * SLOWLY CHANGING DIMENSION (SCD TYPE 2)
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This table implements SCD Type 2 for tracking historical changes:
 * 
 * Example: If San Francisco's population changes significantly:
 * 
 *   id │ city_slug      │ population │ valid_from │ valid_to   │ is_current
 *   ───┼────────────────┼────────────┼────────────┼────────────┼───────────
 *   1  │ san-francisco  │ 870,000    │ 2024-01-01 │ 2025-06-30 │ false
 *   2  │ san-francisco  │ 885,000    │ 2025-07-01 │ 9999-12-31 │ true
 * 
 * This allows queries like:
 * - "Show metrics as of last year" → JOIN on valid_from/valid_to
 * - "Show current metrics" → WHERE is_current = true
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * DERIVED ATTRIBUTES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Pre-computed attributes avoid repeated calculations in queries:
 * 
 *   ┌─────────────────────────┬─────────────────────────────────────────────┐
 *   │ Derived Attribute       │ Computation                                 │
 *   ├─────────────────────────┼─────────────────────────────────────────────┤
 *   │ city_size_category      │ SMALL (<100K), MEDIUM (100K-1M), LARGE (>1M)│
 *   │ region                  │ Mapped from country + state                 │
 *   │ gdp_tier                │ LOW, MEDIUM, HIGH based on gdp_per_capita   │
 *   │ population_density_tier │ Computed from population / area             │
 *   └─────────────────────────┴─────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * EXAMPLE QUERY USAGE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * -- Compare AQI across city size categories
 * SELECT dc.city_size_category,
 *        AVG(fm.metric_value) as avg_aqi,
 *        COUNT(DISTINCT dc.id) as city_count
 * FROM fact_city_metrics fm
 * JOIN dim_city dc ON fm.dim_city_id = dc.id AND dc.is_current = true
 * WHERE fm.metric_type = 'AQI'
 * GROUP BY dc.city_size_category;
 * 
 * @see FactCityMetrics
 * @see FactUserEventsDaily
 */
@Entity
@Table(name = "dim_city", 
    indexes = {
        // Primary lookup patterns
        @Index(name = "idx_dim_city_slug", columnList = "city_slug"),
        @Index(name = "idx_dim_city_current", columnList = "is_current"),
        @Index(name = "idx_dim_city_slug_current", columnList = "city_slug, is_current"),
        
        // Analytics filtering patterns
        @Index(name = "idx_dim_city_size", columnList = "city_size_category"),
        @Index(name = "idx_dim_city_region", columnList = "region"),
        @Index(name = "idx_dim_city_country", columnList = "country"),
        @Index(name = "idx_dim_city_gdp_tier", columnList = "gdp_tier"),
        
        // SCD Type 2 temporal queries
        @Index(name = "idx_dim_city_valid_range", columnList = "valid_from, valid_to"),
        
        // Foreign key to OLTP table
        @Index(name = "idx_dim_city_source_id", columnList = "source_city_id")
    },
    uniqueConstraints = {
        // Each city can only have one current version
        @UniqueConstraint(name = "uq_dim_city_slug_current", 
                         columnNames = {"city_slug", "is_current"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class DimCity {
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * SURROGATE KEY
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Analytics dimension tables use surrogate keys (auto-generated IDs) instead
     * of natural keys (like city_slug) because:
     * 
     * 1. Handles SCD Type 2 - Multiple rows per city for historical versions
     * 2. Faster JOINs - Integer comparison vs string comparison
     * 3. Decoupling - Independent from OLTP primary key changes
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Foreign key reference to OLTP 'cities' table.
     * Used for ETL synchronization - NOT for analytics queries.
     * Analytics queries should use dim_city.id as the join key.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_city_id")
    private City sourceCity;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CITY ATTRIBUTES (Denormalized from OLTP)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * City URL slug (e.g., "san-francisco")
     * Denormalized from cities.slug for query convenience
     */
    @Column(name = "city_slug", nullable = false, length = 100)
    private String citySlug;
    
    /**
     * City display name (e.g., "San Francisco")
     */
    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;
    
    /**
     * State or province
     */
    @Column(length = 100)
    private String state;
    
    /**
     * Country name
     */
    @Column(nullable = false, length = 100)
    private String country;
    
    /**
     * Population at the time this dimension record was created
     */
    @Column(nullable = false)
    private Long population;
    
    /**
     * GDP per capita in USD at snapshot time
     */
    @Column(name = "gdp_per_capita")
    private Double gdpPerCapita;
    
    /**
     * Latitude coordinate
     */
    @Column
    private Double latitude;
    
    /**
     * Longitude coordinate
     */
    @Column
    private Double longitude;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DERIVED ATTRIBUTES (Pre-computed for analytics)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * City size classification based on population.
     * Pre-computed to avoid CASE statements in every query.
     * 
     * Classification:
     * - SMALL: population < 100,000
     * - MEDIUM: population 100,000 - 1,000,000
     * - LARGE: population > 1,000,000
     * - MEGA: population > 10,000,000
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "city_size_category", length = 20)
    private CitySizeCategory citySizeCategory;
    
    /**
     * Geographic region for regional analytics.
     * Derived from country + state mapping.
     * 
     * Examples: "North America", "Western Europe", "East Asia"
     */
    @Column(length = 50)
    private String region;
    
    /**
     * Economic tier based on GDP per capita.
     * 
     * Classification:
     * - LOW: gdp_per_capita < 20,000
     * - MEDIUM: gdp_per_capita 20,000 - 50,000
     * - HIGH: gdp_per_capita > 50,000
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gdp_tier", length = 20)
    private GdpTier gdpTier;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCD TYPE 2 FIELDS (Slowly Changing Dimension)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Start date when this dimension version became effective.
     * For initial load, this is typically the system go-live date.
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;
    
    /**
     * End date when this dimension version expires.
     * Active records use '9999-12-31' as the "forever" sentinel value.
     */
    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;
    
    /**
     * Flag indicating if this is the current/active version of the city.
     * Only ONE row per city_slug should have is_current = true.
     * 
     * This denormalized flag allows simple queries:
     *   WHERE is_current = true
     * Instead of:
     *   WHERE CURRENT_DATE BETWEEN valid_from AND valid_to
     */
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // AUDIT FIELDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Timestamp when this dimension row was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this dimension row was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENUM DEFINITIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * City size classification enum
     */
    public enum CitySizeCategory {
        SMALL,      // < 100K population
        MEDIUM,     // 100K - 1M population  
        LARGE,      // 1M - 10M population
        MEGA        // > 10M population
    }
    
    /**
     * GDP tier classification enum
     */
    public enum GdpTier {
        LOW,        // < $20K per capita
        MEDIUM,     // $20K - $50K per capita
        HIGH        // > $50K per capita
    }
}
