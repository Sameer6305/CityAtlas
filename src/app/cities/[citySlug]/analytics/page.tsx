/**
 * Analytics Section Page
 * Route: /cities/[citySlug]/analytics
 * 
 * Enterprise analytics dashboard with reusable chart components
 */

'use client';

import { ChartCard } from '@/components';
import { AQITrendChart } from '@/components/charts/AQITrendChart';
import { JobSectorChart } from '@/components/charts/JobSectorChart';
import { CostOfLivingChart } from '@/components/charts/CostOfLivingChart';
import { PopulationChart } from '@/components/charts/PopulationChart';
import { 
  getAQITrendData, 
  getJobSectorData, 
  getCostOfLivingData, 
  getPopulationData 
} from '@/lib/mock';

export default function AnalyticsPage() {
  // Note: In production, city data would be fetched using params.citySlug
  // Example: const data = await fetch(`/api/cities/${params.citySlug}/analytics`)
  
  const aqiData = getAQITrendData();
  const jobData = getJobSectorData();
  const costData = getCostOfLivingData();
  const populationData = getPopulationData();

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="mb-6">
        <div className="inline-block px-3 py-1 glass-card text-cyan-300 rounded-full text-xs font-medium mb-3 border border-cyan-400/30">
          Analytics Dashboard
        </div>
        <h1 className="text-2xl md:text-3xl font-bold text-white mb-2">
          City Performance Metrics
        </h1>
        <p className="text-white/70 text-base">
          Comprehensive analytics across environment, economy, and demographics
        </p>
      </div>

      {/* Environmental Quality Section */}
      <div className="mb-4">
        <h2 className="text-xl font-bold text-white mb-2 flex items-center gap-2">
          🌤️ Environmental Quality
        </h2>
        <p className="text-white/70">Air quality index trends and historical data</p>
      </div>
      
      <ChartCard
        title="Air Quality Index (AQI) - 12 Month Trend"
        description="Lower values indicate better air quality. Good: 0-50 | Moderate: 51-100"
      >
        <AQITrendChart data={aqiData} showBenchmark />
      </ChartCard>

      {/* Economic Indicators Section */}
      <div className="mt-10 mb-4">
        <h2 className="text-xl font-bold text-white mb-2 flex items-center gap-2">
          💼 Economic Indicators
        </h2>
        <p className="text-white/70">Employment distribution and cost of living analysis</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <ChartCard
          title="Employment by Sector"
          description="Current workforce distribution across major industries"
        >
          <JobSectorChart data={jobData} />
        </ChartCard>

        <ChartCard
          title="Cost of Living Index"
          description="Comparison to national average (100 = national average)"
        >
          <CostOfLivingChart data={costData} showNationalAverage />
        </ChartCard>
      </div>

      {/* Demographics Section */}
      <div className="mt-12 mb-6">
        <h2 className="text-2xl font-bold text-text-primary mb-2 flex items-center gap-2">
          👥 Demographics
        </h2>
        <p className="text-text-secondary">Population growth trends and projections</p>
      </div>

      <ChartCard
        title="Population Growth (10-Year Trend)"
        description="Historical population in millions with year-over-year growth rates"
      >
        <PopulationChart data={populationData} />
      </ChartCard>

      {/* Data Insights Footer */}
      <div className="mt-12 p-6 bg-surface-elevated rounded-lg border border-surface-border">
        <h3 className="text-lg font-semibold text-text-primary mb-3 flex items-center gap-2">
          <span>💡</span>
          About This Data
        </h3>
        <div className="space-y-2 text-sm text-text-secondary">
          <p>
            <strong className="text-text-primary">Data Sources:</strong> Charts display real-time analytics from 
            environmental monitoring APIs, labor statistics, census data, and economic indices.
          </p>
          <p>
            <strong className="text-text-primary">Update Frequency:</strong> Environmental data: Daily | 
            Economic data: Monthly | Demographics: Quarterly
          </p>
          <p className="text-text-tertiary text-xs mt-4">
            Note: Currently displaying mock data for demonstration. Production version will connect to live APIs.
          </p>
        </div>
      </div>
    </div>
  );
}
