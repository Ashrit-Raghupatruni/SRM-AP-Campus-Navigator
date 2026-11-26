import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for CV Raman Block (Academic Block III / srmap_bldg_20).
 * - 12-Story Concrete & Glass Monolith (Height: 42.0m)
 * - Built strictly on top of verified GeoJSON footprint
 * - 5-Story Western Annex Wing (H = 18.0m)
 * - Floors 10-12 (Central Library): FULL 360-DEGREE CONTINUOUS PANORAMIC CURTAIN GLASS wrapped around all facades
 * - North Facade: Monumental Dual-Tier Sky-Atrium Voids (Floors 5-7) with warm study lounge illumination (#FDE047)
 * - South & East Facades (Facing Main Entrance & SR Block): Full horizontal window ribbons and cantilever floor slab bands
 * - Ground Colonnade: 5.0m double-height reception lobby and cylindrical stilt columns (#334155)
 * - Overhanging dark concrete brow cap and mechanical penthouse (#475569)
 */
export class CVRamanBlockGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    // Authentic Material Colors
    const FAÇADE_PRECAST_WHITE = 0xf1f5f9;
    const FAÇADE_DARK_CONCRETE = 0x475569;
    const CURTAIN_GLASS = 0x38bdf8;
    const LIBRARY_GLASS = 0x1e3a5f;
    const VOID_BACKWALL_WARM = 0xfde047;
    const ROOF_GRAVEL = 0x94a3b8;
    const PILOTIS_COLOR = 0x334155;
    const COLOR_NIGHT_WINDOW = 0xfbbf24;

    // Derive bounding envelope directly from GeoJSON polygon
    const coords: [number, number][] = feature.geometry?.coordinates?.[0] || [];
    let bldgMinX = Infinity, bldgMaxX = -Infinity, northZ = Infinity, southZ = -Infinity;
    for (const [lng, lat] of coords) {
      const p = latLngToLocal(lat, lng);
      bldgMinX = Math.min(bldgMinX, p.x);
      bldgMaxX = Math.max(bldgMaxX, p.x);
      northZ = Math.min(northZ, p.z);
      southZ = Math.max(southZ, p.z);
    }

    const BLDG_MIN_X = bldgMinX;
    const BLDG_MAX_X = bldgMaxX;
    const NORTH_Z = northZ;
    const SOUTH_Z = southZ;
    const BLDG_WIDTH = BLDG_MAX_X - BLDG_MIN_X;
    const BLDG_DEPTH = SOUTH_Z - NORTH_Z;

    // Relative architectural sections
    const WING_DIVIDER_X = BLDG_MIN_X + BLDG_WIDTH * 0.25;
    const WING_HEIGHT = 18.0; // 5 floors
    const TOWER_HEIGHT = 42.0; // 12 floors

    const VOID_MIN_X = BLDG_MIN_X + BLDG_WIDTH * 0.44;
    const VOID_MAX_X = BLDG_MIN_X + BLDG_WIDTH * 0.82;
    const VOID_DEPTH_Z = 10.0;
    const LOWER_VOID_MIN_Y = 13.5;
    const LOWER_VOID_MAX_Y = 21.0;
    const UPPER_VOID_MIN_Y = 23.5;
    const UPPER_VOID_MAX_Y = 31.0;

    // 1. Ground Pilotis Colonnade (Elevated 5.0m ground floor)
    for (let x = BLDG_MIN_X + 4; x <= BLDG_MAX_X - 4; x += 9.0) {
      const colNorth = createBoxMesh(x - 0.5, x + 0.5, 0, 5.0, NORTH_Z, NORTH_Z + 1.0, PILOTIS_COLOR);
      const colSouth = createBoxMesh(x - 0.5, x + 0.5, 0, 5.0, SOUTH_Z - 1.0, SOUTH_Z, PILOTIS_COLOR);
      group.add(colNorth, colSouth);
    }
    // Glazed Reception Lobby
    const groundLobby = createBoxMesh(WING_DIVIDER_X + 2, BLDG_MAX_X - 2, 0.2, 5.0, NORTH_Z + 1.2, SOUTH_Z - 1.2, 0x1e293b, 0.2, 0.7);
    group.add(groundLobby);

    // 2. 5-Story Western Annex Wing (H = 18m)
    const westWing = createBoxMesh(BLDG_MIN_X, WING_DIVIDER_X, 5.0, WING_HEIGHT, NORTH_Z, SOUTH_Z, FAÇADE_PRECAST_WHITE);
    const westParapet = createBoxMesh(BLDG_MIN_X, WING_DIVIDER_X, WING_HEIGHT, WING_HEIGHT + 1.2, NORTH_Z, SOUTH_Z, FAÇADE_DARK_CONCRETE);
    group.add(westWing, westParapet);

    // Windows on Western Annex Wing
    for (let f = 1; f <= 4; f++) {
      const y = 5.0 + f * 3.0;
      const winSouth = createBoxMesh(BLDG_MIN_X + 2, WING_DIVIDER_X - 2, y, y + 1.6, SOUTH_Z - 0.2, SOUTH_Z + 0.2, CURTAIN_GLASS, 0.2, 0.8, isNight ? COLOR_NIGHT_WINDOW : 0x000000, isNight ? 0.8 : 0.0);
      group.add(winSouth);
      windows.push({ mesh: winSouth, nightColor: COLOR_NIGHT_WINDOW, dayColor: CURTAIN_GLASS });
    }

    // 3. 12-Story Main Tower Solid Blocks (Flanking the center void)
    const leftTower = createBoxMesh(WING_DIVIDER_X, VOID_MIN_X, 5.0, TOWER_HEIGHT, NORTH_Z, SOUTH_Z, FAÇADE_PRECAST_WHITE);
    const rightTower = createBoxMesh(VOID_MAX_X, BLDG_MAX_X, 5.0, TOWER_HEIGHT, NORTH_Z, SOUTH_Z, FAÇADE_PRECAST_WHITE);
    const rearTower = createBoxMesh(VOID_MIN_X, VOID_MAX_X, 5.0, TOWER_HEIGHT, NORTH_Z + VOID_DEPTH_Z, SOUTH_Z, FAÇADE_PRECAST_WHITE);
    group.add(leftTower, rightTower, rearTower);

    // Floors 1-4 Solid Podium Base beneath voids
    const voidPodium = createBoxMesh(VOID_MIN_X, VOID_MAX_X, 5.0, LOWER_VOID_MIN_Y, NORTH_Z, NORTH_Z + VOID_DEPTH_Z, FAÇADE_PRECAST_WHITE);
    const crownBridge = createBoxMesh(VOID_MIN_X, VOID_MAX_X, UPPER_VOID_MAX_Y, TOWER_HEIGHT, NORTH_Z, NORTH_Z + VOID_DEPTH_Z, FAÇADE_PRECAST_WHITE);
    const midBridge = createBoxMesh(VOID_MIN_X, VOID_MAX_X, LOWER_VOID_MAX_Y, UPPER_VOID_MIN_Y, NORTH_Z, NORTH_Z + VOID_DEPTH_Z, FAÇADE_DARK_CONCRETE);
    group.add(voidPodium, crownBridge, midBridge);

    // 4. North Facade Monumental Sky-Atrium Voids with Warm Glowing Study Lounges
    const lowerVoidGlow = createBoxMesh(
      VOID_MIN_X + 0.5, VOID_MAX_X - 0.5,
      LOWER_VOID_MIN_Y + 0.5, LOWER_VOID_MAX_Y - 0.5,
      NORTH_Z + VOID_DEPTH_Z - 0.3, NORTH_Z + VOID_DEPTH_Z,
      VOID_BACKWALL_WARM,
      0.3, 0.1,
      isNight ? 0xfbbf24 : 0x000000,
      isNight ? 0.95 : 0.0
    );
    const upperVoidGlow = createBoxMesh(
      VOID_MIN_X + 0.5, VOID_MAX_X - 0.5,
      UPPER_VOID_MIN_Y + 0.5, UPPER_VOID_MAX_Y - 0.5,
      NORTH_Z + VOID_DEPTH_Z - 0.3, NORTH_Z + VOID_DEPTH_Z,
      VOID_BACKWALL_WARM,
      0.3, 0.1,
      isNight ? 0xfbbf24 : 0x000000,
      isNight ? 0.95 : 0.0
    );
    group.add(lowerVoidGlow, upperVoidGlow);
    windows.push(
      { mesh: lowerVoidGlow, nightColor: 0xfbbf24, dayColor: VOID_BACKWALL_WARM },
      { mesh: upperVoidGlow, nightColor: 0xfbbf24, dayColor: VOID_BACKWALL_WARM }
    );

    const lowerRailing = createBoxMesh(VOID_MIN_X, VOID_MAX_X, LOWER_VOID_MIN_Y + 0.2, LOWER_VOID_MIN_Y + 1.4, NORTH_Z - 0.1, NORTH_Z + 0.1, CURTAIN_GLASS, 0.1, 0.8);
    const upperRailing = createBoxMesh(VOID_MIN_X, VOID_MAX_X, UPPER_VOID_MIN_Y + 0.2, UPPER_VOID_MIN_Y + 1.4, NORTH_Z - 0.1, NORTH_Z + 0.1, CURTAIN_GLASS, 0.1, 0.8);
    group.add(lowerRailing, upperRailing);

    // North Facade Vertical Sky-Blue Glass Curtain Spines
    const spineW = 3.6;
    const leftGlassSpine = createBoxMesh(VOID_MIN_X - spineW, VOID_MIN_X, 5.0, TOWER_HEIGHT, NORTH_Z - 0.3, NORTH_Z + 0.2, CURTAIN_GLASS, 0.1, 0.6);
    const rightGlassSpine = createBoxMesh(VOID_MAX_X, VOID_MAX_X + spineW, 5.0, TOWER_HEIGHT, NORTH_Z - 0.3, NORTH_Z + 0.2, CURTAIN_GLASS, 0.1, 0.6);
    group.add(leftGlassSpine, rightGlassSpine);

    // 5. South Facade (Facing Main Boulevard) Windows & Horizontal Slab Ribbons (Floors 1-9)
    for (let f = 1; f <= 9; f++) {
      const y = 5.0 + (f - 1) * 3.1;
      // Cantilever slab band
      const southSlab = createBoxMesh(WING_DIVIDER_X - 0.4, BLDG_MAX_X + 0.4, y, y + 0.3, SOUTH_Z - 0.4, SOUTH_Z + 0.6, FAÇADE_PRECAST_WHITE);
      group.add(southSlab);

      // Continuous ribbon window
      const southWin = createBoxMesh(
        WING_DIVIDER_X + 1.0, BLDG_MAX_X - 1.0,
        y + 0.5, y + 2.2,
        SOUTH_Z - 0.15, SOUTH_Z + 0.25,
        CURTAIN_GLASS,
        0.15, 0.8,
        isNight ? COLOR_NIGHT_WINDOW : 0x000000,
        isNight ? 0.75 : 0.0
      );
      group.add(southWin);
      windows.push({ mesh: southWin, nightColor: COLOR_NIGHT_WINDOW, dayColor: CURTAIN_GLASS });
    }

    // East Facade (Facing SR Block) Windows (Floors 1-9)
    for (let f = 1; f <= 9; f++) {
      const y = 5.0 + (f - 1) * 3.1;
      const eastWin = createBoxMesh(
        BLDG_MAX_X - 0.2, BLDG_MAX_X + 0.25,
        y + 0.5, y + 2.2,
        NORTH_Z + 3.0, SOUTH_Z - 3.0,
        CURTAIN_GLASS,
        0.15, 0.8,
        isNight ? COLOR_NIGHT_WINDOW : 0x000000,
        isNight ? 0.75 : 0.0
      );
      group.add(eastWin);
      windows.push({ mesh: eastWin, nightColor: COLOR_NIGHT_WINDOW, dayColor: CURTAIN_GLASS });
    }

    // 6. Floors 10-12: Full 360-Degree Continuous Panoramic Ribbon Curtain Glass (Central Library)
    const libYMin = TOWER_HEIGHT - 9.0; // ~33.0m
    const libYMax = TOWER_HEIGHT - 1.0; // ~41.0m

    // North Library Glass
    const libNorth = createBoxMesh(WING_DIVIDER_X - 0.3, BLDG_MAX_X + 0.3, libYMin, libYMax, NORTH_Z - 0.3, NORTH_Z + 0.2, LIBRARY_GLASS, 0.1, 0.8, isNight ? 0x38bdf8 : 0x000000, isNight ? 0.9 : 0.0);
    // South Library Glass
    const libSouth = createBoxMesh(WING_DIVIDER_X - 0.3, BLDG_MAX_X + 0.3, libYMin, libYMax, SOUTH_Z - 0.2, SOUTH_Z + 0.3, LIBRARY_GLASS, 0.1, 0.8, isNight ? 0x38bdf8 : 0x000000, isNight ? 0.9 : 0.0);
    // East Library Glass
    const libEast = createBoxMesh(BLDG_MAX_X - 0.2, BLDG_MAX_X + 0.3, libYMin, libYMax, NORTH_Z - 0.3, SOUTH_Z + 0.3, LIBRARY_GLASS, 0.1, 0.8, isNight ? 0x38bdf8 : 0x000000, isNight ? 0.9 : 0.0);
    // West Library Glass
    const libWest = createBoxMesh(WING_DIVIDER_X - 0.3, WING_DIVIDER_X + 0.2, libYMin, libYMax, NORTH_Z - 0.3, SOUTH_Z + 0.3, LIBRARY_GLASS, 0.1, 0.8, isNight ? 0x38bdf8 : 0x000000, isNight ? 0.9 : 0.0);

    group.add(libNorth, libSouth, libEast, libWest);
    windows.push(
      { mesh: libNorth, nightColor: 0x38bdf8, dayColor: LIBRARY_GLASS },
      { mesh: libSouth, nightColor: 0x38bdf8, dayColor: LIBRARY_GLASS },
      { mesh: libEast, nightColor: 0x38bdf8, dayColor: LIBRARY_GLASS },
      { mesh: libWest, nightColor: 0x38bdf8, dayColor: LIBRARY_GLASS }
    );

    // 7. Overhanging Dark Concrete Brow Cap & Mechanical Penthouse
    const browCap = createBoxMesh(WING_DIVIDER_X - 1.4, BLDG_MAX_X + 1.4, TOWER_HEIGHT, TOWER_HEIGHT + 1.4, NORTH_Z - 1.4, SOUTH_Z + 1.4, FAÇADE_DARK_CONCRETE);
    const roofGravel = createBoxMesh(WING_DIVIDER_X + 0.5, BLDG_MAX_X - 0.5, TOWER_HEIGHT + 1.2, TOWER_HEIGHT + 1.3, NORTH_Z + 0.5, SOUTH_Z - 0.5, ROOF_GRAVEL);
    const penthouse = createBoxMesh(WING_DIVIDER_X + 8, BLDG_MAX_X - 8, TOWER_HEIGHT + 1.4, TOWER_HEIGHT + 4.5, NORTH_Z + 6, SOUTH_Z - 6, FAÇADE_DARK_CONCRETE);
    group.add(browCap, roofGravel, penthouse);

    const centerX = (BLDG_MIN_X + BLDG_MAX_X) / 2;
    const centerZ = (NORTH_Z + SOUTH_Z) / 2;

    group.userData = { name: 'CV Raman Block', feature, floors: 12 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, TOWER_HEIGHT + 6, centerZ),
      name: 'CV Raman Block',
      category: 'Academic',
      color: '#38bdf8',
      floors: 12,
    };
  }
}
