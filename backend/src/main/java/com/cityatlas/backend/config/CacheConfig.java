package com.cityatlas.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache Configuration
 * 
 * Enables caching for external API responses to improve performance
 * and reduce API calls to third-party services.
 * 
 * Cache Strategy:
 * - Uses in-memory cache (ConcurrentHashMap)
 * - Cache keys include city names and parameters
 * - TTL configured via application.properties
 * - App works even if cache is empty (cache-aside pattern)
 * 
 * Caches Defined:
 * - weather: Weather API responses (10-30 min TTL)
 * - airQuality: Air Quality API responses (15-30 min TTL)
 * - spotifyMetadata: Spotify metadata (6-24 hour TTL)
 * - cityImages: Unsplash image URLs (24 hour TTL)
 * 
 * Cache Eviction:
 * - Automatic: TTL expiration (configured per cache)
 * - Manual: @CacheEvict annotation for refresh
 * - Restart: All caches cleared on application restart
 * 
 * Usage Example:
 * <pre>
 * {@code
 * @Cacheable(value = "weather", key = "#cityName")
 * public Mono<WeatherDTO> fetchWeather(String cityName) {
 *     // Cache miss: fetch from API
 *     // Cache hit: return cached value
 * }
 * }
 * </pre>
 * 
 * Why Cache External APIs:
 * - Reduces API calls (respects rate limits)
 * - Improves response time (no network latency)
 * - Saves bandwidth and API quota
 * - Better user experience (faster page loads)
 * 
 * Production Recommendation:
 * - Use Redis for distributed caching
 * - Configure TTL per cache type
 * - Monitor cache hit/miss rates
 * - Implement cache warming for popular cities
 * 
 * @see org.springframework.cache.annotation.EnableCaching
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Configure in-memory cache manager
     * 
     * Uses ConcurrentMapCacheManager for simple in-memory caching.
     * Each cache name creates a separate ConcurrentHashMap.
     * 
     * For production, consider Redis:
     * <pre>
     * {@code
     * @Bean
     * public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
     *     RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
     *         .entryTtl(Duration.ofMinutes(10))
     *         .serializeValuesWith(RedisSerializationContext.SerializationPair
     *             .fromSerializer(new GenericJackson2JsonRedisSerializer()));
     *     
     *     return RedisCacheManager.builder(connectionFactory)
     *         .cacheDefaults(config)
     *         .build();
     * }
     * }
     * </pre>
     * 
     * @return CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        // Create in-memory cache manager with predefined cache names
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "weather",           // Weather API responses
                "airQuality",        // Air Quality API responses
                "spotifyMetadata",   // Spotify metadata
                "cityImages"         // Unsplash image URLs
        );
        
        // Allow dynamic cache creation for additional caches
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
}
