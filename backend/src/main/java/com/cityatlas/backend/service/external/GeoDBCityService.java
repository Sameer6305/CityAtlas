package com.cityatlas.backend.service.external;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.util.retry.Retry;

/**
 * GeoDB Cities API Service — Free tier, no API key needed
 * 
 * Fetches city-level data from the GeoDB free endpoint.
 * Base URL: http://geodb-free-service.wirefreethought.com/v1/geo/
 * 
 * Free tier limits: ~1 request/second
 * 
 * Data available:
 * - City name, population, latitude, longitude
 * - Region/state, country, country code
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeoDBCityService {

    private static final String BASE_URL = "http://geodb-free-service.wirefreethought.com/v1/geo";

    private final WebClient webClient;

    /**
     * Search cities by name prefix. Returns top matches.
     *
     * @param namePrefix city name or prefix (e.g., "San Fran", "Austin")
     * @param limit      max results (1-10)
     * @return list of CityInfo records
     */
    @Cacheable(value = "geodbCitySearch", key = "#namePrefix + ':' + #limit", unless = "#result == null || #result.isEmpty()")
    public List<CityInfo> searchCities(String namePrefix, int limit) {
        try {
            Map<?, ?> response = webClient.mutate()
                .baseUrl(BASE_URL)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/cities")
                    .queryParam("namePrefix", namePrefix)
                    .queryParam("limit", String.valueOf(Math.min(limit, 10)))
                    .queryParam("sort", "-population")
                    .queryParam("types", "CITY")
                    .queryParam("minPopulation", "50000")
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)))
                .timeout(Duration.ofSeconds(15))
                .block();

            if (response == null || !response.containsKey("data")) {
                return List.of();
            }

            List<?> data = (List<?>) response.get("data");
            return data.stream()
                .map(this::parseCityInfo)
                .filter(c -> c != null)
                .toList();
        } catch (Exception e) {
            log.error("GeoDB search failed for '{}': {}", namePrefix, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get a single best-match city by name.
     * Searches for exact or best prefix match, returns the most populous result.
     *
     * @param cityName full city name (e.g., "San Francisco")
     * @return CityInfo or null if not found
     */
    @Cacheable(value = "geodbCity", key = "#cityName", unless = "#result == null")
    public CityInfo findCity(String cityName) {
        List<CityInfo> results = searchCities(cityName, 5);
        if (results.isEmpty()) {
            log.warn("No GeoDB results for city: {}", cityName);
            return null;
        }

        // Try exact name match first
        for (CityInfo city : results) {
            if (city.name().equalsIgnoreCase(cityName)) {
                return city;
            }
        }

        // Fall back to first (most populous) result
        return results.get(0);
    }

    private CityInfo parseCityInfo(Object raw) {
        try {
            Map<?, ?> entry = (Map<?, ?>) raw;
            return new CityInfo(
                getLong(entry, "id"),
                getString(entry, "name"),
                getString(entry, "region"),
                getString(entry, "country"),
                getString(entry, "countryCode"),
                getLong(entry, "population"),
                getDouble(entry, "latitude"),
                getDouble(entry, "longitude")
            );
        } catch (Exception e) {
            log.warn("Failed to parse GeoDB city entry: {}", e.getMessage());
            return null;
        }
    }

    private String getString(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private Long getLong(Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.longValue();
        return null;
    }

    private Double getDouble(Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    /**
     * City information from GeoDB.
     */
    public record CityInfo(
        Long id,
        String name,
        String region,
        String country,
        String countryCode,
        Long population,
        Double latitude,
        Double longitude
    ) {}
}
