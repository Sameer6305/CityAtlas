package com.cityatlas.backend.ai;

import java.util.List;

import lombok.Builder;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * AI INFERENCE PIPELINE - Data Transfer Objects
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Defines the input/output contracts for the AI inference pipeline.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * PIPELINE STAGES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   STAGE 1: Input Preparation
 *   ├── Input:  City entity
 *   └── Output: CityFeatureInput
 * 
 *   STAGE 2: Rule-Based Inference
 *   ├── Input:  CityFeatureInput
 *   └── Output: InferenceInsights
 * 
 *   STAGE 3: Output Validation
 *   ├── Input:  InferenceInsights
 *   └── Output: ValidationResult
 * 
 *   STAGE 4: Response Packaging
 *   ├── Input:  InferenceInsights + ValidationResult
 *   └── Output: InferenceResult
 * 
 * @see AiInferenceService
 */
public final class AiInferencePipeline {
    
    private AiInferencePipeline() {
        // Utility class
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INFERENCE INSIGHTS (Internal Stage Output)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Output of Stage 2: Rule-Based Inference.
     * Contains raw generated insights before validation.
     */
    @Builder
    public record InferenceInsights(
        String personality,
        List<String> strengths,
        List<String> weaknesses,
        List<String> audienceSegments
    ) {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INFERENCE RESULT (Final Pipeline Output)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Final output of the complete inference pipeline.
     * This is what gets returned to the API layer.
     */
    @Builder
    public record InferenceResult(
        // City identification
        String citySlug,
        
        // Generated insights
        String personality,
        List<String> strengths,
        List<String> weaknesses,
        List<String> bestSuitedFor,
        
        // Metadata
        Double confidence,        // 0-1, based on data completeness
        Long inferenceTimeMs,     // Pipeline execution time
        String pipelineVersion,   // e.g., "1.0.0"
        
        // Validation
        Boolean valid,            // Did output pass constraints?
        String validationErrors   // If invalid, what failed?
    ) {
        /**
         * Check if inference was successful and valid.
         */
        public boolean isSuccessful() {
            return valid != null && valid && personality != null && 
                   strengths != null && !strengths.isEmpty();
        }
        
        /**
         * Get quality tier based on confidence.
         */
        public String qualityTier() {
            if (confidence == null) return "unknown";
            if (confidence >= 0.9) return "high";
            if (confidence >= 0.7) return "medium";
            if (confidence >= 0.5) return "low";
            return "very-low";
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PIPELINE METRICS (for observability)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Metrics collected during pipeline execution.
     * Used for monitoring and optimization.
     */
    @Builder
    public record PipelineMetrics(
        String citySlug,
        Long stage1DurationMs,    // Input preparation
        Long stage2DurationMs,    // Rule inference
        Long stage3DurationMs,    // Validation
        Long totalDurationMs,
        Integer rulesApplied,
        Boolean validationPassed,
        String failureReason
    ) {}
}
