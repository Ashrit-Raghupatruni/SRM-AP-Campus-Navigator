# SRM AP Campus Geospatial Dataset Audit Report

**Date of Audit:** November 7, 2026  
**Source Dataset:** OpenStreetMap export via Overpass Turbo (`srmap_campus_data` / `campus_all.geojson`)  
**Auditor:** Android Geospatial Lead & Routing Engineer  

---

## 1. Executive Summary

An exhaustive geographical and topological audit was conducted on the SRM University–AP campus dataset. The dataset includes 8 individual GeoJSON files covering boundary polygons, buildings, roads, pathways, facilities, grounds, and sports areas, along with a master `campus_all.geojson` containing 69 filtered campus features.

Key findings:
- All geometries are valid, well-formed GeoJSON (`Polygon` and `LineString`).
- The campus boundary encompasses a total area of ~0.5 km² with coordinates centered at `(16.4632311 N, 80.5064108 E)`.
- 15 named buildings and 2 named facilities are cataloged.
- The pedestrian pathways and campus road segments form a **single contiguous connected component (112 nodes, 117 segments)** without topological disconnection inside the active campus zone.
- Buildings are located within 10m to 109m of their nearest pathway network nodes, allowing clean snapping to graph entry points.

---

## 2. Feature Inventory by File & Geometry Type

| GeoJSON File | Feature Count | Geometry Types | Named Features | Primary Tags Checked |
| :--- | :---: | :---: | :---: | :--- |
| `campus_boundary.geojson` | 1 | Polygon (1) | 1 | `amenity=university`, `name`, `wikidata`, `wikipedia` |
| `buildings.geojson` | 20 | Polygon (20) | 15 | `building`, `building:levels`, `tourism=hostel`, `name` |
| `roads.geojson` | 9 | LineString (9) | 4 | `highway` (secondary, residential, service, construction) |
| `paths.geojson` | 12 | LineString (12) | 0 | `highway` (path, footway) |
| `facilities.geojson` | 6 | Polygon (6) | 2 | `amenity`, `building`, `office`, `name` |
| `sports.geojson` | 6 | Polygon (6) | 0 | `leisure=pitch`, `sport` |
| `grounds.geojson` | 18 | Polygon (18) | 2 | `landuse=grass`, `leisure=garden`, `name` |
| `campus_all.geojson` | 69 | Polygon (48), LineString (21) | 23 | Master combined layer |

---

## 3. Named Campus Buildings and Facilities

### 3.1 Buildings (15 Named, 5 Unnamed / Utility)
1. **Annapurna Mess** (Central Dining / Mess Facility)
2. **CV Raman Block** (Academic / Administrative Block)
3. **Ganga** (Student Hostel)
4. **Gate - 3** (Campus Security & Entry Gate)
5. **Godavari** (Student Hostel)
6. **JC Bose** (Academic Block)
7. **Kaveri** (Student Hostel)
8. **Krishna** (Student Hostel)
9. **Narmada** (Student Hostel)
10. **SR Block** (Academic / Faculty Block)
11. **Vedavathi** (Student Hostel)
12. **Vikram Sarabhai block** (Academic Block)
13. **Water Treatment Plant** (Campus Utility)
14. **X - lab** (Research & Development Laboratories)
15. **Yamuna** (Student Hostel)

*Unnamed building entries in dataset:*
- 5 auxiliary structures (security cabins, electrical substations, water tower `tower:type`, utility sheds).

### 3.2 Facilities & Grounds (4 Named)
- **Gate - 3** (`office=security` / gate facility)
- **Event Ground** (`amenity=events_venue`)
- **Flag area** (`leisure=common`)
- **Tree Court Area** (`leisure=park`)

---

## 4. Road and Pathway Classification

### 4.1 Campus Roads (`roads.geojson` - 9 segments)
- `secondary`: 4 segments (Amaravati arterial spine touching campus boundary, e.g. E12, N10)
- `service`: 2 segments (Internal campus service roads)
- `residential`: 1 segment (Internal residential/hostel loop access)
- `construction`: 2 segments (Sections of E12/N10 under development)

### 4.2 Pedestrian Pathways (`paths.geojson` - 12 segments)
- `path`: 10 segments (Paved and unpaved campus pedestrian walkways connecting blocks)
- `footway`: 2 segments (Dedicated footways between academic blocks and hostels)

---

## 5. Coordinate Bounding Boxes

### 5.1 Campus Boundary (Core University Footprint)
- **Min Longitude:** `80.5038838° E`
- **Max Longitude:** `80.5089378° E`
- **Min Latitude:** `16.4595519° N`
- **Max Latitude:** `16.4669104° N`
- **Campus Center Point:** `16.4632311° N, 80.5064108° E`

### 5.2 Extended Features (Including Boundary Buffer Arterial Roads)
- **Min Longitude:** `80.5011437° E`
- **Max Longitude:** `80.5573475° E` (Arterial highway E12 extending eastward)
- **Min Latitude:** `16.4543557° N`
- **Max Latitude:** `16.4850459° N`

---

## 6. Graph Connectivity & Routing Audit

1. **Topology Check:** Combining all walkable road segments and pedestrian pathways produces **112 unique coordinate nodes** and **117 segments**.
2. **Component Analysis:** A breadth-first search (BFS) on this network identified **exactly 1 connected component** covering all 112 nodes. There are zero orphaned or isolated path segments.
3. **Building Snapping Analysis:**
   - Every mapped building's centroid is within walking distance of the graph network:
     - Minimum distance: Gate - 3 (9.6 m)
     - Average distance across student hostels: ~45 m
     - Maximum distance: Unnamed utility building (109.6 m)
   - The routing engine will project/snap user departure points and destination buildings onto the nearest network edge/node prior to running A* / Dijkstra.
4. **Pedestrian Safety & Access Rules:**
   - Primary arterial highways tagged as `construction` or external fast-moving `secondary` roads outside Gate 3 can have access penalties or be restricted to designated pedestrian crossings.

---

## 7. Known Data Limitations & Handling Strategy

1. **Building Entrances:** OSM did not explicitly tag discrete `entrance=main` point nodes for all hostel blocks.
   - *Mitigation:* The application computes the polygon centroid or the polygon perimeter vertex closest to the pedestrian network as the building destination node.
2. **Indoor Routing:** The dataset contains exterior footprint polygons, not multi-story indoor floorplans.
   - *Mitigation:* Explicitly scoped to outdoor campus navigation between buildings and facilities.
3. **Sports Facilities Names:** The 6 sports pitches (basketball/tennis/volleyball courts) do not have individual `name` tags in OSM.
   - *Mitigation:* Cataloged under category "Sports Grounds" with generic identifiers (e.g., "Sports Complex / Court") derived from the `sport` tag.
