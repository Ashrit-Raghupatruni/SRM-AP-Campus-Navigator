# SRM AP Campus Navigator 🗺️📍

A production-grade, offline-first native Android navigation application for **SRM University–AP, Andhra Pradesh**, built using **Kotlin**, **Jetpack Compose**, **Material 3**, **MapTiler Cloud**, and **Room Database**.

---

## 📱 Features

### 📍 Interactive Campus Map & MapTiler Layers
- **Custom Vector Campus Canvas:** Renders buildings, footpaths, roads, and landscape features parsed from verified OpenStreetMap GeoJSON datasets.
- **5 MapTiler Styles:** Seamlessly switch between **Default Vector**, **Satellite**, **Hybrid**, **Topographic**, and **Outdoor** basemaps.
- **Fluid Camera & Canvas Controls:** Responsive multi-touch zoom, pan, floating `+` and `-` zoom triggers, compass north reset, and camera recentering with smooth animations.
- **Anti-Collision Label Rendering:** Building titles and room codes render with high-contrast background halos, automatically preventing label overlapping.

### 🧭 Live GPS Tracking, Kalman Filtering & Map Matching
- **Hardware-Level Sensor Checks:** Validates real GNSS provider fixes (`GPS_PROVIDER` vs. coarse network), rejecting stale, high-uncertainty (>35m), or anomalous jumps.
- **Adaptive 1D Kalman Smoothing:** Smooths raw GPS coordinates using speed and bearing models, mitigating urban canyon jitter and space-frame canopy interference.
- **Topological Map Matching with Hysteresis:** Snaps user coordinates to the nearest pedestrian walkway. Employs a +14m sticky hysteresis buffer on the active edge to eliminate jumping between adjacent parallel pathways.

### 🚶 Dynamic Route Progress Visualization
- **Dual-State Route Slicing:** As the user navigates:
  - **Completed Portion:** Rendered as a muted slate line (`#64748B`), dynamically extending behind the user.
  - **Remaining Portion:** Rendered as a prominent electric blue polyline (`#0284C7`), shrinking as progress is made.
- **Live Progress & Distance Tracker:** Computes remaining meters and walking seconds in real time, updating the Material 3 navigation bottom sheet and linear progress bar.
- **Arrival Detection & Off-Route Correction:**
  - Automatically signals destination arrival when within 12 meters.
  - Triggers automatic A* route recalculation if the user deviates >25 meters from the planned route for 3 consecutive GPS fixes.

### 🏛️ Verified Campus Geometry & Pedestrian Concourse
- **Academic Complex Separation:** Replaced monolithic building footprint with two verified independent 5-story blocks:
  - **Vikram Sarabhai Block** (West Wing, `way/1472341023`)
  - **Homi J. Bhabha Block** (East Wing, `way/1472341098_bhabha`)
- **Academic Central Passage:** 13-meter open pedestrian concourse (`way/1472341099_passage`) directly beneath the central canopy, connecting the south plaza to the north tree court.
- **Hostel Zone Courtyard Avenue:**
  - Verified 6-building layout: **Kaveri**, **Godavari**, **Krishna** on the West; **Yamuna**, **Narmada**, and **Annapurna Mess** (green-roofed) on the East.
  - Integrated **Hostel Central Avenue** (`way/1472341100_hostel_avenue`) with lateral entrance connectors to all hostel blocks, mess hall, and academic entrances.

### 🔍 Unified Search & Offline Routing Engine
- **Instant Search:** Search across all campus buildings, departments, hostels, mess halls, and amenities completely offline.
- **Online Geocoding Fallback:** MapTiler Cloud forward/reverse geocoding debounced with Amaravati bounding box proximity.
- **Dual Engine Verification:** A* shortest path navigation verified with 100% path parity against Dijkstra.

### 💾 Persistence & Bookmarks
- **Room Database:** Local SQLite storage persists bookmarked buildings (`isFavorite`), custom labels, and user search history across app restarts.
- **Dedicated Saved Tab:** Instant one-tap navigation to frequently visited locations.

---

- **Language:** Kotlin 1.9.24
- **UI Toolkit:** Jetpack Compose (BOM 2024.04.01) + Material 3
- **Local Database:** Room 2.6.1
- **Networking:** OkHttp 4.12.0
- **Location & Sensors:** Google Play Services Location 21.2.0
- **Build System:** Gradle 8.7 (Kotlin DSL)
- **Min SDK:** 26 (Android 8.0) | **Target SDK:** 34 (Android 14/15/16)

---

## 📊 Routing Benchmarks

Verified in automated unit test suite (`ComprehensiveAuditTest.kt`):

| Origin | Destination | Total Route Distance | Estimated Time | Routing Path |
|---|---|---|---|---|
| **Kaveri** | **Annapurna Mess** | 178.7 m | 137 s | Direct south via Hostel Central Avenue |
| **Godavari** | **Annapurna Mess** | 141.2 m | 108 s | Direct south via Hostel Central Avenue |
| **Krishna** | **Annapurna Mess** | 105.0 m | 80 s | Direct via South Courtyard |
| **Yamuna** | **Annapurna Mess** | 165.7 m | 127 s | Direct south via East Concourse |
| **Narmada** | **Annapurna Mess** | 145.9 m | 112 s | Direct south via East Concourse |
| **Vikram Sarabhai Block** | **Homi J. Bhabha Block** | 81.5 m | 62 s | Via Academic Central Passage |

---

## 📚 Technical Reports & Audits
Detailed technical audits and verification reports are available in the `docs/` folder:
- [Campus Geometry & Pedestrian Network Report](docs/CAMPUS_GEOMETRY_AND_PEDESTRIAN_NETWORK_REPORT.md)
- [Production Readiness Audit](docs/PRODUCTION_READINESS_REPORT.md)
- [GeoJSON Accuracy & ODbL Verification](docs/GEOJSON_ACCURACY_REPORT.md)
- [Routing Validation & Algorithm Parity](docs/ROUTING_VALIDATION_REPORT.md)

---

## 📄 License & Attribution
- Designed and developed for **SRM University–AP, Andhra Pradesh**.
- Map data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors (ODbL).
- Map styles and geocoding powered by [MapTiler Cloud](https://www.maptiler.com/).
