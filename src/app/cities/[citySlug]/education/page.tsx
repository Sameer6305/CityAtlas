/**
 * Education Section Page
 * Route: /cities/[citySlug]/education
 * 
 * Premium glassmorphism design with smooth animations
 * Displays universities, literacy rates, STEM pipeline, talent retention
 */

'use client';

import { useState, useEffect } from 'react';
import { AreaChart } from '@/components';

// Enrollment trend data
const enrollmentData = [
  { year: '2018', students: 245 },
  { year: '2019', students: 258 },
  { year: '2020', students: 242 },
  { year: '2021', students: 265 },
  { year: '2022', students: 278 },
  { year: '2023', students: 295 },
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
  return (
    <GlassCard className="p-5 hover:bg-white/[0.05] hover:scale-[1.02] transition-all duration-500 group" delay={delay}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-white/50 text-sm font-medium mb-1">{label}</p>
          <p className="text-3xl font-bold text-white">{value}</p>
          {subtitle && <p className="text-white/40 text-sm mt-1">{subtitle}</p>}
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

export default function EducationPage() {
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
          label="Literacy Rate"
          value="98.5%"
          trend="up"
          trendValue="+0.8% vs last year"
          iconBg="from-blue-500/20 to-indigo-500/20"
          delay={100}
          icon={
            <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          }
        />
        <MetricDisplay
          label="Universities"
          value="45"
          subtitle="12 top-ranked globally"
          iconBg="from-purple-500/20 to-pink-500/20"
          delay={200}
          icon={
            <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
          }
        />
        <MetricDisplay
          label="Student-Teacher Ratio"
          value="14:1"
          trend="down"
          trendValue="-1 vs last year"
          iconBg="from-emerald-500/20 to-teal-500/20"
          delay={300}
          icon={
            <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Research Output"
          value="8,500"
          subtitle="Publications per year"
          trend="up"
          trendValue="+12.3% YoY"
          iconBg="from-amber-500/20 to-orange-500/20"
          delay={400}
          icon={
            <svg className="w-6 h-6 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
            </svg>
          }
        />
      </div>

      {/* Top Universities */}
      <GlassCard className="p-6" delay={500}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 flex items-center justify-center">
            <svg className="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
          </div>
          Top Universities
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[
            { name: 'City Technical University', ranking: 'Top 50 Global', students: '45K', research: 'High', color: 'from-blue-500/10 to-indigo-500/10' },
            { name: 'Metropolitan State University', ranking: 'Top 100 Global', students: '38K', research: 'High', color: 'from-purple-500/10 to-pink-500/10' },
            { name: 'Downtown Business School', ranking: 'Top 20 Business', students: '12K', research: 'Medium', color: 'from-amber-500/10 to-orange-500/10' },
            { name: 'City Medical College', ranking: 'Top 30 Medical', students: '8K', research: 'High', color: 'from-emerald-500/10 to-teal-500/10' },
          ].map((uni, index) => (
            <div key={uni.name} className={`p-5 bg-gradient-to-br ${uni.color} rounded-xl border border-white/5 hover:border-white/20 transition-all duration-300 hover:scale-[1.02]`}>
              <div className="flex items-start justify-between mb-3">
                <div>
                  <div className="text-white font-semibold mb-1">{uni.name}</div>
                  <div className="text-sm text-cyan-400">{uni.ranking}</div>
                </div>
                <div className="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center text-white font-bold">
                  #{index + 1}
                </div>
              </div>
              <div className="flex items-center gap-4 text-sm">
                <div className="text-white/60">
                  <span className="text-white font-medium">{uni.students}</span> students
                </div>
                <div className="text-white/60">
                  Research: <span className={`font-medium ${uni.research === 'High' ? 'text-emerald-400' : 'text-amber-400'}`}>{uni.research}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </GlassCard>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <GlassCard className="p-6" delay={600}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Enrollment Trends</h3>
            <p className="text-white/50 text-sm">Student enrollment over time (thousands)</p>
          </div>
          <AreaChart
            data={enrollmentData}
            xKey="year"
            areas={[
              { dataKey: 'students', name: 'Students (K)', color: '#8b5cf6', fillOpacity: 0.3 },
            ]}
            height={260}
          />
        </GlassCard>

        <GlassCard className="p-6" delay={700}>
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Education Funding</h3>
            <p className="text-white/50 text-sm">Budget allocation by category</p>
          </div>
          <div className="space-y-4 mt-6">
            {[
              { category: 'Higher Education', amount: '$2.8B', percentage: 35, color: '#8b5cf6' },
              { category: 'K-12 Schools', amount: '$2.2B', percentage: 28, color: '#06b6d4' },
              { category: 'Research Grants', amount: '$1.5B', percentage: 19, color: '#10b981' },
              { category: 'Vocational Training', amount: '$800M', percentage: 10, color: '#f59e0b' },
              { category: 'Special Programs', amount: '$640M', percentage: 8, color: '#ec4899' },
            ].map((item, index) => (
              <div key={item.category} className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-white/70">{item.category}</span>
                  <span className="text-white font-medium">{item.amount}</span>
                </div>
                <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                  <div 
                    className="h-full rounded-full transition-all duration-1000"
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

      {/* STEM Pipeline */}
      <GlassCard className="p-6" delay={800}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500/20 to-blue-500/20 flex items-center justify-center">
            <span className="text-xl">🔬</span>
          </div>
          STEM Pipeline
        </h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { label: 'STEM Graduates', value: '28K', change: '+15%', icon: '🎓' },
            { label: 'Tech Internships', value: '12.5K', change: '+22%', icon: '💻' },
            { label: 'Research Labs', value: '340+', change: '+8%', icon: '🧪' },
            { label: 'Patent Filings', value: '2,800', change: '+18%', icon: '📄' },
          ].map((stat) => (
            <div key={stat.label} className="p-5 bg-white/[0.02] rounded-xl border border-white/5 text-center hover:bg-white/[0.05] transition-all duration-300">
              <div className="text-3xl mb-3">{stat.icon}</div>
              <div className="text-2xl font-bold text-white mb-1">{stat.value}</div>
              <div className="text-sm text-white/50 mb-2">{stat.label}</div>
              <div className="text-xs text-emerald-400">{stat.change} YoY</div>
            </div>
          ))}
        </div>
      </GlassCard>
    </div>
  );
}
