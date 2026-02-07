/**
 * City Overview Page (Default)
 * Route: /cities/[citySlug]
 * 
 * Premium glassmorphism dashboard design
 * Shows high-level city information and key metrics.
 * Data sourced from: GeoDB Cities, World Bank, OpenWeatherMap, OpenAQ APIs.
 */

'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams } from 'next/navigation';
import { AreaChart } from '@/components';
import { fetchCityData, fetchAnalyticsData, formatPopulation, formatCurrency } from '@/lib/api';
import type { CityData, AnalyticsData } from '@/lib/api';

// Custom hook for scroll-triggered animations using Intersection Observer
function useScrollAnimation(threshold = 0.1) {
  const ref = useRef<HTMLDivElement>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
        }
      },
      { threshold, rootMargin: '50px' }
    );

    const currentRef = ref.current;
    if (currentRef) {
      observer.observe(currentRef);
    }

    return () => {
      if (currentRef) {
        observer.unobserve(currentRef);
      }
    };
  }, [threshold]);

  return { ref, isVisible };
}

// Glassmorphism card component with scroll-triggered animation - Matches All Cities page style
function GlassCard({ children, className = '', delay = 0 }: { children: React.ReactNode; className?: string; delay?: number }) {
  const { ref, isVisible: isInView } = useScrollAnimation(0.1);
  const [isAnimated, setIsAnimated] = useState(false);
  
  useEffect(() => {
    if (isInView) {
      const timer = setTimeout(() => setIsAnimated(true), delay);
      return () => clearTimeout(timer);
    }
  }, [isInView, delay]);

  return (
    <div 
      ref={ref}
      className={`backdrop-blur-xl bg-white/[0.03] border border-white/[0.08] rounded-2xl transition-all duration-[800ms] ease-out hover:bg-white/[0.06] hover:border-white/[0.15] hover:shadow-2xl hover:shadow-cyan-500/10 hover:scale-[1.01] hover:-translate-y-1 ${isAnimated ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-12 scale-[0.97]'} ${className}`}
    >
      {children}
    </div>
  );
}

// Metric display component with scroll-triggered animation - Matches All Cities page style
function MetricDisplay({ 
  label, 
  value, 
  icon, 
  trend, 
  trendValue,
  iconBg = 'from-cyan-500/20 to-blue-500/20',
  delay = 0
}: { 
  label: string; 
  value: string; 
  icon: React.ReactNode;
  trend?: 'up' | 'down';
  trendValue?: string;
  iconBg?: string;
  delay?: number;
}) {
  const { ref, isVisible: isInView } = useScrollAnimation(0.1);
  const [isAnimated, setIsAnimated] = useState(false);
  
  useEffect(() => {
    if (isInView) {
      const timer = setTimeout(() => setIsAnimated(true), delay);
      return () => clearTimeout(timer);
    }
  }, [isInView, delay]);

  return (
    <div 
      ref={ref}
      className={`backdrop-blur-xl bg-white/[0.03] border border-white/[0.08] rounded-2xl p-5 hover:bg-white/[0.06] hover:border-white/[0.15] hover:scale-[1.03] hover:-translate-y-1 hover:shadow-2xl hover:shadow-cyan-500/15 transition-all duration-[800ms] ease-out group ${isAnimated ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-12 scale-[0.97]'}`}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-white/50 text-sm font-medium mb-1.5">{label}</p>
          <p className="text-3xl font-bold text-white">{value}</p>
          {trend && trendValue && (
            <div className={`flex items-center gap-1.5 mt-2.5 text-sm ${trend === 'up' ? 'text-emerald-400' : 'text-red-400'}`}>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d={trend === 'up' ? "M5 10l7-7m0 0l7 7m-7-7v18" : "M19 14l-7 7m0 0l-7-7m7 7V3"} 
                />
              </svg>
              <span className="font-medium">{trendValue}</span>
            </div>
          )}
        </div>
        <div className={`w-14 h-14 rounded-xl bg-gradient-to-br ${iconBg} flex items-center justify-center group-hover:scale-110 group-hover:rotate-6 transition-all duration-500 shadow-lg group-hover:shadow-xl`}>
          {icon}
        </div>
      </div>
    </div>
  );
}

export default function CityOverviewPage() {
  const [isLoaded, setIsLoaded] = useState(false);
  const params = useParams();
  const citySlug = params.citySlug as string;

  // Real data from backend APIs
  const [cityData, setCityData] = useState<CityData | null>(null);
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (!citySlug) return;
    setLoading(true);
    Promise.all([
      fetchCityData(citySlug),
      fetchAnalyticsData(citySlug),
    ]).then(([city, anal]) => {
      setCityData(city);
      setAnalytics(anal);
      setLoading(false);
    });
  }, [citySlug]);

  // Prepare population chart data from real World Bank data
  const populationChartData = analytics?.populationTrend?.length
    ? analytics.populationTrend.map(p => ({ year: p.year, population: p.population }))
    : [];

  return (
    <div className={`space-y-8 transition-all duration-[1200ms] ease-out ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'}`}>
      
      {/* Loading State */}
      {loading && (
        <div className="flex items-center justify-center py-20">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cyan-400"></div>
          <span className="ml-4 text-white/60">Loading real city data...</span>
        </div>
      )}

      {!loading && (
        <>
          {/* Key Metrics Grid — all from real APIs */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
            <MetricDisplay
              label="Population"
              value={formatPopulation(cityData?.population ?? null)}
              trend={cityData?.population ? 'up' : undefined}
              trendValue={cityData?.population ? 'GeoDB Cities' : undefined}
              iconBg="from-cyan-500/20 to-blue-500/20"
              delay={100}
              icon={
                <svg className="w-6 h-6 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              }
            />
            <MetricDisplay
              label="GDP per Capita"
              value={formatCurrency(cityData?.gdpPerCapita ?? null)}
              trend={cityData?.gdpPerCapita ? 'up' : undefined}
              trendValue={cityData?.gdpPerCapita ? 'World Bank' : undefined}
              iconBg="from-emerald-500/20 to-teal-500/20"
              delay={200}
              icon={
                <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              }
            />
            <MetricDisplay
              label="Life Expectancy"
              value={cityData?.lifeExpectancy != null ? `${cityData.lifeExpectancy} yr` : 'N/A'}
              trend={cityData?.lifeExpectancy != null ? (cityData.lifeExpectancy > 70 ? 'up' : 'down') : undefined}
              trendValue={cityData?.lifeExpectancy != null ? 'World Bank' : undefined}
              iconBg="from-rose-500/20 to-pink-500/20"
              delay={300}
              icon={
                <svg className="w-6 h-6 text-rose-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              }
            />
            <MetricDisplay
              label="Country"
              value={cityData?.country ?? 'N/A'}
              iconBg="from-purple-500/20 to-pink-500/20"
              delay={400}
              icon={
                <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              }
            />
          </div>

          {/* Live Weather Strip — from OpenWeatherMap */}
          {cityData?.weatherTemp != null && (
            <GlassCard className="p-4 bg-gradient-to-r from-blue-500/10 via-transparent to-cyan-500/10" delay={50}>
              <div className="flex items-center justify-between flex-wrap gap-3">
                <div className="flex items-center gap-3">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="text-white/70 text-sm font-medium">Live Weather</span>
                </div>
                <div className="flex items-center gap-6 text-sm">
                  <span className="text-white font-bold text-lg">{cityData.weatherTemp}°C</span>
                  <span className="text-white/60 capitalize">{cityData.weatherDescription}</span>
                  <span className="text-white/50">💧 {cityData.weatherHumidity}%</span>
                  <span className="text-white/50">🌬️ {cityData.weatherWindSpeed} m/s</span>
                  <span className="text-white/30 text-xs">OpenWeatherMap</span>
                </div>
              </div>
            </GlassCard>
          )}

          {/* Live AQI Badge — from OpenAQ */}
          {cityData?.airQualityIndex != null && (
            <GlassCard className="p-4" delay={75}>
              <div className="flex items-center justify-between flex-wrap gap-3">
                <div className="flex items-center gap-3">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="text-white/70 text-sm font-medium">Air Quality</span>
                </div>
                <div className="flex items-center gap-5 text-sm">
                  <span className={`font-bold text-lg ${cityData.airQualityIndex <= 50 ? 'text-emerald-400' : cityData.airQualityIndex <= 100 ? 'text-yellow-400' : 'text-red-400'}`}>
                    AQI {cityData.airQualityIndex}
                  </span>
                  <span className="text-white/60">{cityData.airQualityCategory}</span>
                  {cityData.pm25 != null && <span className="text-white/50">PM2.5: {cityData.pm25} μg/m³</span>}
                  <span className="text-white/30 text-xs">OpenAQ</span>
                </div>
              </div>
            </GlassCard>
          )}

          {/* Population Trend — Real World Bank Data */}
          {populationChartData.length > 0 && (
            <GlassCard className="p-6" delay={100}>
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h3 className="text-lg font-semibold text-white">Population Trend (Country)</h3>
                  <p className="text-white/50 text-sm">Historical data from World Bank API — country-level</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-cyan-400 animate-pulse"></span>
                  <span className="text-white/60 text-sm">Population (M)</span>
                </div>
              </div>
              <AreaChart
                data={populationChartData}
                xKey="year"
                areas={[
                  { dataKey: 'population', name: 'Population (M)', color: '#06b6d4', fillOpacity: 0.3 },
                ]}
                height={280}
              />
            </GlassCard>
          )}

          {/* City Description */}
          {cityData?.description && (
            <GlassCard className="p-6" delay={150}>
              <h3 className="text-lg font-semibold text-white mb-3 flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500/20 to-blue-500/20 flex items-center justify-center">
                  <svg className="w-4 h-4 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                About {cityData.name}
              </h3>
              <p className="text-white/70 leading-relaxed">{cityData.description}</p>
              {cityData.state && (
                <p className="text-white/50 text-sm mt-3">
                  📍 {cityData.state}, {cityData.country} 
                  {cityData.latitude && cityData.longitude && (
                    <span className="ml-2">
                      ({cityData.latitude.toFixed(2)}°, {cityData.longitude.toFixed(2)}°)
                    </span>
                  )}
                </p>
              )}
              <p className="text-white/30 text-xs mt-2">
                Data sources: GeoDB Cities API, World Bank Open Data • Last updated: {new Date(cityData.lastUpdated).toLocaleDateString()}
              </p>
            </GlassCard>
          )}

          {/* No Data Fallback */}
          {!cityData && !loading && (
            <GlassCard className="p-8 text-center" delay={100}>
              <p className="text-white/60 text-lg">No data available for this city.</p>
              <p className="text-white/40 text-sm mt-2">The backend API may be unavailable or this city is not in the database.</p>
            </GlassCard>
          )}
        </>
      )}
    </div>
  );
}

