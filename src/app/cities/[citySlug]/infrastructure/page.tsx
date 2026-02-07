/**
 * Infrastructure & Connectivity Section Page
 * Route: /cities/[citySlug]/infrastructure
 * 
 * Displays REAL infrastructure indicators from World Bank API (country-level):
 * - Internet Users (% of population) — IT.NET.USER.ZS
 * - Mobile Subscriptions per 100 people — IT.CEL.SETS.P2
 * - Access to Electricity (% of population) — EG.ELC.ACCS.ZS
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

function ProgressBar({ value, max = 100, color = 'cyan' }: { value: number; max?: number; color?: string }) {
  const pct = Math.min((value / max) * 100, 100);
  const colorMap: Record<string, string> = {
    cyan: 'from-cyan-500 to-blue-500',
    emerald: 'from-emerald-500 to-teal-500',
    amber: 'from-amber-500 to-orange-500',
    purple: 'from-purple-500 to-pink-500',
  };
  return (
    <div className="w-full h-2.5 rounded-full bg-white/[0.05] overflow-hidden">
      <div
        className={`h-full rounded-full bg-gradient-to-r ${colorMap[color] || colorMap.cyan} transition-all duration-1000 ease-out`}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}

export default function InfrastructurePage() {
  const [isLoaded, setIsLoaded] = useState(false);
  const params = useParams();
  const citySlug = params.citySlug as string;
  const [city, setCity] = useState<CityData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const t = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(t);
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
          <div className="w-12 h-12 border-4 border-blue-400/30 border-t-blue-400 rounded-full animate-spin mx-auto" />
          <p className="text-white/50 text-sm">Loading infrastructure data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-8 transition-all duration-[1200ms] ease-out ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'}`}>

      {/* KPI Grid — Real World Bank Data */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <MetricDisplay
          label="Internet Users"
          value={city?.internetUsersPct != null ? `${city.internetUsersPct}%` : 'N/A'}
          subtitle="% of population — World Bank"
          iconBg="from-cyan-500/20 to-blue-500/20"
          delay={100}
          icon={
            <svg className="w-6 h-6 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
            </svg>
          }
        />
        <MetricDisplay
          label="Mobile Subscriptions"
          value={city?.mobileSubscriptionsPer100 != null ? `${city.mobileSubscriptionsPer100}` : 'N/A'}
          subtitle="Per 100 people — World Bank"
          iconBg="from-purple-500/20 to-pink-500/20"
          delay={200}
          icon={
            <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Electricity Access"
          value={city?.electricityAccessPct != null ? `${city.electricityAccessPct}%` : 'N/A'}
          subtitle="% of population — World Bank"
          iconBg="from-amber-500/20 to-orange-500/20"
          delay={300}
          icon={
            <svg className="w-6 h-6 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          }
        />
      </div>

      {/* Connectivity Breakdown */}
      <GlassCard className="p-6" delay={200}>
        <h3 className="text-lg font-semibold text-white mb-5 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center">
            <svg className="w-5 h-5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          Digital Connectivity Breakdown
        </h3>
        <div className="space-y-5">
          {city?.internetUsersPct != null && (
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-white/70">Internet Penetration</span>
                <span className="text-cyan-400 font-medium">{city.internetUsersPct}%</span>
              </div>
              <ProgressBar value={city.internetUsersPct} color="cyan" />
            </div>
          )}
          {city?.electricityAccessPct != null && (
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-white/70">Electricity Access</span>
                <span className="text-amber-400 font-medium">{city.electricityAccessPct}%</span>
              </div>
              <ProgressBar value={city.electricityAccessPct} color="amber" />
            </div>
          )}
          {city?.mobileSubscriptionsPer100 != null && (
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-white/70">Mobile Saturation</span>
                <span className="text-purple-400 font-medium">{city.mobileSubscriptionsPer100} / 100</span>
              </div>
              <ProgressBar value={city.mobileSubscriptionsPer100} max={200} color="purple" />
            </div>
          )}
        </div>
      </GlassCard>

      {/* Data Sources */}
      <GlassCard className="p-6" delay={300}>
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500/20 to-cyan-500/20 flex items-center justify-center flex-shrink-0">
            <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="space-y-3">
            <h3 className="text-lg font-semibold text-white">Data Sources</h3>
            <div className="space-y-2 text-sm text-white/60">
              <p><span className="text-white font-medium">Internet Users:</span> World Bank — IT.NET.USER.ZS</p>
              <p><span className="text-white font-medium">Mobile Subscriptions:</span> World Bank — IT.CEL.SETS.P2</p>
              <p><span className="text-white font-medium">Electricity Access:</span> World Bank — EG.ELC.ACCS.ZS</p>
              <p className="text-white/40 text-xs pt-2 border-t border-white/5">
                All indicators are country-level from the World Bank Open Data API. No fabricated data.
              </p>
            </div>
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
