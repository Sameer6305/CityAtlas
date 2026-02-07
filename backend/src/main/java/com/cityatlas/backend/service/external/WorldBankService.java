package com.cityatlas.backend.service.external;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * World Bank API Service — Free, no API key needed
 * 
 * Fetches country-level indicators from the World Bank Open Data API.
 * Base URL: https://api.worldbank.org/v2/
 * 
 * Supported indicators:
 * - SP.POP.TOTL — Total population
 * - NY.GDP.PCAP.CD — GDP per capita (current USD)
 * - SL.UEM.TOTL.ZS — Unemployment rate (% labor force)
 * - SE.ADT.LITR.ZS — Adult literacy rate
 * - SE.PRM.ENRL.TC.ZS — Pupil-teacher ratio, primary
 * - EG.FEC.RNEW.ZS — Renewable energy consumption (% of total)
 * - EN.ATM.CO2E.PC — CO2 emissions per capita (metric tons)
 * - SE.PRM.ENRR — School enrollment, primary (% gross)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorldBankService {

    private static final String BASE_URL = "https://api.worldbank.org/v2";
    
    private final WebClient webClient;

    /**
     * Fetch a single indicator value for a country (most recent year).
     *
     * @param countryCode ISO-3166-1 alpha-2 code (e.g., "US", "IN", "GB")
     * @param indicator   World Bank indicator code (e.g., "SP.POP.TOTL")
     * @return the most recent value, or null if no data
     */
    @Cacheable(value = "worldBankIndicator", key = "#countryCode + ':' + #indicator", unless = "#result == null")
    public Double fetchIndicator(String countryCode, String indicator) {
        try {
            List<?> raw = webClient.mutate()
                .baseUrl(BASE_URL)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/country/{code}/indicator/{indicator}")
                    .queryParam("format", "json")
                    .queryParam("per_page", "1")
                    .queryParam("mrv", "1")  // most recent value
                    .build(countryCode, indicator))
                .retrieve()
                .bodyToMono(List.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .timeout(Duration.ofSeconds(10))
                .block();

            if (raw == null || raw.size() < 2) {
                log.warn("No World Bank data for {}/{}", countryCode, indicator);
                return null;
            }

            List<?> dataRows = (List<?>) raw.get(1);
            if (dataRows == null || dataRows.isEmpty()) {
                log.warn("Empty data rows for {}/{}", countryCode, indicator);
                return null;
            }

            Map<?, ?> first = (Map<?, ?>) dataRows.get(0);
            Object value = first.get("value");
            if (value == null) {
                log.warn("Null value for {}/{}", countryCode, indicator);
                return null;
            }
            
            return ((Number) value).doubleValue();
        } catch (Exception e) {
            log.error("Failed to fetch World Bank indicator {}/{}: {}", countryCode, indicator, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch historical values for a country indicator (last N years).
     * Returns list ordered from oldest to newest.
     *
     * @param countryCode ISO alpha-2 code
     * @param indicator   World Bank indicator
     * @param years       number of years of history
     * @return list of YearValue entries
     */
    @Cacheable(value = "worldBankHistory", key = "#countryCode + ':' + #indicator + ':' + #years", unless = "#result == null || #result.isEmpty()")
    public List<YearValue> fetchIndicatorHistory(String countryCode, String indicator, int years) {
        try {
            List<?> raw = webClient.mutate()
                .baseUrl(BASE_URL)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/country/{code}/indicator/{indicator}")
                    .queryParam("format", "json")
                    .queryParam("per_page", String.valueOf(years))
                    .queryParam("mrv", String.valueOf(years))
                    .build(countryCode, indicator))
                .retrieve()
                .bodyToMono(List.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .timeout(Duration.ofSeconds(10))
                .block();

            if (raw == null || raw.size() < 2) {
                return List.of();
            }

            List<?> dataRows = (List<?>) raw.get(1);
            if (dataRows == null || dataRows.isEmpty()) {
                return List.of();
            }

            List<YearValue> results = new ArrayList<>();
            for (Object row : dataRows) {
                Map<?, ?> entry = (Map<?, ?>) row;
                Object value = entry.get("value");
                Object date = entry.get("date");
                if (value != null && date != null) {
                    results.add(new YearValue(date.toString(), ((Number) value).doubleValue()));
                }
            }

            // World Bank returns newest first; reverse for chronological
            java.util.Collections.reverse(results);
            return results;
        } catch (Exception e) {
            log.error("Failed to fetch World Bank history {}/{}: {}", countryCode, indicator, e.getMessage());
            return List.of();
        }
    }

    /**
     * Simple record for year+value pairs.
     */
    public record YearValue(String year, double value) {}

    // ============================================
    // Convenience methods for common indicators
    // ============================================

    /** Total population (country-level) */
    public Double fetchPopulation(String countryCode) {
        return fetchIndicator(countryCode, "SP.POP.TOTL");
    }

    /** GDP per capita in current USD */
    public Double fetchGdpPerCapita(String countryCode) {
        return fetchIndicator(countryCode, "NY.GDP.PCAP.CD");
    }

    /** Unemployment rate (% of total labor force) */
    public Double fetchUnemploymentRate(String countryCode) {
        return fetchIndicator(countryCode, "SL.UEM.TOTL.ZS");
    }

    /** Adult literacy rate (% of people ages 15+) */
    public Double fetchLiteracyRate(String countryCode) {
        return fetchIndicator(countryCode, "SE.ADT.LITR.ZS");
    }

    /** Pupil-teacher ratio, primary school */
    public Double fetchPupilTeacherRatio(String countryCode) {
        return fetchIndicator(countryCode, "SE.PRM.ENRL.TC.ZS");
    }

    /** Renewable energy consumption (% of total) */
    public Double fetchRenewableEnergyPct(String countryCode) {
        return fetchIndicator(countryCode, "EG.FEC.RNEW.ZS");
    }

    /** CO2 emissions per capita (metric tons) */
    public Double fetchCo2PerCapita(String countryCode) {
        return fetchIndicator(countryCode, "EN.ATM.CO2E.PC");
    }

    /** Population history (last N years) */
    public List<YearValue> fetchPopulationHistory(String countryCode, int years) {
        return fetchIndicatorHistory(countryCode, "SP.POP.TOTL", years);
    }

    // ============================================
    // Health & Safety indicators
    // ============================================

    /** Hospital beds per 1,000 people */
    public Double fetchHospitalBeds(String countryCode) {
        return fetchIndicator(countryCode, "SH.MED.BEDS.ZS");
    }

    /** Current health expenditure per capita (current USD) */
    public Double fetchHealthExpenditure(String countryCode) {
        return fetchIndicator(countryCode, "SH.XPD.CHEX.PC.CD");
    }

    /** Life expectancy at birth (years) */
    public Double fetchLifeExpectancy(String countryCode) {
        return fetchIndicator(countryCode, "SP.DYN.LE00.IN");
    }

    // ============================================
    // Infrastructure & Connectivity indicators
    // ============================================

    /** Internet users (% of population) */
    public Double fetchInternetUsers(String countryCode) {
        return fetchIndicator(countryCode, "IT.NET.USER.ZS");
    }

    /** Mobile cellular subscriptions per 100 people */
    public Double fetchMobileSubscriptions(String countryCode) {
        return fetchIndicator(countryCode, "IT.CEL.SETS.P2");
    }

    /** Access to electricity (% of population) */
    public Double fetchElectricityAccess(String countryCode) {
        return fetchIndicator(countryCode, "EG.ELC.ACCS.ZS");
    }
}
