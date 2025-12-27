package com.cityatlas.backend.entity;

/**
 * Analytics Event Type Enum
 * 
 * Defines types of events tracked for analytics and monitoring.
 * Used for user behavior, data updates, and system events.
 */
public enum EventType {
    // User Interaction Events
    /**
     * User viewed city profile
     */
    CITY_VIEW,
    
    /**
     * User viewed a specific page or section
     */
    PAGE_VIEW,
    
    /**
     * User viewed analytics dashboard
     */
    ANALYTICS_VIEW,
    
    /**
     * User searched for cities
     */
    SEARCH,
    
    /**
     * User compared cities
     */
    COMPARISON,
    
    /**
     * User bookmarked a city
     */
    BOOKMARK,
    
    // Data Update Events
    /**
     * City data was refreshed from external source
     */
    DATA_SYNC,
    
    /**
     * Metrics were updated
     */
    METRICS_UPDATE,
    
    /**
     * AI summary was generated
     */
    AI_SUMMARY_GENERATED,
    
    // System Events
    /**
     * API request received
     */
    API_REQUEST,
    
    /**
     * API error occurred
     */
    API_ERROR,
    
    /**
     * Background job executed
     */
    BACKGROUND_JOB,
    
    /**
     * Cache invalidation
     */
    CACHE_INVALIDATION
}
