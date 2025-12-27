package com.cityatlas.backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Analytics Event Entity
 * 
 * Tracks events for analytics, monitoring, and user behavior analysis.
 * High-volume table optimized for time-series queries.
 * 
 * Indexes:
 * - city_id + event_type + timestamp: Primary composite for city-specific event queries
 * - city_id: For retrieving all events for a city (analytics dashboard)
 * - event_type + timestamp: For cross-city event analysis and trending
 * - timestamp: For temporal queries and cleanup operations
 * - user_id: For user behavior tracking and analytics
 * - session_id: For session analysis and user journey tracking
 * 
 * Note: Consider partitioning by timestamp for large-scale deployments (>100M rows)
 */
@Entity
@Table(name = "analytics_events", indexes = {
    @Index(name = "idx_event_city_type_time", columnList = "city_id, event_type, event_timestamp"),
    @Index(name = "idx_event_city_id", columnList = "city_id"),
    @Index(name = "idx_event_type_time", columnList = "event_type, event_timestamp"),
    @Index(name = "idx_event_timestamp", columnList = "event_timestamp"),
    @Index(name = "idx_event_user_id", columnList = "user_id"),
    @Index(name = "idx_event_session_id", columnList = "session_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "city")
public class AnalyticsEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Associated city (optional - some events are system-wide)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;
    
    /**
     * Type of event
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;
    
    /**
     * Event value (numeric metric or count)
     * Examples: view count, duration, error code
     */
    @Column(name = "event_value")
    private Double value;
    
    /**
     * Additional event metadata (JSON format)
     * Examples: user agent, IP address, request parameters
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * User identifier (optional, for user-specific tracking)
     */
    @Column(name = "user_id", length = 100)
    private String userId;
    
    /**
     * Session identifier (for grouping related events)
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    /**
     * Timestamp when event occurred
     * Critical for time-series analysis
     */
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;
    
    /**
     * Timestamp when record was created in database
     * May differ from eventTimestamp if events are queued
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
