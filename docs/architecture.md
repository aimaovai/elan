# Architecture

```text
Next.js public site + authenticated /admin
        ↓ credentialed session requests with CSRF headers
Spring Security (HttpOnly session cookie, roles, rate limit)
        ↓ DTO validation and transactional domain services
Spring Data JPA + Flyway
        ↓
PostgreSQL (content, reservations, audits, notification outbox)
        ↓ leased scheduled dispatcher
Resend (explicit test/live mode) or local logging provider
```

## Responsibilities

- The frontend renders public content and admin operational views. Admin credentials are never placed in localStorage. Its API client sends cookies and CSRF tokens for admin mutations. Public content requests use `no-store` so admin publication changes do not wait for a Next.js data-cache interval.
- Admin APIs are authenticated server-side. `ADMIN`, `MANAGER`, `STAFF`, and `DEMO_VIEWER` permissions are enforced in Spring; admin-route visibility is not the authorization boundary. The bootstrap account is configured only through backend environment variables.
- Public reservation and inquiry endpoints do not use cookie authentication; their CSRF exclusions are narrow and their sensitive reservation-management capability is a random token sent in a dedicated header. Admin state-changing endpoints continue to require CSRF.
- Reservation allocation pessimistically locks active table rows in stable order and then rechecks overlaps. This is adequate for small demo inventory but serializes allocations and is not a high-throughput restaurant design.
- Business mutations and notification records share a database transaction. A scheduled worker claims outbox events with row-level skip-locking and a short lease, then delivers outside the claim transaction. Retries are bounded and provider errors are stored as sanitized categories.
- Email content is generated in the backend. The API key is never exposed to Next.js. A confirmation management token is encrypted in the outbox payload when the separately configured AES key is present; the raw token is not stored as notification template JSON. Management tokens themselves remain one-way SHA-256 hashes in PostgreSQL.
- Rate limiting is process-local and does not trust forwarded IP headers. It is suitable only for the documented local/demo topology; production multi-instance deployments must use a shared limiter.

## Persistence

Flyway V1–V4 provide restaurant content, reservations, and inquiries. V5–V8 add administrator accounts, audit fields, outbox delivery state, and multi-token guest management. Hibernate uses schema validation; it does not create or mutate production schema.

Private dining `BOOKED` status is administrative workflow only and does not allocate ordinary dining capacity. Menus and experiences are managed via authenticated APIs and published content is retrieved uncached by public pages.
