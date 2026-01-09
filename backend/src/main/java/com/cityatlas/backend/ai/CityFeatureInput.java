package com.cityatlas.backend.ai;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * STRUCTURED FEATURE INPUT FOR AI PROMPTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Provides a standardized, typed input structure for all AI prompt templates.
 * Ensures deterministic behavior by requiring explicit feature values.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * DESIGN PRINCIPLES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. TYPE SAFETY: All inputs are strongly typed (no raw strings)
 * 2. EXPLICIT NULLABILITY: Missing data is explicitly marked
 * 3. NORMALIZED VALUES: Scores are 0-100 for consistency
 * 4. AUDIT TRAIL: Input values can be traced to output
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * USAGE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * <pre>
 * CityFeatureInput input = CityFeatureInput.builder()
 *     .cityIdentifier(CityIdentifier.builder()
 *         .slug("san-francisco")
 *         .name("San Francisco")
 *         .country("USA")
 *         .build())
 *     .economyFeatures(EconomyFeatures.builder()
 *         .gdpPerCapita(95000.0)
 *         .unemploymentRate(3.2)
 *         .economyScore(82.0)
 *         .build())
 *     .build();
 * 
 * String prompt = PromptTemplates.CITY_PERSONALITY.render(input);
 * </pre>
 * 
 * @see PromptTemplates
 * @see PromptConstraints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CityFeatureInput {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CITY IDENTIFICATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * City identification details.
     */
    private CityIdentifier cityIdentifier;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FEATURE CATEGORIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Economic metrics and scores.
     */
    private EconomyFeatures economyFeatures;
    
    /**
     * Livability metrics and scores.
     */
    private LivabilityFeatures livabilityFeatures;
    
    /**
     * Environmental/sustainability metrics and scores.
     */
    private SustainabilityFeatures sustainabilityFeatures;
    
    /**
     * Growth trajectory metrics and scores.
     */
    private GrowthFeatures growthFeatures;
    
    /**
     * Overall computed assessment.
     */
    private OverallAssessment overallAssessment;
    
    /**
     * Data quality metadata.
     */
    private DataQualityMetadata dataQuality;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NESTED CLASSES - City Identifier
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityIdentifier {
        private String slug;
        private String name;
        private String state;
        private String country;
        private Long population;
        private String sizeCategory; // "major", "mid-sized", "small"
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NESTED CLASSES - Economy Features
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EconomyFeatures {
        // Raw values
        private Double gdpPerCapita;
        private Double unemploymentRate;
        private Integer costOfLivingIndex;
        
        // Computed score (0-100)
        private Double economyScore;
        private String economyTier; // "excellent", "good", "average", "below-average", "poor"
        
        // Explanation
        private String explanation;
        private List<String> components;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NESTED CLASSES - Livability Features
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LivabilityFeatures {
        // Raw values
        private Integer aqiIndex;
        private Integer costOfLivingIndex;
        private Long population;
        
        // Computed score (0-100)
        private Double livabilityScore;
        private String livabilityTier;
        
        // Explanation
        private String explanation;
        private List<String> components;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NESTED CLASSES - Sustainability Features
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SustainabilityFeatures {
        // Raw values
        private Integer aqiIndex;
        private String aqiCategory; // "good", "moderate", "unhealthy", etc.
        
        // Computed score (0-100)
        private Double sustainabilityScore;
        private String sustainabilityTier;
        
        // Explanation
        private String explanation;
        private List<String> components;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NESTED CLASSES - Growth Features
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthFeatures {
        // Raw values (nullable if not available)
        private Double populationGrowthRate;
        private Double gdpGrowthRate;
        
        // Computed score (0-100)
        private Double growthScore;
        private String growthTier;
        
        // Explanation
        private String explanation;
        private List<String> components;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NESTED CLASSES - Overall Assessment
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallAssessment {
        // Final score (0-100)
        private Double overallScore;
        private String overallTier;
        
        // Weight breakdown
        private Map<String, Double> weights; // e.g., {"economy": 0.30, "livability": 0.35}
        
        // Explanation
        private String explanation;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NESTED CLASSES - Data Quality Metadata
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataQualityMetadata {
        /**
         * Percentage of data available (0-100).
         */
        private Double completenessPercentage;
        
        /**
         * List of missing data fields.
         */
        private List<String> missingFields;
        
        /**
         * Data freshness indicator.
         */
        private String freshnessCategory; // "fresh", "stale", "outdated"
        
        /**
         * Confidence level in the assessment (0-1).
         */
        private Double confidenceScore;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Validate that required fields are present.
     * 
     * @throws IllegalStateException if required fields are missing
     */
    public void validate() {
        if (cityIdentifier == null || cityIdentifier.getSlug() == null) {
            throw new IllegalStateException("City identifier with slug is required");
        }
        if (cityIdentifier.getName() == null) {
            throw new IllegalStateException("City name is required");
        }
    }
    
    /**
     * Get overall data completeness for this input.
     */
    public double getDataCompleteness() {
        if (dataQuality != null && dataQuality.getCompletenessPercentage() != null) {
            return dataQuality.getCompletenessPercentage();
        }
        
        // Calculate based on available features
        int available = 0;
        int total = 4;
        
        if (economyFeatures != null && economyFeatures.getEconomyScore() != null) available++;
        if (livabilityFeatures != null && livabilityFeatures.getLivabilityScore() != null) available++;
        if (sustainabilityFeatures != null && sustainabilityFeatures.getSustainabilityScore() != null) available++;
        if (growthFeatures != null && growthFeatures.getGrowthScore() != null) available++;
        
        return (available * 100.0) / total;
    }
}
