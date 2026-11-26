import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for SR Block (Sarvepalli Radhakrishnan Block / srmap_bldg_16).
 * Perkins+Will Continuous Horizontal Ribbon Architecture:
 * - 7 Floors (Total height 23.1m + 3.4m mechanical penthouse = 26.5m)
 * - Built directly on top of verified GeoJSON polygon footprint
 * - 1.35m Cantilevered concrete floor slab eyebrows (Color: #E2E8F0)
 * - Dark charcoal horizontal spandrel sub-bands (#1E293B)
 * - Wrap-around continuous solar-control ribbon glazing (#3B5971)
 * - Solid warm sandstone vertical shear core towers (#D3CBBE)
 * - Ground floor colonnade with structural cylindrical columns
 */
export class SRBlockGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_SLAB_EYEBROW = 0xe2e8f0;
    const COLOR_DARK_SPANDREL = 0x1e293b;
    const COLOR_RIBBON_GLAZING = 0x3b5971;
    const COLOR_VERTICAL_CORE = 0xd3cbbe;
    const COLOR_COLONNADE_PILLARS = 0xe2e8f0;
    const COLOR_PENTHOUSE_WALL = 0x64748b;

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
    const LEVELS = 7;
    const FLOOR_H = 3.3;
    const ROOF_H = LEVELS * FLOOR_H; // 23.1m

    // 1. Ground Colonnade Cylindrical Pillars (Recessed Glass Arcade)
    for (let z = MIN_Z + 4; z <= MAX_Z - 4; z += 7.5) {
      const pillarGeo = new THREE.CylinderGeometry(0.4, 0.4, FLOOR_H, 12);
      const pillarMat = new THREE.MeshStandardMaterial({ color: COLOR_COLONNADE_PILLARS, roughness: 0.5 });
      const pWest = new THREE.Mesh(pillarGeo, pillarMat);
      pWest.position.set(MIN_X + 1.2, FLOOR_H / 2, z);
      const pEast = new THREE.Mesh(pillarGeo, pillarMat);
      pEast.position.set(MAX_X - 1.2, FLOOR_H / 2, z);
      group.add(pWest, pEast);
    }

    // Ground Floor Recessed Concourse Glazing
    const groundGlazing = createBoxMesh(MIN_X + 2.0, MAX_X - 2.0, 0.2, FLOOR_H, MIN_Z + 1.5, MAX_Z - 1.5, COLOR_RIBBON_GLAZING, 0.1, 0.7);
    group.add(groundGlazing);

    // 2. Continuous Horizontal Cantilevered Slab Eyebrows & Ribbon Glazing (Floors 1 through 7)
    for (let f = 0; f < LEVELS; f++) {
      const slabY = (f + 1) * FLOOR_H;
      // 1.35m Cantilevered horizontal concrete eyebrow plate projecting around the facade
      const eyebrow = createBoxMesh(
        MIN_X - 1.35, MAX_X + 1.35,
        slabY - 0.35, slabY,
        MIN_Z - 1.35, MAX_Z + 1.35,
        COLOR_SLAB_EYEBROW,
        0.5, 0.1
      );
      group.add(eyebrow);

      // Dark charcoal spandrel sub-band
      const spandrel = createBoxMesh(
        MIN_X - 0.15, MAX_X + 0.15,
        slabY - 1.1, slabY - 0.35,
        MIN_Z - 0.15, MAX_Z + 0.15,
        COLOR_DARK_SPANDREL,
        0.3, 0.3
      );
      group.add(spandrel);

      // Continuous wrap-around ribbon glass band
      if (f > 0) {
        const glassYMin = f * FLOOR_H;
        const glassYMax = slabY - 1.1;
        const ribbonGlass = createBoxMesh(
          MIN_X, MAX_X,
          glassYMin, glassYMax,
          MIN_Z, MAX_Z,
          COLOR_RIBBON_GLAZING,
          0.1, 0.8,
          isNight ? 0xfbbf24 : 0x000000,
          isNight ? 0.6 : 0.0
        );
        group.add(ribbonGlass);
        windows.push({ mesh: ribbonGlass, nightColor: 0xfbbf24, dayColor: COLOR_RIBBON_GLAZING });
      }
    }

    // 3. Solid Sandstone Vertical Shear Core Towers (Stair / Elevator Shafts)
    const towerW = 6.5;
    const towerD = 5.5;
    // Core 1 (North-West)
    const core1 = createBoxMesh(MIN_X - 0.2, MIN_X + towerW, 0, ROOF_H + 2.0, MIN_Z + 15, MIN_Z + 15 + towerD, COLOR_VERTICAL_CORE);
    // Core 2 (Central West)
    const core2 = createBoxMesh(MIN_X - 0.2, MIN_X + towerW, 0, ROOF_H + 2.0, (MIN_Z + MAX_Z) / 2 - towerD / 2, (MIN_Z + MAX_Z) / 2 + towerD / 2, COLOR_VERTICAL_CORE);
    // Core 3 (South-West)
    const core3 = createBoxMesh(MIN_X - 0.2, MIN_X + towerW, 0, ROOF_H + 2.0, MAX_Z - 15 - towerD, MAX_Z - 15, COLOR_VERTICAL_CORE);
    group.add(core1, core2, core3);

    // 4. Rooftop Mechanical Penthouse (Elongated 32m x 16m HVAC Enclosure)
    const pW = Math.min(32.0, MAX_X - MIN_X - 8);
    const pL = Math.min(48.0, MAX_Z - MIN_Z - 16);
    const pCenterX = (MIN_X + MAX_X) / 2;
    const pCenterZ = (MIN_Z + MAX_Z) / 2;
    const penthouse = createBoxMesh(
      pCenterX - pW / 2, pCenterX + pW / 2,
      ROOF_H, ROOF_H + 3.4,
      pCenterZ - pL / 2, pCenterZ + pL / 2,
      COLOR_PENTHOUSE_WALL
    );
    // Parapet coping around the entire roof
    const parapet = createBoxMesh(MIN_X, MAX_X, ROOF_H, ROOF_H + 1.0, MIN_Z, MAX_Z, COLOR_SLAB_EYEBROW);
    const roofDeck = createBoxMesh(MIN_X + 0.8, MAX_X - 0.8, ROOF_H, ROOF_H + 0.1, MIN_Z + 0.8, MAX_Z - 0.8, 0x94a3b8);
    group.add(penthouse, parapet, roofDeck);

    const centerX = (MIN_X + MAX_X) / 2;
    const centerZ = (MIN_Z + MAX_Z) / 2;

    group.userData = { name: 'SR Block', feature, floors: 7 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, ROOF_H + 5, centerZ),
      name: 'SR Block',
      category: 'Academic',
      color: '#38bdf8',
      floors: 7,
    };
  }
}
