# Phase status

## Completed in this implementation

- Next.js App Router foundation with all requested public routes, shared navigation/footer, editorial styling, responsive layouts, reduced-motion support, page metadata, sitemap, robots, and image failure fallback.
- Typed API client for menus, featured menu items, experiences, featured experience, private dining spaces, gallery, and restaurant settings. API failures are presented as explicit errors, not successful mock data.
- Spring Boot API with DTO mapping, JPA entities/repositories, read-only service methods, CORS origin restriction, sanitized JSON error responses, Actuator health, and Swagger/OpenAPI.
- PostgreSQL schema and deterministic fictional content managed by Flyway, including five menus, menu items, dietary flags/allergens, three experiences and course progressions, three private dining spaces, and twelve gallery records.
- Reservation availability is calculated from restaurant-local service periods, booking limits, active single-table inventory, blocking statuses, and date/time closures.
- Reservation creation locks active table rows within the database transaction, revalidates overlap, assigns the most capacity-efficient table, and stores a confirmed reservation, guest, secure management-token hash, and status history.
- Reservation creation uses a required hashed idempotency key, rejects conflicting key reuse, returns the existing reservation on retry, and rotates the returned management credential.
- Guests can view, modify, or cancel a reservation using a private token sent in a request header; cancellation cutoff and modification availability are enforced server-side.
- Private-dining inquiries are validated against selected-space capacity, persisted separately from reservations, and explicitly returned as unconfirmed inquiries.
- The public reservation and private dining forms call the actual backend APIs and display validation/conflict outcomes without fake success states.

## Structure and run commands

- `frontend/`: `npm install`; `npm run dev`; checks: `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`.
- `backend/`: `mvn spring-boot:run`; checks: `mvn test`, `mvn verify`.
- Root: `docker compose up -d postgres`.

## Implemented APIs and database

See [api-documentation.md](api-documentation.md) and [database-schema.md](database-schema.md). Flyway owns the schema and seed data; Hibernate validates it on startup.

## Known limitations and unresolved checks

- This is a demonstration site; booking/inquiry data is fictional and local demo data only. No real physical address or monitored email exists.
- Administrative authentication, role-based operations/dashboard, table/menu/experience/settings management, inquiry operations, notification delivery, and rate limiting are not implemented.
- The reservation engine supports single-table assignment only. It serializes bookings by locking all active tables, appropriate to the small demo inventory but not a high-throughput production approach.
- Image URLs reference Unsplash-hosted assets and are not bundled. If an image request fails, the frontend renders an accessible fallback panel.
- PostgreSQL-backed validation was completed using an isolated temporary PostgreSQL 16 container on an alternate local port. The default Compose port 5433 was already allocated, so no shared service was stopped or changed.
- Automated integration tests against PostgreSQL/Testcontainers and browser E2E tests are not part of the current changes.
- Browser-based visual checks were not run.

## Verification completed

- `frontend`: `npm run typecheck`, `npm run lint`, `npm test` (3 files / 3 tests), and `npm run build` — passed.
- `backend`: `mvn test` — passed (9 tests total). Reservation tests cover availability, overlap/adjacency, inactive tables, party capacity, idempotent replay, and conflicting key reuse.
- Fresh PostgreSQL 16 startup applied Flyway migrations V1–V4 and passed Hibernate schema validation.
- API smoke flow passed: query live availability, create and replay an idempotent reservation, verify key mismatch returns 409, retrieve and modify it with the rotated management token, cancel it, and submit an inquiry that remained `NEW` / unconfirmed.
- `git diff --check` — passed.
- Browser E2E tests were not run.
