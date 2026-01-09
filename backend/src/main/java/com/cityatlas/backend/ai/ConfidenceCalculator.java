package com.cityatlas.backend.ai;

import org.springframework.stereotype.Service;

/**
 * Calculates confidence scores for AI inference results.
 * Based on data quality, score patterns, and inference characteristics.
 */
@Service
public class ConfidenceCalculator {

    private static final double HIGH_CONFIDENCE_THRESHOLD = 80.0;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 60.0;
    
    private static final double EXTREME_SCORE_THRESHOLD = 90.0; // Very high/low scores reduce confidence
    private static final double LOW_SCORE_THRESHOLD = 10.0;

    /**
     * Calculates overall confidence for an inference result.
     * 
     * Confidence factors:
     * 1. Data completeness (40% weight)
     * 2. Score pattern reliability (30% weight)
     * 3. Inference strength (30% weight)
     */
    public ConfidenceResult calculateConfidence(
        CityFeatureInput input,
        DataQualityChecker.DataQualityResult dataQuality,
        AiInferencePipeline.InferenceInsights insights
    ) {
        // Factor 1: Data completeness (40% weight)
        double dataScore = dataQuality.completeness(); // Already 0-100
        double dataContribution = dataScore * 0.4;
        
        // Factor 2: Score pattern reliability (30% weight)
        double patternScore = calculatePatternReliability(input);
        double patternContribution = patternScore * 0.3;
        
        // Factor 3: Inference strength (30% weight)
        double inferenceScore = calculateInferenceStrength(insights);
        double inferenceContribution = inferenceScore * 0.3;
        
        // Total confidence (0-100)
        double totalConfidence = dataContribution + patternContribution + inferenceContribution;
        
        // Determine confidence level
        ConfidenceLevel level = determineLevel(totalConfidence);
        
        // Generate reasoning
        String reasoning = generateReasoning(dataScore, patternScore, inferenceScore, level);
        
        return new ConfidenceResult(
            totalConfidence,
            level,
            reasoning,
            new ConfidenceBreakdown(dataScore, patternScore, inferenceScore)
        );
    }
    
    /**
     * Calculates how reliable the score patterns are.
     * Lower confidence for extreme scores or inconsistent patterns.
     */
    private double calculatePatternReliability(CityFeatureInput input) {
        int totalScores = 0;
        int reliableScores = 0;
        
        // Check each score for reliability
        if (input.getEconomyFeatures() != null) {
            reliableScores += isReliableScore(input.getEconomyFeatures().getEconomyScore()) ? 1 : 0; 
            totalScores++;
        }
        if (input.getLivabilityFeatures() != null) {
            reliableScores += isReliableScore(input.getLivabilityFeatures().getLivabilityScore()) ? 1 : 0; 
            totalScores++;
        }
        if (input.getSustainabilityFeatures() != null) {
            reliableScores += isReliableScore(input.getSustainabilityFeatures().getSustainabilityScore()) ? 1 : 0; 
            totalScores++;
        }
        if (input.getGrowthFeatures() != null) {
            reliableScores += isReliableScore(input.getGrowthFeatures().getGrowthScore()) ? 1 : 0; 
            totalScores++;
        }
        
        if (totalScores == 0) return 50.0; // Default if no scores available
        
        double baseReliability = (double) reliableScores / totalScores * 100.0;
        
        // Penalty for high variance (inconsistent scores suggest data quality issues)
        double variance = calculateScoreVariance(input);
        double variancePenalty = Math.min(variance / 10.0, 20.0); // Max 20% penalty
        
        return Math.max(0, baseReliability - variancePenalty);
    }
    
    /**
     * Checks if a score is in a reliable range (not too extreme).
     */
    private boolean isReliableScore(Double score) {
        if (score == null) return false;
        return score >= LOW_SCORE_THRESHOLD && score <= EXTREME_SCORE_THRESHOLD;
    }
    
    /**
     * Calculates variance in feature scores.
     */
    private double calculateScoreVariance(CityFeatureInput input) {
        double[] scores = {
            input.getEconomyFeatures() != null && input.getEconomyFeatures().getEconomyScore() != null ? 
                input.getEconomyFeatures().getEconomyScore() : 50.0,
            input.getLivabilityFeatures() != null && input.getLivabilityFeatures().getLivabilityScore() != null ? 
                input.getLivabilityFeatures().getLivabilityScore() : 50.0,
            input.getSustainabilityFeatures() != null && input.getSustainabilityFeatures().getSustainabilityScore() != null ? 
                input.getSustainabilityFeatures().getSustainabilityScore() : 50.0,
            input.getGrowthFeatures() != null && input.getGrowthFeatures().getGrowthScore() != null ? 
                input.getGrowthFeatures().getGrowthScore() : 50.0
        };
        
        double mean = 0;
        for (double score : scores) mean += score;
        mean /= scores.length;
        
        double variance = 0;
        for (double score : scores) {
            variance += Math.pow(score - mean, 2);
        }
        
        return Math.sqrt(variance / scores.length); // Standard deviation
    }
    
    /**
     * Calculates how strong the inference is.
     * More insights = higher confidence.
     */
    private double calculateInferenceStrength(AiInferencePipeline.InferenceInsights insights) {
        int strengthCount = insights.strengths().size();
        int weaknessCount = insights.weaknesses().size();
        int audienceCount = insights.audienceSegments().size();
        
        // Expected ranges (from PromptConstraints)
        int minStrengths = 2, maxStrengths = 6;
        int minWeaknesses = 1, maxWeaknesses = 5;
        int minAudience = 2, maxAudience = 6;
        
        // Calculate how well each category fits expected range
        double strengthScore = calculateFitScore(strengthCount, minStrengths, maxStrengths);
        double weaknessScore = calculateFitScore(weaknessCount, minWeaknesses, maxWeaknesses);
        double audienceScore = calculateFitScore(audienceCount, minAudience, maxAudience);
        
        // Average fit score
        return (strengthScore + weaknessScore + audienceScore) / 3.0;
    }
    
    /**
     * Calculates how well a count fits within expected range (0-100).
     */
    private double calculateFitScore(int actual, int min, int max) {
        if (actual < min) {
            // Below minimum: penalty based on how far below
            double deficit = (double) (min - actual) / min;
            return Math.max(0, 100.0 - (deficit * 50.0)); // Max 50% penalty
        } else if (actual > max) {
            // Above maximum: penalty based on how far above
            double excess = (double) (actual - max) / max;
            return Math.max(0, 100.0 - (excess * 50.0)); // Max 50% penalty
        } else {
            // Within range: full score
            return 100.0;
        }
    }
    
    /**
     * Determines confidence level from score.
     */
    private ConfidenceLevel determineLevel(double confidence) {
        if (confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.HIGH;
        } else if (confidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.MEDIUM;
        } else {
            return ConfidenceLevel.LOW;
        }
    }
    
    /**
     * Generates human-readable reasoning for confidence score.
     */
    private String generateReasoning(double dataScore, double patternScore, double inferenceScore, ConfidenceLevel level) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append(level).append(" confidence: ");
        
        // Identify primary factor
        double maxScore = Math.max(dataScore, Math.max(patternScore, inferenceScore));
        
        if (maxScore == dataScore) {
            reasoning.append(String.format("Data completeness %.0f%%. ", dataScore));
        }
        if (patternScore < 70) {
            reasoning.append(String.format("Score patterns show %.0f%% reliability. ", patternScore));
        }
        if (inferenceScore < 70) {
            reasoning.append(String.format("Inference strength %.0f%%. ", inferenceScore));
        }
        
        // Add guidance based on level
        switch (level) {
            case HIGH -> reasoning.append("Output is highly reliable.");
            case MEDIUM -> reasoning.append("Output is generally reliable with minor data gaps.");
            case LOW -> reasoning.append("Output should be used cautiously due to data limitations.");
        }
        
        return reasoning.toString();
    }
    
    /**
     * Confidence level categorization.
     */
    public enum ConfidenceLevel {
        HIGH,    // >= 80% - Reliable output
        MEDIUM,  // 60-79% - Generally reliable with caveats
        LOW      // < 60% - Use with caution
    }
    
    /**
     * Detailed confidence calculation result.
     */
    public record ConfidenceResult(
        double overallConfidence,      // 0-100
        ConfidenceLevel level,
        String reasoning,
        ConfidenceBreakdown breakdown
    ) {}
    
    /**
     * Breakdown of confidence factors.
     */
    public record ConfidenceBreakdown(
        double dataCompleteness,    // 0-100
        double patternReliability,  // 0-100
        double inferenceStrength    // 0-100
    ) {}
}
