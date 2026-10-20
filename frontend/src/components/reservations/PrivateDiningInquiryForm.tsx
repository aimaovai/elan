"use client";

import { useState } from "react";
import { apiMutation } from "@/lib/api/client";
import type { PrivateDiningSpace } from "@/types/content";

type InquiryConfirmation = { referenceNumber: string; status: string; isConfirmedEvent: boolean };

export function PrivateDiningInquiryForm({ spaces }: { spaces: PrivateDiningSpace[] }) {
  const [confirmation, setConfirmation] = useState<InquiryConfirmation | null>(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    const form = event.currentTarget;
    const values = new FormData(form);
    setSubmitting(true);
    setError("");
    try {
      const result = await apiMutation<InquiryConfirmation>("/private-dining/inquiries", "POST", {
        firstName: values.get("firstName"),
        lastName: values.get("lastName"),
        email: values.get("email"),
        phoneNumber: values.get("phoneNumber"),
        eventType: values.get("eventType"),
        preferredDate: values.get("preferredDate"),
        alternateDate: values.get("alternateDate") || null,
        guestCount: Number(values.get("guestCount")),
        privateDiningSpaceId: values.get("privateDiningSpaceId") || null,
        budgetPerGuest: values.get("budgetPerGuest") ? Number(values.get("budgetPerGuest")) : null,
        dietaryRequirements: values.get("dietaryRequirements") || null,
        beveragePreferences: values.get("beveragePreferences") || null,
        eventDescription: values.get("eventDescription"),
        specialRequests: values.get("specialRequests") || null,
        preferredContactMethod: values.get("preferredContactMethod"),
      });
      setConfirmation(result);
      form.reset();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Your inquiry could not be submitted.");
    } finally {
      setSubmitting(false);
    }
  }

  if (confirmation) {
    return <section className="booking-panel" aria-live="polite"><span className="eyebrow">INQUIRY RECEIVED</span><h2>Thank you for considering Élan.</h2><p>We have received your private dining inquiry. Reference <strong>{confirmation.referenceNumber}</strong>.</p><p>Submitting an inquiry does not confirm a private event or reserve a date. Élan is a fictional demonstration restaurant.</p></section>;
  }

  return (
    <form className="booking-panel" onSubmit={submit}>
      <span className="eyebrow">PRIVATE DINING</span><h2>Tell us about your gathering.</h2>
      <p className="demo-note">An inquiry is not a confirmed event booking. Élan is a fictional demonstration restaurant; submissions are saved only in the local demo system.</p>
      <div className="form-grid">
        <label>First name<input name="firstName" autoComplete="given-name" maxLength={80} required /></label>
        <label>Last name<input name="lastName" autoComplete="family-name" maxLength={80} required /></label>
        <label>Email<input name="email" type="email" autoComplete="email" maxLength={254} required /></label>
        <label>Phone<input name="phoneNumber" type="tel" autoComplete="tel" maxLength={32} required /></label>
        <label>Event type<select name="eventType" defaultValue="CELEBRATION"><option value="BIRTHDAY">Birthday</option><option value="ANNIVERSARY">Anniversary</option><option value="CORPORATE">Corporate</option><option value="WEDDING_RELATED">Wedding related</option><option value="CELEBRATION">Celebration</option><option value="OTHER">Other</option></select></label>
        <label>Guests<input name="guestCount" type="number" min={1} max={200} required /></label>
        <label>Preferred date<input name="preferredDate" type="date" required /></label>
        <label>Alternate date<input name="alternateDate" type="date" /></label>
        <label>Private dining space<select name="privateDiningSpaceId" defaultValue=""><option value="">Let us recommend a space</option>{spaces.map((space) => <option key={space.id} value={space.id}>{space.name} (up to {space.capacity})</option>)}</select></label>
        <label>Budget per guest<input name="budgetPerGuest" type="number" min={0} step="0.01" /></label>
        <label>Preferred contact<select name="preferredContactMethod" defaultValue="EMAIL"><option value="EMAIL">Email</option><option value="PHONE">Phone</option></select></label>
        <label className="form-wide">Event details<textarea name="eventDescription" rows={4} maxLength={2000} required /></label>
        <label className="form-wide">Dietary requirements<textarea name="dietaryRequirements" rows={2} maxLength={1000} /></label>
        <label className="form-wide">Beverage preferences<textarea name="beveragePreferences" rows={2} maxLength={1000} /></label>
        <label className="form-wide">Special requests<textarea name="specialRequests" rows={2} maxLength={1000} /></label>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="button" type="submit" disabled={submitting}>{submitting ? "Submitting…" : "Send inquiry"}</button>
    </form>
  );
}
