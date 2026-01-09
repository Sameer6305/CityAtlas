package com.cityatlas.backend.ai;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates data quality before AI inference.
 * Detects missing fields, unrealistic values, and insufficient data.
 */
@Service
public class DataQualityChecker {

    private static final double MIN_VALID_SCORE = 0.0;
    private static final double MAX_VALID_SCORE = 100.0;
    private static final double WEAK_DATA_THRESHOLD = 30.0; // Less than 30% completeness = weak

    /**
     * Validates input data quality and returns detailed assessment.
     */
    public DataQualityResult validateData(CityFeatureInput input) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check basic metadata
        if (input.getCityIdentifier() == null || input.getCityIdentifier().getName() == null || 
            input.getCityIdentifier().getName().isBlank()) {
            issues.add("Missing city name");
        }
        if (input.getCityIdentifier() == null || input.getCityIdentifier().getCountry() == null || 
            input.getCityIdentifier().getCountry().isBlank()) {
            issues.add("Missing country");
        }
        
        // Check feature scores
        if (input.getEconomyFeatures() != null) {
            validateScore(input.getEconomyFeatures().getEconomyScore(), "Economy", issues, warnings);
        } else {
            warnings.add("Missing economy features");
        }
        
        if (input.getLivabilityFeatures() != null) {
            validateScore(input.getLivabilityFeatures().getLivabilityScore(), "Livability", issues, warnings);
        } else {
            warnings.add("Missing livability features");
        }
        
        if (input.getSustainabilityFeatures() != null) {
            validateScore(input.getSustainabilityFeatures().getSustainabilityScore(), "Sustainability", issues, warnings);
        } else {
            warnings.add("Missing sustainability features");
        }
        
        if (input.getGrowthFeatures() != null) {
            validateScore(input.getGrowthFeatures().getGrowthScore(), "Growth", issues, warnings);
        } else {
            warnings.add("Missing growth features");
        }
        
        // Check population
        if (input.getCityIdentifier() == null || input.getCityIdentifier().getPopulation() == null) {
            warnings.add("Missing population data");
        } else if (input.getCityIdentifier().getPopulation() <= 0) {
            issues.add("Invalid population: " + input.getCityIdentifier().getPopulation());
        }
        
        // Check economic indicators
        if (input.getEconomyFeatures() != null) {
            if (input.getEconomyFeatures().getGdpPerCapita() == null) {
                warnings.add("Missing GDP per capita");
            } else if (input.getEconomyFeatures().getGdpPerCapita() < 0) {
                issues.add("Invalid GDP per capita: " + input.getEconomyFeatures().getGdpPerCapita());
            }
            
            if (input.getEconomyFeatures().getUnemploymentRate() == null) {
                warnings.add("Missing unemployment rate");
            } else if (input.getEconomyFeatures().getUnemploymentRate() < 0 || 
                       input.getEconomyFeatures().getUnemploymentRate() > 100) {
                issues.add("Invalid unemployment rate: " + input.getEconomyFeatures().getUnemploymentRate());
            }
        }
        
        // Calculate completeness score
        double completeness = calculateCompleteness(input);
        
        // Determine if data is sufficient
        boolean isSufficient = issues.isEmpty() && completeness >= WEAK_DATA_THRESHOLD;
        
        return new DataQualityResult(
            isSufficient,
            completeness,
            issues,
            warnings
        );
    }
    
    /**
     * Validates that a score is within valid range.
     */
    private void validateScore(Double score, String scoreName, List<String> issues, List<String> warnings) {
        if (score == null) {
            warnings.add("Missing " + scoreName + " score");
        } else if (score < MIN_VALID_SCORE || score > MAX_VALID_SCORE) {
            issues.add(scoreName + " score out of range [0-100]: " + score);
        } else if (score == 0.0) {
            warnings.add(scoreName + " score is zero (may indicate missing data)");
        }
    }
    
    /**
     * Calculates data completeness as percentage of non-null critical fields.
     */
    private double calculateCompleteness(CityFeatureInput input) {
        // Use the built-in data quality metadata if available
        if (input.getDataQuality() != null && input.getDataQuality().getCompletenessPercentage() != null) {
            return input.getDataQuality().getCompletenessPercentage();
        }
        
        // Otherwise, calculate manually
        int totalFields = 0;
        int presentFields = 0;
        
        // Critical fields
        totalFields++; 
        if (input.getCityIdentifier() != null && input.getCityIdentifier().getName() != null && 
            !input.getCityIdentifier().getName().isBlank()) presentFields++;
        
        totalFields++; 
        if (input.getCityIdentifier() != null && input.getCityIdentifier().getCountry() != null && 
            !input.getCityIdentifier().getCountry().isBlank()) presentFields++;
        
        // Feature scores
        totalFields++; 
        if (input.getEconomyFeatures() != null && input.getEconomyFeatures().getEconomyScore() != null && 
            input.getEconomyFeatures().getEconomyScore() > 0) presentFields++;
        
        totalFields++; 
        if (input.getLivabilityFeatures() != null && input.getLivabilityFeatures().getLivabilityScore() != null && 
            input.getLivabilityFeatures().getLivabilityScore() > 0) presentFields++;
        
        totalFields++; 
        if (input.getSustainabilityFeatures() != null && input.getSustainabilityFeatures().getSustainabilityScore() != null && 
            input.getSustainabilityFeatures().getSustainabilityScore() > 0) presentFields++;
        
        totalFields++; 
        if (input.getGrowthFeatures() != null && input.getGrowthFeatures().getGrowthScore() != null && 
            input.getGrowthFeatures().getGrowthScore() > 0) presentFields++;
        
        // Economic indicators
        totalFields++; 
        if (input.getEconomyFeatures() != null && input.getEconomyFeatures().getGdpPerCapita() != null && 
            input.getEconomyFeatures().getGdpPerCapita() > 0) presentFields++;
        
        totalFields++; 
        if (input.getEconomyFeatures() != null && input.getEconomyFeatures().getUnemploymentRate() != null) 
            presentFields++;
        
        // Demographics
        totalFields++; 
        if (input.getCityIdentifier() != null && input.getCityIdentifier().getPopulation() != null && 
            input.getCityIdentifier().getPopulation() > 0) presentFields++;
        
        return (double) presentFields / totalFields * 100.0;
    }
    
    /**
     * Result of data quality validation.
     */
    public record DataQualityResult(
        boolean isSufficient,      // Can we safely run inference?
        double completeness,        // 0-100% completeness score
        List<String> issues,        // Critical issues (block inference)
        List<String> warnings       // Non-critical warnings
    ) {
        public String getSummary() {
            if (isSufficient) {
                return String.format("Data quality: %.1f%% complete, %d warnings", 
                    completeness, warnings.size());
            } else {
                return String.format("Insufficient data: %.1f%% complete, %d issues, %d warnings", 
                    completeness, issues.size(), warnings.size());
            }
        }
    }
}

