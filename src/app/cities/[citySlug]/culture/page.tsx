/**
 * Culture Section Page
 * Route: /cities/[citySlug]/culture
 * 
 * Displays arts scene, diversity, expat community, quality of life
 */

import { MetricCard, ChartCard } from '@/components';

interface CulturePageProps {
  params: {
    citySlug: string;
  };
}

export default function CulturePage() {
  return (
    <div className="space-y-6">
      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Museums & Galleries"
          value="125"
          subtitle="World-class collections"
          icon="🎨"
        />
        <MetricCard
          label="Theaters & Venues"
          value="78"
          subtitle="Including 15 major theaters"
          icon="🎭"
        />
        <MetricCard
          label="Restaurants"
          value="12,500+"
          subtitle="65 Michelin-starred"
          icon="🍽️"
        />
        <MetricCard
          label="Languages Spoken"
          value="800+"
          subtitle="Most diverse globally"
          icon="🌍"
        />
      </div>

      {/* Cultural Highlights */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <div className="glass-card p-5">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <span>🎪</span>
            Major Annual Events
          </h3>
          <div className="space-y-3">
            {[
              { name: 'City Arts Festival', attendance: '2M visitors', period: 'June-July' },
              { name: 'International Food Fair', attendance: '500K visitors', period: 'September' },
              { name: 'Music & Culture Week', attendance: '350K visitors', period: 'March' },
              { name: 'Film & Media Festival', attendance: '200K visitors', period: 'November' },
            ].map((event) => (
              <div key={event.name} className="flex items-center justify-between p-3 bg-white/5 rounded-lg border border-white/10">
                <div>
                  <div className="text-white font-medium">{event.name}</div>
                  <div className="text-sm text-white/50">{event.period}</div>
                </div>
                <div className="text-sm text-white/70">{event.attendance}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-card p-5">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <span>🏛️</span>
            Notable Cultural Sites
          </h3>
          <div className="space-y-3">
            {[
              { name: 'City Museum of Art', visitors: '3.5M/year', rating: '4.8/5' },
              { name: 'Historic Opera House', visitors: '850K/year', rating: '4.9/5' },
              { name: 'Contemporary Art Center', visitors: '1.2M/year', rating: '4.7/5' },
              { name: 'Cultural Heritage District', visitors: '2M/year', rating: '4.8/5' },
            ].map((site) => (
              <div key={site.name} className="flex items-center justify-between p-3 bg-surface-elevated rounded-lg">
                <div>
                  <div className="text-text-primary font-medium">{site.name}</div>
                  <div className="text-sm text-text-tertiary">{site.visitors}</div>
                </div>
                <div className="text-sm text-warning">⭐ {site.rating}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Charts */}
      <ChartCard
        title="Cultural Diversity Index"
        description="Demographic and linguistic diversity metrics"
      >
        <div className="h-64 flex items-center justify-center text-text-tertiary">
          <div className="text-center">
            <div className="text-5xl mb-4">🌈</div>
            <p>Diversity metrics visualization</p>
            <p className="text-sm mt-2">Chart integration pending</p>
          </div>
        </div>
      </ChartCard>
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
