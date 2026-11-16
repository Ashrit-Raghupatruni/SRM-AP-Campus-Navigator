<div align="center">
  <img src="./docs/images/app_logo.png" alt="SRM AP Campus Navigator Logo" width="160" />
  <h1>SRM AP Campus Navigator</h1>
  <p><strong>A production-grade, offline-first native Android navigation & room-level spatial intelligence platform for SRM University–AP</strong></p>
</div>

---

## 📱 Features

### 🏢 Room-Level Spatial Intelligence & Multi-Floor Navigation
- **Precise QGIS Point Ingestion:** Over **580 verified spatial door coordinates** extracted directly from QGIS point shapefiles for academic buildings:
  - **SR1 / OE1 Block:** Classrooms (e.g., `S201`, `S203`), faculty cabins (`C21`–`C29`, `C2 10`–`C2 24`), and the Dean Academic Office.
  - **V-Block:** Classrooms, lecture halls, Quantum Open & Closed Research Laboratories.
  - **X-Lab Complex:** AI & Machine Learning labs, Mechanical prototyping/fabrication facilities, and main auditorium `AUD-1`.
  - **CV Raman Block:** 10-story high-rise academic tower lecture halls and faculty suites.
  - **Homi J. Bhabha Block:** Advanced science and research facilities across 5 levels.
- **Interactive Map Floor Selector:** Dedicated vertical floor level selector (`ALL`, `F1`, `F2`, `F3`, `F4`, `F5`, `F10`) on the map canvas to dynamically filter and render room door markers on demand.
- **Pulsing Room Pins & Labels:** Highlighting selected search results with animated pulse rings, color-coded badges, and anti-collision text labels.

### 🔍 Unified Search Engine (Rooms, Faculty & Equipment)
- **Room Number Search:** Instantly locate any classroom, seminar hall, or auditorium (e.g., `S203`, `V101`, `X101`, `210`).
- **Faculty Directory Search:** Search faculty cabins by name, designation, and department (e.g., `Dr. Ramesh Kumar`, `Computer Science`, `Dean`).
- **Laboratory Equipment Search:** Search for research instruments and equipment (e.g., `Microscope`, `Electron Microscope`, `Spectrometer`, `Smart Board`) to pinpoint the exact laboratory room.
- **Category Filter Chips:** Rapid filter chips (`All`, `Rooms`, `Faculty`, `Labs`, `Equipment`) directly on the search bar.
- **SQLite FTS5 Full-Text Search:** Sub-millisecond offline full-text search index with `unicode61` tokenizer.

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

### 💾 Persistence & Non-Destructive Migrations
- **Room Database V2:** Non-destructive migration (`MIGRATION_1_2`) retaining user favorites and recent search history while introducing relational tables for `blocks`, `rooms`, `faculty`, `equipment`, and `departments`.
- **Prepopulated Assets:** Enriched GeoJSON (`srmap_rooms_all.geojson`) and prebuilt SQLite database (`srmap_blocks.db`) packaged directly in app assets for instant offline readiness.

---

## 🛠️ Tech Stack & Tooling

- **Language:** Kotlin 1.9.24
- **UI Toolkit:** Jetpack Compose (BOM 2024.04.01) + Material 3
- **Local Database:** Room 2.6.1 + SQLite FTS5
- **GIS Tools:** QGIS 3.44 (PyQGIS), GDAL / OGR, GeoPandas, Shapely
- **Networking:** OkHttp 4.12.0
- **Location & Sensors:** Google Play Services Location 21.2.0
- **Build System:** Gradle 8.7 (Kotlin DSL) with OpenJDK 21 (Android Studio JBR)
- **Min SDK:** 26 (Android 8.0) | **Target SDK:** 34 (Android 14/15/16)

---

## 💻 CLI & GIS Ingestion Pipeline

To reproduce or update campus room data with a new master CSV:

1. **Place CSV file:** Save classrooms, faculty, and equipment metadata into `data/master_campus_rooms.csv`.
2. **Process GIS Shapefiles & Match CSV:**
   ```powershell
   & "C:\Program Files\QGIS 3.44.12\bin\python-qgis-ltr.bat" scripts/process_blocks_with_csv.py
   ```
3. **Rebuild SQLite Database & FTS5 Index:**
   ```powershell
   python scripts/build_sql_database.py
   ```
4. **Test Search from Terminal:**
   ```powershell
   python scripts/search_campus.py --room S203
   python scripts/search_campus.py --faculty Kumar
   python scripts/search_campus.py --equipment microscope
   ```

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
Detailed technical audits and verification reports are available in the `docs/` and `artifacts/` folders:
- [Room-Level Search Architecture & Ingestion Guide](docs/ROOM_LEVEL_SEARCH_ARCHITECTURE.md)
- [GIS Dataset Profiling & Audit Report](artifacts/gis_dataset_audit.md)
- [Campus Geometry & Pedestrian Network Report](docs/CAMPUS_GEOMETRY_AND_PEDESTRIAN_NETWORK_REPORT.md)
- [Production Readiness Audit](docs/PRODUCTION_READINESS_REPORT.md)
- [GeoJSON Accuracy & ODbL Verification](docs/GEOJSON_ACCURACY_REPORT.md)
- [Routing Validation & Algorithm Parity](docs/ROUTING_VALIDATION_REPORT.md)

---

## 📄 License & Attribution
- Designed and developed for **SRM University–AP, Andhra Pradesh**.
- Map data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors (ODbL).
- Map styles and geocoding powered by [MapTiler Cloud](https://www.maptiler.com/).
