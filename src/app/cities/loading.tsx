/**
 * Cities List Loading State
 * Fast skeleton loading for better perceived performance
 */

export default function Loading() {
  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Background placeholder */}
      <div className="fixed inset-0 -z-10 bg-gradient-to-b from-[#0a0e27] via-[#0f1420] to-[#000000]" />
      
      <div className="flex">
        {/* Sidebar skeleton */}
        <div className="w-64 h-screen glass-nav p-4 space-y-4">
          <div className="skeleton h-12 w-full rounded-lg" />
          <div className="skeleton h-10 w-full rounded-lg" />
          <div className="skeleton h-10 w-full rounded-lg" />
          <div className="skeleton h-10 w-full rounded-lg" />
        </div>
        
        {/* Main content skeleton */}
        <div className="flex-1 p-8">
          {/* Header skeleton */}
          <div className="mb-8 text-center">
            <div className="skeleton h-8 w-32 mx-auto mb-4 rounded-full" />
            <div className="skeleton h-12 w-64 mx-auto mb-4 rounded-lg" />
            <div className="skeleton h-6 w-96 mx-auto rounded-lg" />
          </div>
          
          {/* Search skeleton */}
          <div className="max-w-3xl mx-auto mb-12">
            <div className="skeleton h-16 w-full rounded-2xl" />
          </div>
          
          {/* Cards skeleton - 4 column grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
            {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
              <div key={i} className="backdrop-blur-md bg-white/[0.03] border border-white/10 p-6 rounded-2xl animate-pulse">
                <div className="flex items-start justify-between mb-5">
                  <div className="w-14 h-14 rounded-2xl bg-white/10" />
                  <div className="w-8 h-8 rounded-lg bg-white/5" />
                </div>
                <div className="h-6 w-32 bg-white/10 rounded mb-2" />
                <div className="h-4 w-full bg-white/5 rounded mb-3" />
                <div className="flex items-center justify-between">
                  <div className="h-8 w-20 bg-white/5 rounded-lg" />
                  <div className="w-5 h-5 bg-white/10 rounded" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
