/// <reference types="jest" />

import { beforeEach, describe, expect, test } from '@jest/globals';
import { useAppStore } from '@/store/useAppStore';

describe('useAppStore', () => {
  beforeEach(() => {
    useAppStore.setState({
      selectedCity: null,
      theme: 'dark',
      preferences: {
        defaultView: 'grid',
        itemsPerPage: 12,
        comparisonList: [],
        bookmarkedCities: [],
        recentlyViewed: [],
        preferredChartType: 'line',
        showTrendLines: true,
        aiSummaryLength: 'medium',
      },
    });
  });

  test('initializes with expected default state', () => {
    const state = useAppStore.getState();
    // FIXED: Baseline state test guards against accidental store regression.
    expect(state.theme).toBe('dark');
    expect(state.selectedCity).toBeNull();
    expect(state.preferences.comparisonList).toHaveLength(0);
  });

  test('setSelectedCity updates state', () => {
    const city = {
      id: '1',
      slug: 'new-york',
      name: 'New York',
      country: 'US',
      population: 1000,
      gdpPerCapita: 1,
      coordinates: {
        lat: 1,
        lng: 1,
      },
      lastUpdated: new Date(),
    };

    useAppStore.getState().setSelectedCity(city);

    expect(useAppStore.getState().selectedCity?.slug).toBe('new-york');
  });
});
