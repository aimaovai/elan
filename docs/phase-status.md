# Phase status

## Completed in this implementation

- Next.js App Router foundation with all requested public routes, shared navigation/footer, editorial styling, responsive layouts, reduced-motion support, page metadata, sitemap, robots, and image failure fallback.
- Typed API client for menus, featured menu items, experiences, featured experience, private dining spaces, gallery, and restaurant settings. API failures are presented as explicit errors, not successful mock data.
- Spring Boot API with DTO mapping, JPA entities/repositories, read-only service methods, CORS origin restriction, sanitized JSON error responses, Actuator health, and Swagger/OpenAPI.
- PostgreSQL schema and deterministic fictional content managed by Flyway, including five menus, menu items, dietary flags/allergens, three experiences and course progressions, three private dining spaces, and twelve gallery records.
- Reservations and private-dining pages clearly state that no booking or inquiry is operational.

## Structure and run commands

- `frontend/`: `npm install`; `npm run dev`; checks: `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`.
- `backend/`: `mvn spring-boot:run`; checks: `mvn test`, `mvn verify`.
- Root: `docker compose up -d postgres`.

## Implemented APIs and database

See [api-documentation.md](api-documentation.md) and [database-schema.md](database-schema.md). Flyway owns the schema and seed data; Hibernate validates it on startup.

## Known limitations and unresolved checks

- This is a demonstration site; no real physical address, monitored email, booking, or inquiry submission exists.
- Image URLs reference Unsplash-hosted assets and are not bundled. If an image request fails, the frontend renders an accessible fallback panel.
- PostgreSQL-backed startup and visual browser checks depend on local Docker/network availability. Docker CLI was present, but the Docker Desktop Linux engine was not running during this session.
- Automated integration tests against PostgreSQL/Testcontainers and browser E2E tests are not part of this phase.
- Runtime API endpoints, Flyway execution against PostgreSQL, and schema validation could not be exercised because the Docker engine was unavailable.

## Verification completed

- `npm run typecheck` — passed.
- `npm run lint` — passed.
- `npm test` — passed, 2 test files / 2 tests.
- `npm run build` — passed; all public routes compiled.
- `mvn -q verify` — passed; Java tests and Maven verification completed.
- Production HTTP smoke test — `/`, `/menus`, `/experiences`, `/private-dining`, `/reservations`, and `/contact` each returned HTTP 200.
- `docker compose up -d postgres` — blocked: Docker Desktop Linux engine pipe was unavailable.

## Phase 2 extension points

Add reservations and inquiry workflows as separately validated service/controller domains. Introduce availability, reservation, guest, and inquiry records with new Flyway migrations; implement conflict handling and notifications only with persistent services. Add authentication before admin write endpoints. Keep Phase 1 public controllers read-only and never treat client-selected availability as a booking.
