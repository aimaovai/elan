import type { Metadata } from "next";
import { PageHero } from "@/components/layout/PageHero";
import { ContentState } from "@/components/ui/ContentState";
import { MenuExplorer } from "@/components/menus/MenuExplorer";
import { getMenus } from "@/lib/api/client";

export const metadata: Metadata = {
  title: "Menus",
  description: "Explore Élan's seasonal dinner, tasting, cocktail, wine, and dessert menus.",
  alternates: { canonical: "/menus" },
};

export default async function MenusPage() {
  const result = await getMenus();
  return (
    <>
      <PageHero eyebrow="THE KITCHEN, IN SEASON" title="Menus for the moment." description="Thoughtfully sourced, shaped by the season, and made to be shared around the table." compact />
      {result.data ? result.data.length
        ? <MenuExplorer menus={result.data} />
        : <div className="page-container menu-explorer"><ContentState kind="empty" message="Our menus are being refreshed. Please return soon." /></div>
        : <div className="page-container menu-explorer"><ContentState message={result.error} /></div>}
    </>
  );
}
