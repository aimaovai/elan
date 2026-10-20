import Link from "next/link";

export default function NotFound() {
  return <section className="closing-invitation page-container"><span className="eyebrow">A QUIET DETOUR</span><h1>That page has moved on.</h1><p>Return to the table and discover what is in season.</p><Link href="/" className="button">Back to Élan</Link></section>;
}
