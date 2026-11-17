# SRM AP Campus Navigator — Complete Technical Architecture & 3D Specification

## 1. System Vision & Architecture

**SRM AP Campus Navigator** is an offline-first spatial intelligence and navigation platform for SRM University–AP, Andhra Pradesh. Built natively on Android using **Kotlin**, **Jetpack Compose**, **OpenGL ES 3.0**, **Room (SQLite FTS5)**, and **Material 3**.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│                                                                        │
│   Explore Screen (2D)             First-Person Navigation (3D)         │
│   - Canvas Vector Map             - OpenGL ES 3.0 Real-time Shaders    │
│   - MapTiler 5-Style Basemaps     - 1.6m Walking Eye Height Camera     │
│   - 3D Isometric Building Pop     - Procedural 3D Trees & Sports Turf  │
│   - F1–F10 Floor Explorer         - Route Ribbons & Touch Look-Around  │
│                                                                        │
│               ▲                                 ▲                      │
│               │                                 │                      │
│      CampusViewModel                     NavigationViewModel           │
│     (StateFlow UI State)                (Dynamic A* Route & 2D/3D)     │
└───────┬─────────────────────────────────────────┬──────────────────────┘
        │                                         │
┌───────▼─────────────────────────────────────────▼──────────────────────┐
│                             DOMAIN LAYER                               │
│                                                                        │
│  - PedestrianRoutingEngine (A* Graph Search over surveyed paths)       │
│  - FirstPersonCameraController (1.6m eye height, lerp/slerp tracking)  │
│  - PedestrianMapMatcher (Topological Hysteresis Snapping)              │
│  - DynamicRouteProgress (Dual-Color Polyline Slicing)                  │
│  - GpsFilterEngine (1D Adaptive Kalman Filter)                         │
└───────┬─────────────────────────────────────────┬──────────────────────┘
        │                                         │
┌───────▼─────────────────────────────────────────▼──────────────────────┐
│                              DATA LAYER                                │
│                                                                        │
│   CampusRepositoryImpl                 3D Geometry Generators          │
│   - GeoJsonAssetParser (Real GIS)      - LocalCoordinateConverter (ENU)│
│   - StaticCampusDataSource (Seed/Demo) - PolygonTriangulator (Ear-clip)│
│   - FTS5 Search Repository             - BuildingMeshGenerator         │
│                                        - SportsGroundMeshGenerator     │
│                                        - TreeMeshGenerator             │
│                                        - RoadMeshGenerator             │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Metric Local Tangent Plane (ENU) Coordinate System

Geographic coordinates (WGS-84) are transformed into metric Cartesian space referenced to the SRM AP Campus Centroid:
- **Reference Origin:** Latitude $16.4632311^\circ \text{ N}$, Longitude $80.5064108^\circ \text{ E}$, Elevation $0.0\text{ m}$.
- **Axes Convention:**
  - $+X$: Easting (meters)
  - $+Y$: Up (meters, Ground Plane is $Y = 0$)
  - $+Z$: Southing / $-Z$: Northing (meters, matching OpenGL standard camera view towards $-Z$)

### Ellipsoid Formulas
At latitude $\phi_0 = 16.4632311^\circ$:
$$M(\phi_0) = \frac{a(1 - e^2)}{(1 - e^2 \sin^2 \phi_0)^{3/2}} \implies \text{Meters/deg Lat} \approx 110{,}649.3\text{ m}$$
$$N(\phi_0) = \frac{a}{\sqrt{1 - e^2 \sin^2 \phi_0}} \implies \text{Meters/deg Lon} \approx 106{,}708.4\text{ m}$$

---

## 3. Procedural 3D Environment Generation

### 3.1 Buildings & Facades (`BuildingMeshGenerator.kt`)
- Polygon footprints from `buildings.geojson` are converted into metric vertices.
- Walls are generated as quad polygons extruded to $H = \text{levels} \times 3.8\text{m}$.
- Roofs are closed via robust Ear-Clipping triangulation (`PolygonTriangulator.kt`).
- Destination blocks receive prominent SRM Crimson styling (`#A61B35`) with dark ruby roof highlights.

### 3.2 Sports Grounds & Athletics (`SportsGroundMeshGenerator.kt`)
- **Cricket Ground:** Light green grass oval (`#4ADE80`) with a center 22-yard earthy beige pitch strip (`#FDE68A`).
- **Football / Soccer Ground:** Emerald sports turf (`#22C55E`).
- **Athletics Running Track:** Brick red / terracotta oval track (`#DC2626`).
- **Basketball / Tennis / Volleyball Courts:** Distinct blue (`#38BDF8`) and orange (`#F97316`) court surfaces.

### 3.3 Procedural 3D Trees & Walking Avenues (`TreeMeshGenerator.kt`)
- All 16 surveyed tree zones (`landuse: forest`, `outdoor_seating: Tree Court Area`) are populated with procedural 3D trees:
  - **Trunk:** Bark brown octagonal prisms ($H = 1.8\text{m}$, $R = 0.22\text{m}$).
  - **Foliage:** Multi-tier forest green pyramidal cones ($H = 3.6\text{m}$, $R = 1.6\text{m}$).
- **Walkway Clearance Buffer:** Trees maintain $\ge 2.5\text{m}$ clearance from pedestrian paths, preserving unobstructed walking avenues between tree belts.

### 3.4 3D Walkways & Route Polyline (`RoadMeshGenerator.kt`)
- Walkway ribbons ($W = 3.2\text{m}$) generated from topological graph edges, elevated to $Y = 0.04\text{m}$ to prevent Z-fighting with the ground.
- Active navigation route rendered as a vivid electric blue ($W = 1.6\text{m}$, `#0284C7`) chevron strip ahead of the user.

---

## 4. First-Person Camera & Controls

- **Eye Level:** Constrained to $1.6\text{m}$ above the local ground plane.
- **Field of View:** $65^\circ$ vertical perspective projection with near plane $0.3\text{m}$ and far plane $500\text{m}$.
- **Tracking & Smoothing:**
  - Position lerp ($\alpha = 0.18$) smoothly advances the camera with GPS updates.
  - Heading shortest-path angular lerp ($\alpha = 0.12$) aligns the view to the current walking segment.
- **Touch Look-Around:** Users can drag horizontally to inspect surroundings; a 3.5s auto-recenter timer returns the orientation to forward travel.

---

## 5. Offline Operation & Verification
- 100% offline operation: All geometry is generated procedurally from bundled local assets (`buildings.geojson`, `campus_all.geojson`, `paths.geojson`, `roads.geojson`).
- Zero external 3D runtime dependencies or cloud APIs.
