/**
 * Mock Cost of Living Data
 * 
 * In production, this would come from:
 * - Numbeo API
 * - Council for Community and Economic Research (C2ER)
 * - City economic reports
 * 
 * API structure: GET /api/cities/:slug/economy/cost-of-living
 */

export interface CostOfLivingData {
  category: string;
  index: number;
  nationalAverage: number;
  monthlyAvg: number;
}

export const getCostOfLivingData = (): CostOfLivingData[] => {
  // Mock data - replace with API call
  // Index: National average = 100
  return [
    { category: 'Housing', index: 195, nationalAverage: 100, monthlyAvg: 2850 },
    { category: 'Food', index: 142, nationalAverage: 100, monthlyAvg: 680 },
    { category: 'Transportation', index: 128, nationalAverage: 100, monthlyAvg: 420 },
    { category: 'Healthcare', index: 135, nationalAverage: 100, monthlyAvg: 550 },
    { category: 'Utilities', index: 118, nationalAverage: 100, monthlyAvg: 185 },
    { category: 'Entertainment', index: 148, nationalAverage: 100, monthlyAvg: 320 },
  ];
};
