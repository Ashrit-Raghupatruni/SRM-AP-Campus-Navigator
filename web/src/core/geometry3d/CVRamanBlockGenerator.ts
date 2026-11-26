import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';

/**
 * Procedural 3D Architectural Reconstruction for CV Raman Block (Academic Block III / srmap_bldg_20).
 * - 12-Story Concrete Monolith (Height: 42.0m)
 * - 5-Story Western Annex Wing (X in [-124.9, -104.0], H = 18.0m)
 * - Monumental Dual-Tier Sky-Atrium Voids on the North Facade (Floors 5-7) with warm study lounge glow
 * - Full-height vertical sky-blue glass curtain spines flanking the voids
 * - Continuous wrap-around ribbon glass for top floors (Floors 10-12 / Central Library)
 * - Overhanging dark concrete brow cap and mechanical penthouse
 */
export class CVRamanBlockGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    // Authentic Material Colors
    const FAÇADE_PRECAST_WHITE = 0xf1f5f9;
    const FAÇADE_DARK_CONCRETE = 0x475569;
    const CURTAIN_GLASS = 0x38bdf8;
    const VOID_BACKWALL_WARM = 0xfde047;
    const ROOF_GRAVEL = 0x94a3b8;
    const PILOTIS_COLOR = 0x334155;

    // Metrics in Local Tangent ENU
    const BLDG_MIN_X = -124.9;
    const BLDG_MAX_X = -40.6;
    const NORTH_Z = 209.0;
    const SOUTH_Z = 244.1;
    const WING_DIVIDER_X = -104.0;
    const WING_HEIGHT = 18.0;
    const TOWER_HEIGHT = 42.0;

    const VOID_MIN_X = -88.0;
    const VOID_MAX_X = -56.0;
    const VOID_DEPTH_Z = 10.0;
    const LOWER_VOID_MIN_Y = 13.5;
    const LOWER_VOID_MAX_Y = 21.0;
    const UPPER_VOID_MIN_Y = 23.5;
    const UPPER_VOID_MAX_Y = 31.0;

    // 1. Ground Pilotis
    for (let x = BLDG_MIN_X + 4; x <= BLDG_MAX_X - 4; x += 9.0) {
      const colNorth = createBoxMesh(x - 0.5, x + 0.5, 0, 5.0, NORTH_Z, NORTH_Z + 1.0, PILOTIS_COLOR);
      const colSouth = createBoxMesh(x - 0.5, x + 0.5, 0, 5.0, SOUTH_Z - 1.0, SOUTH_Z, PILOTIS_COLOR);
      group.add(colNorth, colSouth);
    }

    // Double-Height Glazed Ground Reception Lobby
    const groundLobby = createBoxMesh(WING_DIVIDER_X + 4, BLDG_MAX_X - 4, 0.2, 5.0, NORTH_Z + 1.0, SOUTH_Z - 1.0, 0x1e293b, 0.2, 0.7);
    group.add(groundLobby);

    // 2. 5-Story Western Annex Wing (H = 18m)
    const westWing = createBoxMesh(BLDG_MIN_X, WING_DIVIDER_X, 5.0, WING_HEIGHT, NORTH_Z, SOUTH_Z, FAÇADE_PRECAST_WHITE);
    group.add(westWing);

    // West Wing Rooftop Parapet
    const westParapet = createBoxMesh(BLDG_MIN_X, WING_DIVIDER_X, WING_HEIGHT, WING_HEIGHT + 1.2, NORTH_Z, SOUTH_Z, FAÇADE_DARK_CONCRETE);
    group.add(westParapet);

    // 3. 12-Story Main Tower Solid Blocks (Flanking the center void)
    const leftTower = createBoxMesh(WING_DIVIDER_X, VOID_MIN_X, 5.0, TOWER_HEIGHT, NORTH_Z, SOUTH_Z, FAÇADE_PRECAST_WHITE);
    const rightTower = createBoxMesh(VOID_MAX_X, BLDG_MAX_X, 5.0, TOWER_HEIGHT, NORTH_Z, SOUTH_Z, FAÇADE_PRECAST_WHITE);
    const rearTower = createBoxMesh(VOID_MIN_X, VOID_MAX_X, 5.0, TOWER_HEIGHT, NORTH_Z + VOID_DEPTH_Z, SOUTH_Z, FAÇADE_PRECAST_WHITE);
    group.add(leftTower, rightTower, rearTower);

    // Floors 1-4 Solid Podium Base beneath voids
    const voidPodium = createBoxMesh(VOID_MIN_X, VOID_MAX_X, 5.0, LOWER_VOID_MIN_Y, NORTH_Z, NORTH_Z + VOID_DEPTH_Z, FAÇADE_PRECAST_WHITE);
    group.add(voidPodium);

    // Floors 10-12 Monumental Bridge over upper void (Floors 10-12 / Central Library)
    const crownBridge = createBoxMesh(VOID_MIN_X, VOID_MAX_X, UPPER_VOID_MAX_Y, TOWER_HEIGHT, NORTH_Z, NORTH_Z + VOID_DEPTH_Z, FAÇADE_PRECAST_WHITE);
    group.add(crownBridge);

    // Bridge between lower and upper voids (Floors 6-7 deck)
    const midBridge = createBoxMesh(VOID_MIN_X, VOID_MAX_X, LOWER_VOID_MAX_Y, UPPER_VOID_MIN_Y, NORTH_Z, NORTH_Z + VOID_DEPTH_Z, FAÇADE_DARK_CONCRETE);
    group.add(midBridge);

    // 4. Monumental Sky-Atrium Voids with Warm Glowing Study Lounges
    // Lower Void Lounge Backwall
    const lowerVoidGlow = createBoxMesh(
      VOID_MIN_X + 0.5, VOID_MAX_X - 0.5,
      LOWER_VOID_MIN_Y + 0.5, LOWER_VOID_MAX_Y - 0.5,
      NORTH_Z + VOID_DEPTH_Z - 0.3, NORTH_Z + VOID_DEPTH_Z,
      VOID_BACKWALL_WARM,
      0.3, 0.1,
      isNight ? 0xfbbf24 : 0x000000,
      isNight ? 0.9 : 0.0
    );
    group.add(lowerVoidGlow);
    windows.push({ mesh: lowerVoidGlow, nightColor: 0xfbbf24, dayColor: VOID_BACKWALL_WARM });

    // Upper Void Lounge Backwall
    const upperVoidGlow = createBoxMesh(
      VOID_MIN_X + 0.5, VOID_MAX_X - 0.5,
      UPPER_VOID_MIN_Y + 0.5, UPPER_VOID_MAX_Y - 0.5,
      NORTH_Z + VOID_DEPTH_Z - 0.3, NORTH_Z + VOID_DEPTH_Z,
      VOID_BACKWALL_WARM,
      0.3, 0.1,
      isNight ? 0xfbbf24 : 0x000000,
      isNight ? 0.9 : 0.0
    );
    group.add(upperVoidGlow);
    windows.push({ mesh: upperVoidGlow, nightColor: 0xfbbf24, dayColor: VOID_BACKWALL_WARM });

    // Safety glass railings across both voids
    const lowerRailing = createBoxMesh(VOID_MIN_X, VOID_MAX_X, LOWER_VOID_MIN_Y + 0.2, LOWER_VOID_MIN_Y + 1.4, NORTH_Z - 0.1, NORTH_Z + 0.1, CURTAIN_GLASS, 0.1, 0.8);
    const upperRailing = createBoxMesh(VOID_MIN_X, VOID_MAX_X, UPPER_VOID_MIN_Y + 0.2, UPPER_VOID_MIN_Y + 1.4, NORTH_Z - 0.1, NORTH_Z + 0.1, CURTAIN_GLASS, 0.1, 0.8);
    group.add(lowerRailing, upperRailing);

    // 5. Vertical Sky-Blue Glass Curtain Spines (Ground to Roof)
    const spineW = 3.6;
    const leftGlassSpine = createBoxMesh(VOID_MIN_X - spineW, VOID_MIN_X, 5.0, TOWER_HEIGHT, NORTH_Z - 0.3, NORTH_Z + 0.2, CURTAIN_GLASS, 0.1, 0.6);
    const rightGlassSpine = createBoxMesh(VOID_MAX_X, VOID_MAX_X + spineW, 5.0, TOWER_HEIGHT, NORTH_Z - 0.3, NORTH_Z + 0.2, CURTAIN_GLASS, 0.1, 0.6);
    group.add(leftGlassSpine, rightGlassSpine);

    // 6. Floors 10-12 Continuous Panoramic Ribbon Curtain Glass (Central Library)
    const libraryGlazing = createBoxMesh(WING_DIVIDER_X - 0.2, BLDG_MAX_X + 0.2, TOWER_HEIGHT - 9.0, TOWER_HEIGHT - 1.2, NORTH_Z - 0.25, NORTH_Z + 0.15, 0x1e3a5f, 0.15, 0.7);
    group.add(libraryGlazing);

    // 7. Overhanging Dark Brow Cap & Mechanical Penthouse
    const browCap = createBoxMesh(WING_DIVIDER_X - 1.2, BLDG_MAX_X + 1.2, TOWER_HEIGHT, TOWER_HEIGHT + 1.4, NORTH_Z - 1.4, SOUTH_Z + 1.2, FAÇADE_DARK_CONCRETE);
    const roofGravel = createBoxMesh(WING_DIVIDER_X + 0.5, BLDG_MAX_X - 0.5, TOWER_HEIGHT + 1.2, TOWER_HEIGHT + 1.3, NORTH_Z + 0.5, SOUTH_Z - 0.5, ROOF_GRAVEL);
    const penthouse = createBoxMesh(WING_DIVIDER_X + 10, BLDG_MAX_X - 10, TOWER_HEIGHT + 1.4, TOWER_HEIGHT + 4.5, NORTH_Z + 8, SOUTH_Z - 8, FAÇADE_DARK_CONCRETE);
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
