/**
 * Store Usage Examples
 * 
 * This file demonstrates how to use the Zustand store in components.
 * DELETE THIS FILE - It's only for reference during development.
 */

'use client';

import { 
  useAppStore, 
  useTheme, 
  useCityState, 
  useComparison, 
  useBookmarks 
} from '@/store/useAppStore';

// ========================================
// Example 1: Using the entire store
// ========================================
function ExampleFullStore() {
  const { 
    theme, 
    toggleTheme, 
    preferences,
  } = useAppStore();
  
  return (
    <div>
      <p>Current theme: {theme}</p>
      <button onClick={toggleTheme}>Toggle Theme</button>
      <p>Items per page: {preferences.itemsPerPage}</p>
    </div>
  );
}

// ========================================
// Example 2: Using theme hook (recommended)
// ========================================
function ExampleTheme() {
  const { theme, toggleTheme, setTheme } = useTheme();
  
  return (
    <div>
      <button onClick={toggleTheme}>
        Switch to {theme === 'dark' ? 'Light' : 'Dark'} Mode
      </button>
      <button onClick={() => setTheme('dark')}>Force Dark</button>
      <button onClick={() => setTheme('light')}>Force Light</button>
    </div>
  );
}

// ========================================
// Example 3: Using city state
// ========================================
function ExampleCityState() {
  const { selectedCity, setSelectedCity } = useCityState();
  
  const handleSelectCity = () => {
    setSelectedCity({
      id: '1',
      slug: 'san-francisco',
      name: 'San Francisco',
      country: 'USA',
      population: 815201,
      gdpPerCapita: 128400,
      coordinates: { lat: 37.7749, lng: -122.4194 },
      lastUpdated: new Date(),
    });
  };
  
  return (
    <div>
      {selectedCity ? (
        <div>
          <h2>{selectedCity.name}</h2>
          <p>Population: {selectedCity.population.toLocaleString()}</p>
          <button onClick={() => setSelectedCity(null)}>Clear</button>
        </div>
      ) : (
        <button onClick={handleSelectCity}>Select San Francisco</button>
      )}
    </div>
  );
}

// ========================================
// Example 4: Using comparison feature
// ========================================
function ExampleComparison() {
  const { 
    comparisonList, 
    addToComparison, 
    removeFromComparison,
    clearComparison 
  } = useComparison();
  
  return (
    <div>
      <h3>Compare Cities ({comparisonList.length}/5)</h3>
      <ul>
        {comparisonList.map((slug) => (
          <li key={slug}>
            {slug}
            <button onClick={() => removeFromComparison(slug)}>Remove</button>
          </li>
        ))}
      </ul>
      <button onClick={() => addToComparison('san-francisco')}>
        Add San Francisco
      </button>
      <button onClick={() => addToComparison('austin')}>
        Add Austin
      </button>
      <button onClick={clearComparison}>Clear All</button>
    </div>
  );
}

// ========================================
// Example 5: Using bookmarks
// ========================================
function ExampleBookmarks() {
  const { 
    bookmarkedCities, 
    bookmarkCity, 
    unbookmarkCity,
    isBookmarked 
  } = useBookmarks();
  
  const citySlug = 'san-francisco';
  
  return (
    <div>
      <h3>Bookmarked Cities ({bookmarkedCities.length})</h3>
      <ul>
        {bookmarkedCities.map((slug) => (
          <li key={slug}>
            {slug}
            <button onClick={() => unbookmarkCity(slug)}>★</button>
          </li>
        ))}
      </ul>
      
      <button 
        onClick={() => 
          isBookmarked(citySlug) 
            ? unbookmarkCity(citySlug)
            : bookmarkCity(citySlug)
        }
      >
        {isBookmarked(citySlug) ? '★ Bookmarked' : '☆ Bookmark'}
      </button>
    </div>
  );
}

// ========================================
// Example 6: Updating preferences
// ========================================
function ExamplePreferences() {
  const updatePreferences = useAppStore((state) => state.updatePreferences);
  const preferences = useAppStore((state) => state.preferences);
  
  return (
    <div>
      <h3>Display Preferences</h3>
      
      {/* View Mode */}
      <select 
        value={preferences.defaultView}
        onChange={(e) => updatePreferences({ 
          defaultView: e.target.value as 'grid' | 'list' | 'map' 
        })}
      >
        <option value="grid">Grid</option>
        <option value="list">List</option>
        <option value="map">Map</option>
      </select>
      
      {/* Items per page */}
      <input
        type="number"
        value={preferences.itemsPerPage}
        onChange={(e) => updatePreferences({ 
          itemsPerPage: parseInt(e.target.value) 
        })}
      />
      
      {/* Chart type */}
      <select
        value={preferences.preferredChartType}
        onChange={(e) => updatePreferences({ 
          preferredChartType: e.target.value as 'line' | 'bar' | 'area' 
        })}
      >
        <option value="line">Line Chart</option>
        <option value="bar">Bar Chart</option>
        <option value="area">Area Chart</option>
      </select>
    </div>
  );
}

// ========================================
// Example 7: Accessing recently viewed
// ========================================
function ExampleRecentlyViewed() {
  const recentlyViewed = useAppStore((state) => state.preferences.recentlyViewed);
  
  return (
    <div>
      <h3>Recently Viewed</h3>
      <ul>
        {recentlyViewed.map((slug) => (
          <li key={slug}>{slug}</li>
        ))}
      </ul>
      <p className="text-xs text-text-tertiary">
        Cities are automatically added when selected
      </p>
    </div>
  );
}

// ========================================
// Example 8: Selector optimization
// ========================================
function ExampleOptimizedSelectors() {
  // ✅ Good: Only re-renders when theme changes
  const theme = useAppStore((state) => state.theme);
  
  // ✅ Good: Only re-renders when comparison list changes
  const comparisonCount = useAppStore(
    (state) => state.preferences.comparisonList.length
  );
  
  // ✅ Good: Multiple selectors for specific values
  const isBookmarked = useAppStore((state) => 
    state.preferences.bookmarkedCities.includes('san-francisco')
  );
  
  return (
    <div>
      <p>Theme: {theme}</p>
      <p>Comparing: {comparisonCount} cities</p>
      <p>SF Bookmarked: {isBookmarked ? 'Yes' : 'No'}</p>
    </div>
  );
}

// ========================================
// Export all examples (for reference only)
// ========================================
export {
  ExampleFullStore,
  ExampleTheme,
  ExampleCityState,
  ExampleComparison,
  ExampleBookmarks,
  ExamplePreferences,
  ExampleRecentlyViewed,
  ExampleOptimizedSelectors,
};
