import os
import sys
import re
import csv
import json
import glob
from osgeo import ogr, gdal

gdal.SetConfigOption("SHAPE_RESTORE_SHX", "YES")

SOURCE_BASE = r"C:\Users\ashri\Downloads\Remote Sensing and GIS\Blocks"
CLASSROOMS_CSV = r"data/classrooms.csv"
FACULTY_CSV = r"data/faculty.csv"
EQUIPMENT_CSV = r"data/equipment.csv"
MASTER_CSV = r"data/master_campus_rooms.csv"

OUTPUT_DIR = r"data/processed"
ARTIFACTS_DIR = r"artifacts"

os.makedirs(OUTPUT_DIR, exist_ok=True)
os.makedirs(ARTIFACTS_DIR, exist_ok=True)

BLOCK_ALIASES = {
    "cv_block": "CV", "cv": "CV",
    "sr1_block": "SR1", "sr_block": "SR1", "oe1": "SR1",
    "vblock": "V", "v_block": "V",
    "x-lab_1to3": "X-LAB", "x-lab": "X-LAB", "xlab": "X-LAB",
    "homi j bhabha": "HOMI", "homi": "HOMI"
}

def normalize_block_name(name):
    if not name: return "UNKNOWN"
    clean = re.sub(r"[_\-\s]+", "", name.strip().lower())
    for alias, canonical in BLOCK_ALIASES.items():
        if re.sub(r"[_\-\s]+", "", alias.lower()) == clean:
            return canonical
    return name.strip().upper()

def normalize_room_number(room_str):
    if not room_str: return ""
    r = str(room_str).strip()
    r_clean = re.sub(r"\s+", "", r)
    m = re.match(r"^([A-Za-z]+)[\-_]?(\d+)$", r_clean)
    if m:
        prefix = m.group(1).upper()
        num = m.group(2)
        return f"{prefix}{num}"
    return r_clean.upper()

def infer_floor(block, room_norm):
    if not room_norm: return "1"
    m = re.search(r"(\d+)", room_norm)
    if m:
        digits = m.group(1)
        if len(digits) >= 3:
            return digits[:-2]
        elif len(digits) == 2 and room_norm.startswith("C"):
            return digits[0]
        elif len(digits) == 1:
            return digits
    return "1"

def process_pipeline():
    print("=== Processing GIS Datasets with Multi-CSV (Classrooms, Faculty, Equipment) ===")

    # 1. Load CSV Records
    rooms_meta = {}      # comp_key -> {room_type, capacity, description, floor}
    faculty_meta = {}    # comp_key -> list of {name, designation, department, email}
    equipment_meta = {}  # comp_key -> list of {equipment_name, category, lab_name, specifications}

    # Load classrooms.csv
    if os.path.exists(CLASSROOMS_CSV):
        with open(CLASSROOMS_CSV, mode="r", encoding="utf-8") as f:
            for row in csv.DictReader(f):
                blk = normalize_block_name(row.get("block"))
                r_no = normalize_room_number(row.get("room_no"))
                k = f"{blk}_{r_no}"
                rooms_meta[k] = {
                    "floor": row.get("floor"),
                    "room_type": row.get("room_type", "Classroom"),
                    "capacity": row.get("capacity"),
                    "description": row.get("description", "")
                }
        print(f"Loaded {len(rooms_meta)} classroom definitions from {CLASSROOMS_CSV}")

    # Load faculty.csv
    if os.path.exists(FACULTY_CSV):
        fac_count = 0
        with open(FACULTY_CSV, mode="r", encoding="utf-8") as f:
            for row in csv.DictReader(f):
                blk = normalize_block_name(row.get("block"))
                cab_no = normalize_room_number(row.get("cabin_no") or row.get("room_no"))
                k = f"{blk}_{cab_no}"
                if k not in faculty_meta:
                    faculty_meta[k] = []
                faculty_meta[k].append({
                    "name": row.get("name"),
                    "designation": row.get("designation"),
                    "department": row.get("department"),
                    "email": row.get("email"),
                    "floor": row.get("floor")
                })
                fac_count += 1
        print(f"Loaded {fac_count} faculty assignments from {FACULTY_CSV}")

    # Load equipment.csv
    if os.path.exists(EQUIPMENT_CSV):
        eq_count = 0
        with open(EQUIPMENT_CSV, mode="r", encoding="utf-8") as f:
            for row in csv.DictReader(f):
                blk = normalize_block_name(row.get("block"))
                r_no = normalize_room_number(row.get("room_no"))
                k = f"{blk}_{r_no}"
                if k not in equipment_meta:
                    equipment_meta[k] = []
                equipment_meta[k].append({
                    "name": row.get("equipment_name"),
                    "category": row.get("category"),
                    "lab_name": row.get("lab_name"),
                    "specifications": row.get("specifications"),
                    "floor": row.get("floor")
                })
                eq_count += 1
        print(f"Loaded {eq_count} equipment items from {EQUIPMENT_CSV}")

    # 2. Discover Shapefiles
    primary_shapefiles = [
        (os.path.join(SOURCE_BASE, r"CV_block\cv_block\cv_block.shp"), "CV"),
        (os.path.join(SOURCE_BASE, r"OE1\OE1\Sr1_block.shp"), "SR1"),
        (os.path.join(SOURCE_BASE, r"VBlock\VBlock\VBlock.shp"), "V"),
        (os.path.join(SOURCE_BASE, r"x-lab_1to3\x-lab_1to3\X-LAB_1to3.shp"), "X-LAB")
    ]
    for lvl in range(1, 6):
        primary_shapefiles.append((
            os.path.join(SOURCE_BASE, f"HOMI J BHABHA\\HOMI J BHABHA\\LEVEL {lvl}.shp"),
            "HOMI"
        ))

    geojson_features = []
    enriched_rows = []
    total_spatial_points = 0
    matched_keys = set()

    for shp_path, default_block in primary_shapefiles:
        if not os.path.exists(shp_path): continue
        ds = ogr.Open(shp_path)
        if not ds: continue

        lyr = ds.GetLayer(0)
        defn = lyr.GetLayerDefn()
        fields = [defn.GetFieldDefn(i).GetName() for i in range(defn.GetFieldCount())]

        for feat in lyr:
            total_spatial_points += 1
            fid = feat.GetFID()
            geom = feat.GetGeometryRef()
            if not geom: continue

            pt_x = round(geom.GetX(), 7)
            pt_y = round(geom.GetY(), 7)

            attr = {f: feat.GetField(f) for f in fields}
            raw_room = None
            for k in ["Room_no", "room_num", "Room_No", "ROOM_NO", "Room", "room"]:
                if k in attr and attr[k] is not None:
                    raw_room = str(attr[k]).strip()
                    break
            raw_name = None
            for k in ["Name", "name", "NAME"]:
                if k in attr and attr[k] is not None:
                    raw_name = str(attr[k]).strip()
                    break

            if not raw_room and raw_name and raw_name.lower() != default_block.lower():
                raw_room = raw_name

            norm_room = normalize_room_number(raw_room) if raw_room else f"P{fid}"
            comp_key = f"{default_block}_{norm_room}"

            # Join metadata
            rm_data = rooms_meta.get(comp_key, {})
            fac_data_list = faculty_meta.get(comp_key, [])
            eq_data_list = equipment_meta.get(comp_key, [])

            floor = rm_data.get("floor")
            if not floor and fac_data_list:
                floor = fac_data_list[0].get("floor")
            if not floor and eq_data_list:
                floor = eq_data_list[0].get("floor")
            if not floor:
                floor = infer_floor(default_block, norm_room)

            # Determine Room Type
            if rm_data.get("room_type"):
                room_type = rm_data["room_type"]
            elif fac_data_list:
                room_type = "Faculty Cabin"
            elif eq_data_list:
                room_type = "Research Lab"
            elif norm_room.startswith("C") and len(norm_room) in [3, 4]:
                room_type = "Faculty Cabin"
            elif re.match(r"^[A-Z]?\d+", norm_room):
                room_type = "Classroom"
            else:
                room_type = "Facility"

            match_status = "EXACT_MATCH" if (rm_data or fac_data_list or eq_data_list) else "UNMATCHED_SPATIAL_FEATURE"
            if match_status == "EXACT_MATCH":
                matched_keys.add(comp_key)

            stable_id = f"{default_block}_F{floor}_{norm_room}"

            faculty_names = [f["name"] for f in fac_data_list if f.get("name")]
            faculty_name_str = ", ".join(faculty_names) if faculty_names else None
            designation_str = fac_data_list[0].get("designation") if fac_data_list else None
            department_str = fac_data_list[0].get("department") if fac_data_list else None

            equipment_names = [e["name"] for e in eq_data_list if e.get("name")]
            equipment_str = "|".join(equipment_names) if equipment_names else ""

            description = rm_data.get("description", "")
            capacity = rm_data.get("capacity")

            feature_dict = {
                "type": "Feature",
                "id": stable_id,
                "geometry": {
                    "type": "Point",
                    "coordinates": [pt_x, pt_y]
                },
                "properties": {
                    "stable_id": stable_id,
                    "block": default_block,
                    "floor": str(floor),
                    "room_no": norm_room,
                    "raw_room_no": raw_room,
                    "room_type": room_type,
                    "faculty_name": faculty_name_str,
                    "designation": designation_str,
                    "department": department_str,
                    "equipment": equipment_names,
                    "description": description,
                    "capacity": capacity,
                    "match_status": match_status,
                    "source_dataset": os.path.basename(shp_path)
                }
            }
            geojson_features.append(feature_dict)

            enriched_rows.append({
                "room_id": stable_id,
                "block": default_block,
                "floor": floor,
                "room_no": norm_room,
                "room_type": room_type,
                "faculty_name": faculty_name_str or "",
                "designation": designation_str or "",
                "department": department_str or "",
                "equipment": equipment_str,
                "description": description,
                "capacity": capacity or "",
                "latitude": pt_y,
                "longitude": pt_x,
                "match_status": match_status,
                "source_dataset": os.path.basename(shp_path)
            })

    # Save Enriched GeoJSON
    geojson_out = {
        "type": "FeatureCollection",
        "name": "srmap_rooms_all",
        "features": geojson_features
    }
    geojson_path = os.path.join(OUTPUT_DIR, "srmap_rooms_all.geojson")
    with open(geojson_path, "w", encoding="utf-8") as f:
        json.dump(geojson_out, f, indent=2)

    # Save Enriched CSV
    csv_path = os.path.join(OUTPUT_DIR, "srmap_rooms_enriched.csv")
    with open(csv_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=list(enriched_rows[0].keys()))
        writer.writeheader()
        writer.writerows(enriched_rows)

    print(f"Exported {len(geojson_features)} spatial features to GeoJSON & CSV")
    print(f"Matched {len(matched_keys)} composite room keys from 3 CSVs")

if __name__ == "__main__":
    process_pipeline()
