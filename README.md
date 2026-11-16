<div align="center">
  <img src="./docs/images/app_logo.png" alt="SRM AP Campus Navigator Logo" width="160" />
  <h1>SRM AP Campus Navigator</h1>
  <p><strong>A production-grade, offline-first pedestrian navigation, room-level spatial intelligence, and 3D campus discovery application for SRM University–AP</strong></p>
</div>

---

## 🌟 Overview

**SRM AP Campus Navigator** is a native Android application built with **Jetpack Compose**, **Compose Canvas**, **Room (SQLite FTS5)**, and **Material 3**. Designed specifically for students, faculty, staff, and campus visitors, it delivers offline-first university campus navigation, multi-floor indoor wayfinding, and directory search down to individual room doors, faculty cabins, and laboratory equipment.

---

## 📱 Key Features

### 🏢 Selective Isometric 3D Building Highlight (V1)
- **Focused Elevation:** When you select a destination building or start navigation, **only that destination building** visually elevates into a 3D isometric structure.
- **Directional Shading & Lighting:** Side walls are shaded dynamically according to their spatial orientation and surface normals relative to overhead lighting.
- **Contact Drop Shadow & Outlines:** Realistic contact shadow cast beneath the raised structure with high-contrast boundary contours.
- **Pure Vector & Native Compose:** Zero external 3D engines (no Mapbox 3D, Unity, Blender, or GLTF runtimes). Rendered using custom vector math and Compose `Canvas`.
- **2D Context Preservation:** Surrounding campus blocks retain clean, flat 2D vector geometry to avoid visual distraction.

### 🧭 Multi-Floor Indoor Wayfinding & Floor Explorer
- **Interactive Floor Explorer:** Dedicated floor-by-floor browser supporting blocks up to 10 floors (e.g. CV Raman Academic Tower, SR Block, V-Block, X-Lab, Homi J. Bhabha).
- **Floor-Level Search Badges:** Search results display clear floor chips (e.g., `Floor 2`, `Floor 4`).
- **Vertical Step Navigation:** Navigating to an indoor location guides the user from their outdoor GPS coordinates to the block entrance, followed by explicit vertical instructions (staircase/elevator and floor door).

### 🔍 Universal Campus Directory & FTS5 Search
- **Instant Default Directory:** When the search bar is empty, browse categorized campus listings:
  - **ALL:** Highlights across classrooms, faculty, and research equipment.
  - **ROOMS:** High-capacity lecture halls, smart classrooms, and tutorial rooms with capacities.
  - **FACULTY:** Directory of professors, assistant professors, and heads of department with cabin locations and emails.
  - **LABS:** Specialized research centers, computing labs, and cleanrooms.
  - **EQUIPMENT:** Direct catalog of instruments (electron microscopes, GPU servers, laser benches, 3D printers) and their housing labs.
- **Recent Searches:** Offline history with 1-tap re-search and clear history options.
- **Sub-Millisecond Search:** Powered by SQLite `FTS5` with exact-match prioritization.

### 🗺️ High-Precision Campus Map & MapTiler Basemaps
- **Real-World GIS Boundaries:** Direct vector rendering of verified university GIS polygons (`buildings.geojson`, `paths.geojson`, `roads.geojson`).
- **5 Basemap Styles:** One-tap toggle between **Vector Default**, **Satellite**, **Hybrid**, **Topographic**, and **Outdoor** MapTiler layers.
- **Fluid Camera Interaction:** Multi-touch pinch-to-zoom, pan, floating zoom buttons, compass north re-alignment, and recenter-to-user triggers.

### 🚶 A* Pedestrian Navigation & Dynamic Progress Slicing
- **Topological Pedestrian Routing:** A* algorithm routed exclusively along surveyed pedestrian footpaths and roads.
- **Dual-State Dynamic Polyline:**
  - **Traversed Route:** Rendered in muted slate grey (`#64748B`) behind user.
  - **Remaining Route:** Rendered in vivid electric blue (`#0284C7`) ahead of user.
- **Turn-by-Turn Bottom Sheet:** Displays remaining distance, estimated walking time, and live step cues.
- **Auto-Recalculation & Arrival:** Automatically re-routes if the user deviates >25 meters, and signals arrival when within 12 meters.

### 📡 GNSS Quality Checking, Kalman Filtering & Map Matching
- **Hardware Sensor Validation:** Filters out inaccurate or stale GPS readings (>35m uncertainty).
- **1D Adaptive Kalman Filter:** Eliminates urban canyon jitter around multi-story academic buildings.
- **Topological Map Matching:** Snaps coordinates to the nearest walkway with +14m sticky hysteresis to prevent oscillating between adjacent paths.

---

## 🎨 Material 3 Design System

Built strictly following modern Material 3 guidelines and a custom SRM AP color palette:

| Token Name | Light Hex | Dark Hex | Role |
|---|---|---|---|
| **Midnight Navy** | `#172B4D` | `#111C2E` | Primary App Bar & Header Elements |
| **Deep Teal** | `#0F766E` | `#5EEAD4` | Primary Actions & Brand Accents |
| **SRM Crimson** | `#A61B35` | `#E11D48` | Action Highlights & University Accent |
| **Cloud White / Slate** | `#F5F7FA` | `#0B1220` | Surface & Scaffold Backgrounds |
| **Slate Grey** | `#64748B` | `#94A3B8` | Secondary Text & Traversed Routes |
| **Border Grey** | `#E2E8F0` | `#1E293B` | Card Outlines & Dividers |

---

## 🏗️ Architecture & Project Structure

The project follows Clean Architecture and MVVM patterns:

```
com.srmap.campusnavigator/
├── core/
│   ├── designsystem/          # Material 3 colors, typography, theme, and reusable components
│   └── location/              # CampusLocationClient, GpsFilterEngine, Kalman filter
├── data/
│   ├── datasource/            # StaticCampusDataSource (centralized offline seed provider)
│   ├── geojson/               # GeoJsonAssetParser (real-world vector boundaries)
│   ├── local/                 # Room Database, DAOs, Entities, FTS5 Search Index
│   ├── maptiler/              # MapTiler API and Style configuration
│   └── repository/            # CampusRepository, SearchRepository implementations
├── domain/
│   ├── graph/                 # CampusGraph, GraphNode, GraphEdge
│   ├── models/                # CampusBuilding, GeoPoint, UnifiedSearchResult, PedestrianRoute
│   └── routing/               # PedestrianRoutingEngine (A*), PedestrianMapMatcher
└── presentation/
    ├── bottomsheet/           # BuildingDetailsSheet, RoomDetailsSheet, RouteNavigationSheet
    ├── components/            # ExploreTopBar, SearchBar, CategoryChips, SearchResultCards
    ├── floorexplorer/         # FloorExplorerSheet (F1–F10 multi-floor viewer)
    ├── map/                   # InteractiveCampusCanvasMap
    │   ├── model/             # ExtrusionConfig, ExtrudedBuildingFaces
    │   └── renderer/          # SelectedBuilding3DRenderer, Building2DRenderer, GeometryUtils
    ├── search/                # SearchScreen, CategoryFilter, SearchHistory
    └── viewmodel/             # CampusViewModel, SearchViewModel, NavigationViewModel
```

---

## 🛠️ Tech Stack & Requirements

- **Language:** Kotlin 1.9.24
- **UI Toolkit:** Jetpack Compose (BOM 2024.04.01) + Material 3
- **Local Persistence:** Room 2.6.1 + SQLite FTS5
- **Location Services:** Google Play Services Location 21.2.0
- **Vector Graphics:** Native Android Graphics + Compose Canvas
- **Build System:** Gradle 8.7 (Kotlin DSL) with OpenJDK 17/21
- **Target SDK:** Android 14/15/16 (API 34)
- **Minimum SDK:** Android 8.0 Oreo (API 26)

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Iguana / Jellyfish / Ladybug or newer.
- Android SDK 34 installed.
- (Optional) Connected physical device with USB debugging enabled.

### Build via Command Line
```powershell
# 1. Run unit tests
.\gradlew testDebugUnitTest

# 2. Compile Kotlin sources
.\gradlew compileDebugKotlin

# 3. Assemble Debug APK
.\gradlew assembleDebug

# 4. Install onto connected Android device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📊 Routing Benchmarks

Verified in automated unit test suite (`ComprehensiveAuditTest.kt`):

| Origin | Destination | Route Distance | Estimated Time | Routing Path |
|---|---|---|---|---|
| **Kaveri** | **Annapurna Mess** | 178.7 m | 137 s | Direct south via Hostel Central Avenue |
| **Godavari** | **Annapurna Mess** | 141.2 m | 108 s | Direct south via Hostel Central Avenue |
| **Krishna** | **Annapurna Mess** | 105.0 m | 80 s | Direct via South Courtyard |
| **Yamuna** | **Annapurna Mess** | 165.7 m | 127 s | Direct south via East Concourse |
| **Narmada** | **Annapurna Mess** | 145.9 m | 112 s | Direct south via East Concourse |
| **SR Block** | **Homi J. Bhabha Block** | ~270 m | 208 s | Via Campus Core Walkway |

---

## 📚 Technical Documentation & Reports

Detailed technical specifications and GIS audit reports:
- [Architecture & Engineering Specification](docs/ARCHITECTURE_AND_FEATURES.md)
- [Room-Level Search Architecture & Ingestion Guide](docs/ROOM_LEVEL_SEARCH_ARCHITECTURE.md)
- [GIS Dataset Profiling & Audit Report](artifacts/gis_dataset_audit.md)
- [Campus Geometry & Pedestrian Network Report](docs/CAMPUS_GEOMETRY_AND_PEDESTRIAN_NETWORK_REPORT.md)
- [Production Readiness Audit](docs/PRODUCTION_READINESS_REPORT.md)
- [Routing Validation & Algorithm Parity](docs/ROUTING_VALIDATION_REPORT.md)

---

## 📄 License & Attribution
- Developed for **SRM University–AP, Andhra Pradesh**.
- Map geometry & pedestrian vectors © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors (ODbL).
- Basemap styles powered by [MapTiler Cloud](https://www.maptiler.com/).
