import { EditorialImage } from "@/components/ui/EditorialImage";
import { Reveal } from "@/components/ui/Reveal";

export function PageHero({
  eyebrow,
  title,
  description,
  image,
  alt,
  compact = false,
}: {
  eyebrow: string;
  title: string;
  description?: string;
  image?: string;
  alt?: string;
  compact?: boolean;
}) {
  if (image) {
    return (
      <section className={`page-hero image-hero ${compact ? "page-hero-compact" : ""}`}>
        <div className="image-fill"><EditorialImage src={image} alt={alt ?? ""} priority /></div>
        <div className="hero-shade" />
        <Reveal className="page-hero-copy page-hero-copy-light">
          <span className="eyebrow">{eyebrow}</span>
          <h1>{title}</h1>
          {description && <p>{description}</p>}
        </Reveal>
      </section>
    );
  }
  return (
    <section className={`page-hero page-hero-paper ${compact ? "page-hero-compact" : ""}`}>
      <Reveal className="page-hero-copy">
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </Reveal>
      <span className="hero-rule" aria-hidden="true" />
    </section>
  );
}
