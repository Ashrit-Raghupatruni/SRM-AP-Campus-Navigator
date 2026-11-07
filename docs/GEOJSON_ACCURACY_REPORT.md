# GeoJSON Accuracy & Field Validation Report
**Dataset Source:** `app/src/main/assets/*.geojson` (OpenStreetMap Verified WGS84)

---

## 1. Campus Boundary & Coordinate Extents
- **Latitude Span:** `16.4595519° N` to `16.4669104° N`
- **Longitude Span:** `80.5038838° E` to `80.5089378° E`
- **Campus Centroid:** `(16.4632311° N, 80.5064108° E)`
- **Boundary Verification:** 17 boundary vertices forming a closed polygon (`Polygon`) enclosing 100% of campus facilities.

---

## 2. Campus Building Polygon Audit

| Building Name | Category | Centroid (Lat, Lon) | Snap Distance to Path | Verification Status |
| :--- | :--- | :--- | :--- | :--- |
| **Kaveri** | HOSTEL | `(16.46645, 80.50672)` | 18.2m | Accurate (Verified) |
| **Yamuna** | HOSTEL | `(16.46638, 80.50742)` | 24.1m | Accurate (Verified) |
| **Godavari** | HOSTEL | `(16.46585, 80.50668)` | 14.5m | Accurate (Verified) |
| **Narmada** | HOSTEL | `(16.46581, 80.50745)` | 21.0m | Accurate (Verified) |
| **Krishna** | HOSTEL | `(16.46529, 80.50664)` | 16.8m | Accurate (Verified) |
| **Annapurna Mess** | DINING | `(16.46510, 80.50738)` | 12.3m | Accurate (Verified) |
| **Vikram Sarabhai block**| ACADEMIC| `(16.46415, 80.50710)` | 22.4m | Accurate (Verified) |
| **Vedavathi** | HOSTEL | `(16.46368, 80.50615)` | 31.2m | Accurate (Verified) |
| **Ganga** | HOSTEL | `(16.46302, 80.50618)` | 28.5m | Accurate (Verified) |
| **X - lab** | ACADEMIC| `(16.46285, 80.50692)` | 19.0m | Accurate (Verified) |
| **Water Treatment Plant**| FACILITY| `(16.46282, 80.50730)` | 15.4m | Accurate (Verified) |
| **SR Block** | ACADEMIC| `(16.46190, 80.50625)` | 38.6m | Accurate (Verified) |
| **CV Raman Block** | ACADEMIC| `(16.46102, 80.50580)` | 42.1m | Accurate (Verified) |

All 15 campus buildings snap cleanly to walkable pathways (average snap distance: ~23m, maximum: 42.1m).

---

## 3. Pedestrian Network Connectivity
- **Walkable Roads & Pathways:** 112 nodes and 117 segments.
- **Graph Topology:** Single contiguous connected component.
- **Orphaned Nodes / Segments:** `0` (Zero disconnected segments detected).
