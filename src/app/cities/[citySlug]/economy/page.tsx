/**
 * Economy Section Page
 * Route: /cities/[citySlug]/economy
 * 
 * Displays real economic indicators from World Bank API (country-level).
 * Available: GDP per Capita, Unemployment Rate.
 * Removed: Median Income, Job Growth, Startup Density, Industry/Income charts (no free source).
 */

'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { fetchCityData, formatCurrency, formatPercent } from '@/lib/api';
import type { CityData } from '@/lib/api';

// Glassmorphism card component
function GlassCard({ children, className = '', delay = 0 }: { children: React.ReactNode; className?: string; delay?: number }) {
  const [isVisible, setIsVisible] = useState(false);
  
  useEffect(() => {
    const timer = setTimeout(() => setIsVisible(true), delay);
    return () => clearTimeout(timer);
  }, [delay]);

  return (
    <div className={`backdrop-blur-xl bg-white/[0.03] border border-white/10 rounded-2xl transition-all duration-700 ${isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'} ${className}`}>
      {children}
    </div>
  );
}

// Metric display component
function MetricDisplay({ 
  label, 
  value, 
  icon, 
  subtitle,
  iconBg = 'from-cyan-500/20 to-blue-500/20',
  delay = 0
}: { 
  label: string; 
  value: string; 
  icon: React.ReactNode;
  subtitle?: string;
  iconBg?: string;
  delay?: number;
}) {
  return (
    <GlassCard className="p-5 hover:bg-white/[0.05] hover:scale-[1.02] transition-all duration-500 group" delay={delay}>
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
    </GlassCard>
  );
}

export default function EconomyPage() {
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
          <div className="w-12 h-12 border-4 border-cyan-400/30 border-t-cyan-400 rounded-full animate-spin mx-auto" />
          <p className="text-white/50 text-sm">Loading economic data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-6 transition-all duration-700 ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>
      
      {/* KPI Grid — Real data from World Bank */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <MetricDisplay
          label="GDP per Capita"
          value={formatCurrency(city?.gdpPerCapita ?? null)}
          subtitle="Country-level — World Bank"
          iconBg="from-cyan-500/20 to-blue-500/20"
          delay={100}
          icon={
            <svg className="w-6 h-6 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Unemployment Rate"
          value={formatPercent(city?.unemploymentRate ?? null)}
          subtitle="Country-level — World Bank"
          iconBg="from-emerald-500/20 to-teal-500/20"
          delay={200}
          icon={
            <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          }
        />
      </div>

      {/* Data Availability Notice */}
      <GlassCard className="p-5 border-amber-500/10" delay={300}>
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-lg bg-amber-500/10 flex items-center justify-center flex-shrink-0">
            <span className="text-lg">📊</span>
          </div>
          <div className="space-y-2">
            <p className="text-white/70 text-sm">
              <span className="text-white font-medium">Limited Data Available:</span> City-level economic metrics such as 
              median income, job growth rate, startup density, and industry distribution require paid data sources 
              (Bureau of Labor Statistics premium, Numbeo API, etc.) that are not currently integrated.
            </p>
            <p className="text-white/50 text-xs">
              The GDP per Capita and Unemployment Rate shown are country-level indicators from the World Bank Open Data API.
            </p>
          </div>
        </div>
      </GlassCard>

      {/* Data Source Attribution */}
      <GlassCard className="p-6" delay={400}>
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-500/20 flex items-center justify-center flex-shrink-0">
            <svg className="w-6 h-6 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="space-y-3">
            <h3 className="text-lg font-semibold text-white">Data Sources</h3>
            <div className="space-y-2 text-sm text-white/60">
              <p>
                <span className="text-white font-medium">GDP per Capita:</span> World Bank API — Indicator NY.GDP.PCAP.CD (current USD)
              </p>
              <p>
                <span className="text-white font-medium">Unemployment:</span> World Bank API — Indicator SL.UEM.TOTL.ZS (ILO estimate)
              </p>
              <p className="text-white/40 text-xs pt-2 border-t border-white/5">
                All data from free, open international organization APIs. Updated annually. No fabricated data.
              </p>
            </div>
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
