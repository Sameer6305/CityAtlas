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
     * Timestamp of last data update
     */
    private LocalDateTime lastUpdated;
}
