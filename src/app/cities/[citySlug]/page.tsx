/**
 * City Overview Page (Default)
 * Route: /cities/[citySlug]
 * 
 * This is the default page when accessing a city.
 * Shows high-level city information and key metrics.
 */

'use client';

import { MetricCard, ChartCard, AreaChart, BarChart } from '@/components';

// Sample population growth data
const populationData = [
  { year: '2015', population: 7.8 },
  { year: '2016', population: 7.9 },
  { year: '2017', population: 8.0 },
  { year: '2018', population: 8.1 },
  { year: '2019', population: 8.2 },
  { year: '2020', population: 8.1 },
  { year: '2021', population: 8.2 },
  { year: '2022', population: 8.25 },
  { year: '2023', population: 8.3 },
];

// Cost of living breakdown
const costOfLivingData = [
  { category: 'Housing', index: 195 },
  { category: 'Food', index: 142 },
  { category: 'Transport', index: 128 },
  { category: 'Healthcare', index: 135 },
  { category: 'Education', index: 168 },
];

const costColors = ['#ef4444', '#f59e0b', '#10b981', '#3b82f6', '#8b5cf6'];

export default function CityOverviewPage() {
  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="mb-6">
        <div className="inline-block px-3 py-1 glass-card text-primary rounded-full text-xs font-semibold mb-2">
          City Overview
        </div>
        <h1 className="text-2xl md:text-3xl font-bold text-text-primary mb-2">
          Key Metrics & Highlights
        </h1>
        <p className="text-text-secondary">
          High-level overview of city performance and characteristics
        </p>
      </div>

      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Population"
          value="8.3M"
          change={{ value: 2.3, period: 'vs last year' }}
          trend="up"
          status="good"
        />
        <MetricCard
          label="GDP per Capita"
          value="$85K"
          change={{ value: 4.1, period: 'vs last year' }}
          trend="up"
          status="good"
        />
        <MetricCard
          label="Unemployment Rate"
          value="3.8%"
          change={{ value: -0.5, period: 'vs last quarter' }}
          trend="down"
          status="good"
        />
        <MetricCard
          label="Cost of Living Index"
          value="158"
          change={{ value: 1.2, period: 'vs last year' }}
          trend="up"
          status="warning"
        />
      </div>

      {/* Charts Section */}
      <div className="mb-3">
        <h2 className="text-xl font-bold text-text-primary mb-1">📊 Trends & Analytics</h2>
        <p className="text-text-secondary text-sm">Historical data and growth patterns</p>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <ChartCard
          title="Population Growth Trend"
          description="Historical population data over the last 9 years (in millions)"
        >
          <AreaChart
            data={populationData}
            xKey="year"
            areas={[
              { dataKey: 'population', name: 'Population (M)', color: '#6366f1', fillOpacity: 0.6 },
            ]}
            yAxisLabel="Population (Millions)"
            height={280}
          />
        </ChartCard>

        <ChartCard
          title="Cost of Living Index"
          description="Comparison by category (National Average = 100)"
        >
          <BarChart
            data={costOfLivingData}
            xKey="category"
            bars={[
              { dataKey: 'index', name: 'Cost Index', color: '#8b5cf6' },
            ]}
            yAxisLabel="Index Value"
            height={280}
            customColors={costColors}
          />
        </ChartCard>
      </div>

      {/* City Highlights */}
      <div className="mb-3">
        <h2 className="text-xl font-bold text-text-primary mb-1">🎯 Strengths & Challenges</h2>
        <p className="text-text-secondary text-sm">Key advantages and areas for improvement</p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="glass-card p-5 rounded-xl">
          <h3 className="text-lg font-semibold text-text-primary mb-4 flex items-center gap-2">
            <span>🏙️</span>
            City Highlights
          </h3>
          <ul className="space-y-3">
            <li className="flex items-start gap-3">
              <span className="text-success text-xl">✓</span>
              <div>
                <p className="text-text-primary font-medium">Global Financial Hub</p>
                <p className="text-sm text-text-secondary">Major center for finance and business</p>
              </div>
            </li>
            <li className="flex items-start gap-3">
              <span className="text-success text-xl">✓</span>
              <div>
                <p className="text-text-primary font-medium">Cultural Diversity</p>
                <p className="text-sm text-text-secondary">Over 800 languages spoken</p>
              </div>
            </li>
            <li className="flex items-start gap-3">
              <span className="text-success text-xl">✓</span>
              <div>
                <p className="text-text-primary font-medium">Innovation Leader</p>
                <p className="text-sm text-text-secondary">Top 5 in global innovation index</p>
              </div>
            </li>
          </ul>
        </div>

        <div className="card p-6">
          <h3 className="text-lg font-semibold text-text-primary mb-4 flex items-center gap-2">
            <span>⚠️</span>
            Key Challenges
          </h3>
          <ul className="space-y-3">
            <li className="flex items-start gap-3">
              <span className="text-warning text-xl">⚡</span>
              <div>
                <p className="text-text-primary font-medium">Housing Affordability</p>
                <p className="text-sm text-text-secondary">Rising costs impact residents</p>
              </div>
            </li>
            <li className="flex items-start gap-3">
              <span className="text-warning text-xl">⚡</span>
              <div>
                <p className="text-text-primary font-medium">Transportation Congestion</p>
                <p className="text-sm text-text-secondary">Infrastructure under pressure</p>
              </div>
            </li>
            <li className="flex items-start gap-3">
              <span className="text-warning text-xl">⚡</span>
              <div>
                <p className="text-text-primary font-medium">Climate Adaptation</p>
                <p className="text-sm text-text-secondary">Need for sustainable solutions</p>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}

