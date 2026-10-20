import type { Metadata } from "next";
import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import { ContentState } from "@/components/ui/ContentState";
import { EditorialImage } from "@/components/ui/EditorialImage";
import { getPrivateDiningSpaces } from "@/lib/api/client";

export const metadata: Metadata = {
  title: "Private dining",
  description: "Explore fictional private dining spaces at Élan, created for intimate gatherings and celebrations.",
  alternates: { canonical: "/private-dining" },
};

export default async function PrivateDiningPage() {
  const result = await getPrivateDiningSpaces();
  return (
    <>
      <PageHero eyebrow="GATHER AT ÉLAN" title="An occasion, exceptionally considered." description="An intimate setting for celebrations, meaningful conversations, and evenings made to linger." image="https://images.unsplash.com/photo-1514933651103-005eec06c04b?auto=format&fit=crop&w=2000&q=90" alt="An elegant private dining room ready for guests" />
      <section className="page-container">
        <div className="story-section">
          <div className="story-image image-frame"><EditorialImage src="https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1400&q=85" alt="A softly illuminated restaurant dining room" sizes="(max-width: 760px) 100vw, 50vw" /></div>
          <div className="story-copy"><span className="eyebrow">A PLACE TO COME TOGETHER</span><h2>Good company deserves good care.</h2><p>From a family celebration to a thoughtful dinner with colleagues, our imagined private spaces bring the warmth of Élan to gatherings of different sizes. Each setting is paired with a menu shaped around the season.</p><p>Élan is fictional. Space details are demonstration content, and inquiries are not currently accepted.</p></div>
        </div>
      </section>
      <section className="space-list page-container">
        {!result.data ? <ContentState message={result.error} /> : result.data.length
          ? result.data.map((space) => (
            <article className="space-card" key={space.id}>
              <div className="space-image image-frame"><EditorialImage src={space.imageUrl} alt={`${space.name} private dining space`} sizes="(max-width: 760px) 100vw, 50vw" /></div>
              <div className="space-copy"><span className="eyebrow">PRIVATE DINING</span><h2>{space.name}</h2><p>{space.description}</p><span className="space-capacity">Up to {space.capacity} guests</span><Link href="/contact" className="text-link">Plan a visit <span aria-hidden="true">↗</span></Link></div>
            </article>
          ))
          : <ContentState kind="empty" message="Private dining details are being prepared." />}
      </section>
      <section className="closing-invitation page-container"><span className="eyebrow">A NOTE ON INQUIRIES</span><h2>For a gathering, in time.</h2><p>Private dining inquiries are not yet open in this demonstration. No personal details are collected.</p><Link href="/contact" className="button">View visiting information</Link></section>
    </>
  );
}
