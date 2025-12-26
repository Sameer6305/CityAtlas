/**
 * Environment Section Page
 * Route: /cities/[citySlug]/environment
 * 
 * Displays air quality, green space, sustainability, climate risk
 */

interface EnvironmentPageProps {
  params: {
    citySlug: string;
  };
}

export default function EnvironmentPage({ params }: EnvironmentPageProps) {
  const { citySlug } = params;

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-semibold text-text-primary mb-2">
          Environment
        </h2>
        <p className="text-text-secondary">
          Environmental metrics and sustainability for {citySlug.replace(/-/g, ' ')}
        </p>
      </div>

      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        {[
          { label: 'Air Quality (AQI)', value: '—', status: '—' },
          { label: 'Green Space', value: '—', unit: 'm²/capita' },
          { label: 'Renewable Energy', value: '—', unit: '%' },
          { label: 'Climate Risk', value: '—', unit: '/100' },
        ].map((metric) => (
          <div key={metric.label} className="card-metric">
            <div className="card-metric-label">{metric.label}</div>
            <div className="card-metric-value">{metric.value}</div>
            <div className="text-xs text-text-tertiary mt-1">
              {metric.unit || metric.status}
            </div>
          </div>
        ))}
      </div>

      <div className="card p-8">
        <p className="text-text-secondary text-center">
          Environmental data and sustainability metrics will be loaded here
        </p>
      </div>
    </div>
  );
}

export async function generateMetadata({ params }: EnvironmentPageProps) {
  const cityName = params.citySlug.replace(/-/g, ' ');
  
  return {
    title: `${cityName} - Environment | CityAtlas`,
    description: `Assess ${cityName}'s environmental quality, sustainability initiatives, and climate risks.`,
  };
}
