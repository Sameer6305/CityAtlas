/**
 * AppShell Component
 * 
 * Main application layout wrapper that provides consistent structure
 * across all pages. Includes sidebar and main content area.
 * 
 * Usage:
 * <AppShell>
 *   <YourPageContent />
 * </AppShell>
 */

'use client';

import { ReactNode } from 'react';
import { Sidebar } from './Sidebar';

interface AppShellProps {
  children: ReactNode;
  showSidebar?: boolean;
}

export function AppShell({ children, showSidebar = true }: AppShellProps) {
  return (
    <div className="min-h-screen bg-background flex">
      {/* Sidebar */}
      {showSidebar && <Sidebar />}
      
      {/* Main Content Area */}
      <main className={`flex-1 ${showSidebar ? 'ml-0 lg:ml-64' : ''}`}>
        {children}
      </main>
    </div>
  );
}
