package com.cityatlas.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cityatlas.backend.dto.response.ExplainableAiSummary;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.AiTransparency;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.CityAssessment;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.DataEvidence;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.DataFreshness;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.FeatureContributions;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.FeatureImpact;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.ReasonedConclusion;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.ReasoningChain;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.ScoreBreakdown;
import com.cityatlas.backend.dto.response.ExplainableAiSummary.ScoreComponent;
import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.service.CityFeatureComputer.CityFeatures;
import com.cityatlas.backend.service.CityFeatureComputer.ScoreResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * AI EXPLAINABILITY ENGINE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Generates transparent, interview-justifiable explanations for AI assessments.
 * Every conclusion traces back to concrete data with clear reasoning chains.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * INTERVIEW TALKING POINTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Q: "How do you handle AI explainability in your system?"
 * A: "We use a three-layer explainability architecture:
 *     
 *     Layer 1: FEATURE ATTRIBUTION
 *     - Each score shows exactly which inputs contributed and by how much
 *     - Example: Economy score = 78, where GDP contributed 32pts, unemployment 28pts
 *     
 *     Layer 2: REASONING CHAINS
 *     - Every conclusion includes the rule that triggered it
 *     - Example: 'Strong economy' because GDP > $60K threshold (rule) AND GDP = $85K (evidence)
 *     
 *     Layer 3: TRANSPARENCY METADATA
 *     - Algorithm version, data freshness, limitations are all explicit
 *     - Users know exactly how confident they should be in each assessment"
 * 
 * @see ExplainableAiSummary
 * @see CityFeatureComputer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiExplainabilityEngine {
    
    private final CityFeatureComputer featureComputer;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // THRESHOLDS FOR REASONING (must match CityFeatureComputer)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final double HIGH_GDP_THRESHOLD = 60_000.0;
    private static final double LOW_UNEMPLOYMENT_THRESHOLD = 4.5;
    private static final double GOOD_AQI_THRESHOLD = 50.0;
    private static final double HIGH_COST_THRESHOLD = 120.0;
    private static final long MAJOR_CITY_POPULATION = 1_000_000L;
    
    // Score tier boundaries
    private static final double EXCELLENT_THRESHOLD = 80.0;
    private static final double GOOD_THRESHOLD = 60.0;
    private static final double AVERAGE_THRESHOLD = 40.0;
    private static final double BELOW_AVERAGE_THRESHOLD = 20.0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN EXPLAINABILITY GENERATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generate a fully explainable AI summary for a city.
     * 
     * @param city The city entity with all metrics
     * @param currentAqi Optional real-time AQI value
     * @return ExplainableAiSummary with full reasoning chains
     */
    public ExplainableAiSummary generateExplainableSummary(City city, Double currentAqi) {
        log.info("Generating explainable AI summary for city: {}", city.getSlug());
        
        // Phase 1: Compute features using existing engine
        Integer aqiInt = currentAqi != null ? currentAqi.intValue() : null;
        CityFeatures features = featureComputer.computeFeatures(city, aqiInt);
        
        // Phase 2: Build explainable summary with reasoning
        return ExplainableAiSummary.builder()
                .citySlug(city.getSlug())
                .cityName(city.getName())
                .generatedAt(LocalDateTime.now())
                .assessment(generateAssessment(city, features))
                .featureContributions(generateFeatureContributions(city, features, currentAqi))
                .strengths(generateExplainedStrengths(city, features, currentAqi))
                .weaknesses(generateExplainedWeaknesses(city, features, currentAqi))
                .bestSuitedFor(generateExplainedAudienceFit(city, features, currentAqi))
                .transparency(generateTransparencyMetadata(features))
                .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ASSESSMENT GENERATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private CityAssessment generateAssessment(City city, CityFeatures features) {
        Double overallScore = features.getOverallScore().score();
        String verdict = getScoreTier(overallScore);
        
        List<String> reasoningChain = new ArrayList<>();
        if (overallScore != null) {
            reasoningChain.add(String.format("Overall score: %.0f/100", overallScore));
        }
        if (features.getEconomyScore().score() != null) {
            reasoningChain.add(String.format("Economy score (30%% weight): %.0f", 
                    features.getEconomyScore().score()));
        }
        if (features.getLivabilityScore().score() != null) {
            reasoningChain.add(String.format("Livability score (35%% weight): %.0f", 
                    features.getLivabilityScore().score()));
        }
        if (features.getSustainabilityScore().score() != null) {
            reasoningChain.add(String.format("Sustainability score (20%% weight): %.0f", 
                    features.getSustainabilityScore().score()));
        }
        if (features.getGrowthScore().score() != null) {
            reasoningChain.add(String.format("Growth score (15%% weight): %.0f", 
                    features.getGrowthScore().score()));
        }
        reasoningChain.add(String.format("Therefore: %s overall assessment", verdict));
        
        List<FeatureImpact> keyDrivers = identifyKeyDrivers(features);
        String summary = generateAssessmentSummary(features, keyDrivers);
        
        return CityAssessment.builder()
                .verdict(verdict)
                .confidence(features.getDataCompleteness())
                .summary(summary)
                .reasoningChain(reasoningChain)
                .keyDrivers(keyDrivers)
                .build();
    }
    
    private List<FeatureImpact> identifyKeyDrivers(CityFeatures features) {
        List<FeatureImpact> drivers = new ArrayList<>();
        
        Double economyScore = features.getEconomyScore().score();
        if (economyScore != null) {
            drivers.add(FeatureImpact.builder()
                    .feature("economy")
                    .score(economyScore)
                    .direction(economyScore >= 60 ? "positive" : 
                            economyScore >= 40 ? "neutral" : "negative")
                    .magnitude(getMagnitude(economyScore))
                    .explanation(features.getEconomyScore().explanation())
                    .build());
        }
        
        Double livabilityScore = features.getLivabilityScore().score();
        if (livabilityScore != null) {
            drivers.add(FeatureImpact.builder()
                    .feature("livability")
                    .score(livabilityScore)
                    .direction(livabilityScore >= 60 ? "positive" : 
                            livabilityScore >= 40 ? "neutral" : "negative")
                    .magnitude(getMagnitude(livabilityScore))
                    .explanation(features.getLivabilityScore().explanation())
                    .build());
        }
        
        Double sustainabilityScore = features.getSustainabilityScore().score();
        if (sustainabilityScore != null) {
            drivers.add(FeatureImpact.builder()
                    .feature("sustainability")
                    .score(sustainabilityScore)
                    .direction(sustainabilityScore >= 60 ? "positive" : 
                            sustainabilityScore >= 40 ? "neutral" : "negative")
                    .magnitude(getMagnitude(sustainabilityScore))
                    .explanation(features.getSustainabilityScore().explanation())
                    .build());
        }
        
        Double growthScore = features.getGrowthScore().score();
        if (growthScore != null) {
            drivers.add(FeatureImpact.builder()
                    .feature("growth")
                    .score(growthScore)
                    .direction(growthScore >= 60 ? "positive" : 
                            growthScore >= 40 ? "neutral" : "negative")
                    .magnitude(getMagnitude(growthScore))
                    .explanation(features.getGrowthScore().explanation())
                    .build());
        }
        
        // Sort by magnitude of impact (deviation from 50)
        drivers.sort((a, b) -> Double.compare(
                Math.abs(b.getScore() - 50), 
                Math.abs(a.getScore() - 50)));
        
        return drivers;
    }
    
    private String generateAssessmentSummary(CityFeatures features, List<FeatureImpact> keyDrivers) {
        List<String> positives = new ArrayList<>();
        List<String> negatives = new ArrayList<>();
        
        for (FeatureImpact driver : keyDrivers) {
            if ("positive".equals(driver.getDirection()) && "high".equals(driver.getMagnitude())) {
                positives.add("strong " + driver.getFeature());
            } else if ("negative".equals(driver.getDirection()) && "high".equals(driver.getMagnitude())) {
                negatives.add("weak " + driver.getFeature());
            }
        }
        
        StringBuilder summary = new StringBuilder();
        if (!positives.isEmpty()) {
            summary.append(String.join(" and ", positives));
        }
        if (!negatives.isEmpty()) {
            if (!positives.isEmpty()) {
                summary.append(", but ");
            }
            summary.append(String.join(" and ", negatives));
        }
        
        return summary.length() > 0 ? 
                summary.substring(0, 1).toUpperCase() + summary.substring(1) : 
                "Balanced performance across all dimensions";
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FEATURE CONTRIBUTIONS - Component-level breakdown
    // ═══════════════════════════════════════════════════════════════════════════
    
    private FeatureContributions generateFeatureContributions(City city, 
            CityFeatures features, Double currentAqi) {
        
        return FeatureContributions.builder()
                .economy(generateEconomyBreakdown(city, features.getEconomyScore()))
                .livability(generateLivabilityBreakdown(city, features.getLivabilityScore(), currentAqi))
                .sustainability(generateSustainabilityBreakdown(city, features.getSustainabilityScore(), currentAqi))
                .growth(generateGrowthBreakdown(features.getGrowthScore()))
                .overall(generateOverallBreakdown(features))
                .build();
    }
    
    private ScoreBreakdown generateEconomyBreakdown(City city, ScoreResult economyScore) {
        List<ScoreComponent> components = new ArrayList<>();
        List<String> missingInputs = new ArrayList<>();
        
        // GDP per capita component (40% weight)
        if (city.getGdpPerCapita() != null) {
            double gdp = city.getGdpPerCapita();
            double normalized = normalizeGdp(gdp);
            double contribution = normalized * 0.40;
            
            components.add(ScoreComponent.builder()
                    .metric("GDP per capita")
                    .rawValue(String.format("$%,.0f", gdp))
                    .normalizedValue(normalized)
                    .weight(0.40)
                    .contribution(contribution)
                    .impact(normalized >= 50 ? "positive" : "negative")
                    .explanation(String.format(
                            "GDP of $%,.0f is %s, contributing %.1f points to economy score",
                            gdp, getGdpTier(gdp), contribution))
                    .build());
        } else {
            missingInputs.add("GDP per capita");
        }
        
        // Unemployment rate component (40% weight)
        if (city.getUnemploymentRate() != null) {
            double unemployment = city.getUnemploymentRate();
            double normalized = normalizeUnemployment(unemployment);
            double contribution = normalized * 0.40;
            
            components.add(ScoreComponent.builder()
                    .metric("Unemployment rate")
                    .rawValue(String.format("%.1f%%", unemployment))
                    .normalizedValue(normalized)
                    .weight(0.40)
                    .contribution(contribution)
                    .impact(unemployment <= LOW_UNEMPLOYMENT_THRESHOLD ? "positive" : "negative")
                    .explanation(String.format(
                            "Unemployment of %.1f%% is %s, contributing %.1f points to economy score",
                            unemployment, getUnemploymentTier(unemployment), contribution))
                    .build());
        } else {
            missingInputs.add("Unemployment rate");
        }
        
        // Cost of living component (20% weight, inverse)
        if (city.getCostOfLivingIndex() != null) {
            double col = city.getCostOfLivingIndex();
            double normalized = normalizeCostOfLiving(col);
            double contribution = normalized * 0.20;
            
            components.add(ScoreComponent.builder()
                    .metric("Cost of living index")
                    .rawValue(String.format("%.0f", col))
                    .normalizedValue(normalized)
                    .weight(0.20)
                    .contribution(contribution)
                    .impact(col <= 100 ? "positive" : "negative")
                    .explanation(String.format(
                            "Cost of living index of %.0f is %s, contributing %.1f points to economy score",
                            col, getCostOfLivingTier(col), contribution))
                    .build());
        } else {
            missingInputs.add("Cost of living index");
        }
        
        double confidence = components.size() / 3.0;
        
        return ScoreBreakdown.builder()
                .score(economyScore.score())
                .tier(getScoreTier(economyScore.score()))
                .explanation(economyScore.explanation())
                .components(components)
                .missingInputs(missingInputs.isEmpty() ? null : missingInputs)
                .confidence(confidence)
                .build();
    }
    
    private ScoreBreakdown generateLivabilityBreakdown(City city, ScoreResult livabilityScore, 
            Double currentAqi) {
        List<ScoreComponent> components = new ArrayList<>();
        List<String> missingInputs = new ArrayList<>();
        
        // AQI component (40% weight) - use currentAqi if provided
        if (currentAqi != null) {
            double normalized = normalizeAqi(currentAqi);
            double contribution = normalized * 0.40;
            
            components.add(ScoreComponent.builder()
                    .metric("Air Quality Index (AQI)")
                    .rawValue(String.format("%.0f", currentAqi))
                    .normalizedValue(normalized)
                    .weight(0.40)
                    .contribution(contribution)
                    .impact(currentAqi <= GOOD_AQI_THRESHOLD ? "positive" : "negative")
                    .explanation(String.format(
                            "AQI of %.0f is %s, contributing %.1f points to livability score",
                            currentAqi, getAqiTier(currentAqi), contribution))
                    .build());
        } else {
            missingInputs.add("Air Quality Index (AQI)");
        }
        
        // Cost of living component (30% weight)
        if (city.getCostOfLivingIndex() != null) {
            double col = city.getCostOfLivingIndex();
            double normalized = normalizeCostOfLiving(col);
            double contribution = normalized * 0.30;
            
            components.add(ScoreComponent.builder()
                    .metric("Cost of living (affordability)")
                    .rawValue(String.format("%.0f", col))
                    .normalizedValue(normalized)
                    .weight(0.30)
                    .contribution(contribution)
                    .impact(col <= 100 ? "positive" : "negative")
                    .explanation(String.format(
                            "Cost of living of %.0f affects affordability, contributing %.1f points",
                            col, contribution))
                    .build());
        } else {
            missingInputs.add("Cost of living index");
        }
        
        // Population as urban infrastructure proxy (30% weight)
        if (city.getPopulation() != null) {
            long pop = city.getPopulation();
            double normalized = normalizePopulation(pop);
            double contribution = normalized * 0.30;
            
            components.add(ScoreComponent.builder()
                    .metric("Urban infrastructure (population proxy)")
                    .rawValue(formatPopulation(pop))
                    .normalizedValue(normalized)
                    .weight(0.30)
                    .contribution(contribution)
                    .impact("neutral")
                    .explanation(String.format(
                            "Population of %s indicates %s urban infrastructure, contributing %.1f points",
                            formatPopulation(pop), pop >= MAJOR_CITY_POPULATION ? "major" : "developing", contribution))
                    .build());
        } else {
            missingInputs.add("Population");
        }
        
        double confidence = components.size() / 3.0;
        
        return ScoreBreakdown.builder()
                .score(livabilityScore.score())
                .tier(getScoreTier(livabilityScore.score()))
                .explanation(livabilityScore.explanation())
                .components(components)
                .missingInputs(missingInputs.isEmpty() ? null : missingInputs)
                .confidence(confidence)
                .build();
    }
    
    private ScoreBreakdown generateSustainabilityBreakdown(City city, 
            ScoreResult sustainabilityScore, Double currentAqi) {
        List<ScoreComponent> components = new ArrayList<>();
        List<String> missingInputs = new ArrayList<>();
        
        // AQI component (70% weight for sustainability)
        if (currentAqi != null) {
            double normalized = normalizeAqi(currentAqi);
            double contribution = normalized * 0.70;
            
            components.add(ScoreComponent.builder()
                    .metric("Air Quality Index (environmental health)")
                    .rawValue(String.format("%.0f", currentAqi))
                    .normalizedValue(normalized)
                    .weight(0.70)
                    .contribution(contribution)
                    .impact(currentAqi <= GOOD_AQI_THRESHOLD ? "positive" : "negative")
                    .explanation(String.format(
                            "AQI of %.0f indicates %s environmental health, contributing %.1f points",
                            currentAqi, getAqiTier(currentAqi), contribution))
                    .build());
        } else {
            missingInputs.add("Air Quality Index (AQI)");
        }
        
        // Population density as green space proxy (30% weight) - placeholder
        if (city.getPopulation() != null) {
            // Use default density estimation (no area data available)
            double estimatedDensity = city.getPopulation() / 500.0; // Assume 500 km² avg
            double normalized = normalizeGreenSpace(estimatedDensity);
            double contribution = normalized * 0.30;
            
            components.add(ScoreComponent.builder()
                    .metric("Green space potential (estimated)")
                    .rawValue(String.format("%s population", formatPopulation(city.getPopulation())))
                    .normalizedValue(normalized)
                    .weight(0.30)
                    .contribution(contribution)
                    .impact("neutral")
                    .explanation(String.format(
                            "Population size suggests %s green space availability, contributing %.1f points",
                            city.getPopulation() < MAJOR_CITY_POPULATION ? "adequate" : "limited", contribution))
                    .build());
        } else {
            missingInputs.add("Population data");
        }
        
        double confidence = components.size() / 2.0;
        
        return ScoreBreakdown.builder()
                .score(sustainabilityScore.score())
                .tier(getScoreTier(sustainabilityScore.score()))
                .explanation(sustainabilityScore.explanation())
                .components(components)
                .missingInputs(missingInputs.isEmpty() ? null : missingInputs)
                .confidence(confidence)
                .build();
    }
    
    private ScoreBreakdown generateGrowthBreakdown(ScoreResult growthScore) {
        List<String> missingInputs = Arrays.asList(
                "Population growth rate", 
                "GDP growth rate"
        );
        
        // Growth data not available in current City entity
        return ScoreBreakdown.builder()
                .score(growthScore.score())
                .tier(getScoreTier(growthScore.score()))
                .explanation(growthScore.explanation())
                .components(new ArrayList<>())
                .missingInputs(missingInputs)
                .confidence(0.0)
                .build();
    }
    
    private ScoreBreakdown generateOverallBreakdown(CityFeatures features) {
        List<ScoreComponent> components = new ArrayList<>();
        
        // Economy contribution (30% weight)
        Double economyScore = features.getEconomyScore().score();
        if (economyScore != null) {
            components.add(ScoreComponent.builder()
                    .metric("Economy dimension")
                    .rawValue(String.format("%.0f/100", economyScore))
                    .normalizedValue(economyScore)
                    .weight(0.30)
                    .contribution(economyScore * 0.30)
                    .impact(economyScore >= 60 ? "positive" : 
                            economyScore >= 40 ? "neutral" : "negative")
                    .explanation(String.format(
                            "Economy score of %.0f contributes %.1f points (30%% weight)",
                            economyScore, economyScore * 0.30))
                    .build());
        }
        
        // Livability contribution (35% weight - highest)
        Double livabilityScore = features.getLivabilityScore().score();
        if (livabilityScore != null) {
            components.add(ScoreComponent.builder()
                    .metric("Livability dimension")
                    .rawValue(String.format("%.0f/100", livabilityScore))
                    .normalizedValue(livabilityScore)
                    .weight(0.35)
                    .contribution(livabilityScore * 0.35)
                    .impact(livabilityScore >= 60 ? "positive" : 
                            livabilityScore >= 40 ? "neutral" : "negative")
                    .explanation(String.format(
                            "Livability score of %.0f contributes %.1f points (35%% weight, highest)",
                            livabilityScore, livabilityScore * 0.35))
                    .build());
        }
        
        // Sustainability contribution (20% weight)
        Double sustainabilityScore = features.getSustainabilityScore().score();
        if (sustainabilityScore != null) {
            components.add(ScoreComponent.builder()
                    .metric("Sustainability dimension")
                    .rawValue(String.format("%.0f/100", sustainabilityScore))
                    .normalizedValue(sustainabilityScore)
                    .weight(0.20)
                    .contribution(sustainabilityScore * 0.20)
                    .impact(sustainabilityScore >= 60 ? "positive" : 
                            sustainabilityScore >= 40 ? "neutral" : "negative")
                    .explanation(String.format(
                            "Sustainability score of %.0f contributes %.1f points (20%% weight)",
                            sustainabilityScore, sustainabilityScore * 0.20))
                    .build());
        }
        
        // Growth contribution (15% weight)
        Double growthScore = features.getGrowthScore().score();
        if (growthScore != null) {
            components.add(ScoreComponent.builder()
                    .metric("Growth dimension")
                    .rawValue(String.format("%.0f/100", growthScore))
                    .normalizedValue(growthScore)
                    .weight(0.15)
                    .contribution(growthScore * 0.15)
                    .impact(growthScore >= 60 ? "positive" : 
                            growthScore >= 40 ? "neutral" : "negative")
                    .explanation(String.format(
                            "Growth score of %.0f contributes %.1f points (15%% weight)",
                            growthScore, growthScore * 0.15))
                    .build());
        }
        
        return ScoreBreakdown.builder()
                .score(features.getOverallScore().score())
                .tier(getScoreTier(features.getOverallScore().score()))
                .explanation(features.getOverallScore().explanation())
                .components(components)
                .confidence(features.getDataCompleteness())
                .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXPLAINED STRENGTHS - With full reasoning chains
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<ReasonedConclusion> generateExplainedStrengths(City city, 
            CityFeatures features, Double currentAqi) {
        List<ReasonedConclusion> strengths = new ArrayList<>();
        
        // Check economy strengths
        if (city.getGdpPerCapita() != null && city.getGdpPerCapita() >= HIGH_GDP_THRESHOLD) {
            strengths.add(ReasonedConclusion.builder()
                    .conclusion("Prosperous economy with high income levels")
                    .category("economy")
                    .confidence(0.95)
                    .significance("high")
                    .reasoning(ReasoningChain.builder()
                            .rule("GDP per capita > $60,000 indicates prosperous economy")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("GDP per capita")
                                            .value(String.format("$%,.0f", city.getGdpPerCapita()))
                                            .comparison("Above prosperity threshold ($60,000)")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("GDP per capita is $%,.0f", city.getGdpPerCapita()),
                                    String.format("$%,.0f > $60,000 prosperity threshold", city.getGdpPerCapita()),
                                    "Therefore: City has a prosperous economy"))
                            .build())
                    .build());
        }
        
        // Check employment strengths
        if (city.getUnemploymentRate() != null && city.getUnemploymentRate() <= LOW_UNEMPLOYMENT_THRESHOLD) {
            strengths.add(ReasonedConclusion.builder()
                    .conclusion("Strong job market with low unemployment")
                    .category("economy")
                    .confidence(0.90)
                    .significance("high")
                    .reasoning(ReasoningChain.builder()
                            .rule("Unemployment rate < 4.5% indicates healthy job market")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Unemployment rate")
                                            .value(String.format("%.1f%%", city.getUnemploymentRate()))
                                            .comparison("Below healthy threshold (4.5%)")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Unemployment rate is %.1f%%", city.getUnemploymentRate()),
                                    String.format("%.1f%% < 4.5%% healthy threshold", city.getUnemploymentRate()),
                                    "Therefore: City has a strong job market"))
                            .build())
                    .build());
        }
        
        // Check AQI strengths
        if (currentAqi != null && currentAqi <= GOOD_AQI_THRESHOLD) {
            strengths.add(ReasonedConclusion.builder()
                    .conclusion("Excellent air quality")
                    .category("sustainability")
                    .confidence(0.95)
                    .significance("high")
                    .reasoning(ReasoningChain.builder()
                            .rule("AQI ≤ 50 indicates 'Good' air quality (EPA standard)")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Air Quality Index")
                                            .value(String.format("%.0f", currentAqi))
                                            .comparison("In 'Good' range (0-50)")
                                            .source("EPA Air Quality Index")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Current AQI is %.0f", currentAqi),
                                    String.format("%.0f ≤ 50 (Good threshold)", currentAqi),
                                    "Therefore: City has excellent air quality"))
                            .build())
                    .build());
        }
        
        // Check affordability strengths
        if (city.getCostOfLivingIndex() != null && city.getCostOfLivingIndex() <= 100) {
            strengths.add(ReasonedConclusion.builder()
                    .conclusion("Affordable cost of living")
                    .category("livability")
                    .confidence(0.85)
                    .significance("medium")
                    .reasoning(ReasoningChain.builder()
                            .rule("Cost of living index ≤ 100 indicates below-average living costs")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Cost of living index")
                                            .value(String.format("%d", city.getCostOfLivingIndex()))
                                            .comparison("At or below national average (100)")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Cost of living index is %d", city.getCostOfLivingIndex()),
                                    String.format("%d ≤ 100 (national average)", city.getCostOfLivingIndex()),
                                    "Therefore: City is affordable to live in"))
                            .build())
                    .build());
        }
        
        return strengths;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXPLAINED WEAKNESSES - With full reasoning chains
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<ReasonedConclusion> generateExplainedWeaknesses(City city, 
            CityFeatures features, Double currentAqi) {
        List<ReasonedConclusion> weaknesses = new ArrayList<>();
        
        // Check high cost of living
        if (city.getCostOfLivingIndex() != null && city.getCostOfLivingIndex() >= HIGH_COST_THRESHOLD) {
            weaknesses.add(ReasonedConclusion.builder()
                    .conclusion("High cost of living may strain budgets")
                    .category("livability")
                    .confidence(0.90)
                    .significance("high")
                    .reasoning(ReasoningChain.builder()
                            .rule("Cost of living index ≥ 120 indicates expensive city")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Cost of living index")
                                            .value(String.format("%d", city.getCostOfLivingIndex()))
                                            .comparison("Above high-cost threshold (120)")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Cost of living index is %d", city.getCostOfLivingIndex()),
                                    String.format("%d ≥ 120 (high-cost threshold)", city.getCostOfLivingIndex()),
                                    "Therefore: City has high cost of living"))
                            .caveat("High cost may be offset by high income opportunities")
                            .build())
                    .build());
        }
        
        // Check poor air quality
        if (currentAqi != null && currentAqi > 100) {
            String healthRisk = currentAqi > 150 ? "Unhealthy for all" : "Unhealthy for sensitive groups";
            weaknesses.add(ReasonedConclusion.builder()
                    .conclusion("Poor air quality affects health and outdoor activities")
                    .category("sustainability")
                    .confidence(0.95)
                    .significance("high")
                    .reasoning(ReasoningChain.builder()
                            .rule("AQI > 100 indicates unhealthy air quality")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Air Quality Index")
                                            .value(String.format("%.0f", currentAqi))
                                            .comparison(healthRisk + " (AQI > 100)")
                                            .source("EPA Air Quality Index")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Current AQI is %.0f", currentAqi),
                                    String.format("%.0f > 100 (Unhealthy threshold)", currentAqi),
                                    "Therefore: City has poor air quality"))
                            .caveat("Air quality can vary seasonally")
                            .build())
                    .build());
        }
        
        // Check high unemployment
        if (city.getUnemploymentRate() != null && city.getUnemploymentRate() > 7.0) {
            weaknesses.add(ReasonedConclusion.builder()
                    .conclusion("Elevated unemployment indicates job market challenges")
                    .category("economy")
                    .confidence(0.85)
                    .significance("high")
                    .reasoning(ReasoningChain.builder()
                            .rule("Unemployment > 7% indicates challenging job market")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Unemployment rate")
                                            .value(String.format("%.1f%%", city.getUnemploymentRate()))
                                            .comparison("Above concern threshold (7%)")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Unemployment rate is %.1f%%", city.getUnemploymentRate()),
                                    String.format("%.1f%% > 7%% concern threshold", city.getUnemploymentRate()),
                                    "Therefore: Job market may be challenging"))
                            .caveat("Situation may be sector-specific")
                            .build())
                    .build());
        }
        
        return weaknesses;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXPLAINED AUDIENCE FIT - With justification
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<ReasonedConclusion> generateExplainedAudienceFit(City city, 
            CityFeatures features, Double currentAqi) {
        List<ReasonedConclusion> audiences = new ArrayList<>();
        
        // Professionals - high GDP, low unemployment
        Double economyScore = features.getEconomyScore().score();
        if (economyScore != null && economyScore >= 70) {
            audiences.add(ReasonedConclusion.builder()
                    .conclusion("Career-focused professionals seeking economic opportunities")
                    .category("economy")
                    .confidence(0.90)
                    .significance("high")
                    .reasoning(ReasoningChain.builder()
                            .rule("Economy score ≥ 70 indicates strong professional opportunities")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Economy score")
                                            .value(String.format("%.0f/100", economyScore))
                                            .comparison("Above professional threshold (70)")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Economy score is %.0f/100", economyScore),
                                    "Strong economy indicates job opportunities and high wages",
                                    "Therefore: Suitable for career-focused professionals"))
                            .build())
                    .build());
        }
        
        // Families - good livability, not too expensive
        Double livabilityScore = features.getLivabilityScore().score();
        if (livabilityScore != null && livabilityScore >= 60 && 
                (city.getCostOfLivingIndex() == null || city.getCostOfLivingIndex() < HIGH_COST_THRESHOLD)) {
            audiences.add(ReasonedConclusion.builder()
                    .conclusion("Families seeking balanced quality of life")
                    .category("livability")
                    .confidence(0.85)
                    .significance("high")
                    .reasoning(ReasoningChain.builder()
                            .rule("High livability + moderate costs = family-friendly")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Livability score")
                                            .value(String.format("%.0f/100", livabilityScore))
                                            .comparison("Above family threshold (60)")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Livability score is %.0f/100", livabilityScore),
                                    "Good livability indicates quality of life amenities",
                                    "Affordable costs allow for family budgets",
                                    "Therefore: Suitable for families"))
                            .build())
                    .build());
        }
        
        // Remote workers - affordable with good air quality
        if ((city.getCostOfLivingIndex() != null && city.getCostOfLivingIndex() <= 100) &&
                (currentAqi == null || currentAqi <= 100)) {
            audiences.add(ReasonedConclusion.builder()
                    .conclusion("Remote workers seeking affordable, livable locations")
                    .category("livability")
                    .confidence(0.80)
                    .significance("medium")
                    .reasoning(ReasoningChain.builder()
                            .rule("Low cost + decent environment = remote worker friendly")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Cost of living")
                                            .value(String.format("%d", city.getCostOfLivingIndex()))
                                            .comparison("At or below average (100)")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Cost of living is %d (affordable)", city.getCostOfLivingIndex()),
                                    "Remote workers can work from anywhere",
                                    "Low cost stretches remote salary further",
                                    "Therefore: Attractive for remote workers"))
                            .build())
                    .build());
        }
        
        // Eco-conscious - good sustainability
        Double sustainabilityScore = features.getSustainabilityScore().score();
        if (sustainabilityScore != null && sustainabilityScore >= 70) {
            audiences.add(ReasonedConclusion.builder()
                    .conclusion("Environmentally-conscious residents")
                    .category("sustainability")
                    .confidence(0.85)
                    .significance("medium")
                    .reasoning(ReasoningChain.builder()
                            .rule("Sustainability score ≥ 70 indicates eco-friendly city")
                            .evidence(Arrays.asList(
                                    DataEvidence.builder()
                                            .metric("Sustainability score")
                                            .value(String.format("%.0f/100", sustainabilityScore))
                                            .comparison("Above eco-friendly threshold (70)")
                                            .build()))
                            .inferenceSteps(Arrays.asList(
                                    String.format("Sustainability score is %.0f/100", sustainabilityScore),
                                    "Good air quality and environmental health",
                                    "Therefore: Suitable for eco-conscious residents"))
                            .build())
                    .build());
        }
        
        return audiences;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSPARENCY METADATA
    // ═══════════════════════════════════════════════════════════════════════════
    
    private AiTransparency generateTransparencyMetadata(CityFeatures features) {
        return AiTransparency.builder()
                .algorithm("Rule-based deterministic scoring")
                .version("2.0")
                .analyzedAt(LocalDateTime.now())
                .dataFreshness(DataFreshness.builder()
                        .freshPercentage(features.getDataCompleteness() * 100)
                        .sources(Arrays.asList("City database", "External APIs"))
                        .build())
                .limitations(Arrays.asList(
                        "Based on available quantitative data only",
                        "Does not account for subjective quality-of-life factors",
                        "Historical data may not reflect recent changes",
                        "Some metrics use proxy measurements"))
                .interpretationGuide("Scores range from 0-100. " +
                        "Excellent (80+), Good (60-79), Average (40-59), " +
                        "Below Average (20-39), Poor (<20). " +
                        "Confidence indicates data availability percentage.")
                .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NORMALIZATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private double normalizeGdp(double gdp) {
        return Math.min(100, Math.max(0, (gdp / 100000.0) * 100));
    }
    
    private double normalizeUnemployment(double unemployment) {
        return Math.min(100, Math.max(0, 100 - (unemployment * 10)));
    }
    
    private double normalizeCostOfLiving(double col) {
        return Math.min(100, Math.max(0, 100 - ((col - 50) * (100.0 / 150.0))));
    }
    
    private double normalizeAqi(double aqi) {
        return Math.min(100, Math.max(0, 100 - (aqi / 2)));
    }
    
    private double normalizePopulation(long pop) {
        return Math.min(100, Math.max(0, (pop / 10_000_000.0) * 100));
    }
    
    private double normalizeGreenSpace(double density) {
        return Math.min(100, Math.max(0, 100 - (density / 100)));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TIER HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String getScoreTier(Double score) {
        if (score == null) return "unavailable";
        if (score >= EXCELLENT_THRESHOLD) return "excellent";
        if (score >= GOOD_THRESHOLD) return "good";
        if (score >= AVERAGE_THRESHOLD) return "average";
        if (score >= BELOW_AVERAGE_THRESHOLD) return "below-average";
        return "poor";
    }
    
    private String getMagnitude(Double score) {
        if (score == null) return "low";
        double deviation = Math.abs(score - 50);
        if (deviation >= 30) return "high";
        if (deviation >= 15) return "medium";
        return "low";
    }
    
    private String getGdpTier(double gdp) {
        if (gdp >= 80000) return "highly prosperous";
        if (gdp >= HIGH_GDP_THRESHOLD) return "prosperous";
        if (gdp >= 40000) return "moderate";
        if (gdp >= 20000) return "developing";
        return "low-income";
    }
    
    private String getUnemploymentTier(double rate) {
        if (rate <= 3.0) return "excellent";
        if (rate <= LOW_UNEMPLOYMENT_THRESHOLD) return "healthy";
        if (rate <= 6.0) return "moderate";
        if (rate <= 8.0) return "concerning";
        return "high";
    }
    
    private String getCostOfLivingTier(double col) {
        if (col <= 80) return "very affordable";
        if (col <= 100) return "affordable";
        if (col <= HIGH_COST_THRESHOLD) return "moderate";
        if (col <= 150) return "expensive";
        return "very expensive";
    }
    
    private String getAqiTier(double aqi) {
        if (aqi <= GOOD_AQI_THRESHOLD) return "good";
        if (aqi <= 100) return "moderate";
        if (aqi <= 150) return "unhealthy for sensitive groups";
        if (aqi <= 200) return "unhealthy";
        return "very unhealthy";
    }
    
    private String formatPopulation(long pop) {
        if (pop >= 1_000_000) {
            return String.format("%.1fM", pop / 1_000_000.0);
        } else if (pop >= 1_000) {
            return String.format("%.0fK", pop / 1_000.0);
        }
        return String.valueOf(pop);
    }
}
