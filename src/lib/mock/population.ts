/**
 * Mock Population Data
 * 
 * In production, this would come from:
 * - U.S. Census Bureau API
 * - World Bank Data API
 * - City planning department databases
 * 
 * API structure: GET /api/cities/:slug/demographics/population?years=10
 */

export interface PopulationDataPoint {
  year: string;
  population: number;
  growthRate: number;
}

export const getPopulationData = (): PopulationDataPoint[] => {
  // Mock data - replace with API call
  return [
    { year: '2015', population: 7.8, growthRate: 1.8 },
    { year: '2016', population: 7.9, growthRate: 1.3 },
    { year: '2017', population: 8.0, growthRate: 1.3 },
    { year: '2018', population: 8.1, growthRate: 1.2 },
    { year: '2019', population: 8.2, growthRate: 1.2 },
    { year: '2020', population: 8.1, growthRate: -1.2 },
    { year: '2021', population: 8.2, growthRate: 1.2 },
    { year: '2022', population: 8.25, growthRate: 0.6 },
    { year: '2023', population: 8.3, growthRate: 0.6 },
    { year: '2024', population: 8.35, growthRate: 0.6 },
  ];
};
