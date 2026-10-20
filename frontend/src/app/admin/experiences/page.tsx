"use client";

import { FormEvent, useEffect, useState } from "react";
import { adminRequest } from "@/lib/admin";

type Course = { id: string | null; clientKey?: string; courseNumber: number; name: string; description: string; imageUrl: string | null; displayOrder: number };
type Experience = { id: string; name: string; slug: string; shortDescription: string; fullDescription: string;
  heroImageUrl: string; price: number; winePairingPrice: number | null; status: string; featured: boolean;
  startDate: string; endDate: string; reservationDurationMinutes: number; courseCount: number; displayOrder: number; courses: Course[] };
type ExperienceInput = Omit<Experience, "id" | "status" | "courseCount" | "courses">;
const blank: ExperienceInput = { name: "", slug: "", shortDescription: "", fullDescription: "", heroImageUrl: "",
  price: 0, winePairingPrice: null, featured: false, startDate: "", endDate: "", reservationDurationMinutes: 120, displayOrder: 0 };
export default function ExperiencesAdminPage() {
  const [rows, setRows] = useState<Experience[]>([]); const [selected, setSelected] = useState<Experience | null>(null);
  const [form, setForm] = useState<ExperienceInput>(blank); const [creating, setCreating] = useState(false);
  const [error, setError] = useState(""); const [message, setMessage] = useState("");
  async function load() { setRows(await adminRequest<Experience[]>("/admin/experiences")); }
  useEffect(() => { void load().catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Experiences are unavailable.")); }, []);
  function startEdit(item: Experience) {
    setSelected(item); setCreating(false);
    setForm({ name: item.name, slug: item.slug, shortDescription: item.shortDescription, fullDescription: item.fullDescription,
      heroImageUrl: item.heroImageUrl, price: item.price, winePairingPrice: item.winePairingPrice,
      featured: item.featured, startDate: item.startDate, endDate: item.endDate,
      reservationDurationMinutes: item.reservationDurationMinutes, displayOrder: item.displayOrder });
  }
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); setMessage("");
    try {
      const response = await adminRequest<Experience>(creating ? "/admin/experiences" : `/admin/experiences/${selected?.id}`,
        creating ? "POST" : "PUT", form);
      await load(); setSelected(response); setCreating(false); setForm({ ...response }); setMessage("Experience saved.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Experience could not be saved."); }
  }
  async function publish(item: Experience) {
    const status = item.status === "PUBLISHED" ? "DRAFT" : "PUBLISHED";
    try { await adminRequest(`/admin/experiences/${item.id}/publication`, "PATCH", { status }); await load(); setMessage(`Experience ${status.toLowerCase()}.`); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Experience status could not be changed."); }
  }
  async function saveCourses(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selected) return;
    const courses = selected.courses.map((course) => ({ id: course.id, name: course.name, description: course.description, imageUrl: course.imageUrl }));
    try {
      const result = await adminRequest<Experience>(`/admin/experiences/${selected.id}/courses`, "PUT", { courses });
      setSelected(result); await load(); setMessage("Course order saved.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Courses could not be saved."); }
  }
  return <div className="admin-stack">
    <section className="admin-panel"><div className="admin-panel-heading"><div><span className="eyebrow">SEASONAL TASTING</span><h2>Experiences</h2></div>
      <button className="button" onClick={() => { setSelected(null); setForm(blank); setCreating(true); }}>Create experience</button></div>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Experience</th><th>Price</th><th>Dates</th><th>Courses</th><th>State</th><th /></tr></thead>
        <tbody>{rows.map((row) => <tr key={row.id}><td>{row.name}<small>/{row.slug}</small></td><td>${Number(row.price).toFixed(2)}</td>
          <td>{row.startDate} – {row.endDate}</td><td>{row.courseCount}</td><td><span className="admin-status">{row.status}</span></td>
          <td><button className="admin-link-button" onClick={() => startEdit(row)}>Edit</button> <button className="admin-link-button" onClick={() => void publish(row)}>{row.status === "PUBLISHED" ? "Unpublish" : "Publish"}</button></td></tr>)}</tbody></table></div>
      {error && <p className="admin-error" role="alert">{error}</p>}{message && <p className="admin-success" role="status">{message}</p>}
    </section>
    {(selected || creating) && <section className="admin-panel"><span className="eyebrow">{creating ? "CREATE" : "EDIT"}</span><h2>{form.name || "New experience"}</h2>
      <form className="admin-form-grid" onSubmit={save}>
        <label>Name<input required maxLength={120} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
        <label>Slug<input required pattern="[a-z0-9-]+" value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} /></label>
        <label>Price<input required type="number" min="0" step=".01" value={form.price} onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} /></label>
        <label>Pairing price<input type="number" min="0" step=".01" value={form.winePairingPrice ?? ""} onChange={(e) => setForm({ ...form, winePairingPrice: e.target.value ? Number(e.target.value) : null })} /></label>
        <label>Start date<input type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></label>
        <label>End date<input type="date" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></label>
        <label>Dining duration<input type="number" min={30} max={600} required value={form.reservationDurationMinutes} onChange={(e) => setForm({ ...form, reservationDurationMinutes: Number(e.target.value) })} /></label>
        <label>Display order<input type="number" min={0} value={form.displayOrder} onChange={(e) => setForm({ ...form, displayOrder: Number(e.target.value) })} /></label>
        <label>Hero image URL<input type="url" required value={form.heroImageUrl} onChange={(e) => setForm({ ...form, heroImageUrl: e.target.value })} /></label>
        <label className="admin-checkbox"><input type="checkbox" checked={form.featured} onChange={(e) => setForm({ ...form, featured: e.target.checked })} /> Featured</label>
        <label className="admin-form-wide">Short description<textarea required rows={2} value={form.shortDescription} onChange={(e) => setForm({ ...form, shortDescription: e.target.value })} /></label>
        <label className="admin-form-wide">Full description<textarea required rows={4} value={form.fullDescription} onChange={(e) => setForm({ ...form, fullDescription: e.target.value })} /></label>
        <button className="button admin-form-wide">Save experience</button>
      </form>
      {selected && <form className="admin-course-editor" onSubmit={(event) => void saveCourses(event)}>
        <div className="admin-panel-heading"><h3>Course sequence</h3><button type="button" onClick={() => setSelected({
          ...selected, courses: [...selected.courses, { id: null, clientKey: crypto.randomUUID(),
            courseNumber: selected.courses.length + 1, name: "", description: "", imageUrl: null,
            displayOrder: selected.courses.length }],
        })}>Add course</button></div>
        {[...selected.courses].sort((a, b) => a.displayOrder - b.displayOrder).map((course, index) => <label key={course.id ?? course.clientKey}>
          <span>Course {index + 1}</span><input required maxLength={120} value={course.name} onChange={(e) =>
            setSelected({ ...selected, courses: selected.courses.map((c) => c === course ? { ...c, name: e.target.value } : c) })} />
          <textarea required maxLength={2000} value={course.description} onChange={(e) =>
            setSelected({ ...selected, courses: selected.courses.map((c) => c === course ? { ...c, description: e.target.value } : c) })} />
          <span className="admin-actions"><button type="button" disabled={index === 0} onClick={() => {
            const sorted = [...selected.courses].sort((a, b) => a.displayOrder - b.displayOrder);
            [sorted[index - 1], sorted[index]] = [sorted[index], sorted[index - 1]];
            setSelected({ ...selected, courses: sorted.map((item, position) => ({ ...item, displayOrder: position })) });
          }}>Move up</button><button type="button" disabled={index === selected.courses.length - 1} onClick={() => {
            const sorted = [...selected.courses].sort((a, b) => a.displayOrder - b.displayOrder);
            [sorted[index], sorted[index + 1]] = [sorted[index + 1], sorted[index]];
            setSelected({ ...selected, courses: sorted.map((item, position) => ({ ...item, displayOrder: position })) });
          }}>Move down</button></span>
        </label>)}
        <button className="button">Save course sequence</button></form>}
    </section>}
  </div>;
}
