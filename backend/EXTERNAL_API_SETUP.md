# External API Setup Guide

This guide explains how to configure external API integrations for CityAtlas backend.

## Overview

CityAtlas integrates with the following external services:

| Service | Purpose | Documentation |
|---------|---------|---------------|
| **OpenWeatherMap** | Weather data, forecasts, air quality | https://openweathermap.org/api |
| **OpenAQ** | Real-time air quality monitoring | https://openaq.org/ |
| **Spotify** | Music scene, cultural events | https://developer.spotify.com/ |
| **Unsplash** | High-quality city imagery | https://unsplash.com/developers |

## Security Requirements

⚠️ **CRITICAL**: Never commit real API keys to version control!

- Use environment variables for all API keys
- Placeholder values in `application.properties` are for development only
- Real keys must be set in production environment

## Configuration

### 1. Sign Up for API Keys

Register for each service and obtain your API credentials:

#### OpenWeatherMap
1. Visit https://openweathermap.org/api
2. Create an account
3. Navigate to "API keys" section
4. Generate a new API key
5. Copy the key (format: `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)

#### OpenAQ
1. Visit https://openaq.org/
2. Create an account
3. Request API access
4. Copy your API key

#### Spotify
1. Visit https://developer.spotify.com/dashboard
2. Log in with Spotify account
3. Create a new app
4. Copy the **Client ID** and **Client Secret**

#### Unsplash
1. Visit https://unsplash.com/developers
2. Create an account
3. Register a new application
4. Copy the **Access Key**

### 2. Set Environment Variables

#### Linux/macOS

Add to your `~/.bashrc`, `~/.zshrc`, or `~/.profile`:

```bash
export OPENWEATHER_API_KEY="your_openweather_key_here"
export OPENAQ_API_KEY="your_openaq_key_here"
export SPOTIFY_CLIENT_ID="your_spotify_client_id_here"
export SPOTIFY_CLIENT_SECRET="your_spotify_client_secret_here"
export UNSPLASH_ACCESS_KEY="your_unsplash_access_key_here"
```

Then reload: `source ~/.bashrc`

#### Windows

PowerShell:
```powershell
$env:OPENWEATHER_API_KEY="your_openweather_key_here"
$env:OPENAQ_API_KEY="your_openaq_key_here"
$env:SPOTIFY_CLIENT_ID="your_spotify_client_id_here"
$env:SPOTIFY_CLIENT_SECRET="your_spotify_client_secret_here"
$env:UNSPLASH_ACCESS_KEY="your_unsplash_access_key_here"
```

Command Prompt:
```cmd
set OPENWEATHER_API_KEY=your_openweather_key_here
set OPENAQ_API_KEY=your_openaq_key_here
set SPOTIFY_CLIENT_ID=your_spotify_client_id_here
set SPOTIFY_CLIENT_SECRET=your_spotify_client_secret_here
set UNSPLASH_ACCESS_KEY=your_unsplash_access_key_here
```

#### Docker

Create a `.env` file (add to `.gitignore`!):

```env
OPENWEATHER_API_KEY=your_openweather_key_here
OPENAQ_API_KEY=your_openaq_key_here
SPOTIFY_CLIENT_ID=your_spotify_client_id_here
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret_here
UNSPLASH_ACCESS_KEY=your_unsplash_access_key_here
```

Docker Compose:
```yaml
services:
  backend:
    image: cityatlas-backend
    env_file:
      - .env
```

#### Kubernetes

Create a secret:
```bash
kubectl create secret generic cityatlas-api-keys \
  --from-literal=OPENWEATHER_API_KEY=your_key \
  --from-literal=OPENAQ_API_KEY=your_key \
  --from-literal=SPOTIFY_CLIENT_ID=your_id \
  --from-literal=SPOTIFY_CLIENT_SECRET=your_secret \
  --from-literal=UNSPLASH_ACCESS_KEY=your_key
```

Reference in deployment:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cityatlas-backend
spec:
  template:
    spec:
      containers:
      - name: backend
        envFrom:
        - secretRef:
            name: cityatlas-api-keys
```

### 3. Verify Configuration

Start the application and check the logs:

```bash
mvn spring-boot:run
```

You should see:
```
=================================================================
EXTERNAL API CONFIGURATION VALIDATION
=================================================================
✅ OpenWeatherMap API: Configured (key length: 32 chars)
✅ OpenAQ API: Configured (key length: 40 chars)
✅ Spotify API: Configured (client ID length: 32 chars)
✅ Unsplash API: Configured (key length: 43 chars)
📊 Retry Config: max-attempts=3, backoff=1000ms
⏱️  Timeout Config: connection=5000ms, read=10000ms
=================================================================
✅ All external API keys configured successfully
=================================================================
```

If you see warnings like:
```
⚠️  OpenWeatherMap API: Using PLACEHOLDER key (set OPENWEATHER_API_KEY env variable)
```

This means the environment variable is not set, and the placeholder value is being used.

## Usage in Code

### Inject Configuration

```java
@Service
@RequiredArgsConstructor
public class WeatherService {
    
    private final ExternalApiConfig apiConfig;
    
    public WeatherData fetchWeather(String cityName) {
        String apiKey = apiConfig.getOpenweather().getApiKey();
        String baseUrl = apiConfig.getOpenweather().getBaseUrl();
        
        // Check if using placeholder
        if (apiConfig.getOpenweather().isPlaceholder()) {
            throw new IllegalStateException("OpenWeatherMap API key not configured");
        }
        
        // Make API call
        String url = String.format("%s/weather?q=%s&appid=%s", baseUrl, cityName, apiKey);
        // ... rest of implementation
    }
}
```

### Access Nested Configuration

```java
// OpenWeatherMap
String weatherApiKey = apiConfig.getOpenweather().getApiKey();
String weatherBaseUrl = apiConfig.getOpenweather().getBaseUrl();

// OpenAQ
String aqApiKey = apiConfig.getOpenaq().getApiKey();
String aqBaseUrl = apiConfig.getOpenaq().getBaseUrl();

// Spotify
String spotifyClientId = apiConfig.getSpotify().getClientId();
String spotifySecret = apiConfig.getSpotify().getClientSecret();
String spotifyAuthUrl = apiConfig.getSpotify().getAuthUrl();

// Unsplash
String unsplashKey = apiConfig.getUnsplash().getAccessKey();
String unsplashBaseUrl = apiConfig.getUnsplash().getBaseUrl();

// Retry/Timeout
int maxRetries = apiConfig.getRetry().getMaxAttempts();
long backoffMs = apiConfig.getRetry().getBackoffMs();
int connectionTimeout = apiConfig.getTimeout().getConnectionMs();
int readTimeout = apiConfig.getTimeout().getReadMs();
```

## API Rate Limits

Be aware of rate limits for each service:

| Service | Free Tier Limit |
|---------|----------------|
| OpenWeatherMap | 1,000 calls/day |
| OpenAQ | 10,000 calls/day |
| Spotify | 10,000 calls/day |
| Unsplash | 50 requests/hour |

Configure appropriate caching and request throttling in your service implementations.

## Troubleshooting

### Placeholder Warning on Startup

**Problem**: See warning "Using PLACEHOLDER key"

**Solution**: Set the corresponding environment variable before starting the application.

### API Call Failures

**Problem**: API calls return 401 Unauthorized

**Solution**: 
1. Verify the API key is correct
2. Check if the key is active (some services require activation)
3. Ensure you're not exceeding rate limits

### Configuration Not Loading

**Problem**: Environment variables not being read

**Solution**:
1. Restart your IDE/terminal after setting environment variables
2. Verify environment variable names match exactly (case-sensitive)
3. Check Spring profile is correct (`application.properties` vs `application-prod.properties`)

## Production Deployment Checklist

- [ ] All API keys obtained from respective services
- [ ] Environment variables set in production environment
- [ ] No placeholder values in environment
- [ ] `.env` file added to `.gitignore`
- [ ] Secrets stored securely (AWS Secrets Manager, Azure Key Vault, etc.)
- [ ] Application logs show "✅ All external API keys configured successfully"
- [ ] Rate limiting implemented for API calls
- [ ] Caching configured to reduce API calls
- [ ] Error handling implemented for API failures
- [ ] Monitoring alerts set up for API quota/rate limits

## Additional Resources

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12-Factor App Config](https://12factor.net/config)
- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
