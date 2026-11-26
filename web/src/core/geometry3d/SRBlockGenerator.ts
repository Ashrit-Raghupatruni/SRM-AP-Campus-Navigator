import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';

/**
 * Procedural 3D Architectural Reconstruction for SR Block (Sarvepalli Radhakrishnan Block / srmap_bldg_16).
 * Perkins+Will Continuous Horizontal Ribbon Architecture:
 * - 7 Floors (Total height 23.1m + 3.4m mechanical penthouse = 26.5m)
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

    const MIN_X = -4.63;
    const MAX_X = 51.94;
    const MIN_Z = 25.75;
    const MAX_Z = 138.18;
    const LEVELS = 7;
    const FLOOR_H = 3.3;
    const ROOF_H = LEVELS * FLOOR_H; // 23.1m

    // 1. Ground Colonnade Cylindrical Pillars (Recessed Glass Arcade)
    for (let z = MIN_Z + 4; z <= MAX_Z - 4; z += 7.5) {
      const colEast = createBoxMesh(MAX_X - 1.2, MAX_X, 0, FLOOR_H, z - 0.4, z + 0.4, COLOR_COLONNADE_PILLARS);
      const colWest = createBoxMesh(MIN_X, MIN_X + 1.2, 0, FLOOR_H, z - 0.4, z + 0.4, COLOR_COLONNADE_PILLARS);
      group.add(colEast, colWest);
    }

    // Ground Floor Glazed Concourse (Recessed 1.8m)
    const groundConcourse = createBoxMesh(MIN_X + 1.8, MAX_X - 1.8, 0.1, FLOOR_H, MIN_Z + 1.8, MAX_Z - 1.8, 0x1e293b, 0.2, 0.7);
    group.add(groundConcourse);

    // 2. Vertical Sandstone Stair/Elevator Shear Core Towers
    // North Shear Core
    const northCore = createBoxMesh(MIN_X - 0.5, MAX_X + 0.5, 0, ROOF_H + 1.5, MIN_Z - 1.0, MIN_Z + 6.0, COLOR_VERTICAL_CORE, 0.6);
    // South Shear Core
    const southCore = createBoxMesh(MIN_X - 0.5, MAX_X + 0.5, 0, ROOF_H + 1.5, MAX_Z - 6.0, MAX_Z + 1.0, COLOR_VERTICAL_CORE, 0.6);
    // Central Stair Tower
    const centerCore = createBoxMesh(MIN_X - 1.0, MIN_X + 8.0, 0, ROOF_H + 2.0, 75.0, 88.0, COLOR_VERTICAL_CORE, 0.6);
    group.add(northCore, southCore, centerCore);

    // 3. Continuous Floors 1 to 7 with 1.35m Cantilevered Slab Eyebrows & Ribbon Windows
    for (let lvl = 1; lvl <= LEVELS; lvl++) {
      const slabY = lvl * FLOOR_H;
      const winBottom = (lvl - 1) * FLOOR_H + 0.9;
      const winTop = slabY - 0.4;

      // Cantilevered Slab Eyebrow extending 1.35m out
      const eyebrow = createBoxMesh(
        MIN_X - 1.35, MAX_X + 1.35,
        slabY - 0.2, slabY + 0.2,
        MIN_Z - 1.35, MAX_Z + 1.35,
        COLOR_SLAB_EYEBROW,
        0.3, 0.1
      );
      group.add(eyebrow);

      // Dark Spandrel Base Band
      const spandrel = createBoxMesh(
        MIN_X - 0.3, MAX_X + 0.3,
        (lvl - 1) * FLOOR_H, winBottom,
        MIN_Z - 0.3, MAX_Z + 0.3,
        COLOR_DARK_SPANDREL,
        0.5, 0.3
      );
      group.add(spandrel);

      // Wrap-around Ribbon Solar Glazing
      const ribbonGlass = createBoxMesh(
        MIN_X - 0.2, MAX_X + 0.2,
        winBottom, winTop,
        MIN_Z - 0.2, MAX_Z + 0.2,
        isNight ? 0xfbbf24 : COLOR_RIBBON_GLAZING,
        0.1, 0.6,
        isNight ? 0xfbbf24 : 0x000000,
        isNight ? 0.8 : 0.0
      );
      group.add(ribbonGlass);
      windows.push({ mesh: ribbonGlass, nightColor: 0xfbbf24, dayColor: COLOR_RIBBON_GLAZING });
    }

    // 4. Rooftop Mechanical Penthouse (HVAC & Lift Motor Enclosure)
    const roofPenthouse = createBoxMesh(
      MIN_X + 8.0, MAX_X - 8.0,
      ROOF_H, ROOF_H + 3.4,
      MIN_Z + 25.0, MAX_Z - 25.0,
      COLOR_PENTHOUSE_WALL,
      0.6
    );
    group.add(roofPenthouse);

    const centerX = (MIN_X + MAX_X) / 2;
    const centerZ = (MIN_Z + MAX_Z) / 2;

    group.userData = { name: 'SR Block', feature, floors: 7 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, ROOF_H + 6, centerZ),
      name: 'SR Block (Radhakrishnan)',
      category: 'Academic',
      color: '#38bdf8',
      floors: 7,
    };
  }
}
