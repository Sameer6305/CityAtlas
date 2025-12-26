/**
 * Education Section Page
 * Route: /cities/[citySlug]/education
 * 
 * Displays universities, literacy rates, STEM pipeline, talent retention
 */

interface EducationPageProps {
  params: {
    citySlug: string;
  };
}

export default function EducationPage({ params }: EducationPageProps) {
  const { citySlug } = params;

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-semibold text-text-primary mb-2">
          Education
        </h2>
        <p className="text-text-secondary">
          Academic institutions and education metrics for {citySlug.replace(/-/g, ' ')}
        </p>
      </div>

      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        {[
          { label: 'Universities', value: '—' },
          { label: 'Literacy Rate', value: '—', unit: '%' },
          { label: 'STEM Graduates', value: '—', unit: '/year' },
          { label: 'Retention Rate', value: '—', unit: '%' },
        ].map((metric) => (
          <div key={metric.label} className="card-metric">
            <div className="card-metric-label">{metric.label}</div>
            <div className="card-metric-value">{metric.value}</div>
            {metric.unit && (
              <div className="text-xs text-text-tertiary mt-1">{metric.unit}</div>
            )}
          </div>
        ))}
      </div>

      <div className="card p-8">
        <p className="text-text-secondary text-center">
          Education data and university rankings will be loaded here
        </p>
      </div>
    </div>
  );
}

export async function generateMetadata({ params }: EducationPageProps) {
  const cityName = params.citySlug.replace(/-/g, ' ');
  
  return {
    title: `${cityName} - Education | CityAtlas`,
    description: `Review ${cityName}'s educational institutions, literacy rates, and STEM talent pipeline.`,
  };
}
