"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import type { Menu } from "@/types/content";
import { money } from "@/lib/format";

type DietaryFilter = "all" | "vegetarian" | "vegan" | "glutenFree";
const filters: { value: DietaryFilter; label: string }[] = [
  { value: "all", label: "All dishes" },
  { value: "vegetarian", label: "Vegetarian" },
  { value: "vegan", label: "Vegan" },
  { value: "glutenFree", label: "Gluten-free" },
];

export function MenuExplorer({ menus }: { menus: Menu[] }) {
  const [selectedSlug, setSelectedSlug] = useState(menus[0]?.slug ?? "");
  const [filter, setFilter] = useState<DietaryFilter>("all");
  const menu = menus.find((item) => item.slug === selectedSlug) ?? menus[0];
  const items = useMemo(
    () => menu?.categories.flatMap((category) => category.items.map((item) => ({ item, category: category.name }))) ?? [],
    [menu],
  );
  const visible = items.filter(({ item }) => item.available && (filter === "all" || item[filter]));
  const selectByKeyboard = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    const next = event.key === "ArrowRight" ? (index + 1) % menus.length
      : event.key === "ArrowLeft" ? (index - 1 + menus.length) % menus.length : -1;
    if (next < 0) return;
    event.preventDefault();
    setSelectedSlug(menus[next].slug);
    document.getElementById(`tab-${menus[next].slug}`)?.focus();
  };

  return (
    <section className="menu-explorer page-container">
      <div className="menu-tabs" role="tablist" aria-label="Menu type">
        {menus.map((item, index) => (
          <button
            key={item.id}
            id={`tab-${item.slug}`}
            type="button"
            role="tab"
            aria-selected={menu?.slug === item.slug}
            aria-controls="menu-panel"
            onClick={() => setSelectedSlug(item.slug)}
            onKeyDown={(event) => selectByKeyboard(event, index)}
          >{item.name}</button>
        ))}
      </div>
      {menu && (
        <div id="menu-panel" role="tabpanel" aria-labelledby={`tab-${menu.slug}`}>
          <div className="menu-intro">
            <div><span className="eyebrow">A MENU FOR THE MOMENT</span><h2>{menu.name}</h2></div>
            <p>{menu.description}</p>
            <Link href={`/menus/${menu.slug}`} className="text-link">View this menu <span aria-hidden="true">↗</span></Link>
          </div>
          <div className="dietary-filters" role="group" aria-label="Filter dishes by dietary preference">
            {filters.map((option) => (
              <button key={option.value} type="button" aria-pressed={filter === option.value}
                onClick={() => setFilter(option.value)}>{option.label}</button>
            ))}
          </div>
          {visible.length ? (
            <div className="menu-list">
              {visible.map(({ item, category }) => (
                <article key={item.id} className="menu-row">
                  <div className="menu-row-copy">
                    <div className="menu-item-heading"><h3>{item.name}</h3>{item.seasonal && <span className="tag">Seasonal</span>}</div>
                    <p>{item.description}</p>
                    <span className="menu-category">{category}</span>
                    {!!item.allergens.length && <span className="allergen-note">Contains: {item.allergens.join(", ")}</span>}
                    <span className="dietary-tags">
                      {item.vegetarian && <span>Vegetarian</span>}
                      {item.vegan && <span>Vegan</span>}
                      {item.glutenFree && <span>Gluten-free</span>}
                    </span>
                  </div>
                  {item.price > 0 && <span className="menu-price">{money(item.price)}</span>}
                </article>
              ))}
            </div>
          ) : <p className="empty-copy">No available dishes match this selection.</p>}
          <p className="allergen-disclaimer">Please let our team know about allergies or dietary needs. Our kitchen handles common allergens, and cross-contact may occur; dietary labels are a guide, not a guarantee.</p>
        </div>
      )}
    </section>
  );
}
