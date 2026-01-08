package com.cityatlas.backend.etl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.cityatlas.backend.entity.MetricType;

import lombok.extern.slf4j.Slf4j;

/**
 * ============================================================================
 *                    DATA QUALITY FALLBACK SERVICE
 * ============================================================================
 * 
 * Provides fallback values when API data is missing or invalid.
 * Implements a tiered fallback strategy:
 * 
 *   TIER 1: Last known good value (cached)
 *   TIER 2: Regional/country average
 *   TIER 3: Global default
 * 
 * FALLBACK STRATEGY:
 * +------------------+-------------------+-------------------+----------------+
 * | Metric Type      | Tier 1 (Cache)    | Tier 2 (Regional) | Tier 3 (Global)|
 * +------------------+-------------------+-------------------+----------------+
 * | AQI              | Last 24h value    | Country avg       | 75 (Moderate)  |
 * | POPULATION       | Last known        | N/A               | N/A (required) |
 * | GDP_PER_CAPITA   | Last year value   | Country avg       | 35,000 USD     |
 * | UNEMPLOYMENT     | Last month value  | Country avg       | 5%             |
 * | COST_OF_LIVING   | Last known        | Country avg       | 100 (baseline) |
 * +------------------+-------------------+-------------------+----------------+
 * 
 * @author CityAtlas ETL Team
 */
@Component
@Slf4j
public class DataQualityFallback {

    // ========================================================================
    //                    GLOBAL DEFAULTS (Tier 3)
    // ========================================================================
    
    private static final Map<MetricType, FallbackConfig> DEFAULTS = new HashMap<>();
    
    static {
        // Environmental - can fallback to moderate values
        DEFAULTS.put(MetricType.AQI, new FallbackConfig(
            75.0, true, 24, "Moderate AQI assumed"
        ));
        DEFAULTS.put(MetricType.CARBON_EMISSIONS, new FallbackConfig(
            8.0, true, 168, "Global average emissions"
        ));
        DEFAULTS.put(MetricType.WATER_QUALITY, new FallbackConfig(
            70.0, true, 168, "Assumed good water quality"
        ));
        
        // Economic - important, longer cache validity
        DEFAULTS.put(MetricType.GDP_PER_CAPITA, new FallbackConfig(
            35000.0, true, 720, "Global average GDP"
        ));
        DEFAULTS.put(MetricType.UNEMPLOYMENT_RATE, new FallbackConfig(
            5.0, true, 168, "Moderate unemployment assumed"
        ));
        DEFAULTS.put(MetricType.COST_OF_LIVING, new FallbackConfig(
            100.0, true, 720, "Baseline cost of living"
        ));
        DEFAULTS.put(MetricType.AVERAGE_SALARY, new FallbackConfig(
            50000.0, true, 720, "Moderate salary assumed"
        ));
        
        // Demographic - population is critical, no global fallback
        DEFAULTS.put(MetricType.POPULATION, new FallbackConfig(
            null, false, 8760, "Population required - no fallback"
        ));
        DEFAULTS.put(MetricType.POPULATION_GROWTH, new FallbackConfig(
            1.0, true, 8760, "Moderate growth assumed"
        ));
        DEFAULTS.put(MetricType.MEDIAN_AGE, new FallbackConfig(
            35.0, true, 8760, "Global median age"
        ));
        
        // Infrastructure
        DEFAULTS.put(MetricType.TRANSIT_COVERAGE, new FallbackConfig(
            50.0, true, 720, "Moderate transit assumed"
        ));
        DEFAULTS.put(MetricType.INTERNET_SPEED, new FallbackConfig(
            50.0, true, 168, "Moderate broadband assumed"
        ));
        DEFAULTS.put(MetricType.HOUSING_AFFORDABILITY, new FallbackConfig(
            100.0, true, 720, "Baseline affordability"
        ));
    }

    // ========================================================================
    //                    TIER 1: CACHE (Last Known Good Values)
    // ========================================================================
    
    /**
     * Cache of last known good values by city+metric.
     * Key format: "city_slug:METRIC_TYPE"
     */
    private final ConcurrentHashMap<String, CachedValue> valueCache = new ConcurrentHashMap<>();
    
    /**
     * Store a known good value in cache for future fallback.
     */
    public void cacheValue(String citySlug, MetricType type, Double value) {
        if (citySlug == null || type == null || value == null) {
            return;
        }
        
        String key = buildCacheKey(citySlug, type);
        valueCache.put(key, new CachedValue(value, LocalDateTime.now()));
        
        log.debug("[DQ-CACHE] Cached {}:{} = {}", citySlug, type, value);
    }
    
    /**
     * Get cached value if still valid.
     */
    public Optional<Double> getCachedValue(String citySlug, MetricType type) {
        String key = buildCacheKey(citySlug, type);
        CachedValue cached = valueCache.get(key);
        
        if (cached == null) {
            return Optional.empty();
        }
        
        FallbackConfig config = DEFAULTS.get(type);
        int maxAgeHours = config != null ? config.cacheValidityHours() : 24;
        
        long hoursOld = java.time.Duration.between(cached.timestamp(), LocalDateTime.now()).toHours();
        
        if (hoursOld > maxAgeHours) {
            log.debug("[DQ-CACHE] Cache expired for {}:{} ({}h old, max {}h)",
                    citySlug, type, hoursOld, maxAgeHours);
            valueCache.remove(key);
            return Optional.empty();
        }
        
        log.debug("[DQ-CACHE] Cache hit for {}:{} = {} ({}h old)",
                citySlug, type, cached.value(), hoursOld);
        return Optional.of(cached.value());
    }
    
    private String buildCacheKey(String citySlug, MetricType type) {
        return citySlug + ":" + type.name();
    }

    // ========================================================================
    //                    TIER 2: REGIONAL AVERAGES
    // ========================================================================
    
    /**
     * Regional averages by country code.
     * In production, this would be loaded from a database or config file.
     */
    private static final Map<String, Map<MetricType, Double>> REGIONAL_AVERAGES = new HashMap<>();
    
    static {
        // USA averages
        Map<MetricType, Double> usa = new HashMap<>();
        usa.put(MetricType.AQI, 45.0);
        usa.put(MetricType.GDP_PER_CAPITA, 65000.0);
        usa.put(MetricType.UNEMPLOYMENT_RATE, 4.0);
        usa.put(MetricType.COST_OF_LIVING, 120.0);
        usa.put(MetricType.AVERAGE_SALARY, 55000.0);
        REGIONAL_AVERAGES.put("US", usa);
        
        // UK averages
        Map<MetricType, Double> uk = new HashMap<>();
        uk.put(MetricType.AQI, 35.0);
        uk.put(MetricType.GDP_PER_CAPITA, 45000.0);
        uk.put(MetricType.UNEMPLOYMENT_RATE, 4.5);
        uk.put(MetricType.COST_OF_LIVING, 115.0);
        uk.put(MetricType.AVERAGE_SALARY, 42000.0);
        REGIONAL_AVERAGES.put("GB", uk);
        
        // Germany averages
        Map<MetricType, Double> de = new HashMap<>();
        de.put(MetricType.AQI, 30.0);
        de.put(MetricType.GDP_PER_CAPITA, 50000.0);
        de.put(MetricType.UNEMPLOYMENT_RATE, 3.5);
        de.put(MetricType.COST_OF_LIVING, 105.0);
        de.put(MetricType.AVERAGE_SALARY, 48000.0);
        REGIONAL_AVERAGES.put("DE", de);
        
        // India averages
        Map<MetricType, Double> india = new HashMap<>();
        india.put(MetricType.AQI, 120.0);
        india.put(MetricType.GDP_PER_CAPITA, 2500.0);
        india.put(MetricType.UNEMPLOYMENT_RATE, 7.0);
        india.put(MetricType.COST_OF_LIVING, 35.0);
        india.put(MetricType.AVERAGE_SALARY, 8000.0);
        REGIONAL_AVERAGES.put("IN", india);
    }
    
    /**
     * Get regional average for a metric type.
     */
    public Optional<Double> getRegionalAverage(String countryCode, MetricType type) {
        if (countryCode == null || type == null) {
            return Optional.empty();
        }
        
        Map<MetricType, Double> countryAverages = REGIONAL_AVERAGES.get(countryCode.toUpperCase());
        if (countryAverages == null) {
            return Optional.empty();
        }
        
        Double avg = countryAverages.get(type);
        if (avg != null) {
            log.debug("[DQ-REGIONAL] Using {} average for {}: {}", countryCode, type, avg);
        }
        return Optional.ofNullable(avg);
    }

    // ========================================================================
    //                    FALLBACK RESOLUTION
    // ========================================================================
    
    /**
     * Resolve a fallback value using the tiered strategy.
     * 
     * @param citySlug City identifier for cache lookup
     * @param countryCode Country code for regional averages (nullable)
     * @param type Metric type
     * @return FallbackResult with value and source tier
     */
    public FallbackResult resolveFallback(String citySlug, String countryCode, MetricType type) {
        log.info("[DQ-FALLBACK] Resolving fallback for {}:{}", citySlug, type);
        
        // TIER 1: Try cache first
        Optional<Double> cached = getCachedValue(citySlug, type);
        if (cached.isPresent()) {
            log.info("[DQ-FALLBACK] Using cached value for {}:{} = {} (Tier 1)",
                    citySlug, type, cached.get());
            return new FallbackResult(cached.get(), FallbackTier.CACHED, 
                    "Last known value from cache");
        }
        
        // TIER 2: Try regional average
        Optional<Double> regional = getRegionalAverage(countryCode, type);
        if (regional.isPresent()) {
            log.info("[DQ-FALLBACK] Using regional average for {}:{} = {} (Tier 2)",
                    citySlug, type, regional.get());
            return new FallbackResult(regional.get(), FallbackTier.REGIONAL,
                    countryCode + " regional average");
        }
        
        // TIER 3: Use global default
        FallbackConfig config = DEFAULTS.get(type);
        if (config != null && config.hasDefault()) {
            log.info("[DQ-FALLBACK] Using global default for {}:{} = {} (Tier 3)",
                    citySlug, type, config.defaultValue());
            return new FallbackResult(config.defaultValue(), FallbackTier.GLOBAL,
                    config.description());
        }
        
        // No fallback available
        log.warn("[DQ-FALLBACK] No fallback available for {}:{}", citySlug, type);
        return new FallbackResult(null, FallbackTier.NONE, 
                "No fallback available - metric is required");
    }
    
    /**
     * Get value with automatic fallback if null.
     * 
     * @param value The actual value (may be null)
     * @param citySlug City identifier
     * @param countryCode Country code (nullable)
     * @param type Metric type
     * @return ValueWithFallback containing final value and metadata
     */
    public ValueWithFallback getValueWithFallback(
            Double value, String citySlug, String countryCode, MetricType type) {
        
        if (value != null) {
            // Value is good - cache it and return
            cacheValue(citySlug, type, value);
            return new ValueWithFallback(value, false, null, null);
        }
        
        // Value is null - resolve fallback
        FallbackResult fallback = resolveFallback(citySlug, countryCode, type);
        
        if (fallback.value() == null) {
            log.error("[DQ-FALLBACK] CRITICAL: No value available for {}:{}", citySlug, type);
            return new ValueWithFallback(null, true, fallback.tier(), fallback.description());
        }
        
        return new ValueWithFallback(fallback.value(), true, fallback.tier(), fallback.description());
    }

    // ========================================================================
    //                    CACHE MANAGEMENT
    // ========================================================================
    
    /**
     * Clear all cached values.
     */
    public void clearCache() {
        int size = valueCache.size();
        valueCache.clear();
        log.info("[DQ-CACHE] Cleared {} cached values", size);
    }
    
    /**
     * Clear cached values for a specific city.
     */
    public void clearCacheForCity(String citySlug) {
        int removed = 0;
        for (String key : valueCache.keySet()) {
            if (key.startsWith(citySlug + ":")) {
                valueCache.remove(key);
                removed++;
            }
        }
        log.info("[DQ-CACHE] Cleared {} cached values for city: {}", removed, citySlug);
    }
    
    /**
     * Get cache statistics.
     */
    public CacheStats getCacheStats() {
        Map<MetricType, Integer> countByType = new HashMap<>();
        
        for (String key : valueCache.keySet()) {
            String[] parts = key.split(":");
            if (parts.length == 2) {
                try {
                    MetricType type = MetricType.valueOf(parts[1]);
                    countByType.merge(type, 1, Integer::sum);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        return new CacheStats(valueCache.size(), countByType);
    }

    // ========================================================================
    //                    RESULT TYPES
    // ========================================================================
    
    public enum FallbackTier {
        CACHED("Tier 1: Cached value"),
        REGIONAL("Tier 2: Regional average"),
        GLOBAL("Tier 3: Global default"),
        NONE("No fallback available");
        
        private final String description;
        
        FallbackTier(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public record FallbackResult(
        Double value,
        FallbackTier tier,
        String description
    ) {}
    
    public record ValueWithFallback(
        Double value,
        boolean usedFallback,
        FallbackTier fallbackTier,
        String fallbackDescription
    ) {}
    
    public record CachedValue(
        Double value,
        LocalDateTime timestamp
    ) {}
    
    public record FallbackConfig(
        Double defaultValue,
        boolean hasDefault,
        int cacheValidityHours,
        String description
    ) {}
    
    public record CacheStats(
        int totalEntries,
        Map<MetricType, Integer> entriesByType
    ) {}
}
