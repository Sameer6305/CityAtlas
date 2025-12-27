package com.cityatlas.backend.entity;

/**
 * Metric Type Enum
 * 
 * Defines the different types of metrics tracked for cities.
 * Used for categorizing time-series data and measurements.
 */
public enum MetricType {
    // Environmental Metrics
    /**
     * Air Quality Index (0-500 scale)
     */
    AQI,
    
    /**
     * Carbon emissions per capita
     */
    CARBON_EMISSIONS,
    
    /**
     * Water quality index
     */
    WATER_QUALITY,
    
    // Economic Metrics
    /**
     * Unemployment rate (percentage)
     */
    UNEMPLOYMENT_RATE,
    
    /**
     * GDP per capita (USD)
     */
    GDP_PER_CAPITA,
    
    /**
     * Cost of living index (100 = national average)
     */
    COST_OF_LIVING,
    
    /**
     * Average salary (USD)
     */
    AVERAGE_SALARY,
    
    // Demographic Metrics
    /**
     * Total population count
     */
    POPULATION,
    
    /**
     * Population growth rate (percentage)
     */
    POPULATION_GROWTH,
    
    /**
     * Median age (years)
     */
    MEDIAN_AGE,
    
    // Infrastructure Metrics
    /**
     * Public transit coverage (percentage)
     */
    TRANSIT_COVERAGE,
    
    /**
     * Internet speed (Mbps)
     */
    INTERNET_SPEED,
    
    /**
     * Housing affordability index
     */
    HOUSING_AFFORDABILITY,
    
    // Education Metrics
    /**
     * High school graduation rate (percentage)
     */
    GRADUATION_RATE,
    
    /**
     * Universities count
     */
    UNIVERSITIES_COUNT,
    
    // Safety Metrics
    /**
     * Crime rate per 100,000 residents
     */
    CRIME_RATE,
    
    /**
     * Safety index (0-100 scale)
     */
    SAFETY_INDEX
}
