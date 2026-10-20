"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { adminRequest, type PageResult } from "@/lib/admin";

type Reservation = { id: string; confirmationCode: string; guestName: string; date: string; time: string; partySize: number; tableName: string; status: string };
function localDateValue(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
export default function CalendarPage() {
  const [date, setDate] = useState(() => localDateValue(new Date()));
  const [view, setView] = useState<"day" | "week">("day");
  const [rows, setRows] = useState<Reservation[]>([]);
  const [error, setError] = useState("");
  useEffect(() => {
    const start = new Date(`${date}T12:00:00`);
    const days = view === "day" ? [start] : Array.from({ length: 7 }, (_, index) => {
      const day = new Date(start); day.setDate(day.getDate() + index); return day;
    });
    Promise.all(days.map((day) => adminRequest<PageResult<Reservation>>(
      `/admin/reservations?date=${day.toISOString().slice(0, 10)}&page=0&size=100`)))
      .then((results) => setRows(results.flatMap((result) => result.content).sort((a, b) => `${a.date}${a.time}`.localeCompare(`${b.date}${b.time}`))))
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Calendar data is unavailable."));
  }, [date, view]);
  return <div className="admin-stack">
    <section className="admin-panel admin-calendar-controls"><label>Starting date<input type="date" value={date} onChange={(event) => setDate(event.target.value)} /></label>
      <div className="admin-toggle-group"><button aria-pressed={view === "day"} onClick={() => setView("day")}>Day</button>
        <button aria-pressed={view === "week"} onClick={() => setView("week")}>Week</button></div></section>
    {error && <p className="admin-error" role="alert">{error}</p>}
    {!rows.length && !error && <section className="admin-panel admin-empty">No reservations are scheduled for this period.</section>}
    <section className="admin-calendar-list">{rows.map((row) => <article className="admin-calendar-row" key={row.id}>
      <div className="admin-calendar-time">{view === "week" && <small>{row.date}</small>}<strong>{row.time}</strong></div>
      <div><span className={`admin-status status-${row.status.toLowerCase()}`}>{row.status}</span>
        <h2><Link href={`/admin/reservations?reservationId=${row.id}`}>{row.guestName}</Link></h2>
        <p>{row.confirmationCode} · {row.partySize} guests · {row.tableName}</p></div>
    </article>)}</section>
  </div>;
}
