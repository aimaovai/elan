"use client";

import { usePathname } from "next/navigation";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { SiteFooter } from "@/components/layout/SiteFooter";

export function AppChrome() {
  const pathname = usePathname();
  if (pathname.startsWith("/admin")) return null;
  return <><SiteHeader /><SiteFooter /></>;
}
