# Architecture

```text
Next.js App Router (server-rendered public pages)
        ↓ typed HTTP API
Spring Boot REST API (/api/v1)
        ↓ validated DTOs and transactional domain services
Spring Data JPA + Hibernate
        ↓ Flyway-managed schema
PostgreSQL
```

## Responsibilities

- **Frontend:** Presents the editorial brand, public routes, accessible interactions, SEO metadata, loading/error/empty states, API-driven content, reservations, and private dining inquiries. Reservation changes are submitted to the backend and are never treated as successful based only on a client-side selection.
- **Controller:** Defines public content, booking, guest-management, and private-dining request contracts using DTOs and request validation. Administrative endpoints have not yet been added.
- **Service:** Read services apply content visibility rules. The reservation service applies restaurant-local booking rules, deterministic capacity-first assignment, overlap checks, idempotency, and guest-token authorization within database transactions. Inquiry submission persists a separate unconfirmed request.
- **Repository/entity:** Uses JPA entity graphs for the aggregate relationships required by each read. Lazy associations are not serialized directly; Open Session in View is disabled.
- **Flyway/PostgreSQL:** Versioned migrations define constraints, indexes, relationships, deterministic demonstration content, service periods, tables, guests, reservations, idempotency records, status history, blocked periods, and inquiries. Hibernate runs in schema-validation mode.

## Booking consistency and guest access

Availability is advisory. Reservation creation takes pessimistic locks on active table rows in stable ID order, then checks overlapping active reservations again before persisting the assignment. This serializes allocation for the seeded single-table inventory; it is intentionally not the high-throughput strategy for a large restaurant. Idempotency keys are stored as hashes with a 24-hour replay window, and replays rotate the management token.

Guest management tokens are cryptographically random, stored as SHA-256 hashes, and sent in a request header rather than a URL. Private inquiry records have a separate persistence model and do not consume reservation capacity. Both public pages clearly identify Élan as fictional demonstration content.

## Remaining extension points

Administration, secure staff authentication, role authorization, management screens, notifications/outbox, rate limiting, concurrent PostgreSQL integration tests, and operational reporting remain future work. Do not expose guest PII in public content routes or add admin write routes before the authentication boundary exists.
