package com.cityatlas.backend.ai;

import com.cityatlas.backend.entity.City;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * AI FALLBACK SERVICE - Graceful Degradation for AI Failures
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Provides fallback AI responses when the primary inference pipeline fails.
 * Ensures users always receive useful output even when:
 * - Data is incomplete or missing
 * - Confidence is too low for reliable inference
 * - External APIs are unavailable
 * - Internal errors occur during processing
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * FALLBACK STRATEGY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   PRIMARY INFERENCE
 *         │
 *         ▼
 *   ┌─────────────────┐
 *   │ Inference OK?   │──Yes──▶ Return Normal Response
 *   └────────┬────────┘
 *            │ No
 *            ▼
 *   ┌─────────────────┐
 *   │ TIER 1 FALLBACK │  ← Use available partial data
 *   │ (Partial Data)  │
 *   └────────┬────────┘
 *            │ Still failing?
 *            ▼
 *   ┌─────────────────┐
 *   │ TIER 2 FALLBACK │  ← Use city metadata only
 *   │ (Metadata Only) │
 *   └────────┬────────┘
 *            │ Still failing?
 *            ▼
 *   ┌─────────────────┐
 *   │ TIER 3 FALLBACK │  ← Generic safe response
 *   │ (Safe Default)  │
 *   └─────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * DESIGN PRINCIPLES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. NEVER RETURN NULL: Always return a valid response object
 * 2. TRANSPARENCY: Clearly indicate when fallback is used
 * 3. GRACEFUL DEGRADATION: Each tier provides less detail but still useful info
 * 4. NO BROKEN UX: Frontend can render any fallback response
 * 
 * @see AiInferenceService
 * @see AiQualityGuard
 */
@Service
@Slf4j
public class AiFallbackService {

    // ═══════════════════════════════════════════════════════════════════════════
    // FALLBACK REASON CODES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Reason why fallback was triggered.
     * Used for logging, debugging, and user communication.
     */
    public enum FallbackReason {
        INCOMPLETE_DATA("Data is incomplete for reliable analysis"),
        LOW_CONFIDENCE("Confidence too low for reliable predictions"),
        API_UNAVAILABLE("External data sources temporarily unavailable"),
        INFERENCE_ERROR("Error during AI processing"),
        QUALITY_GUARD_BLOCKED("Data quality below minimum threshold"),
        TIMEOUT("Processing took too long"),
        UNKNOWN("Unknown error occurred");
        
        private final String userMessage;
        
        FallbackReason(String userMessage) {
            this.userMessage = userMessage;
        }
        
        public String getUserMessage() {
            return userMessage;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FALLBACK TIERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Fallback tier indicating how much data was available.
     * Higher tier = less data available = more generic response.
     */
    public enum FallbackTier {
        TIER_1_PARTIAL_DATA,    // Some scores available, use what we have
        TIER_2_METADATA_ONLY,   // Only city name/country available
        TIER_3_SAFE_DEFAULT     // No data at all, generic response
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN FALLBACK ENTRY POINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generate fallback response when data is incomplete.
     * 
     * LOGIC:
     * 1. Check what data IS available (partial data)
     * 2. Generate insights based on available data only
     * 3. Clearly indicate which insights are limited
     * 
     * @param city The city entity (may have partial data)
     * @param input The feature input (may have null fields)
     * @param dataQuality The quality check result
     * @return Fallback response with available insights
     */
    public FallbackResponse handleIncompleteData(
        City city,
        CityFeatureInput input,
        DataQualityChecker.DataQualityResult dataQuality
    ) {
        log.warn("[FALLBACK] Incomplete data for city: {} ({}% complete)", 
            city.getName(), String.format("%.1f", dataQuality.completeness()));
        
        FallbackTier tier = determineFallbackTier(input, dataQuality);
        
        return switch (tier) {
            case TIER_1_PARTIAL_DATA -> generatePartialDataFallback(city, input, dataQuality);
            case TIER_2_METADATA_ONLY -> generateMetadataOnlyFallback(city);
            case TIER_3_SAFE_DEFAULT -> generateSafeDefaultFallback(city);
        };
    }
    
    /**
     * Generate fallback response when confidence is too low.
     * 
     * LOGIC:
     * 1. Still show the insights, but with strong caveats
     * 2. Highlight which areas have low confidence
     * 3. Suggest why confidence may be low
     * 
     * @param city The city entity
     * @param insights The generated insights (low confidence)
     * @param confidence The confidence calculation result
     * @return Fallback response with confidence caveats
     */
    public FallbackResponse handleLowConfidence(
        City city,
        AiInferencePipeline.InferenceInsights insights,
        ConfidenceCalculator.ConfidenceResult confidence
    ) {
        log.warn("[FALLBACK] Low confidence for city: {} ({}% confidence)", 
            city.getName(), String.format("%.1f", confidence.overallConfidence()));
        
        // For low confidence, we still show insights but with clear warnings
        List<String> caveats = generateLowConfidenceCaveats(confidence);
        
        // Modify personality to include disclaimer
        String modifiedPersonality = String.format(
            "%s (Note: This assessment is based on limited data and should be considered preliminary.)",
            insights.personality()
        );
        
        return FallbackResponse.builder()
            .tier(FallbackTier.TIER_1_PARTIAL_DATA)
            .reason(FallbackReason.LOW_CONFIDENCE)
            .personality(modifiedPersonality)
            .strengths(insights.strengths())
            .weaknesses(insights.weaknesses())
            .audienceSegments(insights.audienceSegments())
            .confidence(confidence.overallConfidence())
            .caveats(caveats)
            .dataAvailability(generateDataAvailabilityMap(confidence))
            .userMessage("Our analysis has lower confidence due to limited data. Results should be verified.")
            .build();
    }
    
    /**
     * Generate fallback response when external APIs are unavailable.
     * 
     * LOGIC:
     * 1. Use cached data if available
     * 2. Use database-only data (no real-time updates)
     * 3. Clearly indicate data may be stale
     * 
     * @param city The city entity
     * @param unavailableApis List of APIs that failed
     * @return Fallback response using cached/stale data
     */
    public FallbackResponse handleApiUnavailable(
        City city,
        List<String> unavailableApis
    ) {
        log.warn("[FALLBACK] APIs unavailable for city: {} - {}", 
            city.getName(), unavailableApis);
        
        // Generate response based on what we have in database
        return FallbackResponse.builder()
            .tier(FallbackTier.TIER_2_METADATA_ONLY)
            .reason(FallbackReason.API_UNAVAILABLE)
            .personality(generateDatabaseOnlyPersonality(city))
            .strengths(generateDatabaseOnlyStrengths(city))
            .weaknesses(List.of("Real-time data temporarily unavailable"))
            .audienceSegments(generateGenericAudienceSegments())
            .confidence(30.0) // Low confidence since no real-time data
            .caveats(List.of(
                "Real-time data from external sources is temporarily unavailable",
                "Information shown is based on stored data and may not reflect current conditions",
                "Weather, air quality, and other live metrics are not included"
            ))
            .dataAvailability(Map.of(
                "database", "available",
                "weather_api", unavailableApis.contains("weather") ? "unavailable" : "available",
                "aqi_api", unavailableApis.contains("aqi") ? "unavailable" : "available"
            ))
            .userMessage("Some live data sources are temporarily unavailable. Showing cached information.")
            .build();
    }
    
    /**
     * Generate fallback response when an error occurs during inference.
     * 
     * LOGIC:
     * 1. Log the error for debugging
     * 2. Return safe, generic response
     * 3. Never expose error details to user
     * 
     * @param city The city entity
     * @param error The exception that occurred
     * @return Safe fallback response
     */
    public FallbackResponse handleInferenceError(City city, Exception error) {
        // Log full error for debugging (not shown to user)
        log.error("[FALLBACK] Inference error for city: {}", city.getName(), error);
        
        // Return safe response - never expose internal errors
        return FallbackResponse.builder()
            .tier(FallbackTier.TIER_3_SAFE_DEFAULT)
            .reason(FallbackReason.INFERENCE_ERROR)
            .personality(generateSafePersonality(city))
            .strengths(List.of("Explore this city's unique characteristics"))
            .weaknesses(List.of())
            .audienceSegments(List.of("Curious travelers", "Urban explorers"))
            .confidence(0.0)
            .caveats(List.of("Detailed analysis is temporarily unavailable"))
            .dataAvailability(Map.of("status", "limited"))
            .userMessage("We're having trouble analyzing this city right now. Please try again later.")
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TIER DETERMINATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Determine which fallback tier to use based on available data.
     * 
     * TIER 1: >= 50% data available - use partial data
     * TIER 2: < 50% but have city metadata - use metadata
     * TIER 3: Nothing useful available - safe default
     */
    private FallbackTier determineFallbackTier(
        CityFeatureInput input,
        DataQualityChecker.DataQualityResult dataQuality
    ) {
        // Tier 1: At least 50% data complete
        if (dataQuality.completeness() >= 50.0) {
            return FallbackTier.TIER_1_PARTIAL_DATA;
        }
        
        // Tier 2: Have city identification at minimum
        if (input.getCityIdentifier() != null && 
            input.getCityIdentifier().getName() != null) {
            return FallbackTier.TIER_2_METADATA_ONLY;
        }
        
        // Tier 3: Nothing useful
        return FallbackTier.TIER_3_SAFE_DEFAULT;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TIER 1: PARTIAL DATA FALLBACK
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generate response using whatever partial data is available.
     * Only make claims about data we actually have.
     */
    private FallbackResponse generatePartialDataFallback(
        City city,
        CityFeatureInput input,
        DataQualityChecker.DataQualityResult dataQuality
    ) {
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> audiences = new ArrayList<>();
        List<String> caveats = new ArrayList<>();
        
        // Only add insights for data we actually have
        if (input.getEconomyFeatures() != null && 
            input.getEconomyFeatures().getEconomyScore() != null) {
            Double score = input.getEconomyFeatures().getEconomyScore();
            if (score >= 60) {
                strengths.add("Economic indicators show positive trends");
                audiences.add("Career-focused professionals");
            } else if (score < 40) {
                weaknesses.add("Economic data suggests challenges");
            }
        } else {
            caveats.add("Economic data not available for analysis");
        }
        
        if (input.getLivabilityFeatures() != null && 
            input.getLivabilityFeatures().getLivabilityScore() != null) {
            Double score = input.getLivabilityFeatures().getLivabilityScore();
            if (score >= 60) {
                strengths.add("Quality of life metrics are favorable");
                audiences.add("Families and retirees");
            } else if (score < 40) {
                weaknesses.add("Some livability concerns noted");
            }
        } else {
            caveats.add("Livability data not available for analysis");
        }
        
        if (input.getSustainabilityFeatures() != null && 
            input.getSustainabilityFeatures().getSustainabilityScore() != null) {
            Double score = input.getSustainabilityFeatures().getSustainabilityScore();
            if (score >= 60) {
                strengths.add("Environmental conditions are positive");
                audiences.add("Environmentally conscious residents");
            } else if (score < 40) {
                weaknesses.add("Environmental metrics could be improved");
            }
        } else {
            caveats.add("Environmental data not available for analysis");
        }
        
        // Ensure minimum content
        if (strengths.isEmpty()) {
            strengths.add("Explore " + city.getName() + "'s unique characteristics");
        }
        if (audiences.isEmpty()) {
            audiences.add("Urban explorers");
            audiences.add("Curious travelers");
        }
        
        String personality = String.format(
            "%s offers a distinctive urban experience. Based on available data, " +
            "we can provide insights into %d key areas.",
            city.getName(),
            3 - caveats.size()
        );
        
        return FallbackResponse.builder()
            .tier(FallbackTier.TIER_1_PARTIAL_DATA)
            .reason(FallbackReason.INCOMPLETE_DATA)
            .personality(personality)
            .strengths(strengths)
            .weaknesses(weaknesses)
            .audienceSegments(audiences)
            .confidence(dataQuality.completeness())
            .caveats(caveats)
            .dataAvailability(Map.of(
                "economy", input.getEconomyFeatures() != null ? "available" : "missing",
                "livability", input.getLivabilityFeatures() != null ? "available" : "missing",
                "sustainability", input.getSustainabilityFeatures() != null ? "available" : "missing"
            ))
            .userMessage("Analysis based on partial data. Some insights may be limited.")
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TIER 2: METADATA ONLY FALLBACK
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generate response using only city metadata (name, country, population).
     * Very generic but still useful.
     */
    private FallbackResponse generateMetadataOnlyFallback(City city) {
        String personality = String.format(
            "%s is a city in %s with a population of approximately %s. " +
            "Detailed metrics are currently unavailable, but we encourage you to explore what this city has to offer.",
            city.getName(),
            city.getCountry() != null ? city.getCountry() : "its region",
            formatPopulation(city.getPopulation())
        );
        
        return FallbackResponse.builder()
            .tier(FallbackTier.TIER_2_METADATA_ONLY)
            .reason(FallbackReason.INCOMPLETE_DATA)
            .personality(personality)
            .strengths(List.of(
                "Discover " + city.getName() + "'s local culture and attractions",
                "Experience the unique character of this destination"
            ))
            .weaknesses(List.of()) // Don't show weaknesses without data
            .audienceSegments(List.of(
                "Adventurous travelers",
                "Those seeking new experiences"
            ))
            .confidence(15.0)
            .caveats(List.of(
                "Detailed city metrics are not currently available",
                "This is a general overview based on basic information"
            ))
            .dataAvailability(Map.of(
                "name", "available",
                "country", city.getCountry() != null ? "available" : "missing",
                "population", city.getPopulation() != null ? "available" : "missing",
                "metrics", "unavailable"
            ))
            .userMessage("Limited information available for this city. Showing general overview.")
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TIER 3: SAFE DEFAULT FALLBACK
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generate the safest possible response when nothing is available.
     * Never fails, always returns valid content.
     */
    private FallbackResponse generateSafeDefaultFallback(City city) {
        String cityName = city != null && city.getName() != null ? 
            city.getName() : "This city";
        
        return FallbackResponse.builder()
            .tier(FallbackTier.TIER_3_SAFE_DEFAULT)
            .reason(FallbackReason.INCOMPLETE_DATA)
            .personality(cityName + " awaits your discovery. Explore its unique character and hidden gems.")
            .strengths(List.of("Every city has its own story to tell"))
            .weaknesses(List.of()) // Never show weaknesses in safe default
            .audienceSegments(List.of("Explorers and adventurers"))
            .confidence(0.0)
            .caveats(List.of("City information is currently unavailable"))
            .dataAvailability(Map.of("status", "unavailable"))
            .userMessage("We don't have detailed information for this city yet. Check back later!")
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generate caveats explaining why confidence is low.
     */
    private List<String> generateLowConfidenceCaveats(ConfidenceCalculator.ConfidenceResult confidence) {
        List<String> caveats = new ArrayList<>();
        
        ConfidenceCalculator.ConfidenceBreakdown breakdown = confidence.breakdown();
        
        if (breakdown.dataCompleteness() < 70) {
            caveats.add("Some data fields are missing or incomplete");
        }
        if (breakdown.patternReliability() < 70) {
            caveats.add("Score patterns show unusual variance");
        }
        if (breakdown.inferenceStrength() < 70) {
            caveats.add("Fewer insights could be generated than typical");
        }
        
        if (caveats.isEmpty()) {
            caveats.add("Analysis based on limited information");
        }
        
        return caveats;
    }
    
    /**
     * Generate data availability map from confidence breakdown.
     */
    private Map<String, String> generateDataAvailabilityMap(ConfidenceCalculator.ConfidenceResult confidence) {
        ConfidenceCalculator.ConfidenceBreakdown breakdown = confidence.breakdown();
        return Map.of(
            "data_completeness", breakdown.dataCompleteness() >= 70 ? "good" : "limited",
            "pattern_reliability", breakdown.patternReliability() >= 70 ? "good" : "limited",
            "inference_quality", breakdown.inferenceStrength() >= 70 ? "good" : "limited"
        );
    }
    
    /**
     * Generate personality from database-only data (no real-time APIs).
     */
    private String generateDatabaseOnlyPersonality(City city) {
        return String.format(
            "%s, %s is a city with stored historical data. " +
            "Real-time information is temporarily unavailable, but we can share what we know.",
            city.getName(),
            city.getCountry() != null ? city.getCountry() : "located in its region"
        );
    }
    
    /**
     * Generate strengths from database-only data.
     */
    private List<String> generateDatabaseOnlyStrengths(City city) {
        List<String> strengths = new ArrayList<>();
        
        if (city.getPopulation() != null && city.getPopulation() > 1000000) {
            strengths.add("Major metropolitan area with diverse offerings");
        } else if (city.getPopulation() != null && city.getPopulation() > 100000) {
            strengths.add("Mid-sized city with local character");
        } else {
            strengths.add("Intimate community atmosphere");
        }
        
        strengths.add("Check back soon for updated real-time data");
        return strengths;
    }
    
    /**
     * Generate generic audience segments.
     */
    private List<String> generateGenericAudienceSegments() {
        return List.of(
            "General travelers",
            "Those researching relocation options",
            "Curious explorers"
        );
    }
    
    /**
     * Generate safe personality that never fails.
     */
    private String generateSafePersonality(City city) {
        String name = city != null && city.getName() != null ? city.getName() : "This destination";
        return name + " is waiting to be explored. We're working on gathering more information.";
    }
    
    /**
     * Format population for display.
     */
    private String formatPopulation(Long population) {
        if (population == null) return "unknown size";
        if (population >= 1000000) {
            return String.format("%.1f million", population / 1000000.0);
        } else if (population >= 1000) {
            return String.format("%,d", population);
        }
        return population.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FALLBACK RESPONSE DTO
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Complete fallback response with all necessary information.
     * Designed to be directly usable by frontend without additional processing.
     */
    @lombok.Builder
    public record FallbackResponse(
        // Fallback metadata
        FallbackTier tier,
        FallbackReason reason,
        
        // Generated content (same structure as normal response)
        String personality,
        List<String> strengths,
        List<String> weaknesses,
        List<String> audienceSegments,
        
        // Quality indicators
        Double confidence,
        List<String> caveats,
        Map<String, String> dataAvailability,
        
        // User communication
        String userMessage
    ) {
        /**
         * Check if this is a degraded response (any fallback tier).
         */
        public boolean isDegraded() {
            return tier != null;
        }
        
        /**
         * Get severity of degradation (1 = minor, 3 = major).
         */
        /**
         * Get severity of degradation (1 = minor, 3 = major, 0 = no degradation).
         */
        public int degradationSeverity() {
            if (tier == null) {
                return 0;
            }
            return switch (tier) {
                case TIER_1_PARTIAL_DATA -> 1;
                case TIER_2_METADATA_ONLY -> 2;
                case TIER_3_SAFE_DEFAULT -> 3;
            };
        }
        
        /**
         * Convert to InferenceResult for API compatibility.
         */
        public AiInferencePipeline.InferenceResult toInferenceResult(String citySlug) {
            return AiInferencePipeline.InferenceResult.builder()
                .citySlug(citySlug)
                .personality(personality)
                .strengths(strengths)
                .weaknesses(weaknesses)
                .bestSuitedFor(audienceSegments)
                .confidence(confidence / 100.0) // Normalize to 0-1
                .inferenceTimeMs(0L)
                .pipelineVersion("fallback-1.0")
                .valid(true) // Fallback responses are always "valid" for display
                .validationErrors(userMessage) // Use as info message
                .build();
        }
    }
}
