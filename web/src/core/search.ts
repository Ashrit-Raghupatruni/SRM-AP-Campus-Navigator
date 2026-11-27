import { RoomRecord, FacultyRecord } from '../types/campus';

export interface SearchResultItem {
  id: string;
  title: string;
  subtitle: string;
  category: 'room' | 'faculty' | 'lab' | 'equipment';
  block: string;
  floor?: number;
  lat: number;
  lng: number;
  details?: any;
}

export class CampusSearchIndex {
  items: SearchResultItem[] = [];

  private cachedRooms: any = null;
  private cachedFaculty: any[] = [];

  loadRooms(roomsGeoJSON: any) {
    this.cachedRooms = roomsGeoJSON;
    this.rebuildIndex();
  }

  loadFaculty(facultyList: any[]) {
    this.cachedFaculty = facultyList || [];
    this.rebuildIndex();
  }

  loadData(roomsGeoJSON: any, facultyList: any[]) {
    this.cachedRooms = roomsGeoJSON;
    this.cachedFaculty = facultyList || [];
    this.rebuildIndex();
  }

  private rebuildIndex() {
    this.items = [];

    // Ingest rooms & labs
    if (this.cachedRooms && this.cachedRooms.features) {
      for (const feat of this.cachedRooms.features) {
        const props = feat.properties || {};
        const coords = feat.geometry?.coordinates;
        if (!coords) continue;
        const [lng, lat] = coords;

        const roomId = props.room_id || props.room_number || `room_${this.items.length}`;
        const roomNum = props.room_number || roomId;
        const block = props.block || props.building || 'Campus';
        const floor = typeof props.floor === 'number' ? props.floor : parseInt(props.floor || '0', 10);
        const type = (props.type || 'Classroom').toLowerCase();

        let category: SearchResultItem['category'] = 'room';
        if (type.includes('lab') || (props.description && props.description.toLowerCase().includes('lab'))) {
          category = 'lab';
        }

        this.items.push({
          id: roomId,
          title: `${block} - ${roomNum}`,
          subtitle: `${props.description || (category === 'lab' ? 'Laboratory Facility' : 'Academic Room')} • Floor ${floor}`,
          category,
          block,
          floor,
          lat,
          lng,
          details: props,
        });
      }
    }

    // Ingest faculty members
    if (Array.isArray(this.cachedFaculty)) {
      for (const f of this.cachedFaculty) {
        const name = f.name || 'Faculty Member';
        const designation = f.designation || 'Faculty';
        const dept = f.department || 'SRM AP';
        const cabin = f.cabin || f.room || 'Cabin';
        const block = f.block || 'Academic Block';
        const floor = typeof f.floor === 'number' ? f.floor : parseInt(f.floor || '1', 10);

        // Fallback campus coordinates if door pin not matched
        const lat = f.lat || 16.5658;
        const lng = f.lng || 80.4996;

        this.items.push({
          id: `fac_${f.email || name}`,
          title: name,
          subtitle: `${designation} • ${dept} (${block} ${cabin})`,
          category: 'faculty',
          block,
          floor,
          lat,
          lng,
          details: f,
        });
      }
    }
  }

  search(query: string, categoryFilter: 'all' | 'room' | 'faculty' | 'lab' = 'all'): SearchResultItem[] {
    const q = query.trim().toLowerCase();
    
    return this.items.filter((item) => {
      if (categoryFilter !== 'all' && item.category !== categoryFilter) {
        return false;
      }
      if (!q) return true;

      const titleMatch = item.title.toLowerCase().includes(q);
      const subMatch = item.subtitle.toLowerCase().includes(q);
      const blockMatch = item.block.toLowerCase().includes(q);
      const idMatch = item.id.toLowerCase().includes(q);

      return titleMatch || subMatch || blockMatch || idMatch;
    }).slice(0, 40);
  }
}
