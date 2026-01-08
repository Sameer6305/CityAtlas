/**
 * Analytics Section Page
 * Route: /cities/[citySlug]/analytics
 * 
 * Premium glassmorphism analytics dashboard with scroll-triggered animations
 */

'use client';

import { useState, useEffect, useRef } from 'react';
import { AQITrendChart } from '@/components/charts/AQITrendChart';
import { JobSectorChart } from '@/components/charts/JobSectorChart';
import { CostOfLivingChart } from '@/components/charts/CostOfLivingChart';
import { PopulationChart } from '@/components/charts/PopulationChart';
import { 
  getAQITrendData, 
  getJobSectorData, 
  getCostOfLivingData, 
  getPopulationData 
} from '@/lib/mock';

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
  
  const aqiData = getAQITrendData();
  const jobData = getJobSectorData();
  const costData = getCostOfLivingData();
  const populationData = getPopulationData();

  useEffect(() => {
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, []);

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
              City Performance Metrics
            </h1>
            <p className="text-white/50 mt-1">
              Comprehensive analytics across environment, economy, and demographics
            </p>
          </div>
        </div>
      </GlassCard>

      {/* Environmental Quality Section */}
      <div>
        <SectionHeader 
          icon="🌤️" 
          title="Environmental Quality" 
          description="Air quality index trends and historical data"
          delay={50}
        />
        <GlassCard className="p-6" delay={100}>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-semibold text-white">Air Quality Index (AQI) - 12 Month Trend</h3>
              <p className="text-white/50 text-sm">Lower values indicate better air quality. Good: 0-50 | Moderate: 51-100</p>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-emerald-400"></span>
                <span className="text-white/60 text-sm">AQI</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-indigo-400"></span>
                <span className="text-white/60 text-sm">Benchmark</span>
              </div>
            </div>
          </div>
          <AQITrendChart data={aqiData} showBenchmark />
        </GlassCard>
      </div>

      {/* Economic Indicators Section */}
      <div>
        <SectionHeader 
          icon="💼" 
          title="Economic Indicators" 
          description="Employment distribution and cost of living analysis"
          delay={50}
        />
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <GlassCard className="p-6 hover:bg-white/[0.05] transition-all duration-500" delay={100}>
            <div className="mb-4">
              <h3 className="text-lg font-semibold text-white">Employment by Sector</h3>
              <p className="text-white/50 text-sm">Current workforce distribution across major industries</p>
            </div>
            <JobSectorChart data={jobData} />
          </GlassCard>

          <GlassCard className="p-6 hover:bg-white/[0.05] transition-all duration-500" delay={150}>
            <div className="mb-4">
              <h3 className="text-lg font-semibold text-white">Cost of Living Index</h3>
              <p className="text-white/50 text-sm">Comparison to national average (100 = national average)</p>
            </div>
            <CostOfLivingChart data={costData} showNationalAverage />
          </GlassCard>
        </div>
      </div>

      {/* Demographics Section */}
      <div>
        <SectionHeader 
          icon="👥" 
          title="Demographics" 
          description="Population growth trends and projections"
          delay={50}
        />
        <GlassCard className="p-6" delay={100}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Population Growth (10-Year Trend)</h3>
            <p className="text-white/50 text-sm">Historical population in millions with year-over-year growth rates</p>
          </div>
          <PopulationChart data={populationData} />
        </GlassCard>
      </div>

      {/* Data Insights Footer */}
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
                <span className="text-white font-medium">Data Sources:</span> Charts display real-time analytics from 
                environmental monitoring APIs, labor statistics, census data, and economic indices.
              </p>
              <p>
                <span className="text-white font-medium">Update Frequency:</span> Environmental data: Daily | 
                Economic data: Monthly | Demographics: Quarterly
              </p>
              <p className="text-white/40 text-xs pt-2 border-t border-white/5">
                Note: Currently displaying mock data for demonstration. Production version will connect to live APIs.
              </p>
            </div>
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
