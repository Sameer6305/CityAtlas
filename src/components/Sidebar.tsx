/**
 * Sidebar Component
 * 
 * Left navigation sidebar for main app navigation
 * Responsive: Collapses to icon-only on mobile, full width on desktop
 */

'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useState } from 'react';
import { useBookmarks, useComparison } from '@/store/useAppStore';

export function Sidebar() {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);
  const { bookmarkedCities } = useBookmarks();
  const { comparisonList } = useComparison();

  const navItems = [
    { label: 'Home', href: '/', icon: '🏠' },
    { label: 'All Cities', href: '/cities', icon: '🏙️' },
    { label: 'Bookmarks', href: '/bookmarks', icon: '⭐', badge: bookmarkedCities.length },
    { label: 'Compare', href: '/compare', icon: '⚖️', badge: comparisonList.length },
  ];

  return (
    <>
      {/* Mobile Overlay */}
      <div className="lg:hidden fixed inset-0 bg-black/50 z-40 hidden" id="sidebar-overlay" />
      
      {/* Sidebar */}
      <aside
        className={`
          fixed top-0 left-0 h-screen glass-nav
          transition-all duration-300 z-50
          ${collapsed ? 'w-16' : 'w-64'}
          lg:translate-x-0
        `}
      >
        {/* Logo & Toggle */}
        <div className="h-16 flex items-center justify-between px-4 border-b border-surface-border/50">
          {!collapsed && (
            <Link href="/" className="flex items-center gap-2 hover:opacity-80 transition-fast">
              <img src="/logo.png" alt="CityAtlas" className="h-10 w-auto rounded-lg" />
              <span className="text-lg font-bold text-text-primary">CityAtlas</span>
            </Link>
          )}
          {collapsed && (
            <button
              onClick={() => setCollapsed(false)}
              className="w-full flex justify-center"
            >
              <img src="/logo.png" alt="CityAtlas" className="h-8 w-auto rounded-lg" />
            </button>
          )}
          {!collapsed && (
            <button
              onClick={() => setCollapsed(true)}
              className="text-text-tertiary hover:text-text-primary transition-fast text-sm"
            >
              ◀
            </button>
          )}
        </div>

        {/* Navigation */}
        <nav className="p-3 space-y-1">
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`
                  flex items-center gap-3 px-3 py-2.5 rounded-lg
                  transition-fast font-medium
                  ${isActive
                    ? 'bg-primary text-white shadow-lg shadow-primary/30'
                    : 'text-text-secondary hover:bg-surface-elevated hover:text-text-primary'
                  }
                  ${collapsed ? 'justify-center' : ''}
                `}
              >
                <span className="text-lg">{item.icon}</span>
                {!collapsed && (
                  <span className="flex-1">{item.label}</span>
                )}
                {!collapsed && item.badge !== undefined && item.badge > 0 && (
                  <span className="px-2 py-0.5 bg-primary/20 text-primary rounded-full text-xs font-semibold">
                    {item.badge}
                  </span>
                )}
              </Link>
            );
          })}
        </nav>

        {/* Quick Links Section */}
        {!collapsed && (
          <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-surface-border">
            <div className="space-y-2">
              <button className="w-full text-left text-sm text-text-tertiary hover:text-text-primary transition-colors">
                ⚙️ Settings
              </button>
              <button className="w-full text-left text-sm text-text-tertiary hover:text-text-primary transition-colors">
                ❓ Help
              </button>
            </div>
          </div>
        )}
      </aside>
    </>
  );
}
