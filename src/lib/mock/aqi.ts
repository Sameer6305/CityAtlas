/**
 * Mock AQI (Air Quality Index) Data
 * 
 * In production, this would come from:
 * - Environmental monitoring APIs (e.g., OpenAQ, IQAir)
 * - City government open data portals
 * 
 * API structure: GET /api/cities/:slug/environment/aqi?period=12m
 */

export interface AQIDataPoint {
  month: string;
  aqi: number;
  category: 'Good' | 'Moderate' | 'Unhealthy' | 'Hazardous';
}

export const getAQITrendData = (): AQIDataPoint[] => {
  // Mock data - replace with API call
  return [
    { month: 'Jan', aqi: 52, category: 'Moderate' },
    { month: 'Feb', aqi: 48, category: 'Good' },
    { month: 'Mar', aqi: 45, category: 'Good' },
    { month: 'Apr', aqi: 41, category: 'Good' },
    { month: 'May', aqi: 38, category: 'Good' },
    { month: 'Jun', aqi: 42, category: 'Good' },
    { month: 'Jul', aqi: 47, category: 'Good' },
    { month: 'Aug', aqi: 44, category: 'Good' },
    { month: 'Sep', aqi: 40, category: 'Good' },
    { month: 'Oct', aqi: 43, category: 'Good' },
    { month: 'Nov', aqi: 46, category: 'Good' },
    { month: 'Dec', aqi: 45, category: 'Good' },
  ];
};
