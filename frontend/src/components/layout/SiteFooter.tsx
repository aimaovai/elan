import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { getRestaurant } from "@/lib/api/client";
import { ContentState } from "@/components/ui/ContentState";

export async function SiteFooter() {
  const result = await getRestaurant();
  return (
    <footer className="site-footer">
      <div className="footer-main">
        <div className="footer-brand">
          <Link href="/" className="wordmark wordmark-footer">ÉLAN<span className="wordmark-dot">.</span></Link>
          <p>A seasonal expression<br />of place, craft, and memory.</p>
        </div>
        <div className="footer-column">
          <h2>Discover</h2>
          <Link href="/menus">Menus</Link>
          <Link href="/experiences">Experiences</Link>
          <Link href="/about">Our story</Link>
          <Link href="/gallery">Gallery</Link>
        </div>
        <div className="footer-column">
          <h2>Visit</h2>
          <Link href="/private-dining">Private dining</Link>
          <Link href="/reservations">Reservations</Link>
          <Link href="/contact">Contact & hours</Link>
          <Link href="/contact#privacy">Privacy & demo</Link>
          {result.data
            ? <p>{result.data.city}<br />{result.data.contactEmail}</p>
            : <ContentState message="Visit details are temporarily unavailable." />}
        </div>
        <div className="footer-invitation">
          <span className="eyebrow">AN EVENING, CONSIDERED</span>
          <Link href="/reservations" className="footer-arrow">Your table awaits <ArrowUpRight size={18} /></Link>
        </div>
      </div>
      <div className="footer-bottom">
        <span>© {new Date().getFullYear()} Élan</span>
        <span>A fictional restaurant created for demonstration. No reservations or inquiries are accepted.</span>
        <span>Made with intention</span>
      </div>
    </footer>
  );
}
