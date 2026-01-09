package com.cityatlas.backend.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cityatlas.backend.entity.City;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * AI INFERENCE SERVICE - Rule-Based Inference Pipeline
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Orchestrates the complete AI inference pipeline from city data to structured
 * AI insights. This is a DETERMINISTIC, RULE-BASED system - no ML models,
 * no training, no hosted models.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * INFERENCE PIPELINE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
 *   │ City Entity │───▶│PromptBuilder│───▶│FeatureInput │───▶│ Rule Engine │
 *   │ (raw data)  │    │(transforms) │    │ (structured)│    │ (if/then)   │
 *   └─────────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
 *                                                                     │
 *                                                                     ▼
 *   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
 *   │ API Response│◀───│ Validation  │◀───│ AI Summary  │◀───│ Generated   │
 *   │    (JSON)   │    │ (bounds)    │    │    DTO      │    │   Insights  │
 *   └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * KEY DESIGN PRINCIPLES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. DETERMINISM: Same city always produces identical output
 * 2. MODULARITY: Each stage is independently testable
 * 3. TRANSPARENCY: Every decision is traceable to a rule
 * 4. NO ML: Pure rule-based logic, no model hosting/training
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * INTERVIEW TALKING POINTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * - "This is an INFERENCE-ONLY pipeline - no training phase"
 * - "Rules are encoded as thresholds and if/then logic"
 * - "Each pipeline stage is a separate method for unit testing"
 * - "Output validation ensures constraints are met"
 * 
 * @see PromptBuilder
 * @see CityFeatureInput
 * @see PromptConstraints
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiInferenceService {
    
    private final PromptBuilder promptBuilder;
    private final DataQualityChecker dataQualityChecker;
    private final ConfidenceCalculator confidenceCalculator;
    private final AiDecisionLogger decisionLogger;
    private final AiQualityGuard qualityGuard;
    private final AiFallbackService fallbackService;
    
    // Canonical thresholds from PromptConstraints
    private static final PromptConstraints.EconomyThresholds ECONOMY = 
        PromptConstraints.EconomyThresholds.DEFAULT;
    
    // Confidence threshold below which we trigger fallback
    private static final double LOW_CONFIDENCE_THRESHOLD = 40.0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN INFERENCE PIPELINE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Execute the complete AI inference pipeline for a city.
     * 
     * PIPELINE STAGES:
     * 1. Input Preparation: City → CityFeatureInput
     * 2. Rule-Based Inference: Apply scoring rules
     * 3. Narrative Generation: Convert scores to text
     * 4. Output Validation: Ensure constraints met
     * 5. Return Structured Response
     * 
     * @param city The city to generate insights for
     * @return Structured AI summary with insights
     */
    public AiInferencePipeline.InferenceResult runInference(City city) {
        log.info("[INFERENCE] Starting quality-enhanced pipeline for city: {}", city.getSlug());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // STAGE 0: Input Preparation
            log.debug("[INFERENCE][STAGE-0] Building feature input");
            CityFeatureInput input = promptBuilder.buildFeatureInput(city);
            
            // STAGE 1: Quality Guard (pre-inference validation)
            log.debug("[INFERENCE][STAGE-1] Running quality guard");
            AiQualityGuard.GuardResult guardResult = qualityGuard.validateForInference(input);
            log.info("[INFERENCE] Quality guard: {}", guardResult.getSummary());
            
            // ═══════════════════════════════════════════════════════════════════
            // FALLBACK: When quality guard blocks inference due to bad data
            // ═══════════════════════════════════════════════════════════════════
            if (!guardResult.shouldProceed()) {
                log.warn("[INFERENCE] Quality guard BLOCKED - triggering fallback");
                
                // Check data quality for fallback tier determination
                DataQualityChecker.DataQualityResult dataQuality = dataQualityChecker.validateData(input);
                
                // Use fallback service to generate graceful response
                AiFallbackService.FallbackResponse fallback = 
                    fallbackService.handleIncompleteData(city, input, dataQuality);
                
                log.info("[INFERENCE] Fallback response generated: tier={}, reason={}", 
                    fallback.tier(), fallback.reason());
                
                return fallback.toInferenceResult(city.getSlug());
            }
            
            if (guardResult.hasRecommendations()) {
                log.info("[INFERENCE] Quality guard: {} recommendations", guardResult.recommendations().size());
            }
            
            // STAGE 2: Data Quality Check
            log.debug("[INFERENCE][STAGE-2] Checking data quality");
            DataQualityChecker.DataQualityResult dataQuality = dataQualityChecker.validateData(input);
            log.info("[INFERENCE] {}", dataQuality.getSummary());
            
            // STAGE 3: Rule-Based Inference
            log.debug("[INFERENCE][STAGE-3] Applying inference rules");
            AiInferencePipeline.InferenceInsights insights = applyInferenceRules(input);
            
            // STAGE 4: Output Validation
            log.debug("[INFERENCE][STAGE-4] Validating output");
            PromptConstraints.ValidationResult validation = validateOutput(insights);
            if (!validation.valid()) {
                log.warn("[INFERENCE] Validation failed: {}", validation.errors());
            }
            
            // STAGE 5: Confidence Calculation
            log.debug("[INFERENCE][STAGE-5] Calculating confidence");
            ConfidenceCalculator.ConfidenceResult confidence = 
                confidenceCalculator.calculateConfidence(input, dataQuality, insights);
            log.info("[INFERENCE] Confidence: {} ({:.1f}%)", confidence.level(), confidence.overallConfidence());
            
            // ═══════════════════════════════════════════════════════════════════
            // FALLBACK: When confidence is too low for reliable insights
            // ═══════════════════════════════════════════════════════════════════
            if (confidence.overallConfidence() < LOW_CONFIDENCE_THRESHOLD) {
                log.warn("[INFERENCE] Low confidence ({:.1f}%) - triggering fallback", 
                    confidence.overallConfidence());
                
                AiFallbackService.FallbackResponse fallback = 
                    fallbackService.handleLowConfidence(city, insights, confidence);
                
                log.info("[INFERENCE] Low-confidence fallback: tier={}", fallback.tier());
                
                return fallback.toInferenceResult(city.getSlug());
            }
            
            // STAGE 6: Package Results
            long duration = System.currentTimeMillis() - startTime;
            AiInferencePipeline.InferenceResult result = AiInferencePipeline.InferenceResult.builder()
                .citySlug(city.getSlug())
                .personality(insights.personality())
                .strengths(insights.strengths())
                .weaknesses(insights.weaknesses())
                .bestSuitedFor(insights.audienceSegments())
                .confidence(confidence.overallConfidence() / 100.0)
                .inferenceTimeMs(duration)
                .pipelineVersion("1.0.0")
                .valid(validation.valid())
                .validationErrors(validation.valid() ? null : validation.errors())
                .build();
            
            // STAGE 7: Audit Logging
            log.debug("[INFERENCE][STAGE-7] Creating audit log");
            AiDecisionLogger.AuditLog auditLog = decisionLogger.logInference(input, result, dataQuality, confidence);
            log.info("[INFERENCE] Complete: {}", auditLog.toSummary());
            
            return result;
                
        } catch (Exception e) {
            // ═══════════════════════════════════════════════════════════════════
            // FALLBACK: When any error occurs during inference
            // ═══════════════════════════════════════════════════════════════════
            log.error("[INFERENCE] Pipeline failed - triggering error fallback", e);
            
            AiFallbackService.FallbackResponse fallback = 
                fallbackService.handleInferenceError(city, e);
            
            log.info("[INFERENCE] Error fallback generated: tier={}", fallback.tier());
            
            return fallback.toInferenceResult(city.getSlug());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STAGE 3: RULE-BASED INFERENCE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Apply rule-based inference to generate insights from feature scores.
     * 
     * This is where the "AI" logic lives - but it's deterministic rules, not ML.
     */
    private AiInferencePipeline.InferenceInsights applyInferenceRules(CityFeatureInput input) {
        return AiInferencePipeline.InferenceInsights.builder()
            .personality(generatePersonality(input))
            .strengths(generateStrengths(input))
            .weaknesses(generateWeaknesses(input))
            .audienceSegments(generateAudienceSegments(input))
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RULE: PERSONALITY GENERATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * RULE: Generate 2-4 sentence personality based on dominant characteristics.
     * 
     * LOGIC:
     * 1. Find the highest-scoring feature (primary characteristic)
     * 2. Mention 1-2 supporting features (good tier)
     * 3. Note significant trade-offs (if any poor scores)
     */
    private String generatePersonality(CityFeatureInput input) {
        StringBuilder personality = new StringBuilder();
        
        // Sentence 1: Primary characteristic (highest score)
        String primaryTrait = determinePrimaryTrait(input);
        personality.append(primaryTrait).append(". ");
        
        // Sentence 2-3: Supporting characteristics
        List<String> supportingTraits = determineSupportingTraits(input);
        if (!supportingTraits.isEmpty()) {
            personality.append(String.join(", ", supportingTraits)).append(". ");
        }
        
        // Sentence 4: Trade-offs or challenges (if any)
        String tradeoff = determineTradeoff(input);
        if (tradeoff != null) {
            personality.append(tradeoff).append(".");
        }
        
        return personality.toString().trim();
    }
    
    private String determinePrimaryTrait(CityFeatureInput input) {
        String city = input.getCityIdentifier().getName();
        
        // Find highest score
        Double economyScore = getScore(input.getEconomyFeatures());
        Double livabilityScore = getScore(input.getLivabilityFeatures());
        Double sustainabilityScore = getScore(input.getSustainabilityFeatures());
        
        Double maxScore = maxOf(economyScore, livabilityScore, sustainabilityScore);
        
        if (maxScore == null) {
            return String.format("%s is a city with emerging data", city);
        }
        
        if (economyScore != null && economyScore.equals(maxScore) && economyScore >= 60) {
            return String.format("%s is a thriving economic hub with strong job opportunities", city);
        } else if (livabilityScore != null && livabilityScore.equals(maxScore) && livabilityScore >= 60) {
            return String.format("%s offers excellent quality of life with diverse amenities", city);
        } else if (sustainabilityScore != null && sustainabilityScore.equals(maxScore) && sustainabilityScore >= 60) {
            return String.format("%s maintains good environmental standards", city);
        } else {
            return String.format("%s is a balanced city with moderate characteristics", city);
        }
    }
    
    private List<String> determineSupportingTraits(CityFeatureInput input) {
        List<String> traits = new ArrayList<>();
        
        CityFeatureInput.EconomyFeatures economy = input.getEconomyFeatures();
        CityFeatureInput.LivabilityFeatures livability = input.getLivabilityFeatures();
        CityFeatureInput.SustainabilityFeatures sustainability = input.getSustainabilityFeatures();
        
        // Add secondary good traits
        if (economy != null && economy.getEconomyScore() != null && 
            economy.getEconomyScore() >= 60 && economy.getEconomyScore() < 80) {
            if (economy.getGdpPerCapita() != null && economy.getGdpPerCapita() >= ECONOMY.highGdpPerCapita()) {
                traits.add("with a strong economic base");
            }
        }
        
        if (livability != null && livability.getLivabilityScore() != null && 
            livability.getLivabilityScore() >= 60) {
            traits.add("providing good livability for residents");
        }
        
        if (sustainability != null && sustainability.getSustainabilityScore() != null && 
            sustainability.getSustainabilityScore() >= 60) {
            traits.add("featuring decent environmental conditions");
        }
        
        return traits.size() > 2 ? traits.subList(0, 2) : traits;
    }
    
    private String determineTradeoff(CityFeatureInput input) {
        CityFeatureInput.EconomyFeatures economy = input.getEconomyFeatures();
        
        // High cost of living trade-off
        if (economy != null && economy.getCostOfLivingIndex() != null && 
            economy.getCostOfLivingIndex() >= ECONOMY.highCostOfLiving()) {
            return "though the high cost of living presents challenges for newcomers";
        }
        
        // Poor sustainability
        CityFeatureInput.SustainabilityFeatures sustainability = input.getSustainabilityFeatures();
        if (sustainability != null && sustainability.getSustainabilityScore() != null && 
            sustainability.getSustainabilityScore() < 40) {
            return "with environmental quality concerns requiring attention";
        }
        
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RULE: STRENGTHS GENERATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * RULE: Generate 2-6 strengths for scores >= 60 (GOOD or EXCELLENT tier).
     * 
     * LOGIC:
     * - Score >= 80: "Excellent [category]"
     * - Score 60-79: "Strong [category]"
     * - Include score attribution
     */
    private List<String> generateStrengths(CityFeatureInput input) {
        List<String> strengths = new ArrayList<>();
        
        CityFeatureInput.EconomyFeatures economy = input.getEconomyFeatures();
        CityFeatureInput.LivabilityFeatures livability = input.getLivabilityFeatures();
        CityFeatureInput.SustainabilityFeatures sustainability = input.getSustainabilityFeatures();
        CityFeatureInput.GrowthFeatures growth = input.getGrowthFeatures();
        
        // Economy strength
        if (economy != null && economy.getEconomyScore() != null && economy.getEconomyScore() >= 60) {
            String prefix = economy.getEconomyScore() >= 80 ? "Excellent" : "Strong";
            strengths.add(String.format("%s economy with diverse opportunities (score: %.0f/100)", 
                prefix, economy.getEconomyScore()));
        }
        
        // Livability strength
        if (livability != null && livability.getLivabilityScore() != null && livability.getLivabilityScore() >= 60) {
            String prefix = livability.getLivabilityScore() >= 80 ? "Exceptional" : "Good";
            strengths.add(String.format("%s quality of life with amenities (score: %.0f/100)", 
                prefix, livability.getLivabilityScore()));
        }
        
        // Sustainability strength
        if (sustainability != null && sustainability.getSustainabilityScore() != null && 
            sustainability.getSustainabilityScore() >= 60) {
            String prefix = sustainability.getSustainabilityScore() >= 80 ? "Excellent" : "Good";
            strengths.add(String.format("%s environmental quality (score: %.0f/100)", 
                prefix, sustainability.getSustainabilityScore()));
        }
        
        // Growth strength
        if (growth != null && growth.getGrowthScore() != null && growth.getGrowthScore() >= 60) {
            String prefix = growth.getGrowthScore() >= 80 ? "Exceptional" : "Strong";
            strengths.add(String.format("%s growth trajectory (score: %.0f/100)", 
                prefix, growth.getGrowthScore()));
        }
        
        // Ensure minimum 2 strengths
        if (strengths.isEmpty()) {
            strengths.add("Emerging city with development potential");
            strengths.add("Diverse community characteristics");
        } else if (strengths.size() == 1) {
            strengths.add("Balanced urban characteristics");
        }
        
        return strengths;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RULE: WEAKNESSES GENERATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * RULE: Generate 1-5 weaknesses for scores < 40 (BELOW-AVERAGE or POOR tier).
     * 
     * LOGIC:
     * - Score < 20: "Significant [category] challenges"
     * - Score 20-39: "[Category] considerations"
     * - Use constructive framing
     */
    private List<String> generateWeaknesses(CityFeatureInput input) {
        List<String> weaknesses = new ArrayList<>();
        
        CityFeatureInput.EconomyFeatures economy = input.getEconomyFeatures();
        CityFeatureInput.LivabilityFeatures livability = input.getLivabilityFeatures();
        CityFeatureInput.SustainabilityFeatures sustainability = input.getSustainabilityFeatures();
        CityFeatureInput.GrowthFeatures growth = input.getGrowthFeatures();
        
        // Economy weakness
        if (economy != null && economy.getEconomyScore() != null && economy.getEconomyScore() < 40) {
            String prefix = economy.getEconomyScore() < 20 ? "Significant economic challenges" : "Economic considerations";
            weaknesses.add(String.format("%s with limited opportunities (score: %.0f/100)", 
                prefix, economy.getEconomyScore()));
        }
        
        // Livability weakness
        if (livability != null && livability.getLivabilityScore() != null && livability.getLivabilityScore() < 40) {
            String prefix = livability.getLivabilityScore() < 20 ? "Quality of life concerns" : "Livability considerations";
            weaknesses.add(String.format("%s requiring attention (score: %.0f/100)", 
                prefix, livability.getLivabilityScore()));
        }
        
        // Sustainability weakness
        if (sustainability != null && sustainability.getSustainabilityScore() != null && 
            sustainability.getSustainabilityScore() < 40) {
            String prefix = sustainability.getSustainabilityScore() < 20 ? "Significant environmental concerns" 
                : "Environmental quality considerations";
            weaknesses.add(String.format("%s (score: %.0f/100)", 
                prefix, sustainability.getSustainabilityScore()));
        }
        
        // Growth weakness
        if (growth != null && growth.getGrowthScore() != null && growth.getGrowthScore() < 40) {
            String prefix = growth.getGrowthScore() < 20 ? "Limited growth prospects" : "Moderate growth trajectory";
            weaknesses.add(String.format("%s (score: %.0f/100)", 
                prefix, growth.getGrowthScore()));
        }
        
        // Minimum 1 weakness if we have low scores, otherwise acceptable to return empty
        return weaknesses;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RULE: AUDIENCE SEGMENTATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * RULE: Generate 2-6 audience segments based on feature score combinations.
     * 
     * MAPPING RULES:
     * - Economy >= 60 → Career professionals
     * - Livability >= 60 → Families
     * - Sustainability >= 70 → Environmentally conscious
     * - Growth >= 60 → Entrepreneurs
     */
    private List<String> generateAudienceSegments(CityFeatureInput input) {
        List<String> segments = new ArrayList<>();
        
        CityFeatureInput.EconomyFeatures economy = input.getEconomyFeatures();
        CityFeatureInput.LivabilityFeatures livability = input.getLivabilityFeatures();
        CityFeatureInput.SustainabilityFeatures sustainability = input.getSustainabilityFeatures();
        CityFeatureInput.GrowthFeatures growth = input.getGrowthFeatures();
        
        // Career-focused professionals
        if (economy != null && economy.getEconomyScore() != null && economy.getEconomyScore() >= 60) {
            segments.add("Career-focused professionals seeking strong job markets");
        }
        
        // Families
        if (livability != null && livability.getLivabilityScore() != null && livability.getLivabilityScore() >= 60) {
            segments.add("Families looking for quality of life and amenities");
        }
        
        // Remote workers (economy + livability)
        if (economy != null && economy.getEconomyScore() != null && economy.getEconomyScore() >= 60 &&
            livability != null && livability.getLivabilityScore() != null && livability.getLivabilityScore() >= 60) {
            segments.add("Remote workers seeking work-life balance");
        }
        
        // Environmentally conscious
        if (sustainability != null && sustainability.getSustainabilityScore() != null && 
            sustainability.getSustainabilityScore() >= 70) {
            segments.add("Environmentally conscious individuals valuing clean air");
        }
        
        // Entrepreneurs
        if (growth != null && growth.getGrowthScore() != null && growth.getGrowthScore() >= 60) {
            segments.add("Entrepreneurs and startup founders");
        }
        
        // Budget-conscious (low cost of living)
        if (economy != null && economy.getCostOfLivingIndex() != null && 
            economy.getCostOfLivingIndex() < ECONOMY.moderateCostOfLiving()) {
            segments.add("Budget-conscious individuals seeking affordability");
        }
        
        // Ensure minimum 2 segments
        if (segments.isEmpty()) {
            segments.add("Individuals seeking diverse urban experiences");
            segments.add("Open-minded explorers");
        } else if (segments.size() == 1) {
            segments.add("Urban lifestyle enthusiasts");
        }
        
        // Cap at 6 segments
        return segments.size() > 6 ? segments.subList(0, 6) : segments;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STAGE 3: OUTPUT VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private PromptConstraints.ValidationResult validateOutput(AiInferencePipeline.InferenceInsights insights) {
        return PromptConstraints.validateOutput(
            insights.personality(),
            insights.strengths(),
            insights.weaknesses(),
            insights.audienceSegments()
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private Double getScore(CityFeatureInput.EconomyFeatures features) {
        return features != null ? features.getEconomyScore() : null;
    }
    
    private Double getScore(CityFeatureInput.LivabilityFeatures features) {
        return features != null ? features.getLivabilityScore() : null;
    }
    
    private Double getScore(CityFeatureInput.SustainabilityFeatures features) {
        return features != null ? features.getSustainabilityScore() : null;
    }
    
    private Double maxOf(Double... values) {
        Double max = null;
        for (Double value : values) {
            if (value != null && (max == null || value > max)) {
                max = value;
            }
        }
        return max;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXCEPTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static class AiInferenceException extends RuntimeException {
        public AiInferenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
