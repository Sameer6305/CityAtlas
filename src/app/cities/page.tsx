/**
 * Cities Directory Page
 * Route: /cities
 * 
 * Shows list of all cities with search and filtering
 */

'use client';

import Link from 'next/link';
import { useState } from 'react';
import { AppShell, TopNav } from '@/components';

export default function CitiesPage() {
  const [searchQuery, setSearchQuery] = useState('');

  // TODO: Fetch from API
  const cities = [
    { 
      name: 'San Francisco', 
      slug: 'san-francisco', 
      country: 'USA', 
      population: 815201,
      description: 'Tech innovation hub with iconic landmarks',
      gradient: 'from-blue-500 to-cyan-500',
      emoji: '🌉'
    },
    { 
      name: 'Austin', 
      slug: 'austin', 
      country: 'USA', 
      population: 964254,
      description: 'Live music capital and startup ecosystem',
      gradient: 'from-purple-500 to-pink-500',
      emoji: '🎸'
    },
    { 
      name: 'Seattle', 
      slug: 'seattle', 
      country: 'USA', 
      population: 749256,
      description: 'Emerald city with thriving tech scene',
      gradient: 'from-green-500 to-teal-500',
      emoji: '🌲'
    },
    { 
      name: 'New York', 
      slug: 'new-york', 
      country: 'USA', 
      population: 8336817,
      description: 'The city that never sleeps',
      gradient: 'from-yellow-500 to-orange-500',
      emoji: '🗽'
    },
    { 
      name: 'Boston', 
      slug: 'boston', 
      country: 'USA', 
      population: 675647,
      description: 'Historical hub of education and innovation',
      gradient: 'from-red-500 to-orange-500',
      emoji: '🎓'
    },
  ];

  const filteredCities = cities.filter(city => 
    city.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    city.country.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <AppShell>
      <TopNav />
      
      {/* Hero Section */}
      <div className="glass-card border-b border-white/10 mx-4 mt-4 rounded-2xl">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
          <div className="text-center">
            <div className="inline-block px-4 py-2 glass-card text-cyan-300 rounded-full text-xs font-medium mb-5 border border-cyan-400/30 animate-fade-in-up">
              City Directory
            </div>
            <h1 className="text-4xl md:text-5xl font-bold text-white mb-4 animate-fade-in-up delay-100">
              Explore Cities
            </h1>
            <p className="text-lg md:text-xl text-white/70 max-w-3xl mx-auto animate-fade-in-up delay-200">
              Dive deep into city data, analytics, and insights across major metropolitan areas
            </p>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">{/* Search Bar */}
        <div className="mb-12">
          <div className="text-center mb-6">
            <h2 className="text-xl font-bold text-white mb-2">Find Your City</h2>
            <p className="text-white/70">Search from our database of world-class cities</p>
          </div>
          <div className="relative max-w-2xl mx-auto">{searchQuery && (
              <div className="absolute -top-6 right-0 text-sm text-white/60">
                {filteredCities.length} {filteredCities.length === 1 ? 'result' : 'results'}
              </div>
            )}
            <input
              type="text"
              placeholder="Search cities by name or country..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full glass-card text-white placeholder:text-white/50 px-6 py-4 pl-12 rounded-xl outline-none border border-white/10 focus:border-cyan-400 transition-smooth"
            />
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-2xl">🔍</span>
          </div>
        </div>

        {/* Cities Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredCities.map((city, index) => (
            <Link
              key={city.slug}
              href={`/cities/${city.slug}`}
              className="group animate-fade-in-up"
              style={{ animationDelay: `${index * 100}ms` }}
            >
              <div className="glass-card p-6 hover:scale-105 transition-smooth hover:shadow-2xl hover:shadow-cyan-500/20 relative overflow-hidden rounded-2xl">
                {/* Gradient Background */}
                <div className={`absolute inset-0 bg-gradient-to-br ${city.gradient} opacity-0 group-hover:opacity-10 transition-smooth`} />
                
                {/* Content */}
                <div className="relative z-10">
                  <div className="flex items-start justify-between mb-4">
                    <div className="text-4xl">{city.emoji}</div>
                    <div className="bg-cyan-500/10 text-cyan-300 px-3 py-1 rounded-full text-xs font-medium border border-cyan-400/30">
                      {city.country}
                    </div>
                  </div>
                  
                  <h3 className="text-xl font-bold text-white mb-2 group-hover:text-cyan-300 transition-smooth">
                    {city.name}
                  </h3>
                  
                  <p className="text-sm text-white/70 mb-4">
                    {city.description}
                  </p>
                  
                  <div className="flex items-center justify-between text-sm">
                    <div className="text-white/60">
                      Pop: {(city.population / 1000000).toFixed(1)}M
                    </div>
                    <div className="text-cyan-400 opacity-0 group-hover:opacity-100 transition-smooth">
                      View Profile →
                    </div>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>

        {filteredCities.length === 0 && (
          <div className="text-center py-12">
            <div className="text-6xl mb-4">🔍</div>
            <p className="text-white/70">No cities found matching &ldquo;{searchQuery}&rdquo;</p>
          </div>
        )}
      </div>
    </AppShell>
  );
}
