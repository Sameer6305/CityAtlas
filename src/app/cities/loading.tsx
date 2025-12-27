/**
 * Cities List Loading State
 */

export default function Loading() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="space-y-6">
        {/* Header Skeleton */}
        <div className="space-y-3">
          <div className="skeleton h-8 w-48" />
          <div className="skeleton h-4 w-96" />
        </div>

        {/* Cards Skeleton */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="glass-card p-6 rounded-xl">
              <div className="skeleton h-12 w-12 rounded-lg mb-4" />
              <div className="skeleton h-6 w-32 mb-2" />
              <div className="skeleton h-4 w-24 mb-4" />
              <div className="skeleton h-4 w-full" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
