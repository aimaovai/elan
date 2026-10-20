import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SiteHeader } from "./SiteHeader";

vi.mock("next/navigation", () => ({ usePathname: () => "/menus" }));

describe("SiteHeader", () => {
  beforeEach(() => {
    window.scrollTo = vi.fn();
  });

  it("renders navigation and supports opening and escaping the mobile menu", () => {
    render(<SiteHeader />);
    expect(screen.getByRole("navigation", { name: "Main navigation" })).toBeInTheDocument();
    const toggle = screen.getByRole("button", { name: "Open navigation menu" });
    fireEvent.click(toggle);
    expect(screen.getByRole("dialog", { name: "Mobile navigation" })).toBeInTheDocument();
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByRole("dialog", { name: "Mobile navigation" })).not.toBeInTheDocument();
    expect(toggle).toHaveFocus();
  });
});
