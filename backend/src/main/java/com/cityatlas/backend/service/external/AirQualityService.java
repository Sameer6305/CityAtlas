package com.cityatlas.backend.service.external;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.cityatlas.backend.config.ExternalApiConfig;
import com.cityatlas.backend.dto.response.AirQualityDTO;
import com.cityatlas.backend.exception.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Air Quality Service — Open-Meteo Air Quality API
 *
 * Uses the completely free Open-Meteo air quality API backed by the
 * Copernicus Atmosphere Monitoring Service (CAMS) — no API key or account needed.
 *
 * Data provided: PM2.5, PM10, NO2, SO2, O3, CO, European AQI
 *
 * Endpoints used:
 *  - Air quality: https://air-quality-api.open-meteo.com/v1/air-quality
 *  - City geocoding: https://geocoding-api.open-meteo.com/v1/search
 *
 * @see <a href="https://open-meteo.com/en/docs/air-quality-api">Open-Meteo Air Quality API</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AirQualityService {

    private static final String OPEN_METEO_AQ_BASE  = "https://air-quality-api.open-meteo.com/v1";
    private static final String OPEN_METEO_GEO_BASE = "https://geocoding-api.open-meteo.com/v1";

    /** Comma-separated fields to request from the current-conditions endpoint */
    private static final String AQ_PARAMS =
        "pm10,pm2_5,nitrogen_dioxide,sulphur_dioxide,ozone,carbon_monoxide,european_aqi";

    private final WebClient webClient;
    private final ExternalApiConfig apiConfig; // used for retry/timeout config only

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Fetch air quality for a city name.
     * Resolves coordinates via Open-Meteo geocoding, then queries air-quality endpoint.
     * Cached by city name for 15 minutes (see CacheConfig).
     */
    @Cacheable(value = "airQuality", key = "#cityName", unless = "#result == null")
    public Mono<AirQualityDTO> fetchAirQuality(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return Mono.error(new IllegalArgumentException("City name cannot be null or empty"));
        }
        log.debug("Fetching air quality for city: {}", cityName);

        return geocodeCity(cityName)
            .flatMap(geo ->
                fetchFromOpenMeteo(geo.getLatitude(), geo.getLongitude())
                    .map(dto -> dto.toBuilder()
                        .location(geo.getName())
                        .city(geo.getName())
                        .country(geo.getCountryCode())
                        .latitude(geo.getLatitude())
                        .longitude(geo.getLongitude())
                        .build()))
            .doOnSuccess(r -> {
                if (r != null) log.info("AQI for {}: {} ({})", cityName, r.getAqi(), r.getAqiCategory());
            })
            .onErrorResume(e -> {
                log.warn("Air quality unavailable for '{}': {}", cityName, e.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Fetch air quality directly by coordinates — preferred path (no geocoding needed).
     *
     * @param radiusKm ignored — Open-Meteo interpolates from nearest station automatically
     */
    public Mono<AirQualityDTO> fetchAirQualityByCoordinates(
            Double latitude, Double longitude, Integer radiusKm) {

        if (latitude == null || longitude == null) {
            return Mono.error(new IllegalArgumentException("Coordinates cannot be null"));
        }
        log.debug("Fetching air quality for coordinates: {}, {}", latitude, longitude);

        return fetchFromOpenMeteo(latitude, longitude)
            .map(dto -> dto.toBuilder().latitude(latitude).longitude(longitude).build())
            .doOnSuccess(r -> {
                if (r != null) log.info("AQI at ({},{}): {} ({})", latitude, longitude, r.getAqi(), r.getAqiCategory());
            })
            .onErrorResume(e -> {
                log.warn("Air quality unavailable at ({},{}): {}", latitude, longitude, e.getMessage());
                return Mono.empty();
            });
    }

    /** Always true — Open-Meteo requires no API key. */
    public boolean isConfigured() { return true; }

    /** Open-Meteo is a free/no-key API — always false. */
    public boolean hasApiKey() { return false; }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Mono<GeoResult> geocodeCity(String cityName) {
        return webClient.mutate().baseUrl(OPEN_METEO_GEO_BASE).build()
            .get()
            .uri(u -> u.path("/search")
                .queryParam("name", cityName)
                .queryParam("count", "1")
                .queryParam("language", "en")
                .queryParam("format", "json")
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                .flatMap(b -> Mono.error(new ExternalApiException("Open-Meteo Geocoding", r.statusCode(), b))))
            .bodyToMono(GeocodingResponse.class)
            .flatMap(resp -> {
                if (resp == null || resp.getResults() == null || resp.getResults().isEmpty()) {
                    return Mono.error(new RuntimeException("No geocoding result for: " + cityName));
                }
                return Mono.just(resp.getResults().get(0));
            })
            .retryWhen(retrySpec());
    }

    private Mono<AirQualityDTO> fetchFromOpenMeteo(double lat, double lon) {
        return webClient.mutate().baseUrl(OPEN_METEO_AQ_BASE).build()
            .get()
            .uri(u -> u.path("/air-quality")
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current", AQ_PARAMS)
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                .flatMap(b -> Mono.error(new ExternalApiException("Open-Meteo AQ", r.statusCode(), b))))
            .bodyToMono(OpenMeteoAqResponse.class)
            .flatMap(r -> {
                AirQualityDTO dto = mapToDto(r);
                return dto != null ? Mono.just(dto) : Mono.empty();
            })
            .retryWhen(retrySpec());
    }

    private static AirQualityDTO mapToDto(OpenMeteoAqResponse r) {
        if (r == null || r.getCurrent() == null) return null;
        OpenMeteoAqResponse.Current c = r.getCurrent();

        Double pm25 = c.getPm2_5();
        Double pm10 = c.getPm10();
        Integer europeanAqi = c.getEuropean_aqi();

        Integer aqi;
        String primaryPollutant;

        if (europeanAqi != null && europeanAqi >= 0) {
            // Use European AQI directly — most accurate, from CAMS satellites
            aqi = europeanAqi;
            primaryPollutant = pm25 != null ? "PM2.5" : (pm10 != null ? "PM10" : "AQI");
        } else if (pm25 != null) {
            aqi = AirQualityDTO.calculateAqiFromPm25(pm25);
            primaryPollutant = "PM2.5";
        } else if (pm10 != null) {
            aqi = (int) Math.round(pm10 * 0.5);
            primaryPollutant = "PM10";
        } else {
            return null;
        }

        return AirQualityDTO.builder()
            .pm25(pm25)
            .pm10(pm10)
            .no2(c.getNitrogen_dioxide())
            .so2(c.getSulphur_dioxide())
            .o3(c.getOzone())
            .co(c.getCarbon_monoxide())
            .aqi(aqi)
            .aqiCategory(AirQualityDTO.getAqiCategory(aqi))
            .primaryPollutant(primaryPollutant)
            .source("Open-Meteo / CAMS")
            .build();
    }

    private Retry retrySpec() {
        return Retry.backoff(
                apiConfig.getRetry().getMaxAttempts(),
                Duration.ofMillis(apiConfig.getRetry().getBackoffMs()))
            .filter(t -> !(t instanceof ExternalApiException)
                || ((ExternalApiException) t).isRetryable());
    }

    // =========================================================================
    // Response POJOs — Open-Meteo
    // =========================================================================

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenMeteoAqResponse {
        private Double latitude;
        private Double longitude;
        private Current current;

        @Data
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Current {
            private Double pm10;
            private Double pm2_5;
            private Double carbon_monoxide;
            private Double nitrogen_dioxide;
            private Double sulphur_dioxide;
            private Double ozone;
            private Integer european_aqi;
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeocodingResponse {
        private List<GeoResult> results;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeoResult {
        private String name;
        private Double latitude;
        private Double longitude;
        private String country;
        @JsonProperty("country_code")
        private String countryCode;
        private String admin1;
    }
}
