/**
 * City Data Types
 * Defines the structure for city data and sections
 */

export type CitySlug = string;

export interface City {
  id: string;
  slug: string;
  name: string;
  state?: string;
  country: string;
  population: number;
  gdpPerCapita: number;
  coordinates: {
    lat: number;
    lng: number;
  };
  bannerImage?: string;
  lastUpdated: Date;
}

export interface CitySection {
  id: string;
  slug: string;
  label: string;
  description: string;
  icon?: string;
}

export const CITY_SECTIONS: CitySection[] = [
  {
    id: 'overview',
    slug: 'overview',
    label: 'Overview',
    description: 'City snapshot, population, GDP, climate, ranking',
  },
  {
    id: 'economy',
    slug: 'economy',
    label: 'Economy',
    description: 'Job market, industries, income, startup ecosystem',
  },
  {
    id: 'infrastructure',
    slug: 'infrastructure',
    label: 'Infrastructure',
    description: 'Transportation, utilities, internet, housing',
  },
  {
    id: 'education',
    slug: 'education',
    label: 'Education',
    description: 'Universities, literacy, STEM pipeline, retention',
  },
  {
    id: 'culture',
    slug: 'culture',
    label: 'Culture',
    description: 'Arts, diversity, expat community, quality of life',
  },
  {
    id: 'environment',
    slug: 'environment',
    label: 'Environment',
    description: 'Air quality, green space, sustainability, climate risk',
  },
  {
    id: 'analytics',
    slug: 'analytics',
    label: 'Analytics',
    description: 'Trends, forecasts, peer comparisons',
  },
  {
    id: 'ai-summary',
    slug: 'ai-summary',
    label: 'AI Summary',
    description: 'LLM-generated insights, strengths, weaknesses',
  },
];

export type SectionSlug = typeof CITY_SECTIONS[number]['slug'];
