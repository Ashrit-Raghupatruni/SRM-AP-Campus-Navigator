import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';

/**
 * Procedural 3D Architectural Reconstruction for Ganga Boys' Hostel (srmap_bldg_15).
 * - 26-story monumental residential skyscraper tower (Height: 80.6m / 83.5m headhouse)
 * - 5.5m high double-height ground pilotis colonnade with glazed reception lobby
 * - 5-story podium base with crisp white slab eyebrows
 * - Slender 21-story upper skyscraper tower in authentic Indian Terracotta Ochre (#966A55)
 * - Dark burnt umber vertical louver fins (#7C5543)
 * - Punched dormitory bedroom window matrices
 * - Double-height sky lounges at mid-levels
 * - Rooftop elevator headhouse and chiller banks
 */
export class GangaHostelGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_TERRACOTTA_OCHRE = 0x966a55;
    const COLOR_TERRACOTTA_DARK = 0x7c5543;
    const COLOR_WHITE_SLAB = 0xf8fafc;
    const COLOR_PILOTIS = 0xe2e8f0;
    const COLOR_CURTAIN_GLASS = 0x38bdf8;
    const COLOR_WINDOW_GLASS = 0x64748b;

    const MIN_X = -5.0;
    const MAX_X = 49.6;
    const MIN_Z = -54.3;
    const MAX_Z = 0.3;

    const TOWER_MIN_X = 15.0;
    const TOWER_MAX_X = 48.0;
    const TOWER_MIN_Z = -54.0;
    const TOWER_MAX_Z = -15.0;

    const PILOTIS_H = 5.5;
    const PODIUM_H = 16.5; // 5 floors
    const TOTAL_H = 80.6;  // 26 floors
    const HEADHOUSE_H = 83.5;

    // 1. Ground Pilotis Colonnade (5.5m clearance)
    for (let pz = MIN_Z + 3.0; pz <= MAX_Z - 3.0; pz += 8.0) {
      for (let px = MIN_X + 3.0; px <= MAX_X - 3.0; px += 8.0) {
        const column = createBoxMesh(px - 0.45, px + 0.45, 0, PILOTIS_H, pz - 0.45, pz + 0.45, COLOR_PILOTIS, 0.3);
        group.add(column);
      }
    }

    // Double-Height Glazed Entrance Lobby on North Facade
    const lobbyGlazing = createBoxMesh(
      MIN_X + 5.0, MAX_X - 5.0,
      0.1, PILOTIS_H,
      MIN_Z + 4.0, MAX_Z - 4.0,
      COLOR_CURTAIN_GLASS,
      0.1, 0.6
    );
    group.add(lobbyGlazing);

    // 2. 5-Story Podium Base (Y: 5.5m to 16.5m)
    const podiumCore = createBoxMesh(MIN_X, MAX_X, PILOTIS_H, PODIUM_H, MIN_Z, MAX_Z, COLOR_TERRACOTTA_OCHRE);
    group.add(podiumCore);

    // Podium floor slab bands
    for (let lvl = 1; lvl <= 4; lvl++) {
      const slabY = PILOTIS_H + lvl * ((PODIUM_H - PILOTIS_H) / 4);
      const slabMesh = createBoxMesh(MIN_X - 0.4, MAX_X + 0.4, slabY - 0.2, slabY + 0.2, MIN_Z - 0.4, MAX_Z + 0.4, COLOR_WHITE_SLAB);
      group.add(slabMesh);
    }

    // 3. 26-Story Upper Skyscraper Tower Core (Y: 16.5m to 80.6m)
    const towerCore = createBoxMesh(TOWER_MIN_X, TOWER_MAX_X, PODIUM_H, TOTAL_H, TOWER_MIN_Z, TOWER_MAX_Z, COLOR_TERRACOTTA_OCHRE);
    group.add(towerCore);

    // 21 Upper Horizontal White Slab Bands
    const upperLevels = 21;
    const floorH = (TOTAL_H - PODIUM_H) / upperLevels;
    for (let lvl = 1; lvl <= upperLevels; lvl++) {
      const slabY = PODIUM_H + lvl * floorH;
      const slabBand = createBoxMesh(
        TOWER_MIN_X - 0.35, TOWER_MAX_X + 0.35,
        slabY - 0.18, slabY + 0.18,
        TOWER_MIN_Z - 0.35, TOWER_MAX_Z + 0.35,
        COLOR_WHITE_SLAB
      );
      group.add(slabBand);

      // Punched Bedroom Window Bands
      const winY = PODIUM_H + (lvl - 1) * floorH + 0.7;
      const winH = floorH - 1.1;

      // North & South Window Bays
      const winNorth = createBoxMesh(
        TOWER_MIN_X + 1.5, TOWER_MAX_X - 1.5,
        winY, winY + winH,
        TOWER_MIN_Z - 0.15, TOWER_MIN_Z + 0.1,
        isNight ? 0xfbbf24 : COLOR_WINDOW_GLASS,
        0.2, 0.4,
        isNight ? 0xfbbf24 : 0x000000,
        isNight ? 0.8 : 0.0
      );
      const winSouth = createBoxMesh(
        TOWER_MIN_X + 1.5, TOWER_MAX_X - 1.5,
        winY, winY + winH,
        TOWER_MAX_Z - 0.1, TOWER_MAX_Z + 0.15,
        isNight ? 0xfbbf24 : COLOR_WINDOW_GLASS,
        0.2, 0.4,
        isNight ? 0xfbbf24 : 0x000000,
        isNight ? 0.8 : 0.0
      );
      group.add(winNorth, winSouth);
      windows.push(
        { mesh: winNorth, nightColor: 0xfbbf24, dayColor: COLOR_WINDOW_GLASS },
        { mesh: winSouth, nightColor: 0xfbbf24, dayColor: COLOR_WINDOW_GLASS }
      );
    }

    // Vertical Burnt Umber Fins on Facades
    for (let fx = TOWER_MIN_X + 2.5; fx <= TOWER_MAX_X - 2.5; fx += 3.4) {
      const finNorth = createBoxMesh(fx - 0.2, fx + 0.2, PODIUM_H, TOTAL_H, TOWER_MIN_Z - 0.45, TOWER_MIN_Z - 0.3, COLOR_TERRACOTTA_DARK);
      const finSouth = createBoxMesh(fx - 0.2, fx + 0.2, PODIUM_H, TOTAL_H, TOWER_MAX_Z + 0.3, TOWER_MAX_Z + 0.45, COLOR_TERRACOTTA_DARK);
      group.add(finNorth, finSouth);
    }

    // Mid-Level Sky Lounges (Levels 14 & 20)
    const skyLounge1Y = PODIUM_H + 9 * floorH;
    const skyLounge1 = createBoxMesh(TOWER_MIN_X + 6, TOWER_MAX_X - 6, skyLounge1Y, skyLounge1Y + floorH * 2, TOWER_MIN_Z - 0.5, TOWER_MIN_Z + 0.2, COLOR_CURTAIN_GLASS, 0.1, 0.7);
    const skyLounge2Y = PODIUM_H + 15 * floorH;
    const skyLounge2 = createBoxMesh(TOWER_MIN_X + 6, TOWER_MAX_X - 6, skyLounge2Y, skyLounge2Y + floorH * 2, TOWER_MIN_Z - 0.5, TOWER_MIN_Z + 0.2, COLOR_CURTAIN_GLASS, 0.1, 0.7);
    group.add(skyLounge1, skyLounge2);

    // 4. Rooftop Headhouse & Mechanical Penthouse (Y: 80.6m to 83.5m)
    const parapet = createBoxMesh(TOWER_MIN_X - 0.3, TOWER_MAX_X + 0.3, TOTAL_H, TOTAL_H + 1.2, TOWER_MIN_Z - 0.3, TOWER_MAX_Z + 0.3, COLOR_WHITE_SLAB);
    const headhouse = createBoxMesh(TOWER_MIN_X + 10, TOWER_MIN_X + 22, TOTAL_H, HEADHOUSE_H, TOWER_MIN_Z + 8, TOWER_MIN_Z + 24, COLOR_WHITE_SLAB);
    group.add(parapet, headhouse);

    const centerX = (TOWER_MIN_X + TOWER_MAX_X) / 2;
    const centerZ = (TOWER_MIN_Z + TOWER_MAX_Z) / 2;

    group.userData = { name: "Ganga Boys' Hostel", feature, floors: 26 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, HEADHOUSE_H + 8, centerZ),
      name: "Ganga Boys' Hostel",
      category: 'Hostels',
      color: '#ec4899',
      floors: 26,
    };
  }
}
