# SRM AP Campus Navigator 🗺️📍

A production-grade, offline-first native Android navigation application for **SRM University–AP, Andhra Pradesh**, built using **Kotlin**, **Jetpack Compose**, **Material 3**, **MapTiler Cloud**, and **Room Database**.

---

## 📱 Features

- **Interactive Campus Map & MapTiler Layers:**
  - Custom vector campus rendering based on actual OpenStreetMap-derived GeoJSON data.
  - Multi-layer support powered by MapTiler Cloud: **Default Vector**, **Satellite**, **Hybrid**, **Topographic**, and **Outdoor**.
  - Dynamic scaling and camera positioning with responsive `+` and `-` zoom controls, compass north reset, and recentering.
  - Automatic label anti-collision handling with legible high-contrast halos.

- **Unified Search:**
  - Instant offline search across SRM AP academic blocks, hostels, mess halls, research facilities, and sports complexes.
  - Debounced online forward and reverse geocoding via MapTiler Cloud (proximity biased to Amaravati / SRM AP coordinates).
  - Search history, category filtering, and direct camera navigation to selected locations.

- **Offline Pedestrian Navigation Engine:**
  - Connected pedestrian network graph constructed from 112 walkable nodes and 117 segments across campus roads and paths.
  - Shortest path routing computed locally using the **A\*** algorithm with Haversine distance heuristics, verified against **Dijkstra**.
  - Vibrant blue route polyline rendering, walking distance calculations, and turn-by-turn guidance.

- **Strict GPS Verification & Positioning:**
  - Active device hardware checks for `GPS_PROVIDER` and `NETWORK_PROVIDER`.
  - Blue location marker renders strictly when location services are turned on and receiving authentic fixes; automatically hidden when location is disabled.
  - Recenter action prompts users to enable location settings when GPS is off.

- **Room Persistence & Bookmarks:**
  - Local SQLite database via Android Room.
  - Permanent bookmarking across app restarts (`isFavorite` state preserved).
  - Dedicated **Saved** tab for quick access to bookmarked buildings and instant route calculations.

- **Modern Material 3 UI / UX:**
  - 5-tab bottom navigation: **Explore**, **Map**, **Directions**, **Saved**, and **Profile**.
  - Modal bottom sheets with draggable handles for building details and route previews.
  - Light and Dark theme support.

---

## 🏗️ Architecture & Tech Stack

```text
app/src/main/java/com/srmap/campusnavigator/
├── core/
│   ├── designsystem/          # Material 3 colors, typography, and motion
│   └── location/              # CampusLocationClient with strict GPS verification
├── data/
│   ├── geojson/               # GeoJSON parser for buildings, boundaries, paths & roads
│   ├── local/                 # Room database, DAOs, and entities
│   └── maptiler/              # MapTiler Cloud geocoding, styles, IP location & elevation
├── domain/
│   ├── graph/                 # Campus pedestrian network graph (112 nodes, 117 edges)
│   ├── models/                # Building, feature, and unified search models
│   └── routing/               # A* & Dijkstra routing algorithms
└── presentation/
    ├── bottomsheet/           # Building details and route guidance bottom sheets
    ├── components/            # Search bar, floating map controls, navigation bar
    ├── directions/            # Origin-destination route planner screen
    ├── explore/               # Campus category directory screen
    ├── layers/                # MapTiler style selector bottom sheet
    ├── map/                   # Interactive Canvas Map with layer-adaptive styling
    ├── profile/               # Settings, offline data status, theme switcher
    └── saved/                 # Bookmarked locations screen
```

- **Language:** Kotlin 1.9.24
- **UI Toolkit:** Jetpack Compose + Material 3
- **Local Database:** Room 2.6.1
- **Networking:** OkHttp 4.12.0
- **Location:** Google Play Services Location 21.2.0
- **Build System:** Gradle 8.7 (Kotlin DSL)
- **Min SDK:** 26 | **Target SDK:** 34

---

## 🚀 Building & Running

### Prerequisites
1. Android Studio (Koala / Ladybug or newer) with Android SDK 34.
2. JDK 17 or 21 (e.g., Android Studio JBR).
3. A connected Android device with USB debugging enabled.

### Configuration
Create or edit `local.properties` in the project root:
```properties
sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
MAPTILER_API_KEY=your_maptiler_api_key_here
```

### Build Commands
Compile debug APK:
```powershell
.\gradlew.bat assembleDebug --no-daemon
```

Run unit tests (routing algorithms & graph connectivity):
```powershell
.\gradlew.bat testDebugUnitTest --no-daemon
```

### Installation via ADB
```powershell
adb devices
adb install -r -d app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.srmap.campusnavigator/.MainActivity
```

---

## 📄 License
Designed and developed for SRM University–AP, Andhra Pradesh. All rights reserved.
