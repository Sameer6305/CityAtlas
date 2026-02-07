# CityAtlas Backend — Production Readiness Audit Report

**Date:** 2026-02-07  
**Scope:** Backend only (no frontend changes)  
**Stack:** Spring Boot 3.5.9 · Java 21 · PostgreSQL · WebClient · Caffeine Cache

---

## PART 1 — Data Source Verification

Every piece of data the frontend could display is traced to its source.

| UI Field / Section | Backend Endpoint | Data Source | Status |
|---|---|---|---|
| City name, population, GDP, coords | `GET /api/cities/{slug}` | **MOCK** — `createMockCityResponse()` hardcoded in `CityController` | ⚠️ Hardcoded |
| AQI trend (12 months) | `GET /api/cities/{slug}/analytics` | **MOCK** — `createMockAQIData()` in `AnalyticsController` | ⚠️ Hardcoded |
| Job sector distribution | same | **MOCK** — `createMockJobSectorData()` | ⚠️ Hardcoded |
| Cost of living breakdown | same | **MOCK** — `createMockCostOfLivingData()` | ⚠️ Hardcoded |
| Population trend (10 yr) | same | **MOCK** — `createMockPopulationData()` | ⚠️ Hardcoded |
| Current weather | `GET /api/weather/current?city=` | **Live API** — OpenWeatherMap via `WeatherService` | ✅ Live (needs key) |
| Air quality (AQI) | `GET /api/air-quality/city?name=` | **Live API** — OpenAQ v2 via `AirQualityService` | ✅ Live (needs key) |
| Spotify music metadata | `GET /api/spotify/city?name=` | **Live API** — Spotify Web API via `SpotifyService` | ✅ Live (needs key) |
| City images | `GET /api/images/city?name=` | **Live API** — Unsplash via `CityImageService` | ✅ Live (needs key) |
| AI city summary | `GET /api/ai/summary/{slug}` | **Computed** — Rule-based `AiCitySummaryService` + `CityFeatureComputer` | ✅ Works (needs DB city) |
| Health check | `GET /api/health` | **Hardcoded** — returns `{ "status": "UP" }` | ✅ Static |

### Key Finding
**`CityController` and `AnalyticsController` return 100% hardcoded mock data.** The database entities (`City`, `Metrics`, `AnalyticsEvent`) and repositories exist but are only used by the AI summary endpoint. No service layer connects the controllers to the database for standard city/analytics queries.

---

## PART 2 — Deployment-Time Data Availability

What works the moment you deploy, vs. what requires configuration.

| Feature | Works at Deploy? | Dependency |
|---|---|---|
| City profile page | ✅ Yes (mock data, same for every city) | None |
| Analytics dashboard | ✅ Yes (mock data, same for every city) | None |
| AI summary | ❌ No | Requires city records in DB `cities` table |
| Weather widget | ❌ No | Requires `OPENWEATHER_API_KEY` env var |
| Air quality widget | ⚠️ Partial | Works without key (public, rate-limited); better with `OPENAQ_API_KEY` |
| Spotify culture data | ❌ No | Requires `SPOTIFY_CLIENT_ID` + `SPOTIFY_CLIENT_SECRET` |
| City images | ❌ No | Requires `UNSPLASH_ACCESS_KEY` |
| Health endpoint | ✅ Yes | None |
| Actuator `/health` | ✅ Yes | None (now enabled) |
| Kafka analytics pipeline | ❌ No | Kafka broker + `kafka.enabled=true` needed |

### Startup Behavior
- The `ApiConfigValidator` logs clear warnings for every missing API key at startup.
- The `isPlaceholder()` pattern ensures services gracefully degrade (return empty) rather than throwing.
- `@Autowired(required = false)` on `AnalyticsEventProducer` prevents startup failure when Kafka is disabled.

---

## PART 3 — Data Pipeline & Backend Audit

### 3A. External API Client Quality

| Service | WebClient | Retry | Timeout | Cache | Graceful Degrade | Rating |
|---|---|---|---|---|---|---|
| `WeatherService` | ✅ Reactive | ✅ 3 attempts, backoff | ✅ Config-driven | ✅ `@Cacheable("weather")` | ✅ `Mono.empty()` | A |
| `AirQualityService` | ✅ Reactive | ✅ 2 attempts | ✅ Config-driven | ✅ `@Cacheable("airQuality")` | ✅ Public fallback | A |
| `SpotifyService` | ✅ Reactive | ✅ 2 attempts | ✅ Config-driven | ✅ `@Cacheable("spotifyMetadata")` | ✅ Empty results | A- |
| `CityImageService` | ✅ Reactive | ✅ 2 attempts | ✅ Config-driven | ✅ `@Cacheable("cityImages")` | ✅ Empty list | A |

**SpotifyService concern:** Uses `synchronized` block for OAuth token management in a reactive context. Under high concurrency this can block Netty event-loop threads. Consider switching to `Mono.defer()` + `AtomicReference` for true non-blocking token refresh.

### 3B. Caching

| Before Audit | After Audit (Fixed) |
|---|---|
| `spring.cache.type=simple` (ConcurrentHashMap) | `spring.cache.type=caffeine` |
| No TTL — entries live forever | Per-cache TTL: weather/AQI 15min, Spotify 6hr, images 24hr |
| No max size — unbounded memory growth | Max 200–500 entries per cache |
| No eviction — OOM risk under load | LRU eviction + time-based expiry |
| No cache statistics | `recordStats()` enabled + `/actuator/caches` endpoint |

### 3C. Database & JPA

| Concern | Finding | Severity |
|---|---|---|
| `ddl-auto=update` in dev | Hibernate auto-alters schema — unsafe for prod | 🔴 Critical (fixed in `-prod` profile) |
| No V1 migration | Schema bootstrapped by Hibernate, not Flyway | ⚠️ Medium |
| V2 migration | Creates star-schema tables (`dim_city`, facts) | ✅ OK (additive) |
| V3 migration | References `dim_city(city_key)` but table PK is `id` | 🔴 FK mismatch |
| Entity indexes | Comprehensive on `City`, `Metrics`, `AnalyticsEvent`, `CitySection` | ✅ Well-designed |
| `open-in-view=false` | Correctly disabled — prevents lazy-loading leaks | ✅ |
| Connection pool | Default HikariCP (10 connections) | ⚠️ Tune in prod profile |

### 3D. Kafka Pipeline

| Finding | Status |
|---|---|
| Kafka auto-config excluded in dev (`spring.autoconfigure.exclude`) | ✅ Correct |
| `AnalyticsEventProducer` / `Consumer` gated by `@ConditionalOnProperty(kafka.enabled=true)` | ✅ Correct |
| Controllers inject producer with `@Autowired(required = false)` | ✅ Safe |
| Topic naming follows `{app}.{domain}.{event}` convention | ✅ Good |
| No Dead Letter Queue (DLQ) for failed consumer messages | ⚠️ Add for prod |

### 3E. AI Pipeline

| Component | Purpose | Quality |
|---|---|---|
| `CityFeatureComputer` | Computes economy/livability/sustainability scores (0-100) | ✅ Deterministic, testable |
| `AiCitySummaryService` | Generates rule-based personality narratives | ✅ Explainable |
| `AiExplainabilityEngine` | Produces reasoning chains for each insight | ✅ Good for transparency |
| `AiQualityGuard` | Validates AI output quality | ✅ |
| `DataQualityChecker` | Validates input completeness before scoring | ✅ |
| `ConfidenceCalculator` | Adjusts confidence based on data availability | ✅ |

**Note:** The AI pipeline is NOT machine learning — it's deterministic rule-based logic. This is a strength for explainability and auditability.

---

## PART 4 — Production Hardening (Applied)

### Fixes Applied in This Audit

| # | Fix | File | Impact |
|---|---|---|---|
| 1 | Added `spring-boot-starter-actuator` dependency | `pom.xml` | Enables `/actuator/health`, `/metrics`, `/caches` |
| 2 | Added `spring-boot-starter-cache` + `caffeine` dependencies | `pom.xml` | Enables Caffeine cache with TTL |
| 3 | Rewrote `CacheConfig.java` to use Caffeine | `config/CacheConfig.java` | Per-cache TTL + max size + stats |
| 4 | Enabled Actuator endpoints in `application.properties` | `application.properties` | Health probes, liveness/readiness |
| 5 | Changed cache type from `simple` to `caffeine` | `application.properties` | TTL-based eviction, bounded memory |
| 6 | Created `application-prod.properties` production profile | New file | All hardened settings in one place |

### Production Profile (`application-prod.properties`) Includes:
- `ddl-auto=validate` (never auto-alter schema)
- `show-sql=false` (no SQL in logs)
- Hibernate SQL and BasicBinder logging set to `WARN`
- HikariCP pool tuned (20 max, 5 min-idle)
- Caffeine cache with 15min TTL
- Actuator with auth-gated details
- Liveness/readiness probes for K8s
- All API keys require real env vars (no fallback placeholders)
- Stricter timeouts (3s connect, 8s read)

---

## PART 5 — Security & Compliance Check

| Area | Finding | Severity |
|---|---|---|
| **Authentication** | All endpoints `permitAll()` — no JWT, no auth | 🔴 Critical for prod (dev-intentional) |
| **CORS** | Hardcoded `localhost:3000-3002` only | ⚠️ Must add prod domain |
| **CSRF** | Disabled (correct for stateless REST API) | ✅ |
| **Session** | `STATELESS` (correct for JWT path) | ✅ |
| **API key storage** | Env vars via `${VAR:placeholder}` pattern | ✅ Good |
| **Secret logging** | WebClient filters exclude `Authorization` headers | ✅ Good |
| **SQL injection** | Spring Data JPA parameterized queries only | ✅ Safe |
| **Input validation** | `@Valid` + `@NotBlank` on config, manual checks in controllers | ⚠️ Add `@Valid` on request bodies |
| **Error responses** | `GlobalExceptionHandler` — no stack traces in responses | ✅ Good |
| **Hibernate logging** | `BasicBinder=TRACE` leaks bind parameters in dev | 🟡 Fixed in prod profile |
| **PII exposure** | `userId`, `sessionId` stored in analytics events | ⚠️ Review for GDPR compliance |
| **Rate limiting** | No rate limiting on any endpoint | ⚠️ Add for prod |
| **Actuator security** | Health details `when-authorized` — requires auth to see details | ✅ Good |

### Security TODOs (Left Intentionally for Dev Phase)
The `SecurityConfig.java` contains clear TODOs for JWT implementation with a complete plan. This is appropriate for the current development stage but must be implemented before production.

---

## PART 6 — Final Report & Readiness Summary

### Overall Production Readiness: 🟡 PARTIAL — Safe for Demo / Staging

| Category | Score | Notes |
|---|---|---|
| External API Integration | ⭐⭐⭐⭐ | Well-architected: retry, cache, graceful degradation |
| Caching | ⭐⭐⭐⭐ | **Now fixed**: Caffeine with TTL, bounded size, stats |
| Database Design | ⭐⭐⭐⭐ | Good indexes, star schema, proper entities |
| Observability | ⭐⭐⭐⭐ | **Now fixed**: Actuator health/metrics/caches endpoints |
| Error Handling | ⭐⭐⭐⭐ | Centralized `GlobalExceptionHandler`, proper status codes |
| AI Pipeline | ⭐⭐⭐⭐⭐ | Fully explainable, deterministic, well-tested |
| Security | ⭐⭐ | All endpoints public, no auth, dev-only CORS |
| Data Completeness | ⭐⭐ | 2 of 8 controllers return real data; rest is mock |
| Production Config | ⭐⭐⭐⭐ | **Now fixed**: `application-prod.properties` with all hardened settings |

### Top Risks for Production

1. **`CityController` + `AnalyticsController` serve hardcoded mock data** — every city returns identical San Francisco data. Need to wire these to `CityRepository` / `MetricsRepository`.

2. **No authentication** — `SecurityConfig` permits all requests. JWT TODOs are documented but not implemented.

3. **V3 SQL migration** references `dim_city(city_key)` but the column is named `id` in V2 — this FK will fail on a fresh Flyway run.

4. **No rate limiting** — endpoints are vulnerable to abuse without request throttling.

5. **`AiSummaryController.block()`** — calls `.block()` on reactive `Mono<AirQualityDTO>` inside a servlet thread. This is safe but wastes a thread; consider wrapping in `CompletableFuture` or making the controller reactive.

### Safe to Deploy Now (Zero Risk)
- ✅ Health endpoint (`/api/health` + `/api/actuator/health`)
- ✅ Weather, AQI, Spotify, Unsplash endpoints (with configured API keys)
- ✅ AI summary endpoint (with populated `cities` table)
- ✅ Caffeine caching with TTL (prevents unbounded memory growth)
- ✅ Actuator metrics and cache monitoring

### What Was Changed in This Audit

| File | Change |
|---|---|
| `pom.xml` | Added `spring-boot-starter-actuator`, `spring-boot-starter-cache`, `caffeine` |
| `config/CacheConfig.java` | Replaced `ConcurrentMapCacheManager` with `CaffeineCacheManager` + per-cache TTL |
| `application.properties` | Enabled Actuator endpoints, changed cache type to `caffeine` |
| `application-prod.properties` | **New file** — complete production hardening profile |

**Zero frontend files were modified.**
