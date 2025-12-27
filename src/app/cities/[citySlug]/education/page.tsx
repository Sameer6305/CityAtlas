/**
 * Education Section Page
 * Route: /cities/[citySlug]/education
 * 
 * Displays universities, literacy rates, STEM pipeline, talent retention
 */

import { MetricCard, ChartCard } from '@/components';

interface EducationPageProps {
  params: {
    citySlug: string;
  };
}

export default function EducationPage() {
  return (
    <div className="space-y-6">
      {/* KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Literacy Rate"
          value="98.5%"
          change={{ value: 0.8, period: 'vs last year' }}
          trend="up"
          status="good"
          icon="📚"
        />
        <MetricCard
          label="Universities"
          value="45"
          subtitle="12 top-ranked globally"
          icon="🎓"
        />
        <MetricCard
          label="Student-Teacher Ratio"
          value="14:1"
          change={{ value: -1, period: 'vs last year' }}
          trend="down"
          status="good"
          icon="👨‍🏫"
        />
        <MetricCard
          label="Research Output"
          value="8,500"
          subtitle="Publications per year"
          change={{ value: 12.3, period: 'YoY' }}
          trend="up"
          status="good"
          icon="🔬"
        />
      </div>

      {/* Top Universities */}
      <div className="glass-card p-5">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <span>🏛️</span>
          Top Universities
        </h3>
        <div className="space-y-3">
          {[
            { name: 'City Technical University', ranking: 'Top 50 Global', students: '45K', research: 'High' },
            { name: 'Metropolitan State University', ranking: 'Top 100 Global', students: '38K', research: 'High' },
            { name: 'Downtown Business School', ranking: 'Top 20 Business', students: '12K', research: 'Medium' },
            { name: 'City Medical College', ranking: 'Top 30 Medical', students: '8K', research: 'High' },
          ].map((uni) => (
            <div key={uni.name} className="flex items-center justify-between p-4 bg-white/5 rounded-lg border border-white/10 hover:bg-white/10 transition-smooth">
              <div>
                <div className="text-white font-medium mb-1">{uni.name}</div>
                <div className="text-sm text-white/60">{uni.ranking}</div>
              </div>
              <div className="text-right">
                <div className="text-white text-sm">{uni.students} students</div>
                <div className="text-xs text-white/50">Research: {uni.research}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Enrollment Trends"
          description="Student enrollment over time"
        >
          <div className="h-64 flex items-center justify-center text-text-tertiary">
            <div className="text-center">
              <div className="text-5xl mb-4">📈</div>
              <p>Enrollment growth chart</p>
              <p className="text-sm mt-2">Chart integration pending</p>
            </div>
          </div>
        </ChartCard>

        <ChartCard
          title="Education Spending"
          description="Investment in education sector"
        >
          <div className="h-64 flex items-center justify-center text-text-tertiary">
            <div className="text-center">
              <div className="text-5xl mb-4">💵</div>
              <p>Spending analysis</p>
              <p className="text-sm mt-2">Chart integration pending</p>
            </div>
          </div>
        </ChartCard>
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
