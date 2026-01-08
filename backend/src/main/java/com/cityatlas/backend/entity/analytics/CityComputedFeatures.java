package com.cityatlas.backend.entity.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * CITY_COMPUTED_FEATURES - Persisted AI Feature Engineering Results
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Stores pre-computed city scores for fast retrieval and reuse.
 * Eliminates redundant computation of deterministic features.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * FEATURE CATEGORIES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────────────┬──────────────────────────────────────────────────────┐
 *   │ Feature             │ Description                                           │
 *   ├─────────────────────┼──────────────────────────────────────────────────────┤
 *   │ economy_score       │ GDP per capita (40%) + Unemployment rate (60%)       │
 *   │ livability_score    │ Cost of living (35%) + AQI (35%) + Population (30%)  │
 *   │ sustainability_score│ AQI (100%) - future: carbon, green space             │
 *   │ growth_score        │ Population growth (50%) + GDP growth (50%)           │
 *   │ overall_score       │ Economy (30%) + Livability (35%) + Sustain. (20%)    │
 *   │                     │ + Growth (15%)                                        │
 *   └─────────────────────┴──────────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * GRAIN DEFINITION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * GRAIN: One row per city per computation date
 * 
 * Features are computed daily to capture AQI changes and metric updates.
 * Historical records allow trend analysis of score evolution.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * RECOMPUTATION STRATEGY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   1. DAILY BATCH: Scheduled job recomputes all city features at 3:00 AM
 *   2. ON-DEMAND: Triggered when city metrics are updated via API
 *   3. CACHE-ASIDE: Compute if not in cache, store for next request
 * 
 * TTL: 24 hours (features are valid for one day before recomputation)
 */
@Entity
@Table(
    name = "city_computed_features",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_features_city_date",
            columnNames = {"city_key", "computation_date"}
        )
    },
    indexes = {
        @Index(name = "idx_features_city_key", columnList = "city_key"),
        @Index(name = "idx_features_date", columnList = "computation_date"),
        @Index(name = "idx_features_overall", columnList = "overall_score DESC"),
        @Index(name = "idx_features_economy", columnList = "economy_score DESC"),
        @Index(name = "idx_features_livability", columnList = "livability_score DESC"),
        @Index(name = "idx_features_growth", columnList = "growth_score DESC")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "dimCity")
public class CityComputedFeatures {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIMENSION RELATIONSHIP
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Reference to city dimension table.
     * Allows historical tracking when city attributes change (SCD Type 2).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_key", nullable = false)
    private DimCity dimCity;
    
    /**
     * Date when features were computed.
     * Combined with city_key forms the unique business key.
     */
    @Column(name = "computation_date", nullable = false)
    private LocalDate computationDate;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMPUTED SCORES (0-100 scale, nullable if insufficient data)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * ECONOMY SCORE (0-100)
     * 
     * FORMULA:
     *   economy_score = (gdp_normalized * 0.40) + (unemployment_normalized * 0.60)
     * 
     * INPUTS:
     *   - GDP per capita: Min-max normalized ($15K - $150K → 0-100)
     *   - Unemployment rate: Inverse normalized (2% - 15% → 100-0)
     * 
     * INTERPRETATION:
     *   80-100: Excellent - Strong job market, high prosperity
     *   60-79:  Good - Healthy economy with opportunities
     *   40-59:  Moderate - Mixed economic indicators
     *   20-39:  Challenged - Limited opportunities
     *   0-19:   Struggling - Significant economic distress
     */
    @Column(name = "economy_score")
    private Double economyScore;
    
    /**
     * LIVABILITY SCORE (0-100)
     * 
     * FORMULA:
     *   livability = (cost_inv * 0.35) + (aqi_inv * 0.35) + (pop_log_inv * 0.30)
     * 
     * INPUTS:
     *   - Cost of living index: Inverse normalized (70-180 → 100-0)
     *   - AQI: Inverse normalized (0-200 → 100-0)
     *   - Population: Log-scale inverse (50K-10M → 100-0, smaller = better)
     * 
     * INTERPRETATION:
     *   80-100: Highly livable - Affordable, clean, manageable size
     *   60-79:  Good quality of life
     *   40-59:  Average with trade-offs
     *   20-39:  Challenging due to cost, pollution, or density
     *   0-19:   Significant livability challenges
     */
    @Column(name = "livability_score")
    private Double livabilityScore;
    
    /**
     * SUSTAINABILITY SCORE (0-100)
     * 
     * FORMULA:
     *   sustainability = aqi_normalized * 100
     * 
     * INPUTS (current):
     *   - AQI: Inverse normalized (0-200 → 100-0)
     * 
     * FUTURE INPUTS:
     *   - Carbon emissions per capita
     *   - Green space percentage
     *   - Renewable energy adoption
     * 
     * INTERPRETATION:
     *   80-100: Excellent environmental conditions
     *   60-79:  Good air quality
     *   40-59:  Moderate - Acceptable for most
     *   20-39:  Unhealthy for sensitive groups
     *   0-19:   Poor - Health concerns for all
     */
    @Column(name = "sustainability_score")
    private Double sustainabilityScore;
    
    /**
     * GROWTH SCORE (0-100)
     * 
     * FORMULA:
     *   growth = (pop_growth_norm * 0.50) + (gdp_growth_norm * 0.50)
     * 
     * INPUTS:
     *   - Population growth rate: Min-max normalized (-2% to +5% → 0-100)
     *   - GDP growth rate: Min-max normalized (-5% to +10% → 0-100)
     * 
     * INTERPRETATION:
     *   80-100: Rapidly growing - High investment potential
     *   60-79:  Healthy growth - Expanding economy and population
     *   40-59:  Stable - Moderate or stagnant growth
     *   20-39:  Declining - Shrinking population or economy
     *   0-19:   Contracting - Significant decline
     */
    @Column(name = "growth_score")
    private Double growthScore;
    
    /**
     * OVERALL SCORE (0-100)
     * 
     * FORMULA:
     *   overall = (economy * 0.30) + (livability * 0.35) 
     *           + (sustainability * 0.20) + (growth * 0.15)
     * 
     * WEIGHTS RATIONALE:
     *   - Livability (35%): Most important for residents
     *   - Economy (30%): Critical for job seekers and businesses
     *   - Sustainability (20%): Long-term health and environment
     *   - Growth (15%): Future potential and investment attractiveness
     */
    @Column(name = "overall_score")
    private Double overallScore;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCORE EXPLANATIONS (Human-readable)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Human-readable explanation of economy score.
     * Example: "Strong GDP ($85K) offset by moderate unemployment (4.2%)"
     */
    @Column(name = "economy_explanation", columnDefinition = "TEXT")
    private String economyExplanation;
    
    /**
     * Human-readable explanation of livability score.
     * Example: "Affordable (index 95) with good air quality (AQI 35)"
     */
    @Column(name = "livability_explanation", columnDefinition = "TEXT")
    private String livabilityExplanation;
    
    /**
     * Human-readable explanation of sustainability score.
     * Example: "Excellent air quality (AQI 25) indicates strong sustainability"
     */
    @Column(name = "sustainability_explanation", columnDefinition = "TEXT")
    private String sustainabilityExplanation;
    
    /**
     * Human-readable explanation of growth score.
     * Example: "Population growing 2.5% YoY with GDP growth of 4.1%"
     */
    @Column(name = "growth_explanation", columnDefinition = "TEXT")
    private String growthExplanation;
    
    /**
     * Human-readable explanation of overall score.
     * Example: "Well-balanced city with strong economy and good livability"
     */
    @Column(name = "overall_explanation", columnDefinition = "TEXT")
    private String overallExplanation;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DATA QUALITY METADATA
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Percentage of input data that was available (0-100).
     * Low completeness indicates estimates or missing data.
     */
    @Column(name = "data_completeness", nullable = false)
    private Double dataCompleteness;
    
    /**
     * Confidence score for the computed features (0-1).
     * Based on data recency, source reliability, and completeness.
     */
    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;
    
    /**
     * Comma-separated list of missing input data.
     * Example: "GDP per capita,Population growth"
     */
    @Column(name = "missing_data", columnDefinition = "TEXT")
    private String missingData;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RAW INPUT VALUES (Auditing and debugging)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * GDP per capita used for computation (USD).
     */
    @Column(name = "input_gdp_per_capita")
    private Double inputGdpPerCapita;
    
    /**
     * Unemployment rate used for computation (%).
     */
    @Column(name = "input_unemployment_rate")
    private Double inputUnemploymentRate;
    
    /**
     * Cost of living index used for computation.
     */
    @Column(name = "input_cost_of_living")
    private Integer inputCostOfLiving;
    
    /**
     * AQI used for computation.
     */
    @Column(name = "input_aqi")
    private Integer inputAqi;
    
    /**
     * Population used for computation.
     */
    @Column(name = "input_population")
    private Long inputPopulation;
    
    /**
     * Population growth rate used for computation (%).
     */
    @Column(name = "input_population_growth")
    private Double inputPopulationGrowth;
    
    /**
     * GDP growth rate used for computation (%).
     */
    @Column(name = "input_gdp_growth")
    private Double inputGdpGrowth;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ETL METADATA
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Batch ID from ETL process that created this record.
     * Enables batch-level debugging and rollback.
     */
    @Column(name = "etl_batch_id", length = 100)
    private String etlBatchId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if features are stale (older than 24 hours).
     */
    public boolean isStale() {
        return computationDate.isBefore(LocalDate.now());
    }
    
    /**
     * Check if this record has high data quality.
     */
    public boolean isHighQuality() {
        return dataCompleteness >= 80.0 && confidenceScore >= 0.8;
    }
    
    /**
     * Get the score tier (excellent/good/average/below-average/poor).
     */
    public String getOverallTier() {
        if (overallScore == null) return "unavailable";
        if (overallScore >= 80) return "excellent";
        if (overallScore >= 60) return "good";
        if (overallScore >= 40) return "average";
        if (overallScore >= 20) return "below-average";
        return "poor";
    }
}
