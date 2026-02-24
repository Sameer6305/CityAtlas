/**
 * Analytics Section Page
 * Route: /cities/[citySlug]/analytics
 * 
 * Premium glassmorphism analytics dashboard with scroll-triggered animations.
 * Data sourced from: World Bank API (population), Open-Meteo (AQI).
 * Job sectors and cost of living removed — no free city-level source.
 */

'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams } from 'next/navigation';
import { PopulationChart } from '@/components/charts/PopulationChart';
import { fetchAnalyticsData } from '@/lib/api';
import type { AnalyticsData } from '@/lib/api';

// Custom hook for scroll-triggered animations
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

// Glassmorphism card component with scroll-triggered animation
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
      className={`backdrop-blur-xl bg-white/[0.03] border border-white/10 rounded-2xl transition-all duration-[800ms] ease-out ${isAnimated ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-12 scale-[0.97]'} ${className}`}
    >
      {children}
    </div>
  );
}

// Section header component with scroll-triggered animation
function SectionHeader({ icon, title, description, delay = 0 }: { icon: string; title: string; description: string; delay?: number }) {
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
      className={`flex items-center gap-4 mb-5 transition-all duration-[800ms] ease-out ${isAnimated ? 'opacity-100 translate-x-0' : 'opacity-0 -translate-x-8'}`}
    >
      <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center border border-white/10">
        <span className="text-2xl">{icon}</span>
      </div>
      <div>
        <h2 className="text-xl font-bold text-white">{title}</h2>
        <p className="text-white/50 text-sm">{description}</p>
      </div>
    </div>
  );
}

export default function AnalyticsPage() {
  const [isLoaded, setIsLoaded] = useState(false);
  const params = useParams();
  const citySlug = params.citySlug as string;

  // Real data from backend API (World Bank + Open-Meteo)
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (!citySlug) return;
    setLoading(true);
    fetchAnalyticsData(citySlug).then(data => {
      setAnalytics(data);
      setLoading(false);
    });
  }, [citySlug]);

  const populationData = analytics?.populationTrend ?? [];
  const aqiData = analytics?.aqiTrend ?? [];
  const cityName = citySlug ? citySlug.split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ') : 'City';

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center space-y-4">
          <div className="w-12 h-12 border-4 border-cyan-400/30 border-t-cyan-400 rounded-full animate-spin mx-auto" />
          <p className="text-white/50 text-sm">Loading analytics data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-8 transition-all duration-[1200ms] ease-out ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'}`}>
      
      {/* Page Header */}
      <GlassCard className="p-6 bg-gradient-to-r from-cyan-500/10 via-purple-500/5 to-transparent" delay={50}>
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-cyan-400/30 to-purple-400/30 flex items-center justify-center border border-white/20">
            <svg className="w-7 h-7 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          <div>
            <span className="inline-block px-3 py-1 bg-cyan-500/20 text-cyan-300 rounded-full text-xs font-medium mb-2 border border-cyan-400/30">
              Analytics Dashboard
            </span>
            <h1 className="text-2xl md:text-3xl font-bold text-white">
              {cityName} Performance Metrics
            </h1>
            <p className="text-white/50 mt-1">
              Population trends and environmental data from verified sources
            </p>
          </div>
        </div>
      </GlassCard>

      {/* Demographics Section — World Bank historical population */}
      <div>
        <SectionHeader 
          icon="👥" 
          title="Demographics" 
          description="Country-level population trends from World Bank Open Data"
          delay={50}
        />
        <GlassCard className="p-6" delay={100}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Population Trend (10-Year)</h3>
            <p className="text-white/50 text-sm">Historical country-level population in millions with year-over-year growth rates</p>
          </div>
          {populationData.length > 0 ? (
            <PopulationChart data={populationData} />
          ) : (
            <div className="flex items-center justify-center h-48 text-white/30 text-sm">
              Population data not available for this location
            </div>
          )}
        </GlassCard>
      </div>

      {/* Environmental Quality Section — Open-Meteo data */}
      {aqiData.length > 0 && (
        <div>
          <SectionHeader 
            icon="🌤️" 
            title="Environmental Quality" 
            description="Air quality data from Open-Meteo CAMS satellite"
            delay={50}
          />
          <GlassCard className="p-6" delay={100}>
            <div className="mb-4">
              <h3 className="text-lg font-semibold text-white">Air Quality Index (AQI)</h3>
              <p className="text-white/50 text-sm">Lower values indicate better air quality. Good: 0-50 | Moderate: 51-100</p>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              {aqiData.map((point: { month: string; aqi: number }, idx: number) => (
                <div key={idx} className="p-3 rounded-xl bg-white/[0.03] border border-white/5 text-center">
                  <p className="text-white/40 text-xs mb-1">{point.month}</p>
                  <p className={`text-xl font-bold ${point.aqi <= 50 ? 'text-emerald-400' : point.aqi <= 100 ? 'text-amber-400' : 'text-red-400'}`}>
                    {point.aqi}
                  </p>
                </div>
              ))}
            </div>
          </GlassCard>
        </div>
      )}

      {/* No Job/Cost Data Notice */}
      <GlassCard className="p-5 border-amber-500/10" delay={100}>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-amber-500/10 flex items-center justify-center flex-shrink-0">
            <span className="text-lg">📊</span>
          </div>
          <div>
            <p className="text-white/70 text-sm">
              <span className="text-white font-medium">Employment & Cost of Living:</span> City-level job sector and cost-of-living data 
              require paid data sources. These sections will be available when premium data providers are integrated.
            </p>
          </div>
        </div>
      </GlassCard>

      {/* Data Sources Footer */}
      <GlassCard className="p-6" delay={50}>
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-500/20 flex items-center justify-center flex-shrink-0">
            <svg className="w-6 h-6 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="space-y-3">
            <h3 className="text-lg font-semibold text-white">About This Data</h3>
            <div className="space-y-2 text-sm text-white/60">
              <p>
                <span className="text-white font-medium">Population:</span> World Bank Open Data API — country-level indicators updated annually.
              </p>
              <p>
                <span className="text-white font-medium">Air Quality:</span> Open-Meteo API — real-time air quality from CAMS European satellite data.
              </p>
              <p>
                <span className="text-white font-medium">Update Frequency:</span> Population: Annual | AQI: Real-time (cached 6h)
              </p>
              <p className="text-white/40 text-xs pt-2 border-t border-white/5">
                All data sourced from free, open government and international organization APIs. No fabricated data.
              </p>
            </div>
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
