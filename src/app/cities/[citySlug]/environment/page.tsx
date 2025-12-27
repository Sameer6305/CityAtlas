/**
 * Environment Section Page
 * Route: /cities/[citySlug]/environment
 * 
 * Displays air quality, green space, sustainability, climate risk
 */

'use client';

import { MetricCard, ChartCard, LineChart } from '@/components';

// Sample AQI data
const aqiData = [
  { month: 'Jan', aqi: 52, benchmark: 50 },
  { month: 'Feb', aqi: 48, benchmark: 50 },
  { month: 'Mar', aqi: 45, benchmark: 50 },
  { month: 'Apr', aqi: 41, benchmark: 50 },
  { month: 'May', aqi: 38, benchmark: 50 },
  { month: 'Jun', aqi: 42, benchmark: 50 },
  { month: 'Jul', aqi: 47, benchmark: 50 },
  { month: 'Aug', aqi: 44, benchmark: 50 },
  { month: 'Sep', aqi: 40, benchmark: 50 },
  { month: 'Oct', aqi: 43, benchmark: 50 },
  { month: 'Nov', aqi: 46, benchmark: 50 },
  { month: 'Dec', aqi: 45, benchmark: 50 },
];

export default function EnvironmentPage() {
  return (
    <div className="space-y-6">
      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Air Quality Index"
          value="45"
          subtitle="Good"
          change={{ value: -8, period: 'vs last month' }}
          trend="down"
          status="good"
          icon="🌤️"
        />
        <MetricCard
          label="Green Space"
          value="28%"
          subtitle="2,800 hectares"
          change={{ value: 3.5, period: 'vs last year' }}
          trend="up"
          status="good"
          icon="🌳"
        />
        <MetricCard
          label="Recycling Rate"
          value="62%"
          change={{ value: 7.2, period: 'YoY' }}
          trend="up"
          status="good"
          icon="♻️"
        />
        <MetricCard
          label="Renewable Energy"
          value="35%"
          subtitle="Of total consumption"
          change={{ value: 12, period: 'YoY' }}
          trend="up"
          status="good"
          icon="⚡"
        />
      </div>

      {/* Sustainability Initiatives */}
      <div className="glass-card p-5">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <span>🌱</span>
          Active Sustainability Programs
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[
            { title: 'Carbon Neutral 2030', progress: 68, description: 'City-wide emissions reduction initiative' },
            { title: 'Urban Greening Project', progress: 82, description: 'Expanding parks and green corridors' },
            { title: 'Clean Energy Transition', progress: 45, description: 'Converting to renewable sources' },
            { title: 'Zero Waste Program', progress: 58, description: 'Municipal waste reduction target' },
          ].map((program) => (
            <div key={program.title} className="p-4 bg-surface-elevated rounded-lg">
              <div className="flex items-center justify-between mb-2">
                <span className="text-text-primary font-medium">{program.title}</span>
                <span className="text-primary font-semibold">{program.progress}%</span>
              </div>
              <p className="text-sm text-text-secondary mb-3">{program.description}</p>
              <div className="h-2 bg-surface-border rounded-full overflow-hidden">
                <div 
                  className="h-full bg-success rounded-full"
                  style={{ width: `${program.progress}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Air Quality Trends"
          description="AQI measurements over the past 12 months"
        >
          <LineChart
            data={aqiData}
            xKey="month"
            lines={[
              { dataKey: 'aqi', name: 'AQI', color: '#10b981', strokeWidth: 3 },
              { dataKey: 'benchmark', name: 'Target (Good)', color: '#6366f1', strokeWidth: 2 },
            ]}
            yAxisLabel="AQI Value"
            height={280}
          />
        </ChartCard>

        <ChartCard
          title="Energy Mix"
          description="Renewable vs traditional energy sources"
        >
          <div className="h-64 flex items-center justify-center text-text-tertiary">
            <div className="text-center">
              <div className="text-5xl mb-4">⚡</div>
              <p>Energy distribution chart</p>
              <p className="text-sm mt-2">Chart integration pending</p>
            </div>
          </div>
        </ChartCard>
      </div>
    </div>
  );
}
