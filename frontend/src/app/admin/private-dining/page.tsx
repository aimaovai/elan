"use client";

import { useCallback, useEffect, useState } from "react";
import { adminRequest, type PageResult } from "@/lib/admin";

type Inquiry = { id: string; referenceNumber: string; guestName: string; preferredDate: string; guestCount: number; eventType: string; status: string; createdAt: string };
type Detail = Inquiry & { firstName: string; lastName: string; email: string; phoneNumber: string; alternateDate: string | null;
  requestedSpace: string | null; budgetPerGuest: number | null; dietaryRequirements: string | null; beveragePreferences: string | null;
  eventDescription: string; specialRequests: string | null; preferredContactMethod: string; internalNotes: string | null };
const statuses = ["NEW", "CONTACTED", "PROPOSAL_SENT", "BOOKED", "DECLINED", "CANCELLED"];
export default function PrivateDiningPage() {
  const [rows, setRows] = useState<Inquiry[]>([]);
  const [selected, setSelected] = useState<Detail | null>(null);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const load = useCallback(async () => {
    const page = await adminRequest<PageResult<Inquiry>>(`/admin/private-dining/inquiries?page=0&size=100${status ? `&status=${status}` : ""}`);
    setRows(page.content);
  }, [status]);
  useEffect(() => { void load().catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Inquiries are unavailable.")); }, [load]);
  async function open(id: string) {
    try { setSelected(await adminRequest<Detail>(`/admin/private-dining/inquiries/${id}`)); setError(""); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Inquiry details could not be loaded."); }
  }
  async function update() {
    if (!selected) return;
    setError(""); setMessage("");
    try {
      const updated = await adminRequest<Detail>(`/admin/private-dining/inquiries/${selected.id}`, "PATCH",
        { status: selected.status, internalNotes: selected.internalNotes });
      setSelected(updated); await load(); setMessage("Inquiry updated.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Inquiry could not be updated."); }
  }
  return <div className="admin-stack">
    <section className="admin-panel admin-calendar-controls"><p>Private dining inquiries are requests, not confirmed events.</p>
      <label>Status<select value={status} onChange={(e) => setStatus(e.target.value)}><option value="">All statuses</option>{statuses.map((value) => <option key={value}>{value}</option>)}</select></label></section>
    {error && <p className="admin-error" role="alert">{error}</p>}{message && <p className="admin-success" role="status">{message}</p>}
    <section className="admin-panel"><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Reference</th><th>Guest</th><th>Event date</th><th>Guests</th><th>Event type</th><th>Status</th></tr></thead>
      <tbody>{rows.map((row) => <tr key={row.id} className="admin-click-row" onClick={() => void open(row.id)}>
        <td>{row.referenceNumber}</td><td>{row.guestName}</td><td>{row.preferredDate}</td><td>{row.guestCount}</td><td>{row.eventType}</td><td><span className="admin-status">{row.status}</span></td></tr>)}</tbody></table></div>
      {!rows.length && <p className="admin-empty">No private dining inquiries match this status.</p>}</section>
    {selected && <section className="admin-panel"><div className="admin-panel-heading"><div><span className="eyebrow">{selected.referenceNumber}</span><h2>{selected.firstName} {selected.lastName}</h2></div>
      <button className="admin-quiet-button" onClick={() => setSelected(null)}>Close details</button></div>
      <div className="admin-detail-grid"><p><strong>Contact</strong><br />{selected.email}<br />{selected.phoneNumber}<br />Preferred: {selected.preferredContactMethod}</p>
        <p><strong>Event</strong><br />{selected.eventType} · {selected.guestCount} guests<br />{selected.preferredDate}{selected.alternateDate ? ` / ${selected.alternateDate}` : ""}<br />{selected.requestedSpace ?? "No room preference"}</p>
        <p><strong>Budget per guest</strong><br />{selected.budgetPerGuest == null ? "Not provided" : `$${selected.budgetPerGuest}`}</p>
        <p><strong>Details</strong><br />{selected.eventDescription}<br />Dietary: {selected.dietaryRequirements || "None"}</p></div>
      <div className="admin-form-grid"><label>Status<select value={selected.status} onChange={(e) => setSelected({ ...selected, status: e.target.value })}>{statuses.map((s) => <option key={s}>{s}</option>)}</select></label>
        <label className="admin-form-wide">Internal notes<textarea rows={3} maxLength={2000} value={selected.internalNotes ?? ""} onChange={(e) => setSelected({ ...selected, internalNotes: e.target.value })} /></label>
        <button className="button" onClick={() => void update()}>Save inquiry</button></div>
    </section>}
  </div>;
}
