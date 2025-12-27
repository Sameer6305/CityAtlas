/**
 * Mock Job Market Data
 * 
 * In production, this would come from:
 * - Bureau of Labor Statistics API
 * - LinkedIn Economic Graph
 * - City employment databases
 * 
 * API structure: GET /api/cities/:slug/economy/jobs
 */

export interface JobSectorData {
  sector: string;
  employees: number;
  percentage: number;
  growthRate: number;
}

export const getJobSectorData = (): JobSectorData[] => {
  // Mock data - replace with API call
  return [
    { sector: 'Technology', employees: 285000, percentage: 28, growthRate: 8.5 },
    { sector: 'Finance', employees: 225000, percentage: 22, growthRate: 3.2 },
    { sector: 'Healthcare', employees: 185000, percentage: 18, growthRate: 5.1 },
    { sector: 'Education', employees: 125000, percentage: 12, growthRate: 2.8 },
    { sector: 'Retail', employees: 105000, percentage: 10, growthRate: -1.2 },
    { sector: 'Other', employees: 95000, percentage: 10, growthRate: 1.5 },
  ];
};
