# Architecture

```text
Next.js App Router (server-rendered public pages)
        ↓ typed HTTP GET
Spring Boot REST API (/api/v1)
        ↓ DTO mapping / read-only services
Spring Data JPA + Hibernate
        ↓ Flyway-managed schema
PostgreSQL
```

## Responsibilities

- **Frontend:** Presents the editorial brand, public routes, accessible interactions, SEO metadata, loading/error/empty states, and API-driven menu/experience/gallery/space content. Browser components are limited to the mobile menu, dietary selector, image fallback, and motion.
- **Controller:** Defines the public HTTP contract. It returns DTOs and provides no public administration or write routes.
- **Service:** Applies visibility rules, selects the featured experience (featured published record first, then first published record), and maps entity relationships into DTOs within a read-only transaction.
- **Repository/entity:** Uses JPA entity graphs for the aggregate relationships required by each read. Lazy associations are not serialized directly; Open Session in View is disabled.
- **Flyway/PostgreSQL:** Versioned migrations define constraints, indexes, relationships, and deterministic demonstration content. Hibernate runs in schema-validation mode.

## Extension points

Phase 2 booking and private-dining inquiry services should be added as separate domain services and validated request/response DTOs. Keep public content GET routes read-only. Add availability and reservation tables through new migrations; never present client-side date/time choices as confirmed availability. Authentication and administrative write routes belong behind a separate authorization boundary.
