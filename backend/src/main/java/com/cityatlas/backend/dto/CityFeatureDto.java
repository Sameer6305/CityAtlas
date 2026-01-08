package com.cityatlas.backend.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * CITY FEATURE DTO - Clean, Reusable Feature Object
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * API response object for city features. Provides a clean, serializable
 * representation of computed scores for frontend consumption.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * FEATURE CATEGORIES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────────────┬──────────────────────────────────────────────────────┐
 *   │ Feature             │ Description                                           │
 *   ├─────────────────────┼──────────────────────────────────────────────────────┤
 *   │ economyScore        │ GDP per capita (40%) + Unemployment rate (60%)       │
 *   │ livabilityScore     │ Cost of living (35%) + AQI (35%) + Population (30%)  │
 *   │ sustainabilityScore │ AQI (100%) - future: carbon, green space             │
 *   │ growthScore         │ Population growth (50%) + GDP growth (50%)           │
 *   │ overallScore        │ Economy (30%) + Livability (35%) + Sustain (20%)     │
 *   │                     │ + Growth (15%)                                        │
 *   └─────────────────────┴──────────────────────────────────────────────────────┘
 * 
 * @see com.cityatlas.backend.service.CityFeatureComputer
 * @see com.cityatlas.backend.service.CityFeatureStore
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CityFeatureDto {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // IDENTIFICATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * City slug (URL-friendly identifier).
     * Example: "san-francisco", "new-york"
     */
    private String citySlug;
    
    /**
     * City display name.
     * Example: "San Francisco", "New York"
     */
    private String cityName;
    
    /**
     * Date when features were computed.
     * Used for freshness checks and historical analysis.
     */
    private LocalDate computationDate;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMPUTED SCORES (0-100 scale)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Economy score (0-100).
     * 
     * FORMULA:
     *   economy_score = (gdp_normalized * 0.40) + (unemployment_normalized * 0.60)
     * 
     * INTERPRETATION:
     *   80-100: Excellent - Strong job market, high prosperity
     *   60-79:  Good - Healthy economy with opportunities
     *   40-59:  Moderate - Mixed economic indicators
     *   20-39:  Challenged - Limited opportunities
     *   0-19:   Struggling - Significant economic distress
     */
    private ScoreDto economy;
    
    /**
     * Livability score (0-100).
     * 
     * FORMULA:
     *   livability = (cost_inv * 0.35) + (aqi_inv * 0.35) + (pop_log_inv * 0.30)
     * 
     * INTERPRETATION:
     *   80-100: Highly livable - Affordable, clean, manageable size
     *   60-79:  Good quality of life
     *   40-59:  Average with trade-offs
     *   20-39:  Challenging due to cost, pollution, or density
     *   0-19:   Significant livability challenges
     */
    private ScoreDto livability;
    
    /**
     * Sustainability score (0-100).
     * 
     * FORMULA (current):
     *   sustainability = aqi_normalized * 100
     * 
     * FUTURE:
     *   Will include carbon emissions, green space, renewable energy
     * 
     * INTERPRETATION:
     *   80-100: Excellent environmental conditions
     *   60-79:  Good air quality
     *   40-59:  Moderate - Acceptable for most
     *   20-39:  Unhealthy for sensitive groups
     *   0-19:   Poor - Health concerns for all
     */
    private ScoreDto sustainability;
    
    /**
     * Growth score (0-100).
     * 
     * FORMULA:
     *   growth = (pop_growth_norm * 0.50) + (gdp_growth_norm * 0.50)
     * 
     * INTERPRETATION:
     *   80-100: Rapidly growing - High investment potential
     *   60-79:  Healthy growth - Expanding economy and population
     *   40-59:  Stable - Moderate or stagnant growth
     *   20-39:  Declining - Shrinking population or economy
     *   0-19:   Contracting - Significant decline
     */
    private ScoreDto growth;
    
    /**
     * Overall score (0-100).
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
    private ScoreDto overall;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DATA QUALITY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Percentage of input data available (0-100).
     * Low completeness indicates estimates or missing data.
     */
    private Double dataCompleteness;
    
    /**
     * Confidence level (0-1) for the computed scores.
     * Based on data recency, source reliability, and completeness.
     */
    private Double confidenceScore;
    
    /**
     * List of missing input data.
     * Example: ["GDP growth rate", "Population growth rate"]
     */
    private List<String> missingData;
    
    /**
     * Whether the features are stale (older than 24 hours).
     */
    private Boolean isStale;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCORE DTO - Individual Score with Explanation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Individual score with value, tier, and explanation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScoreDto {
        
        /**
         * Numeric score (0-100 scale).
         * Null if insufficient data to compute.
         */
        private Double value;
        
        /**
         * Score tier classification.
         * Values: "excellent", "good", "average", "below-average", "poor", "unavailable"
         */
        private String tier;
        
        /**
         * Human-readable explanation.
         * Example: "Strong GDP ($85K) offset by moderate unemployment (4.2%)"
         */
        private String explanation;
        
        /**
         * Breakdown of contributing factors.
         * Example: ["GDP per capita: $85,000 (prosperous, contributes 32 points)",
         *           "Unemployment: 4.2% (healthy job market, contributes 48 points)"]
         */
        private List<String> components;
        
        /**
         * Confidence level (0-1) for this specific score.
         */
        private Double confidence;
        
        /**
         * Create a score DTO from value and explanation.
         */
        public static ScoreDto of(Double value, String explanation) {
            return ScoreDto.builder()
                    .value(value)
                    .tier(computeTier(value))
                    .explanation(explanation)
                    .build();
        }
        
        /**
         * Create a full score DTO with all fields.
         */
        public static ScoreDto full(Double value, String explanation, 
                                     List<String> components, Double confidence) {
            return ScoreDto.builder()
                    .value(value)
                    .tier(computeTier(value))
                    .explanation(explanation)
                    .components(components)
                    .confidence(confidence)
                    .build();
        }
        
        /**
         * Compute tier from score value.
         */
        private static String computeTier(Double score) {
            if (score == null) return "unavailable";
            if (score >= 80) return "excellent";
            if (score >= 60) return "good";
            if (score >= 40) return "average";
            if (score >= 20) return "below-average";
            return "poor";
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONVENIENCE METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if all core scores are available.
     */
    public boolean hasAllScores() {
        return economy != null && economy.getValue() != null
            && livability != null && livability.getValue() != null
            && sustainability != null && sustainability.getValue() != null
            && overall != null && overall.getValue() != null;
    }
    
    /**
     * Check if this is high quality data.
     */
    public boolean isHighQuality() {
        return dataCompleteness != null && dataCompleteness >= 80.0
            && confidenceScore != null && confidenceScore >= 0.8;
    }
    
    /**
     * Get the overall tier for quick categorization.
     */
    public String getOverallTier() {
        return overall != null ? overall.getTier() : "unavailable";
    }
}
