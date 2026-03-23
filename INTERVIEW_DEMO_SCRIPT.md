# CityAtlas Interview Demo Script

## 1. Intro (30 seconds)
- "CityAtlas is a full-stack city intelligence platform with a Next.js frontend and Spring Boot backend."
- "It aggregates weather, AQI, demographic, and analytics signals into one city view."

## 2. Start Services (30 seconds)
Run in two terminals:

```powershell
# Terminal 1
Set-Location "C:\Users\PRANAV KADAM\Desktop\CityAtlas\backend"
.\mvnw.cmd spring-boot:run
```

```powershell
# Terminal 2
Set-Location "C:\Users\PRANAV KADAM\Desktop\CityAtlas"
npm run dev
```

## 3. Smoke Flow (60 seconds)
Run:

```powershell
Set-Location "C:\Users\PRANAV KADAM\Desktop\CityAtlas"
.\scripts\demo-smoke.ps1
```

What to say while it runs:
- "This checks frontend availability, backend health, JWT login, and a protected city endpoint."
- "It verifies critical user journey and API wiring quickly before demos or releases."

## 4. UI Walkthrough (2-3 minutes)
- Open http://localhost:3000
- Search/select a city (for example London or New York)
- Show these tabs and mention value:
  - Overview: consolidated city snapshot
  - Economy: GDP/labor indicators
  - Environment: AQI/weather trends
  - Culture: city context and media
  - Analytics: behavior and usage signals
  - AI Summary: interpreted insights with confidence

## 5. Engineering Depth Talking Points (2 minutes)
- Backend architecture:
  - Spring Boot with typed config, validation, and integration tests
  - External API orchestration with graceful fallback behavior
  - Caching strategy for expensive upstream calls
- Frontend architecture:
  - Next.js app router, typed API layer, and modular components
  - Production build and lint gates in place
- Quality and reliability:
  - Automated tests (frontend + backend)
  - Smoke script for quick operational confidence
  - Secrets moved to environment-based config

## 6. Security and Ops Talking Point (30 seconds)
- "No hardcoded credentials in tracked code."
- "Local `.env` is ignored by git; templates use placeholders only."
- "Runtime config is env-driven for safer deployments."

## 7. Close (15 seconds)
- "This demonstrates product thinking (usable city insights), engineering rigor (tests and validation), and production hygiene (security and config management)."

## 8. Deployed Demo Mode (Vercel) (60 seconds)
- Open your Vercel URL and quickly show:
  - /cities page loads fast
  - /cities/new-york shows weather + AQI populated
  - /compare with 2 cities returns real metrics
- Say this line:
  - "Frontend is hosted on Vercel, backend runs separately, and requests are proxied through a Next route so browser calls stay same-origin."
- Backup verification command (if interviewer asks proof):

```powershell
Set-Location "C:\Users\PRANAV KADAM\Desktop\CityAtlas"
.\check-apis.ps1 -BackendBase "https://YOUR_BACKEND_DOMAIN/api"
```
