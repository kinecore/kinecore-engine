import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Sidebar } from "@/components/Sidebar";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "KineCore | Simulation Hub",
  description: "Enterprise System Dynamics Forecasting Engine",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={`${inter.className} min-h-screen flex bg-[#020205]`}>
        <Sidebar />
        <main className="flex-1 overflow-y-auto px-8 py-8">
          {children}
        </main>
      </body>
    </html>
  );
}
