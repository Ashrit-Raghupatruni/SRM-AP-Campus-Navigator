# MapTiler Cloud API Usage & Security Report

---

## 1. Key Security & Management
- **Key Location:** `local.properties` (`MAPTILER_API_KEY=bYMIT8EqQo17TMX8M66i`).
- **Build Ingestion:** Injected into compiled `BuildConfig.MAPTILER_API_KEY` via Gradle DSL.
- **Git Protection:** `local.properties` is listed in `.gitignore` and excluded from version control.

---

## 2. API Endpoint Status & Entitlements

| Endpoint | Method | Tier Status | Live Probe Result | Action Taken |
| :--- | :--- | :--- | :--- | :--- |
| `maps/streets-v2` | GET | Active (Free) | 200 OK | Default Vector Layer |
| `maps/satellite` | GET | Active (Free) | 200 OK | High-Res Aerial Layer |
| `maps/hybrid` | GET | Active (Free) | 200 OK | Hybrid Imagery Layer |
| `maps/topo-v2` | GET | Active (Free) | 200 OK | Topographic Layer |
| `maps/outdoor-v2` | GET | Active (Free) | 200 OK | Outdoor Contour Layer |
| `geocoding/{query}` | GET | Active (Free) | 200 OK | Regional Place Suggestions |
| `geolocation/ip` | GET | Active (Free) | 200 OK | City-Level Location Fallback |
| `tiles/terrain-rgb` | GET | Active (Free) | 200 OK | Terrain Elevation Calculations |
| `maps/.../static/` | GET | Paid Required | 403 Forbidden | Replaced with vector building cards |
| `weather/point/` | GET | Deprecated/Plan | 404 Not Found | Omitted cleanly |

---

## 3. Rate Limit & Network Optimization
- **Search Debounce:** Search queries debounced at `350ms` to prevent spamming the geocoding endpoint during typing.
- **Proximity Bias:** Pre-configured with coordinates `(80.5064, 16.4632)` to prioritize results nearest SRM University-AP.
- **Offline Fallback:** Network failures are caught silently with zero UI freeze.
