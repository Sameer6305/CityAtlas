package com.cityatlas.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import com.cityatlas.backend.dto.response.AirQualityDTO;
import com.cityatlas.backend.dto.response.AnalyticsResponse;
import com.cityatlas.backend.dto.response.AnalyticsResponse.PopulationDataPoint;
import com.cityatlas.backend.dto.response.CityResponse;
import com.cityatlas.backend.dto.response.WeatherDTO;
import com.cityatlas.backend.service.external.AirQualityService;
import com.cityatlas.backend.service.external.GeoDBCityService;
import com.cityatlas.backend.service.external.GeoDBCityService.CityInfo;
import com.cityatlas.backend.service.external.RestCountriesService;
import com.cityatlas.backend.service.external.RestCountriesService.CountryInfo;
import com.cityatlas.backend.service.external.WeatherService;
import com.cityatlas.backend.service.external.WorldBankService;
import com.cityatlas.backend.service.external.WorldBankService.YearValue;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * City Data Aggregator Service
 * 
 * Combines data from multiple free APIs into unified CityResponse
 * and AnalyticsResponse objects for the frontend.
 * 
 * Data sources:
 * - GeoDB Cities API → City name, population, lat/lon, region, country
 * - World Bank API → GDP per capita, unemployment, literacy (country-level)
 * - REST Countries API → Languages, region info
 * 
 * All sources are free, no API keys required (GeoDB free tier rate-limited to ~1 req/sec).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CityDataAggregator {

    private final GeoDBCityService geoDBCityService;
    private final WorldBankService worldBankService;
    private final RestCountriesService restCountriesService;
    private final WeatherService weatherService;
    private final AirQualityService airQualityService;

    /** Thread pool for parallelizing external API calls (15+ calls per city request). */
    private final ExecutorService apiExecutor = Executors.newFixedThreadPool(
        Math.min(16, Runtime.getRuntime().availableProcessors() * 2)
    );

    @PreDestroy
    public void shutdown() {
        apiExecutor.shutdownNow();
    }

    /** Helper: safely get the result of a CompletableFuture, returning null on failure. */
    private <T> T safeGet(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (Exception e) {
            log.debug("Parallel fetch failed: {}", e.getMessage());
            return null;
        }
    }

    /** Helper: round a nullable Double to N decimal places. */
    private static Double round(Double val, int decimals) {
        if (val == null) return null;
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }

    /**
     * Build a complete CityResponse from real data sources.
     * All external API calls run in PARALLEL for maximum throughput.
     * Falls back to basic info if any source is unavailable.
     *
     * @param slug URL-friendly city identifier (e.g., "san-francisco")
     * @return CityResponse with real data, or best-effort partial data
     */
    @Cacheable(value = "cityData", key = "#slug", unless = "#result == null")
    public CityResponse buildCityResponse(String slug) {
        long startMs = System.currentTimeMillis();
        String cityName = convertSlugToName(slug);
        log.info("Building city response for: {} (slug: {})", cityName, slug);

        // ── Step 1: GeoDB (must complete first — provides countryCode & coordinates) ──
        CityInfo cityInfo = geoDBCityService.findCity(cityName);

        String country = null, countryCode = null, state = null;
        Long population = null;
        Double latitude = null, longitude = null;

        if (cityInfo != null) {
            country = cityInfo.country();
            countryCode = cityInfo.countryCode();
            state = cityInfo.region();
            population = cityInfo.population();
            latitude = cityInfo.latitude();
            longitude = cityInfo.longitude();
            log.info("GeoDB data found for {}: pop={}, country={}", cityName, population, countryCode);
        } else {
            log.warn("No GeoDB data for {}. Using slug-derived name only.", cityName);
        }

        // ── Step 2: Fire ALL remaining calls in PARALLEL ────────────────────────
        // World Bank (12 indicators), REST Countries (1), Weather (1), AQI (1) = 15 calls
        // Previously sequential (~3-4s) → now parallel (~0.5-1s)

        CompletableFuture<Double> fGdp = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fUnemployment = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fLiteracy = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fPupilTeacher = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fRenewable = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fCo2 = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fHospitalBeds = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fHealthExpenditure = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fLifeExpectancy = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fInternetUsers = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fMobileSubs = CompletableFuture.completedFuture(null);
        CompletableFuture<Double> fElectricity = CompletableFuture.completedFuture(null);
        CompletableFuture<List<String>> fLanguages = CompletableFuture.completedFuture(null);

        final String cc = countryCode; // effectively-final for lambdas
        if (cc != null) {
            fGdp = CompletableFuture.supplyAsync(() -> worldBankService.fetchGdpPerCapita(cc), apiExecutor);
            fUnemployment = CompletableFuture.supplyAsync(() -> worldBankService.fetchUnemploymentRate(cc), apiExecutor);
            fLiteracy = CompletableFuture.supplyAsync(() -> worldBankService.fetchLiteracyRate(cc), apiExecutor);
            fPupilTeacher = CompletableFuture.supplyAsync(() -> worldBankService.fetchPupilTeacherRatio(cc), apiExecutor);
            fRenewable = CompletableFuture.supplyAsync(() -> worldBankService.fetchRenewableEnergyPct(cc), apiExecutor);
            fCo2 = CompletableFuture.supplyAsync(() -> worldBankService.fetchCo2PerCapita(cc), apiExecutor);
            fHospitalBeds = CompletableFuture.supplyAsync(() -> worldBankService.fetchHospitalBeds(cc), apiExecutor);
            fHealthExpenditure = CompletableFuture.supplyAsync(() -> worldBankService.fetchHealthExpenditure(cc), apiExecutor);
            fLifeExpectancy = CompletableFuture.supplyAsync(() -> worldBankService.fetchLifeExpectancy(cc), apiExecutor);
            fInternetUsers = CompletableFuture.supplyAsync(() -> worldBankService.fetchInternetUsers(cc), apiExecutor);
            fMobileSubs = CompletableFuture.supplyAsync(() -> worldBankService.fetchMobileSubscriptions(cc), apiExecutor);
            fElectricity = CompletableFuture.supplyAsync(() -> worldBankService.fetchElectricityAccess(cc), apiExecutor);
            fLanguages = CompletableFuture.supplyAsync(() -> {
                CountryInfo info = restCountriesService.fetchCountry(cc);
                return (info != null && info.languages() != null) ? new ArrayList<>(info.languages()) : null;
            }, apiExecutor);
        }

        // Weather + AQI fire in parallel with World Bank calls
        final String cn = cityName;
        final Double lat = latitude, lon = longitude;
        CompletableFuture<WeatherDTO> fWeather = CompletableFuture.supplyAsync(() -> {
            try { return weatherService.fetchCurrentWeather(cn).block(); }
            catch (Exception e) { log.debug("Weather not available for {}: {}", cn, e.getMessage()); return null; }
        }, apiExecutor);

        CompletableFuture<AirQualityDTO> fAqi = CompletableFuture.supplyAsync(() -> {
            try {
                return (lat != null && lon != null)
                    ? airQualityService.fetchAirQualityByCoordinates(lat, lon, null).block()
                    : airQualityService.fetchAirQuality(cn).block();
            } catch (Exception e) { log.debug("AQI not available for {}: {}", cn, e.getMessage()); return null; }
        }, apiExecutor);

        // ── Step 3: Wait for ALL to complete ──
        CompletableFuture.allOf(
            fGdp, fUnemployment, fLiteracy, fPupilTeacher, fRenewable, fCo2,
            fHospitalBeds, fHealthExpenditure, fLifeExpectancy,
            fInternetUsers, fMobileSubs, fElectricity,
            fLanguages, fWeather, fAqi
        ).join();

        // ── Step 4: Collect results (null-safe) ──
        Double gdpPerCapita = safeGet(fGdp);
        Double unemploymentRate = safeGet(fUnemployment);
        Double literacyRate = safeGet(fLiteracy);
        Double pupilTeacherRatio = safeGet(fPupilTeacher);
        Double renewableEnergyPct = safeGet(fRenewable);
        Double co2PerCapita = safeGet(fCo2);
        Double hospitalBedsPer1000 = safeGet(fHospitalBeds);
        Double healthExpenditurePerCapita = safeGet(fHealthExpenditure);
        Double lifeExpectancy = safeGet(fLifeExpectancy);
        Double internetUsersPct = safeGet(fInternetUsers);
        Double mobileSubscriptionsPer100 = safeGet(fMobileSubs);
        Double electricityAccessPct = safeGet(fElectricity);
        List<String> languages = safeGet(fLanguages);

        WeatherDTO weather = safeGet(fWeather);
        Double weatherTemp = null;
        String weatherDescription = null, weatherIcon = null;
        Integer weatherHumidity = null;
        Double weatherWindSpeed = null;
        if (weather != null) {
            weatherTemp = weather.getTemperature();
            weatherDescription = weather.getWeatherDescription();
            weatherIcon = weather.getWeatherIcon();
            weatherHumidity = weather.getHumidity();
            weatherWindSpeed = weather.getWindSpeed();
        }

        AirQualityDTO aqi = safeGet(fAqi);
        Integer airQualityIndex = null;
        String airQualityCategory = null;
        Double pm25 = null;
        if (aqi != null) {
            airQualityIndex = aqi.getAqi();
            airQualityCategory = aqi.getAqiCategory();
            pm25 = aqi.getPm25();
        }

        if (cc != null) {
            log.info("World Bank data for {}: GDP/cap={}, unemployment={}%, literacy={}%, lifeExp={}",
                cc, gdpPerCapita, unemploymentRate, literacyRate, lifeExpectancy);
        }

        // ── Step 5: Assemble response ──
        String description = generateDescription(cityName, state, country, population);

        long elapsed = System.currentTimeMillis() - startMs;
        log.info("City response built for {} in {}ms (parallel)", cityName, elapsed);

        return CityResponse.builder()
            .id(cityInfo != null ? cityInfo.id() : (long) slug.hashCode())
            .slug(slug)
            .name(cityName)
            .state(state)
            .country(country)
            .countryCode(countryCode)
            .population(population)
            .gdpPerCapita(round(gdpPerCapita, 2))
            .latitude(latitude)
            .longitude(longitude)
            .costOfLivingIndex(null)
            .unemploymentRate(round(unemploymentRate, 1))
            .literacyRate(round(literacyRate, 1))
            .pupilTeacherRatio(round(pupilTeacherRatio, 1))
            .renewableEnergyPct(round(renewableEnergyPct, 1))
            .co2PerCapita(round(co2PerCapita, 2))
            .languages(languages)
            .hospitalBedsPer1000(round(hospitalBedsPer1000, 1))
            .healthExpenditurePerCapita(round(healthExpenditurePerCapita, 2))
            .lifeExpectancy(round(lifeExpectancy, 1))
            .internetUsersPct(round(internetUsersPct, 1))
            .mobileSubscriptionsPer100(round(mobileSubscriptionsPer100, 1))
            .electricityAccessPct(round(electricityAccessPct, 1))
            .weatherTemp(round(weatherTemp, 1))
            .weatherDescription(weatherDescription)
            .weatherIcon(weatherIcon)
            .weatherHumidity(weatherHumidity)
            .weatherWindSpeed(round(weatherWindSpeed, 1))
            .airQualityIndex(airQualityIndex)
            .airQualityCategory(airQualityCategory)
            .pm25(round(pm25, 1))
            .bannerImageUrl(null)
            .description(description)
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    /**
     * Build analytics response with real data where available.
     * Uses World Bank for population trends. AQI data comes from Open-Meteo separately.
     * Job sectors and cost of living are REMOVED (no free city-level source).
     *
     * @param slug city slug
     * @return AnalyticsResponse with real data
     */
    @Cacheable(value = "cityAnalytics", key = "#slug", unless = "#result == null")
    public AnalyticsResponse buildAnalyticsResponse(String slug) {
        String cityName = convertSlugToName(slug);

        // Get country code for World Bank queries
        CityInfo cityInfo = geoDBCityService.findCity(cityName);
        String countryCode = cityInfo != null ? cityInfo.countryCode() : null;

        // Population trend from World Bank (country-level, 10 years)
        List<PopulationDataPoint> populationTrend = new ArrayList<>();
        if (countryCode != null) {
            List<YearValue> history = worldBankService.fetchPopulationHistory(countryCode, 10);
            for (int i = 0; i < history.size(); i++) {
                YearValue yv = history.get(i);
                double popMillions = yv.value() / 1_000_000.0;
                double growthRate = 0.0;
                if (i > 0) {
                    double prev = history.get(i - 1).value();
                    growthRate = prev > 0 ? ((yv.value() - prev) / prev) * 100.0 : 0.0;
                }
                populationTrend.add(PopulationDataPoint.builder()
                    .year(yv.year())
                    .population(Math.round(popMillions * 100.0) / 100.0)
                    .growthRate(Math.round(growthRate * 100.0) / 100.0)
                    .build());
            }
        }

        // NOTE: AQI trend data comes from Open-Meteo service (already integrated)
        // Job sectors and cost of living are REMOVED — no free city-level source

        return AnalyticsResponse.builder()
            .citySlug(slug)
            .cityName(cityName)
            .aqiTrend(List.of())         // Populated by Open-Meteo — frontend will fetch separately
            .jobSectors(List.of())        // REMOVED — no free source
            .costOfLiving(List.of())      // REMOVED — no free source
            .populationTrend(populationTrend)
            .build();
    }

    /**
     * Fetch country-level education indicators for the environment/education pages.
     *
     * @param countryCode ISO alpha-2 code
     * @return EducationIndicators with World Bank data
     */
    public EducationIndicators fetchEducationIndicators(String countryCode) {
        if (countryCode == null) return new EducationIndicators(null, null, null);
        
        Double literacy = worldBankService.fetchLiteracyRate(countryCode);
        Double pupilTeacher = worldBankService.fetchPupilTeacherRatio(countryCode);
        Double renewableEnergy = worldBankService.fetchRenewableEnergyPct(countryCode);
        
        return new EducationIndicators(literacy, pupilTeacher, renewableEnergy);
    }

    public record EducationIndicators(
        Double literacyRate,
        Double pupilTeacherRatio,
        Double renewableEnergyPct
    ) {}

    /**
     * Generate a factual description from real city data.
     */
    private String generateDescription(String cityName, String state, String country, Long population) {
        StringBuilder sb = new StringBuilder();
        sb.append(cityName);

        if (state != null && country != null) {
            sb.append(" is a city in ").append(state).append(", ").append(country);
        } else if (country != null) {
            sb.append(" is a city in ").append(country);
        }

        if (population != null) {
            sb.append(" with a population of ").append(formatPopulation(population));
        }

        sb.append(".");
        return sb.toString();
    }

    private String formatPopulation(long pop) {
        if (pop >= 1_000_000) {
            return String.format("%.1fM", pop / 1_000_000.0);
        } else if (pop >= 1_000) {
            return String.format("%,d", pop);
        }
        return String.valueOf(pop);
    }

    /**
     * Convert URL slug to display name.
     * "san-francisco" → "San Francisco"
     */
    private String convertSlugToName(String slug) {
        String[] words = slug.split("-");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!name.isEmpty()) name.append(" ");
            name.append(word.substring(0, 1).toUpperCase())
                .append(word.substring(1).toLowerCase());
        }
        return name.toString();
    }
}
