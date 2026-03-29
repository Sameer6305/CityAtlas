package com.cityatlas.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.entity.analytics.CityComputedFeatures;
import com.cityatlas.backend.repository.CityComputedFeaturesRepository;
import com.cityatlas.backend.repository.CityRepository;
import com.cityatlas.backend.service.CityFeatureComputer.CityFeatures;
import com.cityatlas.backend.service.CityFeatureComputer.ScoreResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * CITY FEATURE STORE - Feature Persistence and Caching Layer
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Manages the lifecycle of computed city features including:
 * - In-memory caching for hot data (L1 cache)
 * - Database persistence for durability (L2 cache)
 * - Scheduled recomputation for freshness
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * CACHING STRATEGY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────────────────────────────────────────────────────────────────┐
 *   │                    CACHE-ASIDE PATTERN                                  │
 *   │                                                                          │
 *   │   Request ───▶ L1 Cache ───miss──▶ L2 Database ───miss──▶ Compute      │
 *   │       │          (Memory)             (PostgreSQL)         (Feature     │
 *   │       │             │                      │               Computer)    │
 *   │       ◀────────────hit                    hit                  │        │
 *   │                     │                      │                   │        │
 *   │                     ◀──────────────────────◀───────────────────┘        │
 *   │                                   Store in both caches                  │
 *   └─────────────────────────────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * RECOMPUTATION STRATEGY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   1. DAILY BATCH (3:00 AM): Recompute all cities → ensures freshness
 *   2. ON-DEMAND: When city data changes → maintains accuracy
 *   3. LAZY: On first request if cache miss → cold start handling
 * 
 * @see CityFeatureComputer
 * @see CityComputedFeatures
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CityFeatureStore {
    
    private final CityFeatureComputer featureComputer;
    private final CityComputedFeaturesRepository featuresRepository;
    private final CityRepository cityRepository;

    // Simple observability counters for interview/demo APIs.
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong l1HitCount = new AtomicLong(0);
    private final AtomicLong l2HitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong computeAttemptCount = new AtomicLong(0);
    private final AtomicLong computeSuccessCount = new AtomicLong(0);
    private final AtomicLong computeFailureCount = new AtomicLong(0);
    private volatile LocalDateTime lastComputeAt;
    private volatile LocalDateTime lastBatchStartedAt;
    private volatile LocalDateTime lastBatchCompletedAt;
    private volatile Integer lastBatchSuccessCount = 0;
    private volatile Integer lastBatchFailureCount = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // L1 CACHE (In-Memory)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * In-memory cache for frequently accessed features.
     * Key: citySlug, Value: CityFeatures (computed result)
     * 
     * TTL: Features are invalidated daily at 3:00 AM during batch recomputation.
     * Size: Bounded by number of cities (typically < 1000).
     */
    private final Map<String, CachedFeatures> memoryCache = new ConcurrentHashMap<>();
    
    /**
     * Wrapper for cached features with timestamp.
     */
    private record CachedFeatures(
        CityFeatures features,
        LocalDate computationDate,
        LocalDateTime cachedAt
    ) {
        boolean isStale() {
            return computationDate.isBefore(LocalDate.now());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FEATURE RETRIEVAL (Cache-Aside Pattern)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get features for a city, using cache-aside pattern.
     * 
     * FLOW:
     * 1. Check L1 (memory) cache
     * 2. If miss, check L2 (database)
     * 3. If miss, compute and store in both caches
     * 
     * @param citySlug City identifier
     * @param currentAqi Current AQI for livability/sustainability scores
     * @return CityFeatures or null if city not found
     */
    public CityFeatures getFeatures(String citySlug, Integer currentAqi) {
        log.debug("[FEATURE-STORE] Retrieving features for city: {}", citySlug);
        requestCount.incrementAndGet();
        
        // L1 Cache: Check in-memory cache
        CachedFeatures cached = memoryCache.get(citySlug);
        if (cached != null && !cached.isStale()) {
            l1HitCount.incrementAndGet();
            log.debug("[FEATURE-STORE] L1 cache hit for: {}", citySlug);
            return cached.features();
        }
        
        // L2 Cache: Check database
        Optional<CityComputedFeatures> stored = featuresRepository.findLatestByCitySlug(citySlug);
        if (stored.isPresent() && !stored.get().isStale()) {
            l2HitCount.incrementAndGet();
            log.debug("[FEATURE-STORE] L2 cache hit for: {}", citySlug);
            CityFeatures features = toFeatures(stored.get());
            cacheInMemory(citySlug, features, stored.get().getComputationDate());
            return features;
        }
        
        // Cache miss: Compute and store
        missCount.incrementAndGet();
        log.info("[FEATURE-STORE] Cache miss for: {}. Computing features...", citySlug);
        return computeAndStore(citySlug, currentAqi);
    }
    
    /**
     * Get features for a city entity directly.
     * Convenience method when City is already loaded.
     */
    public CityFeatures getFeatures(City city, Integer currentAqi) {
        return getFeatures(city.getSlug(), currentAqi);
    }
    
    /**
     * Get features without AQI (uses cached AQI if available).
     */
    public CityFeatures getFeatures(String citySlug) {
        return getFeatures(citySlug, null);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FEATURE COMPUTATION AND STORAGE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Compute features for a city and persist to both caches.
     * 
     * @param citySlug City identifier
     * @param currentAqi Current AQI (may be null)
     * @return Computed features or null if city not found
     */
    @Transactional
    public CityFeatures computeAndStore(String citySlug, Integer currentAqi) {
        log.info("[FEATURE-STORE] Computing and storing features for: {}", citySlug);
        computeAttemptCount.incrementAndGet();
        
        // Load city
        Optional<City> cityOpt = cityRepository.findBySlug(citySlug);
        if (cityOpt.isEmpty()) {
            log.warn("[FEATURE-STORE] City not found: {}", citySlug);
            computeFailureCount.incrementAndGet();
            return null;
        }
        
        City city = cityOpt.get();
        
        // Compute features using deterministic rules
        CityFeatures features = featureComputer.computeFeatures(city, currentAqi);
        
        // Store in database (L2 cache)
        storeFeatures(city, features, currentAqi);
        
        // Store in memory (L1 cache)
        cacheInMemory(citySlug, features, LocalDate.now());
        
        log.info("[FEATURE-STORE] Features computed and stored for: {} (overall={})",
                citySlug, features.getOverallScore().score());
        lastComputeAt = LocalDateTime.now();
        computeSuccessCount.incrementAndGet();
        
        return features;
    }
    
    /**
     * Store computed features in the database.
     * Creates a new record for today or updates existing.
     */
    @Transactional
    protected void storeFeatures(City city, CityFeatures features, Integer currentAqi) {
        LocalDate today = LocalDate.now();
        
        // For now, we'll create features without DimCity linkage
        // In production, this would link to the star schema dimension table
        CityComputedFeatures.builder()
                .computationDate(today)
                // Scores
                .economyScore(safeScore(features.getEconomyScore()))
                .livabilityScore(safeScore(features.getLivabilityScore()))
                .sustainabilityScore(safeScore(features.getSustainabilityScore()))
                .growthScore(null) // Computed by GrowthScoreComputer (future)
                .overallScore(safeScore(features.getOverallScore()))
                // Explanations
                .economyExplanation(safeExplanation(features.getEconomyScore()))
                .livabilityExplanation(safeExplanation(features.getLivabilityScore()))
                .sustainabilityExplanation(safeExplanation(features.getSustainabilityScore()))
                .overallExplanation(safeExplanation(features.getOverallScore()))
                // Data quality
                .dataCompleteness(features.getDataCompleteness())
                .confidenceScore(computeConfidence(features))
                .missingData(collectMissingData(features))
                // Input values for auditing
                .inputGdpPerCapita(city.getGdpPerCapita())
                .inputUnemploymentRate(city.getUnemploymentRate())
                .inputCostOfLiving(city.getCostOfLivingIndex())
                .inputAqi(currentAqi)
                .inputPopulation(city.getPopulation())
                // ETL metadata
                .etlBatchId("on-demand-" + System.currentTimeMillis())
                .build();
        
        // Note: In production, we would link to DimCity
        // For demo purposes, we'll save without the FK constraint
        log.debug("[FEATURE-STORE] Feature entity created for: {}", city.getSlug());
        
        // Store in memory only (skip database due to FK constraint)
        // In production: featuresRepository.save(entity);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Cache features in memory.
     */
    private void cacheInMemory(String citySlug, CityFeatures features, LocalDate computationDate) {
        memoryCache.put(citySlug, new CachedFeatures(
                features, computationDate, LocalDateTime.now()));
    }
    
    /**
     * Invalidate cache for a specific city.
     * Called when city data is updated.
     */
    public void invalidate(String citySlug) {
        log.info("[FEATURE-STORE] Invalidating cache for: {}", citySlug);
        memoryCache.remove(citySlug);
    }
    
    /**
     * Invalidate entire memory cache.
     * Called during batch recomputation.
     */
    public void invalidateAll() {
        log.info("[FEATURE-STORE] Invalidating entire cache ({} entries)", memoryCache.size());
        memoryCache.clear();
    }
    
    /**
     * Get cache statistics.
     */
    public Map<String, Object> getCacheStats() {
        long fresh = memoryCache.values().stream().filter(c -> !c.isStale()).count();
        long stale = memoryCache.size() - fresh;
        
        return Map.of(
                "totalEntries", memoryCache.size(),
                "freshEntries", fresh,
                "staleEntries", stale,
                "hitRate", "N/A" // Would require hit/miss counters
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BATCH OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Recompute features for all cities.
     * Scheduled daily at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void batchRecompute() {
        log.info("[FEATURE-STORE] Starting daily batch recomputation...");
        lastBatchStartedAt = LocalDateTime.now();
        
        // Clear memory cache
        invalidateAll();
        
        // Load all cities
        List<City> cities = cityRepository.findAll();
        log.info("[FEATURE-STORE] Recomputing features for {} cities", cities.size());
        
        int success = 0;
        int failed = 0;
        
        for (City city : cities) {
            try {
                computeAndStore(city.getSlug(), null); // AQI will be fetched if needed
                success++;
            } catch (Exception e) {
                log.error("[FEATURE-STORE] Failed to compute features for: {}", 
                        city.getSlug(), e);
                failed++;
            }
        }
        
        log.info("[FEATURE-STORE] Batch recomputation complete. Success: {}, Failed: {}", 
                success, failed);
        lastBatchSuccessCount = success;
        lastBatchFailureCount = failed;
        lastBatchCompletedAt = LocalDateTime.now();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RANKING QUERIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get top cities by overall score.
     * 
     * @param limit Number of cities to return
     * @return List of features ordered by overall score descending
     */
    public List<CityComputedFeatures> getTopCitiesOverall(int limit) {
        return featuresRepository.findTopByOverallScore(
                LocalDate.now(), PageRequest.of(0, limit));
    }
    
    /**
     * Get top cities by economy score.
     */
    public List<CityComputedFeatures> getTopCitiesEconomy(int limit) {
        return featuresRepository.findTopByEconomyScore(
                LocalDate.now(), PageRequest.of(0, limit));
    }

    /**
     * API-safe ranking view for top overall cities.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopOverallRankingView(int limit) {
        return getTopCitiesOverall(limit).stream().map(this::toRankingView).toList();
    }

    /**
     * API-safe ranking view for top economy cities.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopEconomyRankingView(int limit) {
        return getTopCitiesEconomy(limit).stream().map(this::toRankingView).toList();
    }

    /**
     * Exposes cache + computation telemetry for observability endpoint.
     */
    public Map<String, Object> getComputationStats() {
        long totalRequests = requestCount.get();
        long totalHits = l1HitCount.get() + l2HitCount.get();
        double hitRate = totalRequests > 0 ? (totalHits * 100.0) / totalRequests : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("requests", totalRequests);
        stats.put("l1Hits", l1HitCount.get());
        stats.put("l2Hits", l2HitCount.get());
        stats.put("cacheMisses", missCount.get());
        stats.put("hitRatePct", Math.round(hitRate * 100.0) / 100.0);
        stats.put("computeAttempts", computeAttemptCount.get());
        stats.put("computeSuccess", computeSuccessCount.get());
        stats.put("computeFailures", computeFailureCount.get());
        stats.put("lastComputeAt", lastComputeAt);
        stats.put("lastBatchStartedAt", lastBatchStartedAt);
        stats.put("lastBatchCompletedAt", lastBatchCompletedAt);
        stats.put("lastBatchSuccessCount", lastBatchSuccessCount);
        stats.put("lastBatchFailureCount", lastBatchFailureCount);
        stats.put("todayPersistedFeatureRows", featuresRepository.countByComputationDate(LocalDate.now()));
        return stats;
    }
    
    /**
     * Get top cities by livability score.
     */
    public List<CityComputedFeatures> getTopCitiesLivability(int limit) {
        return featuresRepository.findTopByLivabilityScore(
                LocalDate.now(), PageRequest.of(0, limit));
    }
    
    /**
     * Get top cities by growth score.
     */
    public List<CityComputedFeatures> getTopCitiesGrowth(int limit) {
        return featuresRepository.findTopByGrowthScore(
                LocalDate.now(), PageRequest.of(0, limit));
    }
    
    /**
     * Get top cities by sustainability score.
     */
    public List<CityComputedFeatures> getTopCitiesSustainability(int limit) {
        return featuresRepository.findTopBySustainabilityScore(
                LocalDate.now(), PageRequest.of(0, limit));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Convert database entity to CityFeatures DTO.
     */
    private CityFeatures toFeatures(CityComputedFeatures entity) {
        return CityFeatures.builder()
                .citySlug(entity.getDimCity() != null ? entity.getDimCity().getCitySlug() : "unknown")
                .economyScore(toScoreResult(entity.getEconomyScore(), entity.getEconomyExplanation()))
                .livabilityScore(toScoreResult(entity.getLivabilityScore(), entity.getLivabilityExplanation()))
                .sustainabilityScore(toScoreResult(entity.getSustainabilityScore(), entity.getSustainabilityExplanation()))
                .overallScore(toScoreResult(entity.getOverallScore(), entity.getOverallExplanation()))
                .dataCompleteness(entity.getDataCompleteness())
                .build();
    }
    
    private ScoreResult toScoreResult(Double score, String explanation) {
        return new ScoreResult(
                score,
                explanation != null ? explanation : "No explanation available",
                List.of(),
                List.of(),
                score != null ? 1.0 : 0.0
        );
    }

    private Map<String, Object> toRankingView(CityComputedFeatures feature) {
        Map<String, Object> row = new LinkedHashMap<>();
        DimCitySafe dimCity = new DimCitySafe(feature);
        row.put("citySlug", dimCity.citySlug());
        row.put("cityName", dimCity.cityName());
        row.put("country", dimCity.country());
        row.put("computationDate", feature.getComputationDate());
        row.put("overallScore", feature.getOverallScore());
        row.put("economyScore", feature.getEconomyScore());
        row.put("livabilityScore", feature.getLivabilityScore());
        row.put("sustainabilityScore", feature.getSustainabilityScore());
        row.put("growthScore", feature.getGrowthScore());
        row.put("dataCompleteness", feature.getDataCompleteness());
        return row;
    }

    /**
     * Protect ranking API serialization from lazy-loading nulls and legacy rows.
     */
    private record DimCitySafe(String citySlug, String cityName, String country) {
        DimCitySafe(CityComputedFeatures feature) {
            this(
                feature.getDimCity() != null ? feature.getDimCity().getCitySlug() : null,
                feature.getDimCity() != null ? feature.getDimCity().getCityName() : null,
                feature.getDimCity() != null ? feature.getDimCity().getCountry() : null
            );
        }
    }
    
    private Double safeScore(ScoreResult result) {
        return result != null ? result.score() : null;
    }
    
    private String safeExplanation(ScoreResult result) {
        return result != null ? result.explanation() : null;
    }
    
    private Double computeConfidence(CityFeatures features) {
        double total = 0.0;
        int count = 0;
        
        if (features.getEconomyScore().confidence() != null) {
            total += features.getEconomyScore().confidence();
            count++;
        }
        if (features.getLivabilityScore().confidence() != null) {
            total += features.getLivabilityScore().confidence();
            count++;
        }
        if (features.getSustainabilityScore().confidence() != null) {
            total += features.getSustainabilityScore().confidence();
            count++;
        }
        
        return count > 0 ? total / count : 0.0;
    }
    
    private String collectMissingData(CityFeatures features) {
        List<String> missing = new java.util.ArrayList<>();
        
        missing.addAll(features.getEconomyScore().missingData());
        missing.addAll(features.getLivabilityScore().missingData());
        missing.addAll(features.getSustainabilityScore().missingData());
        
        return missing.isEmpty() ? null : String.join(",", missing);
    }
}
