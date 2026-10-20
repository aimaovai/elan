"use client";

import { FormEvent, useEffect, useState } from "react";
import { adminRequest } from "@/lib/admin";

type Item = { id: string; name: string; description: string; price: number; imageUrl: string | null; vegetarian: boolean; vegan: boolean; glutenFree: boolean; featured: boolean; available: boolean; seasonal: boolean; displayOrder: number; allergens: string[] };
type Category = { id: string; name: string; slug: string; description: string | null; displayOrder: number; items: Item[] };
type Menu = { id: string; name: string; slug: string; description: string; menuType: string; published: boolean; validFrom: string | null; validUntil: string | null; displayOrder: number; categories: Category[] };
type MenuInput = Omit<Menu, "id" | "published" | "categories">;
const blankMenu: MenuInput = { name: "", slug: "", description: "", menuType: "DINNER", validFrom: "", validUntil: "", displayOrder: 0 };

export default function MenusAdminPage() {
  const [menus, setMenus] = useState<Menu[]>([]);
  const [selected, setSelected] = useState<Menu | null>(null);
  const [form, setForm] = useState<MenuInput>(blankMenu);
  const [newMenu, setNewMenu] = useState(false);
  const [error, setError] = useState(""); const [message, setMessage] = useState("");
  async function load() { setMenus(await adminRequest<Menu[]>("/admin/menus")); }
  useEffect(() => { void load().catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Menus are unavailable.")); }, []);
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); setMessage("");
    try {
      const input = { ...form, validFrom: form.validFrom || null, validUntil: form.validUntil || null };
      const result = await adminRequest<Menu>(selected ? `/admin/menus/${selected.id}` : "/admin/menus", selected ? "PUT" : "POST", input);
      await load(); setSelected(result); setForm(result); setNewMenu(false); setMessage("Menu saved.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Menu could not be saved."); }
  }
  async function publish(menu: Menu) {
    try { await adminRequest(`/admin/menus/${menu.id}/publication`, "PATCH", { published: !menu.published }); await load(); setMessage(menu.published ? "Menu unpublished." : "Menu published."); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Menu status could not be changed."); }
  }
  async function addCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selected) return;
    const values = new FormData(event.currentTarget);
    try {
      await adminRequest(`/admin/menus/${selected.id}/categories`, "POST", {
        name: values.get("name"), slug: values.get("slug"), description: values.get("description"), displayOrder: Number(values.get("displayOrder")),
      });
      await load(); const refreshed = await adminRequest<Menu[]>("/admin/menus"); const next = refreshed.find((m) => m.id === selected.id) ?? null;
      setSelected(next); setMessage("Category added.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Category could not be added."); }
  }
  async function addItem(event: FormEvent<HTMLFormElement>, categoryId: string) {
    event.preventDefault(); if (!selected) return;
    const formElement = event.currentTarget; const values = new FormData(formElement);
    try {
      await adminRequest(`/admin/menus/${selected.id}/categories/${categoryId}/items`, "POST", {
        name: values.get("name"), description: values.get("description"), price: Number(values.get("price")),
        imageUrl: values.get("imageUrl") || null, vegetarian: values.has("vegetarian"), vegan: values.has("vegan"),
        glutenFree: values.has("glutenFree"), featured: false, available: true, seasonal: false, displayOrder: 0, allergens: [],
      });
      await load(); const refreshed = await adminRequest<Menu[]>("/admin/menus"); setSelected(refreshed.find((m) => m.id === selected.id) ?? null);
      formElement.reset(); setMessage("Menu item added.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Menu item could not be added."); }
  }
  async function toggleItem(item: Item) {
    try {
      await adminRequest(`/admin/menu-items/${item.id}`, "PUT", { ...item, available: !item.available });
      await load(); if (selected) { const refreshed = await adminRequest<Menu[]>("/admin/menus"); setSelected(refreshed.find((m) => m.id === selected.id) ?? null); }
      setMessage(item.available ? "Item archived." : "Item is available.");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Item availability could not be updated."); }
  }
  function edit(menu: Menu) {
    setSelected(menu); setNewMenu(false);
    setForm({ name: menu.name, slug: menu.slug, description: menu.description, menuType: menu.menuType,
      validFrom: menu.validFrom ?? "", validUntil: menu.validUntil ?? "", displayOrder: menu.displayOrder });
  }
  return <div className="admin-stack">
    <section className="admin-panel"><div className="admin-panel-heading"><div><span className="eyebrow">PUBLIC MENUS</span><h2>Seasonal selections</h2></div>
      <button className="button" onClick={() => { setSelected(null); setForm(blankMenu); setNewMenu(true); }}>Create menu</button></div>
      <p>Published menu and item changes flow directly to the public site.</p>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Name</th><th>Type</th><th>Items</th><th>Dates</th><th>Visibility</th><th /></tr></thead>
        <tbody>{menus.map((menu) => <tr key={menu.id}><td>{menu.name}<small>/{menu.slug}</small></td><td>{menu.menuType}</td>
          <td>{menu.categories.reduce((sum, category) => sum + category.items.length, 0)}</td><td>{menu.validFrom || "—"} – {menu.validUntil || "—"}</td>
          <td><span className="admin-status">{menu.published ? "Published" : "Draft"}</span></td>
          <td><button className="admin-link-button" onClick={() => edit(menu)}>Edit</button> <button className="admin-link-button" onClick={() => void publish(menu)}>{menu.published ? "Unpublish" : "Publish"}</button></td></tr>)}</tbody></table></div>
      {error && <p className="admin-error" role="alert">{error}</p>}{message && <p className="admin-success" role="status">{message}</p>}
    </section>
    {(selected || newMenu) && <section className="admin-panel"><span className="eyebrow">{newMenu ? "CREATE" : "EDIT"}</span><h2>{newMenu ? "New menu" : selected?.name}</h2>
      <form className="admin-form-grid" onSubmit={save}>
        <label>Name<input required maxLength={100} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
        <label>URL slug<input required maxLength={100} pattern="[a-z0-9-]+" value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} /></label>
        <label>Menu type<input required maxLength={30} value={form.menuType} onChange={(e) => setForm({ ...form, menuType: e.target.value })} /></label>
        <label>Display order<input type="number" min={0} value={form.displayOrder} onChange={(e) => setForm({ ...form, displayOrder: Number(e.target.value) })} /></label>
        <label>Valid from<input type="date" value={form.validFrom ?? ""} onChange={(e) => setForm({ ...form, validFrom: e.target.value })} /></label>
        <label>Valid until<input type="date" value={form.validUntil ?? ""} onChange={(e) => setForm({ ...form, validUntil: e.target.value })} /></label>
        <label className="admin-form-wide">Description<textarea required maxLength={5000} rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
        <div className="admin-form-wide admin-actions"><button className="button">Save menu</button><button type="button" onClick={() => { setSelected(null); setNewMenu(false); }}>Close</button></div>
      </form>
      {selected && <div className="admin-stack">
        <form className="admin-form-grid admin-subform" onSubmit={addCategory}><strong className="admin-form-wide">Add category</strong>
          <label>Name<input name="name" required maxLength={100} /></label><label>Slug<input name="slug" required pattern="[a-z0-9-]+" /></label>
          <label>Description<input name="description" maxLength={1000} /></label><label>Display order<input name="displayOrder" type="number" min={0} defaultValue={selected.categories.length} /></label>
          <button className="admin-form-wide">Add category</button></form>
        {selected.categories.map((category) => <section className="admin-category-block" key={category.id}><h3>{category.name}</h3>
          <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Dish</th><th>Price</th><th>Dietary</th><th>Availability</th><th /></tr></thead><tbody>
            {category.items.map((item) => <tr key={item.id}><td>{item.name}<small>{item.description}</small></td><td>${Number(item.price).toFixed(2)}</td>
              <td>{[item.vegetarian && "Vegetarian", item.vegan && "Vegan", item.glutenFree && "Gluten-free"].filter(Boolean).join(", ") || "—"}</td>
              <td>{item.available ? "Available" : "Archived"}</td><td><button className="admin-link-button" onClick={() => void toggleItem(item)}>{item.available ? "Archive" : "Restore"}</button></td></tr>)}
          </tbody></table></div>
          <form className="admin-form-grid admin-subform" onSubmit={(event) => void addItem(event, category.id)}>
            <strong className="admin-form-wide">Add dish to {category.name}</strong>
            <label>Name<input name="name" required maxLength={120} /></label><label>Price<input name="price" type="number" min="0" step="0.01" required /></label>
            <label className="admin-form-wide">Description<input name="description" required maxLength={4000} /></label>
            <label>Image URL<input name="imageUrl" type="url" /></label>
            <label className="admin-checkbox"><input name="vegetarian" type="checkbox" /> Vegetarian</label>
            <label className="admin-checkbox"><input name="vegan" type="checkbox" /> Vegan</label>
            <label className="admin-checkbox"><input name="glutenFree" type="checkbox" /> Gluten-free</label>
            <button className="admin-form-wide">Add dish</button>
          </form>
        </section>)}
      </div>}
    </section>}
  </div>;
}
