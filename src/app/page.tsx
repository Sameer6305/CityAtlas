'use client';

import Link from 'next/link';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useTheme } from '@/store/useAppStore';

export default function Home() {
  const router = useRouter();
  const { theme, toggleTheme } = useTheme();
  const [showContent, setShowContent] = useState(false);
  const [animationComplete, setAnimationComplete] = useState(false);

  // Animation sequence
  useEffect(() => {
    const timer1 = setTimeout(() => setShowContent(true), 500);
    const timer2 = setTimeout(() => setAnimationComplete(true), 3500);
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
      gradient: 'from-blue-500 to-purple-600'
    },
    { 
      name: 'Austin', 
      slug: 'austin', 
      country: 'USA',
      tagline: 'Keep It Weird',
      gradient: 'from-green-500 to-teal-600'
    },
    { 
      name: 'Seattle', 
      slug: 'seattle', 
      country: 'USA',
      tagline: 'Emerald City',
      gradient: 'from-cyan-500 to-blue-600'
    },
    { 
      name: 'New York', 
      slug: 'new-york', 
      country: 'USA',
      tagline: 'The Big Apple',
      gradient: 'from-yellow-500 to-orange-600'
    },
    { 
      name: 'Boston', 
      slug: 'boston', 
      country: 'USA',
      tagline: 'Historic Innovation',
      gradient: 'from-red-500 to-pink-600'
    },
  ];

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Animated Background */}
      <div className="fixed inset-0 -z-10">
        <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-background to-accent/5" />
        <div className="absolute top-20 left-20 w-96 h-96 bg-primary/10 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-20 right-20 w-96 h-96 bg-accent/10 rounded-full blur-3xl animate-pulse delay-1000" />
      </div>

      {/* Landing Animation */}
      {!animationComplete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-background">
          <div className="text-center">
            <div className="relative">
              {/* Airplane Animation */}
              <div className="text-8xl mb-8 animate-plane">
                ✈️
              </div>
              {/* Globe */}
              <div className="text-9xl animate-spin-slow opacity-50">
                🌍
              </div>
            </div>
            <p className="text-text-secondary mt-8 animate-pulse">
              Landing in your city...
            </p>
          </div>
        </div>
      )}

      {/* Theme Toggle - Floating */}
      <button
        onClick={toggleTheme}
        className="fixed top-6 right-6 z-40 p-3 rounded-full bg-surface border border-surface-border shadow-lg hover:scale-110 transition-all"
      >
        {theme === 'dark' ? '☀️' : '🌙'}
      </button>

      {/* Main Content */}
      <div className={`transition-opacity duration-1000 ${showContent ? 'opacity-100' : 'opacity-0'}`}>
        {/* Hero Section */}
        <section className="min-h-screen flex flex-col items-center justify-center px-6 py-20">
          <div className="text-center max-w-4xl">
            {/* Logo & Title */}
            <div className="mb-8 animate-fade-in-up">
              <div className="inline-block p-4 bg-gradient-to-br from-primary to-accent rounded-2xl mb-6 shadow-glow-primary">
                <span className="text-6xl">🏙️</span>
              </div>
              <h1 className="text-6xl md:text-7xl font-bold mb-4 bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent animate-gradient">
                CityAtlas
              </h1>
              <p className="text-2xl md:text-3xl text-text-secondary leading-relaxed font-light">
                City Intelligence Platform
              </p>
              <p className="text-text-tertiary mt-4 text-lg">
                Explore cities as structured resumes with real-time data and AI insights
              </p>
            </div>

            {/* CTA Buttons */}
            <div className="flex flex-col sm:flex-row gap-4 justify-center my-12 animate-fade-in-up delay-200">
              <button
                onClick={() => router.push('/cities')}
                className="px-8 py-4 bg-primary hover:bg-primary-hover text-white rounded-lg font-semibold text-lg shadow-lg hover:shadow-glow-primary transition-all hover:scale-105"
              >
                Explore Cities →
              </button>
              <button
                onClick={() => router.push('/cities/san-francisco')}
                className="px-8 py-4 bg-surface hover:bg-surface-elevated border border-surface-border text-text-primary rounded-lg font-semibold text-lg transition-all hover:scale-105"
              >
                See Demo
              </button>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-3 gap-8 max-w-2xl mx-auto my-16 animate-fade-in-up delay-300">
              {[
                { value: '50+', label: 'Cities' },
                { value: '8', label: 'Data Categories' },
                { value: 'AI', label: 'Powered Insights' },
              ].map((stat, i) => (
                <div key={i} className="text-center">
                  <div className="text-3xl font-bold text-primary mb-2">{stat.value}</div>
                  <div className="text-sm text-text-tertiary">{stat.label}</div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Featured Cities Section */}
        <section className="py-24 px-6 bg-gradient-to-b from-surface/30 to-background">
          <div className="max-w-7xl mx-auto">
            <div className="text-center mb-16">
              <div className="inline-block px-4 py-2 bg-primary/10 text-primary rounded-full text-sm font-medium mb-4">
                Popular Destinations
              </div>
              <h2 className="text-4xl md:text-5xl font-bold text-text-primary mb-4">
                Featured Cities
              </h2>
              <p className="text-text-secondary text-xl max-w-2xl mx-auto">
                Start exploring data-driven city profiles
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
              {featuredCities.map((city, index) => (
                <Link
                  key={city.slug}
                  href={`/cities/${city.slug}`}
                  className="group relative overflow-hidden rounded-xl bg-surface border border-surface-border hover:border-primary transition-all hover:scale-105 hover:shadow-xl"
                  style={{ animationDelay: `${index * 100}ms` }}
                >
                  {/* Gradient Background */}
                  <div className={`absolute inset-0 bg-gradient-to-br ${city.gradient} opacity-10 group-hover:opacity-20 transition-opacity`} />
                  
                  {/* Content */}
                  <div className="relative p-6">
                    <div className="text-4xl mb-4">🏙️</div>
                    <h3 className="text-2xl font-bold text-text-primary mb-2 group-hover:text-primary transition-colors">
                      {city.name}
                    </h3>
                    <p className="text-text-tertiary text-sm mb-4">{city.country}</p>
                    <p className="text-text-secondary font-medium">{city.tagline}</p>
                    
                    {/* Arrow */}
                    <div className="mt-4 text-primary opacity-0 group-hover:opacity-100 transition-opacity">
                      View Profile →
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </section>

        {/* Features Section */}
        <section className="py-24 px-6 border-t border-surface-border">
          <div className="max-w-7xl mx-auto">
            <div className="text-center mb-16">
              <div className="inline-block px-4 py-2 bg-accent/10 text-accent rounded-full text-sm font-medium mb-4">
                Platform Features
              </div>
              <h2 className="text-4xl md:text-5xl font-bold text-text-primary mb-4">
                Why CityAtlas?
              </h2>
              <p className="text-text-secondary text-xl max-w-2xl mx-auto">
                Data-driven insights for smarter decisions
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {[
                {
                  icon: '📊',
                  title: 'Real-Time Analytics',
                  description: 'Access live data on economy, infrastructure, education, and more',
                },
                {
                  icon: '🤖',
                  title: 'AI-Powered Insights',
                  description: 'Get personalized recommendations and trend analysis',
                },
                {
                  icon: '⚖️',
                  title: 'Compare Cities',
                  description: 'Side-by-side comparison of multiple cities for informed decisions',
                },
              ].map((feature, i) => (
                <div
                  key={i}
                  className="card p-8 text-center hover:scale-105 transition-transform"
                >
                  <div className="text-5xl mb-4">{feature.icon}</div>
                  <h3 className="text-xl font-semibold text-text-primary mb-3">
                    {feature.title}
                  </h3>
                  <p className="text-text-secondary">
                    {feature.description}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Footer */}
        <footer className="py-12 px-6 border-t border-surface-border">
          <div className="max-w-7xl mx-auto text-center">
            <div className="text-2xl mb-4">🏙️</div>
            <p className="text-text-tertiary text-sm mb-2">
              CityAtlas - City Intelligence Platform
            </p>
            <p className="text-text-tertiary text-xs">
              Event-driven • Real-time data • AI-powered
            </p>
          </div>
        </footer>
      </div>
    </div>
  );
}


