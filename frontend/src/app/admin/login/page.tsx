"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { adminLogin } from "@/lib/admin";

export default function AdminLoginPage() {
  const router = useRouter();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = new FormData(event.currentTarget);
    setBusy(true);
    setError("");
    try {
      await adminLogin(String(form.get("email")), String(form.get("password")));
      router.replace("/admin");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Sign-in could not be completed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="admin-login-wrap">
      <form className="admin-login-card" onSubmit={submit}>
        <span className="eyebrow">ÉLAN · OPERATIONS</span>
        <h1>Welcome back.</h1>
        <p>Sign in with your administrator account to continue.</p>
        <label>Email<input type="email" name="email" autoComplete="username" required maxLength={254} /></label>
        <label>Password<input type="password" name="password" autoComplete="current-password" required /></label>
        {error && <p className="admin-error" role="alert">{error}</p>}
        <button className="button" type="submit" disabled={busy}>{busy ? "Signing in…" : "Sign in securely"}</button>
        <small>ÉLAN is a fictional restaurant demonstration.</small>
      </form>
    </section>
  );
}
