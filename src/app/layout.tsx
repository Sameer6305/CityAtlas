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
  metadataBase: new URL(process.env.NEXT_PUBLIC_APP_URL || 'http://localhost:3000'),
  title: "CityAtlas - Intelligence in Urban Flow",
  description: "Dark-themed, event-driven city intelligence platform presenting cities as structured resumes with real-time data and AI insights",
  keywords: ["city data", "analytics", "city intelligence", "urban planning", "city comparison"],
  authors: [{ name: "CityAtlas" }],
  icons: {
    icon: "/logo.png",
    apple: "/logo.png",
  },
  openGraph: {
    title: "CityAtlas - Intelligence in Urban Flow",
    description: "Explore cities as structured resumes with real-time data and AI insights",
    type: "website",
    images: ["/logo.png"],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark" suppressHydrationWarning>
      <body className={`${inter.className} bg-background text-text-primary min-h-screen`}>
        {/* Theme Provider - Manages dark/light mode state */}
        <ThemeProvider>
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}


