'use client';

import { ReactNode, useEffect, useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useCityState } from '@/store/useAppStore';
import { CITY_SECTIONS } from '@/types/city';

/**
 * City Layout - Premium Dashboard Design
 * Matches the glassmorphism aesthetic of home and cities pages
 * Dynamic route: /cities/[citySlug]
 */

interface CityLayoutProps {
  children: ReactNode;
  params: {
    citySlug: string;
  };
}

// City data with quotes and gradient accents
const CITY_DATA: Record<string, { quote: string; gradient: string; country: string; bannerImage?: string }> = {
  'mumbai': {
    quote: 'A city that never sleeps.',
    gradient: 'from-orange-500 to-pink-500',
    country: 'India',
    bannerImage: '/cities/mumbai.jpg',
  },
  'bangalore': {
    quote: 'The Silicon Valley of India.',
    gradient: 'from-purple-500 to-indigo-500',
    country: 'India',
    bannerImage: '/cities/bangalore.jpg',
  },
  'new-york': {
    quote: 'The city so nice, they named it twice.',
    gradient: 'from-yellow-500 to-amber-500',
    country: 'USA',
    bannerImage: '/cities/new-york.jpg',
  },
  'san-francisco': {
    quote: 'Where innovation meets the Pacific.',
    gradient: 'from-cyan-500 to-blue-500',
    country: 'USA',
    bannerImage: '/cities/san-francisco.jpg',
  },
  'austin': {
    quote: 'Keep Austin Weird.',
    gradient: 'from-purple-500 to-pink-500',
    country: 'USA',
    bannerImage: '/cities/austin.jpg',
  },
  'seattle': {
    quote: 'The Emerald City of Innovation.',
    gradient: 'from-emerald-500 to-teal-500',
    country: 'USA',
    bannerImage: '/cities/seattle.jpg',
  },
  'boston': {
    quote: 'Where history meets innovation.',
    gradient: 'from-red-500 to-rose-500',
    country: 'USA',
    bannerImage: '/cities/boston.jpg',
  },
};

export default function CityLayout({ children, params }: CityLayoutProps) {
  const { citySlug } = params;
  const router = useRouter();
  const pathname = usePathname();
  const { selectedCity, setSelectedCity } = useCityState();
  const [isLoaded, setIsLoaded] = useState(false);
  const [imageError, setImageError] = useState(false);
  
  // Convert slug to display name
  const cityName = citySlug
    .split('-')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

  const cityInfo = CITY_DATA[citySlug] || {
    quote: 'Discover what makes this city unique.',
    gradient: 'from-cyan-500 to-blue-500',
    country: 'World',
  };

  // Get active section
  const getActiveSection = () => {
    if (pathname === `/cities/${citySlug}`) return 'overview';
    const segments = pathname.split('/');
    return segments[segments.length - 1];
  };
  const activeSection = getActiveSection();

  // Update selected city in store when route changes
  useEffect(() => {
    if (selectedCity?.slug !== citySlug) {
      setSelectedCity({
        id: citySlug,
        name: cityName,
        slug: citySlug,
        country: cityInfo.country,
        population: 0,
        gdpPerCapita: 0,
        coordinates: { lat: 0, lng: 0 },
        lastUpdated: new Date(),
      });
    }
  }, [citySlug, cityName, cityInfo.country, selectedCity, setSelectedCity]);

  // Animation on mount
  useEffect(() => {
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className={`relative min-h-screen overflow-hidden transition-opacity duration-700 ease-out ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>
      {/* Full-screen Background - Matching Home & Cities Pages */}
      <div className="fixed inset-0 -z-10">
        {/* City Skyline Background Image */}
        <Image
          src="/background.png"
          alt="City Skyline"
          fill
          className="object-cover"
          priority
          quality={100}
        />
        
        {/* Dark gradient overlay for readability */}
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0e27]/60 via-[#0f1420]/50 to-[#000000]/70" />
        
        {/* Subtle star particle overlay */}
        <div className="absolute inset-0 opacity-30 pointer-events-none">
          <div className="absolute top-[8%] left-[15%] w-1 h-1 bg-white rounded-full animate-pulse" style={{ animationDelay: '0s' }} />
          <div className="absolute top-[12%] left-[55%] w-1 h-1 bg-white rounded-full animate-pulse" style={{ animationDelay: '0.5s' }} />
          <div className="absolute top-[20%] left-[75%] w-1 h-1 bg-cyan-300 rounded-full animate-pulse" style={{ animationDelay: '1s' }} />
          <div className="absolute top-[35%] left-[10%] w-1 h-1 bg-white rounded-full animate-pulse" style={{ animationDelay: '1.5s' }} />
          <div className="absolute top-[45%] left-[90%] w-1 h-1 bg-blue-300 rounded-full animate-pulse" style={{ animationDelay: '2s' }} />
          <div className="absolute top-[65%] left-[35%] w-1 h-1 bg-white rounded-full animate-pulse" style={{ animationDelay: '2.5s' }} />
        </div>
        
        {/* Soft ambient glow - matching Home page */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[1000px] h-[1000px] bg-cyan-500/[0.03] rounded-full blur-3xl pointer-events-none" />
        <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-blue-500/[0.04] rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-cyan-500/[0.04] rounded-full blur-3xl pointer-events-none" />
      </div>

      <div className="flex min-h-screen">
        {/* Sidebar - Maximum Glassmorphism */}
        <aside className="fixed left-0 top-0 h-screen w-64 backdrop-blur-sm bg-transparent border-r border-white/5 flex flex-col z-50">
          {/* Logo */}
          <div className="p-6 border-b border-white/5">
            <button 
              onClick={() => router.push('/')}
              className="flex items-center gap-3 hover:opacity-80 hover:scale-105 transition-all duration-300 group"
            >
              <Image src="/logo.png" alt="CityAtlas" width={40} height={40} className="rounded-lg group-hover:rotate-6 transition-transform duration-300" />
              <span className="text-xl font-bold text-white group-hover:text-cyan-300 transition-colors duration-300">CityAtlas</span>
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
            <button 
              onClick={() => router.push('/')}
              className="w-full flex items-center gap-3 px-4 py-2.5 text-white/60 hover:text-white hover:bg-white/[0.06] rounded-xl transition-all duration-300 text-sm group"
            >
              <svg className="w-4 h-4 group-hover:text-cyan-400 transition-colors duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
              <span className="font-medium">Home</span>
            </button>

            <button 
              onClick={() => router.push('/cities')}
              className="w-full flex items-center gap-3 px-4 py-2.5 text-white/60 hover:text-white hover:bg-white/[0.06] rounded-xl transition-all duration-300 text-sm group"
            >
              <svg className="w-4 h-4 group-hover:text-cyan-400 transition-colors duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              <span className="font-medium">All Cities</span>
            </button>

            {/* City Sections Divider */}
            <div className="pt-6 pb-2">
              <p className="px-4 text-xs font-semibold text-white/40 uppercase tracking-wider">{cityName}</p>
            </div>

            {/* City Section Links */}
            {CITY_SECTIONS.map((section) => {
              const isActive = activeSection === section.slug;
              const href = section.slug === 'overview' 
                ? `/cities/${citySlug}`
                : `/cities/${citySlug}/${section.slug}`;

              return (
                <Link
                  key={section.id}
                  href={href}
                  className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-xl transition-all duration-300 text-sm group ${
                    isActive 
                      ? 'text-white bg-cyan-500/10 border border-cyan-500/20 shadow-lg shadow-cyan-500/10' 
                      : 'text-white/60 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  <div className={`w-1.5 h-1.5 rounded-full transition-all duration-300 ${isActive ? 'bg-cyan-400 shadow-lg shadow-cyan-400/50' : 'bg-white/30 group-hover:bg-cyan-400/60'}`} />
                  <span className="font-medium">{section.label}</span>
                </Link>
              );
            })}
          </nav>

          {/* Bottom Actions */}
          <div className="p-4 border-t border-white/5 space-y-1">
            <button className="w-full flex items-center gap-3 px-4 py-2.5 text-white/60 hover:text-white hover:bg-white/[0.06] rounded-xl transition-all duration-300 text-sm group">
              <svg className="w-4 h-4 group-hover:text-cyan-400 transition-colors duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
              </svg>
              <span className="font-medium">Bookmark City</span>
            </button>
            
            <button className="w-full flex items-center gap-3 px-4 py-2.5 text-white/60 hover:text-white hover:bg-white/[0.06] rounded-xl transition-all duration-300 text-sm group">
              <svg className="w-4 h-4 group-hover:text-cyan-400 transition-colors duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
              </svg>
              <span className="font-medium">Share City</span>
            </button>
          </div>
        </aside>

        {/* Main Content */}
        <div className="flex-1 ml-64">
          {/* Top Navigation Bar - Maximum Glassmorphism */}
          <div className="sticky top-0 z-40 backdrop-blur-sm bg-transparent border-b border-white/5">
            <div className="flex items-center justify-between px-8 py-4">
              {/* Breadcrumb */}
              <div className="flex items-center gap-2 text-sm">
                <Link href="/cities" className="text-white/50 hover:text-cyan-400 transition-colors duration-300">
                  Cities
                </Link>
                <svg className="w-4 h-4 text-white/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
                <span className="text-white font-medium">{cityName}</span>
              </div>

              {/* Right: Icons + User */}
              <div className="flex items-center gap-3">
                <button className="p-2.5 hover:bg-white/[0.06] rounded-xl transition-all duration-300 hover:scale-110 hover:-translate-y-0.5 group">
                  <svg className="w-5 h-5 text-white/60 group-hover:text-cyan-400 transition-colors duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </button>
                
                <button className="p-2.5 hover:bg-white/[0.06] rounded-xl transition-all duration-300 hover:scale-110 hover:-translate-y-0.5 group">
                  <svg className="w-5 h-5 text-white/60 group-hover:text-cyan-400 transition-colors duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                  </svg>
                </button>

                <div className="w-9 h-9 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-xl flex items-center justify-center text-white font-bold text-sm shadow-lg shadow-cyan-500/30 hover:scale-110 hover:shadow-cyan-500/50 transition-all duration-300 cursor-pointer">
                  U
                </div>
              </div>
            </div>
          </div>

          {/* City Banner Section - Enhanced */}
          <div className="relative h-80 overflow-hidden">
            {/* Banner Image or Gradient Fallback */}
            {cityInfo.bannerImage && !imageError ? (
              <Image
                src={cityInfo.bannerImage}
                alt={cityName}
                fill
                className="object-cover transition-transform duration-700 hover:scale-105"
                onError={() => setImageError(true)}
                priority
              />
            ) : (
              <div className={`absolute inset-0 bg-gradient-to-br ${cityInfo.gradient} opacity-40`} />
            )}
            
            {/* Dark Overlay with enhanced blur */}
            <div className="absolute inset-0 bg-gradient-to-t from-[#0a0e1a] via-[#0a0e1a]/70 to-[#0a0e1a]/30 backdrop-blur-[3px]" />
            
            {/* Subtle mesh gradient overlay */}
            <div className="absolute inset-0 opacity-60 pointer-events-none">
              <div className={`absolute top-0 right-0 w-[500px] h-[500px] bg-gradient-to-br ${cityInfo.gradient} opacity-15 blur-3xl rounded-full`} />
              <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-cyan-500/10 blur-3xl rounded-full" />
            </div>
            
            {/* Animated particles */}
            <div className="absolute inset-0 opacity-40 pointer-events-none">
              <div className="absolute top-[20%] left-[10%] w-1.5 h-1.5 bg-white/60 rounded-full animate-pulse" />
              <div className="absolute top-[40%] right-[15%] w-1 h-1 bg-cyan-300/60 rounded-full animate-pulse" style={{ animationDelay: '0.5s' }} />
              <div className="absolute bottom-[30%] left-[60%] w-1 h-1 bg-white/60 rounded-full animate-pulse" style={{ animationDelay: '1s' }} />
            </div>
            
            {/* City Info */}
            <div className="absolute bottom-0 left-0 right-0 p-8">
              <div className="flex items-end justify-between">
                <div className="space-y-4">
                  {/* Country Badge - Enhanced glassmorphism */}
                  <div className="inline-flex items-center gap-2 px-4 py-2 backdrop-blur-xl bg-white/[0.06] border border-white/[0.12] rounded-full shadow-lg shadow-black/20 hover:bg-white/[0.1] hover:border-white/20 transition-all duration-300">
                    <svg className="w-4 h-4 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span className="text-sm text-white/80 font-medium">{cityInfo.country}</span>
                  </div>
                  
                  <h1 className="text-5xl md:text-6xl font-bold text-white tracking-tight drop-shadow-2xl">
                    {cityName}
                  </h1>
                  <p className="text-lg text-white/70 italic max-w-xl backdrop-blur-sm">
                    &ldquo;{cityInfo.quote}&rdquo;
                  </p>
                </div>
                
                {/* City Initial Badge - Enhanced with glow */}
                <div className={`w-24 h-24 rounded-2xl bg-gradient-to-br ${cityInfo.gradient} flex items-center justify-center shadow-2xl shadow-cyan-500/30 backdrop-blur-xl border border-white/20 hover:scale-110 hover:shadow-cyan-500/50 transition-all duration-500`}>
                  <span className="text-5xl font-bold text-white drop-shadow-lg">{cityName.charAt(0)}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Tab Navigation - Enhanced Glassmorphism */}
          <div className="sticky top-[73px] z-30 backdrop-blur-xl bg-[#0a0e1a]/80 border-b border-white/[0.08]">
            <div className="px-8">
              <div className="flex gap-1 overflow-x-auto pb-px">
                {CITY_SECTIONS.map((section) => {
                  const isActive = activeSection === section.slug;
                  const href = section.slug === 'overview' 
                    ? `/cities/${citySlug}`
                    : `/cities/${citySlug}/${section.slug}`;

                  return (
                    <Link
                      key={section.id}
                      href={href}
                      className={`relative px-5 py-4 whitespace-nowrap font-medium text-sm transition-all duration-300 rounded-t-lg ${
                        isActive
                          ? 'text-cyan-400 bg-white/[0.03]'
                          : 'text-white/60 hover:text-white hover:bg-white/[0.05]'
                      }`}
                    >
                      {section.label}
                      {isActive && (
                        <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-cyan-400 to-blue-400 shadow-lg shadow-cyan-400/50" />
                      )}
                    </Link>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Main Content Area */}
          <main className="p-8">
            {children}
          </main>
        </div>
      </div>
    </div>
  );
}
