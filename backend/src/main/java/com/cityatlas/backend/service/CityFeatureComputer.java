package com.cityatlas.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cityatlas.backend.entity.City;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * ==============================================================================
 * CITY FEATURE COMPUTER - Structured Score Computation
 * ==============================================================================
 * 
 * PURPOSE:
 * Computes deterministic, explainable scores for cities based on raw metrics.
 * These scores feed into the AI Summary engine and enable cross-city comparison.
 * 
 * ==============================================================================
 * SCORE CATEGORIES
 * ==============================================================================
 * 
 *   +------------------+----------------------------------------+---------------+
 *   | SCORE            | INPUTS                                 | WEIGHT        |
 *   +------------------+----------------------------------------+---------------+
 *   | Economy Score    | GDP per capita, Unemployment rate      | 40% / 60%     |
 *   | Livability Score | Cost of living, AQI, Population size   | 35% / 35% /30%|
 *   | Sustainability   | AQI, (future: carbon, green space)     | 100%          |
 *   +------------------+----------------------------------------+---------------+
 * 
 * ==============================================================================
 * DESIGN PRINCIPLES
 * ==============================================================================
 * 
 *   1. DETERMINISTIC: Same inputs always produce same outputs
 *   2. EXPLAINABLE: Each score component can be traced to source metrics
 *   3. BOUNDED: All scores are normalized to 0-100 scale
 *   4. GRACEFUL: Missing data yields partial scores, not failures
 * 
 * ==============================================================================
 * NORMALIZATION STRATEGY
 * ==============================================================================
 * 
 *   We use min-max normalization with domain-specific bounds:
 *   
 *     normalized = (value - min) / (max - min) * 100
 *   
 *   For inverse metrics (higher = worse, like unemployment):
 *   
 *     normalized = (max - value) / (max - min) * 100
 * 
 * @see AiCitySummaryService
 */
@Service
@Slf4j
public class CityFeatureComputer {
    
    // ==========================================================================
    // NORMALIZATION BOUNDS
    // These define the min/max values used for 0-100 scaling
    // ==========================================================================
    
    // GDP per capita bounds (USD)
    // Min: $15,000 (low-income threshold)
    // Max: $150,000 (wealthy metropolitan areas)
    private static final double GDP_MIN = 15_000.0;
    private static final double GDP_MAX = 150_000.0;
    
    // Unemployment rate bounds (percentage)
    // Min: 2% (essentially full employment)
    // Max: 15% (severe economic distress)
    private static final double UNEMPLOYMENT_MIN = 2.0;
    private static final double UNEMPLOYMENT_MAX = 15.0;
    
    // Cost of living index bounds
    // Min: 70 (very affordable areas)
    // Max: 180 (most expensive metro areas like SF, NYC)
    private static final int COST_OF_LIVING_MIN = 70;
    private static final int COST_OF_LIVING_MAX = 180;
    
    // AQI bounds (Air Quality Index)
    // Min: 0 (perfect air)
    // Max: 200 (very unhealthy - beyond this is hazardous)
    private static final int AQI_MIN = 0;
    private static final int AQI_MAX = 200;
    
    // Population bounds (for livability - smaller can be more livable)
    // Min: 50,000 (small city)
    // Max: 10,000,000 (mega city)
    private static final long POPULATION_MIN = 50_000L;
    private static final long POPULATION_MAX = 10_000_000L;
    
    // ==========================================================================
    // SCORE WEIGHTS
    // ==========================================================================
    
    // Economy score weights
    private static final double ECONOMY_GDP_WEIGHT = 0.40;
    private static final double ECONOMY_UNEMPLOYMENT_WEIGHT = 0.60;
    
    // Livability score weights
    private static final double LIVABILITY_COST_WEIGHT = 0.35;
    private static final double LIVABILITY_AQI_WEIGHT = 0.35;
    private static final double LIVABILITY_SIZE_WEIGHT = 0.30;
    
    // ==========================================================================
    // GROWTH SCORE BOUNDS
    // ==========================================================================
    
    // Population growth rate bounds (percentage, annual)
    // Min: -2% (significant population decline)
    // Max: +5% (rapid growth, e.g., boom towns)
    private static final double POPULATION_GROWTH_MIN = -2.0;
    private static final double POPULATION_GROWTH_MAX = 5.0;
    
    // GDP growth rate bounds (percentage, annual)
    // Min: -5% (severe recession)
    // Max: +10% (rapid economic expansion)
    private static final double GDP_GROWTH_MIN = -5.0;
    private static final double GDP_GROWTH_MAX = 10.0;
    
    // Growth score weights
    private static final double GROWTH_POPULATION_WEIGHT = 0.50;
    private static final double GROWTH_GDP_WEIGHT = 0.50;
    
    // Overall score weights (updated to include growth)
    private static final double OVERALL_ECONOMY_WEIGHT = 0.30;
    private static final double OVERALL_LIVABILITY_WEIGHT = 0.35;
    private static final double OVERALL_SUSTAINABILITY_WEIGHT = 0.20;
    private static final double OVERALL_GROWTH_WEIGHT = 0.15;
    
    // ==========================================================================
    // MAIN COMPUTATION METHOD
    // ==========================================================================
    
    /**
     * Compute all structured features for a city.
     * 
     * @param city The city entity with raw metrics
     * @param currentAqi Current air quality index (0-500 scale, null if unavailable)
     * @return CityFeatures containing all computed scores with explanations
     */
    public CityFeatures computeFeatures(City city, Integer currentAqi) {
        return computeFeatures(city, currentAqi, null, null);
    }
    
    /**
     * Compute all structured features for a city with growth metrics.
     * 
     * @param city The city entity with raw metrics
     * @param currentAqi Current air quality index (0-500 scale, null if unavailable)
     * @param populationGrowthRate Annual population growth rate (%, null if unavailable)
     * @param gdpGrowthRate Annual GDP growth rate (%, null if unavailable)
     * @return CityFeatures containing all computed scores with explanations
     */
    public CityFeatures computeFeatures(City city, Integer currentAqi, 
                                         Double populationGrowthRate, Double gdpGrowthRate) {
        log.debug("[FEATURE] Computing features for city: {}", city.getSlug());
        
        // Compute individual scores
        ScoreResult economyScore = computeEconomyScore(city);
        ScoreResult livabilityScore = computeLivabilityScore(city, currentAqi);
        ScoreResult sustainabilityScore = computeSustainabilityScore(currentAqi);
        ScoreResult growthScore = computeGrowthScore(populationGrowthRate, gdpGrowthRate);
        
        // Compute overall score (weighted average of available scores)
        ScoreResult overallScore = computeOverallScore(
                economyScore, livabilityScore, sustainabilityScore, growthScore);
        
        CityFeatures features = CityFeatures.builder()
            .citySlug(city.getSlug())
            .economyScore(economyScore)
            .livabilityScore(livabilityScore)
            .sustainabilityScore(sustainabilityScore)
            .growthScore(growthScore)
            .overallScore(overallScore)
            .dataCompleteness(computeDataCompleteness(city, currentAqi, populationGrowthRate, gdpGrowthRate))
            .build();
        
        log.info("[FEATURE] City {} scores: economy={}, livability={}, sustainability={}, growth={}, overall={}",
                city.getSlug(),
                economyScore.score() != null ? String.format("%.1f", economyScore.score()) : "N/A",
                livabilityScore.score() != null ? String.format("%.1f", livabilityScore.score()) : "N/A",
                sustainabilityScore.score() != null ? String.format("%.1f", sustainabilityScore.score()) : "N/A",
                growthScore.score() != null ? String.format("%.1f", growthScore.score()) : "N/A",
                overallScore.score() != null ? String.format("%.1f", overallScore.score()) : "N/A");
        
        return features;
    }
    
    // ==========================================================================
    // ECONOMY SCORE
    // ==========================================================================
    
    /**
     * Compute economy score based on GDP per capita and unemployment rate.
     * 
     * FORMULA:
     *   economy_score = (gdp_normalized * 0.40) + (unemployment_normalized * 0.60)
     * 
     * WHY THESE WEIGHTS:
     * - Unemployment (60%): More directly affects individual opportunity
     * - GDP (40%): Indicates overall economic health and resources
     * 
     * @param city City with economic metrics
     * @return ScoreResult with value (0-100) and explanation
     */
    private ScoreResult computeEconomyScore(City city) {
        List<String> components = new ArrayList<>();
        List<String> missingData = new ArrayList<>();
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        
        // GDP per capita component
        if (city.getGdpPerCapita() != null) {
            double gdpNormalized = normalize(city.getGdpPerCapita(), GDP_MIN, GDP_MAX);
            weightedSum += gdpNormalized * ECONOMY_GDP_WEIGHT;
            totalWeight += ECONOMY_GDP_WEIGHT;
            
            String gdpTier = classifyGdp(city.getGdpPerCapita());
            components.add(String.format("GDP per capita: $%,.0f (%s, contributes %.1f points)",
                city.getGdpPerCapita(), gdpTier, gdpNormalized * ECONOMY_GDP_WEIGHT));
        } else {
            missingData.add("GDP per capita");
        }
        
        // Unemployment rate component (inverse - lower is better)
        if (city.getUnemploymentRate() != null) {
            double unemploymentNormalized = normalizeInverse(
                city.getUnemploymentRate(), UNEMPLOYMENT_MIN, UNEMPLOYMENT_MAX);
            weightedSum += unemploymentNormalized * ECONOMY_UNEMPLOYMENT_WEIGHT;
            totalWeight += ECONOMY_UNEMPLOYMENT_WEIGHT;
            
            String jobMarket = classifyJobMarket(city.getUnemploymentRate());
            components.add(String.format("Unemployment: %.1f%% (%s, contributes %.1f points)",
                city.getUnemploymentRate(), jobMarket, unemploymentNormalized * ECONOMY_UNEMPLOYMENT_WEIGHT));
        } else {
            missingData.add("Unemployment rate");
        }
        
        // Calculate final score
        if (totalWeight == 0) {
            return new ScoreResult(
                null,
                "Economy score unavailable",
                List.of(),
                missingData,
                0.0
            );
        }
        
        // Scale to account for missing components
        double score = (weightedSum / totalWeight) * 100;
        double confidence = totalWeight / (ECONOMY_GDP_WEIGHT + ECONOMY_UNEMPLOYMENT_WEIGHT);
        
        String explanation = generateEconomyExplanation(score);
        
        return new ScoreResult(score, explanation, components, missingData, confidence);
    }
    
    /**
     * Generate human-readable explanation for economy score.
     */
    private String generateEconomyExplanation(double score) {
        if (score >= 80) {
            return "Excellent economic conditions with strong job market and high prosperity";
        } else if (score >= 60) {
            return "Good economic health with reasonable opportunities";
        } else if (score >= 40) {
            return "Moderate economy with mixed indicators";
        } else if (score >= 20) {
            return "Challenging economic conditions with limited opportunities";
        } else {
            return "Significant economic struggles affecting job seekers";
        }
    }
    
    // ==========================================================================
    // LIVABILITY SCORE
    // ==========================================================================
    
    /**
     * Compute livability score based on cost of living, air quality, and city size.
     * 
     * FORMULA:
     *   livability = (cost_normalized * 0.35) + (aqi_normalized * 0.35) + (size_normalized * 0.30)
     * 
     * WHY THESE WEIGHTS:
     * - Cost of living (35%): Major factor in quality of life
     * - Air quality (35%): Direct health impact
     * - City size (30%): Affects pace of life, community feel (smaller = higher score)
     * 
     * @param city City with livability metrics
     * @param currentAqi Current air quality (null if unavailable)
     * @return ScoreResult with value (0-100) and explanation
     */
    private ScoreResult computeLivabilityScore(City city, Integer currentAqi) {
        List<String> components = new ArrayList<>();
        List<String> missingData = new ArrayList<>();
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        
        // Cost of living component (inverse - lower is better for livability)
        if (city.getCostOfLivingIndex() != null) {
            double costNormalized = normalizeInverse(
                city.getCostOfLivingIndex(), COST_OF_LIVING_MIN, COST_OF_LIVING_MAX);
            weightedSum += costNormalized * LIVABILITY_COST_WEIGHT;
            totalWeight += LIVABILITY_COST_WEIGHT;
            
            String affordability = classifyAffordability(city.getCostOfLivingIndex());
            components.add(String.format("Cost of living index: %d (%s, contributes %.1f points)",
                city.getCostOfLivingIndex(), affordability, costNormalized * LIVABILITY_COST_WEIGHT));
        } else {
            missingData.add("Cost of living index");
        }
        
        // Air quality component (inverse - lower AQI is better)
        if (currentAqi != null) {
            double aqiNormalized = normalizeInverse(currentAqi, AQI_MIN, AQI_MAX);
            weightedSum += aqiNormalized * LIVABILITY_AQI_WEIGHT;
            totalWeight += LIVABILITY_AQI_WEIGHT;
            
            String airQuality = classifyAirQuality(currentAqi);
            components.add(String.format("Air quality (AQI): %d (%s, contributes %.1f points)",
                currentAqi, airQuality, aqiNormalized * LIVABILITY_AQI_WEIGHT));
        } else {
            missingData.add("Air quality index");
        }
        
        // City size component (inverse - smaller cities score higher for livability)
        if (city.getPopulation() != null) {
            // Use log scale for population to avoid mega-cities dominating
            double popNormalized = normalizeInverseLog(
                city.getPopulation(), POPULATION_MIN, POPULATION_MAX);
            weightedSum += popNormalized * LIVABILITY_SIZE_WEIGHT;
            totalWeight += LIVABILITY_SIZE_WEIGHT;
            
            String citySize = classifyCitySize(city.getPopulation());
            components.add(String.format("Population: %,d (%s, contributes %.1f points)",
                city.getPopulation(), citySize, popNormalized * LIVABILITY_SIZE_WEIGHT));
        } else {
            missingData.add("Population");
        }
        
        if (totalWeight == 0) {
            return new ScoreResult(
                null,
                "Livability score unavailable",
                List.of(),
                missingData,
                0.0
            );
        }
        
        double score = (weightedSum / totalWeight) * 100;
        double maxWeight = LIVABILITY_COST_WEIGHT + LIVABILITY_AQI_WEIGHT + LIVABILITY_SIZE_WEIGHT;
        double confidence = totalWeight / maxWeight;
        
        String explanation = generateLivabilityExplanation(score);
        
        return new ScoreResult(score, explanation, components, missingData, confidence);
    }
    
    /**
     * Generate human-readable explanation for livability score.
     */
    private String generateLivabilityExplanation(double score) {
        if (score >= 80) {
            return "Highly livable with excellent affordability, clean air, and manageable size";
        } else if (score >= 60) {
            return "Good quality of life with reasonable costs and environment";
        } else if (score >= 40) {
            return "Average livability with some trade-offs in cost or environment";
        } else if (score >= 20) {
            return "Challenging livability due to high costs, pollution, or urban density";
        } else {
            return "Significant livability challenges requiring careful consideration";
        }
    }
    
    // ==========================================================================
    // SUSTAINABILITY SCORE
    // ==========================================================================
    
    /**
     * Compute sustainability score based on environmental metrics.
     * 
     * CURRENT: Based solely on AQI (air quality)
     * FUTURE: Can incorporate carbon emissions, green space, renewable energy
     * 
     * WHY AQI:
     * - Most readily available environmental metric
     * - Direct indicator of environmental health
     * - Strong correlation with sustainability practices
     * 
     * @param currentAqi Current air quality index
     * @return ScoreResult with value (0-100) and explanation
     */
    private ScoreResult computeSustainabilityScore(Integer currentAqi) {
        List<String> components = new ArrayList<>();
        List<String> missingData = new ArrayList<>();
        
        if (currentAqi == null) {
            missingData.add("Air quality index");
            return new ScoreResult(
                null,
                "Sustainability score unavailable - no environmental data",
                List.of(),
                missingData,
                0.0
            );
        }
        
        // AQI is the primary (currently only) sustainability metric
        double aqiNormalized = normalizeInverse(currentAqi, AQI_MIN, AQI_MAX);
        double score = aqiNormalized * 100;
        
        String airQuality = classifyAirQuality(currentAqi);
        components.add(String.format("Air quality (AQI): %d (%s)", currentAqi, airQuality));
        
        // Note about limited data
        components.add("Note: Sustainability score currently based on air quality only");
        
        String explanation = generateSustainabilityExplanation(score, currentAqi);
        
        return new ScoreResult(score, explanation, components, missingData, 1.0);
    }
    
    /**
     * Generate human-readable explanation for sustainability score.
     */
    private String generateSustainabilityExplanation(double score, int aqi) {
        if (aqi <= 50) {
            return "Excellent environmental conditions with clean air";
        } else if (aqi <= 100) {
            return "Moderate air quality - acceptable for most people";
        } else if (aqi <= 150) {
            return "Unhealthy for sensitive groups - consider health implications";
        } else {
            return "Poor air quality - significant health concerns for all groups";
        }
    }
    
    // ==========================================================================
    // GROWTH SCORE
    // ==========================================================================
    
    /**
     * Compute growth score based on population and GDP growth rates.
     * 
     * FORMULA:
     *   growth_score = (pop_growth_normalized * 0.50) + (gdp_growth_normalized * 0.50)
     * 
     * WHY THESE WEIGHTS:
     * - Population growth (50%): Indicates migration attractiveness and demographic health
     * - GDP growth (50%): Indicates economic momentum and investment potential
     * 
     * NORMALIZATION:
     * - Population growth: -2% to +5% → 0-100
     * - GDP growth: -5% to +10% → 0-100
     * 
     * @param populationGrowthRate Annual population growth rate (%)
     * @param gdpGrowthRate Annual GDP growth rate (%)
     * @return ScoreResult with value (0-100) and explanation
     */
    private ScoreResult computeGrowthScore(Double populationGrowthRate, Double gdpGrowthRate) {
        List<String> components = new ArrayList<>();
        List<String> missingData = new ArrayList<>();
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        
        // Population growth component
        if (populationGrowthRate != null) {
            double popGrowthNormalized = normalize(
                populationGrowthRate, POPULATION_GROWTH_MIN, POPULATION_GROWTH_MAX);
            weightedSum += popGrowthNormalized * GROWTH_POPULATION_WEIGHT;
            totalWeight += GROWTH_POPULATION_WEIGHT;
            
            String growthTier = classifyPopulationGrowth(populationGrowthRate);
            components.add(String.format("Population growth: %.1f%% YoY (%s, contributes %.1f points)",
                populationGrowthRate, growthTier, popGrowthNormalized * GROWTH_POPULATION_WEIGHT * 100));
        } else {
            missingData.add("Population growth rate");
        }
        
        // GDP growth component
        if (gdpGrowthRate != null) {
            double gdpGrowthNormalized = normalize(
                gdpGrowthRate, GDP_GROWTH_MIN, GDP_GROWTH_MAX);
            weightedSum += gdpGrowthNormalized * GROWTH_GDP_WEIGHT;
            totalWeight += GROWTH_GDP_WEIGHT;
            
            String growthTier = classifyGdpGrowth(gdpGrowthRate);
            components.add(String.format("GDP growth: %.1f%% YoY (%s, contributes %.1f points)",
                gdpGrowthRate, growthTier, gdpGrowthNormalized * GROWTH_GDP_WEIGHT * 100));
        } else {
            missingData.add("GDP growth rate");
        }
        
        // Calculate final score
        if (totalWeight == 0) {
            return new ScoreResult(
                null,
                "Growth score unavailable - no growth data",
                List.of(),
                missingData,
                0.0
            );
        }
        
        // Scale to account for missing components
        double score = (weightedSum / totalWeight) * 100;
        double confidence = totalWeight / (GROWTH_POPULATION_WEIGHT + GROWTH_GDP_WEIGHT);
        
        String explanation = generateGrowthExplanation(score, populationGrowthRate, gdpGrowthRate);
        
        return new ScoreResult(score, explanation, components, missingData, confidence);
    }
    
    /**
     * Generate human-readable explanation for growth score.
     */
    private String generateGrowthExplanation(double score, Double popGrowth, Double gdpGrowth) {
        if (score >= 80) {
            return "Rapidly growing city with strong population and economic expansion";
        } else if (score >= 60) {
            return "Healthy growth trajectory with positive momentum";
        } else if (score >= 40) {
            return "Stable with moderate growth - not declining";
        } else if (score >= 20) {
            return "Slow or stagnant growth - limited expansion";
        } else {
            return "Declining city with population loss or economic contraction";
        }
    }
    
    /**
     * Classify population growth rate.
     */
    private String classifyPopulationGrowth(double rate) {
        if (rate >= 3.0) return "rapid growth";
        if (rate >= 1.5) return "strong growth";
        if (rate >= 0.5) return "moderate growth";
        if (rate >= 0.0) return "stable";
        if (rate >= -1.0) return "slow decline";
        return "significant decline";
    }
    
    /**
     * Classify GDP growth rate.
     */
    private String classifyGdpGrowth(double rate) {
        if (rate >= 6.0) return "booming";
        if (rate >= 3.0) return "strong expansion";
        if (rate >= 1.5) return "healthy growth";
        if (rate >= 0.0) return "stable";
        if (rate >= -2.0) return "mild contraction";
        return "recession";
    }
    
    // ==========================================================================
    // OVERALL SCORE
    // ==========================================================================
    
    /**
     * Compute weighted overall score from component scores.
     * 
     * WEIGHTS (updated to include growth):
     * - Economy: 30%
     * - Livability: 35%
     * - Sustainability: 20%
     * - Growth: 15%
     */
    private ScoreResult computeOverallScore(
            ScoreResult economy, ScoreResult livability, 
            ScoreResult sustainability, ScoreResult growth) {
        
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        List<String> components = new ArrayList<>();
        
        if (economy.score() != null) {
            weightedSum += economy.score() * OVERALL_ECONOMY_WEIGHT;
            totalWeight += OVERALL_ECONOMY_WEIGHT;
            components.add(String.format("Economy (30%%): %.1f", economy.score()));
        }
        
        if (livability.score() != null) {
            weightedSum += livability.score() * OVERALL_LIVABILITY_WEIGHT;
            totalWeight += OVERALL_LIVABILITY_WEIGHT;
            components.add(String.format("Livability (35%%): %.1f", livability.score()));
        }
        
        if (sustainability.score() != null) {
            weightedSum += sustainability.score() * OVERALL_SUSTAINABILITY_WEIGHT;
            totalWeight += OVERALL_SUSTAINABILITY_WEIGHT;
            components.add(String.format("Sustainability (20%%): %.1f", sustainability.score()));
        }
        
        if (growth.score() != null) {
            weightedSum += growth.score() * OVERALL_GROWTH_WEIGHT;
            totalWeight += OVERALL_GROWTH_WEIGHT;
            components.add(String.format("Growth (15%%): %.1f", growth.score()));
        }
        
        if (totalWeight == 0) {
            return new ScoreResult(null, "Overall score unavailable", List.of(), List.of(), 0.0);
        }
        
        double score = weightedSum / totalWeight;
        double confidence = totalWeight;
        
        String explanation = generateOverallExplanation(score);
        
        return new ScoreResult(score, explanation, components, List.of(), confidence);
    }
    
    private String generateOverallExplanation(double score) {
        if (score >= 80) {
            return "Outstanding city with excellent conditions across all dimensions";
        } else if (score >= 65) {
            return "Very good city with strong performance in most areas";
        } else if (score >= 50) {
            return "Solid city with reasonable trade-offs";
        } else if (score >= 35) {
            return "Mixed performance with significant areas for improvement";
        } else {
            return "Challenging conditions - carefully evaluate personal priorities";
        }
    }
    
    // ==========================================================================
    // NORMALIZATION UTILITIES
    // ==========================================================================
    
    /**
     * Normalize a value to 0-1 range using min-max scaling.
     * Higher input = higher output.
     */
    private double normalize(double value, double min, double max) {
        if (value <= min) return 0.0;
        if (value >= max) return 1.0;
        return (value - min) / (max - min);
    }
    
    /**
     * Normalize a value to 0-1 range with inverse scaling.
     * Lower input = higher output (used for metrics where lower is better).
     */
    private double normalizeInverse(double value, double min, double max) {
        if (value <= min) return 1.0;
        if (value >= max) return 0.0;
        return (max - value) / (max - min);
    }
    
    /**
     * Normalize using logarithmic scale (for wide-ranging values like population).
     * Inverse because smaller populations score higher for livability.
     */
    private double normalizeInverseLog(long value, long min, long max) {
        if (value <= min) return 1.0;
        if (value >= max) return 0.0;
        
        double logValue = Math.log10(value);
        double logMin = Math.log10(min);
        double logMax = Math.log10(max);
        
        return (logMax - logValue) / (logMax - logMin);
    }
    
    // ==========================================================================
    // CLASSIFICATION UTILITIES
    // ==========================================================================
    
    private String classifyGdp(double gdp) {
        if (gdp >= 100_000) return "wealthy";
        if (gdp >= 60_000) return "prosperous";
        if (gdp >= 40_000) return "comfortable";
        if (gdp >= 25_000) return "developing";
        return "challenged";
    }
    
    private String classifyJobMarket(double unemployment) {
        if (unemployment <= 3.0) return "excellent job market";
        if (unemployment <= 5.0) return "healthy job market";
        if (unemployment <= 7.0) return "moderate job market";
        if (unemployment <= 10.0) return "competitive job market";
        return "struggling job market";
    }
    
    private String classifyAffordability(int costIndex) {
        if (costIndex <= 85) return "very affordable";
        if (costIndex <= 100) return "affordable";
        if (costIndex <= 120) return "moderate";
        if (costIndex <= 150) return "expensive";
        return "very expensive";
    }
    
    private String classifyAirQuality(int aqi) {
        if (aqi <= 50) return "good";
        if (aqi <= 100) return "moderate";
        if (aqi <= 150) return "unhealthy for sensitive groups";
        if (aqi <= 200) return "unhealthy";
        return "very unhealthy";
    }
    
    private String classifyCitySize(long population) {
        if (population < 100_000) return "small city";
        if (population < 500_000) return "mid-sized city";
        if (population < 1_000_000) return "large city";
        if (population < 5_000_000) return "major metropolitan";
        return "mega city";
    }
    
    // ==========================================================================
    // DATA COMPLETENESS
    // ==========================================================================
    
    /**
     * Compute data completeness percentage (0-100).
     * Overloaded for backward compatibility.
     */
    private double computeDataCompleteness(City city, Integer currentAqi) {
        return computeDataCompleteness(city, currentAqi, null, null);
    }
    
    /**
     * Compute data completeness percentage (0-100).
     * Includes growth metrics for full completeness calculation.
     */
    private double computeDataCompleteness(City city, Integer currentAqi,
                                            Double populationGrowthRate, Double gdpGrowthRate) {
        int available = 0;
        int total = 7;  // Updated to include growth metrics
        
        if (city.getGdpPerCapita() != null) available++;
        if (city.getUnemploymentRate() != null) available++;
        if (city.getCostOfLivingIndex() != null) available++;
        if (city.getPopulation() != null) available++;
        if (currentAqi != null) available++;
        if (populationGrowthRate != null) available++;
        if (gdpGrowthRate != null) available++;
        
        return (double) available / total * 100;
    }
    
    // ==========================================================================
    // RESULT DATA CLASSES
    // ==========================================================================
    
    /**
     * Complete feature set for a city.
     * 
     * Contains all computed scores for AI-powered city analysis:
     * - economyScore: Economic health (GDP, unemployment)
     * - livabilityScore: Quality of life (cost, AQI, size)
     * - sustainabilityScore: Environmental health (AQI, future: carbon)
     * - growthScore: Growth trajectory (population, GDP growth)
     * - overallScore: Weighted composite of all scores
     */
    @Data
    @Builder
    public static class CityFeatures {
        private String citySlug;
        private ScoreResult economyScore;
        private ScoreResult livabilityScore;
        private ScoreResult sustainabilityScore;
        private ScoreResult growthScore;
        private ScoreResult overallScore;
        private double dataCompleteness;
        
        /**
         * Generate a structured prompt section for AI summary.
         */
        public String toPromptSection() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== COMPUTED CITY SCORES ===\n");
            
            if (economyScore != null && economyScore.score() != null) {
                sb.append(String.format("Economy Score: %.1f/100 - %s\n",
                    economyScore.score(), economyScore.explanation()));
            }
            
            if (livabilityScore != null && livabilityScore.score() != null) {
                sb.append(String.format("Livability Score: %.1f/100 - %s\n",
                    livabilityScore.score(), livabilityScore.explanation()));
            }
            
            if (sustainabilityScore != null && sustainabilityScore.score() != null) {
                sb.append(String.format("Sustainability Score: %.1f/100 - %s\n",
                    sustainabilityScore.score(), sustainabilityScore.explanation()));
            }
            
            if (growthScore != null && growthScore.score() != null) {
                sb.append(String.format("Growth Score: %.1f/100 - %s\n",
                    growthScore.score(), growthScore.explanation()));
            }
            
            if (overallScore != null && overallScore.score() != null) {
                sb.append(String.format("Overall Score: %.1f/100 - %s\n",
                    overallScore.score(), overallScore.explanation()));
            }
            
            sb.append(String.format("Data Completeness: %.0f%%\n", dataCompleteness));
            
            return sb.toString();
        }
    }
    
    /**
     * Individual score result with explanation and audit trail.
     */
    public record ScoreResult(
        Double score,           // 0-100 scale, null if unavailable
        String explanation,     // Human-readable summary
        List<String> components, // Breakdown of contributing factors
        List<String> missingData, // Data that was unavailable
        Double confidence       // 0-1 confidence based on data availability
    ) {
        /**
         * Get score tier for categorization.
         */
        public String tier() {
            if (score == null) return "unavailable";
            if (score >= 80) return "excellent";
            if (score >= 60) return "good";
            if (score >= 40) return "average";
            if (score >= 20) return "below-average";
            return "poor";
        }
    }
}
