package com.cityatlas.backend.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AiQualityGuard - validates inference blocking logic.
 */
class AiQualityGuardTest {

    private AiQualityGuard guard;
    private DataQualityChecker dataQualityChecker;

    @BeforeEach
    void setUp() {
        dataQualityChecker = new DataQualityChecker();
        guard = new AiQualityGuard(dataQualityChecker);
    }

    @Test
    void testHighQualityDataPasses() {
        CityFeatureInput input = createHighQualityInput();
        
        AiQualityGuard.GuardResult result = guard.validateForInference(input);
        
        assertTrue(result.shouldProceed(), "High quality data should pass");
        assertTrue(result.dataCompleteness() > 90.0);
        assertTrue(result.blockers().isEmpty());
    }

    @Test
    void testIdenticalScoresBlocked() {
        // All scores are 50.0 - suspicious!
        CityFeatureInput input = createInputWithIdenticalScores(50.0);
        
        AiQualityGuard.GuardResult result = guard.validateForInference(input);
        
        assertFalse(result.shouldProceed(), "Identical scores should be blocked");
        assertTrue(result.hasBlockers());
        assertTrue(result.blockers().stream()
            .anyMatch(b -> b.toLowerCase().contains("identical")));
    }

    @Test
    void testMultiplePerfectScoresGeneratesWarning() {
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .name("Test City")
                .country("USA")
                .population(500000L)
                .build())
            .economyFeatures(CityFeatureInput.EconomyFeatures.builder()
                .economyScore(100.0)
                .gdpPerCapita(85000.0)
                .unemploymentRate(4.5)
                .build())
            .livabilityFeatures(CityFeatureInput.LivabilityFeatures.builder()
                .livabilityScore(100.0)
                .build())
            .sustainabilityFeatures(CityFeatureInput.SustainabilityFeatures.builder()
                .sustainabilityScore(100.0)
                .build())
            .growthFeatures(CityFeatureInput.GrowthFeatures.builder()
                .growthScore(50.0)
                .build())
            .build();
        
        AiQualityGuard.GuardResult result = guard.validateForInference(input);
        
        assertTrue(result.shouldProceed(), "Should proceed but with warnings");
        assertTrue(result.hasRecommendations());
        assertTrue(result.recommendations().stream()
            .anyMatch(r -> r.toLowerCase().contains("perfect scores")));
    }

    @Test
    void testMissingPopulationGeneratesRecommendation() {
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .name("Test City")
                .country("USA")
                .population(null) // Missing population
                .build())
            .economyFeatures(createEconomyFeatures())
            .livabilityFeatures(createLivabilityFeatures())
            .sustainabilityFeatures(createSustainabilityFeatures())
            .growthFeatures(createGrowthFeatures())
            .build();
        
        AiQualityGuard.GuardResult result = guard.validateForInference(input);
        
        assertTrue(result.shouldProceed(), "Should proceed with warnings");
        assertTrue(result.hasRecommendations());
        assertTrue(result.recommendations().stream()
            .anyMatch(r -> r.toLowerCase().contains("population")));
    }

    @Test
    void testVerySmallPopulationGeneratesRecommendation() {
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .name("Small Town")
                .country("USA")
                .population(5000L) // Very small
                .build())
            .economyFeatures(createEconomyFeatures())
            .livabilityFeatures(createLivabilityFeatures())
            .sustainabilityFeatures(createSustainabilityFeatures())
            .growthFeatures(createGrowthFeatures())
            .build();
        
        AiQualityGuard.GuardResult result = guard.validateForInference(input);
        
        assertTrue(result.shouldProceed());
        assertTrue(result.hasRecommendations());
        assertTrue(result.recommendations().stream()
            .anyMatch(r -> r.toLowerCase().contains("small population")));
    }

    @Test
    void testLowGDPGeneratesRecommendation() {
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .name("Developing City")
                .country("Country")
                .population(500000L)
                .build())
            .economyFeatures(CityFeatureInput.EconomyFeatures.builder()
                .economyScore(45.0)
                .gdpPerCapita(3000.0) // Very low GDP
                .unemploymentRate(12.0)
                .build())
            .livabilityFeatures(createLivabilityFeatures())
            .sustainabilityFeatures(createSustainabilityFeatures())
            .growthFeatures(createGrowthFeatures())
            .build();
        
        AiQualityGuard.GuardResult result = guard.validateForInference(input);
        
        assertTrue(result.shouldProceed());
        assertTrue(result.hasRecommendations());
        assertTrue(result.recommendations().stream()
            .anyMatch(r -> r.contains("GDP")));
    }

    @Test
    void testGetSummaryFormat() {
        CityFeatureInput input = createHighQualityInput();
        
        AiQualityGuard.GuardResult result = guard.validateForInference(input);
        
        String summary = result.getSummary();
        assertTrue(summary.contains("PASS") || summary.contains("FAIL"));
        assertTrue(summary.contains("%"));
    }

    // Helper methods
    private CityFeatureInput createHighQualityInput() {
        return CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .slug("san-francisco")
                .name("San Francisco")
                .state("California")
                .country("USA")
                .population(875000L)
                .sizeCategory("major")
                .build())
            .economyFeatures(createEconomyFeatures())
            .livabilityFeatures(createLivabilityFeatures())
            .sustainabilityFeatures(createSustainabilityFeatures())
            .growthFeatures(createGrowthFeatures())
            .build();
    }

    private CityFeatureInput createInputWithIdenticalScores(double score) {
        return CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .name("Test City")
                .country("USA")
                .population(500000L)
                .build())
            .economyFeatures(CityFeatureInput.EconomyFeatures.builder()
                .economyScore(score)
                .gdpPerCapita(85000.0)
                .unemploymentRate(4.5)
                .build())
            .livabilityFeatures(CityFeatureInput.LivabilityFeatures.builder()
                .livabilityScore(score)
                .build())
            .sustainabilityFeatures(CityFeatureInput.SustainabilityFeatures.builder()
                .sustainabilityScore(score)
                .build())
            .growthFeatures(CityFeatureInput.GrowthFeatures.builder()
                .growthScore(score)
                .build())
            .build();
    }

    private CityFeatureInput.EconomyFeatures createEconomyFeatures() {
        return CityFeatureInput.EconomyFeatures.builder()
            .gdpPerCapita(85000.0)
            .unemploymentRate(4.5)
            .costOfLivingIndex(140)
            .economyScore(75.0)
            .economyTier("good")
            .build();
    }

    private CityFeatureInput.LivabilityFeatures createLivabilityFeatures() {
        return CityFeatureInput.LivabilityFeatures.builder()
            .aqiIndex(45)
            .costOfLivingIndex(140)
            .population(875000L)
            .livabilityScore(70.0)
            .livabilityTier("good")
            .build();
    }

    private CityFeatureInput.SustainabilityFeatures createSustainabilityFeatures() {
        return CityFeatureInput.SustainabilityFeatures.builder()
            .aqiIndex(45)
            .aqiCategory("Good")
            .sustainabilityScore(72.0)
            .sustainabilityTier("good")
            .build();
    }

    private CityFeatureInput.GrowthFeatures createGrowthFeatures() {
        return CityFeatureInput.GrowthFeatures.builder()
            .populationGrowthRate(1.2)
            .growthScore(65.0)
            .growthTier("average")
            .build();
    }
}
