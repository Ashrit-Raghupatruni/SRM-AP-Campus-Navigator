import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Annapurna Mess (srmap_bldg_8).
 * - Stepped organic landscape pavilion integrated into the slope
 * - Built directly on top of verified GeoJSON polygon footprint
 * - Signature entrance portal in vibrant Terracotta/Coral red (#E06D53)
 * - LIVING GREEN ROOF (Sod/Turf #2E7D32) with white perimeter fascia (#F8FAFC)
 * - Floor-to-ceiling clerestory glass ribbons (#38BDF8)
 * - Stepped granite amphitheater ledges (#334155)
 */
export class AnnapurnaMessGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_CORAL_ENTRANCE = 0xe06d53;
    const COLOR_WHITE_FASCIA = 0xf8fafc;
    const COLOR_WALL_WARM = 0xfef3c7;
    const COLOR_LIVING_GREEN_ROOF = 0x2e7d32; // Living grass turf
    const COLOR_CLERESTORY_GLASS = 0x38bdf8;
    const COLOR_AMPHITHEATER_GRANITE = 0x334155;

    // Derive bounding envelope directly from GeoJSON polygon
    const coords = feature.geometry?.coordinates?.[0] || [];
    let bldgMinX = Infinity, bldgMaxX = -Infinity, bldgMinZ = Infinity, bldgMaxZ = -Infinity;
    for (const [lng, lat] of coords) {
      const p = latLngToLocal(lat, lng);
      bldgMinX = Math.min(bldgMinX, p.x);
      bldgMaxX = Math.max(bldgMaxX, p.x);
      bldgMinZ = Math.min(bldgMinZ, p.z);
      bldgMaxZ = Math.max(bldgMaxZ, p.z);
    }

    const MIN_X = bldgMinX;
    const MAX_X = bldgMaxX;
    const MIN_Z = bldgMinZ;
    const MAX_Z = bldgMaxZ;
    const WIDTH = MAX_X - MIN_X;
    const DEPTH = MAX_Z - MIN_Z;

    // 1. Tier 1: Lower West Dining Wing (H = 4.5m)
    const tier1MaxX = MIN_X + WIDTH * 0.55;
    const lowerDining = createBoxMesh(MIN_X, tier1MaxX, 0, 4.5, MIN_Z + DEPTH * 0.08, MAX_Z - DEPTH * 0.08, COLOR_WALL_WARM);
    // Living Green Roof on Tier 1
    const lowerTurf = createBoxMesh(MIN_X - 0.4, tier1MaxX + 0.4, 4.5, 4.75, MIN_Z + DEPTH * 0.07, MAX_Z - DEPTH * 0.07, COLOR_LIVING_GREEN_ROOF, 0.95, 0.05);
    const lowerFascia = createBoxMesh(MIN_X - 0.6, tier1MaxX + 0.6, 4.5, 4.85, MIN_Z + DEPTH * 0.06, MAX_Z - DEPTH * 0.06, COLOR_WHITE_FASCIA);
    group.add(lowerDining, lowerTurf, lowerFascia);

    // 2. Tier 2: Raised Central Dining Spine (H = 8.5m)
    const upperDining = createBoxMesh(MIN_X + WIDTH * 0.35, MAX_X, 0, 8.5, MIN_Z, MAX_Z, COLOR_WALL_WARM);
    // Living Green Roof on Tier 2
    const upperTurf = createBoxMesh(MIN_X + WIDTH * 0.34, MAX_X + 0.4, 8.5, 8.75, MIN_Z - 0.4, MAX_Z + 0.4, COLOR_LIVING_GREEN_ROOF, 0.95, 0.05);
    const upperFascia = createBoxMesh(MIN_X + WIDTH * 0.33, MAX_X + 0.6, 8.5, 8.85, MIN_Z - 0.6, MAX_Z + 0.6, COLOR_WHITE_FASCIA);
    group.add(upperDining, upperTurf, upperFascia);

    // 3. Continuous Floor-to-Ceiling Clerestory Ribbon Glazing (Overlooking outdoor plazas)
    const glassWest = createBoxMesh(
      MIN_X - 0.1, MIN_X + 0.1,
      0.4, 4.2,
      MIN_Z + DEPTH * 0.12, MAX_Z - DEPTH * 0.12,
      COLOR_CLERESTORY_GLASS,
      0.1, 0.8,
      isNight ? 0xf59e0b : 0x000000,
      isNight ? 0.8 : 0.0
    );
    group.add(glassWest);
    windows.push({ mesh: glassWest, nightColor: 0xf59e0b, dayColor: COLOR_CLERESTORY_GLASS });

    // 4. Iconic Coral / Terracotta Entry Pavilion Portal
    const portalMinX = MIN_X + WIDTH * 0.15;
    const portalMaxX = portalMinX + 8.5;
    const portalZ = MAX_Z;
    const portalH = 6.2;
    const entryPortal = createBoxMesh(
      portalMinX, portalMaxX,
      0, portalH,
      portalZ - 0.5, portalZ + 3.5,
      COLOR_CORAL_ENTRANCE,
      0.3, 0.1
    );
    group.add(entryPortal);

    // 5. Stepped Granite Amphitheater Bleachers on the Northern Slope
    for (let step = 0; step < 4; step++) {
      const stepY = step * 0.55;
      const stepZ = MIN_Z - 1.5 - step * 1.6;
      const bleacher = createBoxMesh(MIN_X + 6.0, MAX_X - 6.0, stepY, stepY + 0.5, stepZ, stepZ + 1.6, COLOR_AMPHITHEATER_GRANITE);
      group.add(bleacher);
    }

    const centerX = (MIN_X + MAX_X) / 2;
    const centerZ = (MIN_Z + MAX_Z) / 2;

    group.userData = { name: 'Annapurna Mess', feature, floors: 2 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, 12.0, centerZ),
      name: 'Annapurna Mess',
      category: 'Dining',
      color: '#f59e0b',
      floors: 2,
    };
  }
}
