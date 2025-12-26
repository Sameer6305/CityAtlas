/**
 * Theme Provider Component
 * 
 * Purpose:
 * - Initializes theme on client-side mount
 * - Prevents flash of unstyled content (FOUC)
 * - Syncs Zustand state with HTML class
 * 
 * Usage:
 * Wrap app content in root layout
 */

'use client';

import { useEffect } from 'react';
import { useTheme } from '@/store/useAppStore';

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const { theme } = useTheme();
  
  /**
   * Apply theme to document on mount and when theme changes
   * This ensures the HTML class matches the stored theme
   */
  useEffect(() => {
    // Remove both classes first
    document.documentElement.classList.remove('dark', 'light');
    
    // Add the current theme class
    document.documentElement.classList.add(theme);
  }, [theme]);
  
  return <>{children}</>;
}
