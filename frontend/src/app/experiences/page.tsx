import type { Metadata } from "next";
import { PageHero } from "@/components/layout/PageHero";
import { ContentState } from "@/components/ui/ContentState";
import { ExperienceCard } from "@/components/experiences/ExperienceCard";
import { getExperiences } from "@/lib/api/client";

export const metadata: Metadata = {
  title: "Seasonal experiences",
  description: "Discover Élan's seasonal tasting experiences and courses.",
  alternates: { canonical: "/experiences" },
};

export default async function ExperiencesPage() {
  const result = await getExperiences();
  return (
    <>
      <PageHero eyebrow="BEYOND THE EVERYDAY" title="Experiences in season." description="A more intimate way to follow the kitchen: thoughtful progressions, considered pairings, and time around the table." image="https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=2000&q=90" alt="An intimate restaurant dining room" />
      <section className="experience-list page-container">
        {!result.data ? <ContentState message={result.error} /> : result.data.length
          ? result.data.map((experience, index) => <ExperienceCard key={experience.id} experience={experience} index={index} />)
          : <ContentState kind="empty" message="New experiences are being prepared for the changing season." />}
      </section>
    </>
  );
}
