import { haversineDistanceMeters } from './geo';

export interface UserLocation {
  lat: number;
  lng: number;
  accuracyMeters: number;
  timestamp: number;
  isSimulated?: boolean;
  label?: string;
}

// Canonical SRM AP Campus Landmark Fallbacks if location permission is denied
export const CAMPUS_FALLBACK_ORIGINS: { id: string; label: string; lat: number; lng: number }[] = [
  { id: 'gate1', label: 'Main Entrance (Gate 1)', lat: 16.46328, lng: 80.50585 },
  { id: 'gate3', label: 'Academic Gate (Gate 3)', lat: 16.46385, lng: 80.50815 },
  { id: 'central_plaza', label: 'Academic Central Plaza', lat: 16.46408, lng: 80.50688 },
  { id: 'ganga_hostel', label: 'Ganga Boys Hostel', lat: 16.46348, lng: 80.50662 },
  { id: 'girls_hostels', label: 'Girls Hostels Complex', lat: 16.46608, lng: 80.50820 },
];

export class GeolocationService {
  private static watchId: number | null = null;
  private static currentLocation: UserLocation | null = null;
  private static listeners: ((loc: UserLocation) => void)[] = [];

  /**
   * Request instant one-shot position or fallback to campus center
   */
  static async getCurrentLocation(): Promise<UserLocation> {
    if (this.currentLocation) {
      return this.currentLocation;
    }

    if (!('geolocation' in navigator)) {
      return this.getDefaultFallback();
    }

    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const userLoc: UserLocation = {
            lat: pos.coords.latitude,
            lng: pos.coords.longitude,
            accuracyMeters: pos.coords.accuracy,
            timestamp: pos.timestamp,
            label: 'Your Current GPS Location',
          };
          this.currentLocation = userLoc;
          this.notify(userLoc);
          resolve(userLoc);
        },
        (err) => {
          console.warn('Geolocation error or permission denied, using campus entrance fallback:', err.message);
          const fallback = this.getDefaultFallback();
          resolve(fallback);
        },
        { enableHighAccuracy: true, timeout: 6000, maximumAge: 10000 }
      );
    });
  }

  /**
   * Start watching user position with live GPS tracking
   */
  static startTracking(onUpdate: (loc: UserLocation) => void): () => void {
    this.listeners.push(onUpdate);
    if (this.currentLocation) {
      onUpdate(this.currentLocation);
    }

    if (!this.watchId && 'geolocation' in navigator) {
      this.watchId = navigator.geolocation.watchPosition(
        (pos) => {
          const userLoc: UserLocation = {
            lat: pos.coords.latitude,
            lng: pos.coords.longitude,
            accuracyMeters: pos.coords.accuracy,
            timestamp: pos.timestamp,
            label: 'Your Current Location',
          };
          this.currentLocation = userLoc;
          this.notify(userLoc);
        },
        (err) => {
          console.warn('Geolocation watch error:', err.message);
        },
        { enableHighAccuracy: true, maximumAge: 3000 }
      );
    }

    return () => {
      this.listeners = this.listeners.filter((l) => l !== onUpdate);
      if (this.listeners.length === 0 && this.watchId !== null) {
        navigator.geolocation.clearWatch(this.watchId);
        this.watchId = null;
      }
    };
  }

  /**
   * Set manual simulated origin (e.g. user selects Gate 1 or Central Plaza)
   */
  static setManualOrigin(landmarkId: string): UserLocation {
    const landmark = CAMPUS_FALLBACK_ORIGINS.find((l) => l.id === landmarkId) || CAMPUS_FALLBACK_ORIGINS[0];
    const loc: UserLocation = {
      lat: landmark.lat,
      lng: landmark.lng,
      accuracyMeters: 5,
      timestamp: Date.now(),
      isSimulated: true,
      label: landmark.label,
    };
    this.currentLocation = loc;
    this.notify(loc);
    return loc;
  }

  private static getDefaultFallback(): UserLocation {
    return {
      lat: CAMPUS_FALLBACK_ORIGINS[0].lat,
      lng: CAMPUS_FALLBACK_ORIGINS[0].lng,
      accuracyMeters: 10,
      timestamp: Date.now(),
      isSimulated: true,
      label: 'Main Entrance (Gate 1)',
    };
  }

  private static notify(loc: UserLocation) {
    for (const listener of this.listeners) {
      listener(loc);
    }
  }
}
