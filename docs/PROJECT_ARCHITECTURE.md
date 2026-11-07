# SRM AP Campus Navigator – Project Architecture & Technical Specifications

**Version:** 1.0.0  
**Target Platform:** Android (Min SDK 26, Target SDK 34/35)  
**Language:** Kotlin (100%)  
**UI Stack:** Jetpack Compose + Material 3  
**Architecture:** Clean Architecture + MVI/MVVM with Unidirectional Data Flow (UDF)  

---

## 1. Architectural Layers

The application is structured into four distinct layers adhering strictly to Clean Architecture principles:

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                           │
│  Jetpack Compose, Material 3, Navigation, ViewModels, State     │
│  (HomeMap, Search, RoutePreview, BuildingDetails, Settings)     │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Observes UIState (StateFlow)
                                ▼ Dispatches UserIntents
┌─────────────────────────────────────────────────────────────────┐
│                       DOMAIN LAYER                              │
│  Pure Kotlin (No Android SDK dependencies)                      │
│  • Models: Building, CampusFeature, Route, NavStep, GeoPoint   │
│  • Routing Engine: AStarRouter, DijkstraRouter, RoutingGraph    │
│  • Use Cases: SearchLocationsUseCase, CalculateRouteUseCase,    │
│               GetFavoritesUseCase, ObserveLocationUseCase       │
│  • Repositories (Interfaces): CampusRepository, LocationRepo    │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Implements Interfaces
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DATA LAYER                               │
│  • Local Database: Room (CampusLocation, Favorites, History)    │
│  • GeoJSON Engine: Asset parsing, Feature deserialization       │
│  • Location Provider: FusedLocationProviderClient + Fallback    │
│  • Offline Cache: Asset bundle + In-memory spatial index        │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     CORE & INFRASTRUCTURE                       │
│  • Dependency Injection: Hilt                                   │
│  • Map Engine: MapLibre Native Android Compose wrapper          │
│  • Geometry Utilities: Haversine, Bearing, Turn-angle calculator │
│  • Dispatchers: IO, Default, Main via Coroutines                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Package Structure

```
com.srmap.campusnavigator/
│
├── MainApplication.kt                # Application subclass with @HiltAndroidApp
│
├── core/
│   ├── common/                       # Result wrappers, Dispatcher providers
│   ├── designsystem/                 # Color, Type, Shape, Material3 theme tokens
│   ├── geometry/                     # Haversine, bearing, polyline snapping
│   ├── location/                     # LocationClient, LocationService, permissions
│   └── utils/                        # Extensions, GeoJSON helpers
│
├── data/
│   ├── local/
│   │   ├── database/                 # AppDatabase (Room)
│   │   ├── dao/                      # CampusDao, FavoritesDao, HistoryDao
│   │   └── entities/                 # LocationEntity, FavoriteEntity, etc.
│   ├── geojson/                      # GeoJsonAssetLoader, FeatureParser
│   └── repository/                   # CampusRepositoryImpl, LocationRepositoryImpl
│
├── domain/
│   ├── models/                       # CampusLocation, Route, RouteSegment, Step
│   ├── repository/                   # CampusRepository, LocationRepository
│   ├── routing/                      # AStarRoutingEngine, DijkstraValidator
│   ├── graph/                        # CampusGraph, GraphNode, GraphEdge
│   └── usecases/                     # GetCampusMapUseCase, FindRouteUseCase, etc.
│
├── presentation/
│   ├── MainActivity.kt               # Single Activity host with Edge-to-Edge
│   ├── navigation/                   # Compose Navigation graph & Destinations
│   ├── map/                          # MapViewContainer, MapLibre layer integration
│   ├── search/                       # SearchBar, SearchResults, FilterChips
│   ├── bottomsheet/                  # BuildingDetailSheet, RouteSummarySheet
│   ├── settings/                     # Theme, Offline status, Attribution
│   └── components/                   # Buttons, Cards, Compass, Layer toggles
│
└── di/                               # Hilt modules (AppModule, DatabaseModule, etc.)
```

---

## 3. Technology Choices & Version Compatibility

1. **Map Engine (MapLibre Native Android):**
   - High-performance, offline vector and raster rendering engine.
   - Independent of proprietary map services, guaranteeing complete offline rendering from local styles and GeoJSON sources.
2. **Pedestrian Routing Engine (Domain Layer):**
   - Pure Kotlin implementation of A* (A-Star) with Haversine distance heuristic function.
   - Dual Dijkstra implementation for automated graph verification and shortest path validation in unit tests.
   - Polyline bearing calculator to output human-readable turn-by-turn guidance ("Turn left onto Academic Walk", "Continue straight for 45m").
3. **Database (Room 2.6+):**
   - Stores pre-indexed campus buildings, categories, search history, and favorite locations with FTS/LIKE indexing for sub-10ms search responses.
4. **Offline Capability:**
   - Pre-packaged `campus_all.geojson` and themed GeoJSONs stored in `app/src/main/assets/`.
   - Local vector/raster styling packaged with the app, with fallback offline canvas rendering for maximum resiliency.

---

## 4. Hardware & Target Verification

- **Host Machine:** Windows 11 with OpenJDK 21 & Android SDK Platform 34/36 installed.
- **Connected Test Device:** `10BE7A08JP0007U` (Model `I2306` running Android 16).
- **Deployment Pipeline:** ADB direct installation with logcat tracking.
