package com.cityatlas.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.entity.MetricType;
import com.cityatlas.backend.entity.Metrics;

/**
 * Metrics Repository
 * 
 * Data access layer for Metrics entity.
 * Optimized for time-series analytics queries.
 */
@Repository
public interface MetricsRepository extends JpaRepository<Metrics, Long> {
    
    /**
     * Find all metrics for a specific city
     * 
     * @param city City entity
     * @return List of metrics
     */
    List<Metrics> findByCity(City city);
    
    /**
     * Find metrics for a city of a specific type
     * 
     * @param city City entity
     * @param metricType Type of metric
     * @return List of metrics
     */
    List<Metrics> findByCityAndMetricType(City city, MetricType metricType);
    
    /**
     * Find metrics for a city within a date range
     * Critical for analytics dashboard time-series charts
     * 
     * @param city City entity
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of metrics in date range
     */
    List<Metrics> findByCityAndRecordedAtBetween(City city, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find metrics by city, type, and date range
     * Most common analytics query pattern
     * 
     * @param city City entity
     * @param metricType Type of metric
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of metrics matching criteria
     */
    List<Metrics> findByCityAndMetricTypeAndRecordedAtBetween(
        City city, 
        MetricType metricType, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    /**
     * Find metrics by type across all cities (for comparisons)
     * 
     * @param metricType Type of metric
     * @return List of metrics of that type
     */
    List<Metrics> findByMetricType(MetricType metricType);
    
    /**
     * Find metrics by type within date range (cross-city)
     * 
     * @param metricType Type of metric
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of metrics
     */
    List<Metrics> findByMetricTypeAndRecordedAtBetween(
        MetricType metricType, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    /**
     * Find latest metric for a city by type
     * Useful for "current value" displays
     * 
     * @param city City entity
     * @param metricType Type of metric
     * @return Optional containing latest metric
     */
    Optional<Metrics> findFirstByCityAndMetricTypeOrderByRecordedAtDesc(City city, MetricType metricType);
    
    /**
     * Find metrics recorded after a specific date
     * Useful for incremental updates
     * 
     * @param city City entity
     * @param recordedAt Cutoff date
     * @return List of recent metrics
     */
    List<Metrics> findByCityAndRecordedAtAfter(City city, LocalDateTime recordedAt);
    
    /**
     * Get average metric value for a city over time period
     * Analytics aggregation query
     * 
     * @param city City entity
     * @param metricType Type of metric
     * @param startDate Start of period
     * @param endDate End of period
     * @return Average value
     */
    @Query("SELECT AVG(m.value) FROM Metrics m WHERE m.city = :city " +
           "AND m.metricType = :metricType " +
           "AND m.recordedAt BETWEEN :startDate AND :endDate")
    Double getAverageMetricValue(
        @Param("city") City city,
        @Param("metricType") MetricType metricType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Get metric value change over time (trend calculation)
     * 
     * @param city City entity
     * @param metricType Type of metric
     * @param startDate Start of period
     * @param endDate End of period
     * @return List of metrics ordered by time
     */
    @Query("SELECT m FROM Metrics m WHERE m.city = :city " +
           "AND m.metricType = :metricType " +
           "AND m.recordedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY m.recordedAt ASC")
    List<Metrics> getMetricTrend(
        @Param("city") City city,
        @Param("metricType") MetricType metricType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Count metrics for a city by type
     * 
     * @param city City entity
     * @param metricType Type of metric
     * @return Count of metrics
     */
    long countByCityAndMetricType(City city, MetricType metricType);
}
