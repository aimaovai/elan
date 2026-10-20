import type { Metadata } from "next";
import { PageHero } from "@/components/layout/PageHero";
import { getRestaurant } from "@/lib/api/client";
import { ContentState } from "@/components/ui/ContentState";
import { ReservationFlow } from "@/components/reservations/ReservationFlow";

export const metadata: Metadata = {
  title: "Reservations",
  description: "Reservation information and dining guidance for the fictional Élan restaurant demonstration.",
  alternates: { canonical: "/reservations" },
};

export default async function ReservationsPage() {
  const result = await getRestaurant();
  return (
    <>
      <PageHero eyebrow="A PLACE AT THE TABLE" title="An evening at Élan." description="Reserve your place at the table." image="https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=2000&q=90" alt="A candlelit restaurant table" compact />
      <section className="page-container visit-layout">
        <div className="visit-panel">
          <span className="eyebrow">RESERVATION INFORMATION</span>
          <h2>A considered evening begins here.</h2>
          <p>Choose a date and party size to see live availability from the restaurant’s service hours, seating inventory, booking window, and existing reservations.</p>
          {!result.data ? <ContentState message={result.error} /> : <div className="reservation-notice"><h2>Reservation policy</h2><p>{result.data.reservationPolicy}</p></div>}
          <p className="demo-note">Élan is fictional. Confirmations are stored in the demonstration system and do not reserve a real restaurant.</p>
        </div>
        <div className="visit-panel">
          <span className="eyebrow">BEFORE YOUR VISIT</span>
          <h2>A few thoughtful notes.</h2>
          <div className="reservation-notice"><h2>Dress</h2><p>{result.data?.dressCode ?? "Dress guidance is unavailable while restaurant information is offline."}</p></div>
          <div className="reservation-notice"><h2>The tasting menu</h2><p>Allow time for the full progression. Please share dietary needs in advance with your reservation.</p></div>
          <div className="reservation-notice"><h2>Access</h2><p>Accessibility information is part of this demonstration. Please contact the fictional venue for further details.</p></div>
        </div>
      </section>
      <section className="page-container booking-section"><ReservationFlow /></section>
    </>
  );
}
