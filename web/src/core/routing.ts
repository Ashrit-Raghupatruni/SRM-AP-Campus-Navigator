import { haversineDistanceMeters } from './geo';
import { RouteResult, RouteStep } from '../types/campus';

export interface GraphNode {
  id: string;
  lat: number;
  lng: number;
  neighbors: Map<string, number>; // neighborId -> distanceMeters
}

export class PedestrianGraph {
  nodes: Map<string, GraphNode> = new Map();

  private makeKey(lat: number, lng: number): string {
    return `${lat.toFixed(6)},${lng.toFixed(6)}`;
  }

  addNode(lat: number, lng: number): GraphNode {
    const key = this.makeKey(lat, lng);
    let node = this.nodes.get(key);
    if (!node) {
      node = { id: key, lat, lng, neighbors: new Map() };
      this.nodes.set(key, node);
    }
    return node;
  }

  addEdge(lat1: number, lng1: number, lat2: number, lng2: number) {
    const u = this.addNode(lat1, lng1);
    const v = this.addNode(lat2, lng2);
    const dist = haversineDistanceMeters(lat1, lng1, lat2, lng2);
    u.neighbors.set(v.id, dist);
    v.neighbors.set(u.id, dist);
  }

  loadFromGeoJSON(pathsGeoJSON: any, roadsGeoJSON?: any) {
    const processFeatureCollection = (geojson: any) => {
      if (!geojson || !geojson.features) return;
      for (const feature of geojson.features) {
        if (!feature.geometry) continue;
        const geomType = feature.geometry.type;
        const coords = feature.geometry.coordinates;

        if (geomType === 'LineString') {
          for (let i = 0; i < coords.length - 1; i++) {
            const [lng1, lat1] = coords[i];
            const [lng2, lat2] = coords[i + 1];
            this.addEdge(lat1, lng1, lat2, lng2);
          }
        } else if (geomType === 'MultiLineString') {
          for (const line of coords) {
            for (let i = 0; i < line.length - 1; i++) {
              const [lng1, lat1] = line[i];
              const [lng2, lat2] = line[i + 1];
              this.addEdge(lat1, lng1, lat2, lng2);
            }
          }
        }
      }
    };

    processFeatureCollection(pathsGeoJSON);
    if (roadsGeoJSON) {
      processFeatureCollection(roadsGeoJSON);
    }
  }

  findNearestNode(lat: number, lng: number): GraphNode | null {
    let nearest: GraphNode | null = null;
    let minDist = Infinity;

    for (const node of this.nodes.values()) {
      const d = haversineDistanceMeters(lat, lng, node.lat, node.lng);
      if (d < minDist) {
        minDist = d;
        nearest = node;
      }
    }
    return nearest;
  }

  findRoute(startLat: number, startLng: number, endLat: number, endLng: number): RouteResult | null {
    const startNode = this.findNearestNode(startLat, startLng);
    const endNode = this.findNearestNode(endLat, endLng);

    if (!startNode || !endNode) return null;

    // A* Pathfinding
    const openSet = new Set<string>([startNode.id]);
    const cameFrom = new Map<string, string>();

    const gScore = new Map<string, number>();
    gScore.set(startNode.id, 0);

    const fScore = new Map<string, number>();
    fScore.set(startNode.id, haversineDistanceMeters(startNode.lat, startNode.lng, endNode.lat, endNode.lng));

    while (openSet.size > 0) {
      // Get node in openSet with lowest fScore
      let currentId = '';
      let lowestF = Infinity;
      for (const id of openSet) {
        const f = fScore.get(id) ?? Infinity;
        if (f < lowestF) {
          lowestF = f;
          currentId = id;
        }
      }

      if (currentId === endNode.id) {
        // Reconstruct path
        const pathCoords: [number, number][] = [];
        let curr: string | undefined = endNode.id;
        while (curr) {
          const n = this.nodes.get(curr)!;
          pathCoords.unshift([n.lat, n.lng]);
          curr = cameFrom.get(curr);
        }

        // Add explicit start and end connections
        pathCoords.unshift([startLat, startLng]);
        pathCoords.push([endLat, endLng]);

        let totalDistance = 0;
        const steps: RouteStep[] = [];

        for (let i = 0; i < pathCoords.length - 1; i++) {
          const [l1, g1] = pathCoords[i];
          const [l2, g2] = pathCoords[i + 1];
          const segmentDist = haversineDistanceMeters(l1, g1, l2, g2);
          totalDistance += segmentDist;
        }

        const estimatedMinutes = Math.max(1, Math.round(totalDistance / 75)); // ~4.5 km/h walking speed

        // Formulate real turn-by-turn guidance based on trajectory angles
        const calculateBearing = (lat1: number, lon1: number, lat2: number, lon2: number) => {
          const y = Math.sin((lon2 - lon1) * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180));
          const x =
            Math.cos(lat1 * (Math.PI / 180)) * Math.sin(lat2 * (Math.PI / 180)) -
            Math.sin(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) * Math.cos((lon2 - lon1) * (Math.PI / 180));
          return ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
        };

        steps.push({
          instruction: 'Head towards the nearest campus walkway',
          distanceMeters: Math.round(haversineDistanceMeters(startLat, startLng, startNode.lat, startNode.lng)),
          lat: startLat,
          lng: startLng,
        });

        for (let i = 1; i < pathCoords.length - 2; i++) {
          const [p1Lat, p1Lng] = pathCoords[i - 1];
          const [p2Lat, p2Lng] = pathCoords[i];
          const [p3Lat, p3Lng] = pathCoords[i + 1];

          const b1 = calculateBearing(p1Lat, p1Lng, p2Lat, p2Lng);
          const b2 = calculateBearing(p2Lat, p2Lng, p3Lat, p3Lng);
          let diff = (b2 - b1 + 360) % 360;
          if (diff > 180) diff -= 360;

          const dist = Math.round(haversineDistanceMeters(p2Lat, p2Lng, p3Lat, p3Lng));
          if (dist > 15) {
            if (diff > 40) {
              steps.push({
                instruction: `Turn right onto concourse path (${dist}m)`,
                distanceMeters: dist,
                lat: p2Lat,
                lng: p2Lng,
              });
            } else if (diff < -40) {
              steps.push({
                instruction: `Turn left onto walkway (${dist}m)`,
                distanceMeters: dist,
                lat: p2Lat,
                lng: p2Lng,
              });
            } else if (i === Math.floor(pathCoords.length / 2)) {
              steps.push({
                instruction: `Continue straight towards block destination (${dist}m)`,
                distanceMeters: dist,
                lat: p2Lat,
                lng: p2Lng,
              });
            }
          }
        }

        steps.push({
          instruction: 'Arrive at destination building entrance / gateway',
          distanceMeters: 0,
          lat: endLat,
          lng: endLng,
        });

        return {
          coordinates: pathCoords,
          totalDistanceMeters: Math.round(totalDistance),
          estimatedMinutes,
          steps,
        };
      }

      openSet.delete(currentId);
      const currentNode = this.nodes.get(currentId)!;
      const currentG = gScore.get(currentId) ?? Infinity;

      for (const [neighborId, edgeDist] of currentNode.neighbors.entries()) {
        const tentativeG = currentG + edgeDist;
        if (tentativeG < (gScore.get(neighborId) ?? Infinity)) {
          cameFrom.set(neighborId, currentId);
          gScore.set(neighborId, tentativeG);
          const neighborNode = this.nodes.get(neighborId)!;
          const h = haversineDistanceMeters(neighborNode.lat, neighborNode.lng, endNode.lat, endNode.lng);
          fScore.set(neighborId, tentativeG + h);
          openSet.add(neighborId);
        }
      }
    }

    return null;
  }
}
