import Link from 'next/link';

/**
 * Global 404 Page
 */
export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#0a0e1a]">
      <div className="text-center space-y-6 max-w-md px-6">
        <div className="text-7xl font-bold text-white/10">404</div>
        <h2 className="text-2xl font-bold text-white">Page not found</h2>
        <p className="text-white/50 text-sm">
          The page you&apos;re looking for doesn&apos;t exist.
        </p>
        <Link
          href="/"
          className="inline-block px-6 py-3 bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-400 rounded-xl border border-cyan-500/30 transition-all duration-300 font-medium"
        >
          Go Home
        </Link>
      </div>
    </div>
  );
}
