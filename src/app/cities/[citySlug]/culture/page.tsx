/**
 * Culture Section Page
 * Route: /cities/[citySlug]/culture
 * 
 * Displays languages spoken (from REST Countries API).
 * Removed: All cultural metrics (museums, theaters, restaurants, events,
 *   cultural sites, diversity index, culinary scene, nightlife) — no free source.
 */

'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { fetchCityData } from '@/lib/api';
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

export default function CulturePage() {
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
          <div className="w-12 h-12 border-4 border-purple-400/30 border-t-purple-400 rounded-full animate-spin mx-auto" />
          <p className="text-white/50 text-sm">Loading culture data...</p>
        </div>
      </div>
    );
  }

  const languages = city?.languages ?? [];

  return (
    <div className={`space-y-6 transition-all duration-700 ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>

      {/* Languages — Real data from REST Countries API */}
      {languages.length > 0 && (
        <GlassCard className="p-6" delay={100}>
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <span className="text-xl">🌐</span> Languages
            <span className="text-white/30 text-sm font-normal ml-2">Country-level — REST Countries API</span>
          </h3>
          <div className="flex flex-wrap gap-3">
            {languages.map((lang, i) => (
              <div
                key={lang}
                className="px-4 py-2 rounded-xl bg-gradient-to-br from-purple-500/10 to-indigo-500/10 border border-purple-500/20 text-white/80 text-sm font-medium hover:bg-white/[0.05] transition-colors duration-300"
                style={{ animationDelay: `${i * 50}ms` }}
              >
                {lang}
              </div>
            ))}
          </div>
        </GlassCard>
      )}

      {/* Data Availability Notice */}
      <GlassCard className="p-5 border-amber-500/10" delay={200}>
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-lg bg-amber-500/10 flex items-center justify-center flex-shrink-0">
            <span className="text-lg">🎭</span>
          </div>
          <div className="space-y-2">
            <p className="text-white/70 text-sm">
              <span className="text-white font-medium">Limited Data Available:</span> City-level cultural metrics such as 
              museum and theater counts, restaurant density, cultural events, performing arts venues, 
              diversity indices, culinary scene ratings, and nightlife scores require proprietary data 
              sources not currently integrated.
            </p>
            <p className="text-white/50 text-xs">
              We are committed to displaying only verified, real data from free and open sources.
              Cultural statistics will be added as reliable free APIs become available.
            </p>
          </div>
        </div>
      </GlassCard>

      {/* Data Source Attribution */}
      <GlassCard className="p-6" delay={300}>
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-500/20 to-indigo-500/20 flex items-center justify-center flex-shrink-0">
            <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="space-y-3">
            <h3 className="text-lg font-semibold text-white">Data Sources</h3>
            <div className="space-y-2 text-sm text-white/60">
              <p>
                <span className="text-white font-medium">Languages:</span> REST Countries API — /v3.1/alpha/{'{countryCode}'}
              </p>
              <p className="text-white/40 text-xs pt-2 border-t border-white/5">
                All data from free, open international organization APIs. No fabricated data.
              </p>
            </div>
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
