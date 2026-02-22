package com.cityatlas.backend.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring Cache Configuration with Caffeine
 * 
 * Provides per-cache TTL and max-size policies using Caffeine,
 * a high-performance in-memory cache with automatic eviction.
 * 
 * Caches Defined:
 * - weather: Weather API responses (15 min TTL, max 200 entries)
 * - airQuality: Air Quality API responses (15 min TTL, max 200 entries)
 * - spotifyMetadata: Spotify metadata (6 hour TTL, max 500 entries)
 * - cityImages: Unsplash image URLs (24 hour TTL, max 500 entries)
 * 
 * @see org.springframework.cache.annotation.EnableCaching
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    /** Per-cache TTL and size configuration */
    private static final Map<String, CacheSpec> CACHE_SPECS = Map.ofEntries(
        Map.entry("weather",              new CacheSpec(Duration.ofMinutes(15), 200)),
        Map.entry("airQuality",           new CacheSpec(Duration.ofMinutes(15), 200)),
        Map.entry("spotifyMetadata",      new CacheSpec(Duration.ofHours(6), 500)),
        Map.entry("cityImages",           new CacheSpec(Duration.ofHours(24), 500)),
        // Real data source caches
        Map.entry("worldBankIndicator",   new CacheSpec(Duration.ofHours(24), 500)),
        Map.entry("worldBankHistory",     new CacheSpec(Duration.ofHours(24), 200)),
        Map.entry("geodbCitySearch",      new CacheSpec(Duration.ofHours(24), 300)),
        Map.entry("geodbCity",            new CacheSpec(Duration.ofHours(24), 200)),
        Map.entry("restCountries",        new CacheSpec(Duration.ofHours(48), 100)),
        Map.entry("cityData",            new CacheSpec(Duration.ofHours(6), 200)),
        Map.entry("cityAnalytics",       new CacheSpec(Duration.ofHours(6), 200))
    );

    record CacheSpec(Duration ttl, int maxSize) {}

    /**
     * Caffeine-backed cache manager with per-cache TTL and eviction.
     * Falls back to 10-minute TTL / 200 max for unlisted caches.
     *
     * Async mode is required so that @Cacheable works on reactive (Mono/Flux)
     * return types (Spring Framework 6.1+ supports sync callers with async caches).
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAllowNullValues(false);
        // Required for @Cacheable on Mono<> return types (WeatherService, AirQualityService, etc.)
        cacheManager.setAsyncCacheMode(true);

        // Register each named cache with its own Caffeine spec
        CACHE_SPECS.forEach((name, spec) -> {
            cacheManager.registerCustomCache(name,
                Caffeine.newBuilder()
                    .expireAfterWrite(spec.ttl())
                    .maximumSize(spec.maxSize())
                    .recordStats()
                    .buildAsync());
            log.info("Cache '{}' configured: TTL={}, maxSize={}", name, spec.ttl(), spec.maxSize());
        });

        // Default spec for any dynamic caches
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(200)
            .recordStats());

        return cacheManager;
    }
}
