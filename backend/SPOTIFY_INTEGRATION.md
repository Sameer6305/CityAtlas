# Spotify API Integration Guide

## Overview

The CityAtlas backend integrates with **Spotify Web API** to provide music and cultural metadata for cities. This integration uses **OAuth2 Client Credentials** flow for authentication and fetches metadata only—**no audio streaming or downloads**.

## What This Integration Does

✅ **Fetches metadata:**
- Artists associated with cities
- Popular playlists mentioning cities
- Music genres prevalent in regions

❌ **Does NOT:**
- Stream audio
- Download tracks
- Access user data
- Require user authentication

## Setup Instructions

### 1. Create Spotify Developer Account

1. Visit [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Log in or create a Spotify account
3. Click "Create App"
4. Fill in app details:
   - **App Name**: CityAtlas
   - **App Description**: City music metadata and cultural analysis
   - **Redirect URI**: Not needed (client credentials flow)
   - **APIs used**: Web API
5. Accept terms and save

### 2. Get Credentials

After creating the app:
1. Go to **Settings**
2. Copy **Client ID**
3. Click "View client secret" and copy **Client Secret**
4. Keep these credentials secure!

### 3. Configure Environment Variables

Set the following environment variables:

**Linux/macOS:**
```bash
export SPOTIFY_CLIENT_ID=your_client_id_here
export SPOTIFY_CLIENT_SECRET=your_client_secret_here
```

**Windows (PowerShell):**
```powershell
$env:SPOTIFY_CLIENT_ID="your_client_id_here"
$env:SPOTIFY_CLIENT_SECRET="your_client_secret_here"
```

**Production (Docker/Kubernetes):**
```yaml
env:
  - name: SPOTIFY_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: cityatlas-secrets
        key: spotify-client-id
  - name: SPOTIFY_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: cityatlas-secrets
        key: spotify-client-secret
```

### 4. Verify Configuration

Check `application.properties`:
```properties
# Spotify API Configuration (via environment variables)
cityatlas.external.spotify.client-id=${SPOTIFY_CLIENT_ID:placeholder_spotify_client_id}
cityatlas.external.spotify.client-secret=${SPOTIFY_CLIENT_SECRET:placeholder_spotify_client_secret}
cityatlas.external.spotify.base-url=https://api.spotify.com/v1
cityatlas.external.spotify.auth-url=https://accounts.spotify.com/api/token
```

## API Usage

### Check Service Status

```bash
curl http://localhost:8080/api/spotify/status
```

**Response (configured):**
```json
{
  "configured": true,
  "service": "Spotify",
  "authType": "OAuth2 Client Credentials",
  "message": "Spotify API is configured and ready",
  "capabilities": [
    "Search artists by city",
    "Search playlists by city",
    "Extract genre metadata",
    "Metadata only (no audio streaming)"
  ]
}
```

**Response (not configured):**
```json
{
  "configured": false,
  "service": "Spotify",
  "authType": "OAuth2 Client Credentials",
  "message": "Spotify credentials not configured. Set SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET.",
  "instructions": "See EXTERNAL_API_SETUP.md for configuration details"
}
```

### Get City Metadata

```bash
curl "http://localhost:8080/api/spotify/city?name=Nashville"
```

**Response:**
```json
{
  "cityName": "Nashville",
  "artists": [
    {
      "id": "6M2wZ9GZgrQXHCFfjv46we",
      "name": "Dua Lipa",
      "genres": ["pop", "dance pop"],
      "popularity": 92,
      "followers": 45678901,
      "externalUrl": "https://open.spotify.com/artist/6M2wZ9GZgrQXHCFfjv46we",
      "images": [
        {
          "url": "https://i.scdn.co/image/...",
          "height": 640,
          "width": 640
        }
      ]
    }
  ],
  "playlists": [
    {
      "id": "37i9dQZF1DX4WYpdgoIcn6",
      "name": "Nashville Sounds",
      "description": "The best country music from Music City",
      "owner": "Spotify",
      "trackCount": 50,
      "externalUrl": "https://open.spotify.com/playlist/37i9dQZF1DX4WYpdgoIcn6",
      "images": [...]
    }
  ],
  "genres": [
    "country",
    "americana",
    "bluegrass",
    "southern rock"
  ],
  "totalResults": 15
}
```

### Refresh Access Token (Admin)

```bash
curl -X POST http://localhost:8080/api/spotify/refresh-token
```

**Response:**
```json
{
  "status": "success",
  "message": "Token cache cleared. New token will be acquired on next API call."
}
```

## Authentication Flow

### OAuth2 Client Credentials

1. **Initial Request**: SpotifyService checks if cached token is valid
2. **If Expired**: Service requests new token:
   ```
   POST https://accounts.spotify.com/api/token
   Authorization: Basic base64(client_id:client_secret)
   Content-Type: application/x-www-form-urlencoded
   
   grant_type=client_credentials
   ```
3. **Token Response**:
   ```json
   {
     "access_token": "BQD...",
     "token_type": "Bearer",
     "expires_in": 3600
   }
   ```
4. **Cache Token**: Stored in memory with expiration timestamp
5. **Use Token**: All API calls include `Authorization: Bearer {access_token}`
6. **Auto-Refresh**: Token refreshed automatically before expiration

### Token Lifecycle

- **Expiration**: 3600 seconds (1 hour)
- **Buffer**: 60 seconds before expiration
- **Cache**: In-memory (single application instance)
- **Refresh**: Automatic on expiration
- **No Refresh Token**: Client credentials flow doesn't provide one

## Code Structure

### Files Created

1. **`SpotifyAuthDTO.java`** - OAuth2 token response
   - Access token storage
   - Expiration validation
   - Token lifecycle methods

2. **`SpotifyMetadataDTO.java`** - Music metadata structures
   - Artist information (name, genres, popularity, followers)
   - Playlist information (name, description, track count)
   - Image metadata (URLs, dimensions)
   - Spotify API response classes

3. **`SpotifyService.java`** - Core service logic
   - OAuth2 authentication
   - Token caching and refresh
   - Search API integration
   - Graceful degradation

4. **`SpotifyController.java`** - REST endpoints
   - `/api/spotify/city` - Get city metadata
   - `/api/spotify/status` - Check configuration
   - `/api/spotify/refresh-token` - Force token refresh

## Graceful Degradation

The service handles missing credentials gracefully:

```java
if (!spotifyService.hasCredentials()) {
    logger.warn("Spotify credentials not configured");
    return Mono.empty(); // No error thrown
}
```

**Behavior:**
- ✅ Application starts successfully
- ✅ Other services continue working
- ✅ `/api/spotify/status` returns "not configured"
- ✅ `/api/spotify/city` returns 204 No Content
- ⚠️ Logs warning messages at startup

## Rate Limits

Spotify imposes rate limits:

- **Normal**: ~180 requests/minute
- **With burst**: Higher short-term rate
- **429 Response**: Rate limit exceeded

**Handling:**
```java
.onStatus(status -> status.value() == 429, response -> {
    logger.warn("Spotify rate limit exceeded");
    return Mono.error(new ExternalApiException(...));
})
```

**Best Practices:**
- Cache results in Redis (recommended)
- Implement exponential backoff
- Monitor rate limit headers
- Consider Premium API access for higher limits

## Error Handling

### Common Errors

1. **401 Unauthorized**: Invalid credentials
   ```
   Check SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET
   ```

2. **429 Too Many Requests**: Rate limit exceeded
   ```
   Wait before retrying, implement caching
   ```

3. **404 Not Found**: No results for city
   ```
   Returns 204 No Content (not an error)
   ```

4. **500 Server Error**: Spotify service issue
   ```
   Automatic retry (2 attempts with exponential backoff)
   ```

### Retry Logic

```java
.retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
    .filter(throwable -> {
        if (throwable instanceof ExternalApiException) {
            return ((ExternalApiException) throwable).isRetryable();
        }
        return false;
    })
)
```

**Retryable Conditions:**
- HTTP 5xx errors (server issues)
- Connection timeouts
- Network errors

**Non-Retryable:**
- HTTP 4xx errors (client issues)
- Invalid credentials
- Rate limit errors (without backoff)

## Testing

### Check Configuration

```bash
# Should return configured=true
curl http://localhost:8080/api/spotify/status
```

### Test City Search

```bash
# Try different cities
curl "http://localhost:8080/api/spotify/city?name=Austin"
curl "http://localhost:8080/api/spotify/city?name=Tokyo"
curl "http://localhost:8080/api/spotify/city?name=London"
```

### Test Graceful Degradation

```bash
# Unset credentials
unset SPOTIFY_CLIENT_ID
unset SPOTIFY_CLIENT_SECRET

# Restart application - should start successfully
mvn spring-boot:run

# Check status - should return configured=false
curl http://localhost:8080/api/spotify/status
```

### Test Token Refresh

```bash
# Get metadata (acquires token)
curl "http://localhost:8080/api/spotify/city?name=Nashville"

# Force refresh
curl -X POST http://localhost:8080/api/spotify/refresh-token

# Get metadata again (acquires new token)
curl "http://localhost:8080/api/spotify/city?name=Nashville"
```

## Compliance & Legal

### Spotify API Terms

- ✅ **Metadata access**: Allowed for informational purposes
- ✅ **Display**: Can show artist names, playlist titles
- ✅ **Attribution**: Link back to Spotify (externalUrl)
- ❌ **Audio**: Do NOT stream or download tracks
- ❌ **Scraping**: Do NOT scrape data beyond API
- ❌ **Caching**: Limit cache duration (recommend 24 hours max)

### Attribution

When displaying data:
```html
<p>Music data from <a href="https://spotify.com">Spotify</a></p>
```

Always include `externalUrl` links in UI:
```javascript
<a href={artist.externalUrl}>View on Spotify</a>
```

## Monitoring

### Key Metrics

1. **Token Refresh Rate**: Should be ~1/hour
2. **API Success Rate**: Target >99%
3. **Rate Limit Errors**: Should be <1%
4. **Cache Hit Rate**: Target >80% (if caching implemented)

### Logs to Monitor

```
INFO: Spotify token acquired successfully (expires in 3600 seconds)
WARN: Spotify rate limit exceeded
ERROR: Failed to acquire Spotify token: Invalid client credentials
```

## Next Steps

1. **Implement Caching**: Use Redis to cache results (24h TTL)
2. **Add Monitoring**: Track API usage and success rates
3. **Batch Requests**: Combine multiple city searches
4. **Enrich Data**: Cross-reference with other music APIs
5. **UI Integration**: Display artists/playlists in frontend

## Troubleshooting

### "Credentials not configured"

**Solution**: Set environment variables
```bash
export SPOTIFY_CLIENT_ID=your_id
export SPOTIFY_CLIENT_SECRET=your_secret
```

### "Invalid client credentials"

**Solution**: Verify credentials are correct
```bash
# Check what's configured
echo $SPOTIFY_CLIENT_ID
# Should match Spotify Developer Dashboard
```

### "Rate limit exceeded"

**Solution**: Implement caching and reduce request frequency
```java
// Add to application.properties
spring.cache.type=redis
```

### No results for city

**Solution**: Not an error - city might not have Spotify data
```
Response: 204 No Content
```

## Resources

- [Spotify Web API Documentation](https://developer.spotify.com/documentation/web-api)
- [Client Credentials Flow](https://developer.spotify.com/documentation/web-api/tutorials/client-credentials-flow)
- [Search API Reference](https://developer.spotify.com/documentation/web-api/reference/search)
- [Rate Limits](https://developer.spotify.com/documentation/web-api/concepts/rate-limits)

---

**Last Updated**: December 27, 2025  
**Integration Version**: 1.0  
**Spotify API Version**: v1
