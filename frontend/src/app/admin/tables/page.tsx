"use client";

import { FormEvent, useEffect, useState } from "react";
import { adminRequest } from "@/lib/admin";

type DiningTable = { id: string; tableNumber: string; displayName: string; minimumCapacity: number; maximumCapacity: number; location: string; active: boolean };
type AssignedReservation = { id: string; confirmationCode: string; guestName: string; date: string; time: string;
  partySize: number; tableName: string; experienceName: string | null; status: string };
const emptyForm = { tableNumber: "", displayName: "", minimumCapacity: 1, maximumCapacity: 2, location: "MAIN_DINING", active: true };
export default function TablesPage() {
  const [tables, setTables] = useState<DiningTable[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [bookingTable, setBookingTable] = useState<string | null>(null);
  const [assignedBookings, setAssignedBookings] = useState<AssignedReservation[]>([]);
  async function refresh() { setTables(await adminRequest<DiningTable[]>("/admin/tables")); }
  useEffect(() => { void refresh().catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Tables are unavailable.")); }, []);
  function edit(table: DiningTable) { setEditing(table.id); setForm({ ...table }); setMessage(""); }
  async function showBookings(table: DiningTable) {
    setError("");
    try {
      setAssignedBookings(await adminRequest<AssignedReservation[]>(`/admin/tables/${table.id}/reservations?days=30`));
      setBookingTable(table.id);
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Upcoming bookings could not be loaded."); }
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (busy) return; setBusy(true); setError(""); setMessage("");
    try {
      await adminRequest<DiningTable>(editing ? `/admin/tables/${editing}` : "/admin/tables",
        editing ? "PUT" : "POST", form);
      setForm(emptyForm); setEditing(null); await refresh(); setMessage("Table details saved.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Table could not be saved."); }
    finally { setBusy(false); }
  }
  return <div className="admin-stack">
    <section className="admin-panel"><div className="admin-panel-heading"><div><span className="eyebrow">DINING ROOM</span><h2>Tables</h2></div></div>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Number</th><th>Name</th><th>Capacity</th><th>Location</th><th>Availability</th><th /></tr></thead>
        <tbody>{tables.map((table) => <tr key={table.id}><td>{table.tableNumber}</td><td>{table.displayName}</td>
          <td>{table.minimumCapacity}–{table.maximumCapacity}</td><td>{table.location.replaceAll("_", " ")}</td>
          <td>{table.active ? "Active" : "Inactive"}</td><td><button className="admin-link-button" onClick={() => edit(table)}>Edit</button>
            <button className="admin-link-button" onClick={() => void showBookings(table)}>Upcoming bookings</button></td></tr>)}</tbody></table></div>
      {!tables.length && <p className="admin-empty">No tables are configured.</p>}
      {bookingTable && <div className="admin-subform"><h3>Upcoming assigned bookings</h3>
        {assignedBookings.length ? <ul className="admin-inquiry-list">{assignedBookings.map((booking) => <li key={booking.id}>
          <span><strong>{booking.date} · {booking.time} · {booking.guestName}</strong>
            <small>{booking.confirmationCode} · {booking.partySize} guests · {booking.status}</small></span></li>)}</ul>
          : <p className="admin-empty">No active bookings for this table in the next 30 days.</p>}
        <button className="admin-quiet-button" onClick={() => setBookingTable(null)}>Close bookings</button></div>}
    </section>
    <section className="admin-panel"><span className="eyebrow">{editing ? "UPDATE TABLE" : "ADD TABLE"}</span><h2>{editing ? "Edit table" : "New table"}</h2>
      <form className="admin-form-grid" onSubmit={submit}>
        <label>Table number<input required maxLength={30} value={form.tableNumber} onChange={(e) => setForm({ ...form, tableNumber: e.target.value })} /></label>
        <label>Display name<input required maxLength={80} value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} /></label>
        <label>Minimum capacity<input type="number" min={1} max={30} required value={form.minimumCapacity} onChange={(e) => setForm({ ...form, minimumCapacity: Number(e.target.value) })} /></label>
        <label>Maximum capacity<input type="number" min={1} max={30} required value={form.maximumCapacity} onChange={(e) => setForm({ ...form, maximumCapacity: Number(e.target.value) })} /></label>
        <label>Location<input required pattern="[A-Za-z_]{2,40}" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></label>
        {editing && <label className="admin-checkbox"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active</label>}
        {error && <p className="admin-error admin-form-wide" role="alert">{error}</p>}
        {message && <p className="admin-success admin-form-wide" role="status">{message}</p>}
        <div className="admin-form-wide admin-actions"><button className="button" disabled={busy}>{busy ? "Saving…" : "Save table"}</button>
          {editing && <button type="button" onClick={() => { setEditing(null); setForm(emptyForm); }}>Cancel edit</button>}</div>
      </form>
    </section>
  </div>;
}
