import type { Metadata } from "next";
import { notFound } from "next/navigation";
import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import { ContentState } from "@/components/ui/ContentState";
import { getMenu } from "@/lib/api/client";
import { money } from "@/lib/format";

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const result = await getMenu(slug);
  return {
    title: result.data?.name ?? "Menu",
    description: result.data?.description ?? "Explore an Élan seasonal menu.",
    alternates: { canonical: `/menus/${slug}` },
  };
}

export default async function MenuDetailPage({ params }: Props) {
  const { slug } = await params;
  const result = await getMenu(slug);
  if (result.error?.includes("404")) notFound();
  if (!result.data) {
    return <><PageHero eyebrow="THE ÉLAN MENUS" title="Menu details." compact /><div className="page-container menu-detail-section"><ContentState message={result.error} /></div></>;
  }
  const menu = result.data;
  return (
    <>
      <PageHero eyebrow={`${menu.menuType} · ÉLAN`} title={menu.name} description={menu.description} compact />
      <section className="menu-detail-intro page-container">
        <span className="eyebrow">A MENU FOR THE MOMENT</span>
        <h2>Made with care, guided by the season.</h2>
        <p>Our menu changes with the harvest. Please speak with our team about dietary needs before your visit.</p>
        <Link href="/menus" className="text-link">Back to all menus <span aria-hidden="true">↗</span></Link>
      </section>
      <section className="menu-detail-section section-paper">
        <div className="page-container">
          {menu.categories.map((category) => (
            <section className="menu-category-section" key={category.id}>
              <div className="menu-intro"><div><span className="eyebrow">FROM THE KITCHEN</span><h2>{category.name}</h2></div><p>{category.description}</p></div>
              {category.items.filter((item) => item.available).map((item) => (
                <article className="menu-row" key={item.id}>
                  <div className="menu-row-copy"><div className="menu-item-heading"><h3>{item.name}</h3>{item.seasonal && <span className="tag">Seasonal</span>}</div>
                    <p>{item.description}</p><span className="allergen-note">{item.allergens.length ? `Contains: ${item.allergens.join(", ")}` : "Please ask about allergens."}</span></div>
                  {item.price > 0 && <span className="menu-price">{money(item.price)}</span>}
                </article>
              ))}
            </section>
          ))}
          <p className="allergen-disclaimer">Dietary indicators and allergen information are provided as a guide only. Our kitchen handles common allergens and cross-contact may occur.</p>
        </div>
      </section>
    </>
  );
}
