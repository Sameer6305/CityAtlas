/**
 * Education Section Page
 * Route: /cities/[citySlug]/education
 * 
 * Displays real education indicators from World Bank API (country-level).
 * Available: Literacy Rate, Pupil-Teacher Ratio.
 * Removed: Universities count, Research Output, Top Universities list,
 *   Enrollment Trends, Education Funding, STEM Pipeline (no free source).
 */

'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { fetchCityData, formatPercent } from '@/lib/api';
import type { CityData } from '@/lib/api';

function GlassCard({ children, className = '', delay = 0 }: { children: React.ReactNode; className?: string; delay?: number }) {
  const [isVisible, setIsVisible] = useState(false);
  useEffect(() => { const t = setTimeout(() => setIsVisible(true), delay); return () => clearTimeout(t); }, [delay]);
  return (
    <div className={`backdrop-blur-xl bg-white/[0.03] border border-white/10 rounded-2xl transition-all duration-700 ${isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'} ${className}`}>
      {children}
    </div>
  );
}

function MetricDisplay({ label, value, subtitle, icon, iconBg = 'from-cyan-500/20 to-blue-500/20', delay = 0 }: {
  label: string; value: string; subtitle?: string; icon: React.ReactNode; iconBg?: string; delay?: number;
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

export default function EducationPage() {
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
          <p className="text-white/50 text-sm">Loading education data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-6 transition-all duration-700 ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>
      
      {/* KPI Grid — Real data from World Bank */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <MetricDisplay
          label="Literacy Rate"
          value={formatPercent(city?.literacyRate ?? null)}
          subtitle="Country-level — World Bank"
          iconBg="from-blue-500/20 to-indigo-500/20"
          delay={100}
          icon={
            <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          }
        />
        <MetricDisplay
          label="Pupil-Teacher Ratio"
          value={city?.pupilTeacherRatio != null ? `${city.pupilTeacherRatio}:1` : 'N/A'}
          subtitle="Country-level — World Bank"
          iconBg="from-emerald-500/20 to-teal-500/20"
          delay={200}
          icon={
            <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          }
        />
      </div>

      {/* Data Availability Notice */}
      <GlassCard className="p-5 border-amber-500/10" delay={300}>
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-lg bg-amber-500/10 flex items-center justify-center flex-shrink-0">
            <span className="text-lg">🎓</span>
          </div>
          <div className="space-y-2">
            <p className="text-white/70 text-sm">
              <span className="text-white font-medium">Limited Data Available:</span> City-level education metrics such as 
              university counts, research output, enrollment trends, education funding, and STEM pipeline statistics 
              require proprietary data sources not currently integrated.
            </p>
            <p className="text-white/50 text-xs">
              The Literacy Rate and Pupil-Teacher Ratio shown are country-level indicators from the World Bank Open Data API.
            </p>
          </div>
        </div>
      </GlassCard>

      {/* Data Source Attribution */}
      <GlassCard className="p-6" delay={400}>
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500/20 to-indigo-500/20 flex items-center justify-center flex-shrink-0">
            <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="space-y-3">
            <h3 className="text-lg font-semibold text-white">Data Sources</h3>
            <div className="space-y-2 text-sm text-white/60">
              <p>
                <span className="text-white font-medium">Literacy Rate:</span> World Bank API — Indicator SE.ADT.LITR.ZS
              </p>
              <p>
                <span className="text-white font-medium">Pupil-Teacher Ratio:</span> World Bank API — Indicator SE.PRM.ENRL.TC.ZS
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
