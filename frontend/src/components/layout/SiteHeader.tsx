"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu as MenuIcon, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";

const links = [
  { href: "/menus", label: "Menus" },
  { href: "/experiences", label: "Experiences" },
  { href: "/private-dining", label: "Private dining" },
  { href: "/about", label: "Our story" },
  { href: "/gallery", label: "Gallery" },
  { href: "/contact", label: "Contact" },
];

export function SiteHeader() {
  const pathname = usePathname();
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const toggleRef = useRef<HTMLButtonElement>(null);
  const onHome = pathname === "/";

  useEffect(() => {
    const update = () => setScrolled(window.scrollY > 24);
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && open) {
        setOpen(false);
        toggleRef.current?.focus();
      } else if (event.key === "Tab" && open) {
        const focusable = document.querySelectorAll<HTMLElement>(".mobile-nav a, .mobile-nav button");
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last?.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first?.focus();
        }
      }
    };
    update();
    window.addEventListener("scroll", update, { passive: true });
    window.addEventListener("keydown", onKeyDown);
    return () => {
      window.removeEventListener("scroll", update);
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  useEffect(() => {
    if (open) document.querySelector<HTMLAnchorElement>(".mobile-nav a")?.focus();
  }, [open]);

  return (
    <header className={`site-header ${onHome && !scrolled ? "site-header-over-hero" : ""} ${scrolled ? "site-header-scrolled" : ""}`}>
      <div className="header-inner">
        <Link href="/" className="wordmark" aria-label="Élan home">ÉLAN<span className="wordmark-dot">.</span></Link>
        <nav className="desktop-nav" aria-label="Main navigation">
          {links.map((link) => (
            <Link key={link.href} href={link.href} aria-current={pathname.startsWith(link.href) ? "page" : undefined}>
              {link.label}
            </Link>
          ))}
        </nav>
        <Link className="button button-header" href="/reservations">Reserve a table</Link>
        <button
          className="menu-toggle"
          type="button"
          ref={toggleRef}
          aria-label={open ? "Close navigation menu" : "Open navigation menu"}
          aria-expanded={open}
          aria-controls="mobile-navigation"
          onClick={() => setOpen((value) => !value)}
        >
          {open ? <X size={22} aria-hidden="true" /> : <MenuIcon size={22} aria-hidden="true" />}
        </button>
      </div>
      {open && (
        <nav id="mobile-navigation" className="mobile-nav" role="dialog" aria-modal="true" aria-label="Mobile navigation">
          <p className="eyebrow">A TABLE AT ÉLAN</p>
          {links.map((link, index) => (
            <Link key={link.href} href={link.href} style={{ animationDelay: `${index * 45}ms` }} onClick={() => setOpen(false)}>
              <span className="mobile-nav-number">0{index + 1}</span>{link.label}
            </Link>
          ))}
          <Link className="button button-light mobile-reserve" href="/reservations" onClick={() => setOpen(false)}>Reserve a table</Link>
          <span className="mobile-nav-note">Northbank · Portland</span>
        </nav>
      )}
    </header>
  );
}
