import type { Metadata } from "next";
import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import { EditorialImage } from "@/components/ui/EditorialImage";

export const metadata: Metadata = {
  title: "Our story",
  description: "Learn about Élan's seasonal approach to cooking, thoughtful sourcing, and hospitality.",
  alternates: { canonical: "/about" },
};

export default function AboutPage() {
  return (
    <>
      <PageHero eyebrow="A SENSE OF PLACE" title="Our story is written in seasons." description="A contemporary table, grounded in its surroundings and open to the world." image="https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=2000&q=90" alt="The calm interior of a contemporary dining room" />
      <section className="story-section page-container">
        <div className="story-image image-frame"><EditorialImage src="https://images.unsplash.com/photo-1506976785307-8732e854ad03?auto=format&fit=crop&w=1400&q=85" alt="Freshly gathered seasonal produce" sizes="(max-width: 760px) 100vw, 50vw" /></div>
        <div className="story-copy"><span className="eyebrow">THE ÉLAN PHILOSOPHY</span><h2>Nature, creativity, and the table.</h2><p>Élan began with a simple belief: a meal can be both a thoughtful act of craft and an open invitation. We cook with the season, make room for the unexpected, and measure a good evening by how welcome it feels.</p><p>Our menus move with the farms, waters, and weather around us. Technique is present, but never louder than the ingredient itself.</p></div>
      </section>
      <section className="story-section story-section-reverse section-paper">
        <div className="story-image image-frame"><EditorialImage src="https://images.unsplash.com/photo-1577219491135-ce391730fb2c?auto=format&fit=crop&w=1400&q=85" alt="A chef finishing a dish at the pass" sizes="(max-width: 760px) 100vw, 50vw" /></div>
        <div className="story-copy"><span className="eyebrow">THE KITCHEN</span><h2>Curiosity, with both feet on the ground.</h2><p>Executive Chef Adrien Laurent’s cooking follows the market rather than a fixed signature. Years spent in small kitchens across Europe shaped his respect for precision; cooking close to growers taught him the value of restraint.</p><p>At Élan, he leads a team that tastes, listens, and keeps learning with every season. The restaurant and its people are fictional, created for this demonstration.</p></div>
      </section>
      <section className="story-section page-container">
        <div className="story-image image-frame"><EditorialImage src="https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1400&q=85" alt="An artful dish built around fresh garden ingredients" sizes="(max-width: 760px) 100vw, 50vw" /></div>
        <div className="story-copy"><span className="eyebrow">THOUGHTFUL SOURCING</span><h2>Good relationships make good food.</h2><p>We look for producers who care for their land and practice their craft with patience. Menus follow what is genuinely at its best; when an ingredient is not in season, we let it wait.</p><p>Our imagined dining room is warm, tactile, and intentionally unhurried—an understated setting where conversation can take its time.</p><Link href="/menus" className="text-link">Explore the menus <span aria-hidden="true">↗</span></Link></div>
      </section>
      <section className="closing-invitation page-container"><span className="eyebrow">COME AS YOU ARE</span><h2>We saved you a place.</h2><p>Online reservations are being prepared for this demonstration experience.</p><Link href="/reservations" className="button">Reservation information</Link></section>
    </>
  );
}
