# Database schema

- `menus` 1—many `menu_categories` 1—many `menu_items`
- `menu_items` many—many `allergens` through `menu_item_allergens`
- `dining_experiences` 1—many `experience_courses`
- `private_dining_spaces` is an independent public content table
- `gallery_images` stores published image metadata and category
- `restaurant_settings` stores the public restaurant profile and policy copy
- `restaurant_settings` also stores timezone, booking-window, party-size, cancellation, and booking-enable rules.
- `dining_tables` stores configurable single-table capacity and active state.
- `restaurant_service_periods` and `restaurant_blocked_dates` define bookable local service windows and full/partial closures.
- `dining_experience_table_restrictions` optionally constrains an experience to assigned table inventory; `dining_experiences.reservation_duration_minutes` overrides the standard service duration.
- `reservation_guests` stores guest contact information; `reservations` references a guest, assigned table, optional experience, interval, status, and a hash of the guest management token.
- `reservation_status_history` preserves guest and administrative reservation status transitions, optionally identifying the acting administrator.
- `reservation_idempotency_records` binds a hashed client key and payload hash to one reservation with a 24-hour replay window.
- `private_dining_inquiries` stores requests separately from reservations; inquiry status never allocates dining capacity.

Primary keys use UUIDs. Slugs are unique, menu/category and experience/course relationships are constrained, and monetary values use `NUMERIC(8,2)`. Ordering and visibility columns are indexed where list reads rely on them. Menu, category, experience, and course foreign keys preserve referential integrity; child content is deleted only with its owning aggregate. All menu dates, course counts, capacity, and price values are constrained. Migrations `V1`–`V3` create the schema and populate reproducible fictional demo data.

Migration `V4` introduces the initial reservation and inquiry schema while preserving Phase 1 content. Database checks protect capacity, date, status, and input-domain invariants. Reservation overlap is rechecked within a transaction after pessimistically locking active table rows; no PostgreSQL-specific range extension is required. The current demo uses a single-table allocation model and deterministic capacity-first assignment. Expired idempotency records are removed opportunistically when reservation requests are processed.

Migration `V5` adds administrator credentials/roles with case-insensitive email uniqueness. `V6` adds private inquiry internal notes and an administrative audit log. `V7` adds a durable outbox with provider idempotency keys, retry state, leasing, sanitized errors, and encrypted sensitive notification data. `V8` preserves multiple hashed guest management tokens so idempotent retries do not invalidate an already emailed link. No plaintext password or management token is stored in these tables. There are no payment/deposit tables.
