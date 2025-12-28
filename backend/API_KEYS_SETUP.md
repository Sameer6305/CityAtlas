# API Keys Setup Guide

This guide explains how to set up API keys for the CityAtlas backend services.

## 🔐 Security First

**IMPORTANT**: Never commit API keys to version control!

The following files are already excluded from Git:
- `application-secrets.properties` - Backend API keys
- `.env.local` - Frontend environment variables
- `.env` - Local environment overrides

## 📋 Required API Keys

### 1. Spotify API (Music & Culture Data)

**Purpose**: Provides local music scene data and cultural insights

**Setup Steps**:
1. Visit [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Log in with your Spotify account (or create one)
3. Click "Create an App"
4. Fill in:
   - App Name: `CityAtlas`
   - App Description: `City intelligence platform`
   - Redirect URI: `http://localhost:8080/api/spotify/callback`
5. Accept terms and create
6. Copy the **Client ID** and **Client Secret**

**Where to add**:
```properties
# In backend/src/main/resources/application-secrets.properties
spotify.client-id=your_actual_client_id_here
spotify.client-secret=your_actual_client_secret_here
```

### 2. OpenWeather API (Weather Data)

**Purpose**: Real-time weather, forecasts, and climate data

**Setup Steps**:
1. Visit [OpenWeather API](https://openweathermap.org/api)
2. Click "Sign Up" and create a free account
3. Verify your email
4. Go to [API Keys section](https://home.openweathermap.org/api_keys)
5. Copy the default API key (or create a new one)
6. Note: Free tier includes 60 calls/minute, 1M calls/month

**Where to add**:
```properties
# In backend/src/main/resources/application-secrets.properties
openweather.api-key=your_actual_api_key_here
```

### 3. Unsplash API (City Images)

**Purpose**: High-quality city photography

**Setup Steps**:
1. Visit [Unsplash Developers](https://unsplash.com/developers)
2. Sign up for a free account
3. Click "New Application"
4. Accept the API terms
5. Fill in application details:
   - Application Name: `CityAtlas`
   - Description: `City intelligence platform`
6. Copy the **Access Key** and **Secret Key**
7. Note: Free tier includes 50 requests/hour

**Where to add**:
```properties
# In backend/src/main/resources/application-secrets.properties
unsplash.access-key=your_actual_access_key_here
unsplash.secret-key=your_actual_secret_key_here
```

### 4. OpenAQ API (Air Quality Data)

**Purpose**: Real-time air quality monitoring

**Setup Steps**:
1. Visit [OpenAQ](https://openaq.org/)
2. Check if API key is required (some endpoints are public)
3. If required, sign up and get API key from their dashboard

**Where to add**:
```properties
# In backend/src/main/resources/application-secrets.properties
openaq.api-key=your_actual_api_key_here
```

## 🚀 Quick Start

### Step 1: Copy the Template

```bash
cd backend/src/main/resources
cp application-secrets.properties.example application-secrets.properties
```

### Step 2: Fill in Your Keys

Open `application-secrets.properties` and replace all placeholder values with your actual API keys.

### Step 3: Verify Configuration

Start the backend application:

```bash
cd backend
./mvnw spring-boot:run
```

Check the logs for any configuration errors. You should see:
```
Started CityAtlasBackendApplication in X seconds
```

### Step 4: Test APIs (Optional)

Test each service endpoint to ensure keys are working:

- Weather: `GET http://localhost:8080/api/weather?city=London`
- Air Quality: `GET http://localhost:8080/api/air-quality?city=London`
- Spotify: `GET http://localhost:8080/api/spotify/city-music?city=London`
- Images: `GET http://localhost:8080/api/images?city=London`

## 🔧 Frontend Setup

For the Next.js frontend, create `.env.local`:

```bash
cd ..  # Back to project root
cp .env.example .env.local
```

Edit `.env.local`:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## 🌐 Production Deployment

For production, use environment variables instead of the secrets file:

```bash
export SPOTIFY_CLIENT_ID=xxx
export SPOTIFY_CLIENT_SECRET=xxx
export OPENWEATHER_API_KEY=xxx
export UNSPLASH_ACCESS_KEY=xxx
export OPENAQ_API_KEY=xxx
```

Or use your cloud provider's secrets management:
- **AWS**: Secrets Manager / Parameter Store
- **Azure**: Key Vault
- **GCP**: Secret Manager
- **Heroku**: Config Vars
- **Vercel**: Environment Variables

## 📝 Database Configuration

Don't forget to also configure your database credentials in `application-secrets.properties`:

```properties
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
```

## ❓ Troubleshooting

### "Failed to load property source from 'classpath:application-secrets.properties'"

**Solution**: The file doesn't exist. Copy it from the example:
```bash
cp application-secrets.properties.example application-secrets.properties
```

### "401 Unauthorized" from API

**Solutions**:
- Verify your API key is correct
- Check if the API key is activated (some services require email verification)
- Ensure you haven't exceeded rate limits
- For Spotify: Verify the redirect URI matches exactly

### "Connection refused" errors

**Solution**: Ensure the backend is running on `http://localhost:8080`

## 📚 Additional Resources

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Spotify API Documentation](https://developer.spotify.com/documentation/web-api)
- [OpenWeather API Docs](https://openweathermap.org/api)
- [Unsplash API Docs](https://unsplash.com/documentation)
- [OpenAQ API Docs](https://docs.openaq.org/)

## 🔒 Security Best Practices

1. ✅ Never commit `application-secrets.properties` or `.env.local` to Git
2. ✅ Use environment variables in production
3. ✅ Rotate API keys regularly
4. ✅ Use different keys for development and production
5. ✅ Monitor API usage to detect unauthorized access
6. ✅ Keep `.gitignore` up to date
7. ✅ Review which team members have access to production keys

---

**Need Help?** Check the [backend documentation](./EXTERNAL_API_SETUP.md) for detailed API integration guides.
