/**
 * Environment Section Page
 * Route: /cities/[citySlug]/environment
 * 
 * Displays real environmental indicators:
 * - Renewable Energy %, CO2 per Capita (World Bank, country-level)
 * - Live Weather: temperature, humidity, wind (OpenWeatherMap, city-level)
 * - Live Air Quality: AQI, PM2.5 (OpenAQ, city-level)
 */

'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams } from 'next/navigation';
import { fetchCityData } from '@/lib/api';
import type { CityData } from '@/lib/api';

function useScrollAnimation(threshold = 0.1) {
  const ref = useRef<HTMLDivElement>(null);
  const [isVisible, setIsVisible] = useState(false);
  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setIsVisible(true); },
      { threshold, rootMargin: '50px' }
    );
    const el = ref.current;
    if (el) observer.observe(el);
    return () => { if (el) observer.unobserve(el); };
  }, [threshold]);
  return { ref, isVisible };
}

function GlassCard({ children, className = '', delay = 0 }: { children: React.ReactNode; className?: string; delay?: number }) {
  const { ref, isVisible: isInView } = useScrollAnimation(0.1);
  const [isAnimated, setIsAnimated] = useState(false);
  useEffect(() => {
    if (isInView) { const t = setTimeout(() => setIsAnimated(true), delay); return () => clearTimeout(t); }
  }, [isInView, delay]);
  return (
    <div ref={ref}
      className={`backdrop-blur-xl bg-white/[0.03] border border-white/[0.08] rounded-2xl transition-all duration-[800ms] ease-out hover:bg-white/[0.06] hover:border-white/[0.15] ${isAnimated ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-12 scale-[0.97]'} ${className}`}
    >{children}</div>
  );
}

function MetricDisplay({ label, value, subtitle, icon, iconBg = 'from-cyan-500/20 to-blue-500/20', delay = 0 }: {
  label: string; value: string; subtitle?: string; icon: React.ReactNode; iconBg?: string; delay?: number;
}) {
  const { ref, isVisible: isInView } = useScrollAnimation(0.1);
  const [isAnimated, setIsAnimated] = useState(false);
  useEffect(() => {
    if (isInView) { const t = setTimeout(() => setIsAnimated(true), delay); return () => clearTimeout(t); }
  }, [isInView, delay]);
  return (
    <div ref={ref}
      className={`backdrop-blur-xl bg-white/[0.03] border border-white/[0.08] rounded-2xl p-5 hover:bg-white/[0.06] hover:border-white/[0.15] hover:scale-[1.03] transition-all duration-[800ms] ease-out group ${isAnimated ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-12 scale-[0.97]'}`}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-white/50 text-sm font-medium mb-1">{label}</p>
          <p className="text-3xl font-bold text-white">{value}</p>
          {subtitle && <p className="text-white/40 text-sm mt-1">{subtitle}</p>}
        </div>
        <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${iconBg} flex items-center justify-center group-hover:scale-110 transition-transform duration-500`}>
          {icon}
        </div>
      </div>
    </div>
  );
}

function getAqiColor(aqi: number): string {
  if (aqi <= 50) return 'text-emerald-400';
  if (aqi <= 100) return 'text-yellow-400';
  if (aqi <= 150) return 'text-orange-400';
  if (aqi <= 200) return 'text-red-400';
  return 'text-purple-400';
}

function getAqiBg(aqi: number): string {
  if (aqi <= 50) return 'from-emerald-500/20 to-green-500/20';
  if (aqi <= 100) return 'from-yellow-500/20 to-amber-500/20';
  if (aqi <= 150) return 'from-orange-500/20 to-red-500/20';
  return 'from-red-500/20 to-purple-500/20';
}

export default function EnvironmentPage() {
  const [isLoaded, setIsLoaded] = useState(false);
  const params = useParams();
  const citySlug = params.citySlug as string;
  const [city, setCity] = useState<CityData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (!citySlug) return;
    setLoading(true);
    fetchCityData(citySlug).then(data => {
      setCity(data);
      setLoading(false);
    });
  }, [citySlug]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center space-y-4">
          <div className="w-12 h-12 border-4 border-emerald-400/30 border-t-emerald-400 rounded-full animate-spin mx-auto" />
          <p className="text-white/50 text-sm">Loading environmental data...</p>
        </div>
      </div>
    );
  }

  const hasWeather = city?.weatherTemp != null;
  const hasAqi = city?.airQualityIndex != null;

  return (
    <div className={`space-y-8 transition-all duration-[1200ms] ease-out ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'}`}>
      
      {/* Live Weather — OpenWeatherMap (city-level) */}
      {hasWeather && (
        <GlassCard className="p-6 bg-gradient-to-r from-blue-500/10 via-cyan-500/5 to-transparent" delay={50}>
          <div className="flex items-center gap-2 mb-4">
            <span className="w-3 h-3 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-emerald-400 text-xs font-medium uppercase tracking-wider">Live Weather</span>
            <span className="text-white/30 text-xs ml-2">OpenWeatherMap — city-level</span>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="text-center p-4 rounded-xl bg-white/[0.03] border border-white/5">
              <p className="text-4xl font-bold text-white">{city.weatherTemp}°C</p>
              <p className="text-white/50 text-sm mt-1">Temperature</p>
            </div>
            <div className="text-center p-4 rounded-xl bg-white/[0.03] border border-white/5">
              <p className="text-2xl font-bold text-white capitalize">{city.weatherDescription}</p>
              <p className="text-white/50 text-sm mt-1">Condition</p>
            </div>
            <div className="text-center p-4 rounded-xl bg-white/[0.03] border border-white/5">
              <p className="text-3xl font-bold text-white">{city.weatherHumidity}%</p>
              <p className="text-white/50 text-sm mt-1">Humidity</p>
            </div>
            <div className="text-center p-4 rounded-xl bg-white/[0.03] border border-white/5">
              <p className="text-3xl font-bold text-white">{city.weatherWindSpeed} m/s</p>
              <p className="text-white/50 text-sm mt-1">Wind Speed</p>
            </div>
          </div>
        </GlassCard>
      )}

      {/* Live Air Quality — OpenAQ (city-level) */}
      {hasAqi && city.airQualityIndex != null && (
        <GlassCard className="p-6" delay={100}>
          <div className="flex items-center gap-2 mb-4">
            <span className="w-3 h-3 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-emerald-400 text-xs font-medium uppercase tracking-wider">Live Air Quality</span>
            <span className="text-white/30 text-xs ml-2">OpenAQ — monitoring stations</span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className={`text-center p-5 rounded-xl bg-gradient-to-br ${getAqiBg(city.airQualityIndex)} border border-white/10`}>
              <p className={`text-5xl font-bold ${getAqiColor(city.airQualityIndex)}`}>{city.airQualityIndex}</p>
              <p className="text-white/70 text-sm mt-2 font-medium">{city.airQualityCategory || 'AQI'}</p>
            </div>
            {city.pm25 != null && (
              <div className="text-center p-5 rounded-xl bg-white/[0.03] border border-white/5">
                <p className="text-3xl font-bold text-white">{city.pm25}</p>
                <p className="text-white/50 text-sm mt-1">PM2.5 (μg/m³)</p>
              </div>
            )}
            <div className="flex items-center justify-center p-5 rounded-xl bg-white/[0.03] border border-white/5">
              <div className="text-center">
                <div className="flex gap-1 justify-center mb-2">
                  {['Good', 'Moderate', 'Unhealthy', 'Very Unhealthy', 'Hazardous'].map((cat, i) => (
                    <div key={cat} className={`w-3 h-8 rounded-sm ${i === 0 ? 'bg-emerald-500' : i === 1 ? 'bg-yellow-500' : i === 2 ? 'bg-orange-500' : i === 3 ? 'bg-red-500' : 'bg-purple-500'} ${city.airQualityCategory?.toLowerCase().includes(cat.toLowerCase().split(' ')[0]) ? 'opacity-100 scale-y-125' : 'opacity-30'}`} />
                  ))}
                </div>
                <p className="text-white/50 text-xs">AQI Scale</p>
              </div>
            </div>
          </div>
        </GlassCard>
      )}

      {/* Country-level indicators — World Bank */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <MetricDisplay
          label="Renewable Energy"
          value={city?.renewableEnergyPct != null ? `${city.renewableEnergyPct}%` : 'N/A'}
          subtitle="% of total consumption — World Bank"
          iconBg="from-emerald-500/20 to-green-500/20"
          delay={150}
          icon={
            <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          }
        />
        <MetricDisplay
          label="CO₂ per Capita"
          value={city?.co2PerCapita != null ? `${city.co2PerCapita} t` : 'N/A'}
          subtitle="Metric tons — World Bank"
          iconBg="from-cyan-500/20 to-blue-500/20"
          delay={200}
          icon={
            <svg className="w-6 h-6 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
            </svg>
          }
        />
      </div>

      {/* Data Sources */}
      <GlassCard className="p-6" delay={250}>
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-emerald-500/20 to-green-500/20 flex items-center justify-center flex-shrink-0">
            <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="space-y-3">
            <h3 className="text-lg font-semibold text-white">Data Sources</h3>
            <div className="space-y-2 text-sm text-white/60">
              {hasWeather && (
                <p><span className="text-emerald-400 font-medium">🟢 Live</span> <span className="text-white font-medium">Weather:</span> OpenWeatherMap API — city-level, refreshed every 15 min</p>
              )}
              {hasAqi && (
                <p><span className="text-emerald-400 font-medium">🟢 Live</span> <span className="text-white font-medium">Air Quality:</span> OpenAQ — government monitoring stations</p>
              )}
              <p><span className="text-white font-medium">Renewable Energy:</span> World Bank — EG.FEC.RNEW.ZS (country-level)</p>
              <p><span className="text-white font-medium">CO₂ Emissions:</span> World Bank — EN.ATM.CO2E.PC (country-level)</p>
              {!hasWeather && !hasAqi && (
                <p className="text-white/40 text-xs italic">Weather & AQI require API keys (OPENWEATHER_API_KEY / OPENAQ_API_KEY). Set them to enable live data.</p>
              )}
              <p className="text-white/40 text-xs pt-2 border-t border-white/5">
                All data from free, open APIs. No fabricated data.
              </p>
            </div>
          </div>
        </div>
      </GlassCard>
    </div>
  );
}

