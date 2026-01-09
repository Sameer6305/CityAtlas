package com.cityatlas.backend.ai;

import java.util.List;
import java.util.Set;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PROMPT CONSTRAINTS - Safety and Determinism Rules
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Defines immutable constraints that ensure AI prompts produce deterministic,
 * safe, and production-ready outputs.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONSTRAINT CATEGORIES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. DETERMINISM: Same input → Same output
 * 2. SAFETY: No harmful, biased, or speculative content
 * 3. BOUNDARY: Strict limits on output structure
 * 4. ATTRIBUTION: All claims must be traceable to input data
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * INTERVIEW TALKING POINTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * - "Constraints are defined as immutable Java records for type safety"
 * - "Each constraint category has explicit rules that can be audited"
 * - "Forbidden topics list prevents generating harmful content"
 * - "Output boundaries prevent runaway generation"
 * 
 * @see PromptTemplate
 * @see CityFeatureInput
 */
public final class PromptConstraints {
    
    private PromptConstraints() {
        // Utility class - prevent instantiation
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DETERMINISM CONSTRAINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Rules ensuring deterministic output.
     */
    public record DeterminismRules(
        boolean useFixedSeed,
        int randomSeed,
        boolean useTemperatureZero,
        boolean disableSampling,
        String hashAlgorithm
    ) {
        public static final DeterminismRules DEFAULT = new DeterminismRules(
            true,           // useFixedSeed
            42,             // randomSeed (consistent)
            true,           // useTemperatureZero (no randomness)
            true,           // disableSampling
            "SHA-256"       // hashAlgorithm for input hashing
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SAFETY CONSTRAINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Topics that must NOT appear in generated content.
     */
    public static final Set<String> FORBIDDEN_TOPICS = Set.of(
        "political opinions",
        "religious views",
        "racial stereotypes",
        "gender discrimination",
        "income discrimination",
        "crime rate assumptions",
        "neighborhood profiling",
        "speculation without data",
        "future predictions beyond data",
        "comparative rankings without basis"
    );
    
    /**
     * Safety rules for content generation.
     */
    public record SafetyRules(
        Set<String> forbiddenTopics,
        boolean requireDataAttribution,
        boolean allowSpeculation,
        boolean allowComparisons,
        int maxConfidenceWithoutData
    ) {
        public static final SafetyRules DEFAULT = new SafetyRules(
            FORBIDDEN_TOPICS,
            true,           // requireDataAttribution
            false,          // allowSpeculation - NEVER speculate
            false,          // allowComparisons - avoid ranking cities
            20              // maxConfidenceWithoutData - 20% max if data missing
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // OUTPUT BOUNDARY CONSTRAINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Limits on generated output.
     */
    public record OutputBoundaries(
        int maxPersonalityLength,
        int minStrengths,
        int maxStrengths,
        int minWeaknesses,
        int maxWeaknesses,
        int minAudienceItems,
        int maxAudienceItems,
        int maxSentenceLength,
        int maxTotalTokens
    ) {
        public static final OutputBoundaries DEFAULT = new OutputBoundaries(
            500,    // maxPersonalityLength (characters)
            2,      // minStrengths
            6,      // maxStrengths
            1,      // minWeaknesses
            5,      // maxWeaknesses
            2,      // minAudienceItems
            6,      // maxAudienceItems
            30,     // maxSentenceLength (words)
            2048    // maxTotalTokens
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ATTRIBUTION CONSTRAINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Rules for tracing output back to input.
     */
    public record AttributionRules(
        boolean requireSourceMetric,
        boolean includeConfidence,
        boolean includeDataFreshness,
        List<String> requiredMetadata
    ) {
        public static final AttributionRules DEFAULT = new AttributionRules(
            true,   // requireSourceMetric
            true,   // includeConfidence
            true,   // includeDataFreshness
            List.of("score", "tier", "explanation")
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCORE THRESHOLDS (Immutable Reference Values)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Standard thresholds for tier classification.
     * These are the canonical values used across all AI components.
     */
    public record ScoreThresholds(
        int excellentThreshold,
        int goodThreshold,
        int averageThreshold,
        int belowAverageThreshold
    ) {
        public static final ScoreThresholds DEFAULT = new ScoreThresholds(
            80,     // excellentThreshold (>= 80)
            60,     // goodThreshold (>= 60)
            40,     // averageThreshold (>= 40)
            20      // belowAverageThreshold (>= 20)
        );
        
        /**
         * Get tier name for a given score.
         */
        public String getTier(Double score) {
            if (score == null) return "unknown";
            if (score >= excellentThreshold) return "excellent";
            if (score >= goodThreshold) return "good";
            if (score >= averageThreshold) return "average";
            if (score >= belowAverageThreshold) return "below-average";
            return "poor";
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ECONOMY-SPECIFIC THRESHOLDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Economic indicator thresholds.
     */
    public record EconomyThresholds(
        double highGdpPerCapita,
        double lowUnemploymentRate,
        int highCostOfLiving,
        int moderateCostOfLiving
    ) {
        public static final EconomyThresholds DEFAULT = new EconomyThresholds(
            60000.0,    // highGdpPerCapita ($60K+)
            4.5,        // lowUnemploymentRate (< 4.5%)
            120,        // highCostOfLiving (index > 120)
            100         // moderateCostOfLiving (index 80-100)
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENVIRONMENT-SPECIFIC THRESHOLDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Environmental quality thresholds (AQI).
     */
    public record EnvironmentThresholds(
        int goodAqi,
        int moderateAqi,
        int unhealthyForSensitiveAqi,
        int unhealthyAqi
    ) {
        public static final EnvironmentThresholds DEFAULT = new EnvironmentThresholds(
            50,     // goodAqi (0-50)
            100,    // moderateAqi (51-100)
            150,    // unhealthyForSensitiveAqi (101-150)
            200     // unhealthyAqi (151-200)
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMBINED CONSTRAINTS BUNDLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Complete constraint bundle for prompts.
     */
    public record ConstraintBundle(
        DeterminismRules determinism,
        SafetyRules safety,
        OutputBoundaries boundaries,
        AttributionRules attribution,
        ScoreThresholds scores,
        EconomyThresholds economy,
        EnvironmentThresholds environment
    ) {
        /**
         * Default production-ready constraint bundle.
         */
        public static final ConstraintBundle PRODUCTION = new ConstraintBundle(
            DeterminismRules.DEFAULT,
            SafetyRules.DEFAULT,
            OutputBoundaries.DEFAULT,
            AttributionRules.DEFAULT,
            ScoreThresholds.DEFAULT,
            EconomyThresholds.DEFAULT,
            EnvironmentThresholds.DEFAULT
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Validate that output conforms to boundaries.
     */
    public static ValidationResult validateOutput(
            String personality,
            List<String> strengths,
            List<String> weaknesses,
            List<String> bestSuitedFor
    ) {
        OutputBoundaries b = OutputBoundaries.DEFAULT;
        StringBuilder errors = new StringBuilder();
        
        // Validate personality length
        if (personality != null && personality.length() > b.maxPersonalityLength()) {
            errors.append(String.format(
                "Personality exceeds max length: %d > %d. ",
                personality.length(), b.maxPersonalityLength()));
        }
        
        // Validate strengths count
        if (strengths != null) {
            if (strengths.size() < b.minStrengths()) {
                errors.append(String.format(
                    "Too few strengths: %d < %d. ",
                    strengths.size(), b.minStrengths()));
            }
            if (strengths.size() > b.maxStrengths()) {
                errors.append(String.format(
                    "Too many strengths: %d > %d. ",
                    strengths.size(), b.maxStrengths()));
            }
        }
        
        // Validate weaknesses count
        if (weaknesses != null) {
            if (weaknesses.size() < b.minWeaknesses()) {
                errors.append(String.format(
                    "Too few weaknesses: %d < %d. ",
                    weaknesses.size(), b.minWeaknesses()));
            }
            if (weaknesses.size() > b.maxWeaknesses()) {
                errors.append(String.format(
                    "Too many weaknesses: %d > %d. ",
                    weaknesses.size(), b.maxWeaknesses()));
            }
        }
        
        // Validate audience items count
        if (bestSuitedFor != null) {
            if (bestSuitedFor.size() < b.minAudienceItems()) {
                errors.append(String.format(
                    "Too few audience items: %d < %d. ",
                    bestSuitedFor.size(), b.minAudienceItems()));
            }
            if (bestSuitedFor.size() > b.maxAudienceItems()) {
                errors.append(String.format(
                    "Too many audience items: %d > %d. ",
                    bestSuitedFor.size(), b.maxAudienceItems()));
            }
        }
        
        String errorMessage = errors.toString().trim();
        return new ValidationResult(errorMessage.isEmpty(), errorMessage);
    }
    
    /**
     * Validation result record.
     */
    public record ValidationResult(boolean valid, String errors) {}
}
