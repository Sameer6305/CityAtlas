package com.cityatlas.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cityatlas.backend.dto.response.AiCitySummaryDTO;
import com.cityatlas.backend.entity.City;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI City Summary Service
 * 
 * Generates explainable, rule-based city personality summaries and insights.
 * 
 * This is NOT machine learning or external AI - it uses transparent, human-readable
 * logic to analyze city data and produce insights. All rules are documented with
 * clear reasoning for why they exist.
 * 
 * Design Philosophy:
 * - Explainability: Every decision can be traced to a specific rule
 * - Maintainability: Logic is simple enough for any developer to modify
 * - Transparency: No black boxes - all reasoning is visible in code
 * 
 * Data Sources Used:
 * - City metrics: population, GDP, unemployment, cost of living
 * - Environmental data: AQI (Air Quality Index)
 * - User engagement: analytics event counts (popularity indicators)
 * 
 * @see AiCitySummaryDTO
 * @see City
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiCitySummaryService {
    
    // ============================================
    // THRESHOLDS AND CONSTANTS
    // Explanation: These define what "high" or "low" means
    // ============================================
    
    /** Population threshold for classifying as a "major city" (1 million+) */
    private static final long MAJOR_CITY_POPULATION = 1_000_000;
    
    /** Population threshold for classifying as a "mid-sized city" (250k+) */
    private static final long MIDSIZED_CITY_POPULATION = 250_000;
    
    /** Cost of living index threshold for "expensive" (>120 = 20% above national avg) */
    private static final int HIGH_COST_THRESHOLD = 120;
    
    /** Cost of living index threshold for "affordable" (<90 = 10% below national avg) */
    private static final int LOW_COST_THRESHOLD = 90;
    
    /** Unemployment rate threshold for "healthy job market" (<4.5%) */
    private static final double LOW_UNEMPLOYMENT_THRESHOLD = 4.5;
    
    /** Unemployment rate threshold for "struggling job market" (>7%) */
    private static final double HIGH_UNEMPLOYMENT_THRESHOLD = 7.0;
    
    /** AQI threshold for "good air quality" (<50 = EPA "Good" category) */
    private static final int GOOD_AQI_THRESHOLD = 50;
    
    /** AQI threshold for "concerning air quality" (>100 = EPA "Unhealthy for Sensitive Groups") */
    private static final int POOR_AQI_THRESHOLD = 100;
    
    /** GDP per capita threshold for "wealthy" ($60k+ suggests strong economy) */
    private static final double HIGH_GDP_THRESHOLD = 60_000;
    
    /** GDP per capita threshold for "struggling economy" (<$30k suggests economic challenges) */
    private static final double LOW_GDP_THRESHOLD = 30_000;
    
    
    // ============================================
    // MAIN GENERATION METHOD
    // ============================================
    
    /**
     * Generate AI city summary with personality, strengths, weaknesses, and audience fit
     * 
     * @param city The city entity with basic info (population, GDP, etc.)
     * @param currentAqi Current air quality index (0-500 scale)
     * @param popularityScore Relative popularity based on analytics events (0-100)
     * @return AiCitySummaryDTO with generated insights
     */
    public AiCitySummaryDTO generateSummary(City city, Integer currentAqi, Integer popularityScore) {
        log.info("Generating AI summary for city: {}", city.getSlug());
        
        // Log data availability for transparency and debugging
        logDataAvailability(city, currentAqi, popularityScore);
        
        return AiCitySummaryDTO.builder()
                .personality(generatePersonality(city, currentAqi, popularityScore))
                .strengths(generateStrengths(city, currentAqi, popularityScore))
                .weaknesses(generateWeaknesses(city, currentAqi))
                .bestSuitedFor(generateBestSuitedFor(city, currentAqi))
                .build();
    }
    
    
    // ============================================
    // PERSONALITY GENERATION
    // Rule: Combine size, economy, and quality of life into narrative
    // ============================================
    
    /**
     * Generate personality paragraph describing the city's character
     * 
     * Logic:
     * 1. Determine city size category (affects culture and opportunities)
     * 2. Assess economic health (job market + GDP)
     * 3. Evaluate quality of life factors (cost, air quality)
     * 4. Combine into coherent narrative
     * 
     * Why: Personality should reflect tangible characteristics that affect daily life
     */
    private String generatePersonality(City city, Integer currentAqi, Integer popularityScore) {
        StringBuilder personality = new StringBuilder();
        
        // Size-based personality traits
        // Why: City size fundamentally affects culture, opportunities, and lifestyle
        if (city.getPopulation() > MAJOR_CITY_POPULATION) {
            personality.append("A major metropolitan hub ");
        } else if (city.getPopulation() > MIDSIZED_CITY_POPULATION) {
            personality.append("A mid-sized city ");
        } else {
            personality.append("A smaller city ");
        }
        
        // Economic character
        // Why: Economic health is the #1 factor in livability and opportunity
        if (city.getGdpPerCapita() != null && city.getGdpPerCapita() > HIGH_GDP_THRESHOLD) {
            personality.append("with a thriving, prosperous economy. ");
        } else if (city.getGdpPerCapita() != null && city.getGdpPerCapita() < LOW_GDP_THRESHOLD) {
            personality.append("facing economic challenges but with growth potential. ");
        } else if (city.getGdpPerCapita() != null) {
            personality.append("with a stable, middle-class economy. ");
        } else {
            // Fallback when GDP data is unavailable
            log.debug("GDP data unavailable for {}, using neutral economic description", city.getSlug());
            personality.append("with an evolving economic landscape. ");
        }
        
        // Job market context
        // Why: Employment opportunities drive migration and quality of life
        if (city.getUnemploymentRate() != null && city.getUnemploymentRate() < LOW_UNEMPLOYMENT_THRESHOLD) {
            personality.append("The job market is strong with diverse opportunities. ");
        } else if (city.getUnemploymentRate() != null && city.getUnemploymentRate() > HIGH_UNEMPLOYMENT_THRESHOLD) {
            personality.append("The job market is competitive with limited openings. ");
        } else if (city.getUnemploymentRate() == null) {
            // Fallback when unemployment data is unavailable
            log.debug("Unemployment data unavailable for {}, skipping job market assessment", city.getSlug());
        }
        
        // Quality of life summary
        // Why: Cost and environment are daily concerns for residents
        if (currentAqi != null && currentAqi < GOOD_AQI_THRESHOLD) {
            personality.append("Residents enjoy clean air and a healthy environment");
        } else if (currentAqi != null && currentAqi > POOR_AQI_THRESHOLD) {
            personality.append("Air quality is a concern for health-conscious residents");
        } else if (currentAqi != null) {
            personality.append("Environmental conditions are typical for an urban area");
        } else {
            // Fallback when AQI data is unavailable
            log.debug("AQI data unavailable for {}, using generic environmental description", city.getSlug());
            personality.append("Insufficient data to assess air quality impact");
        }
        
        // Cost of living context
        // Why: Affordability is often the deciding factor for relocation
        if (city.getCostOfLivingIndex() != null && city.getCostOfLivingIndex() > HIGH_COST_THRESHOLD) {
            personality.append(", though the high cost of living requires significant income.");
        } else if (city.getCostOfLivingIndex() != null && city.getCostOfLivingIndex() < LOW_COST_THRESHOLD) {
            personality.append(", with an affordable cost of living that stretches your dollar.");
        } else if (city.getCostOfLivingIndex() != null) {
            personality.append(", with costs in line with national averages.");
        } else {
            // Fallback when cost of living data is unavailable
            log.debug("Cost of living data unavailable for {}", city.getSlug());
            personality.append(", though cost of living data is currently unavailable.");
        }
        
        return personality.toString();
    }
    
    
    // ============================================
    // STRENGTHS GENERATION
    // Rule: Identify 3-5 positive attributes based on metrics
    // ============================================
    
    /**
     * Generate list of city strengths
     * 
     * Logic: Check each metric against thresholds and add corresponding strength
     * 
     * Why strengths matter: Helps users understand what makes this city attractive
     * and what advantages they'd gain by living there
     */
    private List<String> generateStrengths(City city, Integer currentAqi, Integer popularityScore) {
        List<String> strengths = new ArrayList<>();
        
        // Economic strengths
        // Why: High GDP = more resources for services, infrastructure, and quality of life
        if (city.getGdpPerCapita() != null && city.getGdpPerCapita() > HIGH_GDP_THRESHOLD) {
            strengths.add("Strong, prosperous economy");
        }
        
        // Job market strengths
        // Why: Low unemployment = job security and career opportunities
        if (city.getUnemploymentRate() != null && city.getUnemploymentRate() < LOW_UNEMPLOYMENT_THRESHOLD) {
            strengths.add("Healthy job market with low unemployment");
        }
        
        // Environmental strengths
        // Why: Clean air = better health outcomes and quality of life
        if (currentAqi != null && currentAqi < GOOD_AQI_THRESHOLD) {
            strengths.add("Excellent air quality");
        }
        
        // Affordability strengths
        // Why: Lower costs = higher purchasing power and savings potential
        if (city.getCostOfLivingIndex() != null && city.getCostOfLivingIndex() < LOW_COST_THRESHOLD) {
            strengths.add("Affordable cost of living");
        }
        
        // Size-based strengths
        // Why: Major cities offer unique advantages (culture, diversity, infrastructure)
        if (city.getPopulation() > MAJOR_CITY_POPULATION) {
            strengths.add("Major metropolitan amenities and infrastructure");
        }
        
        // Popularity as a strength indicator
        // Why: High engagement suggests residents are satisfied and actively engaged
        if (popularityScore != null && popularityScore > 70) {
            strengths.add("Growing popularity among professionals");
        }
        
        // Fallback: Every city should have at least one strength
        // Why: Positive framing helps users see opportunities
        if (strengths.isEmpty()) {
            log.info("No specific strengths identified for {} due to limited data, using generic strength", city.getSlug());
            strengths.add("Tight-knit community atmosphere");
            strengths.add("Potential for growth and development");
        }
        
        return strengths;
    }
    
    
    // ============================================
    // WEAKNESSES GENERATION
    // Rule: Identify 2-4 challenges or concerns
    // ============================================
    
    /**
     * Generate list of city weaknesses
     * 
     * Logic: Inverse of strengths - identify metrics that fall below thresholds
     * 
     * Why weaknesses matter: Honest assessment helps users make informed decisions
     * and sets realistic expectations
     */
    private List<String> generateWeaknesses(City city, Integer currentAqi) {
        List<String> weaknesses = new ArrayList<>();
        
        // Cost of living challenges
        // Why: High costs are the #1 barrier to entry for most people
        if (city.getCostOfLivingIndex() != null && city.getCostOfLivingIndex() > HIGH_COST_THRESHOLD) {
            weaknesses.add("High cost of living");
        }
        
        // Job market challenges
        // Why: High unemployment = fewer opportunities and career uncertainty
        if (city.getUnemploymentRate() != null && city.getUnemploymentRate() > HIGH_UNEMPLOYMENT_THRESHOLD) {
            weaknesses.add("Competitive job market with higher unemployment");
        }
        
        // Environmental challenges
        // Why: Poor air quality = health risks, especially for children and elderly
        if (currentAqi != null && currentAqi > POOR_AQI_THRESHOLD) {
            weaknesses.add("Air quality concerns");
        }
        
        // Economic challenges
        // Why: Low GDP = fewer public services, aging infrastructure
        if (city.getGdpPerCapita() != null && city.getGdpPerCapita() < LOW_GDP_THRESHOLD) {
            weaknesses.add("Economic challenges and limited growth");
        }
        
        // Size-based challenges
        // Why: Smaller cities often lack diversity of opportunities and services
        if (city.getPopulation() < MIDSIZED_CITY_POPULATION) {
            weaknesses.add("Limited cultural and entertainment options");
        }
        
        // Fallback: Be honest but constructive
        // Why: Every place has trade-offs - acknowledge them
        if (weaknesses.isEmpty()) {
            log.info("No specific weaknesses identified for {} due to limited data, using generic weakness", city.getSlug());
            weaknesses.add("Insufficient data to identify specific challenges");
        }
        
        return weaknesses;
    }
    
    
    // ============================================
    // BEST SUITED FOR GENERATION
    // Rule: Match city characteristics to ideal resident profiles
    // ============================================
    
    /**
     * Generate list of ideal resident profiles
     * 
     * Logic: Match city's strengths/weaknesses to demographic preferences
     * 
     * Why this matters: Helps users quickly identify if this city aligns with
     * their lifestyle, career stage, and values
     */
    private List<String> generateBestSuitedFor(City city, Integer currentAqi) {
        List<String> bestSuitedFor = new ArrayList<>();
        
        // High earners who can afford expensive cities
        // Why: They won't be priced out and can enjoy the premium amenities
        if (city.getCostOfLivingIndex() != null && city.getCostOfLivingIndex() > HIGH_COST_THRESHOLD 
                && city.getGdpPerCapita() != null && city.getGdpPerCapita() > HIGH_GDP_THRESHOLD) {
            bestSuitedFor.add("High-earning professionals and executives");
        }
        
        // Job seekers drawn to strong markets
        // Why: Low unemployment = easier to find work and negotiate salary
        if (city.getUnemploymentRate() != null && city.getUnemploymentRate() < LOW_UNEMPLOYMENT_THRESHOLD) {
            bestSuitedFor.add("Career-focused individuals seeking job opportunities");
        }
        
        // Budget-conscious residents
        // Why: Affordable cities allow for saving and better quality of life
        if (city.getCostOfLivingIndex() != null && city.getCostOfLivingIndex() < LOW_COST_THRESHOLD) {
            bestSuitedFor.add("Budget-conscious families and retirees");
        }
        
        // Health-conscious and families with children
        // Why: Clean air reduces health risks and medical costs
        if (currentAqi != null && currentAqi < GOOD_AQI_THRESHOLD) {
            bestSuitedFor.add("Families with children and health-conscious individuals");
        }
        
        // Urban enthusiasts
        // Why: Major cities offer unmatched diversity, culture, and networking
        if (city.getPopulation() > MAJOR_CITY_POPULATION) {
            bestSuitedFor.add("Urban enthusiasts seeking diversity and cultural experiences");
        }
        
        // Those seeking community
        // Why: Smaller cities offer stronger social bonds and community identity
        if (city.getPopulation() < MIDSIZED_CITY_POPULATION) {
            bestSuitedFor.add("Those seeking tight-knit community and slower pace");
        }
        
        // Entrepreneurs in strong economies
        // Why: High GDP = more customers with disposable income
        if (city.getGdpPerCapita() != null && city.getGdpPerCapita() > HIGH_GDP_THRESHOLD) {
            bestSuitedFor.add("Entrepreneurs and small business owners");
        }
        
        // Fallback: Universal audience
        // Why: Some cities appeal broadly without standout characteristics
        if (bestSuitedFor.isEmpty()) {
            log.info("No specific audience match for {} due to limited data, using universal profiles", city.getSlug());
            bestSuitedFor.add("Those seeking a balanced, middle-American lifestyle");
            bestSuitedFor.add("Remote workers with location flexibility");
        }
        
        return bestSuitedFor;
    }
    
    
    // ============================================
    // DATA AVAILABILITY LOGGING
    // ============================================
    
    /**
     * Log data availability for transparency and debugging
     * 
     * This helps developers understand why certain insights are missing
     * and helps with troubleshooting data pipeline issues
     * 
     * @param city The city entity
     * @param currentAqi Current AQI value (can be null)
     * @param popularityScore Popularity score (can be null)
     */
    private void logDataAvailability(City city, Integer currentAqi, Integer popularityScore) {
        log.debug("Data availability for {}: GDP={}, Unemployment={}, CostOfLiving={}, AQI={}, Popularity={}, Population={}",
                city.getSlug(),
                city.getGdpPerCapita() != null ? "available" : "MISSING",
                city.getUnemploymentRate() != null ? "available" : "MISSING",
                city.getCostOfLivingIndex() != null ? "available" : "MISSING",
                currentAqi != null ? "available" : "MISSING",
                popularityScore != null ? "available" : "MISSING",
                city.getPopulation() != null ? "available" : "MISSING");
        
        // Warn if critical data is missing
        int missingDataCount = 0;
        if (city.getGdpPerCapita() == null) missingDataCount++;
        if (city.getUnemploymentRate() == null) missingDataCount++;
        if (city.getCostOfLivingIndex() == null) missingDataCount++;
        if (currentAqi == null) missingDataCount++;
        
        if (missingDataCount >= 3) {
            log.warn("City {} has {} missing critical metrics - AI summary quality may be reduced",
                    city.getSlug(), missingDataCount);
        }
    }
}
