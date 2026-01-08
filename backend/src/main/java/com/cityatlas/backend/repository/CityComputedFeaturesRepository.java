package com.cityatlas.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cityatlas.backend.entity.analytics.CityComputedFeatures;
import com.cityatlas.backend.entity.analytics.DimCity;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * CITY COMPUTED FEATURES REPOSITORY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Data access layer for pre-computed city features.
 * Supports feature retrieval, caching, and analytics queries.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * QUERY PATTERNS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   1. CACHE LOOKUP: Get latest features for a city (cache-aside pattern)
 *   2. RANKING: Get cities ranked by specific score
 *   3. BATCH LOAD: Get all features for today (dashboard rendering)
 *   4. HISTORICAL: Get score evolution over time (trend analysis)
 */
@Repository
public interface CityComputedFeaturesRepository extends JpaRepository<CityComputedFeatures, Long> {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLE CITY QUERIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Find features for a city on a specific date.
     * Used for cache lookup and historical analysis.
     */
    Optional<CityComputedFeatures> findByDimCityAndComputationDate(
            DimCity dimCity, LocalDate computationDate);
    
    /**
     * Find the most recent features for a city.
     * Primary method for cache-aside pattern.
     */
    @Query("""
        SELECT f FROM CityComputedFeatures f 
        WHERE f.dimCity = :dimCity 
        ORDER BY f.computationDate DESC 
        LIMIT 1
        """)
    Optional<CityComputedFeatures> findLatestByDimCity(@Param("dimCity") DimCity dimCity);
    
    /**
     * Find latest features by city slug.
     * Convenience method for API endpoints.
     */
    @Query("""
        SELECT f FROM CityComputedFeatures f 
        WHERE f.dimCity.citySlug = :citySlug 
        ORDER BY f.computationDate DESC 
        LIMIT 1
        """)
    Optional<CityComputedFeatures> findLatestByCitySlug(@Param("citySlug") String citySlug);
    
    /**
     * Check if features exist for a city on a given date.
     * Used to avoid redundant computation.
     */
    boolean existsByDimCityAndComputationDate(DimCity dimCity, LocalDate computationDate);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BATCH QUERIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Find all features computed on a specific date.
     * Used for batch operations and daily dashboards.
     */
    List<CityComputedFeatures> findByComputationDate(LocalDate computationDate);
    
    /**
     * Find all cities with features computed today.
     * Used to identify which cities need recomputation.
     */
    @Query("""
        SELECT f.dimCity.citySlug FROM CityComputedFeatures f 
        WHERE f.computationDate = :date
        """)
    List<String> findCitySlugsWithFeaturesOnDate(@Param("date") LocalDate date);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RANKING QUERIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get cities ranked by overall score (descending).
     * Used for "Top Cities" leaderboards.
     */
    @Query("""
        SELECT f FROM CityComputedFeatures f 
        WHERE f.computationDate = :date 
        AND f.overallScore IS NOT NULL 
        ORDER BY f.overallScore DESC
        """)
    List<CityComputedFeatures> findTopByOverallScore(
            @Param("date") LocalDate date, 
            org.springframework.data.domain.Pageable pageable);
    
    /**
     * Get cities ranked by economy score.
     */
    @Query("""
        SELECT f FROM CityComputedFeatures f 
        WHERE f.computationDate = :date 
        AND f.economyScore IS NOT NULL 
        ORDER BY f.economyScore DESC
        """)
    List<CityComputedFeatures> findTopByEconomyScore(
            @Param("date") LocalDate date, 
            org.springframework.data.domain.Pageable pageable);
    
    /**
     * Get cities ranked by livability score.
     */
    @Query("""
        SELECT f FROM CityComputedFeatures f 
        WHERE f.computationDate = :date 
        AND f.livabilityScore IS NOT NULL 
        ORDER BY f.livabilityScore DESC
        """)
    List<CityComputedFeatures> findTopByLivabilityScore(
            @Param("date") LocalDate date, 
            org.springframework.data.domain.Pageable pageable);
    
    /**
     * Get cities ranked by growth score.
     */
    @Query("""
        SELECT f FROM CityComputedFeatures f 
        WHERE f.computationDate = :date 
        AND f.growthScore IS NOT NULL 
        ORDER BY f.growthScore DESC
        """)
    List<CityComputedFeatures> findTopByGrowthScore(
            @Param("date") LocalDate date, 
            org.springframework.data.domain.Pageable pageable);
    
    /**
     * Get cities ranked by sustainability score.
     */
    @Query("""
        SELECT f FROM CityComputedFeatures f 
        WHERE f.computationDate = :date 
        AND f.sustainabilityScore IS NOT NULL 
        ORDER BY f.sustainabilityScore DESC
        """)
    List<CityComputedFeatures> findTopBySustainabilityScore(
            @Param("date") LocalDate date, 
            org.springframework.data.domain.Pageable pageable);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HISTORICAL / TREND QUERIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get feature history for a city within a date range.
     * Used for trend analysis and score evolution charts.
     */
    @Query("""
        SELECT f FROM CityComputedFeatures f 
        WHERE f.dimCity.citySlug = :citySlug 
        AND f.computationDate BETWEEN :startDate AND :endDate 
        ORDER BY f.computationDate ASC
        """)
    List<CityComputedFeatures> findHistoryByCitySlug(
            @Param("citySlug") String citySlug,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    /**
     * Get all feature records for a city (unlimited history).
     * Used for full historical analysis.
     */
    List<CityComputedFeatures> findByDimCityCitySlugOrderByComputationDateDesc(String citySlug);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DATA QUALITY QUERIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Find features with low data completeness.
     * Used for data quality monitoring.
     */
    @Query("""
        SELECT f FROM CityComputedFeatures f 
        WHERE f.computationDate = :date 
        AND f.dataCompleteness < :threshold
        """)
    List<CityComputedFeatures> findLowQualityFeatures(
            @Param("date") LocalDate date,
            @Param("threshold") double threshold);
    
    /**
     * Count features by data quality tier.
     */
    @Query("""
        SELECT 
            CASE 
                WHEN f.dataCompleteness >= 80 THEN 'high'
                WHEN f.dataCompleteness >= 50 THEN 'medium'
                ELSE 'low'
            END as tier,
            COUNT(f) as count
        FROM CityComputedFeatures f 
        WHERE f.computationDate = :date
        GROUP BY 
            CASE 
                WHEN f.dataCompleteness >= 80 THEN 'high'
                WHEN f.dataCompleteness >= 50 THEN 'medium'
                ELSE 'low'
            END
        """)
    List<Object[]> countByDataQualityTier(@Param("date") LocalDate date);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CLEANUP QUERIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Delete old feature records (data retention policy).
     * Default: Keep 365 days of history.
     */
    void deleteByComputationDateBefore(LocalDate date);
    
    /**
     * Count records by computation date.
     * Used for monitoring and capacity planning.
     */
    long countByComputationDate(LocalDate date);
}
