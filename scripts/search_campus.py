import os
import sys
import argparse
import sqlite3

DB_PATH = r"data/processed/srmap_blocks.db"

def search(query=None, room=None, faculty=None, equipment=None, department=None, block=None, floor=None):
    if not os.path.exists(DB_PATH):
        print(f"Error: Database not found at {DB_PATH}. Run build_sql_database.py first.")
        sys.exit(1)

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    print(f"\n=======================================================")
    print(f"  SRM AP CAMPUS INTELLIGENT ROOM SEARCH (OFFLINE SQL)  ")
    print(f"=======================================================\n")

    if room:
        q_norm = room.replace(" ", "").upper()
        sql = """
        SELECT r.room_id, r.block_id, r.floor, r.room_no, r.room_type, r.latitude, r.longitude, r.location_status,
               f.name, f.designation, f.department,
               GROUP_CONCAT(e.name, ', ') as eq_list
        FROM rooms r
        LEFT JOIN faculty f ON r.room_id = f.room_id
        LEFT JOIN equipment e ON r.room_id = e.room_id
        WHERE r.normalized_room_no LIKE ? OR r.room_no LIKE ?
        GROUP BY r.room_id
        """
        cur.execute(sql, (f"%{q_norm}%", f"%{room}%"))
        results = cur.fetchall()
        print(f"--- Query: Room '{room}' (Found {len(results)}) ---")
        for row in results:
            print(f"  📍 [{row[1]} Block • Floor {row[2]}] Room {row[3]} ({row[4]})")
            print(f"     Coordinates: ({row[5]}, {row[6]}) | Status: {row[7]}")
            if row[8]:
                print(f"     Faculty: {row[8]} ({row[9]}, {row[10]})")
            if row[11]:
                print(f"     Equipment: {row[11]}")
            print()

    elif faculty:
        sql = """
        SELECT f.name, f.designation, f.department, r.block_id, r.floor, r.room_no, r.room_type,
               r.latitude, r.longitude, r.location_status
        FROM faculty f
        JOIN rooms r ON f.room_id = r.room_id
        WHERE f.normalized_name LIKE ? OR f.name LIKE ?
        """
        cur.execute(sql, (f"%{faculty.upper()}%", f"%{faculty}%"))
        results = cur.fetchall()
        print(f"--- Query: Faculty '{faculty}' (Found {len(results)}) ---")
        for row in results:
            print(f"  👤 {row[0]} — {row[1]} ({row[2]})")
            print(f"     Cabin: {row[3]} Block, Floor {row[4]}, Room {row[5]} ({row[6]})")
            print(f"     Coordinates: ({row[7]}, {row[8]}) | Status: {row[9]}")
            print()

    elif equipment:
        sql = """
        SELECT e.name, r.block_id, r.floor, r.room_no, r.room_type, r.latitude, r.longitude,
               f.name, f.department
        FROM equipment e
        JOIN rooms r ON e.room_id = r.room_id
        LEFT JOIN faculty f ON r.room_id = f.room_id
        WHERE e.normalized_name LIKE ? OR e.name LIKE ?
        """
        cur.execute(sql, (f"%{equipment.upper()}%", f"%{equipment}%"))
        results = cur.fetchall()
        print(f"--- Query: Equipment '{equipment}' (Found {len(results)}) ---")
        for row in results:
            print(f"  🔬 Equipment: {row[0]}")
            print(f"     Located at: {row[1]} Block, Floor {row[2]}, Room {row[3]} ({row[4]})")
            print(f"     Coordinates: ({row[5]}, {row[6]})")
            if row[7]:
                print(f"     Lab In-Charge: {row[7]} ({row[8]})")
            print()

    elif query:
        # Unified FTS5 Search
        q_clean = query.replace("'", "''")
        sql = f"""
        SELECT s.entity_type, s.title, s.subtitle, s.block_id, s.floor, s.room_no,
               r.latitude, r.longitude, r.location_status
        FROM search_index s
        JOIN rooms r ON s.entity_id = r.room_id
        WHERE search_index MATCH '{q_clean}*'
        ORDER BY rank
        LIMIT 15;
        """
        try:
            cur.execute(sql)
            results = cur.fetchall()
            print(f"--- Full-Text Search: '{query}' (Found {len(results)}) ---")
            for row in results:
                icon = "📍" if row[0] == "ROOM" else ("👤" if row[0] == "FACULTY" else "🔬")
                print(f"  {icon} [{row[0]}] {row[1]}")
                print(f"     {row[2]}")
                print(f"     Location: {row[3]} F{row[4]} (Lat: {row[6]}, Lon: {row[7]}) | {row[8]}")
                print()
        except Exception as e:
            print(f"FTS Search Error: {e}")

    conn.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="SRM AP Campus Room, Faculty & Equipment CLI Search")
    parser.add_argument("--query", "-q", help="Unified search query (FTS5)")
    parser.add_argument("--room", "-r", help="Search by room number")
    parser.add_argument("--faculty", "-f", help="Search by faculty name")
    parser.add_argument("--equipment", "-e", help="Search by equipment name")
    parser.add_argument("--department", "-d", help="Search by department")
    parser.add_argument("--block", "-b", help="Filter by block")
    parser.add_argument("--floor", help="Filter by floor")
    args = parser.parse_args()

    if not any([args.query, args.room, args.faculty, args.equipment, args.department]):
        search(query="microscope")
    else:
        search(
            query=args.query,
            room=args.room,
            faculty=args.faculty,
            equipment=args.equipment,
            department=args.department,
            block=args.block,
            floor=args.floor
        )
