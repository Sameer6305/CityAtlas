/**
 * Infrastructure Section Page
 * Route: /cities/[citySlug]/infrastructure
 * 
 * Premium glassmorphism design with smooth animations
 * Displays transportation, utilities, internet, housing data
 */

'use client';

import { useState, useEffect } from 'react';
import { AreaChart } from '@/components';

// Commute time data
const commuteData = [
  { time: '6 AM', metro: 15, bus: 25, car: 20 },
  { time: '7 AM', metro: 22, bus: 35, car: 45 },
  { time: '8 AM', metro: 28, bus: 42, car: 55 },
  { time: '9 AM', metro: 25, bus: 38, car: 48 },
  { time: '10 AM', metro: 18, bus: 28, car: 30 },
  { time: '5 PM', metro: 26, bus: 40, car: 52 },
  { time: '6 PM', metro: 30, bus: 45, car: 58 },
  { time: '7 PM', metro: 22, bus: 32, car: 35 },
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

export default function InfrastructurePage() {
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
          label="Public Transit Score"
          value="8.5/10"
          trend="up"
          trendValue="+0.3 vs last year"
          iconBg="from-blue-500/20 to-indigo-500/20"
          delay={100}
          icon={
            <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
            </svg>
          }
        />
        <MetricDisplay
          label="Internet Speed"
          value="450 Mbps"
          trend="up"
          trendValue="+12.5% YoY"
          iconBg="from-purple-500/20 to-pink-500/20"
          delay={200}
          icon={
            <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.14 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0" />
            </svg>
          }
        />
        <MetricDisplay
          label="Power Reliability"
          value="99.8%"
          trend="up"
          trendValue="+0.1% vs last year"
          iconBg="from-amber-500/20 to-orange-500/20"
          delay={300}
          icon={
            <svg className="w-6 h-6 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          }
        />
        <MetricDisplay
          label="Water Quality"
          value="95/100"
          trend="up"
          trendValue="+2 vs last year"
          iconBg="from-cyan-500/20 to-blue-500/20"
          delay={400}
          icon={
            <svg className="w-6 h-6 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
            </svg>
          }
        />
      </div>

      {/* Transportation Network */}
      <GlassCard className="p-6" delay={500}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500/20 to-indigo-500/20 flex items-center justify-center">
            <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
            </svg>
          </div>
          Transportation Network
        </h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { label: 'Metro Lines', value: '12', icon: '🚇', color: 'from-blue-500/20 to-blue-600/20' },
            { label: 'Bus Routes', value: '145', icon: '🚌', color: 'from-green-500/20 to-green-600/20' },
            { label: 'Bike Stations', value: '320', icon: '🚴', color: 'from-orange-500/20 to-orange-600/20' },
            { label: 'EV Chargers', value: '850+', icon: '⚡', color: 'from-yellow-500/20 to-yellow-600/20' },
          ].map((item) => (
            <div key={item.label} className={`p-5 bg-gradient-to-br ${item.color} rounded-xl border border-white/5 text-center hover:scale-105 transition-all duration-300`}>
              <div className="text-3xl mb-2">{item.icon}</div>
              <div className="text-2xl font-bold text-white mb-1">{item.value}</div>
              <div className="text-sm text-white/60">{item.label}</div>
            </div>
          ))}
        </div>
      </GlassCard>

      {/* Commute Times Chart */}
      <GlassCard className="p-6" delay={600}>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="text-lg font-semibold text-white">Average Commute Times</h3>
            <p className="text-white/50 text-sm">Minutes by time of day and transport mode</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-blue-400"></span>
              <span className="text-white/60 text-sm">Metro</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-green-400"></span>
              <span className="text-white/60 text-sm">Bus</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-amber-400"></span>
              <span className="text-white/60 text-sm">Car</span>
            </div>
          </div>
        </div>
        <AreaChart
          data={commuteData}
          xKey="time"
          areas={[
            { dataKey: 'metro', name: 'Metro', color: '#3b82f6', fillOpacity: 0.3 },
            { dataKey: 'bus', name: 'Bus', color: '#10b981', fillOpacity: 0.3 },
            { dataKey: 'car', name: 'Car', color: '#f59e0b', fillOpacity: 0.3 },
          ]}
          height={260}
        />
      </GlassCard>

      {/* Utility Coverage */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <GlassCard className="p-6" delay={700}>
          <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-500/20 flex items-center justify-center">
              <span className="text-xl">⚡</span>
            </div>
            Power Grid Status
          </h3>
          <div className="space-y-4">
            {[
              { zone: 'Central District', coverage: 100, status: 'Excellent' },
              { zone: 'North Zone', coverage: 99.5, status: 'Excellent' },
              { zone: 'South Zone', coverage: 98.8, status: 'Good' },
              { zone: 'East Zone', coverage: 99.2, status: 'Excellent' },
              { zone: 'West Zone', coverage: 97.5, status: 'Good' },
            ].map((zone, index) => (
              <div key={zone.zone} className="p-3 bg-white/[0.02] rounded-lg border border-white/5">
                <div className="flex justify-between mb-2">
                  <span className="text-white/80">{zone.zone}</span>
                  <span className={`text-sm ${zone.coverage >= 99 ? 'text-emerald-400' : 'text-amber-400'}`}>
                    {zone.status}
                  </span>
                </div>
                <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                  <div 
                    className={`h-full rounded-full transition-all duration-1000 ${zone.coverage >= 99 ? 'bg-emerald-400' : 'bg-amber-400'}`}
                    style={{ width: `${zone.coverage}%`, transitionDelay: `${index * 100}ms` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </GlassCard>

        <GlassCard className="p-6" delay={800}>
          <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 flex items-center justify-center">
              <span className="text-xl">📡</span>
            </div>
            Digital Infrastructure
          </h3>
          <div className="space-y-4">
            {[
              { service: '5G Coverage', percentage: 85, target: 95 },
              { service: 'Fiber Optic', percentage: 72, target: 90 },
              { service: 'Public WiFi Hotspots', percentage: 68, target: 80 },
              { service: 'Smart Sensors', percentage: 45, target: 70 },
            ].map((item, index) => (
              <div key={item.service} className="p-3 bg-white/[0.02] rounded-lg border border-white/5">
                <div className="flex justify-between mb-2">
                  <span className="text-white/80">{item.service}</span>
                  <span className="text-white/60 text-sm">{item.percentage}% / {item.target}% target</span>
                </div>
                <div className="h-2 bg-white/5 rounded-full overflow-hidden relative">
                  <div 
                    className="h-full rounded-full bg-gradient-to-r from-purple-400 to-pink-400 transition-all duration-1000"
                    style={{ width: `${item.percentage}%`, transitionDelay: `${index * 100}ms` }}
                  />
                  <div 
                    className="absolute top-0 h-full w-0.5 bg-white/40"
                    style={{ left: `${item.target}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </GlassCard>
      </div>
    </div>
  );
}
