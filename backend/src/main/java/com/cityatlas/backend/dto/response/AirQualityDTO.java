package com.cityatlas.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@Builder
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
     * Create AirQualityDTO from OpenAQ API response
     * 
     * @param response OpenAQ API response
     * @return Mapped AirQualityDTO
     */
    public static AirQualityDTO fromOpenAqResponse(OpenAqResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return null;
        }
        
        // Take the first (most recent) result
        OpenAqResponse.Result result = response.getResults().get(0);
        
        AirQualityDTO.AirQualityDTOBuilder builder = AirQualityDTO.builder()
            .location(result.getLocation())
            .city(result.getCity())
            .country(result.getCountry());
        
        // Map coordinates
        if (result.getCoordinates() != null) {
            builder.latitude(result.getCoordinates().getLatitude())
                   .longitude(result.getCoordinates().getLongitude());
        }
        
        // Map measurements
        if (result.getMeasurements() != null) {
            Double maxPm25 = null;
            Double maxPm10 = null;
            String source = null;
            LocalDateTime lastUpdated = null;
            
            for (OpenAqResponse.Measurement measurement : result.getMeasurements()) {
                String parameter = measurement.getParameter();
                Double value = measurement.getValue();
                
                if (parameter != null && value != null) {
                    switch (parameter.toLowerCase()) {
                        case "pm25":
                            if (maxPm25 == null || value > maxPm25) {
                                maxPm25 = value;
                                builder.pm25(value);
                            }
                            break;
                        case "pm10":
                            if (maxPm10 == null || value > maxPm10) {
                                maxPm10 = value;
                                builder.pm10(value);
                            }
                            break;
                        case "o3":
                            builder.o3(value);
                            break;
                        case "no2":
                            builder.no2(value);
                            break;
                        case "so2":
                            builder.so2(value);
                            break;
                        case "co":
                            builder.co(value);
                            break;
                    }
                }
                
                // Get the most recent timestamp and source
                if (measurement.getLastUpdated() != null) {
                    if (lastUpdated == null || measurement.getLastUpdated().isAfter(lastUpdated)) {
                        lastUpdated = measurement.getLastUpdated();
                        source = measurement.getSourceName();
                    }
                }
            }
            
            builder.lastUpdated(lastUpdated);
            builder.source(source);
            
            // Calculate AQI from PM2.5 (primary pollutant in most cases)
            if (maxPm25 != null) {
                Integer aqi = calculateAqiFromPm25(maxPm25);
                builder.aqi(aqi);
                builder.aqiCategory(getAqiCategory(aqi));
                builder.primaryPollutant("PM2.5");
            } else if (maxPm10 != null) {
                // Fallback to PM10 if PM2.5 not available
                builder.primaryPollutant("PM10");
                // Simple approximation: PM10 AQI calculation similar to PM2.5
                Integer aqi = (int) Math.round(maxPm10 * 0.5);
                builder.aqi(aqi);
                builder.aqiCategory(getAqiCategory(aqi));
            }
        }
        
        return builder.build();
    }

    /**
     * OpenAQ API Response Structure
     * 
     * Internal class for deserializing the JSON response from OpenAQ API.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAqResponse {
        
        private List<Result> results;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Result {
            private String location;
            private String city;
            private String country;
            
            private Coordinates coordinates;
            
            private List<Measurement> measurements;
            
            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Coordinates {
                private Double latitude;
                private Double longitude;
            }
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Measurement {
            private String parameter;
            private Double value;
            private String unit;
            
            @JsonProperty("lastUpdated")
            private LocalDateTime lastUpdated;
            
            @JsonProperty("sourceName")
            private String sourceName;
        }
    }
}
