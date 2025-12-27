package com.cityatlas.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cityatlas.backend.entity.AnalyticsEvent;
import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.entity.EventType;

/**
 * Analytics Event Repository
 * 
 * Data access layer for AnalyticsEvent entity.
 * Optimized for high-volume event tracking and analytics queries.
 */
@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {
    
    /**
     * Find all events for a specific city
     * 
     * @param city City entity
     * @return List of events
     */
    List<AnalyticsEvent> findByCity(City city);
    
    /**
     * Find events by type
     * 
     * @param eventType Type of event
     * @return List of events
     */
    List<AnalyticsEvent> findByEventType(EventType eventType);
    
    /**
     * Find events for a city by type
     * 
     * @param city City entity
     * @param eventType Type of event
     * @return List of events
     */
    List<AnalyticsEvent> findByCityAndEventType(City city, EventType eventType);
    
    /**
     * Find events within a date range
     * Critical for analytics reporting
     * 
     * @param startDate Start of range
     * @param endDate End of range
     * @return List of events in range
     */
    List<AnalyticsEvent> findByEventTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find events for a city within date range
     * 
     * @param city City entity
     * @param startDate Start of range
     * @param endDate End of range
     * @return List of events
     */
    List<AnalyticsEvent> findByCityAndEventTimestampBetween(
        City city, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    /**
     * Find events by city, type, and date range
     * Most common analytics query pattern
     * 
     * @param city City entity
     * @param eventType Type of event
     * @param startDate Start of range
     * @param endDate End of range
     * @return List of events
     */
    List<AnalyticsEvent> findByCityAndEventTypeAndEventTimestampBetween(
        City city, 
        EventType eventType, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    /**
     * Find events by type within date range
     * 
     * @param eventType Type of event
     * @param startDate Start of range
     * @param endDate End of range
     * @return List of events
     */
    List<AnalyticsEvent> findByEventTypeAndEventTimestampBetween(
        EventType eventType, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    /**
     * Find events by user ID
     * 
     * @param userId User identifier
     * @return List of user events
     */
    List<AnalyticsEvent> findByUserId(String userId);
    
    /**
     * Find events by session ID
     * 
     * @param sessionId Session identifier
     * @return List of session events
     */
    List<AnalyticsEvent> findBySessionId(String sessionId);
    
    /**
     * Count events for a city
     * 
     * @param city City entity
     * @return Event count
     */
    long countByCity(City city);
    
    /**
     * Count events by type for a city
     * 
     * @param city City entity
     * @param eventType Type of event
     * @return Event count
     */
    long countByCityAndEventType(City city, EventType eventType);
    
    /**
     * Count events within date range
     * 
     * @param startDate Start of range
     * @param endDate End of range
     * @return Event count
     */
    long countByEventTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get event count by type within date range (aggregation)
     * 
     * @param eventType Type of event
     * @param startDate Start of range
     * @param endDate End of range
     * @return Event count
     */
    @Query("SELECT COUNT(e) FROM AnalyticsEvent e WHERE e.eventType = :eventType " +
           "AND e.eventTimestamp BETWEEN :startDate AND :endDate")
    long countEventsByTypeInDateRange(
        @Param("eventType") EventType eventType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Get top cities by event count within date range
     * Analytics query for popular cities
     * 
     * @param startDate Start of range
     * @param endDate End of range
     * @param limit Number of cities to return
     * @return List of cities with event counts
     */
    @Query("SELECT e.city, COUNT(e) as eventCount FROM AnalyticsEvent e " +
           "WHERE e.city IS NOT NULL " +
           "AND e.eventTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY e.city " +
           "ORDER BY eventCount DESC " +
           "LIMIT :limit")
    List<Object[]> findTopCitiesByEventCount(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("limit") int limit
    );
    
    /**
     * Delete old events (for cleanup/archiving)
     * 
     * @param cutoffDate Delete events before this date
     */
    void deleteByEventTimestampBefore(LocalDateTime cutoffDate);
}
