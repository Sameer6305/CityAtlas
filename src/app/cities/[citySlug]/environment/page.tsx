/**
 * Environment Section Page
 * Route: /cities/[citySlug]/environment
 * 
 * Premium glassmorphism design with scroll-triggered animations
 * Displays air quality, green space, sustainability, climate risk
 */

'use client';

import { useState, useEffect, useRef } from 'react';
import { LineChart, AreaChart } from '@/components';

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

// Sample AQI data
const aqiData = [
  { month: 'Jan', aqi: 52, benchmark: 50 },
  { month: 'Feb', aqi: 48, benchmark: 50 },
  { month: 'Mar', aqi: 45, benchmark: 50 },
  { month: 'Apr', aqi: 41, benchmark: 50 },
  { month: 'May', aqi: 38, benchmark: 50 },
  { month: 'Jun', aqi: 42, benchmark: 50 },
  { month: 'Jul', aqi: 47, benchmark: 50 },
  { month: 'Aug', aqi: 44, benchmark: 50 },
  { month: 'Sep', aqi: 40, benchmark: 50 },
  { month: 'Oct', aqi: 43, benchmark: 50 },
  { month: 'Nov', aqi: 46, benchmark: 50 },
  { month: 'Dec', aqi: 45, benchmark: 50 },
];

// Energy mix data
const energyData = [
  { year: '2020', renewable: 25, fossil: 75 },
  { year: '2021', renewable: 28, fossil: 72 },
  { year: '2022', renewable: 32, fossil: 68 },
  { year: '2023', renewable: 35, fossil: 65 },
  { year: '2024', renewable: 42, fossil: 58 },
];

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
      className={`backdrop-blur-xl bg-white/[0.03] border border-white/[0.08] rounded-2xl transition-all duration-[800ms] ease-out hover:bg-white/[0.06] hover:border-white/[0.15] hover:shadow-2xl hover:shadow-cyan-500/10 hover:scale-[1.01] hover:-translate-y-1 ${isAnimated ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-12 scale-[0.97]'} ${className}`}
    >
      {children}
    </div>
  );
}

// Metric display component with scroll-triggered animation
function MetricDisplay({ 
  label, 
  value, 
  subtitle,
  icon, 
  trend, 
  trendValue,
  iconBg = 'from-cyan-500/20 to-blue-500/20',
  delay = 0
}: { 
  label: string; 
  value: string; 
  subtitle?: string;
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
          <p className="text-white/50 text-sm font-medium mb-1">{label}</p>
          <p className="text-3xl font-bold text-white">{value}</p>
          {subtitle && <p className="text-white/40 text-sm mt-1">{subtitle}</p>}
          {trend && trendValue && (
            <div className={`flex items-center gap-1 mt-2 text-sm ${trend === 'up' ? 'text-emerald-400' : trend === 'down' ? 'text-emerald-400' : 'text-red-400'}`}>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d={trend === 'up' ? "M5 10l7-7m0 0l7 7m-7-7v18" : "M19 14l-7 7m0 0l-7-7m7 7V3"} 
                />
              </svg>
              <span>{trendValue}</span>
            </div>
          )}
        </div>
        <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${iconBg} flex items-center justify-center group-hover:scale-110 transition-transform duration-500`}>
          {icon}
        </div>
      </div>
    </div>
  );
}

export default function EnvironmentPage() {
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className={`space-y-8 transition-all duration-[1200ms] ease-out ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'}`}>
      
      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricDisplay
          label="Air Quality Index"
          value="45"
          subtitle="Good"
          trend="down"
          trendValue="-8 vs last month"
          iconBg="from-emerald-500/20 to-teal-500/20"
          delay={100}
          icon={
            <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Green Space"
          value="28%"
          subtitle="2,800 hectares"
          trend="up"
          trendValue="+3.5% vs last year"
          iconBg="from-green-500/20 to-emerald-500/20"
          delay={200}
          icon={
            <svg className="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Recycling Rate"
          value="62%"
          trend="up"
          trendValue="+7.2% YoY"
          iconBg="from-cyan-500/20 to-blue-500/20"
          delay={300}
          icon={
            <svg className="w-6 h-6 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          }
        />
        <MetricDisplay
          label="Renewable Energy"
          value="35%"
          subtitle="Of total consumption"
          trend="up"
          trendValue="+12% YoY"
          iconBg="from-amber-500/20 to-orange-500/20"
          delay={400}
          icon={
            <svg className="w-6 h-6 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          }
        />
      </div>

      {/* Air Quality Chart */}
      <GlassCard className="p-6" delay={500}>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="text-lg font-semibold text-white">Air Quality Trends</h3>
            <p className="text-white/50 text-sm">AQI measurements over the past 12 months</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-emerald-400"></span>
              <span className="text-white/60 text-sm">AQI</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-indigo-400"></span>
              <span className="text-white/60 text-sm">Target</span>
            </div>
          </div>
        </div>
        <LineChart
          data={aqiData}
          xKey="month"
          lines={[
            { dataKey: 'aqi', name: 'AQI', color: '#10b981', strokeWidth: 3 },
            { dataKey: 'benchmark', name: 'Target (Good)', color: '#6366f1', strokeWidth: 2 },
          ]}
          height={280}
        />
      </GlassCard>

      {/* Sustainability Initiatives */}
      <GlassCard className="p-6" delay={600}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-green-500/20 to-emerald-500/20 flex items-center justify-center">
            <span className="text-xl">🌱</span>
          </div>
          Active Sustainability Programs
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[
            { title: 'Carbon Neutral 2030', progress: 68, description: 'City-wide emissions reduction initiative', color: '#10b981' },
            { title: 'Urban Greening Project', progress: 82, description: 'Expanding parks and green corridors', color: '#22c55e' },
            { title: 'Clean Energy Transition', progress: 45, description: 'Converting to renewable sources', color: '#f59e0b' },
            { title: 'Zero Waste Program', progress: 58, description: 'Municipal waste reduction target', color: '#06b6d4' },
          ].map((program, index) => (
            <div key={program.title} className="p-5 bg-white/[0.02] rounded-xl border border-white/5 hover:bg-white/[0.05] transition-all duration-300">
              <div className="flex items-center justify-between mb-2">
                <span className="text-white font-medium">{program.title}</span>
                <span className="text-white font-semibold" style={{ color: program.color }}>{program.progress}%</span>
              </div>
              <p className="text-sm text-white/50 mb-4">{program.description}</p>
              <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                <div 
                  className="h-full rounded-full transition-all duration-1000"
                  style={{ 
                    width: `${program.progress}%`, 
                    backgroundColor: program.color,
                    transitionDelay: `${index * 150}ms`
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      </GlassCard>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <GlassCard className="p-6" delay={700}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Energy Mix Transition</h3>
            <p className="text-white/50 text-sm">Renewable vs fossil fuel sources</p>
          </div>
          <AreaChart
            data={energyData}
            xKey="year"
            areas={[
              { dataKey: 'renewable', name: 'Renewable', color: '#10b981', fillOpacity: 0.4 },
              { dataKey: 'fossil', name: 'Fossil Fuels', color: '#6b7280', fillOpacity: 0.2 },
            ]}
            height={260}
          />
        </GlassCard>

        <GlassCard className="p-6" delay={800}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Green Initiatives</h3>
            <p className="text-white/50 text-sm">Environmental investments</p>
          </div>
          <div className="space-y-4 mt-6">
            {[
              { name: 'Electric Bus Fleet', invested: '$450M', units: '850 buses', progress: 72 },
              { name: 'Solar Panel Installations', invested: '$280M', units: '15K homes', progress: 58 },
              { name: 'EV Charging Network', invested: '$120M', units: '2.5K stations', progress: 45 },
              { name: 'Urban Forest Initiative', invested: '$85M', units: '50K trees', progress: 88 },
            ].map((item, index) => (
              <div key={item.name} className="p-3 bg-white/[0.02] rounded-lg border border-white/5">
                <div className="flex justify-between mb-2">
                  <span className="text-white/80">{item.name}</span>
                  <span className="text-emerald-400 text-sm">{item.invested}</span>
                </div>
                <div className="flex justify-between text-sm text-white/50 mb-2">
                  <span>{item.units}</span>
                  <span>{item.progress}% complete</span>
                </div>
                <div className="h-1.5 bg-white/5 rounded-full overflow-hidden">
                  <div 
                    className="h-full rounded-full bg-gradient-to-r from-emerald-400 to-green-400 transition-all duration-1000"
                    style={{ width: `${item.progress}%`, transitionDelay: `${index * 100}ms` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </GlassCard>
      </div>

      {/* Climate Risk Assessment */}
      <GlassCard className="p-6" delay={900}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500/20 to-red-500/20 flex items-center justify-center">
            <span className="text-xl">🌡️</span>
          </div>
          Climate Risk Assessment
        </h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { risk: 'Flooding', level: 'Moderate', score: 45, color: '#f59e0b' },
            { risk: 'Heat Waves', level: 'High', score: 72, color: '#ef4444' },
            { risk: 'Sea Level Rise', level: 'Low', score: 28, color: '#10b981' },
            { risk: 'Air Pollution', level: 'Moderate', score: 55, color: '#f59e0b' },
          ].map((item) => (
            <div key={item.risk} className="p-5 bg-white/[0.02] rounded-xl border border-white/5 text-center">
              <div className="text-white font-medium mb-3">{item.risk}</div>
              <div className="relative w-16 h-16 mx-auto mb-3">
                <svg className="w-full h-full transform -rotate-90">
                  <circle cx="32" cy="32" r="28" stroke="rgba(255,255,255,0.1)" strokeWidth="4" fill="none" />
                  <circle 
                    cx="32" cy="32" r="28" 
                    stroke={item.color}
                    strokeWidth="4" 
                    fill="none" 
                    strokeLinecap="round"
                    strokeDasharray={`${item.score * 1.76} 176`}
                  />
                </svg>
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-xl font-bold text-white">{item.score}</span>
                </div>
              </div>
              <div className="text-sm font-medium" style={{ color: item.color }}>{item.level}</div>
            </div>
          ))}
        </div>
      </GlassCard>
    </div>
  );
}
