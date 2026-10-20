import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MenuExplorer } from "./MenuExplorer";
import type { Menu } from "@/types/content";

const menus: Menu[] = [
  {
    id: "menu-dinner",
    name: "Dinner",
    slug: "dinner",
    description: "Seasonal dinner.",
    menuType: "DINNER",
    validFrom: null,
    validUntil: null,
    categories: [{
      id: "category-1",
      name: "To Begin",
      slug: "to-begin",
      description: null,
      items: [
        { id: "dish-1", name: "Wild Mushroom Agnolotti", description: "Mushrooms and sage.", price: 32, imageUrl: null, vegetarian: true, vegan: false, glutenFree: false, featured: false, available: true, seasonal: true, allergens: ["Dairy"] },
        { id: "dish-2", name: "Roasted Kabocha", description: "Kabocha and grains.", price: 28, imageUrl: null, vegetarian: true, vegan: true, glutenFree: true, featured: false, available: true, seasonal: true, allergens: [] },
      ],
    }],
  },
  {
    id: "menu-cocktails",
    name: "Cocktails",
    slug: "cocktails",
    description: "House cocktails.",
    menuType: "COCKTAILS",
    validFrom: null,
    validUntil: null,
    categories: [{ id: "category-2", name: "Signatures", slug: "signatures", description: null, items: [] }],
  },
];

describe("MenuExplorer", () => {
  it("switches menu types and filters dishes by dietary preference", () => {
    render(<MenuExplorer menus={menus} />);

    expect(screen.getByText("Wild Mushroom Agnolotti")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Vegan" }));
    expect(screen.queryByText("Wild Mushroom Agnolotti")).not.toBeInTheDocument();
    expect(screen.getByText("Roasted Kabocha")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: "Cocktails" }));
    expect(screen.getByRole("heading", { name: "Cocktails" })).toBeInTheDocument();
    expect(screen.getByText("No available dishes match this selection.")).toBeInTheDocument();
  });
});
