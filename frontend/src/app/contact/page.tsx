import type { Metadata } from "next";
import { PageHero } from "@/components/layout/PageHero";
import { ContentState } from "@/components/ui/ContentState";
import { getRestaurant } from "@/lib/api/client";

export const metadata: Metadata = {
  title: "Contact & visit",
  description: "Fictional visiting details for the Élan restaurant website demonstration.",
  alternates: { canonical: "/contact" },
};

export default async function ContactPage() {
  const result = await getRestaurant();
  const restaurant = result.data;
  return (
    <>
      <PageHero eyebrow="FIND YOUR WAY" title="An evening, in good company." description="Visiting information for the Élan demonstration restaurant." compact />
      <section className="page-container visit-layout">
        <div className="visit-panel">
          <span className="eyebrow">THE NEIGHBORHOOD</span><h2>{restaurant?.city ?? "A fictional neighborhood."}</h2>
          <p>Élan is a fictional restaurant concept. No street address is supplied, and this page intentionally does not point to a real business or map location.</p>
          <div className="location-card"><span className="eyebrow">A PLACE IN THE CITY</span><p>A quiet neighborhood setting, imagined for this demonstration.</p></div>
        </div>
        <div className="visit-panel">
          <span className="eyebrow">PLAN A VISIT</span><h2>Considered details.</h2>
          {!restaurant && <ContentState message={result.error ?? "Restaurant information is unavailable."} />}
          <div className="reservation-notice"><h2>Hours</h2><p>{restaurant?.openingHours ?? "Hours are not available at this time."}</p></div>
          {restaurant && <div className="reservation-notice"><h2>Contact</h2><p>Fictional demonstration contact: {restaurant.contactEmail}. This address is not monitored.</p></div>}
          <div className="reservation-notice"><h2>Getting here</h2><p>Parking and transit guidance are illustrative. There is no physical venue at this demonstration address.</p></div>
          {restaurant && <div className="reservation-notice"><h2>Dress & etiquette</h2><p>{restaurant.dressCode} Please let your host know about access requirements once the fictional venue is operational.</p></div>}
          <div className="reservation-notice" id="privacy"><h2>Privacy & demonstration</h2><p>This demonstration does not collect guest details, accept reservations, or submit private dining inquiries. The restaurant and contact details are fictional.</p></div>
        </div>
      </section>
    </>
  );
}
