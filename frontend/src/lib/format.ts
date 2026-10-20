export function money(value: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
  }).format(value);
}

export function dateRange(start: string, end: string): string {
  const from = new Date(`${start}T12:00:00`);
  const to = new Date(`${end}T12:00:00`);
  const formatter = new Intl.DateTimeFormat("en-US", { month: "long", day: "numeric", year: "numeric" });
  return `${formatter.format(from)} – ${formatter.format(to)}`;
}
