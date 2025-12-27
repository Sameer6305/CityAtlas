/**
 * TopNav Component
 * 
 * Top navigation bar with city search and user actions
 * Sticky header for consistent navigation
 */

'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useTheme } from '@/store/useAppStore';

interface TopNavProps {
  title?: string;
  subtitle?: string;
  showSearch?: boolean;
}

export function TopNav({ title, subtitle, showSearch = true }: TopNavProps) {
  const router = useRouter();
  const { theme, toggleTheme } = useTheme();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Array<{ name: string; slug: string; country: string }>>([]);
  const [showResults, setShowResults] = useState(false);

  // Mock search results - replace with real API call
  const handleSearch = (query: string) => {
    setSearchQuery(query);
    
    if (query.length < 2) {
      setSearchResults([]);
      setShowResults(false);
      return;
    }

    // Mock data - replace with API call
    const mockCities = [
      { name: 'San Francisco', slug: 'san-francisco', country: 'USA' },
      { name: 'Austin', slug: 'austin', country: 'USA' },
      { name: 'Seattle', slug: 'seattle', country: 'USA' },
      { name: 'New York', slug: 'new-york', country: 'USA' },
      { name: 'Boston', slug: 'boston', country: 'USA' },
    ];

    const filtered = mockCities.filter(
      city => city.name.toLowerCase().includes(query.toLowerCase())
    );
    
    setSearchResults(filtered);
    setShowResults(true);
  };

  const handleSelectCity = (slug: string) => {
    router.push(`/cities/${slug}`);
    setSearchQuery('');
    setShowResults(false);
  };

  return (
    <header className="sticky top-0 z-sticky glass-nav">
      <div className="container mx-auto px-6 py-3">
        <div className="flex items-center justify-between gap-4">
          {/* Logo and Title Section */}
          <div className="flex-1 flex items-center gap-3">
            <button onClick={() => router.push('/')} className="hover:opacity-80 transition-fast flex items-center gap-3">
              <img src="/logo.png" alt="CityAtlas" className="h-12 w-auto rounded-lg" />
              <span className="text-xl font-bold text-text-primary hidden sm:block">CityAtlas</span>
            </button>
            {title && (
              <div className="ml-4">
                <h1 className="text-xl font-bold text-text-primary">
                  {title}
                </h1>
                {subtitle && (
                  <p className="text-xs text-text-tertiary mt-0.5">
                    {subtitle}
                  </p>
                )}
              </div>
            )}
          </div>

          {/* Search Bar */}
          {showSearch && (
            <div className="flex-1 max-w-md relative">
              <div className="relative">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => handleSearch(e.target.value)}
                  onFocus={() => searchQuery.length >= 2 && setShowResults(true)}
                  placeholder="Search cities..."
                  className="
                    w-full px-4 py-2 pl-10
                    glass-card rounded-lg
                    text-text-primary placeholder:text-text-tertiary
                    focus:outline-none focus:border-primary/50 focus:ring-2 focus:ring-primary/20
                    transition-fast
                  "
                />
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-tertiary">
                  🔍
                </span>
              </div>

              {/* Search Results Dropdown */}
              {showResults && searchResults.length > 0 && (
                <>
                  {/* Backdrop to close dropdown */}
                  <div 
                    className="fixed inset-0 z-40"
                    onClick={() => setShowResults(false)}
                  />
                  
                  {/* Results */}
                  <div className="absolute top-full mt-2 w-full bg-surface border border-surface-border rounded-md shadow-lg z-50">
                    <div className="py-2">
                      {searchResults.map((city) => (
                        <button
                          key={city.slug}
                          onClick={() => handleSelectCity(city.slug)}
                          className="
                            w-full px-4 py-2 text-left
                            hover:bg-surface-elevated
                            transition-colors
                          "
                        >
                          <div className="font-medium text-text-primary">
                            {city.name}
                          </div>
                          <div className="text-sm text-text-tertiary">
                            {city.country}
                          </div>
                        </button>
                      ))}
                    </div>
                  </div>
                </>
              )}

              {showResults && searchQuery.length >= 2 && searchResults.length === 0 && (
                <>
                  <div 
                    className="fixed inset-0 z-40"
                    onClick={() => setShowResults(false)}
                  />
                  <div className="absolute top-full mt-2 w-full bg-surface border border-surface-border rounded-md shadow-lg z-50 p-4">
                    <p className="text-text-tertiary text-sm">No cities found</p>
                  </div>
                </>
              )}
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center gap-3">
            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              className="
                p-2 rounded-md
                text-text-secondary hover:text-text-primary
                hover:bg-surface-elevated
                transition-all
              "
              title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
            >
              {theme === 'dark' ? '☀️' : '🌙'}
            </button>

            {/* Notifications */}
            <button
              className="
                p-2 rounded-md
                text-text-secondary hover:text-text-primary
                hover:bg-surface-elevated
                transition-all relative
              "
              title="Notifications"
            >
              🔔
              <span className="absolute top-1 right-1 w-2 h-2 bg-danger rounded-full" />
            </button>

            {/* User Menu */}
            <button
              className="
                w-8 h-8 rounded-full
                bg-primary text-white
                flex items-center justify-center
                font-medium text-sm
                hover:bg-primary-hover
                transition-all
              "
              title="User menu"
            >
              U
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
