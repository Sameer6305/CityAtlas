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

export default function AISummaryPage() {
  return (
    <div className="space-y-6">
      {/* AI Overview */}
      <div className="glass-card p-5 bg-gradient-to-br from-cyan-500/10 to-purple-500/10 border-cyan-400/30">
        <div className="flex items-start gap-4">
          <div className="text-4xl">🤖</div>
          <div>
            <h3 className="text-lg font-semibold text-white mb-2">AI-Generated City Profile</h3>
            <p className="text-white/80 leading-relaxed">
              This city demonstrates strong economic fundamentals with a thriving technology sector and robust job market. 
              The population shows steady growth with high quality of life indicators. Infrastructure investments are on track, 
              and sustainability initiatives are progressing well. The cultural scene is vibrant with world-class institutions. 
              Overall outlook is positive with strong growth trajectory expected through 2030.
            </p>
          </div>
        </div>
      </div>

      {/* Key Insights */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <div className="glass-card p-5">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <span>💪</span>
            Strengths
          </h3>
          <ul className="space-y-3">
            {[
              'Leading innovation hub with strong tech ecosystem',
              'World-class educational institutions and research',
              'Diverse and multicultural population',
              'Extensive public transit and infrastructure',
              'Robust economic growth and job creation',
            ].map((strength, i) => (
              <li key={i} className="flex items-start gap-3">
                <span className="text-success text-xl flex-shrink-0">✓</span>
                <span className="text-white/70">{strength}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="glass-card p-5">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <span>⚠️</span>
            Areas for Improvement
          </h3>
          <ul className="space-y-3">
            {[
              'Housing affordability remains a concern',
              'Traffic congestion during peak hours',
              'Air quality improvement needed in some areas',
              'Income inequality requires attention',
              'Climate adaptation infrastructure gaps',
            ].map((area, i) => (
              <li key={i} className="flex items-start gap-3">
                <span className="text-warning text-xl flex-shrink-0">!</span>
                <span className="text-white/70">{area}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* Recommendations */}
      <div className="glass-card p-5">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <span>💡</span>
          AI Recommendations
        </h3>
        <div className="space-y-4">
          {[
            {
              title: 'For Residents',
              icon: '🏠',
              items: ['Consider neighborhoods with growing transit access', 'Explore emerging cultural districts', 'Take advantage of education opportunities']
            },
            {
              title: 'For Businesses',
              icon: '💼',
              items: ['Strong talent pool in tech and finance', 'Growing startup ecosystem support', 'Expanding international market access']
            },
            {
              title: 'For Investors',
              icon: '📈',
              items: ['Real estate in redevelopment zones', 'Green technology initiatives', 'Infrastructure modernization projects']
            },
          ].map((rec) => (
            <div key={rec.title} className="p-4 bg-surface-elevated rounded-lg">
              <div className="flex items-center gap-2 mb-3">
                <span className="text-2xl">{rec.icon}</span>
                <h4 className="text-text-primary font-semibold">{rec.title}</h4>
              </div>
              <ul className="space-y-2 ml-10">
                {rec.items.map((item, i) => (
                  <li key={i} className="text-sm text-text-secondary flex items-start gap-2">
                    <span className="text-primary flex-shrink-0">→</span>
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>

      {/* Future Outlook */}
      <div className="card p-6">
        <h3 className="text-lg font-semibold text-text-primary mb-4 flex items-center gap-2">
          <span>🔮</span>
          Future Outlook (2025-2030)
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h4 className="text-text-primary font-medium mb-3">Expected Developments</h4>
            <ul className="space-y-2">
              {[
                'Major infrastructure projects completion',
                'Expansion of green spaces and sustainability',
                'New tech campuses and innovation districts',
                'Enhanced public transit connections',
              ].map((dev, i) => (
                <li key={i} className="text-sm text-text-secondary flex items-start gap-2">
                  <span className="text-success">▸</span>
                  {dev}
                </li>
              ))}
            </ul>
          </div>
          <div>
            <h4 className="text-text-primary font-medium mb-3">Potential Challenges</h4>
            <ul className="space-y-2">
              {[
                'Managing rapid population growth',
                'Balancing development with affordability',
                'Climate change adaptation measures',
                'Infrastructure capacity constraints',
              ].map((challenge, i) => (
                <li key={i} className="text-sm text-text-secondary flex items-start gap-2">
                  <span className="text-warning">▸</span>
                  {challenge}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      {/* Disclaimer */}
      <div className="text-center text-sm text-text-tertiary">
        <p>AI-generated insights based on available data. Updated quarterly. Last update: December 2025</p>
      </div>
    </div>
  );
}

export async function generateMetadata({ params }: AISummaryPageProps) {
  const cityName = params.citySlug.replace(/-/g, ' ');
  
  return {
    title: `${cityName} - AI Summary | CityAtlas`,
    description: `AI-generated insights and recommendations for ${cityName}.`,
  };
}
