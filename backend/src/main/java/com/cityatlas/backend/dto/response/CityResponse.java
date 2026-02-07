package com.cityatlas.backend.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * City Response DTO
 * 
 * Represents city data returned to the frontend.
 * Decouples API contract from database entity structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityResponse {
    
    /**
     * Unique identifier for the city
     */
    private Long id;
    
    /**
     * URL-friendly city identifier (e.g., "san-francisco")
     */
    private String slug;
    
    /**
     * Display name of the city
     */
    private String name;
    
    /**
     * State/Province name (optional)
     */
    private String state;
    
    /**
     * Country name
     */
    private String country;
    
    /**
     * Population count
     */
    private Long population;
    
    /**
     * GDP per capita in USD
     */
    private Double gdpPerCapita;
    
    /**
     * Latitude coordinate
     */
    private Double latitude;
    
    /**
     * Longitude coordinate
     */
    private Double longitude;
    
    /**
     * Cost of living index (100 = national average)
     */
    private Integer costOfLivingIndex;
    
    /**
     * Unemployment rate as percentage
     */
    private Double unemploymentRate;
    
    /**
     * Banner/hero image URL (optional)
     */
    private String bannerImageUrl;
    
    /**
     * Brief description/tagline
     */
    private String description;
    
    /**
     * Country-level literacy rate (%) from World Bank
     */
    private Double literacyRate;
    
    /**
     * Pupil-to-teacher ratio from World Bank
     */
    private Double pupilTeacherRatio;
    
    /**
     * Renewable energy as % of total consumption from World Bank
     */
    private Double renewableEnergyPct;
    
    /**
     * CO2 emissions per capita (metric tons) from World Bank
     */
    private Double co2PerCapita;
    
    /**
     * Languages spoken in the country from REST Countries API
     */
    private java.util.List<String> languages;
    
    /**
     * ISO country code (alpha-2)
     */
    private String countryCode;

    // ==========================================
    // Health & Safety — World Bank (country-level)
    // ==========================================

    /**
     * Hospital beds per 1,000 people — World Bank SH.MED.BEDS.ZS
     */
    private Double hospitalBedsPer1000;

    /**
     * Current health expenditure per capita (USD) — World Bank SH.XPD.CHEX.PC.CD
     */
    private Double healthExpenditurePerCapita;

    /**
     * Life expectancy at birth (years) — World Bank SP.DYN.LE00.IN
     */
    private Double lifeExpectancy;

    // ==========================================
    // Infrastructure & Connectivity — World Bank (country-level)
    // ==========================================

    /**
     * Internet users (% of population) — World Bank IT.NET.USER.ZS
     */
    private Double internetUsersPct;

    /**
     * Mobile cellular subscriptions per 100 people — World Bank IT.CEL.SETS.P2
     */
    private Double mobileSubscriptionsPer100;

    /**
     * Access to electricity (% of population) — World Bank EG.ELC.ACCS.ZS
     */
    private Double electricityAccessPct;

    // ==========================================
    // Live Weather — OpenWeatherMap (city-level, if API key configured)
    // ==========================================

    /**
     * Current temperature in Celsius
     */
    private Double weatherTemp;

    /**
     * Weather description (e.g., "clear sky", "light rain")
     */
    private String weatherDescription;

    /**
     * Weather icon code (for display)
     */
    private String weatherIcon;

    /**
     * Humidity percentage
     */
    private Integer weatherHumidity;

    /**
     * Wind speed in m/s
     */
    private Double weatherWindSpeed;

    // ==========================================
    // Live Air Quality — OpenAQ (city-level, if available)
    // ==========================================

    /**
     * Air Quality Index (0-500+)
     */
    private Integer airQualityIndex;

    /**
     * AQI category (Good, Moderate, Unhealthy, etc.)
     */
    private String airQualityCategory;

    /**
     * PM2.5 concentration (μg/m³)
     */
    private Double pm25;

    /**
     * Timestamp of last data update
     */
    private LocalDateTime lastUpdated;
}
