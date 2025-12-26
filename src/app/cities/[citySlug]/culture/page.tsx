/**
 * Culture Section Page
 * Route: /cities/[citySlug]/culture
 * 
 * Displays arts scene, diversity, expat community, quality of life
 */

interface CulturePageProps {
  params: {
    citySlug: string;
  };
}

export default function CulturePage({ params }: CulturePageProps) {
  const { citySlug } = params;

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-semibold text-text-primary mb-2">
          Culture
        </h2>
        <p className="text-text-secondary">
          Cultural amenities and quality of life in {citySlug.replace(/-/g, ' ')}
        </p>
      </div>

      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        {[
          { label: 'Museums & Venues', value: '—', unit: '/100k' },
          { label: 'Diversity Index', value: '—', unit: '/100' },
          { label: 'Expat Population', value: '—', unit: '%' },
          { label: 'Safety Index', value: '—', unit: '/100' },
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
          Cultural insights and community data will be loaded here
        </p>
      </div>
    </div>
  );
}

export async function generateMetadata({ params }: CulturePageProps) {
  const cityName = params.citySlug.replace(/-/g, ' ');
  
  return {
    title: `${cityName} - Culture | CityAtlas`,
    description: `Discover ${cityName}'s cultural scene, diversity, expat community, and quality of life.`,
  };
}
