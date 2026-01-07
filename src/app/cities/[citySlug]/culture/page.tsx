/**
 * Culture Section Page
 * Route: /cities/[citySlug]/culture
 * 
 * Premium glassmorphism design with smooth animations
 * Displays arts scene, diversity, expat community, quality of life
 */

'use client';

import { useState, useEffect } from 'react';

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
  iconBg = 'from-cyan-500/20 to-blue-500/20',
  delay = 0
}: { 
  label: string; 
  value: string; 
  subtitle?: string;
  icon: string;
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
        </div>
        <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${iconBg} flex items-center justify-center group-hover:scale-110 transition-transform duration-500 text-2xl`}>
          {icon}
        </div>
      </div>
    </GlassCard>
  );
}

export default function CulturePage() {
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
          label="Museums & Galleries"
          value="125"
          subtitle="World-class collections"
          iconBg="from-pink-500/20 to-rose-500/20"
          delay={100}
          icon="🎨"
        />
        <MetricDisplay
          label="Theaters & Venues"
          value="78"
          subtitle="Including 15 major theaters"
          iconBg="from-purple-500/20 to-indigo-500/20"
          delay={200}
          icon="🎭"
        />
        <MetricDisplay
          label="Restaurants"
          value="12,500+"
          subtitle="65 Michelin-starred"
          iconBg="from-amber-500/20 to-orange-500/20"
          delay={300}
          icon="🍽️"
        />
        <MetricDisplay
          label="Languages Spoken"
          value="800+"
          subtitle="Most diverse globally"
          iconBg="from-cyan-500/20 to-blue-500/20"
          delay={400}
          icon="🌍"
        />
      </div>

      {/* Cultural Highlights */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <GlassCard className="p-6" delay={500}>
          <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-pink-500/20 to-rose-500/20 flex items-center justify-center">
              <span className="text-xl">🎪</span>
            </div>
            Major Annual Events
          </h3>
          <div className="space-y-3">
            {[
              { name: 'City Arts Festival', attendance: '2M visitors', period: 'June-July', color: 'from-pink-500/10 to-rose-500/10' },
              { name: 'International Food Fair', attendance: '500K visitors', period: 'September', color: 'from-amber-500/10 to-orange-500/10' },
              { name: 'Music & Culture Week', attendance: '350K visitors', period: 'March', color: 'from-purple-500/10 to-indigo-500/10' },
              { name: 'Film & Media Festival', attendance: '200K visitors', period: 'November', color: 'from-cyan-500/10 to-blue-500/10' },
            ].map((event) => (
              <div key={event.name} className={`p-4 bg-gradient-to-r ${event.color} rounded-xl border border-white/5 hover:border-white/20 transition-all duration-300`}>
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-white font-medium">{event.name}</div>
                    <div className="text-sm text-white/50">{event.period}</div>
                  </div>
                  <div className="text-sm text-white/70 bg-white/10 px-3 py-1 rounded-full">{event.attendance}</div>
                </div>
              </div>
            ))}
          </div>
        </GlassCard>

        <GlassCard className="p-6" delay={600}>
          <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500/20 to-indigo-500/20 flex items-center justify-center">
              <span className="text-xl">🏛️</span>
            </div>
            Notable Cultural Sites
          </h3>
          <div className="space-y-3">
            {[
              { name: 'City Museum of Art', visitors: '3.5M/year', rating: 4.8 },
              { name: 'Historic Opera House', visitors: '850K/year', rating: 4.9 },
              { name: 'Contemporary Art Center', visitors: '1.2M/year', rating: 4.7 },
              { name: 'Cultural Heritage District', visitors: '2M/year', rating: 4.8 },
            ].map((site) => (
              <div key={site.name} className="p-4 bg-white/[0.02] rounded-xl border border-white/5 hover:bg-white/[0.05] transition-all duration-300">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-white font-medium">{site.name}</div>
                    <div className="text-sm text-white/50">{site.visitors}</div>
                  </div>
                  <div className="flex items-center gap-1">
                    <span className="text-amber-400">★</span>
                    <span className="text-white font-medium">{site.rating}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </GlassCard>
      </div>

      {/* Diversity Index */}
      <GlassCard className="p-6" delay={700}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500/20 to-blue-500/20 flex items-center justify-center">
            <span className="text-xl">🌈</span>
          </div>
          Cultural Diversity Index
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[
            { label: 'Ethnic Diversity', score: 92, description: 'Among the most diverse cities globally' },
            { label: 'Religious Freedom', score: 95, description: 'Strong protections and representation' },
            { label: 'LGBTQ+ Inclusivity', score: 88, description: 'Leading in equality measures' },
          ].map((item, index) => (
            <div key={item.label} className="text-center">
              <div className="relative w-32 h-32 mx-auto mb-4">
                <svg className="w-full h-full transform -rotate-90">
                  <circle
                    cx="64"
                    cy="64"
                    r="56"
                    stroke="rgba(255,255,255,0.1)"
                    strokeWidth="8"
                    fill="none"
                  />
                  <circle
                    cx="64"
                    cy="64"
                    r="56"
                    stroke="url(#gradient)"
                    strokeWidth="8"
                    fill="none"
                    strokeLinecap="round"
                    strokeDasharray={`${item.score * 3.52} 352`}
                    className="transition-all duration-1000"
                    style={{ transitionDelay: `${index * 200}ms` }}
                  />
                  <defs>
                    <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
                      <stop offset="0%" stopColor="#06b6d4" />
                      <stop offset="100%" stopColor="#8b5cf6" />
                    </linearGradient>
                  </defs>
                </svg>
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-3xl font-bold text-white">{item.score}</span>
                </div>
              </div>
              <h4 className="text-white font-medium mb-1">{item.label}</h4>
              <p className="text-white/50 text-sm">{item.description}</p>
            </div>
          ))}
        </div>
      </GlassCard>

      {/* Food & Cuisine */}
      <GlassCard className="p-6" delay={800}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-500/20 flex items-center justify-center">
            <span className="text-xl">🍜</span>
          </div>
          Culinary Scene
        </h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { cuisine: 'Asian Fusion', count: '2,500+', icon: '🍜' },
            { cuisine: 'Italian', count: '1,800+', icon: '🍝' },
            { cuisine: 'Mexican', count: '1,200+', icon: '🌮' },
            { cuisine: 'Indian', count: '950+', icon: '🍛' },
            { cuisine: 'French', count: '680+', icon: '🥐' },
            { cuisine: 'Mediterranean', count: '820+', icon: '🫒' },
            { cuisine: 'Japanese', count: '1,100+', icon: '🍣' },
            { cuisine: 'American', count: '2,200+', icon: '🍔' },
          ].map((item) => (
            <div key={item.cuisine} className="p-4 bg-white/[0.02] rounded-xl border border-white/5 text-center hover:bg-white/[0.05] transition-all duration-300">
              <div className="text-3xl mb-2">{item.icon}</div>
              <div className="text-white font-medium">{item.cuisine}</div>
              <div className="text-sm text-white/50">{item.count} spots</div>
            </div>
          ))}
        </div>
      </GlassCard>

      {/* Nightlife & Entertainment */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[
          { label: 'Nightclubs', value: '450+', icon: '🎵', color: 'from-purple-500/20 to-pink-500/20' },
          { label: 'Live Music Venues', value: '320+', icon: '🎸', color: 'from-red-500/20 to-orange-500/20' },
          { label: 'Comedy Clubs', value: '85+', icon: '😂', color: 'from-amber-500/20 to-yellow-500/20' },
        ].map((item, index) => (
          <GlassCard key={item.label} className={`p-6 bg-gradient-to-br ${item.color}`} delay={900 + index * 100}>
            <div className="text-center">
              <div className="text-4xl mb-3">{item.icon}</div>
              <div className="text-3xl font-bold text-white mb-1">{item.value}</div>
              <div className="text-white/60">{item.label}</div>
            </div>
          </GlassCard>
        ))}
      </div>
    </div>
  );
}
