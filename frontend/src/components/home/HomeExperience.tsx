import Link from "next/link";
import type { DiningExperience } from "@/types/content";
import { money } from "@/lib/format";
import { EditorialImage } from "@/components/ui/EditorialImage";
import { ContentState } from "@/components/ui/ContentState";

export function HomeExperience({ experience, error }: { experience: DiningExperience | null; error: string | null }) {
  if (!experience) return <section className="home-experience"><div className="page-container"><ContentState message={error ?? "The current experience is not available."} /></div></section>;
  return (
    <section className="home-experience">
      <div className="home-experience-image image-frame"><EditorialImage src={experience.heroImageUrl} alt={`The atmosphere of ${experience.name}`} sizes="(max-width: 800px) 100vw, 52vw" /></div>
      <div className="home-experience-copy">
        <span className="eyebrow">THE CURRENT EXPERIENCE</span>
        <h2>{experience.name}</h2>
        <p>{experience.shortDescription}</p>
        <div className="experience-meta"><span>{experience.courseCount} courses</span><span>{money(experience.price)} per guest</span></div>
        {experience.winePairingPrice !== null && <p className="pairing-note">Optional wine pairing +{money(experience.winePairingPrice)}</p>}
        <Link href={`/experiences/${experience.slug}`} className="button button-light">Discover the experience</Link>
      </div>
    </section>
  );
}
