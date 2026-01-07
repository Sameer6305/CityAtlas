/**
 * City Overview Page (Default)
 * Route: /cities/[citySlug]
 * 
 * Premium glassmorphism dashboard design
 * Shows high-level city information and key metrics.
 */

'use client';

import { useState, useEffect, useRef } from 'react';
import { AreaChart, PieChart } from '@/components';

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

// Sample population growth data
const populationData = [
  { year: '2015', population: 7.8 },
  { year: '2016', population: 7.9 },
  { year: '2017', population: 8.0 },
  { year: '2018', population: 8.1 },
  { year: '2019', population: 8.2 },
  { year: '2020', population: 8.1 },
  { year: '2021', population: 8.2 },
  { year: '2022', population: 8.25 },
  { year: '2023', population: 8.3 },
];

// AQI Trend data
const aqiData = [
  { month: 'Jan', aqi: 85 },
  { month: 'Feb', aqi: 92 },
  { month: 'Mar', aqi: 78 },
  { month: 'Apr', aqi: 65 },
  { month: 'May', aqi: 72 },
  { month: 'Jun', aqi: 88 },
  { month: 'Jul', aqi: 95 },
  { month: 'Aug', aqi: 102 },
  { month: 'Sep', aqi: 85 },
  { month: 'Oct', aqi: 78 },
  { month: 'Nov', aqi: 82 },
  { month: 'Dec', aqi: 90 },
];

// Cost of living breakdown
const costOfLivingData = [
  { category: 'Housing', value: 45, color: '#ef4444' },
  { category: 'Food', value: 15, color: '#f59e0b' },
  { category: 'Transport', value: 12, color: '#10b981' },
  { category: 'Healthcare', value: 10, color: '#3b82f6' },
  { category: 'Education', value: 8, color: '#8b5cf6' },
  { category: 'Utilities', value: 5, color: '#ec4899' },
  { category: 'Other', value: 5, color: '#6b7280' },
];

// Jobs sector data
const jobsData = [
  { name: 'Tech', value: 28 },
  { name: 'Finance', value: 22 },
  { name: 'Healthcare', value: 18 },
  { name: 'Retail', value: 15 },
  { name: 'Education', value: 10 },
  { name: 'Other', value: 7 },
];

const jobsColors = ['#06b6d4', '#8b5cf6', '#10b981', '#f59e0b', '#3b82f6', '#6b7280'];

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

  useEffect(() => {
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className={`space-y-8 transition-all duration-[1200ms] ease-out ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'}`}>
      
      {/* Key Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <MetricDisplay
          label="Weather"
          value="32°C"
          trend="up"
          trendValue="Sunny"
          iconBg="from-amber-500/20 to-orange-500/20"
          delay={100}
          icon={
            <svg className="w-6 h-6 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Population"
          value="20.4M"
          trend="up"
          trendValue="+2.3%"
          iconBg="from-cyan-500/20 to-blue-500/20"
          delay={200}
          icon={
            <svg className="w-6 h-6 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          }
        />
        <MetricDisplay
          label="GDP per Capita"
          value="$85K"
          trend="up"
          trendValue="+4.1%"
          iconBg="from-emerald-500/20 to-teal-500/20"
          delay={300}
          icon={
            <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Air Quality"
          value="AQI 78"
          trend="down"
          trendValue="Moderate"
          iconBg="from-purple-500/20 to-pink-500/20"
          delay={400}
          icon={
            <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
            </svg>
          }
        />
      </div>

      {/* Main Dashboard Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Population Trend - Spans 2 columns */}
        <GlassCard className="lg:col-span-2 p-6" delay={100}>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-lg font-semibold text-white">Population Growth</h3>
              <p className="text-white/50 text-sm">Historical trend over 9 years</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-cyan-400 animate-pulse"></span>
              <span className="text-white/60 text-sm">Population (M)</span>
            </div>
          </div>
          <AreaChart
            data={populationData}
            xKey="year"
            areas={[
              { dataKey: 'population', name: 'Population (M)', color: '#06b6d4', fillOpacity: 0.3 },
            ]}
            height={260}
          />
        </GlassCard>

        {/* Music Culture Card */}
        <GlassCard className="p-6" delay={150}>
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-pink-500/20 to-purple-500/20 flex items-center justify-center">
              <svg className="w-5 h-5 text-pink-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
              </svg>
            </div>
            <div>
              <h3 className="text-lg font-semibold text-white">Music Culture</h3>
              <p className="text-white/50 text-sm">Top genres in the city</p>
            </div>
          </div>
          <div className="space-y-3">
            {[
              { genre: 'Bollywood', percentage: 45, color: 'from-pink-500 to-rose-500' },
              { genre: 'Hip-Hop', percentage: 25, color: 'from-purple-500 to-indigo-500' },
              { genre: 'Classical', percentage: 15, color: 'from-amber-500 to-orange-500' },
              { genre: 'EDM', percentage: 10, color: 'from-cyan-500 to-blue-500' },
              { genre: 'Rock', percentage: 5, color: 'from-emerald-500 to-teal-500' },
            ].map((item) => (
              <div key={item.genre} className="space-y-1">
                <div className="flex justify-between text-sm">
                  <span className="text-white/70">{item.genre}</span>
                  <span className="text-white font-medium">{item.percentage}%</span>
                </div>
                <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                  <div 
                    className={`h-full bg-gradient-to-r ${item.color} rounded-full transition-all duration-1000`}
                    style={{ width: `${item.percentage}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </GlassCard>

        {/* Jobs Distribution */}
        <GlassCard className="p-6" delay={50}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Jobs Distribution</h3>
            <p className="text-white/50 text-sm">Employment by sector</p>
          </div>
          <div className="h-[200px]">
            <PieChart
              data={jobsData}
              colors={jobsColors}
              height={200}
              innerRadius={50}
              showLabels={false}
            />
          </div>
          <div className="grid grid-cols-2 gap-2 mt-4">
            {jobsData.map((item, index) => (
              <div key={item.name} className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full" style={{ backgroundColor: jobsColors[index] }}></span>
                <span className="text-white/60 text-xs">{item.name}: {item.value}%</span>
              </div>
            ))}
          </div>
        </GlassCard>

        {/* Cost of Living */}
        <GlassCard className="p-6" delay={100}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Cost of Living</h3>
            <p className="text-white/50 text-sm">Monthly expense breakdown</p>
          </div>
          <div className="space-y-3">
            {costOfLivingData.map((item) => (
              <div key={item.category} className="flex items-center gap-3">
                <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }}></div>
                <div className="flex-1">
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-white/70">{item.category}</span>
                    <span className="text-white font-medium">{item.value}%</span>
                  </div>
                  <div className="h-1.5 bg-white/5 rounded-full overflow-hidden">
                    <div 
                      className="h-full rounded-full transition-all duration-1000"
                      style={{ width: `${item.value}%`, backgroundColor: item.color }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </GlassCard>

        {/* AQI Trend - Spans full width */}
        <GlassCard className="lg:col-span-3 p-6" delay={150}>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-lg font-semibold text-white">Air Quality Index Trend</h3>
              <p className="text-white/50 text-sm">Monthly AQI readings for the year</p>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-emerald-400"></span>
                <span className="text-white/60 text-sm">Good (0-50)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-amber-400"></span>
                <span className="text-white/60 text-sm">Moderate (51-100)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-red-400"></span>
                <span className="text-white/60 text-sm">Unhealthy (100+)</span>
              </div>
            </div>
          </div>
          <AreaChart
            data={aqiData}
            xKey="month"
            areas={[
              { dataKey: 'aqi', name: 'AQI', color: '#f59e0b', fillOpacity: 0.2 },
            ]}
            height={200}
          />
        </GlassCard>
      </div>

      {/* City Highlights & Challenges */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <GlassCard className="p-6" delay={50}>
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-500/20 to-teal-500/20 flex items-center justify-center">
              <svg className="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
              </svg>
            </div>
            City Highlights
          </h3>
          <ul className="space-y-4">
            {[
              { title: 'Global Financial Hub', desc: 'Major center for finance and business' },
              { title: 'Cultural Diversity', desc: 'Over 800 languages spoken' },
              { title: 'Innovation Leader', desc: 'Top 5 in global innovation index' },
            ].map((item) => (
              <li key={item.title} className="flex items-start gap-3">
                <span className="w-6 h-6 rounded-full bg-emerald-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg className="w-3.5 h-3.5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                </span>
                <div>
                  <p className="text-white font-medium">{item.title}</p>
                  <p className="text-white/50 text-sm">{item.desc}</p>
                </div>
              </li>
            ))}
          </ul>
        </GlassCard>

        <GlassCard className="p-6" delay={100}>
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-amber-500/20 to-orange-500/20 flex items-center justify-center">
              <svg className="w-4 h-4 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            Key Challenges
          </h3>
          <ul className="space-y-4">
            {[
              { title: 'Housing Affordability', desc: 'Rising costs impact residents' },
              { title: 'Transportation Congestion', desc: 'Infrastructure under pressure' },
              { title: 'Climate Adaptation', desc: 'Need for sustainable solutions' },
            ].map((item) => (
              <li key={item.title} className="flex items-start gap-3">
                <span className="w-6 h-6 rounded-full bg-amber-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg className="w-3.5 h-3.5 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                </span>
                <div>
                  <p className="text-white font-medium">{item.title}</p>
                  <p className="text-white/50 text-sm">{item.desc}</p>
                </div>
              </li>
            ))}
          </ul>
        </GlassCard>
      </div>
    </div>
  );
}

