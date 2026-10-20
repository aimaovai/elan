"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { adminRequest, type AdminUser } from "@/lib/admin";

const sections = [
  ["Dashboard", "/admin"],
  ["Reservations", "/admin/reservations"],
  ["Calendar", "/admin/calendar"],
  ["Tables", "/admin/tables"],
  ["Menus", "/admin/menus"],
  ["Experiences", "/admin/experiences"],
  ["Private Dining", "/admin/private-dining"],
  ["Notifications", "/admin/notifications"],
  ["Settings", "/admin/settings"],
] as const;

export function AdminLayoutClient({ children }: Readonly<{ children: React.ReactNode }>) {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<AdminUser | null>(null);
  const [ready, setReady] = useState(pathname === "/admin/login");
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (pathname === "/admin/login") return;
    let active = true;
    adminRequest<AdminUser>("/admin/auth/me")
      .then((value) => { if (active) { setUser(value); setReady(true); } })
      .catch(() => { if (active) router.replace("/admin/login"); })
      .finally(() => { if (active) setReady(true); });
    return () => { active = false; };
  }, [pathname, router]);

  async function logout() {
    try {
      await adminRequest<void>("/admin/auth/logout", "POST");
    } finally {
      setUser(null);
      router.replace("/admin/login");
    }
  }

  if (pathname === "/admin/login") return <>{children}</>;
  if (!ready || !user) return <div className="admin-loading" role="status">Checking your secure session…</div>;

  const title = sections.find(([, href]) => href === pathname)?.[0] ?? "ÉLAN Operations";
  return (
    <div className="admin-frame">
      <button className="admin-mobile-toggle" type="button" aria-expanded={menuOpen}
        aria-controls="admin-navigation" onClick={() => setMenuOpen((value) => !value)}>
        {menuOpen ? "Close menu" : "Menu"}
      </button>
      <aside id="admin-navigation" className={`admin-sidebar ${menuOpen ? "admin-sidebar-open" : ""}`}>
        <Link href="/admin" className="admin-wordmark">ÉLAN<span>.</span><small>RESTAURANT OPERATIONS</small></Link>
        <nav aria-label="Administration">
          {sections.map(([label, href]) => <Link key={href} href={href}
            aria-current={pathname === href ? "page" : undefined} onClick={() => setMenuOpen(false)}>{label}</Link>)}
        </nav>
        <p className="admin-demo-note">Fictional restaurant demonstration</p>
      </aside>
      <div className="admin-content">
        <header className="admin-topbar">
          <div><span className="eyebrow">ÉLAN OPERATIONS</span><h1>{title}</h1></div>
          <div className="admin-user"><span>{user.displayName}<small>{user.role}</small></span>
            <button type="button" onClick={logout}>Sign out</button></div>
        </header>
        <div className="admin-main">{children}</div>
      </div>
    </div>
  );
}
