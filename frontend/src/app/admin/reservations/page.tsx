"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { adminRequest, type PageResult } from "@/lib/admin";

type Reservation = {
  id: string; confirmationCode: string; guestName: string; date: string; time: string;
  partySize: number; tableName: string; experienceName: string | null; status: string; specialOccasion: boolean;
};
type Detail = Omit<Reservation, "specialOccasion"> & {
  guest: { firstName: string; lastName: string; email: string; phone: string };
  endTime: string; specialOccasion: string | null; dietaryNotes: string | null; specialRequests: string | null;
  internalNotes: string | null; history: Array<{ oldStatus: string | null; newStatus: string; source: string; reason: string | null; changedAt: string }>;
};
type DiningTable = { id: string; displayName: string; active: boolean; minimumCapacity: number; maximumCapacity: number };

export default function ReservationsPage() {
  const [page, setPage] = useState<PageResult<Reservation> | null>(null);
  const [selected, setSelected] = useState<Detail | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [filter, setFilter] = useState({ date: "", status: "", q: "" });
  const [pageIndex, setPageIndex] = useState(0);
  const [manualOpen, setManualOpen] = useState(false);
  const [tables, setTables] = useState<DiningTable[]>([]);
  const [busy, setBusy] = useState(false);
  const load = useCallback(async (index = pageIndex, filters = filter) => {
    const params = new URLSearchParams({ page: String(index), size: "25" });
    if (filters.date) params.set("date", filters.date);
    if (filters.status) params.set("status", filters.status);
    if (filters.q.trim()) params.set("q", filters.q.trim());
    const result = await adminRequest<PageResult<Reservation>>(`/admin/reservations?${params}`);
    setPage(result);
    setError("");
  }, [filter, pageIndex]);
  useEffect(() => { void load().catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Reservations are unavailable.")); }, [load]);
  useEffect(() => {
    const reservationId = new URLSearchParams(window.location.search).get("reservationId");
    if (!reservationId) return;
    Promise.all([
      adminRequest<Detail>(`/admin/reservations/${reservationId}`),
      adminRequest<DiningTable[]>("/admin/tables"),
    ]).then(([detail, availableTables]) => { setSelected(detail); setTables(availableTables); })
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Reservation details could not be loaded."));
  }, []);

  async function inspect(id: string) {
    try {
      const [detail, availableTables] = await Promise.all([
        adminRequest<Detail>(`/admin/reservations/${id}`), adminRequest<DiningTable[]>("/admin/tables"),
      ]);
      setSelected(detail); setTables(availableTables); setMessage("");
    }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Reservation details could not be loaded."); }
  }
  async function changeStatus(status: string) {
    if (!selected || busy) return;
    setBusy(true); setError(""); setMessage("");
    try {
      const updated = await adminRequest<Detail>(`/admin/reservations/${selected.id}/status`, "PATCH", { status, reason: "" });
      setSelected(updated); setMessage(`Reservation marked ${status.toLowerCase()}.`);
      try { await load(); } catch { setError("The status was updated, but the reservation list could not be refreshed."); }
    } catch (reason) { setError(reason instanceof Error ? reason.message : "The reservation could not be updated."); }
    finally { setBusy(false); }
  }
  async function saveNotes(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected || busy) return;
    const notes = new FormData(event.currentTarget).get("notes");
    setBusy(true); setError(""); setMessage("");
    try {
      const updated = await adminRequest<Detail>(`/admin/reservations/${selected.id}/notes`, "PATCH", { internalNotes: notes });
      setSelected(updated); setMessage("Staff notes saved.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Notes could not be saved."); }
    finally { setBusy(false); }
  }
  async function saveBookingChange(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selected || busy) return;
    const values = new FormData(event.currentTarget);
    setBusy(true); setError(""); setMessage("");
    try {
      const updated = await adminRequest<Detail>(`/admin/reservations/${selected.id}/modify`, "PATCH", {
        reservationDate: values.get("reservationDate"), reservationTime: values.get("reservationTime"),
        partySize: Number(values.get("partySize")), dietaryNotes: selected.dietaryNotes, specialRequests: selected.specialRequests,
        reason: values.get("reason"),
      });
      setSelected(updated); setMessage("Reservation updated using the standard availability rules.");
      try { await load(); } catch { setError("The reservation was updated, but the list could not be refreshed."); }
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Reservation could not be changed."); }
    finally { setBusy(false); }
  }
  async function changeTable(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selected || busy) return;
    const values = new FormData(event.currentTarget);
    setBusy(true); setError(""); setMessage("");
    try {
      const updated = await adminRequest<Detail>(`/admin/reservations/${selected.id}/table`, "PATCH", {
        tableId: values.get("tableId"), reason: values.get("reason"),
      });
      setSelected(updated); setMessage("Table reassigned after capacity and overlap checks.");
      try { await load(); } catch { setError("The table was reassigned, but the reservation list could not be refreshed."); }
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Table could not be reassigned."); }
    finally { setBusy(false); }
  }
  async function createManualReservation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (busy) return;
    const values = new FormData(event.currentTarget);
    setBusy(true); setError(""); setMessage("");
    try {
      const result = await adminRequest<{ confirmationCode: string }>("/admin/reservations", "POST", {
        firstName: values.get("firstName"), lastName: values.get("lastName"), email: values.get("email"),
        phoneNumber: values.get("phoneNumber"), reservationDate: values.get("reservationDate"),
        reservationTime: values.get("reservationTime"), partySize: Number(values.get("partySize")),
        experienceId: null, specialOccasion: values.get("specialOccasion") || null,
        dietaryNotes: values.get("dietaryNotes") || null, specialRequests: values.get("specialRequests") || null,
      }, { "Idempotency-Key": crypto.randomUUID() });
      setManualOpen(false); setMessage(`Reservation ${result.confirmationCode} created.`);
      try { await load(); } catch { setError("Reservation was created, but the list could not be refreshed."); }
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Reservation could not be created."); }
    finally { setBusy(false); }
  }
  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setPageIndex(0);
    const form = new FormData(event.currentTarget);
    const value = { date: String(form.get("date") ?? ""), status: String(form.get("status") ?? ""), q: String(form.get("q") ?? "") };
    setFilter(value); void load(0, value).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Search failed."));
  }
  const canSeat = selected?.status === "CONFIRMED" || selected?.status === "PENDING";
  return <div className="admin-stack">
    <div className="admin-panel-heading"><span className="eyebrow">BOOKING OPERATIONS</span>
      <button className="button" onClick={() => setManualOpen((value) => !value)}>{manualOpen ? "Close form" : "New reservation"}</button></div>
    {manualOpen && <section className="admin-panel"><h2>Create a reservation</h2>
      <p>Manual reservations use the same service windows, table allocator, and capacity checks as online bookings.</p>
      <form className="admin-form-grid" onSubmit={createManualReservation}>
        <label>First name<input name="firstName" required maxLength={80} /></label><label>Last name<input name="lastName" required maxLength={80} /></label>
        <label>Email<input name="email" type="email" required maxLength={254} /></label><label>Phone<input name="phoneNumber" required pattern="[+()0-9 .-]{7,32}" /></label>
        <label>Date<input name="reservationDate" type="date" required /></label><label>Time<input name="reservationTime" type="time" required /></label>
        <label>Party size<input name="partySize" type="number" min={1} max={20} defaultValue={2} required /></label>
        <label>Occasion<select name="specialOccasion"><option value="">None</option>{["BIRTHDAY", "ANNIVERSARY", "PROPOSAL", "OTHER"].map((value) => <option key={value}>{value}</option>)}</select></label>
        <label className="admin-form-wide">Dietary notes<textarea name="dietaryNotes" maxLength={1000} rows={2} /></label>
        <label className="admin-form-wide">Special requests<textarea name="specialRequests" maxLength={1000} rows={2} /></label>
        <button className="button admin-form-wide" disabled={busy}>{busy ? "Creating…" : "Create reservation"}</button>
      </form></section>}
    <form className="admin-filters" onSubmit={applyFilters}>
      <label>Search<input name="q" placeholder="Name or confirmation code" defaultValue={filter.q} /></label>
      <label>Date<input type="date" name="date" defaultValue={filter.date} /></label>
      <label>Status<select name="status" defaultValue={filter.status}><option value="">All statuses</option>
        {["PENDING", "CONFIRMED", "SEATED", "COMPLETED", "CANCELLED", "NO_SHOW"].map((s) => <option key={s}>{s}</option>)}</select></label>
      <button className="button" type="submit">Apply filters</button>
    </form>
    {error && <p className="admin-error" role="alert">{error}</p>}
    {message && <p className="admin-success" role="status">{message}</p>}
    <section className="admin-panel">
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Confirmation</th><th>Guest</th><th>Date</th><th>Time</th><th>Party</th><th>Table</th><th>Experience</th><th>Status</th></tr></thead>
        <tbody>{page?.content.map((row) => <tr key={row.id} className="admin-click-row" onClick={() => void inspect(row.id)}>
          <td>{row.confirmationCode}</td><td>{row.guestName}</td><td>{row.date}</td><td>{row.time}</td><td>{row.partySize}</td><td>{row.tableName}</td>
          <td>{row.experienceName ?? "—"}</td><td><span className="admin-status">{row.status}</span></td></tr>)}</tbody></table></div>
      {!page?.content.length && <p className="admin-empty">No reservations match these filters.</p>}
      <div className="admin-pagination"><button disabled={!page || page.page === 0} onClick={() => { const n = pageIndex - 1; setPageIndex(n); void load(n); }}>Previous</button>
        <span>{page ? `Page ${page.page + 1} of ${Math.max(1, page.totalPages)} · ${page.totalElements} reservations` : "Loading…"}</span>
        <button disabled={!page || page.page + 1 >= page.totalPages} onClick={() => { const n = pageIndex + 1; setPageIndex(n); void load(n); }}>Next</button></div>
    </section>
    {selected && <section className="admin-panel">
      <div className="admin-panel-heading"><div><span className="eyebrow">{selected.confirmationCode}</span><h2>{selected.guest.firstName} {selected.guest.lastName}</h2></div>
        <button className="admin-quiet-button" type="button" onClick={() => setSelected(null)}>Close details</button></div>
      <div className="admin-detail-grid"><p><strong>Contact</strong><br />{selected.guest.email}<br />{selected.guest.phone}</p>
        <p><strong>Booking</strong><br />{selected.date} · {selected.time}–{selected.endTime}<br />{selected.partySize} guests · {selected.tableName}</p>
        <p><strong>Experience</strong><br />{selected.experienceName ?? "Standard service"}</p>
        <p><strong>Guest requests</strong><br />{selected.dietaryNotes || "None"}<br />{selected.specialRequests || ""}</p></div>
      {["CONFIRMED", "PENDING"].includes(selected.status) && <div className="admin-two-column admin-edit-grid">
        <form key={`${selected.id}:${selected.date}:${selected.time}:${selected.partySize}`}
          className="admin-form-grid" onSubmit={saveBookingChange}><strong className="admin-form-wide">Modify reservation</strong>
          <label>Date<input type="date" name="reservationDate" defaultValue={selected.date} required /></label>
          <label>Time<input type="time" name="reservationTime" defaultValue={selected.time} required /></label>
          <label>Party size<input type="number" name="partySize" min={1} max={20} defaultValue={selected.partySize} required /></label>
          <label className="admin-form-wide">Reason for change<input name="reason" maxLength={500} /></label>
          <button className="admin-form-wide" disabled={busy}>Save booking changes</button>
        </form>
        <form key={`${selected.id}:${selected.tableName}`} className="admin-form-grid" onSubmit={changeTable}><strong className="admin-form-wide">Reassign table</strong>
          <label className="admin-form-wide">Active table<select name="tableId" defaultValue="">
            <option value="" disabled>Select a table</option>{tables.filter((table) => table.active
              && table.minimumCapacity <= selected.partySize && table.maximumCapacity >= selected.partySize)
              .map((table) => <option key={table.id} value={table.id}>{table.displayName}</option>)}</select></label>
          <label className="admin-form-wide">Reason<input name="reason" maxLength={500} /></label>
          <button className="admin-form-wide" disabled={busy}>Reassign safely</button>
        </form>
      </div>}
      <div className="admin-actions">
        {canSeat && <button onClick={() => void changeStatus("SEATED")} disabled={busy}>Mark seated</button>}
        {selected.status === "SEATED" && <button onClick={() => void changeStatus("COMPLETED")} disabled={busy}>Mark completed</button>}
        {canSeat && <button onClick={() => void changeStatus("NO_SHOW")} disabled={busy}>Mark no-show</button>}
        {["CONFIRMED", "PENDING"].includes(selected.status) && <button className="admin-danger" onClick={() => {
          if (window.confirm("Cancel this reservation?")) void changeStatus("CANCELLED");
        }} disabled={busy}>Cancel</button>}
      </div>
      <form key={selected.id} className="admin-notes" onSubmit={saveNotes}><label>Internal staff notes<textarea name="notes" rows={3} maxLength={2000} defaultValue={selected.internalNotes ?? ""} /></label>
        <button className="button" disabled={busy}>Save notes</button></form>
      <h3>Status history</h3><ul className="admin-history">{selected.history.map((item, index) =>
        <li key={`${item.changedAt}-${index}`}><span>{item.oldStatus ?? "Created"} → {item.newStatus}</span><small>{item.changedAt} · {item.source}{item.reason ? ` · ${item.reason}` : ""}</small></li>)}</ul>
    </section>}
  </div>;
}
