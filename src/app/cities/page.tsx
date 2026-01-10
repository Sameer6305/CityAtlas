/**
 * Cities Directory Page
 * Route: /cities
 * 
 * Shows list of all cities with search and filtering
 * Redesigned with custom sidebar and full-screen background
 */

'use client';

import Link from 'next/link';
import Image from 'next/image';
import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { TiltCard } from '@/components/TiltCard';

export default function CitiesPage() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    // Trigger animation on mount with delay
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, []);

  // TODO: Fetch from API
  const cities = [
    { 
      name: 'San Francisco', 
      slug: 'san-francisco', 
      country: 'USA', 
      population: 815201,
      description: 'Tech innovation hub with iconic landmarks',
      gradient: 'from-blue-500 to-cyan-500',
      accentColor: '#06b6d4'
    },
    { 
      name: 'Austin', 
      slug: 'austin', 
      country: 'USA', 
      population: 964254,
      description: 'Live music capital and startup ecosystem',
      gradient: 'from-purple-500 to-pink-500',
      accentColor: '#a855f7'
    },
    { 
      name: 'Seattle', 
      slug: 'seattle', 
      country: 'USA', 
      population: 749256,
      description: 'Emerald city with thriving tech scene',
      gradient: 'from-green-500 to-teal-500',
      accentColor: '#10b981'
    },
    { 
      name: 'New York', 
      slug: 'new-york', 
      country: 'USA', 
      population: 8336817,
      description: 'The city that never sleeps',
      gradient: 'from-yellow-500 to-orange-500',
      accentColor: '#f59e0b'
    },
    { 
      name: 'Boston', 
      slug: 'boston', 
      country: 'USA', 
      population: 675647,
      description: 'Historical hub of education and innovation',
      gradient: 'from-red-500 to-orange-500',
      accentColor: '#ef4444'
    },
  ];

  const filteredCities = cities.filter(city => 
    city.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    city.country.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className={`relative min-h-screen overflow-hidden transition-opacity duration-[1200ms] ease-out ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>
      {/* Full-screen Background with City Skyline */}
      <div className="fixed inset-0 -z-10">
        <Image
          src="/background.png"
          alt="City Skyline"
          fill
          className="object-cover"
          priority
          quality={100}
        />
        {/* Dark gradient overlay for readability */}
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0e27]/10 via-[#0f1420]/5 to-[#000000]/15" />
      </div>

      <div className="flex min-h-screen">
        {/* Custom Sidebar */}
        <aside className={`fixed left-0 top-0 h-screen w-64 backdrop-blur-sm bg-transparent border-r border-white/5 flex flex-col z-50 transition-transform duration-700 ease-out ${isLoaded ? 'translate-x-0' : '-translate-x-full'}`}>
          {/* Logo */}
          <div className="p-6 border-b border-white/5">
            <button 
              onClick={() => router.push('/')}
              className="flex items-center gap-3 hover:opacity-80 transition-opacity"
            >
              <Image src="/logo.png" alt="CityAtlas" width={40} height={40} className="rounded-lg" />
              <span className="text-xl font-bold text-white">CityAtlas</span>
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 p-4 space-y-2">
            <button 
              onClick={() => router.push('/')}
              className="w-full flex items-center gap-3 px-4 py-3 text-white/60 hover:text-white hover:bg-white/5 rounded-lg transition-all"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
              <span className="font-medium">Home</span>
            </button>

            <div className="w-full flex items-center gap-3 px-4 py-3 text-white bg-cyan-500/10 border border-cyan-500/20 rounded-lg">
              <svg className="w-5 h-5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              <span className="font-medium">All Cities</span>
            </div>

            <button 
              onClick={() => router.push('/compare')}
              className="w-full flex items-center gap-3 px-4 py-3 text-white/60 hover:text-white hover:bg-white/5 rounded-lg transition-all"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
              <span className="font-medium">Compare</span>
            </button>
          </nav>

          {/* Bottom Actions */}
          <div className="p-4 border-t border-white/[0.08] space-y-2">
            <button className="w-full flex items-center gap-3 px-4 py-3 text-white/60 hover:text-white hover:bg-white/5 rounded-lg transition-all">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <span className="font-medium">Settings</span>
            </button>

            <button className="w-full flex items-center gap-3 px-4 py-3 text-white/60 hover:text-white hover:bg-white/5 rounded-lg transition-all">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="font-medium">Help</span>
            </button>
          </div>
        </aside>

        {/* Main Content */}
        <div className={`flex-1 ml-64 transition-all duration-700 ease-out delay-200 ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'}`}>
          {/* Top Navigation Bar */}
          <div className="sticky top-0 z-40 backdrop-blur-sm bg-transparent border-b border-white/5">
            <div className="flex items-center justify-between px-8 py-4 rounded-b-3xl">
              {/* Left: Empty space for balance */}
              <div className="w-10"></div>

              {/* Center: Search Bar */}
              <div className="flex-1 max-w-2xl mx-8">
                <div className="relative">
                  <div className="absolute left-4 top-1/2 -translate-y-1/2 text-white/50">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                  </div>
                  <input
                    type="text"
                    placeholder="Search cities..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full bg-white/5 border border-white/5 text-white placeholder:text-white/50 pl-12 pr-4 py-2.5 rounded-lg outline-none focus:border-cyan-400/50 focus:bg-white/[0.08] transition-all"
                  />
                </div>
              </div>

              {/* Right: Icons + User */}
              <div className="flex items-center gap-3">
                <button className="p-2 hover:bg-white/5 rounded-lg transition-colors">
                  <svg className="w-5 h-5 text-white/70" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                  </svg>
                </button>

                <button className="p-2 hover:bg-white/5 rounded-lg transition-colors">
                  <svg className="w-5 h-5 text-white/70" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                  </svg>
                </button>

                <div className="w-9 h-9 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-full flex items-center justify-center text-white font-bold text-sm">
                  U
                </div>
              </div>
            </div>
          </div>

          {/* Page Content */}
          <div className="px-8 py-12">
            {/* Hero Section */}
            <div className="text-center mb-12">
              <div className="inline-block px-4 py-1.5 backdrop-blur-md bg-white/[0.05] text-white/80 rounded-full text-sm font-medium mb-6 border border-white/10">
                City Directory
              </div>
              <h1 className="text-5xl md:text-6xl font-bold text-white mb-4">
                Explore Cities
              </h1>
              <p className="text-lg md:text-xl text-white/70 max-w-3xl mx-auto">
                Dive deep into city data, analytics, and insights across major metropolitan areas
              </p>
            </div>

            {/* Large Search Bar */}
            <div className="max-w-3xl mx-auto mb-16">
              <div className="relative">
                <div className="absolute left-5 top-1/2 -translate-y-1/2 text-white/50">
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <input
                  type="text"
                  placeholder="Search cities by name or country..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full backdrop-blur-md bg-white/[0.05] border border-white/10 text-white placeholder:text-white/50 px-14 py-5 rounded-2xl outline-none focus:border-cyan-400/50 focus:bg-white/[0.08] transition-all duration-300 text-base"
                />
              </div>
            </div>

            {/* Find Your City Section */}
            <div className="text-center mb-10">
              <h2 className="text-2xl font-bold text-white mb-2">Find Your City</h2>
              <p className="text-white/60">Search from our database of world-class cities</p>
            </div>

            {/* Cities Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-12" style={{ perspective: '1000px' }}>
              {filteredCities.map((city) => (
                <TiltCard key={city.slug} maxTilt={12} scale={1.02}>
                  <Link
                    href={`/cities/${city.slug}`}
                    prefetch={true}
                    className="group block"
                  >
                    <div className="backdrop-blur-md bg-white/[0.03] border border-white/10 p-6 hover:bg-white/[0.06] hover:border-white/20 transition-all duration-300 rounded-2xl hover:shadow-2xl hover:shadow-cyan-500/20 relative overflow-hidden">
                      {/* Gradient Background on Hover */}
                      <div className={`absolute inset-0 bg-gradient-to-br ${city.gradient} opacity-0 group-hover:opacity-10 transition-opacity duration-500`} />
                      {/* Subtle corner glow */}
                      <div className="absolute -top-20 -right-20 w-40 h-40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-500" style={{ background: `radial-gradient(circle, ${city.accentColor}20 0%, transparent 70%)` }} />
                    
                    {/* Content */}
                    <div className="relative z-10">
                      <div className="flex items-start justify-between mb-5">
                        {/* City Initial Badge */}
                        <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${city.gradient} flex items-center justify-center shadow-lg group-hover:scale-110 group-hover:shadow-xl transition-all duration-500`}>
                          <span className="text-2xl font-bold text-white">{city.name.charAt(0)}</span>
                        </div>
                        <button className="p-2 rounded-lg hover:bg-white/10 transition-colors opacity-0 group-hover:opacity-100">
                          <svg className="w-5 h-5 text-white/60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                          </svg>
                        </button>
                      </div>
                      
                      <h3 className="text-xl font-bold text-white mb-1 group-hover:text-cyan-300 transition-colors duration-500">
                        {city.name}
                      </h3>
                      
                      <p className="text-sm text-white/50 mb-3">
                        {city.description}
                      </p>
                      
                      <div className="flex items-center justify-between">
                        <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-white/[0.05] rounded-lg text-sm">
                          <svg className="w-4 h-4 text-white/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                          </svg>
                          <span className="text-white/70">{city.country}</span>
                        </div>
                        <svg className="w-5 h-5 text-white/30 group-hover:text-cyan-400 group-hover:translate-x-1 transition-all duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                        </svg>
                      </div>
                    </div>
                  </div>
                  </Link>
                </TiltCard>
              ))}
            </div>

            {filteredCities.length === 0 && (
              <div className="text-center py-16 backdrop-blur-xl bg-white/[0.02] border border-white/10 rounded-2xl">
                <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-white/[0.05] flex items-center justify-center">
                  <svg className="w-8 h-8 text-white/40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <p className="text-white/50 text-lg">No cities found matching</p>
                <p className="text-white/70 font-medium">&ldquo;{searchQuery}&rdquo;</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
