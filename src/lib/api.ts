/**
 * CityAtlas API Client
 * 
 * Fetches real data from the backend API with in-memory caching.
 * Backend sources: GeoDB Cities, World Bank, Open-Meteo AQ, OpenWeatherMap.
 */

const API_BASE = process.env.NEXT_PUBLIC_API_URL || '/api/backend';

// ── Client-side in-memory cache ──────────────────────────────────────────
// Prevents redundant network requests when navigating between tabs/pages.
// TTL: 5 minutes (backend Caffeine cache handles long-term caching at 6h).
const CACHE_TTL_MS = 5 * 60 * 1000;
const cache = new Map<string, { data: unknown; ts: number }>();
const inflight = new Map<string, Promise<unknown>>();
const REQUEST_TIMEOUT_MS = 12_000;

function getCached<T>(key: string): T | undefined {
  const entry = cache.get(key);
  if (entry && Date.now() - entry.ts < CACHE_TTL_MS) return entry.data as T;
  if (entry) cache.delete(key);
  return undefined;
}

function setCache<T>(key: string, data: T): T {
  cache.set(key, { data, ts: Date.now() });
  return data;
}

function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE}${normalizedPath}`;
}

async function fetchJsonOrNull<T>(path: string, requestKey: string): Promise<T | null> {
  const cached = getCached<T>(requestKey);
  if (cached) return cached;

  const existing = inflight.get(requestKey) as Promise<T | null> | undefined;
  if (existing) return existing;

  const request = (async () => {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    try {
      const res = await fetch(buildApiUrl(path), {
        signal: controller.signal,
        headers: {
          Accept: 'application/json',
        },
      });
      if (!res.ok) return null;
      const data: T = await res.json();
      return setCache(requestKey, data);
    } catch (err) {
      console.error(`Failed API request for ${path}:`, err);
      return null;
    } finally {
      clearTimeout(timeoutId);
      inflight.delete(requestKey);
    }
  })();

  inflight.set(requestKey, request);
  return request;
}

export interface CityData {
  id: number;
  slug: string;
  name: string;
  state: string | null;
  country: string | null;
  countryCode: string | null;
  population: number | null;
  gdpPerCapita: number | null;
  latitude: number | null;
  longitude: number | null;
  costOfLivingIndex: number | null;
  unemploymentRate: number | null;
  literacyRate: number | null;
  pupilTeacherRatio: number | null;
  renewableEnergyPct: number | null;
  co2PerCapita: number | null;
  languages: string[] | null;
  bannerImageUrl: string | null;
  description: string | null;
  lastUpdated: string;
  // Health & Safety — World Bank (country-level)
  hospitalBedsPer1000: number | null;
  healthExpenditurePerCapita: number | null;
  lifeExpectancy: number | null;
  // Infrastructure & Connectivity — World Bank (country-level)
  internetUsersPct: number | null;
  mobileSubscriptionsPer100: number | null;
  electricityAccessPct: number | null;
  // Live Weather — OpenWeatherMap (city-level)
  weatherTemp: number | null;
  weatherDescription: string | null;
  weatherIcon: string | null;
  weatherHumidity: number | null;
  weatherWindSpeed: number | null;
  // Live Air Quality — Open-Meteo (city-level)
  airQualityIndex: number | null;
  airQualityCategory: string | null;
  pm25: number | null;
}

export interface AnalyticsData {
  citySlug: string;
  cityName: string;
  aqiTrend: { month: string; aqi: number; category: string }[];
  jobSectors: { sector: string; employees: number; percentage: number; growthRate: number }[];
  costOfLiving: { category: string; index: number; nationalAverage: number; monthlyAvg: number }[];
  populationTrend: { year: string; population: number; growthRate: number }[];
}

/**
 * Fetch city profile data from backend.
 * Uses in-memory cache (5min TTL) to avoid redundant requests on tab navigation.
 */
export async function fetchCityData(slug: string): Promise<CityData | null> {
  const key = `city:${slug}`;
  return fetchJsonOrNull<CityData>(`/cities/${slug}`, key);
}

/**
 * Fetch analytics data from backend.
 * Uses in-memory cache (5min TTL) to avoid redundant requests.
 */
export async function fetchAnalyticsData(slug: string): Promise<AnalyticsData | null> {
  const key = `analytics:${slug}`;
  return fetchJsonOrNull<AnalyticsData>(`/cities/${slug}/analytics`, key);
}

/**
 * Warm both city and analytics cache entries for smoother section navigation.
 * Uses existing in-memory cache and inflight de-duplication; no new infrastructure.
 */
export function prefetchCityBundle(slug: string): void {
  void fetchCityData(slug);
  void fetchAnalyticsData(slug);
}

/**
 * Format population for display.
 * 8336817 → "8.3M" | 815201 → "815,201"
 */
export function formatPopulation(pop: number | null): string {
  if (pop == null) return 'N/A';
  if (pop >= 1_000_000) return `${(pop / 1_000_000).toFixed(1)}M`;
  return pop.toLocaleString();
}

/**
 * Format currency values.
 * 85000 → "$85K" | 1234 → "$1.2K"
 */
export function formatCurrency(value: number | null): string {
  if (value == null) return 'N/A';
  if (value >= 1000) return `$${(value / 1000).toFixed(0)}K`;
  return `$${value.toFixed(0)}`;
}

/**
 * Format percentage.
 * 3.8 → "3.8%"
 */
export function formatPercent(value: number | null): string {
  if (value == null) return 'N/A';
  return `${value.toFixed(1)}%`;
}
