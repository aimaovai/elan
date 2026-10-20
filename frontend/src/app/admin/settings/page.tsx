"use client";

import { FormEvent, useEffect, useState } from "react";
import { adminRequest } from "@/lib/admin";

type Settings = { restaurantTimezone: string; minimumAdvanceBookingHours: number; maximumAdvanceBookingDays: number;
  minimumPartySize: number; maximumOnlinePartySize: number; defaultReservationDurationMinutes: number;
  reservationIntervalMinutes: number; cancellationCutoffHours: number; bookingEnabled: boolean; privateDiningEnabled: boolean };
type Period = { id: string; name: string; dayOfWeek: number; startTime: string; endTime: string;
  slotIntervalMinutes: number; diningDurationMinutes: number; active: boolean };
type Closure = { id: string; date: string; reason: string; fullDay: boolean; startTime: string | null; endTime: string | null };
const initial: Settings = { restaurantTimezone: "America/Los_Angeles", minimumAdvanceBookingHours: 2,
  maximumAdvanceBookingDays: 90, minimumPartySize: 1, maximumOnlinePartySize: 8,
  defaultReservationDurationMinutes: 120, reservationIntervalMinutes: 30, cancellationCutoffHours: 24,
  bookingEnabled: true, privateDiningEnabled: true };
export default function SettingsPage() {
  const [settings, setSettings] = useState(initial);
  const [periods, setPeriods] = useState<Period[]>([]);
  const [closures, setClosures] = useState<Closure[]>([]);
  const [editingPeriod, setEditingPeriod] = useState<string | null>(null);
  const [periodForm, setPeriodForm] = useState({ name: "Dinner", dayOfWeek: 5, startTime: "17:30",
    endTime: "22:00", slotIntervalMinutes: 30, diningDurationMinutes: 120, active: true });
  const [error, setError] = useState(""); const [message, setMessage] = useState(""); const [busy, setBusy] = useState(false);
  const [loaded, setLoaded] = useState(false);
  useEffect(() => {
    Promise.all([
      adminRequest<Settings>("/admin/settings"),
      adminRequest<Period[]>("/admin/settings/service-periods"),
      adminRequest<Closure[]>("/admin/settings/blocked-dates"),
    ]).then(([value, service, blocked]) => { setSettings(value); setPeriods(service); setClosures(blocked); })
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Settings could not be loaded."))
      .finally(() => setLoaded(true));
  }, []);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (busy) return; setBusy(true); setError(""); setMessage("");
    try { setSettings(await adminRequest<Settings>("/admin/settings", "PATCH", settings)); setMessage("Settings saved and applied to new availability checks."); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Settings could not be saved."); }
    finally { setBusy(false); }
  }
  function numberField(key: keyof Settings, label: string, min: number, max: number) {
    return <label>{label}<input type="number" min={min} max={max} value={Number(settings[key])}
      onChange={(e) => setSettings({ ...settings, [key]: Number(e.target.value) })} /></label>;
  }
  async function reloadOperations() {
    const [service, blocked] = await Promise.all([
      adminRequest<Period[]>("/admin/settings/service-periods"),
      adminRequest<Closure[]>("/admin/settings/blocked-dates"),
    ]);
    setPeriods(service); setClosures(blocked);
  }
  async function savePeriod(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(""); setMessage("");
    try {
      await adminRequest<Period>(editingPeriod ? `/admin/settings/service-periods/${editingPeriod}` : "/admin/settings/service-periods",
        editingPeriod ? "PUT" : "POST", periodForm);
      await reloadOperations(); setEditingPeriod(null); setMessage("Service period saved.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Service period could not be saved."); }
    finally { setBusy(false); }
  }
  async function removePeriod(period: Period) {
    if (!window.confirm("Deactivate this service period?")) return;
    try { await adminRequest<void>(`/admin/settings/service-periods/${period.id}`, "DELETE"); await reloadOperations(); setMessage("Service period deactivated."); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Service period could not be deactivated."); }
  }
  async function saveClosure(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget; const values = new FormData(form); setError(""); setMessage("");
    const fullDay = values.has("fullDay");
    try {
      await adminRequest<Closure>("/admin/settings/blocked-dates", "POST", {
        blockedDate: values.get("date"), reason: values.get("reason"), fullDay,
        startTime: fullDay ? null : values.get("startTime"), endTime: fullDay ? null : values.get("endTime"),
      });
      await reloadOperations(); form.reset(); setMessage("Closure added.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Closure could not be saved."); }
  }
  async function removeClosure(block: Closure) {
    if (!window.confirm(`Remove the closure on ${block.date}?`)) return;
    try { await adminRequest<void>(`/admin/settings/blocked-dates/${block.id}`, "DELETE"); await reloadOperations(); setMessage("Closure removed."); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Closure could not be removed."); }
  }
  if (!loaded) return error ? <p className="admin-error" role="alert">{error}</p>
    : <p className="admin-loading" role="status">Loading restaurant settings…</p>;
  return <div className="admin-stack"><section className="admin-panel"><span className="eyebrow">RESERVATION POLICY</span><h2>Operating settings</h2>
    <p>Updates are persisted and immediately affect online availability. Existing confirmed reservations are retained.</p>
    <form className="admin-form-grid" onSubmit={submit}>
      <label>Restaurant timezone<input required value={settings.restaurantTimezone} onChange={(e) => setSettings({ ...settings, restaurantTimezone: e.target.value })} placeholder="America/Los_Angeles" /></label>
      {numberField("minimumAdvanceBookingHours", "Minimum lead time (hours)", 0, 720)}
      {numberField("maximumAdvanceBookingDays", "Advance booking window (days)", 1, 365)}
      {numberField("minimumPartySize", "Minimum party size", 1, 20)}
      {numberField("maximumOnlinePartySize", "Maximum online party size", 1, 20)}
      {numberField("defaultReservationDurationMinutes", "Default seating duration (minutes)", 30, 600)}
      {numberField("reservationIntervalMinutes", "Reservation interval (minutes)", 5, 180)}
      {numberField("cancellationCutoffHours", "Cancellation cutoff (hours)", 0, 720)}
      <label className="admin-checkbox"><input type="checkbox" checked={settings.bookingEnabled} onChange={(e) => setSettings({ ...settings, bookingEnabled: e.target.checked })} /> Online reservations enabled</label>
      <label className="admin-checkbox"><input type="checkbox" checked={settings.privateDiningEnabled} onChange={(e) => setSettings({ ...settings, privateDiningEnabled: e.target.checked })} /> Private dining inquiries enabled</label>
      {error && <p className="admin-error admin-form-wide" role="alert">{error}</p>}
      {message && <p className="admin-success admin-form-wide" role="status">{message}</p>}
      <button className="button admin-form-wide" disabled={busy}>{busy ? "Saving…" : "Save settings"}</button>
    </form></section>
    <section className="admin-panel"><span className="eyebrow">WEEKLY SERVICE</span><h2>Service periods</h2>
      <p>Changes are rejected when they would exclude existing upcoming reservations.</p>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Day</th><th>Service</th><th>Hours</th><th>Slot / duration</th><th>State</th><th /></tr></thead>
        <tbody>{periods.map((period) => <tr key={period.id}><td>{["", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"][period.dayOfWeek]}</td>
          <td>{period.name}</td><td>{period.startTime}–{period.endTime}</td><td>{period.slotIntervalMinutes} / {period.diningDurationMinutes} min</td>
          <td>{period.active ? "Active" : "Inactive"}</td><td><button className="admin-link-button" onClick={() => {
            setEditingPeriod(period.id); setPeriodForm({ name: period.name, dayOfWeek: period.dayOfWeek,
              startTime: period.startTime, endTime: period.endTime, slotIntervalMinutes: period.slotIntervalMinutes,
              diningDurationMinutes: period.diningDurationMinutes, active: period.active });
          }}>Edit</button>{period.active && <button className="admin-link-button" onClick={() => void removePeriod(period)}>Deactivate</button>}</td></tr>)}</tbody></table></div>
      <form className="admin-form-grid admin-subform" onSubmit={savePeriod}>
        <label>Service name<input required maxLength={80} value={periodForm.name} onChange={(e) => setPeriodForm({ ...periodForm, name: e.target.value })} /></label>
        <label>Day<select value={periodForm.dayOfWeek} onChange={(e) => setPeriodForm({ ...periodForm, dayOfWeek: Number(e.target.value) })}>
          {["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"].map((day, index) => <option key={day} value={index + 1}>{day}</option>)}</select></label>
        <label>Start<input type="time" required value={periodForm.startTime} onChange={(e) => setPeriodForm({ ...periodForm, startTime: e.target.value })} /></label>
        <label>End<input type="time" required value={periodForm.endTime} onChange={(e) => setPeriodForm({ ...periodForm, endTime: e.target.value })} /></label>
        <label>Reservation interval (min)<input type="number" min={5} max={180} value={periodForm.slotIntervalMinutes} onChange={(e) => setPeriodForm({ ...periodForm, slotIntervalMinutes: Number(e.target.value) })} /></label>
        <label>Dining duration (min)<input type="number" min={30} max={600} value={periodForm.diningDurationMinutes} onChange={(e) => setPeriodForm({ ...periodForm, diningDurationMinutes: Number(e.target.value) })} /></label>
        {editingPeriod && <label className="admin-checkbox"><input type="checkbox" checked={periodForm.active} onChange={(e) => setPeriodForm({ ...periodForm, active: e.target.checked })} /> Active</label>}
        <div className="admin-form-wide admin-actions"><button className="button" disabled={busy}>{editingPeriod ? "Save period" : "Add period"}</button>
          {editingPeriod && <button type="button" onClick={() => setEditingPeriod(null)}>Cancel edit</button>}</div>
      </form>
    </section>
    <section className="admin-panel"><span className="eyebrow">CLOSURES</span><h2>Blocked dates</h2>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Date</th><th>Reason</th><th>Hours</th><th /></tr></thead>
        <tbody>{closures.map((block) => <tr key={block.id}><td>{block.date}</td><td>{block.reason}</td>
          <td>{block.fullDay ? "Full day" : `${block.startTime}–${block.endTime}`}</td>
          <td><button className="admin-link-button" onClick={() => void removeClosure(block)}>Remove</button></td></tr>)}</tbody></table></div>
      <form className="admin-form-grid admin-subform" onSubmit={saveClosure}>
        <label>Date<input name="date" type="date" required /></label><label>Reason<input name="reason" required maxLength={160} /></label>
        <label className="admin-checkbox"><input name="fullDay" type="checkbox" defaultChecked /> Full-day closure</label>
        <label>Partial start<input name="startTime" type="time" /></label><label>Partial end<input name="endTime" type="time" /></label>
        <button className="admin-form-wide">Add closure</button>
      </form>
      {error && <p className="admin-error" role="alert">{error}</p>}{message && <p className="admin-success" role="status">{message}</p>}
    </section></div>;
}
