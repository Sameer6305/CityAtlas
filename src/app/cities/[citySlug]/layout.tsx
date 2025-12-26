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
      
      {/* City Hero Section */}
      <div className="bg-gradient-to-r from-primary/20 to-accent/20 border-b border-surface-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-sm text-text-tertiary mb-2 uppercase tracking-wide">
            City Profile
          </div>
          <h1 className="text-3xl font-bold text-text-primary">{cityName}</h1>
          <p className="mt-2 text-text-secondary">
            Comprehensive city intelligence and analytics
          </p>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="sticky top-16 z-10 bg-surface/95 backdrop-blur-sm border-b border-surface-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <TabNavigation citySlug={citySlug} />
        </div>
      </div>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>
    </AppShell>
  );
}
