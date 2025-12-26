/**
 * Global Application Store (Zustand)
 * 
 * Why Zustand?
 * - Minimal boilerplate compared to Redux
 * - No Provider wrapper needed
 * - TypeScript-first design
 * - Built-in persistence middleware
 * - Excellent DevTools support
 * 
 * Store Structure:
 * 1. City State - Currently selected city for comparison/bookmarking
 * 2. Theme State - Dark/light mode preference (persisted)
 * 3. User Preferences - UI settings, filters, comparison lists
 */

import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { City } from '@/types/city';

/**
 * User Preferences Interface
 * Stores UI settings and user-specific configurations
 */
export interface UserPreferences {
  // Display preferences
  defaultView: 'grid' | 'list' | 'map';
  itemsPerPage: number;
  
  // Comparison & bookmarks
  comparisonList: string[]; // Array of city slugs
  bookmarkedCities: string[]; // Array of city slugs
  recentlyViewed: string[]; // Array of city slugs (max 10)
  
  // Analytics preferences
  preferredChartType: 'line' | 'bar' | 'area';
  showTrendLines: boolean;
  
  // AI preferences
  aiSummaryLength: 'short' | 'medium' | 'detailed';
}

/**
 * Application State Interface
 * Defines the complete state shape
 */
interface AppState {
  // ========================================
  // CITY STATE
  // ========================================
  
  /**
   * Currently selected/active city
   * Used for detail views and comparisons
   */
  selectedCity: City | null;
  
  /**
   * Update the selected city
   */
  setSelectedCity: (city: City | null) => void;
  
  // ========================================
  // THEME STATE (PERSISTED)
  // ========================================
  
  /**
   * Current theme mode
   * Default: 'dark' (matches design system)
   * Persisted in localStorage
   */
  theme: 'dark' | 'light';
  
  /**
   * Toggle between dark and light themes
   */
  toggleTheme: () => void;
  
  /**
   * Set theme explicitly
   */
  setTheme: (theme: 'dark' | 'light') => void;
  
  // ========================================
  // USER PREFERENCES (PERSISTED)
  // ========================================
  
  /**
   * User-specific UI preferences
   * Persisted in localStorage
   */
  preferences: UserPreferences;
  
  /**
   * Update user preferences (partial update)
   */
  updatePreferences: (prefs: Partial<UserPreferences>) => void;
  
  /**
   * Add city to comparison list (max 5 cities)
   */
  addToComparison: (citySlug: string) => void;
  
  /**
   * Remove city from comparison list
   */
  removeFromComparison: (citySlug: string) => void;
  
  /**
   * Clear all cities from comparison
   */
  clearComparison: () => void;
  
  /**
   * Bookmark a city
   */
  bookmarkCity: (citySlug: string) => void;
  
  /**
   * Remove bookmark
   */
  unbookmarkCity: (citySlug: string) => void;
  
  /**
   * Add to recently viewed (FIFO, max 10)
   */
  addToRecentlyViewed: (citySlug: string) => void;
  
  /**
   * Reset all preferences to defaults
   */
  resetPreferences: () => void;
}

/**
 * Default User Preferences
 */
const DEFAULT_PREFERENCES: UserPreferences = {
  defaultView: 'grid',
  itemsPerPage: 12,
  comparisonList: [],
  bookmarkedCities: [],
  recentlyViewed: [],
  preferredChartType: 'line',
  showTrendLines: true,
  aiSummaryLength: 'medium',
};

/**
 * Global Application Store
 * 
 * Persistence Strategy:
 * - Theme and preferences are persisted to localStorage
 * - Selected city is NOT persisted (session-only)
 * - Uses 'cityatlas-storage' key in localStorage
 */
export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      // ========================================
      // INITIAL STATE
      // ========================================
      
      selectedCity: null,
      theme: 'dark', // Default to dark mode (matches design system)
      preferences: DEFAULT_PREFERENCES,
      
      // ========================================
      // CITY ACTIONS
      // ========================================
      
      setSelectedCity: (city) => {
        set({ selectedCity: city });
        
        // Auto-add to recently viewed when city is selected
        if (city) {
          get().addToRecentlyViewed(city.slug);
        }
      },
      
      // ========================================
      // THEME ACTIONS
      // ========================================
      
      toggleTheme: () => {
        const currentTheme = get().theme;
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        
        set({ theme: newTheme });
        
        // Update HTML class for CSS
        if (typeof document !== 'undefined') {
          document.documentElement.classList.remove('dark', 'light');
          document.documentElement.classList.add(newTheme);
        }
      },
      
      setTheme: (theme) => {
        set({ theme });
        
        // Update HTML class for CSS
        if (typeof document !== 'undefined') {
          document.documentElement.classList.remove('dark', 'light');
          document.documentElement.classList.add(theme);
        }
      },
      
      // ========================================
      // PREFERENCES ACTIONS
      // ========================================
      
      updatePreferences: (prefs) => {
        set((state) => ({
          preferences: {
            ...state.preferences,
            ...prefs,
          },
        }));
      },
      
      addToComparison: (citySlug) => {
        set((state) => {
          const current = state.preferences.comparisonList;
          
          // Prevent duplicates
          if (current.includes(citySlug)) {
            return state;
          }
          
          // Limit to 5 cities for comparison
          const updated = [...current, citySlug].slice(-5);
          
          return {
            preferences: {
              ...state.preferences,
              comparisonList: updated,
            },
          };
        });
      },
      
      removeFromComparison: (citySlug) => {
        set((state) => ({
          preferences: {
            ...state.preferences,
            comparisonList: state.preferences.comparisonList.filter(
              (slug) => slug !== citySlug
            ),
          },
        }));
      },
      
      clearComparison: () => {
        set((state) => ({
          preferences: {
            ...state.preferences,
            comparisonList: [],
          },
        }));
      },
      
      bookmarkCity: (citySlug) => {
        set((state) => {
          const current = state.preferences.bookmarkedCities;
          
          // Prevent duplicates
          if (current.includes(citySlug)) {
            return state;
          }
          
          return {
            preferences: {
              ...state.preferences,
              bookmarkedCities: [...current, citySlug],
            },
          };
        });
      },
      
      unbookmarkCity: (citySlug) => {
        set((state) => ({
          preferences: {
            ...state.preferences,
            bookmarkedCities: state.preferences.bookmarkedCities.filter(
              (slug) => slug !== citySlug
            ),
          },
        }));
      },
      
      addToRecentlyViewed: (citySlug) => {
        set((state) => {
          const current = state.preferences.recentlyViewed;
          
          // Remove if already exists (to move it to front)
          const filtered = current.filter((slug) => slug !== citySlug);
          
          // Add to front, limit to 10 items (FIFO)
          const updated = [citySlug, ...filtered].slice(0, 10);
          
          return {
            preferences: {
              ...state.preferences,
              recentlyViewed: updated,
            },
          };
        });
      },
      
      resetPreferences: () => {
        set({
          preferences: DEFAULT_PREFERENCES,
        });
      },
    }),
    {
      name: 'cityatlas-storage', // localStorage key
      storage: createJSONStorage(() => localStorage),
      
      /**
       * Partial Persistence Configuration
       * Only persist theme and preferences, not selectedCity
       */
      partialize: (state) => ({
        theme: state.theme,
        preferences: state.preferences,
        // selectedCity is intentionally excluded (session-only)
      }),
    }
  )
);

/**
 * Hook to get theme state and actions
 * Convenience hook for components that only need theme
 */
export const useTheme = () => {
  const theme = useAppStore((state) => state.theme);
  const toggleTheme = useAppStore((state) => state.toggleTheme);
  const setTheme = useAppStore((state) => state.setTheme);
  
  return { theme, toggleTheme, setTheme };
};

/**
 * Hook to get city state and actions
 * Convenience hook for components that only need city state
 */
export const useCityState = () => {
  const selectedCity = useAppStore((state) => state.selectedCity);
  const setSelectedCity = useAppStore((state) => state.setSelectedCity);
  
  return { selectedCity, setSelectedCity };
};

/**
 * Hook to get comparison state and actions
 * Convenience hook for comparison features
 */
export const useComparison = () => {
  const comparisonList = useAppStore((state) => state.preferences.comparisonList);
  const addToComparison = useAppStore((state) => state.addToComparison);
  const removeFromComparison = useAppStore((state) => state.removeFromComparison);
  const clearComparison = useAppStore((state) => state.clearComparison);
  
  return {
    comparisonList,
    addToComparison,
    removeFromComparison,
    clearComparison,
  };
};

/**
 * Hook to get bookmarks state and actions
 * Convenience hook for bookmark features
 */
export const useBookmarks = () => {
  const bookmarkedCities = useAppStore((state) => state.preferences.bookmarkedCities);
  const bookmarkCity = useAppStore((state) => state.bookmarkCity);
  const unbookmarkCity = useAppStore((state) => state.unbookmarkCity);
  
  return {
    bookmarkedCities,
    bookmarkCity,
    unbookmarkCity,
    isBookmarked: (citySlug: string) => bookmarkedCities.includes(citySlug),
  };
};
