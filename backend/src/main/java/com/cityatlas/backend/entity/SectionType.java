package com.cityatlas.backend.entity;

/**
 * City Section Type Enum
 * 
 * Defines the different sections/categories of city information.
 * Matches the frontend tab navigation structure.
 */
public enum SectionType {
    /**
     * Overview section - general city information
     */
    OVERVIEW,
    
    /**
     * Economy section - job market, industries, GDP
     */
    ECONOMY,
    
    /**
     * Infrastructure section - transportation, utilities
     */
    INFRASTRUCTURE,
    
    /**
     * Environment section - air quality, sustainability
     */
    ENVIRONMENT,
    
    /**
     * Education section - schools, universities
     */
    EDUCATION,
    
    /**
     * Culture section - arts, entertainment, diversity
     */
    CULTURE,
    
    /**
     * Analytics section - comprehensive metrics and charts
     */
    ANALYTICS
}
