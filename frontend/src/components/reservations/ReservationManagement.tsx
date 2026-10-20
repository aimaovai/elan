"use client";

import { useEffect, useState } from "react";
import { apiMutation } from "@/lib/api/client";

type ManagedReservation = {
  confirmationCode: string;
  status: string;
  reservationDate: string;
  reservationTime: string;
  partySize: number;
  tableName: string;
  experienceName: string | null;
  specialOccasion: string | null;
  dietaryNotes: string | null;
  specialRequests: string | null;
  cancellationPolicy: string;
};

const API = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export function ReservationManagement() {
  const [token, setToken] = useState("");
  const [reservation, setReservation] = useState<ManagedReservation | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const url = new URL(window.location.href);
    const linkedToken = url.hash.startsWith("#token=") ? decodeURIComponent(url.hash.slice(7)) : null;
    const savedToken = linkedToken ?? sessionStorage.getItem("elan-reservation-management-token") ?? "";
    if (linkedToken) {
      sessionStorage.setItem("elan-reservation-management-token", linkedToken);
      window.history.replaceState(null, "", `${url.pathname}${url.search}`);
    }
    setToken(savedToken);
    if (!savedToken) {
      setError("This browser session does not contain a reservation management link. Open this page from your booking confirmation.");
      return;
    }
    fetch(`${API}/reservations/manage`, { cache: "no-store", headers: { "X-Reservation-Management-Token": savedToken } })
      .then(async (response) => {
        if (!response.ok) throw new Error("This reservation link is invalid or has expired.");
        return response.json() as Promise<ManagedReservation>;
      })
      .then(setReservation)
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Reservation details could not be loaded."));
  }, []);

  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!reservation || busy) return;
    const values = new FormData(event.currentTarget);
    setBusy(true);
    setError("");
    try {
      const updated = await apiMutation<ManagedReservation>("/reservations/manage", "PATCH", {
        reservationDate: values.get("reservationDate"),
        reservationTime: values.get("reservationTime"),
        partySize: Number(values.get("partySize")),
        dietaryNotes: values.get("dietaryNotes") || null,
        specialRequests: values.get("specialRequests") || null,
      }, { "X-Reservation-Management-Token": token });
      setReservation(updated);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The reservation could not be changed.");
    } finally {
      setBusy(false);
    }
  }

  async function cancel() {
    if (!window.confirm("Cancel this reservation? This action cannot be undone.")) return;
    setBusy(true);
    setError("");
    try {
      await apiMutation<void>("/reservations/manage", "DELETE", undefined, { "X-Reservation-Management-Token": token });
      setReservation((current) => current ? { ...current, status: "CANCELLED" } : current);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The reservation could not be cancelled.");
    } finally {
      setBusy(false);
    }
  }

  if (error && !reservation) return <section className="booking-panel"><p className="form-error" role="alert">{error}</p><a href="/reservations" className="text-link">Return to reservations</a></section>;
  if (!reservation) return <section className="booking-panel"><p role="status">Loading reservation…</p></section>;

  return (
    <section className="booking-panel">
      <span className="eyebrow">RESERVATION {reservation.confirmationCode}</span>
      <h2>{reservation.status === "CANCELLED" ? "This reservation is cancelled." : "Your evening at Élan."}</h2>
      <p>{reservation.cancellationPolicy}</p>
      <form className="form-grid" onSubmit={save}>
        <label>Date<input type="date" name="reservationDate" defaultValue={reservation.reservationDate} required disabled={reservation.status === "CANCELLED"} /></label>
        <label>Time<input type="time" name="reservationTime" defaultValue={reservation.reservationTime} required disabled={reservation.status === "CANCELLED"} /></label>
        <label>Party size<input type="number" name="partySize" min={1} max={8} defaultValue={reservation.partySize} required disabled={reservation.status === "CANCELLED"} /></label>
        <label className="form-wide">Dietary notes<textarea name="dietaryNotes" defaultValue={reservation.dietaryNotes ?? ""} maxLength={1000} rows={3} disabled={reservation.status === "CANCELLED"} /></label>
        <label className="form-wide">Special requests<textarea name="specialRequests" defaultValue={reservation.specialRequests ?? ""} maxLength={1000} rows={3} disabled={reservation.status === "CANCELLED"} /></label>
        {error && <p className="form-error form-wide" role="alert">{error}</p>}
        {reservation.status !== "CANCELLED" && <div className="form-actions form-wide"><button className="button" type="submit" disabled={busy}>{busy ? "Saving…" : "Save changes"}</button><button className="button button-outline" type="button" onClick={cancel} disabled={busy}>Cancel reservation</button></div>}
      </form>
    </section>
  );
}
