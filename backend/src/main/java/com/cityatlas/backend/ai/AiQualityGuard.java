package com.cityatlas.backend.ai;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Prevents misleading AI summaries through pre-inference validation.
 * Enforces quality gates before allowing inference to proceed.
 */
@Service
public class AiQualityGuard {

    private final DataQualityChecker dataQualityChecker;

    public AiQualityGuard(DataQualityChecker dataQualityChecker) {
        this.dataQualityChecker = dataQualityChecker;
    }

    /**
     * Validates if inference should proceed based on data quality.
     * Returns validation result with actionable guidance.
     */
    public GuardResult validateForInference(CityFeatureInput input) {
        DataQualityChecker.DataQualityResult dataQuality = dataQualityChecker.validateData(input);
        
        List<String> blockers = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        // Critical blockers (prevent inference)
        if (!dataQuality.isSufficient()) {
            blockers.add("Insufficient data quality: " + dataQuality.getSummary());
            blockers.addAll(dataQuality.issues());
        }
        
        // Check for misleading patterns
        checkForMisleadingPatterns(input, blockers, recommendations);
        
        // Check for edge cases
        checkForEdgeCases(input, recommendations);
        
        boolean shouldProceed = blockers.isEmpty();
        String guidance = generateGuidance(shouldProceed, blockers, recommendations);
        
        return new GuardResult(
            shouldProceed,
            dataQuality.completeness(),
            blockers,
            recommendations,
            guidance
        );
    }
    
    /**
     * Checks for patterns that could lead to misleading summaries.
     */
    private void checkForMisleadingPatterns(
        CityFeatureInput input,
        List<String> blockers,
        List<String> recommendations
    ) {
        // Pattern 1: All scores are identical (likely default/placeholder values)
        if (hasIdenticalScores(input)) {
            blockers.add("All feature scores are identical - likely placeholder data");
        }
        
        // Pattern 2: Scores are suspiciously perfect (all 100 or all 0)
        if (hasSuspiciouslyPerfectScores(input)) {
            recommendations.add("Multiple perfect scores detected - verify data accuracy");
        }
        
        // Pattern 3: Missing key economic context
        if (input.getEconomyFeatures() == null || 
            (input.getEconomyFeatures().getGdpPerCapita() == null && 
             input.getEconomyFeatures().getUnemploymentRate() == null)) {
            recommendations.add("No economic indicators available - economy score may be unreliable");
        }
        
        // Pattern 4: Population data missing
        if (input.getCityIdentifier() == null || input.getCityIdentifier().getPopulation() == null || 
            input.getCityIdentifier().getPopulation() <= 0) {
            recommendations.add("Missing population data - demographic insights may be limited");
        }
    }
    
    /**
     * Checks if all feature scores are identical.
     */
    private boolean hasIdenticalScores(CityFeatureInput input) {
        Double economyScore = input.getEconomyFeatures() != null ? input.getEconomyFeatures().getEconomyScore() : null;
        if (economyScore == null) return false;
        
        Double livabilityScore = input.getLivabilityFeatures() != null ? input.getLivabilityFeatures().getLivabilityScore() : null;
        Double sustainabilityScore = input.getSustainabilityFeatures() != null ? input.getSustainabilityFeatures().getSustainabilityScore() : null;
        Double growthScore = input.getGrowthFeatures() != null ? input.getGrowthFeatures().getGrowthScore() : null;
        
        return economyScore.equals(livabilityScore)
            && economyScore.equals(sustainabilityScore)
            && economyScore.equals(growthScore);
    }
    
    /**
     * Checks for suspiciously perfect scores.
     */
    private boolean hasSuspiciouslyPerfectScores(CityFeatureInput input) {
        int perfectScoreCount = 0;
        
        if (input.getEconomyFeatures() != null && isPerfectScore(input.getEconomyFeatures().getEconomyScore())) perfectScoreCount++;
        if (input.getLivabilityFeatures() != null && isPerfectScore(input.getLivabilityFeatures().getLivabilityScore())) perfectScoreCount++;
        if (input.getSustainabilityFeatures() != null && isPerfectScore(input.getSustainabilityFeatures().getSustainabilityScore())) perfectScoreCount++;
        if (input.getGrowthFeatures() != null && isPerfectScore(input.getGrowthFeatures().getGrowthScore())) perfectScoreCount++;
        
        return perfectScoreCount >= 3; // 3 or more perfect scores is suspicious
    }
    
    /**
     * Checks if a score is perfectly 0 or 100.
     */
    private boolean isPerfectScore(Double score) {
        return score != null && (score == 0.0 || score == 100.0);
    }
    
    /**
     * Checks for edge cases that need special handling.
     */
    private void checkForEdgeCases(CityFeatureInput input, List<String> recommendations) {
        // Edge case 1: Very small population
        if (input.getCityIdentifier() != null && input.getCityIdentifier().getPopulation() != null && 
            input.getCityIdentifier().getPopulation() < 10000) {
            recommendations.add("Very small population (<10K) - results may not generalize");
        }
        
        // Edge case 2: Very low GDP per capita
        if (input.getEconomyFeatures() != null && input.getEconomyFeatures().getGdpPerCapita() != null && 
            input.getEconomyFeatures().getGdpPerCapita() < 5000) {
            recommendations.add("Low GDP per capita (<$5K) - economic insights limited");
        }
    }
    
    /**
     * Generates actionable guidance based on validation results.
     */
    private String generateGuidance(
        boolean shouldProceed,
        List<String> blockers,
        List<String> recommendations
    ) {
        if (shouldProceed) {
            if (recommendations.isEmpty()) {
                return "Data quality is sufficient. Safe to proceed with inference.";
            } else {
                return "Inference can proceed with " + recommendations.size() + " caveats. Review recommendations.";
            }
        } else {
            return "Inference blocked due to " + blockers.size() + " critical issues. " +
                   "Address blockers before proceeding.";
        }
    }
    
    /**
     * Result of quality guard validation.
     */
    public record GuardResult(
        boolean shouldProceed,          // Can inference proceed?
        double dataCompleteness,        // 0-100%
        List<String> blockers,          // Critical issues blocking inference
        List<String> recommendations,   // Non-critical warnings
        String guidance                 // Actionable guidance message
    ) {
        public boolean hasBlockers() {
            return !blockers.isEmpty();
        }
        
        public boolean hasRecommendations() {
            return !recommendations.isEmpty();
        }
        
        public String getSummary() {
            return String.format("%s - Completeness: %.1f%%, Blockers: %d, Recommendations: %d",
                shouldProceed ? "PASS" : "FAIL",
                dataCompleteness,
                blockers.size(),
                recommendations.size()
            );
        }
    }
}
