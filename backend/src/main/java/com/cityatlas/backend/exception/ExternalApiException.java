package com.cityatlas.backend.exception;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;

/**
 * Exception for External API Call Failures
 * 
 * Thrown when external API calls fail (weather, air quality, Spotify, Unsplash, etc.)
 * This exception wraps the HTTP status code and error details for proper handling.
 * 
 * Design rationale:
 * - Separates external API failures from internal application errors
 * - Preserves HTTP status for proper retry logic (retry 5xx, don't retry 4xx)
 * - Includes service name for better error messages and monitoring
 * 
 * Usage Example:
 * <pre>
 * {@code
 * webClient.get()
 *     .uri("/weather?q={city}", city)
 *     .retrieve()
 *     .onStatus(HttpStatusCode::isError, response -> 
 *         response.bodyToMono(String.class)
 *             .flatMap(errorBody -> Mono.error(new ExternalApiException(
 *                 "OpenWeatherMap",
 *                 response.statusCode(),
 *                 "Failed to fetch weather: " + errorBody
 *             )))
 *     )
 *     .bodyToMono(WeatherData.class);
 * }
 * </pre>
 */
@Getter
public class ExternalApiException extends RuntimeException {

    /**
     * Name of the external service (e.g., "OpenWeatherMap", "Spotify")
     */
    private final String serviceName;
    
    /**
     * HTTP status code from the failed API call
     */
    private final HttpStatusCode statusCode;
    
    /**
     * HTTP status code value (e.g., 404, 500)
     */
    private final int statusValue;

    /**
     * Create an ExternalApiException with service name, status code, and message
     * 
     * @param serviceName Name of external service
     * @param statusCode HTTP status code
     * @param message Error message
     */
    public ExternalApiException(String serviceName, HttpStatusCode statusCode, String message) {
        super(String.format("[%s] API Error (HTTP %d): %s", 
            serviceName, statusCode.value(), message));
        this.serviceName = serviceName;
        this.statusCode = statusCode;
        this.statusValue = statusCode.value();
    }

    /**
     * Create an ExternalApiException with service name, status code, message, and cause
     * 
     * @param serviceName Name of external service
     * @param statusCode HTTP status code
     * @param message Error message
     * @param cause Underlying exception
     */
    public ExternalApiException(String serviceName, HttpStatusCode statusCode, String message, Throwable cause) {
        super(String.format("[%s] API Error (HTTP %d): %s", 
            serviceName, statusCode.value(), message), cause);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
        this.statusValue = statusCode.value();
    }

    /**
     * Create an ExternalApiException with service name and message (no status code)
     * Used for network errors, timeouts, etc. where no HTTP response was received
     * 
     * @param serviceName Name of external service
     * @param message Error message
     */
    public ExternalApiException(String serviceName, String message) {
        super(String.format("[%s] API Error: %s", serviceName, message));
        this.serviceName = serviceName;
        this.statusCode = null;
        this.statusValue = -1;
    }

    /**
     * Create an ExternalApiException with service name, message, and cause (no status code)
     * 
     * @param serviceName Name of external service
     * @param message Error message
     * @param cause Underlying exception
     */
    public ExternalApiException(String serviceName, String message, Throwable cause) {
        super(String.format("[%s] API Error: %s", serviceName, message), cause);
        this.serviceName = serviceName;
        this.statusCode = null;
        this.statusValue = -1;
    }

    /**
     * Check if this is a client error (4xx)
     * 
     * @return true if status is 4xx, false otherwise
     */
    public boolean isClientError() {
        return statusCode != null && statusCode.is4xxClientError();
    }

    /**
     * Check if this is a server error (5xx)
     * 
     * @return true if status is 5xx, false otherwise
     */
    public boolean isServerError() {
        return statusCode != null && statusCode.is5xxServerError();
    }

    /**
     * Check if this error is retryable
     * Generally, 5xx errors are retryable, 4xx errors are not
     * 
     * @return true if error might succeed on retry
     */
    public boolean isRetryable() {
        // Network errors (no status code) are retryable
        if (statusCode == null) {
            return true;
        }
        
        // 5xx server errors are retryable
        if (isServerError()) {
            return true;
        }
        
        // 429 Too Many Requests is retryable (after backoff)
        if (statusValue == 429) {
            return true;
        }
        
        // 408 Request Timeout is retryable
        if (statusValue == 408) {
            return true;
        }
        
        // 4xx client errors (except above) are NOT retryable
        return false;
    }
}
