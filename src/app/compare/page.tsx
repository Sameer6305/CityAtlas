/**
 * City Comparison Page
 * Route: /compare
 * 
 * Side-by-side comparison of two cities using ONLY real API data.
 * Available metrics: Population, GDP per Capita, Unemployment Rate,
 * Literacy Rate, Pupil-Teacher Ratio, Renewable Energy %, CO2 per Capita.
 * 
 * Data Sources: World Bank API (country-level), GeoDB Cities API (city-level)
 */

'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useState, useEffect, useRef, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useComparison } from '@/store/useAppStore';
import { useTilt3D } from '@/lib/useTilt3D';
import { fetchCityData } from '@/lib/api';
import type { CityData as ApiCityData } from '@/lib/api';

// ============================================
// TYPES
// ============================================

interface CompareCityData {
  slug: string;
  name: string;
  country: string;
  population: string;
  populationRaw: number;
  gdpPerCapita: number | null;
  unemploymentRate: number | null;
  literacyRate: number | null;
  pupilTeacherRatio: number | null;
  renewableEnergyPct: number | null;
  co2PerCapita: number | null;
  lifeExpectancy: number | null;
  internetUsersPct: number | null;
  hospitalBedsPer1000: number | null;
  healthExpenditurePerCapita: number | null;
  electricityAccessPct: number | null;
  gradient: string;
  accentColor: string;
}

// Curated city catalog for dropdown (visual info only - data fetched from API)
const CITY_CATALOG = [
  { slug: 'san-francisco', name: 'San Francisco', gradient: 'from-blue-500 to-cyan-500', accentColor: '#06b6d4' },
  { slug: 'austin', name: 'Austin', gradient: 'from-purple-500 to-pink-500', accentColor: '#a855f7' },
  { slug: 'seattle', name: 'Seattle', gradient: 'from-green-500 to-teal-500', accentColor: '#10b981' },
  { slug: 'new-york', name: 'New York', gradient: 'from-yellow-500 to-orange-500', accentColor: '#f59e0b' },
  { slug: 'boston', name: 'Boston', gradient: 'from-red-500 to-orange-500', accentColor: '#ef4444' },
];

function getCityVisual(slug: string) {
  return CITY_CATALOG.find(c => c.slug === slug) || { slug, name: slug, gradient: 'from-gray-500 to-gray-600', accentColor: '#6b7280' };
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
  if (num >= 1000) return (num / 1000).toFixed(0) + 'K';
  return num.toString();
}

function getWinner(valueA: number | null, valueB: number | null, higherIsBetter: boolean = true): 'A' | 'B' | 'tie' | 'none' {
  if (valueA == null || valueB == null) return 'none';
  if (valueA === valueB) return 'tie';
  if (higherIsBetter) return valueA > valueB ? 'A' : 'B';
  return valueA < valueB ? 'A' : 'B';
}

// ============================================
// COMPONENTS
// ============================================

function useScrollAnimation(threshold = 0.1) {
  const ref = useRef<HTMLDivElement>(null);
  const [isVisible, setIsVisible] = useState(false);
  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => { if (entry.isIntersecting) setIsVisible(true); }, { threshold, rootMargin: '50px' });
    const el = ref.current;
    if (el) observer.observe(el);
    return () => { if (el) observer.unobserve(el); };
  }, [threshold]);
  return { ref, isVisible };
}

function GlassCard({ children, className = '', delay = 0, enableTilt = true }: { children: React.ReactNode; className?: string; delay?: number; enableTilt?: boolean }) {
  const { ref, isVisible } = useScrollAnimation(0.1);
  const [isAnimated, setIsAnimated] = useState(false);
  const tilt = useTilt3D({ maxTilt: 8, scale: 1.01, speed: 150, enabled: enableTilt });
  useEffect(() => { if (isVisible) { const t = setTimeout(() => setIsAnimated(true), delay); return () => clearTimeout(t); } }, [isVisible, delay]);
  return (
    <div
      ref={(el) => {
        (ref as React.MutableRefObject<HTMLDivElement | null>).current = el;
        (tilt.ref as React.MutableRefObject<HTMLDivElement | null>).current = el;
      }}
      className={`backdrop-blur-xl bg-white/[0.03] border border-white/[0.08] rounded-2xl transition-all duration-300 ease-out hover:bg-white/[0.06] hover:border-white/[0.15] hover:shadow-2xl hover:shadow-cyan-500/10 ${isAnimated ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-12'} ${className}`}
      style={{ ...tilt.style, transformStyle: 'preserve-3d' as const, willChange: 'transform' }}
      onMouseMove={tilt.onMouseMove} onMouseEnter={tilt.onMouseEnter} onMouseLeave={tilt.onMouseLeave}
    >
      {children}
    </div>
  );
}

function CitySelector({ value, onChange, excludeSlug, label }: { value: string; onChange: (slug: string) => void; excludeSlug?: string; label: string }) {
  const cities = CITY_CATALOG.filter(c => c.slug !== excludeSlug);
  return (
    <div className="space-y-2">
      <label className="text-white/60 text-sm font-medium">{label}</label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full px-4 py-3 bg-white/[0.05] border border-white/[0.1] rounded-xl text-white focus:outline-none focus:border-cyan-500/50 focus:ring-2 focus:ring-cyan-500/20 transition-all appearance-none cursor-pointer"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='white'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E")`,
          backgroundRepeat: 'no-repeat', backgroundPosition: 'right 12px center', backgroundSize: '20px',
        }}
      >
        <option value="" className="bg-[#0f1420] text-white/60">Select a city...</option>
        {cities.map((city) => (
          <option key={city.slug} value={city.slug} className="bg-[#0f1420] text-white">{city.name}</option>
        ))}
      </select>
    </div>
  );
}

function ComparisonMetric({ label, valueA, valueB, formatFn, unit = '', higherIsBetter = true }: {
  label: string; valueA: number | null; valueB: number | null; formatFn?: (v: number) => string; unit?: string; higherIsBetter?: boolean;
}) {
  const winner = getWinner(valueA, valueB, higherIsBetter);
  const fmt = formatFn || ((v: number) => v.toString());
  return (
    <div className="grid grid-cols-3 gap-4 py-4 border-b border-white/[0.05] last:border-0">
      <div className={`flex items-center justify-start gap-2 ${winner === 'A' ? 'text-emerald-400' : 'text-white/70'}`}>
        {winner === 'A' && (
          <svg className="w-4 h-4 text-emerald-400" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
        )}
        <span className="font-semibold text-lg">{valueA != null ? `${fmt(valueA)}${unit}` : 'N/A'}</span>
      </div>
      <div className="flex items-center justify-center text-center">
        <span className="text-white/60 text-sm font-medium">{label}</span>
      </div>
      <div className={`flex items-center justify-end gap-2 ${winner === 'B' ? 'text-emerald-400' : 'text-white/70'}`}>
        <span className="font-semibold text-lg">{valueB != null ? `${fmt(valueB)}${unit}` : 'N/A'}</span>
        {winner === 'B' && (
          <svg className="w-4 h-4 text-emerald-400" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
        )}
      </div>
    </div>
  );
}

// ============================================
// MAIN PAGE COMPONENT
// ============================================

function ComparePageContent() {
  const searchParams = useSearchParams();
  const { comparisonList } = useComparison();

  const [isLoaded, setIsLoaded] = useState(false);
  const [cityASlug, setCityASlug] = useState<string>('');
  const [cityBSlug, setCityBSlug] = useState<string>('');
  const [cityA, setCityA] = useState<CompareCityData | null>(null);
  const [cityB, setCityB] = useState<CompareCityData | null>(null);
  const [loadingA, setLoadingA] = useState(false);
  const [loadingB, setLoadingB] = useState(false);

  // Map API data to comparison data
  function mapApiData(slug: string, data: ApiCityData): CompareCityData {
    const visual = getCityVisual(slug);
    return {
      slug,
      name: data.name || visual.name,
      country: data.country || '',
      population: data.population != null ? formatNumber(data.population) : 'N/A',
      populationRaw: data.population ?? 0,
      gdpPerCapita: data.gdpPerCapita ?? null,
      unemploymentRate: data.unemploymentRate ?? null,
      literacyRate: data.literacyRate ?? null,
      pupilTeacherRatio: data.pupilTeacherRatio ?? null,
      renewableEnergyPct: data.renewableEnergyPct ?? null,
      co2PerCapita: data.co2PerCapita ?? null,
      lifeExpectancy: data.lifeExpectancy ?? null,
      internetUsersPct: data.internetUsersPct ?? null,
      hospitalBedsPer1000: data.hospitalBedsPer1000 ?? null,
      healthExpenditurePerCapita: data.healthExpenditurePerCapita ?? null,
      electricityAccessPct: data.electricityAccessPct ?? null,
      gradient: visual.gradient,
      accentColor: visual.accentColor,
    };
  }

  // Initialize from URL params or comparison list
  useEffect(() => {
    const a = searchParams.get('cityA');
    const b = searchParams.get('cityB');
    if (a) setCityASlug(a);
    else if (comparisonList.length > 0) setCityASlug(comparisonList[0]);
    if (b) setCityBSlug(b);
    else if (comparisonList.length > 1) setCityBSlug(comparisonList[1]);
    const t = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(t);
  }, [searchParams, comparisonList]);

  // Fetch city A data
  useEffect(() => {
    if (!cityASlug) { setCityA(null); return; }
    setLoadingA(true);
    fetchCityData(cityASlug).then(data => {
      setCityA(data ? mapApiData(cityASlug, data) : null);
      setLoadingA(false);
    });
  }, [cityASlug]);

  // Fetch city B data
  useEffect(() => {
    if (!cityBSlug) { setCityB(null); return; }
    setLoadingB(true);
    fetchCityData(cityBSlug).then(data => {
      setCityB(data ? mapApiData(cityBSlug, data) : null);
      setLoadingB(false);
    });
  }, [cityBSlug]);

  const canCompare = cityA && cityB && !loadingA && !loadingB;

  return (
    <div className={`relative min-h-screen overflow-hidden transition-opacity duration-[1200ms] ease-out ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>
      {/* Full-screen Background */}
      <div className="fixed inset-0 -z-10">
        <Image src="/background.png" alt="City Skyline" fill className="object-cover" priority quality={100} />
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0e27]/10 via-[#0f1420]/5 to-[#000000]/15" />
      </div>

      {/* Main Content */}
      <div className="min-h-screen p-6 lg:p-8">
        {/* Header */}
        <div className="max-w-7xl mx-auto mb-8">
          <div className="flex items-center gap-4 mb-6">
            <Link href="/cities" className="p-2 rounded-xl bg-white/[0.05] border border-white/[0.08] hover:bg-white/[0.1] transition-all">
              <svg className="w-5 h-5 text-white/60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
            </Link>
            <div>
              <h1 className="text-3xl font-bold text-white">City Comparison</h1>
              <p className="text-white/50">Compare cities side-by-side using verified data</p>
            </div>
          </div>

          {/* City Selectors */}
          <GlassCard className="p-6" delay={50}>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <div className={`w-4 h-4 rounded-full bg-gradient-to-r ${getCityVisual(cityASlug).gradient}`} />
                <CitySelector label="City A" value={cityASlug} onChange={setCityASlug} excludeSlug={cityBSlug} />
                {loadingA && <p className="text-white/40 text-sm animate-pulse">Loading city data...</p>}
                {cityA && (
                  <div className="flex items-center gap-3 p-3 bg-white/[0.02] rounded-xl">
                    <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${cityA.gradient} flex items-center justify-center text-white font-bold`}>
                      {cityA.name.charAt(0)}
                    </div>
                    <div>
                      <p className="text-white font-medium">{cityA.name}</p>
                      <p className="text-white/50 text-sm">{cityA.country} &bull; Pop: {cityA.population}</p>
                    </div>
                  </div>
                )}
              </div>
              <div className="space-y-4">
                <div className={`w-4 h-4 rounded-full bg-gradient-to-r ${getCityVisual(cityBSlug).gradient}`} />
                <CitySelector label="City B" value={cityBSlug} onChange={setCityBSlug} excludeSlug={cityASlug} />
                {loadingB && <p className="text-white/40 text-sm animate-pulse">Loading city data...</p>}
                {cityB && (
                  <div className="flex items-center gap-3 p-3 bg-white/[0.02] rounded-xl">
                    <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${cityB.gradient} flex items-center justify-center text-white font-bold`}>
                      {cityB.name.charAt(0)}
                    </div>
                    <div>
                      <p className="text-white font-medium">{cityB.name}</p>
                      <p className="text-white/50 text-sm">{cityB.country} &bull; Pop: {cityB.population}</p>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </GlassCard>
        </div>

        {/* Comparison Content */}
        {canCompare ? (
          <div className="max-w-7xl mx-auto space-y-6">
            {/* City Headers */}
            <div className="grid grid-cols-3 gap-4 text-center">
              <div className={`p-4 rounded-xl bg-gradient-to-br ${cityA.gradient}/20 border border-white/[0.08]`}>
                <h2 className="text-xl font-bold text-white">{cityA.name}</h2>
                <p className="text-white/50 text-sm">{cityA.country}</p>
              </div>
              <div className="flex items-center justify-center">
                <span className="text-white/40 text-2xl font-bold">VS</span>
              </div>
              <div className={`p-4 rounded-xl bg-gradient-to-br ${cityB.gradient}/20 border border-white/[0.08]`}>
                <h2 className="text-xl font-bold text-white">{cityB.name}</h2>
                <p className="text-white/50 text-sm">{cityB.country}</p>
              </div>
            </div>

            {/* Economy — Real data from World Bank */}
            <GlassCard className="p-6" delay={100}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500/20 to-teal-500/20 flex items-center justify-center">
                  <svg className="w-5 h-5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">Economy</h3>
                  <p className="text-white/50 text-sm">Country-level indicators — World Bank</p>
                </div>
              </div>
              <div className="space-y-2">
                <ComparisonMetric label="GDP per Capita" valueA={cityA.gdpPerCapita} valueB={cityB.gdpPerCapita} formatFn={(v) => `$${v.toLocaleString()}`} higherIsBetter={true} />
                <ComparisonMetric label="Unemployment Rate" valueA={cityA.unemploymentRate} valueB={cityB.unemploymentRate} unit="%" higherIsBetter={false} />
              </div>
            </GlassCard>

            {/* Education — Real data from World Bank */}
            <GlassCard className="p-6" delay={120}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500/20 to-indigo-500/20 flex items-center justify-center">
                  <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">Education</h3>
                  <p className="text-white/50 text-sm">Country-level indicators — World Bank</p>
                </div>
              </div>
              <div className="space-y-2">
                <ComparisonMetric label="Literacy Rate" valueA={cityA.literacyRate} valueB={cityB.literacyRate} formatFn={(v) => v.toFixed(1)} unit="%" higherIsBetter={true} />
                <ComparisonMetric label="Pupil-Teacher Ratio" valueA={cityA.pupilTeacherRatio} valueB={cityB.pupilTeacherRatio} formatFn={(v) => `${v.toFixed(1)}:1`} higherIsBetter={false} />
              </div>
            </GlassCard>

            {/* Environment — Real data from World Bank */}
            <GlassCard className="p-6" delay={140}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-green-500/20 to-emerald-500/20 flex items-center justify-center">
                  <svg className="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">Environment</h3>
                  <p className="text-white/50 text-sm">Country-level indicators — World Bank</p>
                </div>
              </div>
              <div className="space-y-2">
                <ComparisonMetric label="Renewable Energy" valueA={cityA.renewableEnergyPct} valueB={cityB.renewableEnergyPct} formatFn={(v) => v.toFixed(1)} unit="%" higherIsBetter={true} />
                <ComparisonMetric label="CO₂ per Capita" valueA={cityA.co2PerCapita} valueB={cityB.co2PerCapita} formatFn={(v) => v.toFixed(1)} unit=" tons" higherIsBetter={false} />
              </div>
            </GlassCard>

            {/* Health & Safety — Real data from World Bank */}
            <GlassCard className="p-6" delay={160}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-red-500/20 to-pink-500/20 flex items-center justify-center">
                  <svg className="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">Health &amp; Safety</h3>
                  <p className="text-white/50 text-sm">Country-level indicators — World Bank</p>
                </div>
              </div>
              <div className="space-y-2">
                <ComparisonMetric label="Life Expectancy" valueA={cityA.lifeExpectancy} valueB={cityB.lifeExpectancy} formatFn={(v) => v.toFixed(1)} unit=" yr" higherIsBetter={true} />
                <ComparisonMetric label="Hospital Beds" valueA={cityA.hospitalBedsPer1000} valueB={cityB.hospitalBedsPer1000} formatFn={(v) => v.toFixed(1)} unit="/1K" higherIsBetter={true} />
                <ComparisonMetric label="Health Spending" valueA={cityA.healthExpenditurePerCapita} valueB={cityB.healthExpenditurePerCapita} formatFn={(v) => `$${v.toLocaleString()}`} higherIsBetter={true} />
              </div>
            </GlassCard>

            {/* Infrastructure — Real data from World Bank */}
            <GlassCard className="p-6" delay={180}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-500/20 flex items-center justify-center">
                  <svg className="w-5 h-5 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.14 0M1.394 9.393c5.857-5.858 15.355-5.858 21.213 0" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">Infrastructure</h3>
                  <p className="text-white/50 text-sm">Country-level indicators — World Bank</p>
                </div>
              </div>
              <div className="space-y-2">
                <ComparisonMetric label="Internet Users" valueA={cityA.internetUsersPct} valueB={cityB.internetUsersPct} formatFn={(v) => v.toFixed(1)} unit="%" higherIsBetter={true} />
                <ComparisonMetric label="Electricity Access" valueA={cityA.electricityAccessPct} valueB={cityB.electricityAccessPct} formatFn={(v) => v.toFixed(1)} unit="%" higherIsBetter={true} />
              </div>
            </GlassCard>

            {/* Data Source Notice */}
            <GlassCard className="p-5 border-blue-500/10" delay={200}>
              <div className="flex items-start gap-4">
                <div className="w-10 h-10 rounded-lg bg-blue-500/10 flex items-center justify-center flex-shrink-0">
                  <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div className="space-y-2">
                  <p className="text-white/70 text-sm">
                    <span className="text-white font-medium">About this comparison:</span> All metrics shown are from 
                    verified, free data sources. Economic, education, environmental, health, and infrastructure indicators are 
                    {' '}<strong className="text-white/80">country-level</strong> data from the World Bank Open Data API. 
                    Population is city-level from GeoDB Cities.
                  </p>
                  <p className="text-white/40 text-xs">
                    Some metrics may show as &quot;N/A&quot; when data is unavailable for a particular country.
                  </p>
                </div>
              </div>
            </GlassCard>
          </div>
        ) : (
          /* Empty State */
          <div className="max-w-7xl mx-auto">
            <GlassCard className="p-12 text-center" delay={100}>
              <div className="w-20 h-20 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center">
                <svg className="w-10 h-10 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <h3 className="text-2xl font-semibold text-white mb-3">Select Two Cities to Compare</h3>
              <p className="text-white/50 max-w-md mx-auto">
                Choose cities from the dropdowns above to see a detailed side-by-side comparison 
                using verified data from the World Bank and GeoDB Cities APIs.
              </p>
            </GlassCard>
          </div>
        )}
      </div>
    </div>
  );
}

function ComparePageLoading() {
  return (
    <div className="relative min-h-screen overflow-hidden">
      <div className="fixed inset-0 -z-10">
        <Image src="/background.png" alt="City Skyline" fill className="object-cover" priority quality={100} />
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0e27]/10 via-[#0f1420]/5 to-[#000000]/15" />
      </div>
      <div className="min-h-screen p-6 lg:p-8 flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center animate-pulse">
            <svg className="w-8 h-8 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          <p className="text-white/50">Loading comparison...</p>
        </div>
      </div>
    </div>
  );
}

export default function ComparePage() {
  return (
    <Suspense fallback={<ComparePageLoading />}>
      <ComparePageContent />
    </Suspense>
  );
}
