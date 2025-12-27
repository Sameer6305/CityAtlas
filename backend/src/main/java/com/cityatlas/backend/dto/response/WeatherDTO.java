package com.cityatlas.backend.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Weather Data Transfer Object
 * 
 * Represents weather information for a city, mapped from OpenWeatherMap API response.
 * This is a simplified DTO exposing only the most relevant weather data.
 * 
 * OpenWeatherMap API response structure is complex with nested objects.
 * This DTO flattens the structure for easier consumption by frontend.
 * 
 * @see <a href="https://openweathermap.org/current">OpenWeatherMap Current Weather API</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherDTO {

    /**
     * City name
     */
    private String cityName;
    
    /**
     * Country code (e.g., "US", "FR", "IN")
     */
    private String countryCode;
    
    /**
     * Temperature in Celsius
     */
    private Double temperature;
    
    /**
     * "Feels like" temperature in Celsius
     */
    private Double feelsLike;
    
    /**
     * Minimum temperature in Celsius
     */
    private Double tempMin;
    
    /**
     * Maximum temperature in Celsius
     */
    private Double tempMax;
    
    /**
     * Atmospheric pressure (hPa)
     */
    private Integer pressure;
    
    /**
     * Humidity percentage
     */
    private Integer humidity;
    
    /**
     * Weather condition (e.g., "Clear", "Clouds", "Rain")
     */
    private String weatherMain;
    
    /**
     * Weather description (e.g., "clear sky", "few clouds")
     */
    private String weatherDescription;
    
    /**
     * Weather icon code (e.g., "01d", "02n")
     */
    private String weatherIcon;
    
    /**
     * Wind speed in meters/second
     */
    private Double windSpeed;
    
    /**
     * Wind direction in degrees
     */
    private Integer windDegrees;
    
    /**
     * Cloudiness percentage
     */
    private Integer cloudiness;
    
    /**
     * Visibility in meters
     */
    private Integer visibility;
    
    /**
     * Timestamp when weather data was calculated
     */
    private LocalDateTime timestamp;
    
    /**
     * Sunrise time
     */
    private LocalDateTime sunrise;
    
    /**
     * Sunset time
     */
    private LocalDateTime sunset;
    
    /**
     * Timezone offset in seconds
     */
    private Integer timezoneOffset;

    /**
     * Create WeatherDTO from OpenWeatherMap API response
     * 
     * @param response OpenWeatherMap API response
     * @return Mapped WeatherDTO
     */
    public static WeatherDTO fromOpenWeatherResponse(OpenWeatherResponse response) {
        if (response == null) {
            return null;
        }
        
        WeatherDTO.WeatherDTOBuilder builder = WeatherDTO.builder()
            .cityName(response.getName())
            .timezoneOffset(response.getTimezone());
        
        // Map coordinates and country
        if (response.getSys() != null) {
            builder.countryCode(response.getSys().getCountry());
            
            // Convert sunrise/sunset from Unix timestamp to LocalDateTime
            if (response.getSys().getSunrise() != null) {
                builder.sunrise(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(response.getSys().getSunrise()),
                    ZoneId.systemDefault()
                ));
            }
            if (response.getSys().getSunset() != null) {
                builder.sunset(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(response.getSys().getSunset()),
                    ZoneId.systemDefault()
                ));
            }
        }
        
        // Map main weather data (temperature, pressure, humidity)
        if (response.getMain() != null) {
            builder.temperature(response.getMain().getTemp())
                .feelsLike(response.getMain().getFeelsLike())
                .tempMin(response.getMain().getTempMin())
                .tempMax(response.getMain().getTempMax())
                .pressure(response.getMain().getPressure())
                .humidity(response.getMain().getHumidity());
        }
        
        // Map weather condition (first element, as API returns array)
        if (response.getWeather() != null && !response.getWeather().isEmpty()) {
            OpenWeatherResponse.Weather weather = response.getWeather().get(0);
            builder.weatherMain(weather.getMain())
                .weatherDescription(weather.getDescription())
                .weatherIcon(weather.getIcon());
        }
        
        // Map wind data
        if (response.getWind() != null) {
            builder.windSpeed(response.getWind().getSpeed())
                .windDegrees(response.getWind().getDeg());
        }
        
        // Map clouds
        if (response.getClouds() != null) {
            builder.cloudiness(response.getClouds().getAll());
        }
        
        // Map visibility
        builder.visibility(response.getVisibility());
        
        // Convert timestamp
        if (response.getDt() != null) {
            builder.timestamp(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(response.getDt()),
                ZoneId.systemDefault()
            ));
        }
        
        return builder.build();
    }

    /**
     * OpenWeatherMap API Response Structure
     * 
     * This internal class maps the raw JSON response from OpenWeatherMap API.
     * It's used for deserialization only and should not be exposed to other layers.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenWeatherResponse {
        
        private String name;
        private Integer timezone;
        private Long dt;
        private Integer visibility;
        
        @JsonProperty("main")
        private Main main;
        
        @JsonProperty("weather")
        private java.util.List<Weather> weather;
        
        @JsonProperty("wind")
        private Wind wind;
        
        @JsonProperty("clouds")
        private Clouds clouds;
        
        @JsonProperty("sys")
        private Sys sys;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Main {
            private Double temp;
            
            @JsonProperty("feels_like")
            private Double feelsLike;
            
            @JsonProperty("temp_min")
            private Double tempMin;
            
            @JsonProperty("temp_max")
            private Double tempMax;
            
            private Integer pressure;
            private Integer humidity;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Weather {
            private String main;
            private String description;
            private String icon;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Wind {
            private Double speed;
            private Integer deg;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Clouds {
            private Integer all;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Sys {
            private String country;
            private Long sunrise;
            private Long sunset;
        }
    }
}
