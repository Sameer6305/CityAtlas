# Unsplash API Integration Guide

## Overview

The CityAtlas backend integrates with **Unsplash API** to provide high-quality, royalty-free city images. This integration fetches image URLs and metadata for visual content in city profiles and galleries.

## What This Integration Does

✅ **Fetches image data:**
- Search for city images by name
- Get random city images
- Multiple image sizes (raw, full, regular, small, thumb)
- Photographer attribution metadata
- Image dimensions and colors

❌ **Does NOT:**
- Host or store images
- Provide images without attribution
- Exceed API rate limits

## Setup Instructions

### 1. Create Unsplash Developer Account

1. Visit [Unsplash Developers](https://unsplash.com/developers)
2. Log in or create an Unsplash account
3. Click "Register as a developer"
4. Accept the API Terms and Guidelines
5. Click "New Application"
6. Fill in application details:
   - **Application Name**: CityAtlas
   - **Description**: City image gallery and visual content
   - Accept terms and create application

### 2. Get Access Key

After creating the app:
1. Go to your application dashboard
2. Copy the **Access Key** (not Secret Key for this use case)
3. Keep this key secure!

### 3. Configure Environment Variable

Set the following environment variable:

**Linux/macOS:**
```bash
export UNSPLASH_ACCESS_KEY=your_access_key_here
```

**Windows (PowerShell):**
```powershell
$env:UNSPLASH_ACCESS_KEY="your_access_key_here"
```

**Production (Docker/Kubernetes):**
```yaml
env:
  - name: UNSPLASH_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: cityatlas-secrets
        key: unsplash-access-key
```

### 4. Verify Configuration

Check `application.properties`:
```properties
# Unsplash API Configuration (via environment variable)
cityatlas.external.unsplash.access-key=${UNSPLASH_ACCESS_KEY:placeholder_unsplash_access_key}
cityatlas.external.unsplash.base-url=https://api.unsplash.com
```

## API Usage

### Check Service Status

```bash
curl http://localhost:8080/api/images/status
```

**Response (configured):**
```json
{
  "configured": true,
  "service": "Unsplash",
  "message": "Unsplash API is configured and ready",
  "capabilities": [
    "Search city images",
    "Get random city image",
    "Track image downloads",
    "Automatic photographer attribution"
  ],
  "rateLimit": "50 requests/hour (demo), 5000 requests/hour (production)",
  "attribution": "REQUIRED - Must display photographer name and link"
}
```

## Attribution Requirements

### CRITICAL - Must Follow

Per Unsplash API Guidelines, you **MUST**:

1. **Display photographer attribution**
2. **Include UTM parameters** in attribution links
3. **Track downloads** when using an image

### Frontend Example (React)

```jsx
function CityImage({ image }) {
  useEffect(() => {
    // Track download when image is displayed
    fetch('/api/images/track-download', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ downloadLocation: image.downloadLocation })
    });
  }, [image.downloadLocation]);

  return (
    <div>
      <img src={image.urls.regular} alt={image.altDescription} />
      <p>
        Photo by{' '}
        <a href={image.user.links.html + '?utm_source=cityatlas&utm_medium=referral'}>
          {image.user.name}
        </a>
        {' '}on{' '}
        <a href="https://unsplash.com/?utm_source=cityatlas&utm_medium=referral">
          Unsplash
        </a>
      </p>
    </div>
  );
}
```

## Rate Limits

### Demo Tier (Default)
- **50 requests per hour**
- Suitable for development and testing

### Production Tier (Apply for approval)
- **5,000 requests per hour**
- Required for production use

## Code Structure

### Files Created

1. **`UnsplashImageDTO.java`** - Image data structure
   - Image metadata (dimensions, color, likes)
   - Multiple URL sizes
   - Photographer information
   - Attribution helpers

2. **`CityImageService.java`** - Core service logic
   - Search city images
   - Get random city image
   - Track downloads
   - Graceful degradation

3. **`UnsplashController.java`** - REST endpoints
   - `/api/images/city` - Search city images
   - `/api/images/random` - Get random image
   - `/api/images/track-download` - Track downloads
   - `/api/images/status` - Check configuration

## Graceful Degradation

The service handles missing access key gracefully:
- ✅ Application starts successfully
- ✅ Other services continue working
- ✅ Returns 204 No Content when not configured

## Resources

- [Unsplash API Documentation](https://unsplash.com/documentation)
- [API Guidelines](https://help.unsplash.com/en/articles/2511245-unsplash-api-guidelines)
- [Apply for Production](https://unsplash.com/oauth/applications)

---

**Last Updated**: December 27, 2025  
**Integration Version**: 1.0
