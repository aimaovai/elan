import type { Metadata } from "next";
import { PageHero } from "@/components/layout/PageHero";
import { ReservationManagement } from "@/components/reservations/ReservationManagement";

export const metadata: Metadata = {
  title: "Manage reservation",
  description: "View or update your Élan demonstration reservation.",
  robots: { index: false, follow: false },
  referrer: "no-referrer",
};

export default function ManageReservationPage() {
  return (
    <>
      <PageHero eyebrow="YOUR EVENING" title="A detail, thoughtfully arranged." description="Review or update your reservation." image="https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=2000&q=90" alt="A candlelit restaurant table" compact />
      <section className="page-container booking-section"><ReservationManagement /></section>
    </>
  );
}
