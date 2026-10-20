# ÉLAN

ÉLAN is a fictional contemporary fine-dining restaurant experience built as a full-stack portfolio project. The Phase 1 foundation combines an editorial Next.js website with a read-only Spring Boot API backed by PostgreSQL.

## Stack

- **Web:** Next.js App Router, React, TypeScript, Tailwind CSS, Motion, Lucide
- **API:** Java 21, Spring Boot, Spring Web, Spring Data JPA, Hibernate, Flyway, springdoc OpenAPI
- **Data:** PostgreSQL 16

## Architecture

The `frontend/` application renders public pages and retrieves dynamic menus, experiences, dining spaces, gallery content, and restaurant details from the Java API. The `backend/` owns domain rules and persistence; controllers expose DTOs, services map JPA aggregates, and repositories query PostgreSQL. Flyway owns schema changes and repeatable demo data. Public endpoints are read-only; reservations, inquiries, and administration remain Phase 2 work.

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

4. Start the website in another terminal:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

   Visit `http://localhost:3000`. The API is at `http://localhost:8080`; OpenAPI UI is at `http://localhost:8080/swagger-ui.html`, and health is at `http://localhost:8080/actuator/health`.

Environment variables are documented in [.env.example](.env.example) and [frontend/.env.example](frontend/.env.example). Never put production credentials in either file.

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

- **Phase 2:** Reservation availability and booking lifecycle, private-dining inquiry submissions, secure admin authentication, and content management. The read API, domain entities, DTO boundary, and route composition are extension points; no booking or inquiry is represented as operational.
- **Phase 3:** Deposit/payment processing, advanced policies, analytics, image management, E2E coverage, CI/CD, and deployment.

## Troubleshooting

- If pages show API-unavailable messages, check that PostgreSQL is healthy and Spring Boot is listening on port 8080.
- If Flyway reports a schema mismatch, use a fresh local database for this demo rather than disabling schema validation.
- If images cannot be reached, the pages retain their layout and descriptive image fallback.
- Ensure `NEXT_PUBLIC_API_BASE_URL` in `frontend/.env.local` points to the API origin.
