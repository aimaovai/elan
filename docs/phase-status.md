# Phase status

## Implemented

- Public restaurant content, reservation availability/booking/guest management, and private dining inquiry submission.
- Spring Security session authentication at `/api/v1/admin/auth/login`, `/logout`, `/me`, and `/csrf`, with BCrypt passwords, HttpOnly session cookies, CSRF tokens, session fixation protection, role-based method authorization, inactive-account rejection, and environment-only initial administrator bootstrap.
- Role-aware administration at `/admin`: database-backed dashboard/alerts, reservation listing/detail/status/notes/manual creation/rescheduling/table reassignment, day/week calendar, table management with upcoming booking views, menu/category/item management, experience/course management, private dining inquiry management, weekly service periods/closures, reservation-setting controls, and notification delivery status.
- Admin operational APIs validate reservation/inquiry state changes, preserve historical reservations when tables are deactivated, prevent capacity reductions that conflict with upcoming bookings, and record administrative changes. Demo-viewer booking results redact guest names/contact fields.
- Flyway V5–V8 add administrator accounts, administrative audit/inquiry notes, the durable email outbox, and additional hashed reservation management tokens. Existing guest management tokens remain SHA-256 hashed.
- Reservation create/modify/cancel and private-dining submission write notification records in the same transaction. A scheduled worker claims rows with `FOR UPDATE SKIP LOCKED`, leases processing rows, retries transient failures with bounded delays, and uses stable Resend idempotency keys.
- Resend supports explicit `log`, `test`, and `live` modes. Log mode is labeled `SIMULATED`; test mode redirects all messages to its configured recipient; live mode requires explicit enablement. API keys remain backend-only. Confirmation-link tokens are AES-GCM encrypted at rest when the encryption key is configured and placed in a URL fragment; the management page immediately removes the fragment before using the token in its existing request header.
- Public Next.js content fetches are uncached so published admin edits are reflected without a deployment. Admin and guest-management responses/pages are configured not to cache and not to send referrers.
- Request limits cover login, reservation creation, availability, guest-token management, and inquiry submission. The limiter uses bounded process-local memory and remote socket addresses only.
- PostgreSQL Testcontainers coverage exercises repeated concurrent competing bookings, rollback of losing guest inserts, conflicting modifications/closures/service-hour changes, dashboard data, table validation, login rate limiting, unauthenticated admin access, role restrictions, and CSRF enforcement. Notification unit tests cover log-mode simulation, HTML escaping, inquiry semantics, and encryption-key handling.
- Next.js and Vitest were upgraded to security-fixed major versions; ESLint now uses flat configs without the vulnerable Next lint-plugin glob dependency chain.

## Remaining constraints

- The request limiter is single-process/demo-grade. Multi-instance deployments need a shared limiter such as Redis before production traffic.
- Resend delivery has not been exercised against a real account or verified sender domain; no API key was supplied or stored. Configure DNS/domain verification and explicit backend environment values before live delivery.
- Provider acceptance is not inbox delivery. The admin notification page is read-only; retrying terminal events is intentionally not exposed because Resend idempotency retention is 24 hours and an older ambiguous retry could duplicate a message.
- Email templates clearly label the fictional demonstration. The administrator notification requires an explicit `PRIVATE_DINING_ADMIN_EMAIL`.
- Admin settings, weekly service periods, and full/partial blocked dates are editable with checks that reject changes conflicting with upcoming bookings. Manual reservations, rescheduling, and table reassignment route through the existing allocator and validate capacity/overlap.
- No payment/deposit integration or real restaurant operation is included.

## Local administrator setup

Set `ELAN_ADMIN_EMAIL` and `ELAN_ADMIN_PASSWORD` (at least 12 characters) in the backend process environment before its first startup. The password is encoded with BCrypt. The account is created only when the configured email is absent; there are no default credentials. If an administrator needs replacement, use a controlled database/admin provisioning process rather than placing passwords in Git.

## Email configuration

The root `.env.example` lists blank values only. Local default: `EMAIL_DELIVERY_MODE=log`, `RESEND_ENABLED=false`. For test mode, set a dedicated `RESEND_TEST_RECIPIENT`; for live mode, explicitly enable delivery, configure a verified `RESEND_FROM_EMAIL`, reply-to, and provider API key in the backend environment. Configure `ELAN_NOTIFICATION_ENCRYPTION_KEY` as a unique random secret (32 random bytes, base64 encoded) to include management links. Use HTTPS for `APP_PUBLIC_URL` and secure session cookies outside local development. Configure/verify the sender domain and DNS records in Resend before live use.

Resend idempotency is stable per outbox event and payload. Local outbox state remains authoritative; provider idempotency is documented as time-limited (24 hours), so the system does not claim exactly-once delivery after ambiguous failures beyond that window.

## Validation

- Backend `mvn test` passes 24 tests, including Testcontainers/PostgreSQL 16 migration and concurrency/security integration coverage.
- Frontend: `npm run typecheck`, `npm run lint`, `npm test`, and `npm run build`.
- Dependency audit was rerun after upgrades; the current package tree reports zero npm advisories.
- Browser-based visual checks and real Resend delivery were not run.
