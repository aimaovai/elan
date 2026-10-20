"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { adminRequest } from "@/lib/admin";

type Reservation = {
  id: string; confirmationCode: string; guestName: string; date: string; time: string;
  partySize: number; tableName: string; experienceName: string | null; status: string; specialOccasion: boolean;
};
type Inquiry = { id: string; referenceNumber: string; guestName: string; preferredDate: string; guestCount: number; status: string };
type Alert = { type: string; title: string; detail: string; href: string };
type Dashboard = {
  todaysReservations: number; confirmedReservations: number; expectedGuests: number;
  upcomingReservations: number; newInquiries: number; recentCancellations: number;
  todaysService: Reservation[]; upcoming: Reservation[]; inquiries: Inquiry[]; alerts: Alert[];
};

export default function AdminDashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState("");
  useEffect(() => {
    adminRequest<Dashboard>("/admin/dashboard").then(setData)
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Dashboard data is unavailable."));
  }, []);
  if (error) return <p className="admin-error" role="alert">{error}</p>;
  if (!data) return <p className="admin-loading" role="status">Loading today’s service…</p>;

  const metrics = [
    ["Today's reservations", data.todaysReservations],
    ["Confirmed today", data.confirmedReservations],
    ["Expected guests", data.expectedGuests],
    ["Upcoming confirmed", data.upcomingReservations],
    ["New inquiries", data.newInquiries],
    ["Recent cancellations", data.recentCancellations],
  ];
  return (
    <div className="admin-stack">
      <section className="admin-metrics" aria-label="Operational summary">
        {metrics.map(([label, value]) => <article className="admin-metric" key={label}>
          <span>{label}</span><strong>{value}</strong>
        </article>)}
      </section>
      {data.alerts.length > 0 && <section className="admin-panel" aria-label="Operational alerts">
        <div className="admin-panel-heading"><div><span className="eyebrow">ATTENTION</span><h2>Operational alerts</h2></div></div>
        <ul className="admin-inquiry-list">{data.alerts.map((alert, index) => <li key={`${alert.type}-${index}`}>
          <span><strong>{alert.title}</strong><small>{alert.detail}</small></span>
          <Link className="text-link" href={alert.href}>Review</Link></li>)}</ul>
      </section>}
      <section className="admin-panel">
        <div className="admin-panel-heading"><div><span className="eyebrow">SERVICE</span><h2>Today’s tables</h2></div>
          <Link className="text-link" href="/admin/reservations">View reservations</Link></div>
        <ReservationTable rows={data.todaysService} />
      </section>
      <div className="admin-two-column">
        <section className="admin-panel"><div className="admin-panel-heading"><div><span className="eyebrow">NEXT</span><h2>Coming up</h2></div>
          <Link className="text-link" href="/admin/calendar">Open calendar</Link></div><ReservationTable rows={data.upcoming} /></section>
        <section className="admin-panel"><div className="admin-panel-heading"><div><span className="eyebrow">PRIVATE DINING</span><h2>New inquiries</h2></div>
          <Link className="text-link" href="/admin/private-dining">Review inquiries</Link></div>
          {data.inquiries.length ? <ul className="admin-inquiry-list">{data.inquiries.map((item) =>
            <li key={item.id}><span><strong>{item.guestName}</strong><small>{item.referenceNumber} · {item.preferredDate} · {item.guestCount} guests</small></span><span className="admin-status">{item.status}</span></li>)}</ul>
            : <p className="admin-empty">No new inquiries require attention.</p>}</section>
      </div>
      <section className="admin-quick-actions" aria-label="Quick actions">
        <Link href="/admin/tables">Manage tables</Link><Link href="/admin/menus">Update menus</Link>
        <Link href="/admin/settings">Restaurant settings &amp; closures</Link>
      </section>
    </div>
  );
}

function ReservationTable({ rows }: { rows: Reservation[] }) {
  if (!rows.length) return <p className="admin-empty">No reservations in this period.</p>;
  return <div className="admin-table-wrap"><table className="admin-table"><thead><tr>
    <th>Time</th><th>Guest</th><th>Party</th><th>Table</th><th>Status</th></tr></thead>
    <tbody>{rows.map((row) => <tr key={row.id}><td><Link href={`/admin/reservations?reservationId=${row.id}`}>{row.time}</Link></td><td>{row.guestName}{row.specialOccasion && <small className="admin-occasion">Special occasion</small>}</td>
      <td>{row.partySize}</td><td>{row.tableName}</td><td><span className="admin-status">{row.status}</span></td></tr>)}</tbody></table></div>;
}
