/**
 * Analytics Section Page
 * Route: /cities/[citySlug]/analytics
 * 
 * Displays trend graphs, forecasts, peer city comparisons
 */

'use client';

import { MetricCard, ChartCard, LineChart, BarChart } from '@/components';

// Multi-year growth trends
const growthTrendData = [
  { year: '2020', gdp: 78, population: 7.9, employment: 82 },
  { year: '2021', gdp: 82, population: 8.1, employment: 84 },
  { year: '2022', gdp: 88, population: 8.25, employment: 87 },
  { year: '2023', gdp: 92, population: 8.3, employment: 89 },
  { year: '2024', gdp: 95, population: 8.4, employment: 91 },
];

// Peer city comparison
const peerComparisonData = [
  { city: 'This City', score: 92 },
  { city: 'Seattle', score: 88 },
  { city: 'Boston', score: 86 },
  { city: 'Austin', score: 84 },
  { city: 'Denver', score: 82 },
];

export default function AnalyticsPage() {
  return (
    <div className="space-y-8">
      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <MetricCard
          label="Growth Trajectory"
          value="Strong"
          subtitle="Above national average"
          change={{ value: 5.8, period: 'composite score' }}
          trend="up"
          status="good"
          icon="📈"
        />
        <MetricCard
          label="Innovation Index"
          value="8.7/10"
          subtitle="Top 5 globally"
          change={{ value: 0.4, period: 'YoY' }}
          trend="up"
          status="good"
          icon="💡"
        />
        <MetricCard
          label="Livability Score"
          value="92/100"
          subtitle="Excellent"
          change={{ value: 3, period: 'vs last year' }}
          trend="up"
          status="good"
          icon="🏆"
        />
      </div>

      {/* Comparative Analysis */}
      <div className="card p-6">
        <h3 className="text-lg font-semibold text-text-primary mb-4 flex items-center gap-2">
          <span>📊</span>
          Key Performance Indicators
        </h3>
        <div className="space-y-4">
          {[
            { category: 'Economic Performance', score: 88, rank: 'Top 10%', change: 'up' },
            { category: 'Quality of Life', score: 92, rank: 'Top 5%', change: 'up' },
            { category: 'Infrastructure', score: 85, rank: 'Top 15%', change: 'up' },
            { category: 'Innovation & Technology', score: 91, rank: 'Top 8%', change: 'up' },
            { category: 'Sustainability', score: 78, rank: 'Top 25%', change: 'up' },
            { category: 'Cultural Richness', score: 95, rank: 'Top 3%', change: 'stable' },
          ].map((kpi) => (
            <div key={kpi.category} className="flex items-center justify-between">
              <div className="flex-1">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-text-primary font-medium">{kpi.category}</span>
                  <div className="flex items-center gap-4">
                    <span className="text-sm text-text-secondary">{kpi.rank}</span>
                    <span className="text-primary font-semibold w-12 text-right">{kpi.score}</span>
                  </div>
                </div>
                <div className="h-2 bg-surface-border rounded-full overflow-hidden">
                  <div 
                    className={`h-full rounded-full ${
                      kpi.score >= 90 ? 'bg-success' : kpi.score >= 80 ? 'bg-primary' : 'bg-warning'
                    }`}
                    style={{ width: `${kpi.score}%` }}
                  />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Historical Trends"
          description="Multi-year performance across key metrics (indexed to 100)"
        >
          <LineChart
            data={growthTrendData}
            xKey="year"
            lines={[
              { dataKey: 'gdp', name: 'GDP Growth', color: '#10b981', strokeWidth: 3 },
              { dataKey: 'population', name: 'Population', color: '#6366f1', strokeWidth: 2 },
              { dataKey: 'employment', name: 'Employment', color: '#f59e0b', strokeWidth: 2 },
            ]}
            yAxisLabel="Index Value"
            height={280}
          />
        </ChartCard>

        <ChartCard
          title="Peer City Comparison"
          description="Livability scores vs similar metropolitan areas"
        >
          <BarChart
            data={peerComparisonData}
            xKey="city"
            bars={[
              { dataKey: 'score', name: 'Livability Score', color: '#8b5cf6' },
            ]}
            yAxisLabel="Score"
            height={280}
            customColors={['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981']}
          />
        </ChartCard>
      </div>

      {/* Predictions */}
      <div className="card p-6">
        <h3 className="text-lg font-semibold text-text-primary mb-4 flex items-center gap-2">
          <span>🔮</span>
          5-Year Projections
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[
            { metric: 'Population', current: '8.3M', projected: '9.1M', growth: '+9.6%' },
            { metric: 'GDP', current: '$715B', projected: '$892B', growth: '+24.8%' },
            { metric: 'Employment', current: '4.2M', projected: '4.6M', growth: '+9.5%' },
          ].map((proj) => (
            <div key={proj.metric} className="p-4 bg-surface-elevated rounded-lg text-center">
              <div className="text-text-tertiary text-sm mb-2">{proj.metric}</div>
              <div className="flex items-center justify-center gap-2 mb-2">
                <span className="text-text-secondary">{proj.current}</span>
                <span className="text-text-tertiary">→</span>
                <span className="text-text-primary font-bold">{proj.projected}</span>
              </div>
              <div className="text-success text-sm font-medium">{proj.growth}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
