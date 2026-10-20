"use client";

export default function GlobalError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <section className="closing-invitation page-container">
      <span className="eyebrow">A MOMENT, PLEASE</span>
      <h1>We could not prepare this page.</h1>
      <p>Please try once more.</p>
      <button className="button" type="button" onClick={reset}>Try again</button>
    </section>
  );
}
