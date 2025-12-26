/**
 * Infrastructure Section Page
 * Route: /cities/[citySlug]/infrastructure
 * 
 * Displays transportation, utilities, internet, housing data
 */

interface InfrastructurePageProps {
  params: {
    citySlug: string;
  };
}

export default function InfrastructurePage({ params }: InfrastructurePageProps) {
  const { citySlug } = params;

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-semibold text-text-primary mb-2">
          Infrastructure
        </h2>
        <p className="text-text-secondary">
          Transportation, connectivity, and housing data for {citySlug.replace(/-/g, ' ')}
        </p>
      </div>

      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        {[
          { label: 'Transit Coverage', value: '—', unit: '%' },
          { label: 'Internet Speed', value: '—', unit: 'Mbps' },
          { label: 'Housing Vacancy', value: '—', unit: '%' },
          { label: 'Road Quality', value: '—', unit: '/100' },
        ].map((metric) => (
          <div key={metric.label} className="card-metric">
            <div className="card-metric-label">{metric.label}</div>
            <div className="card-metric-value">{metric.value}</div>
            <div className="text-xs text-text-tertiary mt-1">{metric.unit}</div>
          </div>
        ))}
      </div>

      <div className="card p-8">
        <p className="text-text-secondary text-center">
          Infrastructure metrics and visualizations will be loaded here
        </p>
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
