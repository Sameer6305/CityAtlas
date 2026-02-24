package com.cityatlas.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Air Quality Data Transfer Object
 * 
 * Represents air quality information for a city, mapped from OpenAQ API response.
 * OpenAQ provides real-time air quality data from monitoring stations worldwide.
 * 
 * AQI (Air Quality Index) Categories:
 * - 0-50: Good (Green)
 * - 51-100: Moderate (Yellow)
 * - 101-150: Unhealthy for Sensitive Groups (Orange)
 * - 151-200: Unhealthy (Red)
 * - 201-300: Very Unhealthy (Purple)
 * - 301+: Hazardous (Maroon)
 * 
 * @see <a href="https://docs.openaq.org/">OpenAQ API Documentation</a>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AirQualityDTO {

    /**
     * Location/city name
     */
    private String location;
    
    /**
     * City name
     */
    private String city;
    
    /**
     * Country code (e.g., "US", "IN", "FR")
     */
    private String country;
    
    /**
     * Latitude coordinate
     */
    private Double latitude;
    
    /**
     * Longitude coordinate
     */
    private Double longitude;
    
    /**
     * Overall AQI (Air Quality Index) value
     * Calculated from primary pollutant
     */
    private Integer aqi;
    
    /**
     * AQI category (Good, Moderate, Unhealthy, etc.)
     */
    private String aqiCategory;
    
    /**
     * Primary pollutant (PM2.5, PM10, O3, NO2, SO2, CO)
     */
    private String primaryPollutant;
    
    /**
     * PM2.5 concentration (μg/m³)
     * Fine particulate matter < 2.5 micrometers
     */
    private Double pm25;
    
    /**
     * PM10 concentration (μg/m³)
     * Particulate matter < 10 micrometers
     */
    private Double pm10;
    
    /**
     * Ozone (O3) concentration (μg/m³)
     */
    private Double o3;
    
    /**
     * Nitrogen Dioxide (NO2) concentration (μg/m³)
     */
    private Double no2;
    
    /**
     * Sulfur Dioxide (SO2) concentration (μg/m³)
     */
    private Double so2;
    
    /**
     * Carbon Monoxide (CO) concentration (μg/m³)
     */
    private Double co;
    
    /**
     * Timestamp of the measurement
     */
    private LocalDateTime lastUpdated;
    
    /**
     * Data source/provider
     */
    private String source;

    /**
     * Get AQI category from numeric AQI value
     * 
     * @param aqi AQI numeric value
     * @return Category string
     */
    public static String getAqiCategory(Integer aqi) {
        if (aqi == null) return "Unknown";
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    /**
     * Calculate AQI from PM2.5 concentration using US EPA formula
     * 
     * @param pm25 PM2.5 concentration in μg/m³
     * @return AQI value
     */
    public static Integer calculateAqiFromPm25(Double pm25) {
        if (pm25 == null || pm25 < 0) return null;
        
        // US EPA AQI breakpoints for PM2.5 (24-hour average)
        if (pm25 <= 12.0) {
            return (int) Math.round((50.0 / 12.0) * pm25);
        } else if (pm25 <= 35.4) {
            return (int) Math.round(50 + ((100 - 50) / (35.4 - 12.1)) * (pm25 - 12.1));
        } else if (pm25 <= 55.4) {
            return (int) Math.round(100 + ((150 - 100) / (55.4 - 35.5)) * (pm25 - 35.5));
        } else if (pm25 <= 150.4) {
            return (int) Math.round(150 + ((200 - 150) / (150.4 - 55.5)) * (pm25 - 55.5));
        } else if (pm25 <= 250.4) {
            return (int) Math.round(200 + ((300 - 200) / (250.4 - 150.5)) * (pm25 - 150.5));
        } else {
            return (int) Math.round(300 + ((500 - 300) / (500.4 - 250.5)) * (pm25 - 250.5));
        }
    }

    /**
     * Create AirQualityDTO from OpenAQ API v3 response.
     *
     * v3 /measurements returns a flat list — each result is one parameter reading.
     * We scan for PM2.5 first, fall back to PM10 if absent.
     *
     * @param response OpenAQ v3 API response
     * @return Mapped AirQualityDTO, or null if no usable data
     */
    public static AirQualityDTO fromOpenAqResponse(OpenAqResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return null;
        }

        OpenAqResponse.V3Measurement first = response.getResults().get(0);

        AirQualityDTO.AirQualityDTOBuilder builder = AirQualityDTO.builder()
            .location(first.getLocation())
            .city(first.getCity())
            .country(first.getCountry())
            .source("OpenAQ v3");

        if (first.getCoordinates() != null) {
            builder.latitude(first.getCoordinates().getLatitude())
                   .longitude(first.getCoordinates().getLongitude());
        }

        // Scan flat list for PM2.5, PM10, NO2, O3, SO2, CO
        Double pm25 = null;
        Double pm10 = null;

        for (OpenAqResponse.V3Measurement m : response.getResults()) {
            if (m.getParameter() == null || m.getValue() == null || m.getValue() < 0) continue;
            switch (m.getParameter().toLowerCase()) {
                case "pm25": if (pm25 == null) pm25 = m.getValue(); builder.pm25(m.getValue()); break;
                case "pm10": if (pm10 == null) pm10 = m.getValue(); builder.pm10(m.getValue()); break;
                case "o3":   builder.o3(m.getValue());   break;
                case "no2":  builder.no2(m.getValue());  break;
                case "so2":  builder.so2(m.getValue());  break;
                case "co":   builder.co(m.getValue());   break;
                default: break;
            }
        }

        // Calculate AQI from PM2.5 (preferred), fall back to PM10
        if (pm25 != null) {
            Integer aqi = calculateAqiFromPm25(pm25);
            builder.aqi(aqi).aqiCategory(getAqiCategory(aqi)).primaryPollutant("PM2.5");
        } else if (pm10 != null) {
            Integer aqi = (int) Math.round(pm10 * 0.5);
            builder.aqi(aqi).aqiCategory(getAqiCategory(aqi)).primaryPollutant("PM10");
        } else {
            return null; // No usable pollutant data
        }

        return builder.build();
    }

    /**
     * OpenAQ API v3 Response Structure (flat measurements format)
     *
     * v3 /measurements endpoint returns one measurement object per result,
     * unlike v2 /latest which grouped measurements by location.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAqResponse {

        private List<V3Measurement> results;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class V3Measurement {
            /** Measurement ID */
            private Long id;
            /** Station/location ID */
            private Long locationId;
            /** Station name */
            private String location;
            /** Parameter name: "pm25", "pm10", "no2", "o3", "so2", "co" */
            private String parameter;
            /** Measured value in the corresponding unit */
            private Double value;
            /** Unit string (e.g. "\u00b5g/m\u00b3") */
            private String unit;
            /** Country ISO code */
            private String country;
            /** City name */
            private String city;
            /** Timestamp wrapper */
            private V3Date date;
            /** GPS coordinates of the station */
            private Coordinates coordinates;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class V3Date {
                private String utc;
                private String local;
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Coordinates {
                private Double latitude;
                private Double longitude;
            }
        }
    }
}
