import Link from "next/link";

export default function ExperienceNotFound() {
  return <section className="closing-invitation page-container"><span className="eyebrow">A SEASONAL PAUSE</span><h1>This experience has passed.</h1><p>Explore the experiences currently taking shape in our kitchen.</p><Link className="button" href="/experiences">Discover experiences</Link></section>;
}
