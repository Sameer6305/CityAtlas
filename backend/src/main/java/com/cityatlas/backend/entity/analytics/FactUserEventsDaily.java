package com.cityatlas.backend.entity.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.cityatlas.backend.entity.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * FACT_USER_EVENTS_DAILY - Daily Aggregated User Events Fact Table
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * FACT TABLE ROLE:
 * This is a pre-aggregated fact table that rolls up raw analytics_events
 * into daily summaries. This enables fast dashboard queries without scanning
 * millions of individual event rows.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * AGGREGATION STRATEGY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * RAW EVENTS (OLTP)                    AGGREGATED FACTS (OLAP)
 * ─────────────────                    ───────────────────────
 * 
 * analytics_events                     fact_user_events_daily
 * ┌────────────────────────────┐      ┌────────────────────────────────┐
 * │ id │ city │ type │ time   │      │ city │ type │ date │ count    │
 * ├────┼──────┼──────┼────────┤      ├──────┼──────┼──────┼──────────┤
 * │ 1  │ NYC  │ VIEW │ 10:01  │      │ NYC  │ VIEW │ 01-01│ 3 events │
 * │ 2  │ NYC  │ VIEW │ 10:15  │  ──▶ │      │      │      │ 2 users  │
 * │ 3  │ NYC  │ VIEW │ 14:30  │      │      │      │      │ 2 sessions│
 * │ 4  │ NYC  │ SRCH │ 10:02  │      │ NYC  │ SRCH │ 01-01│ 1 event  │
 * │ 5  │ LA   │ VIEW │ 11:00  │      │ LA   │ VIEW │ 01-01│ 1 event  │
 * └────────────────────────────┘      └────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY PRE-AGGREGATE?
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. QUERY PERFORMANCE
 *    ──────────────────
 *    Without aggregation (scanning raw events):
 *      SELECT city_id, COUNT(*) 
 *      FROM analytics_events 
 *      WHERE event_timestamp >= '2026-01-01'
 *      GROUP BY city_id
 *      -- Scans 10 million rows = 30 seconds
 *    
 *    With pre-aggregation:
 *      SELECT dim_city_id, SUM(event_count)
 *      FROM fact_user_events_daily
 *      WHERE event_date >= '2026-01-01'
 *      GROUP BY dim_city_id
 *      -- Scans 365 rows per city = 50 milliseconds
 * 
 * 2. STORAGE EFFICIENCY
 *    ──────────────────
 *    Raw events: 10 million rows × 200 bytes = 2 GB
 *    Daily aggregates: 365 days × 100 cities × 10 types × 100 bytes = 36 MB
 *    
 *    Compression ratio: ~55:1
 * 
 * 3. CONSISTENT METRICS
 *    ──────────────────
 *    Pre-aggregation ensures all queries use the same counting logic.
 *    No risk of different dashboards showing different numbers.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * GRAIN DEFINITION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * GRAIN: One row per city per event type per day
 * 
 *   dim_city_id + event_type + event_date = UNIQUE ROW
 * 
 * This grain allows:
 * - Daily trends per city
 * - Event type comparisons
 * - Cross-city analysis
 * - Weekly/monthly roll-ups via SUM()
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * MEASURE TYPES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────────────────┬────────────────────────────────────────────────┐
 *   │ Measure                 │ Aggregation Function                           │
 *   ├─────────────────────────┼────────────────────────────────────────────────┤
 *   │ event_count             │ COUNT(*) - Total events                        │
 *   │ unique_users            │ COUNT(DISTINCT user_id) - Unique users         │
 *   │ unique_sessions         │ COUNT(DISTINCT session_id) - Unique sessions   │
 *   │ total_duration_seconds  │ SUM(duration) - For time_spent events          │
 *   │ avg_duration_seconds    │ AVG(duration) - Average engagement             │
 *   │ max_duration_seconds    │ MAX(duration) - Longest session                │
 *   │ bounce_count            │ COUNT where duration < threshold               │
 *   └─────────────────────────┴────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * ETL AGGREGATION QUERY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This fact table is populated by a nightly ETL job running:
 * 
 *   INSERT INTO fact_user_events_daily (
 *       dim_city_id, event_type, event_date,
 *       event_count, unique_users, unique_sessions,
 *       total_duration_seconds, avg_duration_seconds
 *   )
 *   SELECT 
 *       dc.id as dim_city_id,
 *       ae.event_type,
 *       DATE(ae.event_timestamp) as event_date,
 *       COUNT(*) as event_count,
 *       COUNT(DISTINCT ae.user_id) as unique_users,
 *       COUNT(DISTINCT ae.session_id) as unique_sessions,
 *       SUM(CAST(ae.metadata->>'duration' AS NUMERIC)) as total_duration,
 *       AVG(CAST(ae.metadata->>'duration' AS NUMERIC)) as avg_duration
 *   FROM analytics_events ae
 *   JOIN dim_city dc ON dc.source_city_id = ae.city_id AND dc.is_current = true
 *   WHERE DATE(ae.event_timestamp) = CURRENT_DATE - INTERVAL '1 day'
 *   GROUP BY dc.id, ae.event_type, DATE(ae.event_timestamp);
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * EXAMPLE ANALYTICS QUERIES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * -- 1. Top 10 most viewed cities this month
 * SELECT dc.city_name, SUM(f.event_count) as total_views
 * FROM fact_user_events_daily f
 * JOIN dim_city dc ON f.dim_city_id = dc.id AND dc.is_current = true
 * WHERE f.event_type = 'CITY_VIEW'
 *   AND f.event_date >= DATE_TRUNC('month', CURRENT_DATE)
 * GROUP BY dc.city_name
 * ORDER BY total_views DESC
 * LIMIT 10;
 * 
 * -- 2. Daily user engagement trend
 * SELECT f.event_date,
 *        SUM(f.event_count) as total_events,
 *        SUM(f.unique_users) as daily_active_users,
 *        SUM(f.total_duration_seconds) / 60.0 as total_minutes
 * FROM fact_user_events_daily f
 * WHERE f.event_date >= CURRENT_DATE - INTERVAL '30 days'
 * GROUP BY f.event_date
 * ORDER BY f.event_date;
 * 
 * -- 3. Bounce rate by city (views with duration < 10 seconds)
 * SELECT dc.city_name,
 *        SUM(f.bounce_count)::FLOAT / NULLIF(SUM(f.event_count), 0) as bounce_rate
 * FROM fact_user_events_daily f
 * JOIN dim_city dc ON f.dim_city_id = dc.id AND dc.is_current = true
 * WHERE f.event_type = 'CITY_VIEW'
 *   AND f.event_date >= CURRENT_DATE - INTERVAL '7 days'
 * GROUP BY dc.city_name
 * HAVING SUM(f.event_count) > 10
 * ORDER BY bounce_rate DESC;
 * 
 * -- 4. Week-over-week growth
 * WITH weekly AS (
 *     SELECT DATE_TRUNC('week', event_date) as week,
 *            SUM(unique_users) as weekly_users
 *     FROM fact_user_events_daily
 *     WHERE event_date >= CURRENT_DATE - INTERVAL '8 weeks'
 *     GROUP BY DATE_TRUNC('week', event_date)
 * )
 * SELECT week,
 *        weekly_users,
 *        LAG(weekly_users) OVER (ORDER BY week) as prev_week,
 *        (weekly_users - LAG(weekly_users) OVER (ORDER BY week))::FLOAT /
 *        NULLIF(LAG(weekly_users) OVER (ORDER BY week), 0) * 100 as growth_pct
 * FROM weekly
 * ORDER BY week;
 * 
 * @see DimCity
 * @see com.cityatlas.backend.entity.AnalyticsEvent
 */
@Entity
@Table(name = "fact_user_events_daily",
    indexes = {
        // ═══════════════════════════════════════════════════════════════════════
        // PRIMARY ACCESS PATTERNS
        // ═══════════════════════════════════════════════════════════════════════
        
        // City-specific event history
        // SELECT * FROM fact_user_events_daily WHERE dim_city_id = ? ORDER BY event_date
        @Index(name = "idx_fact_events_city_date", 
               columnList = "dim_city_id, event_date"),
        
        // Event type trends across all cities
        // SELECT SUM(event_count) FROM fact_user_events_daily WHERE event_type = ? GROUP BY event_date
        @Index(name = "idx_fact_events_type_date", 
               columnList = "event_type, event_date"),
        
        // Daily dashboard: all events for a date
        // SELECT * FROM fact_user_events_daily WHERE event_date = ?
        @Index(name = "idx_fact_events_date", 
               columnList = "event_date"),
        
        // ═══════════════════════════════════════════════════════════════════════
        // COMPOSITE PATTERNS
        // ═══════════════════════════════════════════════════════════════════════
        
        // Full grain lookup
        @Index(name = "idx_fact_events_city_type_date", 
               columnList = "dim_city_id, event_type, event_date"),
        
        // Engagement analysis
        @Index(name = "idx_fact_events_unique_users", 
               columnList = "event_date, unique_users")
    },
    uniqueConstraints = {
        // Enforce grain: one row per city per event type per day
        @UniqueConstraint(name = "uq_fact_events_grain",
                         columnNames = {"dim_city_id", "event_type", "event_date"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "dimCity")
public class FactUserEventsDaily {
    
    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIMENSION FOREIGN KEYS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Foreign key to dim_city dimension table.
     * 
     * Nullable for system-wide events not associated with a specific city
     * (e.g., homepage visits, global searches).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dim_city_id")
    private DimCity dimCity;
    
    /**
     * Date of the aggregated events.
     * 
     * All events from analytics_events with this DATE(event_timestamp)
     * are rolled up into this row.
     */
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEGENERATE DIMENSION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Type of event being aggregated.
     * 
     * Stored directly in fact table as a degenerate dimension
     * (no separate dimension table needed for event types).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COUNT MEASURES (Additive)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Total number of events of this type for this city on this date.
     * 
     * Aggregation: COUNT(*)
     * Fully additive - can be summed across all dimensions.
     */
    @Column(name = "event_count", nullable = false)
    private Long eventCount;
    
    /**
     * Number of distinct users who generated events.
     * 
     * Aggregation: COUNT(DISTINCT user_id)
     * 
     * IMPORTANT: Semi-additive measure!
     * - Can be compared across time (trend analysis)
     * - Cannot be summed across cities (would double-count users visiting multiple cities)
     * - For cross-city totals, re-aggregate from raw events
     */
    @Column(name = "unique_users", nullable = false)
    private Long uniqueUsers;
    
    /**
     * Number of distinct sessions.
     * 
     * Aggregation: COUNT(DISTINCT session_id)
     * 
     * Semi-additive like unique_users.
     */
    @Column(name = "unique_sessions", nullable = false)
    private Long uniqueSessions;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DURATION MEASURES (For TIME_SPENT events)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Total time spent across all events (in seconds).
     * 
     * Aggregation: SUM(duration)
     * Fully additive - can be summed across all dimensions.
     * 
     * Only populated for event types that track duration
     * (e.g., SECTION_VIEWED, TIME_SPENT).
     */
    @Column(name = "total_duration_seconds")
    private Long totalDurationSeconds;
    
    /**
     * Average time spent per event (in seconds).
     * 
     * Aggregation: AVG(duration)
     * 
     * Non-additive! Cannot be summed or averaged with other rows.
     * For combined averages, use: SUM(total_duration) / SUM(event_count)
     */
    @Column(name = "avg_duration_seconds")
    private Double avgDurationSeconds;
    
    /**
     * Maximum duration of any single event (in seconds).
     * 
     * Aggregation: MAX(duration)
     * 
     * Non-additive - useful for outlier detection.
     */
    @Column(name = "max_duration_seconds")
    private Long maxDurationSeconds;
    
    /**
     * Minimum duration of any single event (in seconds).
     * 
     * Aggregation: MIN(duration)
     * 
     * Useful for identifying quick bounces.
     */
    @Column(name = "min_duration_seconds")
    private Long minDurationSeconds;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENGAGEMENT QUALITY MEASURES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Number of "bounce" events (duration < 10 seconds).
     * 
     * Aggregation: COUNT(*) WHERE duration < 10
     * 
     * Pre-computed for quick bounce rate calculations:
     *   bounce_rate = bounce_count / event_count
     */
    @Column(name = "bounce_count")
    private Long bounceCount;
    
    /**
     * Number of "engaged" events (duration >= 60 seconds).
     * 
     * Aggregation: COUNT(*) WHERE duration >= 60
     * 
     * Pre-computed for engagement rate calculations:
     *   engagement_rate = engaged_count / event_count
     */
    @Column(name = "engaged_count")
    private Long engagedCount;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ETL METADATA
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Timestamp when this aggregated row was created by ETL.
     */
    @CreationTimestamp
    @Column(name = "etl_loaded_at", nullable = false, updatable = false)
    private LocalDateTime etlLoadedAt;
    
    /**
     * ETL batch identifier for debugging and auditing.
     */
    @Column(name = "etl_batch_id", length = 50)
    private String etlBatchId;
    
    /**
     * Number of raw events aggregated into this row.
     * 
     * Should equal event_count, but tracked separately
     * for ETL validation and debugging.
     */
    @Column(name = "raw_event_count")
    private Long rawEventCount;
}
