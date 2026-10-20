const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

type CsrfResponse = { token: string; headerName: string };

async function csrf(): Promise<CsrfResponse> {
  const response = await fetch(`${API_BASE_URL}/admin/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
    headers: { Accept: "application/json" },
  });
  if (!response.ok) throw new Error("A secure admin session could not be initialized.");
  return (await response.json()) as CsrfResponse;
}

export async function adminRequest<T>(path: string, method = "GET", body?: unknown,
                                      extraHeaders: Record<string, string> = {}): Promise<T> {
  const csrfResponse = method === "GET" ? null : await csrf();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    credentials: "include",
    cache: "no-store",
    headers: {
      Accept: "application/json",
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
      ...(csrfResponse ? { [csrfResponse.headerName]: csrfResponse.token } : {}),
      ...extraHeaders,
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  if (!response.ok) {
    let message = `The request could not be completed (${response.status}).`;
    try {
      const payload = (await response.json()) as { message?: string };
      if (payload.message) message = payload.message;
    } catch {
      // Keep the status message when the server response is not JSON.
    }
    throw new Error(response.status === 401 ? "Your admin session has expired. Please sign in again." : message);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export async function adminLogin(email: string, password: string) {
  const token = await csrf();
  const response = await fetch(`${API_BASE_URL}/admin/auth/login`, {
    method: "POST",
    credentials: "include",
    cache: "no-store",
    headers: { Accept: "application/json", "Content-Type": "application/json", [token.headerName]: token.token },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) throw new Error("Email or password is incorrect.");
  return (await response.json()) as AdminUser;
}

export type AdminUser = { email: string; displayName: string; role: string };
export type PageResult<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number };
