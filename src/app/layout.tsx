import type { Metadata } from "next";
import { Inter } from "next/font/google";
import { ThemeProvider } from "@/components/ThemeProvider";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "CityAtlas - City Intelligence Platform",
  description: "Dark-themed, event-driven city intelligence platform presenting cities as structured resumes with real-time data and AI insights",
  keywords: ["city data", "analytics", "city intelligence", "urban planning", "city comparison"],
  authors: [{ name: "CityAtlas" }],
  openGraph: {
    title: "CityAtlas - City Intelligence Platform",
    description: "Explore cities as structured resumes with real-time data and AI insights",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark" suppressHydrationWarning>
      <body className={inter.className}>
        {/* Theme Provider - Manages dark/light mode state */}
        <ThemeProvider>
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}


