package com.cityatlas.backend.config;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * KAFKA TOPICS CONFIGURATION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Centralized definition of all Kafka topic names used in CityAtlas.
 * Using constants ensures consistency across producers and consumers.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY KAFKA FOR CITYATLAS ANALYTICS?
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. DECOUPLING:
 *    - Frontend actions don't wait for analytics processing
 *    - Backend can scale analytics independently of API
 *    - New analytics consumers can be added without code changes
 * 
 * 2. RELIABILITY:
 *    - Events are durably stored on Kafka brokers
 *    - Survives application restarts and deployments
 *    - Can replay events for debugging or backfilling
 * 
 * 3. SCALABILITY:
 *    - Partitioning allows parallel processing
 *    - Consumer groups enable horizontal scaling
 *    - Handles traffic spikes without data loss
 * 
 * 4. ANALYTICS PIPELINE:
 *    - Single source of truth for user behavior
 *    - Supports multiple downstream consumers:
 *      • Real-time dashboards (Kafka Streams)
 *      • Data warehouse sync (Kafka Connect → BigQuery)
 *      • ML feature extraction (Spark Streaming)
 *      • Alerting systems (anomaly detection)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * TOPIC ARCHITECTURE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────────────────────────────────────────────────────────────────┐
 *   │                        KAFKA CLUSTER                                   │
 *   ├─────────────────────────────────────────────────────────────────────────┤
 *   │                                                                         │
 *   │  ┌─────────────────────────────────────────────────────────────────┐   │
 *   │  │  cityatlas.analytics.city-searched                              │   │
 *   │  │  ─────────────────────────────────────────────────────────────  │   │
 *   │  │  Event: User searches for cities                                │   │
 *   │  │  Partition Key: citySlug (or "global" if no city selected)     │   │
 *   │  │  Retention: 30 days                                             │   │
 *   │  │  Use Case: Search ranking, content gaps, SEO optimization      │   │
 *   │  └─────────────────────────────────────────────────────────────────┘   │
 *   │                                                                         │
 *   │  ┌─────────────────────────────────────────────────────────────────┐   │
 *   │  │  cityatlas.analytics.section-viewed                             │   │
 *   │  │  ─────────────────────────────────────────────────────────────  │   │
 *   │  │  Event: User views a city section (economy, culture, etc.)     │   │
 *   │  │  Partition Key: citySlug                                        │   │
 *   │  │  Retention: 30 days                                             │   │
 *   │  │  Use Case: Section popularity, content prioritization          │   │
 *   │  └─────────────────────────────────────────────────────────────────┘   │
 *   │                                                                         │
 *   │  ┌─────────────────────────────────────────────────────────────────┐   │
 *   │  │  cityatlas.analytics.time-spent-on-section                      │   │
 *   │  │  ─────────────────────────────────────────────────────────────  │   │
 *   │  │  Event: User leaves section (duration captured)                 │   │
 *   │  │  Partition Key: citySlug                                        │   │
 *   │  │  Retention: 90 days (longer for engagement analysis)           │   │
 *   │  │  Use Case: Content quality, engagement scoring, recommendations│   │
 *   │  └─────────────────────────────────────────────────────────────────┘   │
 *   │                                                                         │
 *   └─────────────────────────────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * TOPIC NAMING CONVENTION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Format: {application}.{domain}.{event-type}
 * 
 * Examples:
 * - cityatlas.analytics.city-searched
 * - cityatlas.analytics.section-viewed
 * - cityatlas.data.city-updated (future)
 * - cityatlas.ai.summary-requested (future)
 * 
 * Benefits:
 * - Easy to filter topics by domain in monitoring tools
 * - Clear ownership and purpose
 * - Supports ACLs (access control lists) by prefix
 */
public final class KafkaTopics {
    
    private KafkaTopics() {
        // Prevent instantiation - utility class
    }
    
    /**
     * Base prefix for all CityAtlas topics
     * Change this per environment (dev, staging, prod)
     */
    private static final String BASE_PREFIX = "cityatlas";
    
    // ============================================
    // ANALYTICS TOPICS
    // ============================================
    
    /**
     * Topic: City Search Events
     * 
     * Published when:
     * - User searches for cities using the search bar
     * - Search query is submitted (not on every keystroke)
     * 
     * Use cases:
     * - Track popular search terms
     * - Improve search relevance
     * - Identify gaps in city coverage
     * 
     * Payload: AnalyticsEventPayload with eventType=CITY_SEARCHED
     * Partitioning: By citySlug (if specific city clicked) or round-robin
     * Retention: 30 days
     */
    public static final String CITY_SEARCHED = BASE_PREFIX + ".analytics.city-searched";
    
    /**
     * Topic: Section View Events
     * 
     * Published when:
     * - User navigates to a specific city section (Economy, Environment, etc.)
     * - Section tab is clicked and content is loaded
     * 
     * Use cases:
     * - Track most popular sections per city
     * - Understand user interests and priorities
     * - Optimize content placement and prioritization
     * 
     * Payload: AnalyticsEventPayload with eventType=SECTION_VIEWED, section populated
     * Partitioning: By citySlug for ordered event processing per city
     * Retention: 30 days
     */
    public static final String SECTION_VIEWED = BASE_PREFIX + ".analytics.section-viewed";
    
    /**
     * Topic: Time Spent on Section
     * 
     * Published when:
     * - User leaves a section (navigates away, closes tab, switches tabs)
     * - Calculated on frontend and sent as single event per section visit
     * 
     * Use cases:
     * - Measure content engagement and quality
     * - Identify sections with low engagement (need improvement)
     * - Calculate average read time per section type
     * - A/B testing for content changes
     * 
     * Payload: AnalyticsEventPayload with eventType=TIME_SPENT_ON_SECTION, 
     *          section and durationInSeconds populated
     * Partitioning: By citySlug for session ordering
     * Retention: 90 days (valuable for long-term analysis)
     */
    public static final String TIME_SPENT_ON_SECTION = BASE_PREFIX + ".analytics.time-spent-on-section";
    
    // ============================================
    // FUTURE TOPICS (Commented for reference)
    // ============================================
    
    // Data pipeline topics (when integrating external data sources)
    // public static final String CITY_DATA_UPDATED = BASE_PREFIX + ".data.city-updated";
    // public static final String METRICS_SYNC = BASE_PREFIX + ".data.metrics-sync";
    
    // AI processing topics (for async AI summary generation)
    // public static final String AI_SUMMARY_REQUESTED = BASE_PREFIX + ".ai.summary-requested";
    // public static final String AI_SUMMARY_COMPLETED = BASE_PREFIX + ".ai.summary-completed";
    
    // User interaction topics
    // public static final String CITY_BOOKMARKED = BASE_PREFIX + ".user.city-bookmarked";
    // public static final String CITY_COMPARED = BASE_PREFIX + ".user.city-compared";
    
    /**
     * Get all active topic names as an array
     * Useful for topic creation scripts and monitoring
     * 
     * @return Array of all active Kafka topics
     */
    public static String[] getAllTopics() {
        return new String[] {
            CITY_SEARCHED,
            SECTION_VIEWED,
            TIME_SPENT_ON_SECTION
        };
    }
    
    /**
     * Get topic configuration recommendations
     * These should be configured when creating topics in Kafka
     * 
     * @param topicName The topic to get config for
     * @return Human-readable configuration recommendations
     */
    public static String getTopicConfig(String topicName) {
        return switch (topicName) {
            case CITY_SEARCHED -> """
                Partitions: 3
                Replication Factor: 2
                Retention: 30 days
                Partitioning Strategy: Round-robin (or by citySlug if available)
                """;
            case SECTION_VIEWED -> """
                Partitions: 3
                Replication Factor: 2
                Retention: 30 days
                Partitioning Strategy: By citySlug (maintains event order per city)
                """;
            case TIME_SPENT_ON_SECTION -> """
                Partitions: 3
                Replication Factor: 2
                Retention: 90 days (longer for engagement analysis)
                Partitioning Strategy: By citySlug (session ordering important)
                Compaction: None (time-series data)
                """;
            default -> "No configuration defined for topic: " + topicName;
        };
    }
}
