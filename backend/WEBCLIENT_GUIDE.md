# WebClient Configuration Guide

This guide explains the WebClient setup for external API integrations in CityAtlas backend.

## Overview

The backend uses Spring WebFlux's `WebClient` for all external HTTP API calls. This provides:

- **Non-blocking I/O**: Better performance for concurrent API calls
- **Centralized configuration**: Timeouts, error handling, logging in one place
- **Reusability**: One bean, customizable per service
- **Modern API**: WebClient is the recommended replacement for RestTemplate

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     WebClient Bean                       │
│  (Shared configuration: timeouts, logging, errors)      │
└─────────────┬───────────────────────────────────────────┘
              │
              │ .mutate() - Create service-specific copies
              │
   ┌──────────┼──────────┬──────────┬──────────┐
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
Weather   OpenAQ    Spotify   Unsplash   (Future)
Service   Service   Service   Service    Services
```

## Components

### 1. WebClientConfig.java

Central configuration class that creates the WebClient bean.

**Key Features:**

#### Timeout Configuration
```java
// Connection timeout: Time to establish TCP connection
.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)

// Read timeout: Time to wait for response data
.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS))

// Response timeout: Overall request-response cycle
.responseTimeout(Duration.ofMillis(15000))
```

Values are read from `application.properties`:
```properties
cityatlas.external.timeout.connection-ms=5000
cityatlas.external.timeout.read-ms=10000
```

#### Request/Response Logging
```java
.filter(logRequest())   // Logs outgoing requests
.filter(logResponse())  // Logs incoming responses
```

Logs at DEBUG level to avoid cluttering production logs. Excludes sensitive headers (Authorization, API keys).

#### Error Handling
```java
.filter(handleErrors())  // Centralized error logging
```

- Logs 4xx client errors (bad request, auth failure) as WARN
- Logs 5xx server errors (API downtime) as ERROR
- Preserves errors for caller to handle (doesn't swallow them)

### 2. ExternalApiException.java

Custom exception for external API failures.

**Features:**
- Wraps HTTP status code
- Includes service name (OpenWeatherMap, Spotify, etc.)
- Helper methods: `isRetryable()`, `isClientError()`, `isServerError()`

**Usage:**
```java
.onStatus(HttpStatusCode::isError, response -> 
    response.bodyToMono(String.class)
        .flatMap(errorBody -> Mono.error(new ExternalApiException(
            "OpenWeatherMap",
            response.statusCode(),
            "Failed to fetch weather: " + errorBody
        )))
)
```

### 3. WeatherService.java (Example)

Reference implementation showing proper WebClient usage.

**Pattern:**
1. Validate configuration (check for placeholder API keys)
2. Clone shared WebClient with `.mutate()`
3. Customize with service-specific base URL
4. Build request with URI parameters
5. Handle errors
6. Add retry logic

## Usage Patterns

### Basic GET Request

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final WebClient webClient;
    private final ExternalApiConfig apiConfig;
    
    public Mono<MyData> fetchData(String id) {
        return webClient.mutate()
            .baseUrl("https://api.example.com")
            .build()
            .get()
            .uri("/data/{id}", id)
            .retrieve()
            .bodyToMono(MyData.class);
    }
}
```

### GET with Query Parameters

```java
.uri(uriBuilder -> uriBuilder
    .path("/search")
    .queryParam("q", searchTerm)
    .queryParam("limit", 10)
    .queryParam("apikey", apiConfig.getSomeService().getApiKey())
    .build())
```

### POST Request with Body

```java
.post()
.uri("/endpoint")
.bodyValue(requestObject)
.retrieve()
.bodyToMono(ResponseType.class)
```

### Adding Custom Headers

```java
.header("Authorization", "Bearer " + token)
.header("X-Custom-Header", "value")
```

### Error Handling

```java
.retrieve()
.onStatus(HttpStatusCode::isError, response -> 
    response.bodyToMono(String.class)
        .flatMap(errorBody -> Mono.error(new ExternalApiException(
            "ServiceName",
            response.statusCode(),
            errorBody
        )))
)
```

### Retry Logic

```java
.retryWhen(Retry.backoff(
    apiConfig.getRetry().getMaxAttempts(),  // 3 attempts
    Duration.ofMillis(apiConfig.getRetry().getBackoffMs())  // 1000ms backoff
)
.filter(throwable -> {
    // Only retry 5xx errors and network failures
    if (throwable instanceof ExternalApiException) {
        return ((ExternalApiException) throwable).isRetryable();
    }
    return true;
})
.doBeforeRetry(signal -> 
    log.warn("Retrying request (attempt {})", signal.totalRetries() + 1)
))
```

### Blocking vs Non-Blocking

```java
// Non-blocking (preferred for most cases)
Mono<WeatherData> weatherMono = weatherService.fetchCurrentWeather("Paris");
weatherMono.subscribe(
    data -> log.info("Got weather: {}", data),
    error -> log.error("Error: {}", error.getMessage())
);

// Blocking (use only when necessary, e.g., in tests or legacy code)
WeatherData weather = weatherService.fetchCurrentWeather("Paris")
    .block();  // ⚠️ Blocks current thread
```

## Creating a New External Service Integration

### Step 1: Add Configuration to ExternalApiConfig.java

```java
@Getter
@Setter
public static class MyServiceConfig {
    @NotBlank(message = "MyService API key is required")
    private String apiKey;
    
    @NotBlank(message = "MyService base URL is required")
    private String baseUrl;
    
    public boolean isPlaceholder() {
        return apiKey == null || apiKey.contains("placeholder");
    }
}
```

### Step 2: Add Properties to application.properties

```properties
# MyService API
cityatlas.external.myservice.api-key=${MYSERVICE_API_KEY:placeholder-myservice-key}
cityatlas.external.myservice.base-url=https://api.myservice.com/v1
```

### Step 3: Create Service Class

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MyService {
    
    private final WebClient webClient;
    private final ExternalApiConfig apiConfig;
    
    public Mono<MyData> fetchData(String id) {
        // Validate configuration
        if (apiConfig.getMyservice().isPlaceholder()) {
            return Mono.error(new ExternalApiException(
                "MyService",
                "API key not configured"
            ));
        }
        
        // Get config
        String apiKey = apiConfig.getMyservice().getApiKey();
        String baseUrl = apiConfig.getMyservice().getBaseUrl();
        
        // Make request
        return webClient.mutate()
            .baseUrl(baseUrl)
            .build()
            .get()
            .uri("/endpoint/{id}?apikey={key}", id, apiKey)
            .retrieve()
            .onStatus(HttpStatusCode::isError, response ->
                response.bodyToMono(String.class)
                    .flatMap(errorBody -> Mono.error(new ExternalApiException(
                        "MyService",
                        response.statusCode(),
                        errorBody
                    )))
            )
            .bodyToMono(MyData.class)
            .retryWhen(Retry.backoff(
                apiConfig.getRetry().getMaxAttempts(),
                Duration.ofMillis(apiConfig.getRetry().getBackoffMs())
            )
            .filter(throwable -> 
                throwable instanceof ExternalApiException &&
                ((ExternalApiException) throwable).isRetryable()
            ));
    }
}
```

## Configuration Reference

### application.properties

```properties
# Timeout Configuration
cityatlas.external.timeout.connection-ms=5000    # TCP connection timeout
cityatlas.external.timeout.read-ms=10000         # Response data timeout

# Retry Configuration
cityatlas.external.retry.max-attempts=3          # Number of retry attempts
cityatlas.external.retry.backoff-ms=1000         # Delay between retries
```

### Timeout Guidelines

| Scenario | Connection | Read | Notes |
|----------|-----------|------|-------|
| Fast APIs (< 1s response) | 3s | 5s | Most public APIs |
| Medium APIs (1-3s response) | 5s | 10s | Default |
| Slow APIs (3-10s response) | 5s | 15s | Large data transfers |
| Heavy processing | 5s | 30s | ML APIs, image processing |

### Retry Guidelines

| Error Type | Retry? | Max Attempts | Backoff |
|------------|--------|--------------|---------|
| 5xx Server Error | ✅ Yes | 3 | Exponential (1s → 2s → 4s) |
| Network Timeout | ✅ Yes | 3 | Exponential |
| 429 Rate Limit | ✅ Yes | 2 | Fixed (respect Retry-After header) |
| 4xx Client Error | ❌ No | 0 | Immediate failure |
| 401/403 Auth Error | ❌ No | 0 | Immediate failure |

## Monitoring & Debugging

### Enable Debug Logging

```properties
# Enable WebClient debug logs
logging.level.org.springframework.web.reactive.function.client=DEBUG

# Enable Netty debug logs (connection details)
logging.level.reactor.netty.http.client=DEBUG

# Enable our custom WebClient logs
logging.level.com.cityatlas.backend.config.WebClientConfig=DEBUG
```

### Common Issues

#### Issue: Timeouts

**Symptoms:** `ReadTimeoutException`, `ConnectTimeoutException`

**Solutions:**
- Increase timeout values in application.properties
- Check if external API is slow/overloaded
- Verify network connectivity

#### Issue: 401 Unauthorized

**Symptoms:** `ExternalApiException` with status 401

**Solutions:**
- Check API key is configured (not placeholder)
- Verify API key is valid on service provider's website
- Check if API key needs to be activated
- Ensure correct header format (e.g., "Bearer {token}" vs just token)

#### Issue: Rate Limiting (429)

**Symptoms:** `ExternalApiException` with status 429

**Solutions:**
- Implement caching to reduce API calls
- Add delays between requests
- Upgrade to paid tier with higher limits
- Check Retry-After header in response

#### Issue: Memory Issues

**Symptoms:** OutOfMemoryError with large responses

**Solutions:**
- Use streaming for large responses: `.bodyToFlux()` instead of `.bodyToMono()`
- Implement pagination
- Add response size limits

## Best Practices

### ✅ DO

- Clone WebClient with `.mutate()` for service-specific customization
- Validate configuration before making API calls
- Use Mono/Flux for reactive programming
- Handle errors explicitly with `.onStatus()`
- Add retry logic for transient failures
- Log errors with context (service name, status code)
- Use environment variables for API keys

### ❌ DON'T

- Don't modify the shared WebClient bean directly
- Don't log sensitive data (API keys, tokens)
- Don't block reactive streams unnecessarily (`.block()`)
- Don't retry 4xx client errors (except 429)
- Don't hardcode API keys in code
- Don't ignore timeout configuration

## Testing

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private ExternalApiConfig apiConfig;
    
    private WeatherService weatherService;
    
    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder().build();
        weatherService = new WeatherService(webClient, apiConfig);
    }
    
    @Test
    void fetchWeather_withValidCity_returnsWeatherData() {
        // Use WireMock or MockWebServer to mock HTTP responses
        // Test implementation...
    }
}
```

### Integration Test with WireMock

```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class WeatherServiceIntegrationTest {
    
    @Autowired
    private WeatherService weatherService;
    
    @Test
    void fetchWeather_integration() {
        stubFor(get(urlPathEqualTo("/weather"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"temp\": 20}")));
        
        StepVerifier.create(weatherService.fetchCurrentWeather("Paris"))
            .expectNextMatches(data -> data.contains("20"))
            .verifyComplete();
    }
}
```

## Additional Resources

- [Spring WebClient Documentation](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- [Project Reactor Documentation](https://projectreactor.io/docs/core/release/reference/)
- [Netty Documentation](https://netty.io/wiki/user-guide-for-4.x.html)
- [HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)
