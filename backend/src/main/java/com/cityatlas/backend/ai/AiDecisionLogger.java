package com.cityatlas.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Logs AI decisions for auditability and debugging.
 * Creates detailed audit trail of inference process.
 */
@Service
public class AiDecisionLogger {

    private static final Logger log = LoggerFactory.getLogger(AiDecisionLogger.class);
    
    /**
     * Logs the complete inference decision process.
     */
    public AuditLog logInference(
        CityFeatureInput input,
        AiInferencePipeline.InferenceResult result,
        DataQualityChecker.DataQualityResult dataQuality,
        ConfidenceCalculator.ConfidenceResult confidence
    ) {
        String auditId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        
        // Log to SLF4J for persistence
        String cityName = input.getCityIdentifier() != null ? input.getCityIdentifier().getName() : "Unknown";
        log.info("AI Inference Audit [{}] - City: {}, Confidence: {}, Valid: {}", 
            auditId, 
            cityName, 
            confidence.level(),
            result.valid()
        );
        
        // Create detailed audit log
        String cityCountry = input.getCityIdentifier() != null ? input.getCityIdentifier().getCountry() : "Unknown";
        
        // Build InferenceInsights from result
        AiInferencePipeline.InferenceInsights insights = AiInferencePipeline.InferenceInsights.builder()
            .personality(result.personality())
            .strengths(result.strengths())
            .weaknesses(result.weaknesses())
            .audienceSegments(result.bestSuitedFor())
            .build();
        
        AuditLog auditLog = new AuditLog(
            auditId,
            timestamp,
            cityName,
            cityCountry,
            insights,
            dataQuality,
            confidence,
            result.inferenceTimeMs(),
            extractAppliedRules(input, insights),
            extractValidationMessages(result)
        );
        
        // Log key decision points
        logDecisionPoints(auditLog);
        
        return auditLog;
    }
    
    /**
     * Extracts which rules were applied during inference.
     */
    private List<AppliedRule> extractAppliedRules(
        CityFeatureInput input,
        AiInferencePipeline.InferenceInsights insights
    ) {
        List<AppliedRule> rules = new ArrayList<>();
        
        // Strength rules
        for (String strength : insights.strengths()) {
            String rule = identifyStrengthRule(strength, input);
            if (rule != null) {
                rules.add(new AppliedRule(rule, strength, "STRENGTH"));
            }
        }
        
        // Weakness rules
        for (String weakness : insights.weaknesses()) {
            String rule = identifyWeaknessRule(weakness, input);
            if (rule != null) {
                rules.add(new AppliedRule(rule, weakness, "WEAKNESS"));
            }
        }
        
        // Audience rules
        for (String audience : insights.audienceSegments()) {
            String rule = identifyAudienceRule(audience, input);
            if (rule != null) {
                rules.add(new AppliedRule(rule, audience, "AUDIENCE"));
            }
        }
        
        return rules;
    }
    
    /**
     * Identifies which rule generated a strength.
     */
    private String identifyStrengthRule(String strength, CityFeatureInput input) {
        Double economy = input.getEconomyFeatures() != null ? input.getEconomyFeatures().getEconomyScore() : null;
        Double livability = input.getLivabilityFeatures() != null ? input.getLivabilityFeatures().getLivabilityScore() : null;
        Double sustainability = input.getSustainabilityFeatures() != null ? input.getSustainabilityFeatures().getSustainabilityScore() : null;
        
        if (strength.contains("Excellent economy") && economy != null && economy >= 80) {
            return String.format("Economy >= 80 (actual: %.1f)", economy);
        }
        if (strength.contains("Strong economy") && economy != null && economy >= 60) {
            return String.format("Economy >= 60 (actual: %.1f)", economy);
        }
        if (strength.contains("livability") && livability != null && livability >= 70) {
            return String.format("Livability >= 70 (actual: %.1f)", livability);
        }
        if (strength.contains("sustainable") && sustainability != null && sustainability >= 70) {
            return String.format("Sustainability >= 70 (actual: %.1f)", sustainability);
        }
        
        return "Rule not identified";
    }
    
    /**
     * Identifies which rule generated a weakness.
     */
    private String identifyWeaknessRule(String weakness, CityFeatureInput input) {
        Double economy = input.getEconomyFeatures() != null ? input.getEconomyFeatures().getEconomyScore() : null;
        Double livability = input.getLivabilityFeatures() != null ? input.getLivabilityFeatures().getLivabilityScore() : null;
        Double sustainability = input.getSustainabilityFeatures() != null ? input.getSustainabilityFeatures().getSustainabilityScore() : null;
        
        if (weakness.contains("Economic challenges") && economy != null && economy < 40) {
            return String.format("Economy < 40 (actual: %.1f)", economy);
        }
        if (weakness.contains("Quality of life") && livability != null && livability < 40) {
            return String.format("Livability < 40 (actual: %.1f)", livability);
        }
        if (weakness.contains("environmental") && sustainability != null && sustainability < 40) {
            return String.format("Sustainability < 40 (actual: %.1f)", sustainability);
        }
        
        return "Rule not identified";
    }
    
    /**
     * Identifies which rule generated an audience segment.
     */
    private String identifyAudienceRule(String audience, CityFeatureInput input) {
        Double economy = input.getEconomyFeatures() != null ? input.getEconomyFeatures().getEconomyScore() : null;
        Double livability = input.getLivabilityFeatures() != null ? input.getLivabilityFeatures().getLivabilityScore() : null;
        Double sustainability = input.getSustainabilityFeatures() != null ? input.getSustainabilityFeatures().getSustainabilityScore() : null;
        
        if (audience.contains("Career-focused professionals") && economy != null && economy >= 60) {
            return String.format("Economy >= 60 (actual: %.1f)", economy);
        }
        if (audience.contains("Remote workers") && economy != null && livability != null 
            && economy >= 60 && livability >= 60) {
            return String.format("Economy >= 60 AND Livability >= 60 (actual: %.1f, %.1f)", economy, livability);
        }
        if (audience.contains("Environmentally conscious") && sustainability != null && sustainability >= 70) {
            return String.format("Sustainability >= 70 (actual: %.1f)", sustainability);
        }
        
        return "Rule not identified";
    }
    
    /**
     * Extracts validation messages from inference result.
     */
    private List<String> extractValidationMessages(AiInferencePipeline.InferenceResult result) {
        List<String> messages = new ArrayList<>();
        
        if (!result.valid()) {
            if (result.validationErrors() != null && !result.validationErrors().isEmpty()) {
                messages.add(result.validationErrors());
            }
        } else {
            messages.add("All validations passed");
        }
        
        return messages;
    }
    
    /**
     * Logs key decision points for debugging.
     */
    private void logDecisionPoints(AuditLog auditLog) {
        log.debug("Audit [{}] - Data Quality: {}", 
            auditLog.auditId, 
            auditLog.dataQuality.getSummary()
        );
        
        log.debug("Audit [{}] - Confidence: {} ({})", 
            auditLog.auditId,
            String.format("%.1f%%", auditLog.confidence.overallConfidence()),
            auditLog.confidence.reasoning()
        );
        
        log.debug("Audit [{}] - Applied {} rules in {}ms", 
            auditLog.auditId,
            auditLog.appliedRules.size(),
            auditLog.inferenceTimeMs
        );
        
        // Log each applied rule
        for (AppliedRule rule : auditLog.appliedRules) {
            log.debug("Audit [{}] - Rule: {} -> {} ({})", 
                auditLog.auditId,
                rule.ruleCondition,
                rule.outputGenerated,
                rule.category
            );
        }
    }
    
    /**
     * Complete audit log for an inference decision.
     */
    public record AuditLog(
        String auditId,
        Instant timestamp,
        String cityName,
        String country,
        AiInferencePipeline.InferenceInsights insights,
        DataQualityChecker.DataQualityResult dataQuality,
        ConfidenceCalculator.ConfidenceResult confidence,
        long inferenceTimeMs,
        List<AppliedRule> appliedRules,
        List<String> validationMessages
    ) {
        public String toSummary() {
            return String.format(
                "Audit %s: %s, %s - Confidence: %s (%.1f%%), Rules: %d, Time: %dms",
                auditId.substring(0, 8),
                cityName,
                country,
                confidence.level(),
                confidence.overallConfidence(),
                appliedRules.size(),
                inferenceTimeMs
            );
        }
    }
    
    /**
     * Record of a rule application.
     */
    public record AppliedRule(
        String ruleCondition,      // e.g., "Economy >= 80 (actual: 85.0)"
        String outputGenerated,    // e.g., "Excellent economy with robust job market"
        String category            // STRENGTH, WEAKNESS, AUDIENCE
    ) {}
}
