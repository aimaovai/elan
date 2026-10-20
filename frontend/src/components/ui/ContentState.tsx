export function ContentState({ message, kind = "error" }: { message: string; kind?: "error" | "empty" }) {
  return (
    <div className={`content-state content-state-${kind}`} role={kind === "error" ? "alert" : "status"}>
      <p>{message}</p>
    </div>
  );
}
