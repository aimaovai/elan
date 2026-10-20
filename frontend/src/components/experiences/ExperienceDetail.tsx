import Link from "next/link";
import type { DiningExperience } from "@/types/content";
import { dateRange, money } from "@/lib/format";
import { PageHero } from "@/components/layout/PageHero";
import { SectionHeading } from "@/components/layout/SectionHeading";

export function ExperienceDetail({ experience }: { experience: DiningExperience }) {
  return (
    <>
      <PageHero eyebrow={`${experience.courseCount} COURSES · SEASONAL TASTING`} title={experience.name}
        description={experience.shortDescription} image={experience.heroImageUrl} alt={`Dining atmosphere for ${experience.name}`} />
      <section className="experience-overview page-container">
        <div><span className="eyebrow">THE EVENING</span><p>{experience.fullDescription}</p></div>
        <dl className="experience-facts">
          <div><dt>Menu</dt><dd>{experience.courseCount} courses</dd></div>
          <div><dt>Price</dt><dd>{money(experience.price)} per guest</dd></div>
          {experience.winePairingPrice !== null && <div><dt>Wine pairing</dt><dd>+{money(experience.winePairingPrice)}</dd></div>}
          <div><dt>Presented</dt><dd>{dateRange(experience.startDate, experience.endDate)}</dd></div>
        </dl>
      </section>
      <section className="courses-section section-paper">
        <div className="page-container">
          <SectionHeading eyebrow="THE PROGRESSION" title="A story in courses." description="The sequence is an expression of the season; dishes may change with the harvest." align="center" />
          <ol className="course-list">
            {experience.courses.map((course) => (
              <li key={course.id}>
                <span className="course-number">{String(course.courseNumber).padStart(2, "0")}</span>
                <div><h3>{course.name}</h3><p>{course.description}</p></div>
              </li>
            ))}
          </ol>
          {!experience.courses.length && <p className="empty-copy">Course details are being prepared for this experience.</p>}
          <p className="allergen-disclaimer">Please share dietary needs with our team before visiting. The kitchen handles common allergens and cannot guarantee an allergen-free environment.</p>
        </div>
      </section>
      <section className="closing-invitation page-container">
        <span className="eyebrow">AN EVENING AT ÉLAN</span>
        <h2>Your table awaits.</h2>
        <p>Online reservations are being prepared for this demonstration experience.</p>
        <Link href="/reservations" className="button">Reservation information</Link>
      </section>
    </>
  );
}
