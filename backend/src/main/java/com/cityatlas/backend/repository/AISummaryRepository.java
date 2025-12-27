package com.cityatlas.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cityatlas.backend.entity.AISummary;
import com.cityatlas.backend.entity.City;

/**
 * AI Summary Repository
 * 
 * Data access layer for AISummary entity.
 * Handles AI-generated city summaries.
 */
@Repository
public interface AISummaryRepository extends JpaRepository<AISummary, Long> {
    
    /**
     * Find AI summary for a specific city
     * One-to-one relationship lookup
     * 
     * @param city City entity
     * @return Optional containing summary if exists
     */
    Optional<AISummary> findByCity(City city);
    
    /**
     * Check if AI summary exists for a city
     * 
     * @param city City entity
     * @return true if summary exists
     */
    boolean existsByCity(City city);
    
    /**
     * Find summaries generated after a specific date
     * Useful for finding recent generations
     * 
     * @param generatedAt Cutoff date
     * @return List of recent summaries
     */
    List<AISummary> findByGeneratedAtAfter(LocalDateTime generatedAt);
    
    /**
     * Find summaries generated before a specific date
     * Useful for identifying stale summaries that need regeneration
     * 
     * @param generatedAt Cutoff date
     * @return List of old summaries
     */
    List<AISummary> findByGeneratedAtBefore(LocalDateTime generatedAt);
    
    /**
     * Find summaries by length type
     * 
     * @param summaryLength Length type (short, medium, detailed)
     * @return List of summaries
     */
    List<AISummary> findBySummaryLength(String summaryLength);
    
    /**
     * Find summaries by AI model
     * 
     * @param aiModel Model name (e.g., "gpt-4")
     * @return List of summaries
     */
    List<AISummary> findByAiModel(String aiModel);
    
    /**
     * Find summaries with confidence score above threshold
     * 
     * @param confidenceScore Minimum confidence
     * @return List of high-confidence summaries
     */
    List<AISummary> findByConfidenceScoreGreaterThan(Double confidenceScore);
    
    /**
     * Find stale summaries that need regeneration
     * Summaries older than specified age
     * 
     * @param olderThan Date threshold
     * @return List of stale summaries
     */
    @Query("SELECT s FROM AISummary s WHERE s.generatedAt < :olderThan")
    List<AISummary> findStaleSummaries(@Param("olderThan") LocalDateTime olderThan);
    
    /**
     * Count summaries generated within date range
     * Analytics query for AI usage
     * 
     * @param startDate Start of range
     * @param endDate End of range
     * @return Count of summaries
     */
    long countByGeneratedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get average confidence score
     * Analytics query for AI quality
     * 
     * @return Average confidence score
     */
    @Query("SELECT AVG(s.confidenceScore) FROM AISummary s WHERE s.confidenceScore IS NOT NULL")
    Double getAverageConfidenceScore();
}
