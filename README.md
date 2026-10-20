# ÉLAN

ÉLAN is a fictional contemporary fine-dining restaurant experience built as a full-stack portfolio project. The Phase 1 foundation combines an editorial Next.js website with a read-only Spring Boot API backed by PostgreSQL.

## Stack

- **Web:** Next.js App Router, React, TypeScript, Tailwind CSS, Motion, Lucide
- **API:** Java 21, Spring Boot, Spring Web, Spring Data JPA, Hibernate, Flyway, springdoc OpenAPI
- **Data:** PostgreSQL 16

## Architecture

The `frontend/` application renders public pages and authenticated administration. The `backend/` owns domain rules, session security, persistence, and transactional notifications; PostgreSQL and Flyway hold the restaurant data and outbox.

See [docs/architecture.md](docs/architecture.md), [docs/database-schema.md](docs/database-schema.md), [docs/api-documentation.md](docs/api-documentation.md), and [docs/phase-status.md](docs/phase-status.md).

## Prerequisites

- Node.js 20.19 or newer (or 22.13+) and npm
- Java 21
- Maven 3.9+
- Docker Desktop / Docker Engine with Compose

## Local setup

1. Optionally copy the Compose configuration and copy the frontend example:

   ```powershell
   Copy-Item frontend/.env.example frontend/.env.local
   ```

   Compose and the API use matching local defaults if you do not create a root `.env`.
   If you change the database credentials, set the matching `SPRING_DATASOURCE_*` variables in the shell used to launch Maven.

2. Start PostgreSQL:

   ```powershell
   docker compose up -d postgres
   ```

3. Start the API in a terminal:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

   Flyway creates the schema and loads deterministic demo content at startup.

   For local administrator bootstrap, set `ELAN_ADMIN_EMAIL` and a unique `ELAN_ADMIN_PASSWORD` of at least 12 characters in the API process environment before startup. No administrator or default password is seeded. Bootstrap runs only when the account does not exist.

4. Start the website in another terminal:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

   Visit `http://localhost:3000`. The API is at `http://localhost:8080`; OpenAPI UI is at `http://localhost:8080/swagger-ui.html`, and health is at `http://localhost:8080/actuator/health`.

Environment variables are documented in [.env.example](.env.example) and [frontend/.env.example](frontend/.env.example). Never put production credentials in either file.

Admin access is at `/admin/login`. Admin sessions use HttpOnly cookies, CSRF protection, role checks, and a 30-minute idle timeout. The local CORS setup expects the frontend at `http://localhost:3000`; configure the exact deployed origin and set `SESSION_COOKIE_SECURE=true` for HTTPS. See [docs/phase-status.md](docs/phase-status.md) for current module scope and operational limitations.

Transactional mail is disabled by default: `EMAIL_DELIVERY_MODE=log` records outbox deliveries as `SIMULATED` and never contacts Resend. To explicitly enable `test` or `live`, configure the backend-only `RESEND_API_KEY`, a verified `RESEND_FROM_EMAIL`, and `RESEND_ENABLED=true`. Test mode sends only to `RESEND_TEST_RECIPIENT`. Do not put the API key in frontend environment variables. Generate a separate random, base64-encoded 32-byte key for `ELAN_NOTIFICATION_ENCRYPTION_KEY` if reservation management links should be included in confirmation emails; the backend rejects malformed or short key material.

## Commands

```powershell
# Frontend
cd frontend
npm run lint
npm run typecheck
npm test
npm run build

# Backend
cd backend
mvn test
mvn verify
```

## Phase 1 features

- Responsive editorial website for the homepage, menus, menu details, seasonal experiences, experience details, about, gallery, private dining, reservations, and contact.
- API-backed featured dishes and experiences, all menus, experience courses, gallery metadata, private dining spaces, and restaurant information.
- Menu type and dietary filters, accessible mobile navigation, reduced-motion-aware transitions, loading/error/empty states, page metadata, sitemap, and robots rules.
- PostgreSQL schema and deterministic demo data managed by Flyway.

All address, contact, chef, hours, and hospitality content is fictional demonstration data. It does not represent an operating restaurant. Image sources are remote Unsplash image URLs and are not restaurant-owned photography.

## Phase roadmap

- **Phase 2 (in progress):** Reservation availability, booking, guest self-service, admin session authentication, operational admin APIs and screens, a transactional email outbox, and Resend delivery modes are implemented for the fictional demo. See the status document for remaining limits and verification.
- **Phase 3:** Deposit/payment processing, advanced policies, analytics, image management, E2E coverage, CI/CD, and deployment.

## Troubleshooting

- If pages show API-unavailable messages, check that PostgreSQL is healthy and Spring Boot is listening on port 8080.
- If Flyway reports a schema mismatch, use a fresh local database for this demo rather than disabling schema validation.
- If images cannot be reached, the pages retain their layout and descriptive image fallback.
- Ensure `NEXT_PUBLIC_API_BASE_URL` in `frontend/.env.local` points to the API origin.
