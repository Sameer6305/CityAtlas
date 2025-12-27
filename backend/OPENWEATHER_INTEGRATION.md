# OpenWeather API Integration - Usage Guide

The OpenWeather API has been successfully integrated into CityAtlas backend.

## Features

✅ **Fetch current weather by city name**
✅ **Environment variable configuration** (OPENWEATHER_API_KEY)
✅ **Graceful degradation** when API key is missing
✅ **No application startup failure** if key is not configured
✅ **Proper error handling** and retry logic
✅ **Structured DTOs** for weather data

## Quick Start

### 1. Set API Key (Optional for Development)

The application will start and run without the API key. Weather endpoints will return 204 No Content.

To enable weather integration:

**Linux/macOS:**
```bash
export OPENWEATHER_API_KEY="your_actual_api_key_here"
```

**Windows PowerShell:**
```powershell
$env:OPENWEATHER_API_KEY="your_actual_api_key_here"
```

### 2. Start the Application

```bash
mvn spring-boot:run
```

Check the logs for weather service status:
- ✅ Configured: `OpenWeatherMap API: Configured (key length: 32 chars)`
- ⚠️ Not configured: `OpenWeatherMap API: Using PLACEHOLDER key`

### 3. Test the API

**Check service status:**
```bash
curl http://localhost:8080/api/weather/status
```

Response when configured:
```json
{
  "service": "OpenWeatherMap",
  "configured": true,
  "message": "Weather service is configured and ready"
}
```

Response when NOT configured:
```json
{
  "service": "OpenWeatherMap",
  "configured": false,
  "message": "Weather service not configured. Set OPENWEATHER_API_KEY environment variable."
}
```

**Fetch current weather:**
```bash
curl "http://localhost:8080/api/weather/current?city=Paris"
```

Response (200 OK):
```json
{
  "cityName": "Paris",
  "countryCode": "FR",
  "temperature": 15.5,
  "feelsLike": 14.2,
  "tempMin": 13.0,
  "tempMax": 17.0,
  "pressure": 1013,
  "humidity": 72,
  "weatherMain": "Clouds",
  "weatherDescription": "scattered clouds",
  "weatherIcon": "03d",
  "windSpeed": 3.5,
  "windDegrees": 220,
  "cloudiness": 40,
  "visibility": 10000,
  "timestamp": "2025-12-27T14:30:00",
  "sunrise": "2025-12-27T08:41:00",
  "sunset": "2025-12-27T17:08:00",
  "timezoneOffset": 3600
}
```

Response when API key not configured (204 No Content):
```
(Empty body)
```

## API Endpoints

### GET /api/weather/current

Fetch current weather for a city.

**Query Parameters:**
- `city` (required): City name (e.g., "Paris", "New York", "Tokyo")

**Response Codes:**
- `200 OK`: Weather data successfully fetched
- `204 No Content`: Weather service not configured (missing API key)
- `400 Bad Request`: City parameter missing
- `404 Not Found`: City not found in OpenWeatherMap
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: API call failed

**Examples:**
```bash
# Single word city
curl "http://localhost:8080/api/weather/current?city=Paris"

# Multi-word city (URL encode spaces)
curl "http://localhost:8080/api/weather/current?city=New%20York"

# City with country code (more accurate)
curl "http://localhost:8080/api/weather/current?city=Paris,FR"
curl "http://localhost:8080/api/weather/current?city=London,GB"
```

### GET /api/weather/status

Check if weather service is configured.

**Response:**
```json
{
  "service": "OpenWeatherMap",
  "configured": true/false,
  "message": "Status message"
}
```

## Code Usage

### In a Service

```java
@Service
@RequiredArgsConstructor
public class CityDataService {
    
    private final WeatherService weatherService;
    
    public Mono<CityDetails> getCityWithWeather(String cityName) {
        // Check if weather service is available
        if (!weatherService.isConfigured()) {
            log.warn("Weather service not configured, skipping weather data");
            return getCityDetailsWithoutWeather(cityName);
        }
        
        // Fetch weather (reactive)
        return weatherService.fetchCurrentWeather(cityName)
            .flatMap(weather -> {
                // Combine city details with weather
                return getCityDetails(cityName)
                    .map(city -> {
                        city.setWeather(weather);
                        return city;
                    });
            })
            .onErrorResume(error -> {
                // Graceful fallback if weather fetch fails
                log.warn("Failed to fetch weather, returning city without weather", error);
                return getCityDetailsWithoutWeather(cityName);
            });
    }
}
```

### Blocking Usage (Not Recommended)

```java
// Only use .block() when absolutely necessary (tests, legacy code)
WeatherDTO weather = weatherService.fetchCurrentWeather("Paris")
    .block();  // ⚠️ Blocks current thread
```

## WeatherDTO Structure

```java
WeatherDTO {
    String cityName;           // "Paris"
    String countryCode;        // "FR"
    Double temperature;        // 15.5 (Celsius)
    Double feelsLike;          // 14.2 (Celsius)
    Double tempMin;            // 13.0 (Celsius)
    Double tempMax;            // 17.0 (Celsius)
    Integer pressure;          // 1013 (hPa)
    Integer humidity;          // 72 (%)
    String weatherMain;        // "Clouds"
    String weatherDescription; // "scattered clouds"
    String weatherIcon;        // "03d" (for icon URL)
    Double windSpeed;          // 3.5 (m/s)
    Integer windDegrees;       // 220 (degrees)
    Integer cloudiness;        // 40 (%)
    Integer visibility;        // 10000 (meters)
    LocalDateTime timestamp;   // When data was calculated
    LocalDateTime sunrise;     // Sunrise time
    LocalDateTime sunset;      // Sunset time
    Integer timezoneOffset;    // 3600 (seconds from UTC)
}
```

### Weather Icons

Use the `weatherIcon` field to display icons:

```
Icon URL: http://openweathermap.org/img/wn/{weatherIcon}@2x.png

Examples:
- "01d" → Clear sky (day)
- "01n" → Clear sky (night)
- "02d" → Few clouds (day)
- "10d" → Rain (day)
- "13d" → Snow (day)
```

## Error Handling

### Scenario 1: API Key Not Configured

**Behavior:** Application starts normally, weather endpoints return 204 No Content

**Logs:**
```
⚠️  OpenWeatherMap API: Using PLACEHOLDER key (set OPENWEATHER_API_KEY env variable)
```

**Response:**
```
HTTP 204 No Content
(Empty body)
```

**Fix:** Set `OPENWEATHER_API_KEY` environment variable

### Scenario 2: Invalid API Key

**Behavior:** API call fails with 401 Unauthorized

**Response:**
```
HTTP 500 Internal Server Error
```

**Fix:** Verify API key at https://openweathermap.org/

### Scenario 3: City Not Found

**Behavior:** API returns 404

**Response:**
```
HTTP 404 Not Found
```

**Fix:** Check city name spelling or use city name with country code

### Scenario 4: Rate Limit Exceeded

**Behavior:** API returns 429

**Response:**
```
HTTP 429 Too Many Requests
```

**Fix:** 
- Implement caching (cache weather for 10-30 minutes)
- Upgrade OpenWeatherMap plan
- Reduce request frequency

## Configuration

### application.properties

```properties
# OpenWeatherMap API
cityatlas.external.openweather.api-key=${OPENWEATHER_API_KEY:placeholder-openweather-key}
cityatlas.external.openweather.base-url=https://api.openweathermap.org/data/2.5

# Timeout settings
cityatlas.external.timeout.connection-ms=5000
cityatlas.external.timeout.read-ms=10000

# Retry settings
cityatlas.external.retry.max-attempts=3
cityatlas.external.retry.backoff-ms=1000
```

### Environment Variables

```bash
# Required for weather integration
OPENWEATHER_API_KEY=your_api_key_here

# Optional overrides
OPENWEATHER_BASE_URL=https://api.openweathermap.org/data/2.5
```

## Rate Limits

**OpenWeatherMap Free Tier:**
- 1,000 API calls per day
- 60 API calls per minute

**Recommendations:**
- Cache weather data for at least 10 minutes
- Use city IDs instead of city names for better accuracy
- Monitor usage in OpenWeatherMap dashboard

## Testing

### Manual Test (with curl)

```bash
# 1. Set API key
export OPENWEATHER_API_KEY="your_key"

# 2. Start application
mvn spring-boot:run

# 3. Test endpoints
curl "http://localhost:8080/api/weather/status"
curl "http://localhost:8080/api/weather/current?city=Paris"
```

### Integration Test (with real API)

```java
@SpringBootTest
@ActiveProfiles("test")
class WeatherServiceIntegrationTest {
    
    @Autowired
    private WeatherService weatherService;
    
    @Test
    void fetchCurrentWeather_withRealApi() {
        // Only runs if API key is configured
        assumeTrue(weatherService.isConfigured());
        
        StepVerifier.create(weatherService.fetchCurrentWeather("Paris"))
            .assertNext(weather -> {
                assertNotNull(weather);
                assertEquals("Paris", weather.getCityName());
                assertNotNull(weather.getTemperature());
            })
            .verifyComplete();
    }
}
```

## Next Steps

### Caching

Add Spring Cache to reduce API calls:

```java
@Cacheable(value = "weather", key = "#cityName")
public Mono<WeatherDTO> fetchCurrentWeather(String cityName) {
    // Implementation...
}
```

### Database Integration

Store weather history:

```java
weatherService.fetchCurrentWeather(cityName)
    .doOnSuccess(weather -> weatherRepository.save(toEntity(weather)))
    .subscribe();
```

### Frontend Integration

```typescript
// Fetch weather for a city
async function getWeather(city: string) {
  const response = await fetch(`/api/weather/current?city=${city}`);
  
  if (response.status === 204) {
    console.warn('Weather service not configured');
    return null;
  }
  
  if (!response.ok) {
    throw new Error('Failed to fetch weather');
  }
  
  return await response.json();
}
```

## Troubleshooting

### Issue: No weather data returned

**Check:**
1. API key is set: `curl http://localhost:8080/api/weather/status`
2. City name is correct: Try with country code `Paris,FR`
3. Check logs for errors
4. Verify API key at https://openweathermap.org/

### Issue: Slow responses

**Solutions:**
- Implement caching
- Check network connectivity
- Increase timeout values in application.properties

### Issue: 429 Rate Limit

**Solutions:**
- Add caching (10-30 minute TTL)
- Reduce request frequency
- Upgrade OpenWeatherMap plan

## Resources

- [OpenWeatherMap API Documentation](https://openweathermap.org/api)
- [OpenWeatherMap Current Weather API](https://openweathermap.org/current)
- [Weather Condition Codes](https://openweathermap.org/weather-conditions)
- [Weather Icons](https://openweathermap.org/weather-conditions#How-to-get-icon-URL)
