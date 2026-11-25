// Local East-North-Up (ENU) Cartesian coordinate projection around campus origin
import { CAMPUS_LAT, CAMPUS_LNG } from './solar';

const EARTH_RADIUS = 6378137.0; // WGS84 major axis in meters

/**
 * Projects (lat, lng) to local metric (x, z) coordinates in meters.
 * x: Easting (positive East)
 * z: Southing (in Three.js, -z is North, +x is East, +y is Up)
 */
export function latLngToLocal(lat: number, lng: number, originLat = CAMPUS_LAT, originLng = CAMPUS_LNG): { x: number; z: number } {
  const dLat = (lat - originLat) * (Math.PI / 180);
  const dLng = (lng - originLng) * (Math.PI / 180);
  const meanLat = (originLat * Math.PI) / 180;

  const x = EARTH_RADIUS * dLng * Math.cos(meanLat);
  const y = EARTH_RADIUS * dLat; // North

  // Three.js coordinates: X is East, -Z is North
  return { x, z: -y };
}

/**
 * Inverse conversion from local (x, z) back to (lat, lng)
 */
export function localToLatLng(x: number, z: number, originLat = CAMPUS_LAT, originLng = CAMPUS_LNG): { lat: number; lng: number } {
  const y = -z;
  const meanLat = (originLat * Math.PI) / 180;

  const dLat = (y / EARTH_RADIUS) * (180 / Math.PI);
  const dLng = (x / (EARTH_RADIUS * Math.cos(meanLat))) * (180 / Math.PI);

  return {
    lat: originLat + dLat,
    lng: originLng + dLng,
  };
}

/**
 * Great-circle distance between two GPS coordinates in meters
 */
export function haversineDistanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371e3; // meters
  const phi1 = (lat1 * Math.PI) / 180;
  const phi2 = (lat2 * Math.PI) / 180;
  const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
  const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}
