package com.cityatlas.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * EXPLAINABLE AI SUMMARY - Feature Reasoning Metadata
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Provides transparent, interview-justifiable AI reasoning for city assessments.
 * Every strength, weakness, and recommendation traces back to concrete data.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * EXPLAINABILITY FRAMEWORK
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────────────────────────────────────────────────────────────────────┐
 *   │                    EXPLAINABLE AI ARCHITECTURE                              │
 *   │                                                                             │
 *   │   Raw Data ───▶ Features ───▶ Reasoning ───▶ Conclusions ───▶ Explanations │
 *   │   (GDP, AQI)    (Scores)      (Rules)       (Strengths)      (Why/How)     │
 *   │                                                                             │
 *   │   Example Flow:                                                             │
 *   │   GDP=$85K ───▶ EconScore=78 ───▶ "GDP>$60K" ───▶ "Strong economy" ───▶    │
 *   │                                    (Rule)         (Conclusion)              │
 *   │                 "High GDP of $85K drives economy score (78/100)"           │
 *   │                                    (Explanation)                            │
 *   └─────────────────────────────────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * INTERVIEW TALKING POINTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Q: "How do you ensure AI decisions are explainable?"
 * A: "Every AI conclusion includes:
 *     1. The input data that triggered it (e.g., GDP = $85K)
 *     2. The rule that was applied (e.g., GDP > $60K → 'prosperous')
 *     3. The feature contribution (e.g., GDP contributed 32/100 to economy score)
 *     4. A human-readable explanation for the end user"
 * 
 * @see AiCitySummaryDTO
 * @see com.cityatlas.backend.service.CityFeatureComputer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExplainableAiSummary {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CITY IDENTIFICATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String citySlug;
    private String cityName;
    private LocalDateTime generatedAt;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXPLAINABLE ASSESSMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Overall city assessment with full reasoning chain.
     */
    private CityAssessment assessment;
    
    /**
     * Feature-level explanations showing what contributed to each score.
     */
    private FeatureContributions featureContributions;
    
    /**
     * Strength analyses with data-backed reasoning.
     */
    private List<ReasonedConclusion> strengths;
    
    /**
     * Weakness analyses with data-backed reasoning.
     */
    private List<ReasonedConclusion> weaknesses;
    
    /**
     * Audience fit recommendations with justification.
     */
    private List<ReasonedConclusion> bestSuitedFor;
    
    /**
     * AI transparency metadata.
     */
    private AiTransparency transparency;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CITY ASSESSMENT - High-level verdict with reasoning
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CityAssessment {
        
        /**
         * Overall verdict: "excellent", "good", "average", "below-average", "poor"
         */
        private String verdict;
        
        /**
         * Confidence level (0-1) in the assessment.
         */
        private Double confidence;
        
        /**
         * One-sentence summary of why city received this verdict.
         * Example: "Strong economy and good air quality, but high cost of living"
         */
        private String summary;
        
        /**
         * Detailed reasoning chain for the verdict.
         */
        private List<String> reasoningChain;
        
        /**
         * Which features had the most impact on this verdict.
         */
        private List<FeatureImpact> keyDrivers;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FEATURE CONTRIBUTIONS - What contributed to each score
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeatureContributions {
        
        private ScoreBreakdown economy;
        private ScoreBreakdown livability;
        private ScoreBreakdown sustainability;
        private ScoreBreakdown growth;
        private ScoreBreakdown overall;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScoreBreakdown {
        
        /**
         * Final computed score (0-100).
         */
        private Double score;
        
        /**
         * Score tier: "excellent", "good", "average", "below-average", "poor"
         */
        private String tier;
        
        /**
         * Human-readable explanation of the score.
         */
        private String explanation;
        
        /**
         * Individual components that contributed to this score.
         */
        private List<ScoreComponent> components;
        
        /**
         * Data that was missing and couldn't contribute.
         */
        private List<String> missingInputs;
        
        /**
         * Confidence level based on data availability.
         */
        private Double confidence;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScoreComponent {
        
        /**
         * Name of the input metric.
         * Example: "GDP per capita", "Unemployment rate"
         */
        private String metric;
        
        /**
         * Raw input value.
         * Example: "$85,000", "4.2%"
         */
        private String rawValue;
        
        /**
         * Normalized value (0-100 scale).
         */
        private Double normalizedValue;
        
        /**
         * Weight assigned to this component.
         * Example: 0.40 for GDP in economy score
         */
        private Double weight;
        
        /**
         * Points contributed to final score.
         * Example: 32 points out of 100
         */
        private Double contribution;
        
        /**
         * Direction: "positive", "negative", "neutral"
         * Indicates if this metric helped or hurt the score.
         */
        private String impact;
        
        /**
         * Human-readable explanation.
         * Example: "GDP of $85K is 'prosperous', contributing 32 points"
         */
        private String explanation;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REASONED CONCLUSION - Strength/Weakness with full justification
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReasonedConclusion {
        
        /**
         * The conclusion itself.
         * Example: "Strong job market with diverse opportunities"
         */
        private String conclusion;
        
        /**
         * Category: "economy", "livability", "sustainability", "growth", "overall"
         */
        private String category;
        
        /**
         * Confidence level (0-1) in this conclusion.
         */
        private Double confidence;
        
        /**
         * Chain of reasoning that led to this conclusion.
         */
        private ReasoningChain reasoning;
        
        /**
         * How much this conclusion affects the overall assessment.
         * "high", "medium", "low"
         */
        private String significance;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReasoningChain {
        
        /**
         * The rule or logic that was applied.
         * Example: "Unemployment < 4.5% indicates healthy job market"
         */
        private String rule;
        
        /**
         * Data points that triggered this rule.
         */
        private List<DataEvidence> evidence;
        
        /**
         * Logical inference steps.
         * Example: ["Unemployment is 3.8%", "3.8% < 4.5% threshold", 
         *           "Therefore: healthy job market"]
         */
        private List<String> inferenceSteps;
        
        /**
         * Alternative interpretation, if any.
         * Example: "However, job market diversity is unknown"
         */
        private String caveat;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataEvidence {
        
        /**
         * Name of the data point.
         * Example: "Unemployment rate"
         */
        private String metric;
        
        /**
         * Actual value observed.
         * Example: "3.8%"
         */
        private String value;
        
        /**
         * How this value compares to thresholds/benchmarks.
         * Example: "Below healthy threshold (4.5%)"
         */
        private String comparison;
        
        /**
         * Data source or origin.
         * Example: "Bureau of Labor Statistics"
         */
        private String source;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FEATURE IMPACT - Which features drove the verdict
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeatureImpact {
        
        /**
         * Feature name: "economy", "livability", "sustainability", "growth"
         */
        private String feature;
        
        /**
         * Score for this feature (0-100).
         */
        private Double score;
        
        /**
         * Impact direction: "positive", "negative", "neutral"
         */
        private String direction;
        
        /**
         * Magnitude of impact: "high", "medium", "low"
         */
        private String magnitude;
        
        /**
         * Why this feature had this impact.
         */
        private String explanation;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // AI TRANSPARENCY - How the AI made decisions
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AiTransparency {
        
        /**
         * AI model or algorithm used.
         * Example: "Rule-based deterministic scoring v2.0"
         */
        private String algorithm;
        
        /**
         * Version of the scoring model.
         */
        private String version;
        
        /**
         * Timestamp when analysis was performed.
         */
        private LocalDateTime analyzedAt;
        
        /**
         * Data freshness (how recent the input data is).
         */
        private DataFreshness dataFreshness;
        
        /**
         * Limitations of the analysis.
         */
        private List<String> limitations;
        
        /**
         * How to interpret the results.
         */
        private String interpretationGuide;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataFreshness {
        
        /**
         * Percentage of data that is fresh (< 24 hours).
         */
        private Double freshPercentage;
        
        /**
         * Oldest data point used.
         */
        private String oldestDataPoint;
        
        /**
         * Data sources used.
         */
        private List<String> sources;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a reasoned strength with full justification.
     */
    public static ReasonedConclusion strength(String conclusion, String category,
            String rule, List<DataEvidence> evidence, List<String> steps) {
        return ReasonedConclusion.builder()
                .conclusion(conclusion)
                .category(category)
                .confidence(1.0)
                .significance("high")
                .reasoning(ReasoningChain.builder()
                        .rule(rule)
                        .evidence(evidence)
                        .inferenceSteps(steps)
                        .build())
                .build();
    }
    
    /**
     * Create a reasoned weakness with full justification.
     */
    public static ReasonedConclusion weakness(String conclusion, String category,
            String rule, List<DataEvidence> evidence, List<String> steps, String caveat) {
        return ReasonedConclusion.builder()
                .conclusion(conclusion)
                .category(category)
                .confidence(0.9)
                .significance("medium")
                .reasoning(ReasoningChain.builder()
                        .rule(rule)
                        .evidence(evidence)
                        .inferenceSteps(steps)
                        .caveat(caveat)
                        .build())
                .build();
    }
    
    /**
     * Create a data evidence point.
     */
    public static DataEvidence evidence(String metric, String value, String comparison) {
        return DataEvidence.builder()
                .metric(metric)
                .value(value)
                .comparison(comparison)
                .build();
    }
}
