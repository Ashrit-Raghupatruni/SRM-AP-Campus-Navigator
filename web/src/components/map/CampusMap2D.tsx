import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { CAMPUS_LAT, CAMPUS_LNG } from '../../core/solar';
import { RouteResult } from '../../types/campus';

interface CampusMap2DProps {
  buildingsGeoJSON: any;
  pathsGeoJSON: any;
  roadsGeoJSON: any;
  groundsGeoJSON: any;
  facilitiesGeoJSON: any;
  sportsGeoJSON: any;
  roomsGeoJSON: any;
  selectedFloor: number | null;
  activeRoute: RouteResult | null;
  isNight: boolean;
  onSelectBuilding: (name: string, feature: any) => void;
  onSelectRoom: (room: any) => void;
}

export const CampusMap2D: React.FC<CampusMap2DProps> = ({
  buildingsGeoJSON,
  pathsGeoJSON,
  roadsGeoJSON,
  groundsGeoJSON,
  facilitiesGeoJSON,
  sportsGeoJSON,
  roomsGeoJSON,
  selectedFloor,
  activeRoute,
  isNight,
  onSelectBuilding,
  onSelectRoom,
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const tileLayerRef = useRef<L.TileLayer | null>(null);
  const routeLayerGroupRef = useRef<L.LayerGroup | null>(null);
  const roomsLayerGroupRef = useRef<L.LayerGroup | null>(null);
  const geojsonLayersRef = useRef<L.LayerGroup | null>(null);

  // Helper to determine active tile URL (MapTiler with Carto fallback)
  const getTileUrl = (night: boolean) => {
    const apiKey = import.meta.env.VITE_MAPTILER_API_KEY;
    if (apiKey) {
      return night
        ? `https://api.maptiler.com/maps/ch-swisstopo-lbm-dark/{z}/{x}/{y}.png?key=${apiKey}`
        : `https://api.maptiler.com/maps/streets-v2/{z}/{x}/{y}.png?key=${apiKey}`;
    }
    return night
      ? 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
      : 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png';
  };

  // Initialize Leaflet map
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    const map = L.map(mapContainerRef.current, {
      center: [CAMPUS_LAT, CAMPUS_LNG],
      zoom: 17,
      minZoom: 15,
      maxZoom: 20,
      zoomControl: false,
    });

    L.control.zoom({ position: 'bottomright' }).addTo(map);

    // Initial tile layer
    const tileUrl = getTileUrl(isNight);

    const tileLayer = L.tileLayer(tileUrl, {
      attribution: '&copy; MapTiler, &copy; OpenStreetMap contributors',
      maxZoom: 20,
    }).addTo(map);
    tileLayerRef.current = tileLayer;

    // Layer groups for overlays
    const geojsonGroup = L.layerGroup().addTo(map);
    geojsonLayersRef.current = geojsonGroup;

    const roomsGroup = L.layerGroup().addTo(map);
    roomsLayerGroupRef.current = roomsGroup;

    const routeGroup = L.layerGroup().addTo(map);
    routeLayerGroupRef.current = routeGroup;

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // Update Tile Layer when Night/Day mode changes
  useEffect(() => {
    if (!mapRef.current || !tileLayerRef.current) return;

    tileLayerRef.current.setUrl(getTileUrl(isNight));
  }, [isNight]);

  // Render Base Vector GeoJSON Layers (Buildings, Paths, Roads, Grounds)
  useEffect(() => {
    const group = geojsonLayersRef.current;
    if (!group) return;
    group.clearLayers();

    // 1. Grounds & Greenery
    if (groundsGeoJSON) {
      L.geoJSON(groundsGeoJSON, {
        style: {
          fillColor: isNight ? '#064e3b' : '#dcfce7',
          fillOpacity: isNight ? 0.25 : 0.6,
          color: isNight ? '#047857' : '#86efac',
          weight: 1,
        },
      }).addTo(group);
    }

    // 2. Roads
    if (roadsGeoJSON) {
      L.geoJSON(roadsGeoJSON, {
        style: {
          color: isNight ? '#334155' : '#cbd5e1',
          weight: 5,
          opacity: 0.9,
        },
      }).addTo(group);
    }

    // 3. Pedestrian Walkways
    if (pathsGeoJSON) {
      L.geoJSON(pathsGeoJSON, {
        style: {
          color: isNight ? '#0d9488' : '#0f766e',
          weight: 2.5,
          dashArray: '4, 4',
          opacity: 0.85,
        },
      }).addTo(group);
    }

    // 4. Sports Fields
    if (sportsGeoJSON) {
      L.geoJSON(sportsGeoJSON, {
        style: {
          fillColor: '#38bdf8',
          fillOpacity: 0.2,
          color: '#0284c7',
          weight: 1.5,
        },
      }).addTo(group);
    }

    // 5. Buildings
    if (buildingsGeoJSON) {
      L.geoJSON(buildingsGeoJSON, {
        style: (feature) => {
          const name = feature?.properties?.name || '';
          return {
            fillColor: isNight ? '#1e293b' : '#ffffff',
            fillOpacity: isNight ? 0.9 : 0.95,
            color: isNight ? '#38bdf8' : '#172b4d',
            weight: 2,
          };
        },
        onEachFeature: (feature, layer) => {
          const name = feature.properties?.name || 'Academic Block';
          layer.bindTooltip(`<b>${name}</b>`, {
            permanent: false,
            direction: 'center',
            className: 'campus-tooltip',
          });
          layer.on('click', () => {
            onSelectBuilding(name, feature);
          });
        },
      }).addTo(group);
    }
  }, [buildingsGeoJSON, pathsGeoJSON, roadsGeoJSON, groundsGeoJSON, sportsGeoJSON, isNight]);

  // Render Rooms Pins Filtered by Floor
  useEffect(() => {
    const group = roomsLayerGroupRef.current;
    if (!group) return;
    group.clearLayers();

    if (!roomsGeoJSON || !roomsGeoJSON.features) return;

    for (const feat of roomsGeoJSON.features) {
      const p = feat.properties || {};
      const coords = feat.geometry?.coordinates;
      if (!coords) continue;
      const [lng, lat] = coords;

      const floor = typeof p.floor === 'number' ? p.floor : parseInt(p.floor || '0', 10);
      if (selectedFloor !== null && floor !== selectedFloor) {
        continue;
      }

      const isLab = (p.type || '').toLowerCase().includes('lab') || (p.description || '').toLowerCase().includes('lab');

      const marker = L.circleMarker([lat, lng], {
        radius: 4.5,
        fillColor: isLab ? '#ec4899' : '#0f766e',
        color: '#ffffff',
        weight: 1.5,
        fillOpacity: 0.9,
      });

      marker.bindTooltip(
        `<div class="text-xs font-sans"><b>${p.room_number || p.room_id}</b><br/>${p.block || ''} • Floor ${floor}</div>`,
        { direction: 'top', offset: [0, -5] }
      );

      marker.on('click', () => {
        onSelectRoom(p);
      });

      marker.addTo(group);
    }
  }, [roomsGeoJSON, selectedFloor]);

  // Render Active Navigation Route
  useEffect(() => {
    const group = routeLayerGroupRef.current;
    if (!group) return;
    group.clearLayers();

    if (!activeRoute || !activeRoute.coordinates || activeRoute.coordinates.length < 2) return;

    // Glowing outline
    L.polyline(activeRoute.coordinates, {
      color: '#0284c7',
      weight: 8,
      opacity: 0.4,
      lineCap: 'round',
    }).addTo(group);

    // Active route inner line
    L.polyline(activeRoute.coordinates, {
      color: '#38bdf8',
      weight: 4,
      opacity: 1,
      dashArray: '8, 8',
    }).addTo(group);

    // Destination Pin
    const endCoord = activeRoute.coordinates[activeRoute.coordinates.length - 1];
    L.circleMarker(endCoord, {
      radius: 8,
      fillColor: '#a61b35',
      color: '#ffffff',
      weight: 2,
      fillOpacity: 1,
    }).addTo(group);
  }, [activeRoute]);

  return <div ref={mapContainerRef} className="w-full h-full relative" />;
};
