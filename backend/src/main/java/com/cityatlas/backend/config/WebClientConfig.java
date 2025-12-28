package com.cityatlas.backend.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * WebClient Configuration for External API Calls
 * 
 * Provides a centralized, reusable WebClient bean for making HTTP requests to external services.
 * This configuration includes:
 * - Timeout management (connection, read, write)
 * - Centralized error handling and logging
 * - Request/response logging for debugging
 * - Retry capability (via separate retry configuration)
 * 
 * Design Decisions:
 * 1. **Reactive WebClient vs RestTemplate**: 
 *    - WebClient is non-blocking and more efficient for multiple concurrent API calls
 *    - Better integration with modern Spring ecosystem
 *    - RestTemplate is in maintenance mode
 * 
 * 2. **Shared vs Service-Specific WebClients**:
 *    - This bean provides a base WebClient that can be cloned per service
 *    - Each service can customize headers, base URLs, etc. via mutate()
 *    - Keeps common behavior (timeouts, error handling) consistent
 * 
 * 3. **Timeout Strategy**:
 *    - Connection timeout: How long to wait for TCP connection establishment
 *    - Read timeout: How long to wait for response data
 *    - Write timeout: How long to wait when sending request data
 *    - Values configured in application.properties (cityatlas.external.timeout)
 * 
 * 4. **Error Handling**:
 *    - Centralized logging of request failures
 *    - Preserves status codes for caller to handle
 *    - Logs detailed error info without exposing sensitive data
 * 
 * Usage Example:
 * <pre>
 * {@code
 * @Service
 * @RequiredArgsConstructor
 * public class WeatherService {
 *     private final WebClient webClient;
 *     private final ExternalApiConfig apiConfig;
 *     
 *     public Mono<WeatherData> fetchWeather(String city) {
 *         return webClient.mutate()
 *             .baseUrl(apiConfig.getOpenweather().getBaseUrl())
 *             .build()
 *             .get()
 *             .uri("/weather?q={city}&appid={apiKey}", 
 *                  city, apiConfig.getOpenweather().getApiKey())
 *             .retrieve()
 *             .bodyToMono(WeatherData.class);
 *     }
 * }
 * }
 * </pre>
 * 
 * @see WebClient
 * @see ExternalApiConfig
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebClientConfig {

    private final ExternalApiConfig apiConfig;

    /**
     * Create a reusable WebClient bean with configured timeouts and error handling
     * 
     * This WebClient can be injected into any service and customized via mutate():
     * - Add service-specific base URLs
     * - Add authentication headers
     * - Add custom filters
     * 
     * @return Configured WebClient instance
     */
    @Bean
    public WebClient webClient() {
        // Get timeout configuration from properties
        int connectionTimeoutMs = apiConfig.getTimeout().getConnectionMs();
        int readTimeoutMs = apiConfig.getTimeout().getReadMs();
        
        log.info("Initializing WebClient with timeouts: connection={}ms, read={}ms", 
            connectionTimeoutMs, readTimeoutMs);
        
        // Configure HTTP client with Netty reactor
        // Netty is the underlying async networking library used by WebClient
        HttpClient httpClient = HttpClient.create()
            // Connection timeout: How long to wait for TCP connection establishment
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
            
            // Response timeout: Maximum time for the entire request-response cycle
            // This is a safety net in case read timeout doesn't trigger
            .responseTimeout(Duration.ofMillis(readTimeoutMs + 5000))
            
            // Read and write timeouts: Applied at the I/O level
            .doOnConnected(conn -> conn
                // Read timeout: How long to wait for response data chunks
                .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                
                // Write timeout: How long to wait when sending request data
                // (less critical for GET requests, important for POST/PUT with large payloads)
                .addHandlerLast(new WriteTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
            );
        
        // Build WebClient with all configurations
        return WebClient.builder()
            // Use the configured HTTP client
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            
            // Default headers for all requests (can be overridden per request)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, "CityAtlas-Backend/1.0")
            
            // Add request logging filter (logs before request is sent)
            .filter(logRequest())
            
            // Add response logging filter (logs after response is received)
            .filter(logResponse())
            
            // Add error handling filter (centralized error processing)
            .filter(handleErrors())
            
            .build();
    }

    /**
     * Request Logging Filter
     * 
     * Logs outgoing HTTP requests for debugging and monitoring.
     * Logs: method, URL, headers (excluding sensitive data)
     * 
     * @return ExchangeFilterFunction for request logging
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // Only log at DEBUG level to avoid cluttering production logs
            if (log.isDebugEnabled()) {
                log.debug("External API Request: {} {}", 
                    clientRequest.method(), 
                    clientRequest.url());
                
                // Log headers but exclude sensitive ones (Authorization, API keys)
                clientRequest.headers().forEach((name, values) -> {
                    if (!name.equalsIgnoreCase("Authorization") && 
                        !name.toLowerCase().contains("key") &&
                        !name.toLowerCase().contains("secret")) {
                        log.debug("  Header: {}={}", name, values);
                    }
                });
            }
            
            return Mono.just(clientRequest);
        });
    }

    /**
     * Response Logging Filter
     * 
     * Logs incoming HTTP responses for debugging and monitoring.
     * Logs: status code, headers, response time
     * 
     * Design note: We log the response status but don't consume the body here,
     * as that would interfere with the actual response processing in the caller.
     * 
     * @return ExchangeFilterFunction for response logging
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("External API Response: status={}", clientResponse.statusCode());
                
                // Log response headers (useful for debugging rate limits, cache headers, etc.)
                clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
                    log.debug("  Response Header: {}={}", name, values);
                });
            }
            
            return Mono.just(clientResponse);
        });
    }

    /**
     * Error Handling Filter
     * 
     * Centralized error handling for all external API calls.
     * This filter:
     * - Logs errors with context (URL, status, error message)
     * - Preserves the error response for caller to handle
     * - Does NOT swallow errors (caller should handle with .onErrorResume())
     * 
     * Design decisions:
     * - We don't automatically retry here (use @Retryable in service layer for that)
     * - We don't transform errors into custom exceptions (keep it generic/reusable)
     * - We preserve all error details for caller to decide how to handle
     * 
     * HTTP Status Code Categories:
     * - 4xx: Client errors (bad request, auth failure, not found, etc.)
     * - 5xx: Server errors (API is down, timeout, internal error)
     * 
     * @return ExchangeFilterFunction for error handling
     */
    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            HttpStatusCode status = response.statusCode();
            
            // Check if this is an error response (4xx or 5xx)
            if (status.isError()) {
                // Log the error with context
                if (status.is4xxClientError()) {
                    // 4xx: Usually means our request was invalid (wrong API key, bad params, etc.)
                    log.warn("External API Client Error: status={}, reason={}", 
                        status.value(), 
                        getStatusReason(status));
                } else if (status.is5xxServerError()) {
                    // 5xx: API provider's problem (their server is down, overloaded, etc.)
                    log.error("External API Server Error: status={}, reason={}", 
                        status.value(), 
                        getStatusReason(status));
                }
                
                // For 4xx/5xx, we still return the response (don't throw here)
                // This allows the caller to:
                // 1. Access the error body for more details
                // 2. Decide whether to retry (for 5xx) or fail fast (for 4xx)
                // 3. Transform to custom exceptions as needed
            }
            
            return Mono.just(response);
        });
    }

    /**
     * Get human-readable reason for HTTP status code
     * 
     * @param status HTTP status code
     * @return Human-readable reason phrase
     */
    private String getStatusReason(HttpStatusCode status) {
        try {
            // Try to get the standard reason phrase
            return HttpStatus.valueOf(status.value()).getReasonPhrase();
        } catch (IllegalArgumentException e) {
            // If it's a non-standard status code, return the numeric value
            return "Unknown Status";
        }
    }
}
