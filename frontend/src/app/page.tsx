import Link from "next/link";
import { ArrowDownRight } from "lucide-react";
import { EditorialImage } from "@/components/ui/EditorialImage";
import { Reveal } from "@/components/ui/Reveal";
import { SectionHeading } from "@/components/layout/SectionHeading";
import { HomeExperience } from "@/components/home/HomeExperience";
import { FeaturedDishes } from "@/components/home/FeaturedDishes";
import { getFeaturedExperience, getFeaturedItems } from "@/lib/api/client";

export const metadata = {
  alternates: { canonical: "/" },
};

export default async function HomePage() {
  const [experienceResult, itemsResult] = await Promise.all([getFeaturedExperience(), getFeaturedItems()]);
  return (
    <>
      <section className="home-hero">
        <div className="hero-image image-fill">
          <EditorialImage
            src="https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=2200&q=90"
            alt="A warmly lit dining room prepared for evening service"
            priority
          />
        </div>
        <div className="hero-shade" />
        <Reveal className="home-hero-copy">
          <span className="eyebrow">CONTEMPORARY DINING · NORTHBANK</span>
          <h1>ÉLAN</h1>
          <p className="hero-tagline">A seasonal expression of place, craft, and memory.</p>
          <p className="hero-support">An invitation to experience the extraordinary in the everyday.</p>
          <div className="hero-actions">
            <Link href="/reservations" className="button button-light">Reserve a table</Link>
            <Link href="/menus" className="button button-ghost">Explore our menus</Link>
          </div>
        </Reveal>
        <a className="scroll-cue" href="#introduction">Discover Élan <ArrowDownRight size={15} aria-hidden="true" /></a>
      </section>

      <section id="introduction" className="intro-section page-container">
        <span className="eyebrow">THE ART OF DINING</span>
        <h2>Every season tells a different story.</h2>
        <div className="intro-copy">
          <p>Élan celebrates the relationship between nature, creativity, and the table. Each menu follows the changing light, the work of thoughtful growers, and the quiet pleasure of being together.</p>
          <Link href="/about" className="text-link">The Élan philosophy <span aria-hidden="true">↗</span></Link>
        </div>
        <div className="intro-image image-frame">
          <EditorialImage src="https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1700&q=85" alt="A seasonal plate of fresh vegetables and herbs" sizes="(max-width: 760px) 100vw, 70vw" />
        </div>
      </section>

      <HomeExperience experience={experienceResult.data} error={experienceResult.error} />

      <section className="philosophy-section page-container">
        <div className="philosophy-layout">
          <div className="philosophy-image image-frame">
            <EditorialImage src="https://images.unsplash.com/photo-1556911220-e15b29be8c8f?auto=format&fit=crop&w=1400&q=85" alt="A cook at work in a softly lit kitchen" sizes="(max-width: 760px) 100vw, 50vw" />
          </div>
          <div className="philosophy-copy">
            <span className="eyebrow">OUR PHILOSOPHY</span>
            <h2>Ingredients first. Imagination always.</h2>
            <p>We begin with what is growing well. Technique gives each ingredient room to speak; hospitality makes the evening feel like your own.</p>
            <div className="philosophy-values"><span>Seasonality</span><span>Craft</span><span>Connection</span></div>
          </div>
        </div>
      </section>

      <section className="signature-section section-paper">
        <div className="page-container">
          <SectionHeading eyebrow="FROM OUR KITCHEN" title="A study in flavor." description="A few impressions from the current menu, selected by our kitchen." />
          <FeaturedDishes items={itemsResult.data} error={itemsResult.error} />
          <div className="section-link-row"><Link href="/menus" className="text-link">Explore the menus <span aria-hidden="true">↗</span></Link></div>
        </div>
      </section>

      <section className="chef-preview">
        <div className="chef-preview-image image-frame">
          <EditorialImage src="https://images.unsplash.com/photo-1577219491135-ce391730fb2c?auto=format&fit=crop&w=1400&q=85" alt="A chef preparing a dish in the restaurant kitchen" sizes="(max-width: 760px) 100vw, 50vw" />
        </div>
        <div className="chef-preview-copy">
          <span className="eyebrow">BEHIND THE EXPERIENCE</span>
          <h2>Guided by curiosity. Grounded in craft.</h2>
          <p>For Chef Adrien Laurent, the best ideas begin at the market and find their shape at the stove. His cooking is generous, precise, and always in conversation with the season.</p>
          <Link href="/about" className="text-link">Meet the chef <span aria-hidden="true">↗</span></Link>
        </div>
      </section>

      <section className="private-preview">
        <div className="image-fill"><EditorialImage src="https://images.unsplash.com/photo-1514933651103-005eec06c04b?auto=format&fit=crop&w=2000&q=90" alt="An intimate private dining room set for an evening" sizes="100vw" /></div>
        <div className="hero-shade" />
        <div className="private-preview-copy">
          <span className="eyebrow">PRIVATE DINING</span>
          <h2>Moments worth gathering for.</h2>
          <p>An intimate setting for celebrations, meaningful conversations, and evenings that deserve a little more care.</p>
          <Link href="/private-dining" className="button button-light">Explore private dining</Link>
        </div>
      </section>

      <section className="closing-invitation page-container">
        <span className="eyebrow">A PLACE AT THE TABLE</span>
        <h2>Your table awaits.</h2>
        <p>Join us for an evening shaped by exceptional ingredients and thoughtful hospitality.</p>
        <Link href="/reservations" className="button">Reservation information</Link>
      </section>
    </>
  );
}
