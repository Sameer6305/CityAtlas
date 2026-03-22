/// <reference types="jest" />

import { fetchCityData } from '@/lib/api';

describe('api.fetchCityData', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('returns city payload on success', async () => {
    const payload = {
      id: 1,
      slug: 'new-york',
      name: 'New York',
      state: 'New York',
      country: 'United States',
      countryCode: 'US',
      population: 8336817,
      gdpPerCapita: 85000,
      latitude: 40.7128,
      longitude: -74.006,
      costOfLivingIndex: null,
      unemploymentRate: 4.1,
      literacyRate: 99.0,
      pupilTeacherRatio: 12.5,
      renewableEnergyPct: 20.1,
      co2PerCapita: 5.4,
      languages: ['English'],
      bannerImageUrl: null,
      description: 'Sample',
      lastUpdated: new Date().toISOString(),
      hospitalBedsPer1000: 2.8,
      healthExpenditurePerCapita: 1000,
      lifeExpectancy: 78,
      internetUsersPct: 95,
      mobileSubscriptionsPer100: 120,
      electricityAccessPct: 100,
      weatherTemp: 24,
      weatherDescription: 'Clear',
      weatherIcon: '01d',
      weatherHumidity: 60,
      weatherWindSpeed: 3,
      airQualityIndex: 40,
      airQualityCategory: 'Good',
      pm25: 9,
    };

    global.fetch = (jest.fn().mockResolvedValue({
      ok: true,
      json: async () => payload,
    } as Response) as unknown as typeof fetch);

    const result = await fetchCityData('new-york');
    // FIXED: Assert API client returns parsed city data shape on success.
    expect(result?.name).toBe('New York');
    expect(result?.slug).toBe('new-york');
  });

  test('handles non-2xx responses gracefully', async () => {
    global.fetch = (jest.fn().mockResolvedValue({ ok: false } as Response) as unknown as typeof fetch);

    const result = await fetchCityData('missing-city');
    expect(result).toBeNull();
  });
});
