# Spring Cache Configuration Guide

## Overview

Spring Cache is now enabled for external API responses to improve performance and reduce API calls to third-party services. This implementation uses **in-memory caching** with ConcurrentHashMap for simplicity.

## What's Cached

| Cache Name | Service | Data | Recommended TTL |
|------------|---------|------|-----------------|
| **weather** | WeatherService | Current weather by city | 10-30 minutes |
| **airQuality** | AirQualityService | Air quality data by city | 15-30 minutes |
| **spotifyMetadata** | SpotifyService | City music metadata | 6-24 hours |
| **cityImages** | CityImageService | Unsplash image URLs | 24 hours |

## How It Works

### Cache-Aside Pattern

1. **Cache Hit**: Request → Cache → Return cached data
2. **Cache Miss**: Request → Cache (miss) → External API → Cache + Return

### Automatic Behavior

- ✅ **Transparent**: No code changes needed in controllers
- ✅ **Graceful**: Works even when cache is empty
- ✅ **Safe**: Null results are not cached (`unless = "#result == null"`)
- ✅ **Key-based**: Each city has its own cache entry

## Implementation Details

### Configuration File

[CacheConfig.java](src/main/java/com/cityatlas/backend/config/CacheConfig.java)
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "weather", "airQuality", "spotifyMetadata", "cityImages"
        );
    }
}
```

### Cached Methods

**WeatherService.fetchCurrentWeather()**
```java
@Cacheable(value = "weather", key = "#cityName", unless = "#result == null")
public Mono<WeatherDTO> fetchCurrentWeather(String cityName) {
    // API call only happens on cache miss
}
```

**AirQualityService.fetchAirQuality()**
```java
@Cacheable(value = "airQuality", key = "#cityName", unless = "#result == null")
public Mono<AirQualityDTO> fetchAirQuality(String cityName) {
    // API call only happens on cache miss
}
```

### Cache Keys

Cache entries are keyed by city name:
- Key: `"Paris"` → Value: `WeatherDTO{temp=15.5, ...}`
- Key: `"Tokyo"` → Value: `AirQualityDTO{aqi=42, ...}`

## Benefits

### Performance Improvements

- **Faster Response Time**: No network latency for cached data
- **Reduced API Calls**: Respects rate limits of external services
- **Lower Costs**: Fewer API requests = lower usage charges
- **Better UX**: Instant data for popular cities

### Example Metrics

Without Cache:
```
Request 1: Paris weather → 500ms (API call)
Request 2: Paris weather → 500ms (API call)
Request 3: Paris weather → 500ms (API call)
Total: 1500ms, 3 API calls
```

With Cache:
```
Request 1: Paris weather → 500ms (API call + cache)
Request 2: Paris weather → 5ms (cache hit)
Request 3: Paris weather → 5ms (cache hit)
Total: 510ms, 1 API call
```

## Configuration

### Current Setup (Simple Cache)

From [application.properties](src/main/resources/application.properties):
```properties
# In-memory cache with no TTL
spring.cache.type=simple

# Cache logging for debugging
logging.level.org.springframework.cache=DEBUG
```

**Limitations:**
- ❌ No TTL (Time-To-Live) configuration
- ❌ No cache size limits
- ❌ No distributed caching
- ❌ Lost on application restart

**Advantages:**
- ✅ Zero configuration
- ✅ No external dependencies
- ✅ Perfect for development
- ✅ Works immediately

## Testing Cache

### Check Cache is Working

1. **First Request** (Cache Miss):
```bash
curl "http://localhost:8080/api/weather/current?city=Paris"
# Check logs: Cache miss, API call made
```

2. **Second Request** (Cache Hit):
```bash
curl "http://localhost:8080/api/weather/current?city=Paris"
# Check logs: Cache hit, no API call
```

3. **Different City** (Cache Miss):
```bash
curl "http://localhost:8080/api/weather/current?city=London"
# Check logs: Cache miss, API call made
```

### Log Output

With cache logging enabled:
```
DEBUG org.springframework.cache - Cache miss for key 'Paris' in cache 'weather'
INFO com.cityatlas.backend.service.external.WeatherService - Fetching weather for city: Paris
DEBUG org.springframework.cache - Cache put for key 'Paris' in cache 'weather'

DEBUG org.springframework.cache - Cache hit for key 'Paris' in cache 'weather'
```

## Production Recommendations

### Use Redis Cache

For production, switch to Redis for:
- TTL support (auto-expiration)
- Distributed caching (multiple instances)
- Persistence across restarts
- Better memory management

#### 1. Add Redis Dependency

`pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

#### 2. Update Configuration

`application-prod.properties`:
```properties
# Redis cache configuration
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.redis.time-to-live=600000  # 10 minutes (in milliseconds)
```

#### 3. Update CacheConfig

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    
    // Weather cache: 10 minutes TTL
    cacheConfigurations.put("weather", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10)));
    
    // Air quality cache: 15 minutes TTL
    cacheConfigurations.put("airQuality", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15)));
    
    // Spotify metadata: 6 hours TTL
    cacheConfigurations.put("spotifyMetadata", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(6)));
    
    // City images: 24 hours TTL
    cacheConfigurations.put("cityImages", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24)));
    
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
}
```

### Alternative: Caffeine Cache

For in-process caching with TTL support (no Redis needed):

#### 1. Add Dependency

`pom.xml`:
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

#### 2. Configure

`application.properties`:
```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=10m
```

## Manual Cache Management

### Clear Cache Programmatically

```java
@Autowired
private CacheManager cacheManager;

// Clear specific cache
public void clearWeatherCache() {
    Cache cache = cacheManager.getCache("weather");
    if (cache != null) {
        cache.clear();
    }
}

// Clear specific entry
public void clearWeatherForCity(String cityName) {
    Cache cache = cacheManager.getCache("weather");
    if (cache != null) {
        cache.evict(cityName);
    }
}
```

### Cache Eviction Annotation

```java
@CacheEvict(value = "weather", key = "#cityName")
public void refreshWeatherData(String cityName) {
    // This will remove the cached entry
}

@CacheEvict(value = "weather", allEntries = true)
public void clearAllWeather() {
    // This will clear the entire weather cache
}
```

## Monitoring

### Cache Statistics

Enable cache metrics for production monitoring:

```properties
management.endpoints.web.exposure.include=caches
management.endpoint.caches.enabled=true
```

Access cache stats:
```bash
curl http://localhost:8080/actuator/caches
```

### Key Metrics to Track

- **Cache Hit Rate**: Target >80%
- **Cache Miss Rate**: Should decrease over time
- **Cache Size**: Monitor memory usage
- **Eviction Rate**: How often entries are removed

## Troubleshooting

### Cache Not Working

1. **Check @EnableCaching**:
   - Verify `CacheConfig.java` has `@EnableCaching`
   - Confirm configuration class is scanned by Spring

2. **Check Method Signature**:
   - Methods must be `public`
   - Methods must be called from another bean (not `this`)

3. **Check Logs**:
   ```properties
   logging.level.org.springframework.cache=DEBUG
   ```

### Stale Data Issues

If cached data becomes outdated:

1. **Implement TTL** (use Redis or Caffeine)
2. **Manual eviction** on data updates
3. **Cache warming** on startup for critical data

### Memory Issues

If cache grows too large:

1. **Add size limits**:
   ```java
   Caffeine.newBuilder()
       .maximumSize(10000)
       .build()
   ```

2. **Implement eviction policy**:
   - LRU (Least Recently Used)
   - LFU (Least Frequently Used)
   - Time-based expiration

## Best Practices

### ✅ Do's

- ✅ Cache stable data (city weather, images)
- ✅ Use descriptive cache names
- ✅ Include cache keys in method parameters
- ✅ Set appropriate TTLs based on data freshness
- ✅ Monitor cache hit rates
- ✅ Handle cache misses gracefully

### ❌ Don'ts

- ❌ Cache user-specific data without user ID in key
- ❌ Cache sensitive information without encryption
- ❌ Forget to handle null values
- ❌ Use caching for rapidly changing data
- ❌ Ignore memory limits
- ❌ Cache error responses

## Next Steps

1. **Monitor Performance**: Track cache hit rates in production
2. **Optimize TTLs**: Adjust based on actual data freshness needs
3. **Add Redis**: For production deployment with TTL support
4. **Cache Warming**: Pre-populate cache for popular cities
5. **Implement Eviction**: Clear cache on data updates

## Resources

- [Spring Cache Documentation](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Redis Cache Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.caching.provider.redis)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)

---

**Last Updated**: December 27, 2025  
**Cache Version**: 1.0 (Simple In-Memory)  
**Production Ready**: Requires Redis/Caffeine upgrade
