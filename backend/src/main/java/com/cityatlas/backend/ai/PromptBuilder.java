package com.cityatlas.backend.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.service.CityFeatureComputer;
import com.cityatlas.backend.service.CityFeatureComputer.CityFeatures;
import com.cityatlas.backend.service.CityFeatureComputer.ScoreResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PROMPT BUILDER SERVICE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Bridges the gap between City entities and structured prompt templates.
 * Transforms raw city data into CityFeatureInput for template rendering.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * RESPONSIBILITIES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. Convert City entity → CityFeatureInput (structured input)
 * 2. Apply CityFeatureComputer scores to input
 * 3. Calculate data quality metrics
 * 4. Render prompts using templates
 * 5. Validate output against constraints
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * ARCHITECTURE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────┐    ┌───────────────────┐    ┌─────────────────┐
 *   │ City Entity │───▶│ PromptBuilder     │───▶│ CityFeatureInput│
 *   └─────────────┘    │ (this service)    │    └────────┬────────┘
 *                      └───────────────────┘             │
 *                                                        ▼
 *   ┌─────────────┐    ┌───────────────────┐    ┌─────────────────┐
 *   │ Rendered    │◀───│ PromptTemplate    │◀───│ Variable Map    │
 *   │ Prompt      │    │ (fixed structure) │    │                 │
 *   └─────────────┘    └───────────────────┘    └─────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * INTERVIEW TALKING POINTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * - "Single Responsibility: Only transforms data, doesn't generate content"
 * - "Dependency Injection: Uses CityFeatureComputer for score calculation"
 * - "Type Safety: All transformations are strongly typed"
 * - "Determinism: Same city always produces same prompt"
 * 
 * @see CityFeatureInput
 * @see PromptTemplate
 * @see PromptTemplates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {
    
    private final CityFeatureComputer featureComputer;
    
    // Score tier thresholds (canonical values from PromptConstraints)
    private static final PromptConstraints.ScoreThresholds THRESHOLDS = 
        PromptConstraints.ScoreThresholds.DEFAULT;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Build structured feature input from a City entity.
     * 
     * @param city The city entity
     * @return Structured input ready for template rendering
     */
    public CityFeatureInput buildFeatureInput(City city) {
        log.debug("Building feature input for city: {}", city.getName());
        
        // Compute features using the existing service
        // Note: Pass null for optional parameters (currentAqi, populationGrowthRate, gdpGrowthRate)
        CityFeatures features = featureComputer.computeFeatures(city, null, null, null);
        
        return CityFeatureInput.builder()
            .cityIdentifier(buildCityIdentifier(city))
            .economyFeatures(buildEconomyFeatures(city, features.getEconomyScore()))
            .livabilityFeatures(buildLivabilityFeatures(city, features.getLivabilityScore()))
            .sustainabilityFeatures(buildSustainabilityFeatures(features.getSustainabilityScore()))
            .growthFeatures(buildGrowthFeatures(features.getGrowthScore()))
            .overallAssessment(buildOverallAssessment(features.getOverallScore()))
            .dataQuality(buildDataQuality(features))
            .build();
    }
    
    /**
     * Render a specific prompt template for a city.
     * 
     * @param city The city entity
     * @param template The template to render
     * @return Fully rendered prompt string
     */
    public String renderPrompt(City city, PromptTemplate template) {
        CityFeatureInput input = buildFeatureInput(city);
        return template.render(input);
    }
    
    /**
     * Render the city personality prompt.
     */
    public String renderPersonalityPrompt(City city) {
        return renderPrompt(city, PromptTemplates.CITY_PERSONALITY);
    }
    
    /**
     * Render the city strengths prompt.
     */
    public String renderStrengthsPrompt(City city) {
        return renderPrompt(city, PromptTemplates.CITY_STRENGTHS);
    }
    
    /**
     * Render the city weaknesses prompt.
     */
    public String renderWeaknessesPrompt(City city) {
        return renderPrompt(city, PromptTemplates.CITY_WEAKNESSES);
    }
    
    /**
     * Render the city audience prompt.
     */
    public String renderAudiencePrompt(City city) {
        return renderPrompt(city, PromptTemplates.CITY_AUDIENCE);
    }
    
    /**
     * Render the complete city summary prompt.
     */
    public String renderCompleteSummaryPrompt(City city) {
        return renderPrompt(city, PromptTemplates.CITY_COMPLETE_SUMMARY);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private CityFeatureInput.CityIdentifier buildCityIdentifier(City city) {
        return CityFeatureInput.CityIdentifier.builder()
            .slug(city.getSlug())
            .name(city.getName())
            .state(city.getState())
            .country(city.getCountry())
            .population(city.getPopulation())
            .sizeCategory(categorizeCitySize(city.getPopulation()))
            .build();
    }
    
    private CityFeatureInput.EconomyFeatures buildEconomyFeatures(City city, ScoreResult score) {
        return CityFeatureInput.EconomyFeatures.builder()
            .gdpPerCapita(city.getGdpPerCapita())
            .unemploymentRate(city.getUnemploymentRate())
            .costOfLivingIndex(city.getCostOfLivingIndex())
            .economyScore(score.score())
            .economyTier(THRESHOLDS.getTier(score.score()))
            .explanation(score.explanation())
            .components(score.components())
            .build();
    }
    
    private CityFeatureInput.LivabilityFeatures buildLivabilityFeatures(City city, ScoreResult score) {
        return CityFeatureInput.LivabilityFeatures.builder()
            .costOfLivingIndex(city.getCostOfLivingIndex())
            .population(city.getPopulation())
            .livabilityScore(score.score())
            .livabilityTier(THRESHOLDS.getTier(score.score()))
            .explanation(score.explanation())
            .components(score.components())
            .build();
    }
    
    private CityFeatureInput.SustainabilityFeatures buildSustainabilityFeatures(ScoreResult score) {
        return CityFeatureInput.SustainabilityFeatures.builder()
            .sustainabilityScore(score.score())
            .sustainabilityTier(THRESHOLDS.getTier(score.score()))
            .explanation(score.explanation())
            .components(score.components())
            .build();
    }
    
    private CityFeatureInput.GrowthFeatures buildGrowthFeatures(ScoreResult score) {
        return CityFeatureInput.GrowthFeatures.builder()
            .growthScore(score.score())
            .growthTier(THRESHOLDS.getTier(score.score()))
            .explanation(score.explanation())
            .components(score.components())
            .build();
    }
    
    private CityFeatureInput.OverallAssessment buildOverallAssessment(ScoreResult score) {
        return CityFeatureInput.OverallAssessment.builder()
            .overallScore(score.score())
            .overallTier(THRESHOLDS.getTier(score.score()))
            .explanation(score.explanation())
            .weights(Map.of(
                "economy", 0.30,
                "livability", 0.35,
                "sustainability", 0.20,
                "growth", 0.15
            ))
            .build();
    }
    
    private CityFeatureInput.DataQualityMetadata buildDataQuality(CityFeatures features) {
        List<String> missingFields = new ArrayList<>();
        int availableScores = 0;
        int totalScores = 4;
        
        // Check each feature score
        if (features.getEconomyScore().score() != null) availableScores++; 
        else missingFields.add("economy");
        
        if (features.getLivabilityScore().score() != null) availableScores++; 
        else missingFields.add("livability");
        
        if (features.getSustainabilityScore().score() != null) availableScores++; 
        else missingFields.add("sustainability");
        
        if (features.getGrowthScore().score() != null) availableScores++; 
        else missingFields.add("growth");
        
        double completeness = (availableScores * 100.0) / totalScores;
        double confidence = calculateConfidence(features);
        
        return CityFeatureInput.DataQualityMetadata.builder()
            .completenessPercentage(completeness)
            .missingFields(missingFields)
            .freshnessCategory("fresh") // Could be computed from data timestamps
            .confidenceScore(confidence)
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String categorizeCitySize(Long population) {
        if (population == null) return "unknown";
        if (population >= 1_000_000) return "major";
        if (population >= 100_000) return "mid-sized";
        return "small";
    }
    
    private double calculateConfidence(CityFeatures features) {
        // Average of individual score confidences
        double totalConfidence = 0;
        int count = 0;
        
        if (features.getEconomyScore().confidence() != null) {
            totalConfidence += features.getEconomyScore().confidence();
            count++;
        }
        if (features.getLivabilityScore().confidence() != null) {
            totalConfidence += features.getLivabilityScore().confidence();
            count++;
        }
        if (features.getSustainabilityScore().confidence() != null) {
            totalConfidence += features.getSustainabilityScore().confidence();
            count++;
        }
        if (features.getGrowthScore().confidence() != null) {
            totalConfidence += features.getGrowthScore().confidence();
            count++;
        }
        
        return count > 0 ? totalConfidence / count : 0.0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Validate that a rendered prompt meets constraints.
     * 
     * @param prompt The rendered prompt
     * @return Validation result
     */
    public PromptConstraints.ValidationResult validatePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return new PromptConstraints.ValidationResult(false, "Prompt is empty");
        }
        
        // Check for forbidden topics
        for (String forbidden : PromptConstraints.FORBIDDEN_TOPICS) {
            if (prompt.toLowerCase().contains(forbidden.toLowerCase())) {
                return new PromptConstraints.ValidationResult(
                    false, 
                    "Prompt contains forbidden topic: " + forbidden
                );
            }
        }
        
        return new PromptConstraints.ValidationResult(true, "");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROMPT LOGGING (for audit trail)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Record for audit logging of prompt generation.
     */
    public record PromptAuditLog(
        String citySlug,
        String templateId,
        String templateVersion,
        String promptHash,
        java.time.Instant generatedAt,
        double dataConfidence
    ) {}
    
    /**
     * Generate audit log for a rendered prompt.
     */
    public PromptAuditLog createAuditLog(City city, PromptTemplate template, String renderedPrompt) {
        CityFeatureInput input = buildFeatureInput(city);
        
        double confidence = 0.0;
        if (input.getDataQuality() != null) {
            Double confScore = input.getDataQuality().getConfidenceScore();
            if (confScore != null) {
                confidence = confScore;
            }
        }
        
        return new PromptAuditLog(
            city.getSlug(),
            template.getTemplateId(),
            template.getVersion(),
            computeHash(renderedPrompt),
            java.time.Instant.now(),
            confidence
        );
    }
    
    private String computeHash(String content) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16); // First 16 chars
        } catch (java.security.NoSuchAlgorithmException e) {
            return "hash-error";
        }
    }
}
