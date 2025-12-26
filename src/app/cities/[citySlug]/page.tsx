/**
 * City Overview Page (Default)
 * Route: /cities/[citySlug]
 * 
 * This is the default page when accessing a city.
 * Shows high-level city information and key metrics.
 */

import { MetricCard, ChartCard } from '@/components';

interface CityOverviewPageProps {
  params: {
    citySlug: string;
  };
}

export default function CityOverviewPage() {
  return (
    <div className="space-y-8">
      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
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
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Population Growth Trend"
          description="Historical population data over the last 10 years"
        >
          <div className="h-64 flex items-center justify-center text-text-tertiary">
            <div className="text-center">
              <div className="text-5xl mb-4">📈</div>
              <p>Population growth chart</p>
              <p className="text-sm mt-2">Chart integration pending</p>
            </div>
          </div>
        </ChartCard>

        <ChartCard
          title="Economic Indicators"
          description="Key economic metrics and trends"
        >
          <div className="h-64 flex items-center justify-center text-text-tertiary">
            <div className="text-center">
              <div className="text-5xl mb-4">💹</div>
              <p>Economic indicators chart</p>
              <p className="text-sm mt-2">Chart integration pending</p>
            </div>
          </div>
        </ChartCard>
      </div>

      {/* City Highlights */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="card p-6">
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

/**
 * Generate metadata for SEO
 */
export async function generateMetadata({ params }: CityOverviewPageProps) {
  const cityName = params.citySlug.replace(/-/g, ' ');
  
  return {
    title: `${cityName} - Overview | CityAtlas`,
    description: `Explore ${cityName}'s city profile including population, economy, infrastructure, and more.`,
  };
}

