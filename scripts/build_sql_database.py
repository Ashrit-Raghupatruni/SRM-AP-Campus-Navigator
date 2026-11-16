import os
import sys
import csv
import sqlite3
import json

PROCESSED_CSV = r"data/processed/srmap_rooms_enriched.csv"
OUTPUT_DB = r"data/processed/srmap_blocks.db"

os.makedirs(os.path.dirname(OUTPUT_DB), exist_ok=True)

BLOCK_INFO = [
    ("SR1", "SR1 Academic Block", "SR1", 3, "Main academic block housing lecture halls, laboratories, Dean Academic Office, and faculty cabins."),
    ("V", "V-Block", "V", 3, "Academic & Advanced Research block featuring Quantum computing and specialized physics laboratories."),
    ("X-LAB", "X-Lab Complex", "X-LAB", 3, "Multi-disciplinary engineering facility housing AI labs, mechanical prototyping, and main auditorium AUD-1."),
    ("CV", "CV Raman Block", "CV", 10, "10-storey high-rise academic tower containing large-capacity lecture theatres, computing complexes, and faculty suites."),
    ("HOMI", "Homi J Bhabha Block", "HOMI", 5, "Advanced science, research, and research faculty cabins block across 5 levels.")
]

def build_database():
    print(f"Building SQLite database at: {OUTPUT_DB}")
    if os.path.exists(OUTPUT_DB):
        os.remove(OUTPUT_DB)

    conn = sqlite3.connect(OUTPUT_DB)
    conn.execute("PRAGMA foreign_keys = ON;")
    cur = conn.cursor()

    # Create Schema
    cur.execute("""
    CREATE TABLE blocks (
        block_id TEXT PRIMARY KEY,
        block_name TEXT NOT NULL,
        canonical_name TEXT NOT NULL,
        floor_count INTEGER,
        description TEXT
    );
    """)

    cur.execute("""
    CREATE TABLE departments (
        department_id TEXT PRIMARY KEY,
        department_name TEXT NOT NULL,
        normalized_name TEXT NOT NULL
    );
    """)

    cur.execute("""
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
    """)

    cur.execute("""
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
    """)

    cur.execute("""
    CREATE TABLE equipment (
        equipment_id INTEGER PRIMARY KEY AUTOINCREMENT,
        room_id TEXT NOT NULL,
        name TEXT NOT NULL,
        normalized_name TEXT NOT NULL,
        category TEXT,
        specifications TEXT,
        FOREIGN KEY(room_id) REFERENCES rooms(room_id)
    );
    """)

    cur.execute("""
    CREATE TABLE recent_queries (
        query_id INTEGER PRIMARY KEY AUTOINCREMENT,
        query_text TEXT NOT NULL,
        query_type TEXT,
        timestamp INTEGER NOT NULL
    );
    """)

    # Indexes
    cur.execute("CREATE INDEX idx_rooms_block_floor ON rooms(block_id, floor);")
    cur.execute("CREATE INDEX idx_rooms_norm_no ON rooms(normalized_room_no);")
    cur.execute("CREATE INDEX idx_faculty_room ON faculty(room_id);")
    cur.execute("CREATE INDEX idx_faculty_name ON faculty(normalized_name);")
    cur.execute("CREATE INDEX idx_equipment_room ON equipment(room_id);")
    cur.execute("CREATE INDEX idx_equipment_name ON equipment(normalized_name);")

    # FTS5 Full-Text Search Virtual Table
    cur.execute("""
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
    """)

    # Populate Blocks
    cur.executemany("INSERT INTO blocks VALUES (?, ?, ?, ?, ?);", BLOCK_INFO)

    # Ingest Processed CSV
    departments_set = set()
    total_rooms = 0
    total_faculty = 0
    total_equipment = 0

    with open(PROCESSED_CSV, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            r_id = row["room_id"]
            blk = row["block"]
            flr = int(row["floor"]) if row["floor"].isdigit() else 1
            r_no = row["room_no"]
            norm_r_no = r_no.replace(" ", "").upper()
            r_type = row["room_type"]
            lat = float(row["latitude"]) if row["latitude"] else None
            lon = float(row["longitude"]) if row["longitude"] else None
            loc_status = row["match_status"]
            src_ds = row["source_dataset"]
            desc = row["description"]
            cap = int(row["capacity"]) if row["capacity"] and row["capacity"].isdigit() else None

            cur.execute("""
            INSERT OR REPLACE INTO rooms (
                room_id, block_id, room_no, normalized_room_no, floor, room_type,
                latitude, longitude, location_status, source_dataset, description, capacity
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """, (r_id, blk, r_no, norm_r_no, flr, r_type, lat, lon, loc_status, src_ds, desc, cap))
            total_rooms += 1

            # Index Room in FTS5
            room_title = f"{blk} - {r_no}"
            room_sub = f"{r_type} • Floor {flr} • {blk} Block"
            cur.execute("""
            INSERT INTO search_index (entity_type, entity_id, title, subtitle, block_id, floor, room_no, keywords)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """, ("ROOM", r_id, room_title, room_sub, blk, str(flr), norm_r_no, f"{r_no} {r_type} {desc}"))

            # Faculty
            fac_name = row.get("faculty_name", "").strip()
            if fac_name:
                desig = row.get("designation", "").strip()
                dept = row.get("department", "").strip()
                if dept:
                    departments_set.add(dept)

                cur.execute("""
                INSERT INTO faculty (room_id, name, normalized_name, designation, department)
                VALUES (?, ?, ?, ?, ?);
                """, (r_id, fac_name, fac_name.upper(), desig, dept))
                total_faculty += 1

                # Index Faculty in FTS5
                fac_sub = f"{desig} • {dept} • {blk} F{flr} ({r_no})"
                cur.execute("""
                INSERT INTO search_index (entity_type, entity_id, title, subtitle, block_id, floor, room_no, keywords)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                """, ("FACULTY", r_id, fac_name, fac_sub, blk, str(flr), norm_r_no, f"{fac_name} {desig} {dept} {r_no}"))

            # Equipment
            equip_str = row.get("equipment", "").strip()
            if equip_str:
                items = [e.strip() for e in equip_str.split("|") if e.strip()]
                for eq in items:
                    cur.execute("""
                    INSERT INTO equipment (room_id, name, normalized_name, category)
                    VALUES (?, ?, ?, ?);
                    """, (r_id, eq, eq.upper(), "EQUIPMENT"))
                    total_equipment += 1

                    # Index Equipment in FTS5
                    eq_sub = f"Available in {blk} F{flr} ({r_no})"
                    cur.execute("""
                    INSERT INTO search_index (entity_type, entity_id, title, subtitle, block_id, floor, room_no, keywords)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                    """, ("EQUIPMENT", r_id, eq, eq_sub, blk, str(flr), norm_r_no, f"{eq} {r_no} {blk}"))

    # Populate Departments
    for dept in sorted(list(departments_set)):
        dept_id = dept.replace(" ", "_").upper()
        cur.execute("INSERT OR IGNORE INTO departments VALUES (?, ?, ?);", (dept_id, dept, dept.upper()))

    conn.commit()
    conn.close()

    print(f"Database build complete:")
    print(f"  - Total Rooms: {total_rooms}")
    print(f"  - Total Faculty: {total_faculty}")
    print(f"  - Total Equipment: {total_equipment}")
    print(f"  - Departments: {len(departments_set)}")

if __name__ == "__main__":
    build_database()
