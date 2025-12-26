/**
 * Analytics Section Page
 * Route: /cities/[citySlug]/analytics
 * 
 * Displays trend graphs, forecasts, peer city comparisons
 */

interface AnalyticsPageProps {
  params: {
    citySlug: string;
  };
}

export default function AnalyticsPage({ params }: AnalyticsPageProps) {
  const { citySlug } = params;

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-semibold text-text-primary mb-2">
          Analytics
        </h2>
        <p className="text-text-secondary">
          Trend analysis and comparative insights for {citySlug.replace(/-/g, ' ')}
        </p>
      </div>

      {/* Chart Placeholders */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            Population Growth Trend
          </h3>
          <div className="h-64 flex items-center justify-center bg-surface-elevated rounded">
            <p className="text-text-tertiary">Chart will render here</p>
          </div>
        </div>

        <div className="card p-6">
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            GDP vs. Peer Cities
          </h3>
          <div className="h-64 flex items-center justify-center bg-surface-elevated rounded">
            <p className="text-text-tertiary">Chart will render here</p>
          </div>
        </div>

        <div className="card p-6">
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            Job Market by Sector
          </h3>
          <div className="h-64 flex items-center justify-center bg-surface-elevated rounded">
            <p className="text-text-tertiary">Chart will render here</p>
          </div>
        </div>

        <div className="card p-6">
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            Cost of Living Trajectory
          </h3>
          <div className="h-64 flex items-center justify-center bg-surface-elevated rounded">
            <p className="text-text-tertiary">Chart will render here</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export async function generateMetadata({ params }: AnalyticsPageProps) {
  const cityName = params.citySlug.replace(/-/g, ' ');
  
  return {
    title: `${cityName} - Analytics | CityAtlas`,
    description: `View ${cityName}'s trend analysis, forecasts, and peer city comparisons.`,
  };
}
