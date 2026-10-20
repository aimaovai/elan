import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ExperienceDetail } from "@/components/experiences/ExperienceDetail";
import { ContentState } from "@/components/ui/ContentState";
import { PageHero } from "@/components/layout/PageHero";
import { getExperience } from "@/lib/api/client";

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const result = await getExperience(slug);
  return {
    title: result.data?.name ?? "Seasonal experience",
    description: result.data?.shortDescription ?? "Explore a seasonal dining experience at Élan.",
    alternates: { canonical: `/experiences/${slug}` },
  };
}

export default async function ExperiencePage({ params }: Props) {
  const { slug } = await params;
  const result = await getExperience(slug);
  if (result.error?.includes("404")) notFound();
  if (!result.data) {
    return <><PageHero eyebrow="THE ÉLAN EXPERIENCES" title="An evening in season." compact /><div className="page-container menu-detail-section"><ContentState message={result.error} /></div></>;
  }
  return <ExperienceDetail experience={result.data} />;
}
