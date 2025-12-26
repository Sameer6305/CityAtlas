/**
 * Economy Section Page
 * Route: /cities/[citySlug]/economy
 * 
 * Displays job market, industries, income distribution, startup ecosystem
 */

interface EconomyPageProps {
  params: {
    citySlug: string;
  };
}

export default function EconomyPage({ params }: EconomyPageProps) {
  const { citySlug } = params;

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-semibold text-text-primary mb-2">
          Economy
        </h2>
        <p className="text-text-secondary">
          Economic indicators and job market data for {citySlug.replace(/-/g, ' ')}
        </p>
      </div>

      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        {[
          { label: 'Unemployment Rate', value: '—', change: '—' },
          { label: 'Median Income', value: '—', change: '—' },
          { label: 'Top Industry', value: '—', change: '—' },
          { label: 'Startup Density', value: '—', change: '—' },
        ].map((metric) => (
          <div key={metric.label} className="card-metric">
            <div className="card-metric-label">{metric.label}</div>
            <div className="card-metric-value">{metric.value}</div>
            <div className="card-metric-change">{metric.change}</div>
          </div>
        ))}
      </div>

      {/* Placeholder for charts and detailed data */}
      <div className="card p-8">
        <p className="text-text-secondary text-center">
          Economy charts and analytics will be loaded here
        </p>
      </div>
    </div>
  );
}

export async function generateMetadata({ params }: EconomyPageProps) {
  const cityName = params.citySlug.replace(/-/g, ' ');
  
  return {
    title: `${cityName} - Economy | CityAtlas`,
    description: `Analyze ${cityName}'s economic indicators, job market, industries, and startup ecosystem.`,
  };
}
