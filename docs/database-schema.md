# Database schema

- `menus` 1—many `menu_categories` 1—many `menu_items`
- `menu_items` many—many `allergens` through `menu_item_allergens`
- `dining_experiences` 1—many `experience_courses`
- `private_dining_spaces` is an independent public content table
- `gallery_images` stores published image metadata and category
- `restaurant_settings` stores the public restaurant profile and policy copy

Primary keys use UUIDs. Slugs are unique, menu/category and experience/course relationships are constrained, and monetary values use `NUMERIC(8,2)`. Ordering and visibility columns are indexed where list reads rely on them. Menu, category, experience, and course foreign keys preserve referential integrity; child content is deleted only with its owning aggregate. All menu dates, course counts, capacity, and price values are constrained. Migrations `V1`–`V3` create the schema and populate reproducible fictional demo data.

No reservation, payment, inquiry, or administrator tables are included in Phase 1.
