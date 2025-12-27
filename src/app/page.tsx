'use client';

import Link from 'next/link';
import Image from 'next/image';
import { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useTheme } from '@/store/useAppStore';

export default function Home() {
  const router = useRouter();
  const { theme, toggleTheme } = useTheme();
  const [showContent, setShowContent] = useState(false);
  const [animationComplete, setAnimationComplete] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  // Animation sequence - 8 seconds for full video with smooth fade
  useEffect(() => {
    // Ensure video plays only once
    if (videoRef.current) {
      videoRef.current.play().catch(err => console.error('Video play error:', err));
    }
    
    const timer1 = setTimeout(() => setShowContent(true), 6000); // Start fade at 6s
    const timer2 = setTimeout(() => setAnimationComplete(true), 8000); // Complete at 8s
    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
    };
  }, []);

  const featuredCities = [
    { 
      name: 'San Francisco', 
      slug: 'san-francisco', 
      country: 'USA',
      tagline: 'Tech Innovation Hub',
      gradient: 'from-blue-500 to-purple-600',
      icon: '🌉'
    },
    { 
      name: 'Austin', 
      slug: 'austin', 
      country: 'USA',
      tagline: 'Keep It Weird',
      gradient: 'from-green-500 to-teal-600',
      icon: '🎸'
    },
    { 
      name: 'Seattle', 
      slug: 'seattle', 
      country: 'USA',
      tagline: 'Emerald City',
      gradient: 'from-cyan-500 to-blue-600',
      icon: '🌲'
    },
    { 
      name: 'New York', 
      slug: 'new-york', 
      country: 'USA',
      tagline: 'The Big Apple',
      gradient: 'from-yellow-500 to-orange-600',
      icon: '🗽'
    },
    { 
      name: 'Boston', 
      slug: 'boston', 
      country: 'USA',
      tagline: 'Historic Innovation',
      gradient: 'from-red-500 to-pink-600',
      icon: '⚾'
    },
  ];

  return (
    <div className="min-h-screen relative overflow-hidden bg-background">
      {/* Animated Background with Glass Effect */}
      <div className="fixed inset-0 -z-10">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-cyan-600/20 via-transparent to-transparent" />
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-cyan-500/20 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/20 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />
        <div className="absolute top-1/2 left-1/2 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
      </div>

      {/* Landing Animation with Video */}
      {!animationComplete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black">
          <video
            ref={videoRef}
            muted
            playsInline
            preload="auto"
            className="w-full h-full object-cover"
          >
            <source src="/video.mp4" type="video/mp4" />
          </video>
          {/* Fade Overlay for smooth transition */}
          <div 
            className="absolute inset-0 bg-black transition-opacity duration-2000 ease-in-out pointer-events-none"
            style={{ opacity: showContent ? 1 : 0 }}
          />
          {/* Skip Button */}
          <button
            onClick={() => {
              setShowContent(true);
              setAnimationComplete(true);
            }}
            className="absolute bottom-8 right-8 px-6 py-3 bg-white/10 hover:bg-white/20 backdrop-blur-md border border-white/30 text-white rounded-2xl font-medium transition-all duration-300 hover:scale-105 hover:shadow-lg hover:shadow-white/20"
          >
            Skip Intro →
          </button>
        </div>
      )}

      {/* Glass Navigation Bar */}
      <nav className="fixed top-0 left-0 right-0 z-40 transition-opacity duration-2000" style={{ opacity: animationComplete ? 1 : 0 }}>
        <div className="mx-4 mt-4 glass-nav rounded-2xl shadow-2xl">
          <div className="px-6 py-3 flex items-center justify-between">
            <button onClick={() => router.push('/')} className="flex items-center gap-3 hover:scale-105 transition-smooth">
              <Image src="/logo.png" alt="CityAtlas" width={56} height={56} className="rounded-xl" priority />
              <span className="text-2xl font-bold text-white">CityAtlas</span>
            </button>
            
            <div className="flex items-center gap-3">
              <button
                onClick={toggleTheme}
                className="p-2.5 rounded-xl glass-card hover:bg-white/20 transition-smooth"
              >
                <span className="text-xl">{theme === 'dark' ? '☀️' : '🌙'}</span>
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content - Fade In */}
      <div className={`transition-opacity duration-2000 ease-in-out ${animationComplete ? 'opacity-100' : 'opacity-0'}`}>
        {/* Hero Section with Glass Card */}
        <section className="min-h-screen flex flex-col items-center justify-center px-6 py-24">
          <div className="text-center max-w-5xl w-full">
            {/* Glass Hero Card */}
            <div className="glass-card rounded-[2.5rem] p-10 mb-6 hover:shadow-cyan-500/20 transition-smooth hover:scale-[1.01]">
              {/* Logo */}
              <div className="mb-6 animate-fade-in-up">
                <div className="inline-block p-5 bg-gradient-to-br from-cyan-500/20 to-purple-500/20 backdrop-blur-xl rounded-2xl mb-5 shadow-2xl border border-white/10">
                  <Image src="/logo.png" alt="CityAtlas Logo" width={72} height={72} className="rounded-xl" priority />
                </div>
                <h1 className="text-6xl md:text-7xl font-bold mb-4 bg-gradient-to-r from-cyan-400 via-blue-300 to-purple-400 bg-clip-text text-transparent animate-gradient">
                  CityAtlas
                </h1>
                <p className="text-2xl md:text-3xl text-white/90 leading-relaxed font-light mb-3">
                  Intelligence in Urban Flow
                </p>
                <p className="text-white/70 text-lg max-w-2xl mx-auto leading-relaxed">
                  Explore cities as structured resumes with real-time data and AI-powered insights
                </p>
              </div>

              {/* CTA Buttons */}
              <div className="flex flex-col sm:flex-row gap-3 justify-center animate-fade-in-up" style={{ animationDelay: '200ms' }}>
                <button
                  onClick={() => router.push('/cities')}
                  className="group px-8 py-4 bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600 text-white rounded-xl font-semibold text-base shadow-lg shadow-cyan-500/50 hover:shadow-2xl hover:shadow-cyan-500/60 transition-smooth hover:scale-105"
                >
                  <span className="flex items-center justify-center gap-2">
                    Explore Cities
                    <span className="group-hover:translate-x-1 transition-smooth">→</span>
                  </span>
                </button>
                <button
                  onClick={() => router.push('/cities/san-francisco')}
                  className="px-8 py-4 glass-card hover:bg-white/10 text-white rounded-xl font-semibold text-base transition-smooth hover:scale-105 hover:shadow-lg"
                >
                  See Demo
                </button>
              </div>
            </div>

            {/* Stats Glass Cards */}
            <div className="grid grid-cols-3 gap-4 max-w-3xl mx-auto animate-fade-in-up" style={{ animationDelay: '400ms' }}>
              {[
                { value: '50+', label: 'Cities', icon: '🏙️' },
                { value: '8', label: 'Data Categories', icon: '📊' },
                { value: 'AI', label: 'Powered Insights', icon: '🤖' },
              ].map((stat, i) => (
                <div 
                  key={i} 
                  className="glass-card rounded-2xl p-5 transition-smooth hover:scale-105 hover:shadow-lg hover:shadow-cyan-500/20 group"
                >
                  <div className="text-3xl mb-2 group-hover:scale-110 transition-smooth">{stat.icon}</div>
                  <div className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent mb-1">{stat.value}</div>
                  <div className="text-sm text-white/70">{stat.label}</div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Featured Cities Section */}
        <section className="py-16 px-6">
          <div className="max-w-7xl mx-auto">
            {/* Section Header */}
            <div className="text-center mb-10">
              <div className="inline-block px-4 py-2 glass-card text-cyan-300 rounded-full text-xs font-semibold mb-4 border border-cyan-400/30">
                🌍 Popular Destinations
              </div>
              <h2 className="text-4xl md:text-5xl font-bold text-white mb-3">
                Featured Cities
              </h2>
              <p className="text-white/70 text-lg max-w-2xl mx-auto">
                Start exploring data-driven city profiles
              </p>
            </div>

            {/* City Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {featuredCities.map((city, index) => (
                <Link
                  key={city.slug}
                  href={`/cities/${city.slug}`}
                  className="group relative overflow-hidden rounded-2xl glass-card transition-smooth hover:scale-105 hover:shadow-2xl hover:shadow-cyan-500/20"
                  style={{ animationDelay: `${index * 100}ms` }}
                >
                  {/* Gradient Overlay */}
                  <div className={`absolute inset-0 bg-gradient-to-br ${city.gradient} opacity-0 group-hover:opacity-20 transition-smooth`} />
                  
                  {/* Content */}
                  <div className="relative p-6">
                    <div className="text-5xl mb-4 group-hover:scale-110 transition-smooth">{city.icon}</div>
                    <h3 className="text-2xl font-bold text-white mb-2 group-hover:text-cyan-300 transition-smooth">
                      {city.name}
                    </h3>
                    <p className="text-white/50 text-xs mb-3 font-medium">{city.country}</p>
                    <p className="text-white/80 font-medium mb-4">{city.tagline}</p>
                    
                    {/* View Button */}
                    <div className="flex items-center gap-2 text-cyan-400 font-semibold text-sm opacity-0 group-hover:opacity-100 transition-smooth group-hover:translate-x-2">
                      View Profile
                      <span>→</span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </section>

        {/* Features Section */}
        <section className="py-16 px-6">
          <div className="max-w-7xl mx-auto">
            {/* Section Header */}
            <div className="text-center mb-10">
              <div className="inline-block px-4 py-2 glass-card text-purple-300 rounded-full text-xs font-semibold mb-4 border border-purple-400/30">
                ⚡ Platform Features
              </div>
              <h2 className="text-4xl md:text-5xl font-bold text-white mb-3">
                Why CityAtlas?
              </h2>
              <p className="text-white/70 text-lg max-w-2xl mx-auto">
                Data-driven insights for smarter decisions
              </p>
            </div>

            {/* Feature Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {[
                {
                  icon: '📊',
                  title: 'Real-Time Analytics',
                  description: 'Access live data on economy, infrastructure, education, and environmental metrics',
                  gradient: 'from-cyan-500/20 to-blue-500/20'
                },
                {
                  icon: '🤖',
                  title: 'AI-Powered Insights',
                  description: 'Get personalized recommendations and intelligent trend analysis',
                  gradient: 'from-purple-500/20 to-pink-500/20'
                },
                {
                  icon: '⚖️',
                  title: 'Compare Cities',
                  description: 'Side-by-side comparison of multiple cities for informed decisions',
                  gradient: 'from-green-500/20 to-teal-500/20'
                },
              ].map((feature, i) => (
                <div
                  key={i}
                  className={`group relative overflow-hidden rounded-2xl glass-card bg-gradient-to-br ${feature.gradient} p-8 transition-smooth hover:scale-105 hover:shadow-2xl hover:shadow-cyan-500/10`}
                >
                  <div className="text-5xl mb-4 group-hover:scale-110 transition-smooth">{feature.icon}</div>
                  <h3 className="text-xl font-bold text-white mb-3">
                    {feature.title}
                  </h3>
                  <p className="text-white/70 leading-relaxed">
                    {feature.description}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Footer */}
        <footer className="py-12 px-6 mt-16">
          <div className="max-w-7xl mx-auto">
            <div className="glass-card rounded-2xl p-8">
              <div className="text-center">
                <Image src="/logo.png" alt="CityAtlas" width={56} height={56} className="mx-auto rounded-xl mb-4" />
                <h3 className="text-xl font-bold text-white mb-1">CityAtlas</h3>
                <p className="text-white/60 text-sm mb-1">
                  City Intelligence Platform
                </p>
                <p className="text-white/40 text-xs">
                  Event-driven • Real-time data • AI-powered
                </p>
              </div>
            </div>
          </div>
        </footer>
      </div>
    </div>
  );
}


