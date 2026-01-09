package com.cityatlas.backend.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataQualityChecker - validates data quality detection logic.
 */
class DataQualityCheckerTest {

    private final DataQualityChecker checker = new DataQualityChecker();

    @Test
    void testCompleteHighQualityData() {
        CityFeatureInput input = createCompleteInput();
        
        DataQualityChecker.DataQualityResult result = checker.validateData(input);
        
        assertTrue(result.isSufficient(), "Complete data should be sufficient");
        assertTrue(result.completeness() > 90.0, "Completeness should be > 90%");
        assertTrue(result.issues().isEmpty(), "Should have no critical issues");
    }

    @Test
    void testMissingCityName() {
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .name(null) // Missing name
                .country("USA")
                .build())
            .economyFeatures(createEconomyFeatures())
            .livabilityFeatures(createLivabilityFeatures())
            .sustainabilityFeatures(createSustainabilityFeatures())
            .growthFeatures(createGrowthFeatures())
            .build();
        
        DataQualityChecker.DataQualityResult result = checker.validateData(input);
        
        assertFalse(result.isSufficient(), "Missing city name should fail");
        assertTrue(result.issues().stream()
            .anyMatch(issue -> issue.toLowerCase().contains("city name")));
    }

    @Test
    void testInvalidScoreOutOfRange() {
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(createCityIdentifier())
            .economyFeatures(CityFeatureInput.EconomyFeatures.builder()
                .economyScore(150.0) // Invalid: > 100
                .gdpPerCapita(85000.0)
                .unemploymentRate(4.5)
                .build())
            .livabilityFeatures(createLivabilityFeatures())
            .sustainabilityFeatures(createSustainabilityFeatures())
            .growthFeatures(createGrowthFeatures())
            .build();
        
        DataQualityChecker.DataQualityResult result = checker.validateData(input);
        
        assertFalse(result.isSufficient(), "Out of range score should fail");
        assertTrue(result.issues().stream()
            .anyMatch(issue -> issue.toLowerCase().contains("out of range")));
    }

    @Test
    void testAllZeroScores() {
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(createCityIdentifier())
            .economyFeatures(CityFeatureInput.EconomyFeatures.builder()
                .economyScore(0.0) // All zeros
                .gdpPerCapita(85000.0)
                .unemploymentRate(4.5)
                .build())
            .livabilityFeatures(CityFeatureInput.LivabilityFeatures.builder()
                .livabilityScore(0.0)
                .build())
            .sustainabilityFeatures(CityFeatureInput.SustainabilityFeatures.builder()
                .sustainabilityScore(0.0)
                .build())
            .growthFeatures(CityFeatureInput.GrowthFeatures.builder()
                .growthScore(0.0)
                .build())
            .build();
        
        DataQualityChecker.DataQualityResult result = checker.validateData(input);
        
        assertFalse(result.warnings().isEmpty(), "Zero scores should generate warnings");
        assertTrue(result.warnings().stream()
            .anyMatch(w -> w.toLowerCase().contains("zero")));
    }

    @Test
    void testInvalidPopulation() {
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .name("San Francisco")
                .country("USA")
                .population(-5000L) // Invalid: negative
                .build())
            .economyFeatures(createEconomyFeatures())
            .livabilityFeatures(createLivabilityFeatures())
            .sustainabilityFeatures(createSustainabilityFeatures())
            .growthFeatures(createGrowthFeatures())
            .build();
        
        DataQualityChecker.DataQualityResult result = checker.validateData(input);
        
        assertFalse(result.isSufficient(), "Negative population should fail");
        assertTrue(result.issues().stream()
            .anyMatch(issue -> issue.toLowerCase().contains("invalid population")));
    }

    @Test
    void testMissingEconomicData() {
        CityFeatureInput input = CityFeatureInput.builder()
            .cityIdentifier(createCityIdentifier())
            .economyFeatures(CityFeatureInput.EconomyFeatures.builder()
                .economyScore(75.0)
                .gdpPerCapita(null) // Missing GDP
                .unemploymentRate(null) // Missing unemployment
                .build())
            .livabilityFeatures(createLivabilityFeatures())
            .sustainabilityFeatures(createSustainabilityFeatures())
            .growthFeatures(createGrowthFeatures())
            .build();
        
        DataQualityChecker.DataQualityResult result = checker.validateData(input);
        
        // Should still be sufficient, but with warnings
        assertTrue(result.isSufficient(), "Missing economic data should not block inference");
        assertFalse(result.warnings().isEmpty(), "Should have warnings for missing data");
    }

    @Test
    void testCompletenessCalculation() {
        // Create input with minimal data
        CityFeatureInput partialInput = CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .name("San Francisco")
                .country("USA")
                .build())
            // Missing all features
            .build();
        
        DataQualityChecker.DataQualityResult result = checker.validateData(partialInput);
        
        assertTrue(result.completeness() < 70.0, "Partial data should have < 70% completeness");
        assertTrue(result.completeness() > 10.0, "Should still have some data present");
    }

    // Helper methods
    private CityFeatureInput createCompleteInput() {
        return CityFeatureInput.builder()
            .cityIdentifier(createCityIdentifier())
            .economyFeatures(createEconomyFeatures())
            .livabilityFeatures(createLivabilityFeatures())
            .sustainabilityFeatures(createSustainabilityFeatures())
            .growthFeatures(createGrowthFeatures())
            .build();
    }

    private CityFeatureInput.CityIdentifier createCityIdentifier() {
        return CityFeatureInput.CityIdentifier.builder()
            .slug("san-francisco")
            .name("San Francisco")
            .state("California")
            .country("USA")
            .population(875000L)
            .sizeCategory("major")
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
