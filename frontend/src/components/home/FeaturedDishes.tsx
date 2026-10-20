import Link from "next/link";
import type { MenuItem } from "@/types/content";
import { money } from "@/lib/format";
import { EditorialImage } from "@/components/ui/EditorialImage";
import { ContentState } from "@/components/ui/ContentState";

export function FeaturedDishes({ items, error }: { items: MenuItem[] | null; error: string | null }) {
  if (!items) return <div className="page-container"><ContentState message={error ?? "Featured dishes are not available."} /></div>;
  if (!items.length) return <div className="page-container"><ContentState kind="empty" message="Our featured dishes are being refreshed." /></div>;
  return (
    <div className="featured-dishes">
      {items.map((item, index) => (
        <article className="dish-card" key={item.id}>
          <Link href="/menus" className="dish-card-image image-frame">
            <EditorialImage src={item.imageUrl} alt={item.name} sizes="(max-width: 700px) 86vw, 31vw" />
            <span className="image-index">0{index + 1}</span>
          </Link>
          <div className="dish-card-copy"><h3>{item.name}</h3><p>{item.description}</p><span>{money(item.price)}</span></div>
        </article>
      ))}
    </div>
  );
}
