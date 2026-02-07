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
 * REST Countries API Service — Free, no API key needed
 * 
 * Fetches country-level data from the REST Countries API.
 * Base URL: https://restcountries.com/v3.1/
 * 
 * Data available:
 * - Languages, currencies, capital
 * - Region, subregion, area, population
 * - Timezones, borders, maps
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestCountriesService {

    private static final String BASE_URL = "https://restcountries.com/v3.1";

    private final WebClient webClient;

    /**
     * Fetch country info by ISO alpha-2 code (e.g., "US", "IN", "GB").
     *
     * @param countryCode ISO-3166-1 alpha-2 code
     * @return CountryInfo or null if not found
     */
    @Cacheable(value = "restCountries", key = "#countryCode", unless = "#result == null")
    public CountryInfo fetchCountry(String countryCode) {
        try {
            List<?> raw = webClient.mutate()
                .baseUrl(BASE_URL)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/alpha/{code}")
                    .queryParam("fields", "name,population,languages,currencies,region,subregion,capital,area,timezones")
                    .build(countryCode))
                .retrieve()
                .bodyToMono(List.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .timeout(Duration.ofSeconds(10))
                .block();

            if (raw == null || raw.isEmpty()) {
                log.warn("No REST Countries data for: {}", countryCode);
                return null;
            }

            Map<?, ?> entry = (Map<?, ?>) raw.get(0);
            return parseCountryInfo(entry);
        } catch (Exception e) {
            log.error("Failed to fetch country info for {}: {}", countryCode, e.getMessage());
            return null;
        }
    }

    private CountryInfo parseCountryInfo(Map<?, ?> entry) {
        try {
            Map<?, ?> nameObj = (Map<?, ?>) entry.get("name");
            String commonName = nameObj != null ? getString(nameObj, "common") : null;

            Long population = entry.get("population") instanceof Number n ? n.longValue() : null;
            String region = getString(entry, "region");
            String subregion = getString(entry, "subregion");
            Double area = entry.get("area") instanceof Number n ? n.doubleValue() : null;

            // Parse languages (map of code -> name)
            List<String> languages = List.of();
            Object langObj = entry.get("languages");
            if (langObj instanceof Map<?, ?> langMap) {
                languages = langMap.values().stream()
                    .map(Object::toString)
                    .toList();
            }

            // Parse capital (list)
            String capital = null;
            Object capObj = entry.get("capital");
            if (capObj instanceof List<?> capList && !capList.isEmpty()) {
                capital = capList.get(0).toString();
            }

            // Parse timezones
            List<String> timezones = List.of();
            Object tzObj = entry.get("timezones");
            if (tzObj instanceof List<?> tzList) {
                timezones = tzList.stream().map(Object::toString).toList();
            }

            return new CountryInfo(commonName, population, region, subregion, capital, area, languages, timezones);
        } catch (Exception e) {
            log.warn("Failed to parse country info: {}", e.getMessage());
            return null;
        }
    }

    private String getString(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    /**
     * Country information from REST Countries API.
     */
    public record CountryInfo(
        String name,
        Long population,
        String region,
        String subregion,
        String capital,
        Double areaSqKm,
        List<String> languages,
        List<String> timezones
    ) {}
}
