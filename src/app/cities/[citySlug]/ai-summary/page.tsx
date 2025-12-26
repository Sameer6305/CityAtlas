/**
 * AI Summary Section Page
 * Route: /cities/[citySlug]/ai-summary
 * 
 * LLM-generated city insights, strengths, weaknesses, recommendations
 */

interface AISummaryPageProps {
  params: {
    citySlug: string;
  };
}

export default function AISummaryPage({ params }: AISummaryPageProps) {
  const { citySlug } = params;
  const cityName = citySlug.replace(/-/g, ' ');

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-semibold text-text-primary mb-2">
          AI Summary
        </h2>
        <p className="text-text-secondary">
          LLM-generated insights and analysis for {cityName}
        </p>
      </div>

      {/* AI Summary Card */}
      <div className="card p-8" style={{
        background: 'linear-gradient(135deg, #1a1a24 0%, #2a1a3a 100%)',
        border: '1px solid var(--color-accent)'
      }}>
        <div className="flex items-center gap-3 mb-6">
          <div className="text-2xl">🤖</div>
          <div>
            <h3 className="text-lg font-semibold text-text-primary">
              AI City Summary
            </h3>
            <p className="text-sm text-text-tertiary">
              Generated analysis based on city metrics
            </p>
          </div>
        </div>

        {/* One-liner */}
        <div className="mb-6">
          <div className="text-base text-text-primary leading-relaxed italic">
            &ldquo;Summary will be generated here based on city data&rdquo;
          </div>
        </div>

        {/* Best For */}
        <div className="mb-6">
          <h4 className="text-sm font-semibold text-success mb-2">
            ✅ Best for:
          </h4>
          <p className="text-text-secondary">
            Target demographics will be identified here
          </p>
        </div>

        {/* Strengths */}
        <div className="mb-6">
          <h4 className="text-sm font-semibold text-success mb-2">
            💪 Strengths:
          </h4>
          <ul className="list-disc list-inside text-text-secondary space-y-1">
            <li>Strength analysis will appear here</li>
          </ul>
        </div>

        {/* Weaknesses */}
        <div className="mb-6">
          <h4 className="text-sm font-semibold text-warning mb-2">
            ⚠️ Weaknesses:
          </h4>
          <ul className="list-disc list-inside text-text-secondary space-y-1">
            <li>Weakness analysis will appear here</li>
          </ul>
        </div>

        {/* Risk Factors */}
        <div>
          <h4 className="text-sm font-semibold text-danger mb-2">
            🚨 Risk Factors:
          </h4>
          <ul className="list-disc list-inside text-text-secondary space-y-1">
            <li>Risk assessment will appear here</li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div className="mt-8 flex gap-4">
          <button className="px-4 py-2 bg-primary text-white rounded-md text-sm hover:bg-primary-hover transition-colors">
            Regenerate Summary
          </button>
          <button className="px-4 py-2 border border-surface-border text-text-secondary rounded-md text-sm hover:bg-surface-elevated transition-colors">
            Export PDF
          </button>
        </div>
      </div>

      {/* Methodology */}
      <div className="card p-6">
        <h3 className="text-lg font-semibold text-text-primary mb-3">
          How This Works
        </h3>
        <p className="text-text-secondary text-sm leading-relaxed">
          Our AI analyzes city metrics across economy, infrastructure, education, 
          culture, and environment to generate insights. The summary is created using 
          rule-based analysis combined with LLM-powered natural language generation.
        </p>
      </div>
    </div>
  );
}

export async function generateMetadata({ params }: AISummaryPageProps) {
  const cityName = params.citySlug.replace(/-/g, ' ');
  
  return {
    title: `${cityName} - AI Summary | CityAtlas`,
    description: `AI-generated insights, strengths, weaknesses, and recommendations for ${cityName}.`,
  };
}
