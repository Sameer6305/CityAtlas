/**
 * TabNavigation Component
 * 
 * Horizontal tab navigation for city sections
 * Sticky tabs that follow scroll
 * Resume-style section navigation
 */

'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { CITY_SECTIONS } from '@/types/city';

interface TabNavigationProps {
  citySlug: string;
}

export function TabNavigation({ citySlug }: TabNavigationProps) {
  const pathname = usePathname();

  // Determine active tab based on current path
  const getActivePath = () => {
    // If path ends with citySlug, it's the overview (default)
    if (pathname === `/cities/${citySlug}`) {
      return 'overview';
    }
    
    // Extract section from path
    const segments = pathname.split('/');
    return segments[segments.length - 1];
  };

  const activeSection = getActivePath();

  return (
    <nav className="sticky top-16 z-40 glass-nav border-b border-white/10 overflow-x-auto">
      <div className="container mx-auto px-6">
        <div className="flex gap-1">
          {CITY_SECTIONS.map((section) => {
            const isActive = activeSection === section.slug;
            const href = section.slug === 'overview' 
              ? `/cities/${citySlug}`
              : `/cities/${citySlug}/${section.slug}`;

            return (
              <Link
                key={section.id}
                href={href}
                className={`
                  relative px-4 py-3 whitespace-nowrap
                  font-medium text-sm
                  transition-fast
                  ${isActive
                    ? 'text-cyan-400 border-b-2 border-cyan-400'
                    : 'text-white/60 hover:text-white hover:bg-white/5'
                  }
                `}
                title={section.description}
              >
                {section.label}
                
                {/* Active indicator */}
                {isActive && (
                  <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-cyan-400 shadow-lg shadow-cyan-400/50" />
                )}
              </Link>
            );
          })}
        </div>
      </div>
      
      {/* Fade indicators for scroll */}
      <div className="absolute top-0 left-0 bottom-0 w-8 bg-gradient-to-r from-background to-transparent pointer-events-none" />
      <div className="absolute top-0 right-0 bottom-0 w-8 bg-gradient-to-l from-background to-transparent pointer-events-none" />
    </nav>
  );
}
