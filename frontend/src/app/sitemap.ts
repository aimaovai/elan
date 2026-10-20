import type { MetadataRoute } from "next";

export default function sitemap(): MetadataRoute.Sitemap {
  const base = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
  const routes = ["/", "/menus", "/experiences", "/about", "/gallery", "/private-dining", "/reservations", "/contact"];
  return routes.map((route) => ({ url: `${base}${route}`, lastModified: new Date(), changeFrequency: "monthly", priority: route === "/" ? 1 : 0.7 }));
}
