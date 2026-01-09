package com.cityatlas.backend.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cityatlas.backend.entity.City;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * AI INFERENCE SERVICE TESTS - Demonstrating Pipeline Testability
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Showcases how the modular inference pipeline enables comprehensive unit testing.
 * Each rule can be tested in isolation with controlled inputs.
 * 
 * TEST STRATEGY:
 * 1. Mock PromptBuilder to return controlled CityFeatureInput
 * 2. Execute inference pipeline
 * 3. Assert output matches expected rules
 * 
 * @see AiInferenceService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI Inference Pipeline Tests")
class AiInferenceServiceTest {
    
    @Mock
    private PromptBuilder promptBuilder;
    
    @InjectMocks
    private AiInferenceService inferenceService;
    
    private City testCity;
    
    @BeforeEach
    void setUp() {
        testCity = new City();
        testCity.setSlug("test-city");
        testCity.setName("Test City");
        testCity.setCountry("USA");
        testCity.setPopulation(500000L);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST: STRENGTH GENERATION RULES
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("RULE: Economy score >= 80 should generate 'Excellent economy' strength")
    void testExcellentEconomyGeneratesCorrectStrength() {
        // GIVEN: A city with excellent economy score (85/100)
        CityFeatureInput input = createInputWithEconomyScore(85.0);
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Should generate "Excellent economy" strength
        assertTrue(result.isSuccessful());
        assertTrue(result.strengths().stream()
            .anyMatch(s -> s.contains("Excellent economy") && s.contains("85/100")));
    }
    
    @Test
    @DisplayName("RULE: Economy score 60-79 should generate 'Strong economy' strength")
    void testGoodEconomyGeneratesCorrectStrength() {
        // GIVEN: A city with good economy score (68/100)
        CityFeatureInput input = createInputWithEconomyScore(68.0);
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Should generate "Strong economy" strength
        assertTrue(result.isSuccessful());
        assertTrue(result.strengths().stream()
            .anyMatch(s -> s.contains("Strong economy") && s.contains("68/100")));
    }
    
    @Test
    @DisplayName("RULE: Economy score < 60 should NOT generate economy strength")
    void testBelowAverageEconomyDoesNotGenerateStrength() {
        // GIVEN: A city with below-average economy score (45/100)
        CityFeatureInput input = createInputWithEconomyScore(45.0);
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Should NOT generate economy strength
        assertTrue(result.isSuccessful());
        assertFalse(result.strengths().stream()
            .anyMatch(s -> s.toLowerCase().contains("economy")));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST: WEAKNESS GENERATION RULES
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("RULE: Economy score < 40 should generate economy weakness")
    void testPoorEconomyGeneratesWeakness() {
        // GIVEN: A city with poor economy score (32/100)
        CityFeatureInput input = createInputWithEconomyScore(32.0);
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Should generate economy weakness
        assertTrue(result.isSuccessful());
        assertTrue(result.weaknesses().stream()
            .anyMatch(w -> w.toLowerCase().contains("economic") && w.contains("32/100")));
    }
    
    @Test
    @DisplayName("RULE: All scores >= 60 should generate NO weaknesses")
    void testAllGoodScoresGenerateNoWeaknesses() {
        // GIVEN: A city with all good scores
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(createCityIdentifier())
            .economyFeatures(createEconomyFeatures(75.0))
            .livabilityFeatures(createLivabilityFeatures(70.0))
            .sustainabilityFeatures(createSustainabilityFeatures(65.0))
            .growthFeatures(createGrowthFeatures(62.0))
            .dataQuality(createDataQuality(100.0))
            .build();
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Should have NO weaknesses
        assertTrue(result.isSuccessful());
        assertTrue(result.weaknesses() == null || result.weaknesses().isEmpty());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST: AUDIENCE SEGMENTATION RULES
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("RULE: Economy >= 60 should recommend 'Career-focused professionals'")
    void testGoodEconomyRecommendsCareerProfessionals() {
        // GIVEN: A city with good economy
        CityFeatureInput input = createInputWithEconomyScore(72.0);
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Should recommend career professionals
        assertTrue(result.isSuccessful());
        assertTrue(result.bestSuitedFor().stream()
            .anyMatch(a -> a.contains("Career-focused professionals")));
    }
    
    @Test
    @DisplayName("RULE: Economy >= 60 AND Livability >= 60 should recommend 'Remote workers'")
    void testGoodEconomyAndLivabilityRecommendsRemoteWorkers() {
        // GIVEN: A city with good economy AND livability
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(createCityIdentifier())
            .economyFeatures(createEconomyFeatures(70.0))
            .livabilityFeatures(createLivabilityFeatures(68.0))
            .sustainabilityFeatures(createSustainabilityFeatures(50.0))
            .growthFeatures(createGrowthFeatures(45.0))
            .dataQuality(createDataQuality(100.0))
            .build();
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Should recommend remote workers
        assertTrue(result.isSuccessful());
        assertTrue(result.bestSuitedFor().stream()
            .anyMatch(a -> a.contains("Remote workers")));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST: OUTPUT VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("OUTPUT: Should always have 2-6 strengths")
    void testOutputHasValidStrengthsCount() {
        // GIVEN: Any city input
        CityFeatureInput input = createInputWithEconomyScore(70.0);
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Should have 2-6 strengths
        assertTrue(result.isSuccessful());
        assertTrue(result.strengths().size() >= 2 && result.strengths().size() <= 6);
    }
    
    @Test
    @DisplayName("OUTPUT: Should always have 2-6 audience segments")
    void testOutputHasValidAudienceCount() {
        // GIVEN: Any city input
        CityFeatureInput input = createInputWithEconomyScore(70.0);
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Should have 2-6 audience segments
        assertTrue(result.isSuccessful());
        assertTrue(result.bestSuitedFor().size() >= 2 && result.bestSuitedFor().size() <= 6);
    }
    
    @Test
    @DisplayName("OUTPUT: Personality should be <= 500 characters")
    void testPersonalityLengthIsWithinBounds() {
        // GIVEN: Any city input
        CityFeatureInput input = createInputWithEconomyScore(70.0);
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline
        AiInferencePipeline.InferenceResult result = inferenceService.runInference(testCity);
        
        // THEN: Personality should be within bounds
        assertTrue(result.isSuccessful());
        assertTrue(result.personality().length() <= 500);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST: DETERMINISM
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("DETERMINISM: Same input should produce identical output")
    void testDeterminismSameInputProducesSameOutput() {
        // GIVEN: Same city input
        CityFeatureInput input = createInputWithEconomyScore(75.0);
        when(promptBuilder.buildFeatureInput(any(City.class))).thenReturn(input);
        
        // WHEN: Run inference pipeline twice
        AiInferencePipeline.InferenceResult result1 = inferenceService.runInference(testCity);
        AiInferencePipeline.InferenceResult result2 = inferenceService.runInference(testCity);
        
        // THEN: Results should be identical (except inferenceTimeMs)
        assertEquals(result1.personality(), result2.personality());
        assertEquals(result1.strengths(), result2.strengths());
        assertEquals(result1.weaknesses(), result2.weaknesses());
        assertEquals(result1.bestSuitedFor(), result2.bestSuitedFor());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private CityFeatureInput createInputWithEconomyScore(Double score) {
        return CityFeatureInput.builder()
            .cityIdentifier(createCityIdentifier())
            .economyFeatures(createEconomyFeatures(score))
            .livabilityFeatures(createLivabilityFeatures(50.0))
            .sustainabilityFeatures(createSustainabilityFeatures(50.0))
            .growthFeatures(createGrowthFeatures(50.0))
            .dataQuality(createDataQuality(100.0))
            .build();
    }
    
    private CityFeatureInput.CityIdentifier createCityIdentifier() {
        return CityFeatureInput.CityIdentifier.builder()
            .slug("test-city")
            .name("Test City")
            .country("USA")
            .population(500000L)
            .sizeCategory("mid-sized")
            .build();
    }
    
    private CityFeatureInput.EconomyFeatures createEconomyFeatures(Double score) {
        return CityFeatureInput.EconomyFeatures.builder()
            .economyScore(score)
            .economyTier(getTier(score))
            .gdpPerCapita(60000.0)
            .unemploymentRate(4.5)
            .costOfLivingIndex(100)
            .explanation("Test economy")
            .components(List.of("GDP", "Unemployment"))
            .build();
    }
    
    private CityFeatureInput.LivabilityFeatures createLivabilityFeatures(Double score) {
        return CityFeatureInput.LivabilityFeatures.builder()
            .livabilityScore(score)
            .livabilityTier(getTier(score))
            .explanation("Test livability")
            .components(List.of("Cost", "AQI"))
            .build();
    }
    
    private CityFeatureInput.SustainabilityFeatures createSustainabilityFeatures(Double score) {
        return CityFeatureInput.SustainabilityFeatures.builder()
            .sustainabilityScore(score)
            .sustainabilityTier(getTier(score))
            .explanation("Test sustainability")
            .components(List.of("AQI"))
            .build();
    }
    
    private CityFeatureInput.GrowthFeatures createGrowthFeatures(Double score) {
        return CityFeatureInput.GrowthFeatures.builder()
            .growthScore(score)
            .growthTier(getTier(score))
            .explanation("Test growth")
            .components(List.of("Population growth"))
            .build();
    }
    
    private CityFeatureInput.DataQualityMetadata createDataQuality(Double completeness) {
        return CityFeatureInput.DataQualityMetadata.builder()
            .completenessPercentage(completeness)
            .missingFields(List.of())
            .freshnessCategory("fresh")
            .confidenceScore(0.9)
            .build();
    }
    
    private String getTier(Double score) {
        if (score == null) return "unknown";
        if (score >= 80) return "excellent";
        if (score >= 60) return "good";
        if (score >= 40) return "average";
        if (score >= 20) return "below-average";
        return "poor";
    }
}
