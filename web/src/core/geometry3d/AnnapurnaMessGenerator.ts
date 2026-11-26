import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';

/**
 * Procedural 3D Architectural Reconstruction for Annapurna Mess (srmap_bldg_8).
 * - Stepped organic landscape pavilion integrated into the slope
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

    // Local Coordinates around Annapurna footprint
    const MIN_X = 120.0;
    const MAX_X = 160.0;
    const MIN_Z = -185.0;
    const MAX_Z = -150.0;

    // 1. Tier 1: Lower West Dining Wing (H = 4.5m)
    const lowerDining = createBoxMesh(MIN_X, MIN_X + 22.0, 0, 4.5, MIN_Z + 5.0, MAX_Z - 5.0, COLOR_WALL_WARM);
    // Living Green Roof on Tier 1
    const lowerTurf = createBoxMesh(MIN_X - 0.4, MIN_X + 22.4, 4.5, 4.75, MIN_Z + 4.6, MAX_Z - 4.6, COLOR_LIVING_GREEN_ROOF, 0.95, 0.05);
    const lowerFascia = createBoxMesh(MIN_X - 0.6, MIN_X + 22.6, 4.5, 4.85, MIN_Z + 4.4, MAX_Z - 4.4, COLOR_WHITE_FASCIA);
    group.add(lowerDining, lowerTurf, lowerFascia);

    // 2. Tier 2: Central Double-Height Main Dining Spine (H = 8.5m)
    const centralSpine = createBoxMesh(MIN_X + 18.0, MAX_X - 5.0, 0, 8.5, MIN_Z, MAX_Z, COLOR_WALL_WARM);
    // Living Green Roof on Central Spine
    const spineTurf = createBoxMesh(MIN_X + 17.6, MAX_X - 4.6, 8.5, 8.75, MIN_Z - 0.4, MAX_Z + 0.4, COLOR_LIVING_GREEN_ROOF, 0.95, 0.05);
    const spineFascia = createBoxMesh(MIN_X + 17.4, MAX_X - 4.4, 8.5, 8.85, MIN_Z - 0.6, MAX_Z + 0.6, COLOR_WHITE_FASCIA);
    group.add(centralSpine, spineTurf, spineFascia);

    // 3. Floor-to-Ceiling Clerestory Glazing (Continuous daylight ribbon)
    const clerestory = createBoxMesh(
      MIN_X + 18.0, MAX_X - 5.0,
      5.2, 8.2,
      MIN_Z - 0.15, MIN_Z + 0.15,
      isNight ? 0xf59e0b : COLOR_CLERESTORY_GLASS,
      0.1, 0.7,
      isNight ? 0xf59e0b : 0x000000,
      isNight ? 0.8 : 0.0
    );
    group.add(clerestory);
    windows.push({ mesh: clerestory, nightColor: 0xf59e0b, dayColor: COLOR_CLERESTORY_GLASS });

    // 4. Signature Terracotta/Coral Entrance Portal (#E06D53)
    const entrancePortal = createBoxMesh(
      MIN_X + 28.0, MIN_X + 38.0,
      0, 6.0,
      MAX_Z - 1.0, MAX_Z + 3.5,
      COLOR_CORAL_ENTRANCE,
      0.4, 0.1
    );
    group.add(entrancePortal);

    // 5. Stepped Granite Amphitheater Ledges
    for (let i = 1; i <= 3; i++) {
      const stepRadius = 18.0 + i * 4.0;
      const stepH = i * 0.45;
      const ledge = createBoxMesh(
        MIN_X + 5.0 - i * 1.5, MIN_X + 35.0 + i * 1.5,
        0, stepH,
        MIN_Z - 8.0 - i * 2.0, MIN_Z - 6.0 - i * 2.0,
        COLOR_AMPHITHEATER_GRANITE,
        0.8, 0.2
      );
      group.add(ledge);
    }

    const centerX = (MIN_X + MAX_X) / 2;
    const centerZ = (MIN_Z + MAX_Z) / 2;

    group.userData = { name: 'Annapurna Mess', feature, floors: 2 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, 12.0, centerZ),
      name: 'Annapurna Mess',
      category: 'Food',
      color: '#f97316',
      floors: 2,
    };
  }
}
