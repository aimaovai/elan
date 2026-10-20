import { AdminLayoutClient } from "@/components/admin/AdminLayoutClient";

export const metadata = { robots: { index: false, follow: false }, referrer: "no-referrer" as const };

export default function AdminLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <AdminLayoutClient>{children}</AdminLayoutClient>;
}
