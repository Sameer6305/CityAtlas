package com.cityatlas.backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * AI Summary Entity
 * 
 * Stores AI-generated summaries for cities.
 * One-to-one relationship with City (each city has one summary).
 * 
 * Indexes:
 * - city_id: Unique via @JoinColumn for one-to-one relationship (automatic)
 * - generated_at: For tracking freshness and scheduling regeneration
 * - ai_model: For analyzing performance across different AI models
 * - summary_length: For filtering by summary type (short/medium/detailed)
 */
@Entity
@Table(name = "ai_summaries", indexes = {
    @Index(name = "idx_ai_summary_generated_at", columnList = "generated_at"),
    @Index(name = "idx_ai_summary_model", columnList = "ai_model"),
    @Index(name = "idx_ai_summary_length", columnList = "summary_length")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "city")
public class AISummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Associated city (one-to-one)
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false, unique = true)
    private City city;
    
    /**
     * AI-generated summary text
     * Comprehensive overview of city characteristics
     */
    @Column(name = "summary_text", columnDefinition = "TEXT", nullable = false)
    private String summaryText;
    
    /**
     * Summary length type (short, medium, detailed)
     */
    @Column(name = "summary_length", length = 20)
    private String summaryLength;
    
    /**
     * AI model used for generation
     * Examples: "gpt-4", "claude-3", "custom-model"
     */
    @Column(name = "ai_model", length = 50)
    private String aiModel;
    
    /**
     * Confidence score (0.0 to 1.0)
     * Indicates AI model's confidence in the summary
     */
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    /**
     * Number of data points used to generate summary
     */
    @Column(name = "data_points_count")
    private Integer dataPointsCount;
    
    /**
     * Timestamp when summary was generated
     * Used to determine if regeneration is needed
     */
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    
    /**
     * Timestamp when record was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when record was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
