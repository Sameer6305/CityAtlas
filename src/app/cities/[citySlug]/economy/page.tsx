/**
 * Economy Section Page
 * Route: /cities/[citySlug]/economy
 * 
 * Displays job market, industries, income distribution, startup ecosystem
 */

'use client';

import { MetricCard, ChartCard, PieChart } from '@/components';

// Sample data for job sector distribution
const jobSectorData = [
  { name: 'Technology', value: 28 },
  { name: 'Finance', value: 22 },
  { name: 'Healthcare', value: 18 },
  { name: 'Education', value: 12 },
  { name: 'Retail', value: 10 },
  { name: 'Other', value: 10 },
];

const sectorColors = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#6b7280'];

export default function EconomyPage() {
  return (
    <div className="space-y-6">
      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Unemployment Rate"
          value="3.8%"
          change={{ value: -0.5, period: 'vs last quarter' }}
          trend="down"
          status="good"
          icon="💼"
        />
        <MetricCard
          label="Median Income"
          value="$92K"
          change={{ value: 5.2, period: 'YoY' }}
          trend="up"
          status="good"
          icon="💰"
        />
        <MetricCard
          label="Job Growth Rate"
          value="4.1%"
          change={{ value: 1.3, period: 'YoY' }}
          trend="up"
          status="good"
          icon="📈"
        />
        <MetricCard
          label="Startup Density"
          value="12.5/10K"
          change={{ value: 8.7, period: 'YoY' }}
          trend="up"
          status="good"
          icon="🚀"
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <ChartCard
          title="Industry Distribution"
          description="Employment breakdown by sector"
        >
          <PieChart
            data={jobSectorData}
            colors={sectorColors}
            height={320}
            innerRadius={60}
            showLabels={false}
          />
        </ChartCard>

        <ChartCard
          title="Income Distribution"
          description="Household income levels"
        >
          <div className="h-64 flex items-center justify-center text-text-tertiary">
            <div className="text-center">
              <div className="text-5xl mb-4">💵</div>
              <p>Income distribution chart</p>
              <p className="text-sm mt-2">Chart integration pending</p>
            </div>
          </div>
        </ChartCard>
      </div>

      {/* Top Industries */}
      <div className="card p-6">
        <h3 className="text-lg font-semibold text-text-primary mb-4 flex items-center gap-2">
          <span>🏢</span>
          Top Industries
        </h3>
        <div className="space-y-4">
          {[
            { name: 'Technology', percentage: 32, employees: '125K', growth: 8.5 },
            { name: 'Finance', percentage: 24, employees: '95K', growth: 3.2 },
            { name: 'Healthcare', percentage: 18, employees: '70K', growth: 5.8 },
            { name: 'Professional Services', percentage: 14, employees: '55K', growth: 4.1 },
            { name: 'Retail & Hospitality', percentage: 12, employees: '48K', growth: 2.3 },
          ].map((industry) => (
            <div key={industry.name} className="flex items-center justify-between">
              <div className="flex-1">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-text-primary font-medium">{industry.name}</span>
                  <span className="text-text-secondary text-sm">{industry.percentage}% | {industry.employees} employees</span>
                </div>
                <div className="h-2 bg-surface-border rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-primary rounded-full"
                    style={{ width: `${industry.percentage}%` }}
                  />
                </div>
              </div>
              <div className="ml-4 text-sm">
                <span className={industry.growth > 0 ? 'text-success' : 'text-danger'}>
                  {industry.growth > 0 ? '↑' : '↓'} {Math.abs(industry.growth)}%
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
