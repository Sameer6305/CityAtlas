/**
 * Economy Section Page
 * Route: /cities/[citySlug]/economy
 * 
 * Premium glassmorphism design with smooth animations
 * Displays job market, industries, income distribution, startup ecosystem
 */

'use client';

import { useState, useEffect } from 'react';
import { PieChart } from '@/components';

// Sample data for job sector distribution
const jobSectorData = [
  { name: 'Technology', value: 28 },
  { name: 'Finance', value: 22 },
  { name: 'Healthcare', value: 18 },
  { name: 'Education', value: 12 },
  { name: 'Retail', value: 10 },
  { name: 'Other', value: 10 },
];

const sectorColors = ['#06b6d4', '#8b5cf6', '#10b981', '#f59e0b', '#3b82f6', '#6b7280'];

// Income distribution data
const incomeData = [
  { range: 'Under $30K', percentage: 12, color: '#ef4444' },
  { range: '$30K - $50K', percentage: 18, color: '#f59e0b' },
  { range: '$50K - $75K', percentage: 25, color: '#eab308' },
  { range: '$75K - $100K', percentage: 22, color: '#10b981' },
  { range: '$100K - $150K', percentage: 15, color: '#06b6d4' },
  { range: 'Over $150K', percentage: 8, color: '#8b5cf6' },
];

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
  return (
    <GlassCard className="p-5 hover:bg-white/[0.05] hover:scale-[1.02] transition-all duration-500 group" delay={delay}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-white/50 text-sm font-medium mb-1">{label}</p>
          <p className="text-3xl font-bold text-white">{value}</p>
          {trend && trendValue && (
            <div className={`flex items-center gap-1 mt-2 text-sm ${trend === 'up' ? 'text-emerald-400' : 'text-red-400'}`}>
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
    </GlassCard>
  );
}

export default function EconomyPage() {
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className={`space-y-6 transition-all duration-700 ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>
      
      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricDisplay
          label="Unemployment Rate"
          value="3.8%"
          trend="down"
          trendValue="-0.5% vs last quarter"
          iconBg="from-emerald-500/20 to-teal-500/20"
          delay={100}
          icon={
            <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Median Income"
          value="$92K"
          trend="up"
          trendValue="+5.2% YoY"
          iconBg="from-cyan-500/20 to-blue-500/20"
          delay={200}
          icon={
            <svg className="w-6 h-6 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Job Growth Rate"
          value="4.1%"
          trend="up"
          trendValue="+1.3% YoY"
          iconBg="from-purple-500/20 to-pink-500/20"
          delay={300}
          icon={
            <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
            </svg>
          }
        />
        <MetricDisplay
          label="Startup Density"
          value="12.5/10K"
          trend="up"
          trendValue="+8.7% YoY"
          iconBg="from-amber-500/20 to-orange-500/20"
          delay={400}
          icon={
            <svg className="w-6 h-6 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          }
        />
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <GlassCard className="p-6" delay={500}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Industry Distribution</h3>
            <p className="text-white/50 text-sm">Employment breakdown by sector</p>
          </div>
          <div className="h-[280px]">
            <PieChart
              data={jobSectorData}
              colors={sectorColors}
              height={280}
              innerRadius={60}
              showLabels={false}
            />
          </div>
        </GlassCard>

        <GlassCard className="p-6" delay={600}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Income Distribution</h3>
            <p className="text-white/50 text-sm">Household income levels</p>
          </div>
          <div className="space-y-3 mt-6">
            {incomeData.map((item, index) => (
              <div key={item.range} className="space-y-1">
                <div className="flex justify-between text-sm">
                  <span className="text-white/70">{item.range}</span>
                  <span className="text-white font-medium">{item.percentage}%</span>
                </div>
                <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                  <div 
                    className="h-full rounded-full transition-all duration-1000 ease-out"
                    style={{ 
                      width: `${item.percentage}%`, 
                      backgroundColor: item.color,
                      transitionDelay: `${index * 100}ms`
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </GlassCard>
      </div>

      {/* Top Industries */}
      <GlassCard className="p-6" delay={700}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500/20 to-indigo-500/20 flex items-center justify-center">
            <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
          </div>
          Top Industries
        </h3>
        <div className="space-y-4">
          {[
            { name: 'Technology', percentage: 32, employees: '125K', growth: 8.5, color: '#06b6d4' },
            { name: 'Finance', percentage: 24, employees: '95K', growth: 3.2, color: '#8b5cf6' },
            { name: 'Healthcare', percentage: 18, employees: '70K', growth: 5.8, color: '#10b981' },
            { name: 'Professional Services', percentage: 14, employees: '55K', growth: 4.1, color: '#f59e0b' },
            { name: 'Retail & Hospitality', percentage: 12, employees: '48K', growth: 2.3, color: '#3b82f6' },
          ].map((industry, index) => (
            <div key={industry.name} className="p-4 bg-white/[0.02] rounded-xl border border-white/5 hover:bg-white/[0.05] transition-all duration-300">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 rounded-full" style={{ backgroundColor: industry.color }}></div>
                  <span className="text-white font-medium">{industry.name}</span>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-white/60 text-sm">{industry.employees} employees</span>
                  <span className={`text-sm font-medium ${industry.growth > 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                    {industry.growth > 0 ? '↑' : '↓'} {Math.abs(industry.growth)}%
                  </span>
                </div>
              </div>
              <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                <div 
                  className="h-full rounded-full transition-all duration-1000 ease-out"
                  style={{ 
                    width: `${industry.percentage}%`, 
                    backgroundColor: industry.color,
                    transitionDelay: `${index * 150}ms`
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      </GlassCard>

      {/* Startup Ecosystem */}
      <GlassCard className="p-6" delay={800}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-500/20 flex items-center justify-center">
            <span className="text-xl">🚀</span>
          </div>
          Startup Ecosystem
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[
            { label: 'Active Startups', value: '2,450+', icon: '🏢' },
            { label: 'VC Funding (2023)', value: '$4.2B', icon: '💰' },
            { label: 'Unicorns', value: '12', icon: '🦄' },
          ].map((stat) => (
            <div key={stat.label} className="p-5 bg-white/[0.02] rounded-xl border border-white/5 text-center hover:bg-white/[0.05] transition-all duration-300">
              <div className="text-3xl mb-3">{stat.icon}</div>
              <div className="text-2xl font-bold text-white mb-1">{stat.value}</div>
              <div className="text-sm text-white/50">{stat.label}</div>
            </div>
          ))}
        </div>
      </GlassCard>
    </div>
  );
}
