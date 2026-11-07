# Offline Capability & Resilience Test Report

---

## 1. Test Conditions & Matrix
Simulated network isolation (Wi-Fi and Cellular data disconnected).

| Functionality | Offline Mechanism | Test Result | Remarks |
| :--- | :--- | :--- | :--- |
| **Campus Map Rendering** | Local Assets (`buildings.geojson`, `roads.geojson`, `paths.geojson`) | **PASS** | Renders 100% of campus polygons and paths without network requests. |
| **Building Directory Search** | Local In-Memory & Room Cache (`campus_locations`) | **PASS** | Instant autocomplete search results across all campus buildings. |
| **Pedestrian Navigation** | Local `CampusGraph` + A* Shortest Path Engine | **PASS** | Calculates routes and turn-by-turn guidance entirely on-device. |
| **Saved Locations / Bookmarks** | Local SQLite Room Database (`srmap_campus_nav.db`) | **PASS** | Bookmarks persist across force-stop, app termination, and reboot. |
| **GPS Tracking** | Native Android `LocationManager` hardware provider | **PASS** | Operates on direct GNSS satellite fixes without internet assist. |

---

## 2. Cold Boot Under Zero Connectivity
- App cold-launched with zero network connections.
- Launch time to interactive canvas: **< 320ms**.
- GeoJSON parsing time: **~38ms**.
- Database load time: **~12ms**.
- Zero crash dialogs, zero uncaught network exceptions.
