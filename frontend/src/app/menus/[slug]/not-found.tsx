import Link from "next/link";

export default function MenuNotFound() {
  return <section className="closing-invitation page-container"><span className="eyebrow">A QUIET TURNING</span><h1>This menu has moved on.</h1><p>That menu could not be found. Explore the menus currently at the table.</p><Link className="button" href="/menus">Explore all menus</Link></section>;
}
