# Room-Level Spatial Search Architecture & Ingestion Guide

## Overview
This document outlines the architecture for room-level spatial intelligence, faculty directory, and laboratory equipment search for the SRM AP Campus Navigator platform.

---

## 1. Pipeline Overview

```
[QGIS Shapefiles (Blocks/)]  +  [Master CSV (data/master_campus_rooms.csv)]
                          │
                          ▼
            [scripts/process_blocks_with_csv.py]
                          │
        ┌─────────────────┴─────────────────┐
        ▼                                   ▼
[data/processed/srmap_rooms_all.geojson]  [data/processed/srmap_rooms_enriched.csv]
        │                                   │
        ▼                                   ▼
[app/src/main/assets/GeoJSON]       [scripts/build_sql_database.py]
                                            │
                                            ▼
                                  [srmap_blocks.db (SQLite + FTS5)]
                                            │
                                            ▼
                                  [CampusDao.kt / Room Database v2]
```

---

## 2. GIS Shapefile Datasets (Point Layers)
- **CV Block**: 212 room points (Floors 2 to 10).
- **SR1 / OE1 Block**: 219 room points (Floors 1 to 3, classrooms, faculty cabins C21-C29, C31-C39, C2 10-24, Dean Academic Office).
- **V Block**: 44 room points (Floors 1 to 3, Quantum Open & Closed Labs).
- **X-Lab Complex**: 42 room points (Floors 1 to 3, AI labs, Fabrication labs, Main Auditorium AUD-1).
- **Homi J Bhabha Block**: 64 room points (Levels 1 to 5).
- **Coordinate Reference System**: EPSG:4326 (WGS 84), formatted as `[longitude, latitude]` for GeoJSON and `GeoPoint(latitude, longitude)` for Android.

---

## 3. SQL Relational & FTS5 Schema

```sql
CREATE TABLE blocks (
    block_id TEXT PRIMARY KEY,
    block_name TEXT NOT NULL,
    canonical_name TEXT NOT NULL,
    floor_count INTEGER,
    description TEXT
);

CREATE TABLE rooms (
    room_id TEXT PRIMARY KEY,
    block_id TEXT NOT NULL,
    room_no TEXT NOT NULL,
    normalized_room_no TEXT NOT NULL,
    floor INTEGER NOT NULL,
    room_type TEXT NOT NULL,
    latitude REAL,
    longitude REAL,
    location_status TEXT NOT NULL,
    source_dataset TEXT,
    description TEXT,
    capacity INTEGER,
    FOREIGN KEY(block_id) REFERENCES blocks(block_id)
);

CREATE TABLE faculty (
    faculty_id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_id TEXT NOT NULL,
    name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    designation TEXT,
    department TEXT,
    email TEXT,
    FOREIGN KEY(room_id) REFERENCES rooms(room_id)
);

CREATE TABLE equipment (
    equipment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_id TEXT NOT NULL,
    name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    category TEXT,
    specifications TEXT,
    FOREIGN KEY(room_id) REFERENCES rooms(room_id)
);

CREATE VIRTUAL TABLE search_index USING fts5(
    entity_type,
    entity_id,
    title,
    subtitle,
    block_id,
    floor,
    room_no,
    keywords,
    tokenize = 'unicode61'
);
```

---

## 4. Master CSV Format (`data/master_campus_rooms.csv`)

| Header | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `block` | String | Academic block name or code | `SR1`, `V`, `X-LAB`, `CV`, `HOMI` |
| `floor` | Integer | Floor level | `1`, `2`, `3`, `10` |
| `room_no` | String | Room or Cabin number | `S203`, `C214`, `V101`, `AUD-1` |
| `room_type` | String | Type of room | `Classroom`, `Faculty Cabin`, `Research Lab` |
| `faculty_name` | String | Faculty member name | `Dr. Ramesh Kumar` |
| `designation` | String | Position / Title | `Associate Professor` |
| `department` | String | Academic Department | `Computer Science`, `Physics` |
| `equipment` | String | Pipe-delimited list of equipment | `Microscope\|Spectrometer` |
| `description` | String | Additional notes | `Advanced Research Lab` |
| `capacity` | Integer | Seating capacity | `60` |

---

## 5. Execution Commands

### Re-run GIS Ingestion with Updated Master CSV:
```powershell
& "C:\Program Files\QGIS 3.44.12\bin\python-qgis-ltr.bat" scripts/process_blocks_with_csv.py
```

### Re-build SQLite & FTS5 Database:
```powershell
python scripts/build_sql_database.py
```

### CLI Search Verification:
```powershell
python scripts/search_campus.py --room S203
python scripts/search_campus.py --faculty Kumar
python scripts/search_campus.py --equipment microscope
python scripts/search_campus.py --query "Quantum"
```
