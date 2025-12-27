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
 * Metrics Entity
 * 
 * Stores time-series measurements for various city metrics.
 * Designed for analytics queries and trend analysis.
 * 
 * Indexes:
 * - city_id + metric_type + recorded_at: Primary composite for time-series queries by city and type
 * - city_id: For retrieving all metrics for a city (dashboard overview)
 * - metric_type + recorded_at: For cross-city metric comparisons and aggregations
 * - recorded_at: For temporal queries and data cleanup operations
 */
@Entity
@Table(name = "metrics", indexes = {
    @Index(name = "idx_metrics_city_type_time", columnList = "city_id, metric_type, recorded_at"),
    @Index(name = "idx_metrics_city_id", columnList = "city_id"),
    @Index(name = "idx_metrics_type_time", columnList = "metric_type, recorded_at"),
    @Index(name = "idx_metrics_recorded_at", columnList = "recorded_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "city")
public class Metrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Associated city
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;
    
    /**
     * Type of metric being measured
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private MetricType metricType;
    
    /**
     * Metric value (numeric)
     * Can represent percentages, counts, indices, etc.
     */
    @Column(nullable = false)
    private Double value;
    
    /**
     * Unit of measurement (optional)
     * Examples: "percentage", "USD", "count", "index"
     */
    @Column(length = 50)
    private String unit;
    
    /**
     * Data source or provider
     * Examples: "Census Bureau", "EPA", "OpenAQ"
     */
    @Column(name = "data_source", length = 200)
    private String dataSource;
    
    /**
     * Additional context or notes about this metric
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    /**
     * Timestamp when this metric was recorded/measured
     * Critical for time-series analysis
     */
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    
    /**
     * Timestamp when record was created in our system
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
