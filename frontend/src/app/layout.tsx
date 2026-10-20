import type { Metadata } from "next";
import { Cormorant_Garamond, Manrope } from "next/font/google";
import "./globals.css";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { SiteFooter } from "@/components/layout/SiteFooter";

const serif = Cormorant_Garamond({ subsets: ["latin"], variable: "--font-editorial", weight: ["400", "500", "600"] });
const sans = Manrope({ subsets: ["latin"], variable: "--font-sans", weight: ["400", "500", "600", "700"] });
const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: { default: "ÉLAN — A seasonal expression", template: "%s | ÉLAN" },
  description: "ÉLAN is a fictional fine-dining restaurant demonstration exploring seasonality, craft, and thoughtful hospitality.",
  openGraph: {
    title: "ÉLAN — A seasonal expression",
    description: "A fictional contemporary dining experience, shaped by the seasons.",
    type: "website",
    siteName: "ÉLAN",
  },
  twitter: { card: "summary_large_image", title: "ÉLAN — A seasonal expression" },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body className={`${serif.variable} ${sans.variable}`}>
        <a className="skip-link" href="#main-content">Skip to content</a>
        <SiteHeader />
        <main id="main-content">{children}</main>
        <SiteFooter />
      </body>
    </html>
  );
}
