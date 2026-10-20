import type { Metadata } from "next";
import { PageHero } from "@/components/layout/PageHero";
import { ContentState } from "@/components/ui/ContentState";
import { EditorialImage } from "@/components/ui/EditorialImage";
import { getGallery } from "@/lib/api/client";

export const metadata: Metadata = {
  title: "Gallery",
  description: "A visual journal of the Élan dining room, kitchen, ingredients, and seasonal plates.",
  alternates: { canonical: "/gallery" },
};

export default async function GalleryPage() {
  const result = await getGallery();
  return (
    <>
      <PageHero eyebrow="A VISUAL JOURNAL" title="Details of the evening." description="A few glimpses of the places, gestures, and ingredients behind an Élan evening." compact />
      <section className="page-container">
        {!result.data ? <div className="gallery-grid"><ContentState message={result.error} /></div> : result.data.length
          ? <div className="gallery-grid">
            {result.data.map((image) => (
              <figure className="gallery-item" key={image.id}>
                <EditorialImage src={image.imageUrl} alt={image.altText} sizes="(max-width: 760px) 50vw, 40vw" />
                <figcaption className="gallery-caption">{image.title} · {image.category}</figcaption>
              </figure>
            ))}
          </div>
          : <div className="gallery-grid"><ContentState kind="empty" message="The gallery is being prepared." /></div>}
      </section>
    </>
  );
}
