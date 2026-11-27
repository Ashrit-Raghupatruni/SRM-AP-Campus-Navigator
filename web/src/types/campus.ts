export interface CampusFeatureProperties {
  id?: string | number;
  name?: string;
  building?: string;
  levels?: number | string;
  height?: number;
  type?: string;
  highway?: string;
  amenity?: string;
  leisure?: string;
  sport?: string;
  surface?: string;
  [key: string]: any;
}

export interface RoomRecord {
  room_id: string;
  room_number: string;
  block: string;
  floor: number;
  type: string;
  capacity?: number;
  description?: string;
  tags?: string[];
  lat: number;
  lng: number;
}

export interface FacultyRecord {
  id?: string;
  name: string;
  designation: string;
  department: string;
  email: string;
  block: string;
  cabin: string;
  floor?: number;
}

export interface RouteStep {
  instruction: string;
  distanceMeters: number;
  lat: number;
  lng: number;
}

export interface RouteResult {
  coordinates: [number, number][]; // [lat, lng]
  totalDistanceMeters: number;
  estimatedMinutes: number;
  steps: RouteStep[];
}

export interface CampusLabel {
  id: string;
  name: string;
  category: string;
  color: string;
  x: number;
  z: number;
  screenX: number;
  screenY: number;
  visible: boolean;
}

export interface SolarInfo {
  sunrise: Date;
  sunset: Date;
  dawn: Date;
  dusk: Date;
  goldenHourMorning: Date;
  goldenHourEvening: Date;
  isNight: boolean;
  isGoldenHour: boolean;
  sunAltitude: number; // in radians
  sunAzimuth: number;  // in radians
  timeOfDayProgress: number; // 0 to 1 across 24h
}
