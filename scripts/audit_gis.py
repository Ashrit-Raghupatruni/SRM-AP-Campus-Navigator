import os
import sys
import json
import glob
from osgeo import ogr, gdal, osr

# Enable auto-repairing missing .shx index files
gdal.SetConfigOption("SHAPE_RESTORE_SHX", "YES")

SOURCE_BASE = r"C:\Users\ashri\Downloads\Remote Sensing and GIS\Blocks"

def audit_gis_datasets():
    print(f"Auditing GIS datasets in: {SOURCE_BASE}")
    shps = glob.glob(os.path.join(SOURCE_BASE, "**", "*.shp"), recursive=True)
    shps = [s for s in shps if "__MACOSX" not in s]
    
    report_data = {
        "source_directory": SOURCE_BASE,
        "total_datasets_found": len(shps),
        "datasets": []
    }

    for shp_path in sorted(shps):
        rel_path = os.path.relpath(shp_path, SOURCE_BASE)
        base_no_ext = os.path.splitext(shp_path)[0]
        
        # Check sidecar files
        sidecars = {
            ".shp": os.path.exists(base_no_ext + ".shp"),
            ".shx": os.path.exists(base_no_ext + ".shx"),
            ".dbf": os.path.exists(base_no_ext + ".dbf"),
            ".prj": os.path.exists(base_no_ext + ".prj"),
            ".cpg": os.path.exists(base_no_ext + ".cpg")
        }

        ds = ogr.Open(shp_path)
        if not ds:
            report_data["datasets"].append({
                "file": rel_path,
                "status": "UNREADABLE",
                "sidecars": sidecars
            })
            continue

        lyr = ds.GetLayer(0)
        layer_name = lyr.GetName()
        feat_count = lyr.GetFeatureCount()
        srs = lyr.GetSpatialRef()
        
        crs_info = {
            "name": srs.GetName() if srs else "UNKNOWN",
            "epsg": srs.GetAuthorityCode(None) if srs else "UNKNOWN",
            "is_geographic": bool(srs.IsGeographic()) if srs else False
        }

        defn = lyr.GetLayerDefn()
        field_count = defn.GetFieldCount()
        fields = [defn.GetFieldDefn(i).GetName() for i in range(field_count)]

        # Collect features, coordinates, duplicates
        geometries = []
        bounds = lyr.GetExtent() # (minX, maxX, minY, maxY)
        room_identifiers = []
        samples = []
        invalid_count = 0
        null_room_count = 0

        for i, feat in enumerate(lyr):
            geom = feat.GetGeometryRef()
            if not geom or not geom.IsValid():
                invalid_count += 1
            
            geom_type = geom.GetGeometryName() if geom else "NONE"
            if geom_type == "POINT":
                pt_x, pt_y = geom.GetX(), geom.GetY()
            else:
                pt_x, pt_y = None, None

            attr = {f: feat.GetField(f) for f in fields}
            
            # Identify room field
            room_val = None
            for r_key in ["Room_no", "room_num", "Room_No", "ROOM_NO", "Room", "room"]:
                if r_key in attr and attr[r_key] is not None:
                    room_val = str(attr[r_key]).strip()
                    break

            if room_val:
                room_identifiers.append(room_val)
            else:
                null_room_count += 1

            if i < 3:
                samples.append({
                    "id": feat.GetFID(),
                    "attributes": attr,
                    "point": [pt_x, pt_y] if pt_x is not None else None
                })

        # Duplicates check
        duplicates = {}
        for r in room_identifiers:
            duplicates[r] = duplicates.get(r, 0) + 1
        duplicate_rooms = {k: v for k, v in duplicates.items() if v > 1}

        dataset_entry = {
            "relative_path": rel_path,
            "filename": os.path.basename(shp_path),
            "feature_count": feat_count,
            "geometry_type": lyr.GetGeomType(),
            "geometry_name": ogr.GeometryTypeToName(lyr.GetGeomType()),
            "crs": crs_info,
            "sidecars": sidecars,
            "fields": fields,
            "extent_lon_lat": bounds,
            "invalid_geometries": invalid_count,
            "rooms_found": len(room_identifiers),
            "null_rooms": null_room_count,
            "duplicate_room_codes": duplicate_rooms,
            "samples": samples
        }
        report_data["datasets"].append(dataset_entry)

    # Save JSON audit
    os.makedirs("artifacts", exist_ok=True)
    json_path = os.path.join("artifacts", "gis_dataset_audit.json")
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(report_data, f, indent=2)
    print(f"GIS Audit JSON written to: {json_path}")

    # Generate Markdown Audit
    md_path = os.path.join("artifacts", "gis_dataset_audit.md")
    with open(md_path, "w", encoding="utf-8") as f:
        f.write("# GIS Dataset Profiling & Audit Report\n\n")
        f.write(f"**Source Directory**: `{SOURCE_BASE}`  \n")
        f.write(f"**Total Valid Datasets**: {len(report_data['datasets'])}\n\n")
        f.write("## Dataset Overview Table\n\n")
        f.write("| Filename | Features | Geom Type | CRS (EPSG) | Fields | Missing Sidecars |\n")
        f.write("| :--- | :---: | :---: | :---: | :--- | :--- |\n")
        
        for d in report_data["datasets"]:
            missing_sc = [k for k, v in d.get("sidecars", {}).items() if not v]
            missing_str = ", ".join(missing_sc) if missing_sc else "None"
            f.write(f"| `{d['filename']}` | {d.get('feature_count', 0)} | {d.get('geometry_name', 'N/A')} | {d.get('crs', {}).get('epsg', 'N/A')} | `{d.get('fields', [])}` | {missing_str} |\n")
        
        f.write("\n## Detailed Findings\n\n")
        for d in report_data["datasets"]:
            f.write(f"### `{d['filename']}`\n")
            f.write(f"- **Path**: `{d['relative_path']}`\n")
            f.write(f"- **Count**: {d.get('feature_count')} point features\n")
            f.write(f"- **Bounds [minX, maxX, minY, maxY]**: `{d.get('extent_lon_lat')}`\n")
            f.write(f"- **Null Rooms**: {d.get('null_rooms')}\n")
            f.write(f"- **Duplicate Room Codes**: `{d.get('duplicate_room_codes')}`\n")
            f.write(f"- **Sample Records**:\n```json\n{json.dumps(d.get('samples', []), indent=2)}\n```\n\n")

    print(f"GIS Audit Markdown written to: {md_path}")

if __name__ == "__main__":
    audit_gis_datasets()
