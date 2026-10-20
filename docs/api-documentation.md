# Public API

Base path: `/api/v1`. All endpoints are read-only and return JSON. Swagger UI is available at `/swagger-ui.html`; the OpenAPI document is at `/api-docs`.

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
| GET | `/actuator/health` | Spring Boot health status |

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

Unknown or unpublished slugs return `404` using `{ "timestamp", "status", "error", "message", "path" }`. Unexpected server errors return a sanitized generic message. CORS allows only the configured frontend origin and GET on `/api/v1/**`.
