/**
 * Infrastructure Section Page
 * Route: /cities/[citySlug]/infrastructure
 * 
 * Displays transportation, utilities, internet, housing data
 */

import { MetricCard, ChartCard } from '@/components';

interface InfrastructurePageProps {
  params: {
    citySlug: string;
  };
}

export default function InfrastructurePage() {
  return (
    <div className="space-y-6">
      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Public Transit Score"
          value="8.5/10"
          change={{ value: 0.3, period: 'vs last year' }}
          trend="up"
          status="good"
          icon="🚇"
        />
        <MetricCard
          label="Internet Speed"
          value="450 Mbps"
          change={{ value: 12.5, period: 'YoY' }}
          trend="up"
          status="good"
          icon="📡"
        />
        <MetricCard
          label="Power Reliability"
          value="99.8%"
          change={{ value: 0.1, period: 'vs last year' }}
          trend="up"
          status="good"
          icon="⚡"
        />
        <MetricCard
          label="Water Quality"
          value="95/100"
          change={{ value: 2, period: 'vs last year' }}
          trend="up"
          status="good"
          icon="💧"
        />
      </div>

      {/* Transportation Overview */}
      <div className="glass-card p-5">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <span>🚊</span>
          Transportation Network
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          <div className="text-center p-4 bg-white/5 rounded-lg border border-white/10">
            <div className="text-3xl mb-2">🚇</div>
            <div className="text-2xl font-bold text-white mb-1">12</div>
            <div className="text-sm text-white/60">Metro Lines</div>
          </div>
          <div className="text-center p-4 bg-white/5 rounded-lg border border-white/10">
            <div className="text-3xl mb-2">🚌</div>
            <div className="text-2xl font-bold text-white mb-1">145</div>
            <div className="text-sm text-white/60">Bus Routes</div>
          </div>
          <div className="text-center p-4 bg-white/5 rounded-lg border border-white/10">
            <div className="text-3xl mb-2">🚴</div>
            <div className="text-2xl font-bold text-white mb-1">320</div>
            <div className="text-sm text-white/60">Bike Stations</div>
          </div>
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Commute Times"
          description="Average commute by transport mode"
        >
          <div className="h-64 flex items-center justify-center text-text-tertiary">
            <div className="text-center">
              <div className="text-5xl mb-4">⏱️</div>
              <p>Commute time analysis</p>
              <p className="text-sm mt-2">Chart integration pending</p>
            </div>
          </div>
        </ChartCard>

        <ChartCard
          title="Utility Coverage"
          description="Infrastructure coverage by area"
        >
          <div className="h-64 flex items-center justify-center text-text-tertiary">
            <div className="text-center">
              <div className="text-5xl mb-4">🗺️</div>
              <p>Coverage map</p>
              <p className="text-sm mt-2">Chart integration pending</p>
            </div>
          </div>
        </ChartCard>
      </div>
    </div>
  );
}

export async function generateMetadata({ params }: InfrastructurePageProps) {
  const cityName = params.citySlug.replace(/-/g, ' ');
  
  return {
    title: `${cityName} - Infrastructure | CityAtlas`,
    description: `Explore ${cityName}'s infrastructure including transportation, utilities, internet, and housing.`,
  };
}
