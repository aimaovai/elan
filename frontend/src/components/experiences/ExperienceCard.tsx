import Link from "next/link";
import type { DiningExperience } from "@/types/content";
import { dateRange, money } from "@/lib/format";
import { EditorialImage } from "@/components/ui/EditorialImage";

export function ExperienceCard({ experience, index = 0 }: { experience: DiningExperience; index?: number }) {
  return (
    <article className={`experience-card ${index % 2 ? "experience-card-reverse" : ""}`}>
      <Link href={`/experiences/${experience.slug}`} className="experience-card-image image-frame">
        <EditorialImage src={experience.heroImageUrl} alt={`Atmosphere for ${experience.name}`} sizes="(max-width: 800px) 100vw, 58vw" />
        <span className="image-index">0{index + 1}</span>
      </Link>
      <div className="experience-card-copy">
        <span className="eyebrow">A SEASONAL EXPERIENCE</span>
        <h2><Link href={`/experiences/${experience.slug}`}>{experience.name}</Link></h2>
        <p>{experience.shortDescription}</p>
        <div className="experience-meta"><span>{experience.courseCount} courses</span><span>{money(experience.price)} per guest</span><span>{dateRange(experience.startDate, experience.endDate)}</span></div>
        <Link href={`/experiences/${experience.slug}`} className="text-link">Discover the experience <span aria-hidden="true">↗</span></Link>
      </div>
    </article>
  );
}
