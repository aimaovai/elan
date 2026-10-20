"use client";

import { useEffect, useRef, useState } from "react";
import { apiMutation } from "@/lib/api/client";

type Slot = { time: string; available: boolean };
type Availability = { date: string; partySize: number; timezone: string; availableSlots: Slot[] };
type Confirmation = {
  confirmationCode: string;
  status: string;
  reservationDate: string;
  reservationTime: string;
  endTime: string;
  partySize: number;
  tableName: string;
  experienceName: string | null;
  managementToken: string;
};

function localDate() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

export function ReservationFlow() {
  const [date, setDate] = useState(localDate);
  const [partySize, setPartySize] = useState(2);
  const [availability, setAvailability] = useState<Availability | null>(null);
  const [selectedTime, setSelectedTime] = useState("");
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const idempotencyKey = useRef<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoadingSlots(true);
    setAvailability(null);
    setSelectedTime("");
    setError("");
    fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1"}/reservations/availability?date=${encodeURIComponent(date)}&partySize=${partySize}`, {
      cache: "no-store",
      headers: { Accept: "application/json" },
    }).then(async (response) => {
      if (!response.ok) throw new Error(`Availability could not be loaded (${response.status}).`);
      return response.json() as Promise<Availability>;
    }).then((result) => {
      if (!cancelled) setAvailability(result);
    }).catch((reason: unknown) => {
      if (!cancelled) setError(reason instanceof Error ? reason.message : "Availability could not be loaded.");
    }).finally(() => {
      if (!cancelled) setLoadingSlots(false);
    });
    return () => { cancelled = true; };
  }, [date, partySize]);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedTime || submitting) return;
    const values = new FormData(event.currentTarget);
    idempotencyKey.current ??= crypto.randomUUID();
    setSubmitting(true);
    setError("");
    try {
      const result = await apiMutation<Confirmation>("/reservations", "POST", {
        firstName: values.get("firstName"),
        lastName: values.get("lastName"),
        email: values.get("email"),
        phoneNumber: values.get("phoneNumber"),
        reservationDate: date,
        reservationTime: selectedTime,
        partySize,
        specialOccasion: values.get("specialOccasion") || null,
        dietaryNotes: values.get("dietaryNotes") || null,
        specialRequests: values.get("specialRequests") || null,
      }, { "Idempotency-Key": idempotencyKey.current });
      sessionStorage.setItem("elan-reservation-management-token", result.managementToken);
      setConfirmation(result);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Your reservation could not be completed.");
    } finally {
      setSubmitting(false);
    }
  }

  if (confirmation) {
    return (
      <section className="booking-panel" aria-live="polite">
        <span className="eyebrow">YOUR TABLE IS RESERVED</span>
        <h2>We look forward to welcoming you.</h2>
        <p>Confirmation <strong>{confirmation.confirmationCode}</strong> · {confirmation.reservationDate} at {confirmation.reservationTime} · {confirmation.partySize} guests</p>
        <p className="demo-note">Élan is a fictional demonstration restaurant. This booking is recorded in the demo system only.</p>
        <a className="button" href="/reservations/manage">View or manage reservation</a>
      </section>
    );
  }

  return (
    <form className="booking-panel" onSubmit={submit} onChange={() => { idempotencyKey.current = null; }}>
      <span className="eyebrow">A PLACE AT THE TABLE</span>
      <h2>Find an evening to remember.</h2>
      <p className="demo-note">Élan is a fictional demonstration restaurant. Bookings are stored in the local demo system and do not reserve a real restaurant.</p>
      <div className="form-grid">
        <label>Party size
          <select value={partySize} onChange={(event) => setPartySize(Number(event.target.value))}>
            {Array.from({ length: 8 }, (_, index) => index + 1).map((size) => <option key={size} value={size}>{size} {size === 1 ? "guest" : "guests"}</option>)}
          </select>
        </label>
        <label>Date
          <input type="date" value={date} min={localDate()} onChange={(event) => setDate(event.target.value)} required />
        </label>
      </div>
      <fieldset className="slot-picker">
        <legend>Available times</legend>
        {loadingSlots ? <p role="status">Checking availability…</p> : availability?.availableSlots.some((slot) => slot.available)
          ? <div className="slot-options">{availability.availableSlots.filter((slot) => slot.available).map((slot) => (
            <button className="slot-button" type="button" aria-pressed={selectedTime === slot.time} key={slot.time} onClick={() => { idempotencyKey.current = null; setSelectedTime(slot.time); }}>{slot.time}</button>
          ))}</div>
          : <p>No reservation times are available for this date and party size. Please choose another date.</p>}
      </fieldset>
      <div className="form-grid">
        <label>First name<input name="firstName" autoComplete="given-name" maxLength={80} required /></label>
        <label>Last name<input name="lastName" autoComplete="family-name" maxLength={80} required /></label>
        <label>Email<input name="email" type="email" autoComplete="email" maxLength={254} required /></label>
        <label>Phone<input name="phoneNumber" type="tel" autoComplete="tel" maxLength={32} required /></label>
        <label>Occasion
          <select name="specialOccasion" defaultValue=""><option value="">No special occasion</option><option value="BIRTHDAY">Birthday</option><option value="ANNIVERSARY">Anniversary</option><option value="PROPOSAL">Proposal</option><option value="OTHER">Other</option></select>
        </label>
        <label className="form-wide">Dietary notes<textarea name="dietaryNotes" rows={3} maxLength={1000} /></label>
        <label className="form-wide">Special requests<textarea name="specialRequests" rows={3} maxLength={1000} /></label>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="button" type="submit" disabled={!selectedTime || submitting || loadingSlots}>
        {submitting ? "Confirming…" : "Confirm reservation"}
      </button>
    </form>
  );
}
