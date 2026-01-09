package com.cityatlas.backend.ai;

import com.cityatlas.backend.entity.City;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * AI FALLBACK SERVICE TESTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Tests the graceful degradation behavior when:
 * - Data is incomplete
 * - Confidence is low
 * - APIs are unavailable
 * - Errors occur during inference
 * 
 * KEY INVARIANT: User always receives useful output, never broken UX.
 */
@DisplayName("AiFallbackService")
class AiFallbackServiceTest {
    
    private AiFallbackService fallbackService;
    
    @BeforeEach
    void setUp() {
        fallbackService = new AiFallbackService();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INCOMPLETE DATA FALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("Incomplete Data Handling")
    class IncompleteDataTests {
        
        @Test
        @DisplayName("Should return Tier 1 fallback when data >= 50% complete")
        void tier1FallbackForPartialData() {
            // Given: City with partial data (>50% complete)
            City city = createTestCity("Paris", "France", 2_100_000L);
            CityFeatureInput input = createPartialInput(city, 65.0);
            DataQualityChecker.DataQualityResult quality = createQualityResult(65.0);
            
            // When: Handling incomplete data
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleIncompleteData(city, input, quality);
            
            // Then: Should return Tier 1 response with partial insights
            assertEquals(AiFallbackService.FallbackTier.TIER_1_PARTIAL_DATA, response.tier());
            assertEquals(AiFallbackService.FallbackReason.INCOMPLETE_DATA, response.reason());
            assertNotNull(response.personality());
            assertFalse(response.strengths().isEmpty());
            assertTrue(response.confidence() >= 50.0);
            assertNotNull(response.userMessage());
        }
        
        @Test
        @DisplayName("Should return Tier 2 fallback when only metadata available")
        void tier2FallbackForMetadataOnly() {
            // Given: City with only basic metadata (<50% complete)
            City city = createTestCity("Tokyo", "Japan", 13_960_000L);
            CityFeatureInput input = createMinimalInput(city);
            DataQualityChecker.DataQualityResult quality = createQualityResult(25.0);
            
            // When: Handling incomplete data
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleIncompleteData(city, input, quality);
            
            // Then: Should return Tier 2 response with general info
            assertEquals(AiFallbackService.FallbackTier.TIER_2_METADATA_ONLY, response.tier());
            assertTrue(response.personality().contains("Tokyo"));
            assertTrue(response.personality().contains("Japan"));
            assertFalse(response.strengths().isEmpty());
            assertTrue(response.weaknesses().isEmpty()); // Don't show weaknesses without data
        }
        
        @Test
        @DisplayName("Should return Tier 3 fallback when nothing useful available")
        void tier3FallbackForNoData() {
            // Given: City with almost no data
            City city = new City();
            city.setName("Unknown");
            CityFeatureInput input = createEmptyInput();
            DataQualityChecker.DataQualityResult quality = createQualityResult(5.0);
            
            // When: Handling incomplete data
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleIncompleteData(city, input, quality);
            
            // Then: Should return safe default
            assertEquals(AiFallbackService.FallbackTier.TIER_3_SAFE_DEFAULT, response.tier());
            assertNotNull(response.personality());
            assertFalse(response.strengths().isEmpty()); // Always have at least one strength
            assertEquals(0.0, response.confidence());
        }
        
        @Test
        @DisplayName("Should never return null for any fallback field")
        void neverReturnsNull() {
            // Given: Worst case - null city
            City city = new City();
            CityFeatureInput input = createEmptyInput();
            DataQualityChecker.DataQualityResult quality = createQualityResult(0.0);
            
            // When: Handling incomplete data
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleIncompleteData(city, input, quality);
            
            // Then: No fields should be null
            assertNotNull(response.tier());
            assertNotNull(response.reason());
            assertNotNull(response.personality());
            assertNotNull(response.strengths());
            assertNotNull(response.weaknesses());
            assertNotNull(response.audienceSegments());
            assertNotNull(response.caveats());
            assertNotNull(response.dataAvailability());
            assertNotNull(response.userMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOW CONFIDENCE FALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("Low Confidence Handling")
    class LowConfidenceTests {
        
        @Test
        @DisplayName("Should add caveats when confidence is low")
        void addsCaveatsForLowConfidence() {
            // Given: Insights with low confidence
            City city = createTestCity("Berlin", "Germany", 3_600_000L);
            AiInferencePipeline.InferenceInsights insights = createInsights(
                "Berlin is a dynamic city", 
                List.of("Rich history"), 
                List.of("High cost")
            );
            ConfidenceCalculator.ConfidenceResult confidence = createConfidenceResult(35.0);
            
            // When: Handling low confidence
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleLowConfidence(city, insights, confidence);
            
            // Then: Should include caveats and modified personality
            assertFalse(response.caveats().isEmpty());
            assertTrue(response.personality().contains("preliminary"));
            assertEquals(AiFallbackService.FallbackReason.LOW_CONFIDENCE, response.reason());
        }
        
        @Test
        @DisplayName("Should preserve original insights with warnings")
        void preservesInsightsWithWarnings() {
            // Given: Valid insights but low confidence
            City city = createTestCity("Sydney", "Australia", 5_300_000L);
            List<String> originalStrengths = List.of("Great beaches", "Strong economy");
            AiInferencePipeline.InferenceInsights insights = createInsights(
                "Sydney blends nature and urban life",
                originalStrengths,
                List.of("Expensive housing")
            );
            ConfidenceCalculator.ConfidenceResult confidence = createConfidenceResult(45.0);
            
            // When: Handling low confidence
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleLowConfidence(city, insights, confidence);
            
            // Then: Original insights should be preserved
            assertEquals(originalStrengths, response.strengths());
            assertNotNull(response.userMessage());
            assertTrue(response.userMessage().contains("lower confidence") || 
                       response.userMessage().contains("limited data"));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // API UNAVAILABLE FALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("API Unavailable Handling")
    class ApiUnavailableTests {
        
        @Test
        @DisplayName("Should provide cached data when APIs fail")
        void providesCachedDataWhenApisFail() {
            // Given: City and list of failed APIs
            City city = createTestCity("London", "United Kingdom", 8_900_000L);
            List<String> unavailableApis = List.of("weather", "aqi");
            
            // When: Handling API unavailability
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleApiUnavailable(city, unavailableApis);
            
            // Then: Should return database-only response
            assertEquals(AiFallbackService.FallbackReason.API_UNAVAILABLE, response.reason());
            assertTrue(response.personality().contains("London"));
            assertTrue(response.caveats().stream()
                .anyMatch(c -> c.toLowerCase().contains("unavailable")));
            assertEquals("unavailable", response.dataAvailability().get("weather_api"));
            assertEquals("unavailable", response.dataAvailability().get("aqi_api"));
        }
        
        @Test
        @DisplayName("Should indicate data may be stale")
        void indicatesDataMayBeStale() {
            // Given: City with API failure
            City city = createTestCity("Mumbai", "India", 20_700_000L);
            
            // When: Handling API unavailability
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleApiUnavailable(city, List.of("weather"));
            
            // Then: Should warn about potential staleness
            assertTrue(response.caveats().stream()
                .anyMatch(c -> c.contains("current conditions") || 
                               c.contains("stored data")));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ERROR FALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("Inference Error Handling")
    class InferenceErrorTests {
        
        @Test
        @DisplayName("Should return safe response on error")
        void returnsSafeResponseOnError() {
            // Given: City and an exception
            City city = createTestCity("Toronto", "Canada", 6_200_000L);
            Exception error = new RuntimeException("Database connection failed");
            
            // When: Handling error
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleInferenceError(city, error);
            
            // Then: Should return Tier 3 safe response
            assertEquals(AiFallbackService.FallbackTier.TIER_3_SAFE_DEFAULT, response.tier());
            assertEquals(AiFallbackService.FallbackReason.INFERENCE_ERROR, response.reason());
            assertEquals(0.0, response.confidence());
        }
        
        @Test
        @DisplayName("Should not expose internal error details")
        void doesNotExposeErrorDetails() {
            // Given: Error with sensitive information
            City city = createTestCity("Singapore", "Singapore", 5_700_000L);
            Exception error = new RuntimeException("SQL Error: password=secret123");
            
            // When: Handling error
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleInferenceError(city, error);
            
            // Then: Should not contain error details
            assertFalse(response.personality().contains("SQL"));
            assertFalse(response.personality().contains("password"));
            assertFalse(response.userMessage().contains("secret"));
            assertTrue(response.weaknesses().isEmpty()); // Don't show weaknesses on error
        }
        
        @Test
        @DisplayName("Should provide helpful user message")
        void providesHelpfulUserMessage() {
            // Given: Any error
            City city = createTestCity("Dubai", "UAE", 3_400_000L);
            Exception error = new Exception("Generic error");
            
            // When: Handling error
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleInferenceError(city, error);
            
            // Then: User message should be helpful
            assertTrue(response.userMessage().toLowerCase().contains("try again") ||
                       response.userMessage().toLowerCase().contains("later"));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INFERENCE RESULT CONVERSION
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("InferenceResult Conversion")
    class InferenceResultConversionTests {
        
        @Test
        @DisplayName("Should convert to valid InferenceResult")
        void convertsToValidInferenceResult() {
            // Given: A fallback response
            City city = createTestCity("Amsterdam", "Netherlands", 870_000L);
            CityFeatureInput input = createPartialInput(city, 60.0);
            DataQualityChecker.DataQualityResult quality = createQualityResult(60.0);
            
            AiFallbackService.FallbackResponse response = 
                fallbackService.handleIncompleteData(city, input, quality);
            
            // When: Converting to InferenceResult
            AiInferencePipeline.InferenceResult result = response.toInferenceResult("amsterdam");
            
            // Then: Should be valid for API response
            assertEquals("amsterdam", result.citySlug());
            assertEquals(response.personality(), result.personality());
            assertEquals(response.strengths(), result.strengths());
            assertEquals("fallback-1.0", result.pipelineVersion());
            assertTrue(result.valid()); // Fallbacks are always "valid" for display
        }
        
        @Test
        @DisplayName("Should track degradation severity")
        void tracksDegradationSeverity() {
            // Given: Responses of different tiers
            AiFallbackService.FallbackResponse tier1 = AiFallbackService.FallbackResponse.builder()
                .tier(AiFallbackService.FallbackTier.TIER_1_PARTIAL_DATA)
                .reason(AiFallbackService.FallbackReason.INCOMPLETE_DATA)
                .personality("Test")
                .strengths(List.of())
                .weaknesses(List.of())
                .audienceSegments(List.of())
                .confidence(60.0)
                .caveats(List.of())
                .dataAvailability(Map.of())
                .userMessage("Test")
                .build();
            
            AiFallbackService.FallbackResponse tier3 = AiFallbackService.FallbackResponse.builder()
                .tier(AiFallbackService.FallbackTier.TIER_3_SAFE_DEFAULT)
                .reason(AiFallbackService.FallbackReason.INFERENCE_ERROR)
                .personality("Test")
                .strengths(List.of())
                .weaknesses(List.of())
                .audienceSegments(List.of())
                .confidence(0.0)
                .caveats(List.of())
                .dataAvailability(Map.of())
                .userMessage("Test")
                .build();
            
            // Then: Severity should increase with tier
            assertEquals(1, tier1.degradationSeverity());
            assertEquals(3, tier3.degradationSeverity());
            assertTrue(tier1.isDegraded());
            assertTrue(tier3.isDegraded());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private City createTestCity(String name, String country, Long population) {
        City city = new City();
        city.setName(name);
        city.setSlug(name.toLowerCase().replace(" ", "-"));
        city.setCountry(country);
        city.setPopulation(population);
        return city;
    }
    
    private CityFeatureInput createPartialInput(City city, double completeness) {
        // Create input with some data based on completeness
        CityFeatureInput.CityIdentifier identifier = CityFeatureInput.CityIdentifier.builder()
            .name(city.getName())
            .country(city.getCountry())
            .population(city.getPopulation())
            .build();
        
        // If completeness > 50%, include economy features
        CityFeatureInput.EconomyFeatures economy = null;
        if (completeness >= 50) {
            economy = CityFeatureInput.EconomyFeatures.builder()
                .economyScore(65.0)
                .gdpPerCapita(45000.0)
                .unemploymentRate(4.5)
                .build();
        }
        
        CityFeatureInput.LivabilityFeatures livability = null;
        if (completeness >= 70) {
            livability = CityFeatureInput.LivabilityFeatures.builder()
                .livabilityScore(70.0)
                .build();
        }
        
        return CityFeatureInput.builder()
            .cityIdentifier(identifier)
            .economyFeatures(economy)
            .livabilityFeatures(livability)
            .build();
    }
    
    private CityFeatureInput createMinimalInput(City city) {
        return CityFeatureInput.builder()
            .cityIdentifier(CityFeatureInput.CityIdentifier.builder()
                .name(city.getName())
                .country(city.getCountry())
                .population(city.getPopulation())
                .build())
            .build();
    }
    
    private CityFeatureInput createEmptyInput() {
        return CityFeatureInput.builder().build();
    }
    
    private DataQualityChecker.DataQualityResult createQualityResult(double completeness) {
        // DataQualityResult(isSufficient, completeness, issues, warnings)
        boolean isSufficient = completeness >= 60;
        return new DataQualityChecker.DataQualityResult(
            isSufficient,
            completeness,
            isSufficient ? List.of() : List.of("Insufficient data"),
            List.of()
        );
    }
    
    private ConfidenceCalculator.ConfidenceResult createConfidenceResult(double confidence) {
        // ConfidenceResult(overallConfidence, level, reasoning, breakdown)
        ConfidenceCalculator.ConfidenceLevel level = 
            confidence >= 80 ? ConfidenceCalculator.ConfidenceLevel.HIGH :
            confidence >= 60 ? ConfidenceCalculator.ConfidenceLevel.MEDIUM :
            ConfidenceCalculator.ConfidenceLevel.LOW;
        
        return new ConfidenceCalculator.ConfidenceResult(
            confidence,
            level,
            "Test reasoning",
            new ConfidenceCalculator.ConfidenceBreakdown(
                confidence * 0.4,  // data completeness
                confidence * 0.3,  // pattern reliability
                confidence * 0.3   // inference strength
            )
        );
    }
    
    private AiInferencePipeline.InferenceInsights createInsights(
        String personality, 
        List<String> strengths, 
        List<String> weaknesses
    ) {
        return AiInferencePipeline.InferenceInsights.builder()
            .personality(personality)
            .strengths(strengths)
            .weaknesses(weaknesses)
            .audienceSegments(List.of("Test audience"))
            .build();
    }
}
