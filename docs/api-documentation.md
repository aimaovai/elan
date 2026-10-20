# API documentation

Base path: `/api/v1`. Swagger UI is available at `/swagger-ui.html`; the OpenAPI document is at `/api-docs`.

| Method | Path | Behavior |
|---|---|---|
| GET | `/menus` | Published menus with ordered categories and available/unavailable item metadata |
| GET | `/menus/featured-items` | Up to three featured, available items from published menus |
| GET | `/menus/{slug}` | Published menu, categories, items, dietary flags, and allergens |
| GET | `/experiences` | Published experiences whose configured end date has not passed, with course progression |
| GET | `/experiences/featured` | Featured published experience, or first published experience as fallback |
| GET | `/experiences/{slug}` | Published, non-expired experience and courses |
| GET | `/private-dining/spaces` | Active dining spaces |
| GET | `/private-dining/spaces/{slug}` | Active space by slug |
| GET | `/gallery?category={category}` | Published gallery entries, optionally category-filtered |
| GET | `/restaurant` | Public restaurant name, tagline, fictional demo hours, and policy copy |
| GET | `/reservations/availability?date=YYYY-MM-DD&partySize=2` | Calculate slots from service periods, configured limits, table inventory, existing bookings, and blocked dates. Optional `experienceId` checks the published experience date range. |
| POST | `/reservations` | Validate and persist a confirmed reservation with an allocated table. Requires an `Idempotency-Key` header and returns a management token in the response. |
| GET | `/reservations/manage` | View only the reservation authorized by `X-Reservation-Management-Token`. Response is `no-store`. |
| PATCH | `/reservations/manage` | Change date, time, party size, and guest preferences after rechecking inventory. |
| DELETE | `/reservations/manage` | Cancel a reservation when within the configured cancellation window. |
| POST | `/private-dining/inquiries` | Persist a private dining inquiry and return its reference; this is not a confirmed event. |
| GET | `/actuator/health` | Spring Boot health status |

Reservation creation accepts guest name, email, phone, date/time, party size, optional experience, special occasion, dietary notes, and special requests. The required 16–128 character idempotency key is hashed and scoped to its request; successful keys have a 24-hour replay window, and expired records are purged during subsequent reservation-create requests. Replaying the same key with a different payload returns HTTP 409. Replaying the same payload returns the existing reservation and rotates the management token. The confirmation response contains the confirmation code, assigned display name, status, and the management token. Keep that token private; the API stores only its SHA-256 hash, and the public management API accepts it in a header rather than in a URL. Management tokens expire after 180 days. Guest-specific responses are not cached.

Availability and reservation creation use the restaurant timezone (`America/Los_Angeles` in the demo seed), Tuesday-Saturday service periods, a 90-day advance window, a two-hour minimum lead time, 30-minute slots, and 120-minute default service durations. Experiences can have a configured duration and optional table restrictions; the seeded Chef's Counter is restricted to the counter table. Service settings and table inventory are stored in PostgreSQL. The booking transaction takes pessimistic locks on the active table rows in stable ID order before rechecking overlaps and saving, serializing allocation decisions for the small seeded inventory. Overlap uses half-open time intervals. Conflicts return HTTP 409 with `RESERVATION_SLOT_UNAVAILABLE`.

Inquiry requests validate the selected space's active state and capacity. A successful response has `isConfirmedEvent: false`; no event is booked and no inventory is blocked. This demo does not send email.

Mutation validation errors return HTTP 400. Business errors include a stable `code` field and use HTTP 409 or 422 as appropriate. Private guest details are never returned by public content endpoints.

Example item:

```json
{
  "id": "30000000-0000-0000-0000-000000000001",
  "name": "Citrus-Cured Hamachi",
  "description": "Green apple, finger lime, fennel pollen, and a clear broth of garden herbs.",
  "price": 28.00,
  "vegetarian": false,
  "vegan": true,
  "glutenFree": true,
  "featured": true,
  "available": true,
  "seasonal": true,
  "allergens": ["Fish"]
}
```

Unknown or unpublished slugs return `404` using `{ "timestamp", "status", "error", "message", "path" }`. Unexpected server errors return a sanitized generic message. CORS allows only the configured frontend origin, read methods, and the specific public mutation methods and token header used by the booking flow.
