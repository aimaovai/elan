import type { Metadata } from "next";
import { PageHero } from "@/components/layout/PageHero";
import { getRestaurant } from "@/lib/api/client";
import { ContentState } from "@/components/ui/ContentState";

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
          <h2>Online reservations are being prepared for this demonstration experience.</h2>
          <p>No availability is displayed and no booking is being accepted. The future booking journey will include party size, date, time, guest details, preferences, and confirmation after a real availability service is implemented.</p>
          {!result.data ? <ContentState message={result.error} /> : <div className="reservation-notice"><h2>Reservation policy</h2><p>{result.data.reservationPolicy}</p></div>}
        </div>
        <div className="visit-panel">
          <span className="eyebrow">BEFORE YOUR VISIT</span>
          <h2>A few thoughtful notes.</h2>
          <div className="reservation-notice"><h2>Dress</h2><p>{result.data?.dressCode ?? "Dress guidance is unavailable while restaurant information is offline."}</p></div>
          <div className="reservation-notice"><h2>The tasting menu</h2><p>Allow time for the full progression. Please share dietary needs in advance when the booking service becomes available.</p></div>
          <div className="reservation-notice"><h2>Access</h2><p>Accessibility information is part of this demonstration. Please contact the restaurant only once the fictional venue is operational.</p></div>
        </div>
      </section>
    </>
  );
}
