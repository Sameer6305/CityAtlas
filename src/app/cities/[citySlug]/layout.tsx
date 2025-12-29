'use client';

import { ReactNode, useEffect } from 'react';
import { AppShell, TopNav, TabNavigation } from '@/components';
import { useCityState } from '@/store/useAppStore';

/**
 * City Layout
 * Wraps all city pages with common navigation and structure
 * Dynamic route: /cities/[citySlug]
 */

interface CityLayoutProps {
  children: ReactNode;
  params: {
    citySlug: string;
  };
}

export default function CityLayout({ children, params }: CityLayoutProps) {
  const { citySlug } = params;
  const { selectedCity, setSelectedCity } = useCityState();
  
  // Convert slug to display name
  const cityName = citySlug
    .split('-')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

  // Update selected city in store when route changes
  useEffect(() => {
    if (selectedCity?.slug !== citySlug) {
      setSelectedCity({
        id: citySlug,
        name: cityName,
        slug: citySlug,
        country: 'USA',
        population: 0,
        gdpPerCapita: 0,
        coordinates: { lat: 0, lng: 0 },
        lastUpdated: new Date(),
      });
    }
  }, [citySlug, cityName, selectedCity, setSelectedCity]);

  return (
    <AppShell>
      <TopNav />
      
      {/* City Hero Section with Glassmorphism */}
      <div className="glass-card border-b border-white/10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="text-xs text-text-tertiary mb-1.5 uppercase tracking-wider font-semibold">
            City Profile
          </div>
          <h1 className="text-2xl md:text-3xl font-bold text-text-primary">{cityName}</h1>
          <p className="mt-1.5 text-text-secondary text-sm">
            Comprehensive city intelligence and analytics
          </p>
        </div>
      </div>

      {/* Tab Navigation with Glassmorphism */}
      <div className="sticky top-16 z-10 glass-nav border-b border-white/5">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <TabNavigation citySlug={citySlug} />
        </div>
      </div>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {children}
      </main>
    </AppShell>
  );
}
