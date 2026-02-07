package com.cityatlas.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.cityatlas.backend.dto.response.AirQualityDTO;
import com.cityatlas.backend.dto.response.AnalyticsResponse;
import com.cityatlas.backend.dto.response.AnalyticsResponse.AQIDataPoint;
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

    /**
     * Build a complete CityResponse from real data sources.
     * Falls back to basic info if any source is unavailable.
     *
     * @param slug URL-friendly city identifier (e.g., "san-francisco")
     * @return CityResponse with real data, or best-effort partial data
     */
    @Cacheable(value = "cityData", key = "#slug", unless = "#result == null")
    public CityResponse buildCityResponse(String slug) {
        String cityName = convertSlugToName(slug);
        log.info("Building city response for: {} (slug: {})", cityName, slug);

        // Step 1: Get city-level data from GeoDB
        CityInfo cityInfo = geoDBCityService.findCity(cityName);

        String country;
        String countryCode;
        String state;
        Long population;
        Double latitude;
        Double longitude;

        if (cityInfo != null) {
            country = cityInfo.country();
            countryCode = cityInfo.countryCode();
            state = cityInfo.region();
            population = cityInfo.population();
            latitude = cityInfo.latitude();
            longitude = cityInfo.longitude();
            log.info("GeoDB data found for {}: pop={}, country={}", cityName, population, countryCode);
        } else {
            // Fallback: basic info only
            log.warn("No GeoDB data for {}. Using slug-derived name only.", cityName);
            country = null;
            countryCode = null;
            state = null;
            population = null;
            latitude = null;
            longitude = null;
        }

        // Step 2: Get country-level economic indicators from World Bank
        Double gdpPerCapita = null;
        Double unemploymentRate = null;
        Double literacyRate = null;
        Double pupilTeacherRatio = null;
        Double renewableEnergyPct = null;
        Double co2PerCapita = null;
        // Health indicators
        Double hospitalBedsPer1000 = null;
        Double healthExpenditurePerCapita = null;
        Double lifeExpectancy = null;
        // Infrastructure indicators
        Double internetUsersPct = null;
        Double mobileSubscriptionsPer100 = null;
        Double electricityAccessPct = null;

        java.util.List<String> languages = null;

        if (countryCode != null) {
            gdpPerCapita = worldBankService.fetchGdpPerCapita(countryCode);
            unemploymentRate = worldBankService.fetchUnemploymentRate(countryCode);
            literacyRate = worldBankService.fetchLiteracyRate(countryCode);
            pupilTeacherRatio = worldBankService.fetchPupilTeacherRatio(countryCode);
            renewableEnergyPct = worldBankService.fetchRenewableEnergyPct(countryCode);
            co2PerCapita = worldBankService.fetchCo2PerCapita(countryCode);

            // Health & Safety
            hospitalBedsPer1000 = worldBankService.fetchHospitalBeds(countryCode);
            healthExpenditurePerCapita = worldBankService.fetchHealthExpenditure(countryCode);
            lifeExpectancy = worldBankService.fetchLifeExpectancy(countryCode);

            // Infrastructure & Connectivity
            internetUsersPct = worldBankService.fetchInternetUsers(countryCode);
            mobileSubscriptionsPer100 = worldBankService.fetchMobileSubscriptions(countryCode);
            electricityAccessPct = worldBankService.fetchElectricityAccess(countryCode);

            log.info("World Bank data for {}: GDP/cap={}, unemployment={}%, literacy={}%, lifeExp={}", 
                countryCode, gdpPerCapita, unemploymentRate, literacyRate, lifeExpectancy);

            // Get languages from REST Countries
            CountryInfo countryInfo = restCountriesService.fetchCountry(countryCode);
            if (countryInfo != null && countryInfo.languages() != null) {
                languages = new ArrayList<>(countryInfo.languages());
            }
        }

        // Step 3: Fetch live weather from OpenWeatherMap (if configured)
        Double weatherTemp = null;
        String weatherDescription = null;
        String weatherIcon = null;
        Integer weatherHumidity = null;
        Double weatherWindSpeed = null;

        try {
            WeatherDTO weather = weatherService.fetchCurrentWeather(cityName).block();
            if (weather != null) {
                weatherTemp = weather.getTemperature();
                weatherDescription = weather.getWeatherDescription();
                weatherIcon = weather.getWeatherIcon();
                weatherHumidity = weather.getHumidity();
                weatherWindSpeed = weather.getWindSpeed();
                log.info("Weather for {}: {}°C, {}", cityName, weatherTemp, weatherDescription);
            }
        } catch (Exception e) {
            log.debug("Weather not available for {}: {}", cityName, e.getMessage());
        }

        // Step 4: Fetch live air quality from OpenAQ (if available)
        Integer airQualityIndex = null;
        String airQualityCategory = null;
        Double pm25 = null;

        try {
            AirQualityDTO aqi = airQualityService.fetchAirQuality(cityName).block();
            if (aqi != null) {
                airQualityIndex = aqi.getAqi();
                airQualityCategory = aqi.getAqiCategory();
                pm25 = aqi.getPm25();
                log.info("AQI for {}: {} ({})", cityName, airQualityIndex, airQualityCategory);
            }
        } catch (Exception e) {
            log.debug("AQI not available for {}: {}", cityName, e.getMessage());
        }

        // Step 5: Generate description from real data
        String description = generateDescription(cityName, state, country, population);

        return CityResponse.builder()
            .id(cityInfo != null ? cityInfo.id() : (long) slug.hashCode())
            .slug(slug)
            .name(cityName)
            .state(state)
            .country(country)
            .countryCode(countryCode)
            .population(population)
            .gdpPerCapita(gdpPerCapita != null ? Math.round(gdpPerCapita * 100.0) / 100.0 : null)
            .latitude(latitude)
            .longitude(longitude)
            .costOfLivingIndex(null)  // No free source available — intentionally null
            .unemploymentRate(unemploymentRate != null ? Math.round(unemploymentRate * 10.0) / 10.0 : null)
            .literacyRate(literacyRate != null ? Math.round(literacyRate * 10.0) / 10.0 : null)
            .pupilTeacherRatio(pupilTeacherRatio != null ? Math.round(pupilTeacherRatio * 10.0) / 10.0 : null)
            .renewableEnergyPct(renewableEnergyPct != null ? Math.round(renewableEnergyPct * 10.0) / 10.0 : null)
            .co2PerCapita(co2PerCapita != null ? Math.round(co2PerCapita * 100.0) / 100.0 : null)
            .languages(languages)
            // Health & Safety
            .hospitalBedsPer1000(hospitalBedsPer1000 != null ? Math.round(hospitalBedsPer1000 * 10.0) / 10.0 : null)
            .healthExpenditurePerCapita(healthExpenditurePerCapita != null ? Math.round(healthExpenditurePerCapita * 100.0) / 100.0 : null)
            .lifeExpectancy(lifeExpectancy != null ? Math.round(lifeExpectancy * 10.0) / 10.0 : null)
            // Infrastructure & Connectivity
            .internetUsersPct(internetUsersPct != null ? Math.round(internetUsersPct * 10.0) / 10.0 : null)
            .mobileSubscriptionsPer100(mobileSubscriptionsPer100 != null ? Math.round(mobileSubscriptionsPer100 * 10.0) / 10.0 : null)
            .electricityAccessPct(electricityAccessPct != null ? Math.round(electricityAccessPct * 10.0) / 10.0 : null)
            // Live Weather
            .weatherTemp(weatherTemp != null ? Math.round(weatherTemp * 10.0) / 10.0 : null)
            .weatherDescription(weatherDescription)
            .weatherIcon(weatherIcon)
            .weatherHumidity(weatherHumidity)
            .weatherWindSpeed(weatherWindSpeed != null ? Math.round(weatherWindSpeed * 10.0) / 10.0 : null)
            // Live Air Quality
            .airQualityIndex(airQualityIndex)
            .airQualityCategory(airQualityCategory)
            .pm25(pm25 != null ? Math.round(pm25 * 10.0) / 10.0 : null)
            .bannerImageUrl(null)     // Filled by Unsplash service separately
            .description(description)
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    /**
     * Build analytics response with real data where available.
     * Uses World Bank for population trends. AQI data comes from OpenAQ separately.
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

        // NOTE: AQI trend data comes from OpenAQ service (already integrated)
        // Job sectors and cost of living are REMOVED — no free city-level source

        return AnalyticsResponse.builder()
            .citySlug(slug)
            .cityName(cityName)
            .aqiTrend(List.of())         // Populated by OpenAQ — frontend will fetch separately
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
