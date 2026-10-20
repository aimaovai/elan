"use client";

import { useEffect, useState } from "react";
import { adminRequest, type PageResult } from "@/lib/admin";

type Notification = { id: string; type: string; aggregateType: string; aggregateId: string; recipientHint: string;
  status: string; attempts: number; lastAttemptAt: string | null; acceptedAt: string | null;
  providerMessageId: string | null; errorCategory: string | null; createdAt: string };
export default function NotificationsPage() {
  const [page, setPage] = useState<PageResult<Notification> | null>(null);
  const [error, setError] = useState("");
  useEffect(() => { adminRequest<PageResult<Notification>>("/admin/notifications?page=0&size=50")
    .then(setPage).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Notifications are unavailable.")); }, []);
  return <section className="admin-panel"><span className="eyebrow">DELIVERY OPERATIONS</span><h2>Transactional notifications</h2>
    <p>A provider-accepted message is not proof of inbox delivery. Log-mode rows are marked simulated.</p>
    {error && <p className="admin-error" role="alert">{error}</p>}
    <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Type</th><th>Reference</th><th>Recipient</th><th>Status</th><th>Attempts</th><th>Last attempt</th><th>Provider ref</th></tr></thead>
      <tbody>{page?.content.map((item) => <tr key={item.id}><td>{item.type.replaceAll("_", " ")}</td>
        <td>{item.aggregateType} · {item.aggregateId.slice(0, 8)}</td><td>{item.recipientHint}</td>
        <td><span className="admin-status">{item.status === "SENT" ? "Provider accepted" : item.status}</span>
          {item.errorCategory && <small>{item.errorCategory}</small>}</td><td>{item.attempts}</td>
        <td>{item.lastAttemptAt ?? "Not attempted"}</td><td>{item.providerMessageId ?? "—"}</td></tr>)}</tbody></table></div>
    {!page?.content.length && !error && <p className="admin-empty">No notifications have been queued.</p>}
  </section>;
}
