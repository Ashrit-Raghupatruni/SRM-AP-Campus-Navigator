# SRM AP Campus Navigator — Architecture & Engineering Specification

## 1. Overview & System Purpose

**SRM AP Campus Navigator** is a high-performance, offline-first pedestrian navigation and spatial intelligence Android application engineered specifically for the SRM University–AP campus in Andhra Pradesh.

The system combines real-world GIS vector boundaries, indoor room directories, faculty registries, multi-floor wayfinding, and an on-device pedestrian routing engine into a unified Material 3 mobile application.

---

## 2. Architecture Diagram (MVVM + Clean Architecture)

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│                                                                        │
│   Explore Screen         Search Screen       Floor Explorer            │
│   - Canvas Vector Map    - Category Chips    - Block Selector          │
│   - 3D Isometric Extr.   - Recent Queries    - Floor (F1–F10) Switcher │
│   - MapTiler Base Styles - Live Suggestions  - Room & Lab Pinpoint     │
│                                                                        │
│               ▲                       ▲                     ▲          │
│               │                       │                     │          │
│      CampusViewModel           SearchViewModel      NavigationViewModel │
│     (StateFlow UI State)     (StateFlow UI State)   (Dynamic A* Route)  │
└───────┬───────────────────────────────┬─────────────────────┬──────────┘
        │                               │                     │
┌───────▼───────────────────────────────▼─────────────────────▼──────────┐
│                             DOMAIN LAYER                               │
│                                                                        │
│  - PedestrianRoutingEngine (A* Graph Search)                           │
│  - PedestrianMapMatcher (Topological Hysteresis Snapping)              │
│  - DynamicRouteProgress (Dual-Color Polyline Slicing)                  │
│  - GpsFilterEngine (1D Adaptive Kalman Filter)                         │
│  - UnifiedSearchResult Ranking & Deduplication                         │
└───────┬───────────────────────────────┬─────────────────────┬──────────┘
        │                               │                     │
┌───────▼───────────────────────────────▼─────────────────────▼──────────┐
│                              DATA LAYER                                │
│                                                                        │
│   CampusRepositoryImpl                 SearchRepositoryImpl            │
│   - GeoJsonAssetParser (Real GIS)      - FTS5 Full-Text Matcher        │
│   - StaticCampusDataSource (Demo/Seed) - Exact vs Partial Ranking       │
│                                                                        │
│           ┌──────────────────────────────────────────────┐             │
│           │               LOCAL PERSISTENCE              │             │
│           │                                              │             │
│           │  Room DB (AppDatabase)    Asset GeoJSONs     │             │
│           │  - blocks, rooms          - buildings.geojson│             │
│           │  - faculty, equipment     - campus_all.geojson│            │
│           │  - recent_searches        - paths.geojson    │             │
│           │  - favorite_locations     - roads.geojson    │             │
│           └──────────────────────────────────────────────┘             │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Selective Isometric 3D Building Highlight (V1)

### Visual Specification
When a destination building is selected or navigated to:
1. **Target Building Elevation:** The selected building dynamically elevates into an isometric 3D block with:
   - **Raised Roof Polygon:** Translated along an extrusion offset vector.
   - **Wall Facades (Quads):** Connected between the ground footprint and the elevated roof vertices.
   - **Directional Shading:** Side walls are shaded based on edge normal angles with respect to a simulated overhead light source.
   - **Ground Contact Shadow:** Soft drop shadow offset cast beneath the building.
   - **Border Contour:** High-contrast stroke defining the building silhouette.
2. **2D Campus Context:** All other campus buildings retain flat 2D rendering with standard category fills and strokes, preventing visual clutter.
3. **Smooth Interpolation:** Animated using Compose `Animatable(0f)` with fast-out-slow-in easing over 350ms.

### Geometric Extrusion Math
```kotlin
fun calculateExtrusionOffset(depthPx: Float, angleDegrees: Float): Offset {
    val rad = Math.toRadians(angleDegrees.toDouble())
    return Offset(
        (depthPx * cos(rad)).toFloat(),
        (depthPx * sin(rad)).toFloat()
    )
}
```
Wall quad normals in 2D screen space:
$$\vec{N} = (-dy, dx)$$
Lighting factor:
$$L = \frac{\sin(\theta_{\text{normal}}) + 1}{2}$$

---

## 4. Multi-Floor Indoor & Vertical Directions

Indoor navigation combines outdoor pedestrian routing with vertical step-by-step guidance:
- **Floor Switcher:** Interactive dynamic floor picker supporting ground to 10th floor (CV Raman Tower).
- **Floor-Aware Badges:** Visual floor level badges (`Floor 1`, `Floor 2`, etc.) in search cards and destination sheets.
- **Vertical Step Transition:** When a user arrives at the building entrance, navigation switches to indoor staircase/elevator instructions specifying the target floor and room door.

---

## 5. Universal Search & SQLite FTS5 Engine

- **Categorized Directory:** Default browse mode when query is empty:
  - `ALL`: Highlights across classrooms, faculty, and equipment.
  - `ROOMS`: Lecture halls, smart classrooms, and labs with floor numbers and capacities.
  - `FACULTY`: Faculty directory with designations, departments, cabin codes, and university email.
  - `LABS`: Research facilities, instrumentation suites, and cleanrooms.
  - `EQUIPMENT`: Scientific instruments and laboratory equipment.
- **Search Ranking Order:**
  1. Exact Room Number match (case & hyphen-insensitive).
  2. Exact Faculty Name / Cabin match.
  3. Prefix token matches.
  4. Partial substring matches across metadata.
- **Recent Searches:** Persisted in local database with 1-tap re-querying and clearing.

---

## 6. Pedestrian Routing & Dynamic Progress Slicing

- **A* Algorithm:** Evaluates pedestrian network topology over `paths.geojson` and `roads.geojson`.
- **Dual-State Polyline Rendering:**
  - **Traversed Polyline:** Slate Grey (`#64748B`) behind user.
  - **Remaining Polyline:** Electric Blue (`#0284C7`) ahead of user.
- **Dynamic Recalculation:** Automatic re-route when deviation exceeds 25 meters for 3 consecutive fixes.
- **Arrival Detection:** Auto-complete when within 12 meters of destination entrance.
