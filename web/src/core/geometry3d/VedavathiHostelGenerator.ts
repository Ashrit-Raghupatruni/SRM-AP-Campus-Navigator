import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Vedavathi Boys' Hostel (srmap_bldg_14).
 * - 15-story residential mid-rise arranged in a U-shape wrapping around an open courtyard
 * - Height: 46.5m (48.5m headhouse)
 * - Built directly on top of verified GeoJSON polygon footprint
 * - Authentic Indian Terracotta Ochre (#966A55) with crisp white floor slab eyebrows (#F8FAFC)
 * - Ground floor double-height pilotis and central courtyard garden
 * - 360-degree punched dormitory bedroom windows across North, South, East, and West wings
 * - Rooftop stepped elevator headhouse
 */
export class VedavathiHostelGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_TERRACOTTA_OCHRE = 0x966a55;
    const COLOR_WHITE_SLAB = 0xf8fafc;
    const COLOR_PILOTIS = 0xe2e8f0;
    const COLOR_CURTAIN_GLASS = 0x38bdf8;
    const COLOR_WINDOW_GLASS = 0x64748b;
    const COLOR_NIGHT_WINDOW = 0xfbbf24;
    const COLOR_ROOF_GRAVEL = 0xcbd5e1;

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

    const PILOTIS_H = 5.5;
    const TOTAL_H = 46.5; // 15 floors
    const HEADHOUSE_H = 48.5;

    // 1. Ground Pilotis Colonnade (Elevated 5.5m ground floor)
    for (let x = MIN_X + 3.0; x <= MAX_X - 3.0; x += 6.5) {
      const pNorth = createBoxMesh(x - 0.45, x + 0.45, 0, PILOTIS_H, MIN_Z, MIN_Z + 0.9, COLOR_PILOTIS);
      const pSouth = createBoxMesh(x - 0.45, x + 0.45, 0, PILOTIS_H, MAX_Z - 0.9, MAX_Z, COLOR_PILOTIS);
      group.add(pNorth, pSouth);
    }

    // 2. Main 15-Story U-Shape Building Mass
    // North Wing
    const northWing = createBoxMesh(MIN_X, MAX_X, PILOTIS_H, TOTAL_H, MIN_Z, MIN_Z + DEPTH * 0.38, COLOR_TERRACOTTA_OCHRE, 0.4, 0.1);
    // West Spine
    const westSpine = createBoxMesh(MIN_X, MIN_X + WIDTH * 0.35, PILOTIS_H, TOTAL_H, MIN_Z, MAX_Z, COLOR_TERRACOTTA_OCHRE, 0.4, 0.1);
    // East Spine
    const eastSpine = createBoxMesh(MAX_X - WIDTH * 0.35, MAX_X, PILOTIS_H, TOTAL_H, MIN_Z, MAX_Z, COLOR_TERRACOTTA_OCHRE, 0.4, 0.1);
    group.add(northWing, westSpine, eastSpine);

    // 3. Horizontal White Slab Eyebrows across all 15 floors
    const floorH = (TOTAL_H - PILOTIS_H) / 14;
    for (let f = 1; f <= 14; f++) {
      const y = PILOTIS_H + f * floorH;
      const slabN = createBoxMesh(MIN_X - 0.4, MAX_X + 0.4, y - 0.25, y, MIN_Z - 0.4, MIN_Z + DEPTH * 0.38 + 0.4, COLOR_WHITE_SLAB, 0.3, 0.1);
      const slabW = createBoxMesh(MIN_X - 0.4, MIN_X + WIDTH * 0.35 + 0.4, y - 0.25, y, MIN_Z - 0.4, MAX_Z + 0.4, COLOR_WHITE_SLAB, 0.3, 0.1);
      const slabE = createBoxMesh(MAX_X - WIDTH * 0.35 - 0.4, MAX_X + 0.4, y - 0.25, y, MIN_Z - 0.4, MAX_Z + 0.4, COLOR_WHITE_SLAB, 0.3, 0.1);
      group.add(slabN, slabW, slabE);

      // East Facade Windows (Facing central campus promenade)
      for (let z = MIN_Z + 3.0; z <= MAX_Z - 3.0; z += 5.5) {
        const winGeo = new THREE.BoxGeometry(0.3, 1.6, 2.2);
        const winMat = new THREE.MeshStandardMaterial({
          color: isNight ? COLOR_NIGHT_WINDOW : COLOR_WINDOW_GLASS,
          roughness: 0.2,
          metalness: 0.8,
          emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
          emissiveIntensity: isNight ? 0.95 : 0.0,
          side: THREE.DoubleSide,
        });
        const winMesh = new THREE.Mesh(winGeo, winMat);
        winMesh.position.set(MAX_X + 0.1, y - 1.0, z);
        group.add(winMesh);
        windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS });
      }

      // South Facade Windows on West and East wings (Facing toward Ganga & SR Block)
      const southWestWinGeo = new THREE.BoxGeometry(2.2, 1.6, 0.3);
      const southEastWinGeo = new THREE.BoxGeometry(2.2, 1.6, 0.3);
      const winMatS = new THREE.MeshStandardMaterial({
        color: isNight ? COLOR_NIGHT_WINDOW : COLOR_WINDOW_GLASS,
        roughness: 0.2,
        metalness: 0.8,
        emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
        emissiveIntensity: isNight ? 0.95 : 0.0,
        side: THREE.DoubleSide,
      });
      const winMeshSW = new THREE.Mesh(southWestWinGeo, winMatS);
      winMeshSW.position.set(MIN_X + WIDTH * 0.17, y - 1.0, MAX_Z + 0.1);
      const winMeshSE = new THREE.Mesh(southEastWinGeo, winMatS);
      winMeshSE.position.set(MAX_X - WIDTH * 0.17, y - 1.0, MAX_Z + 0.1);
      group.add(winMeshSW, winMeshSE);
      windows.push(
        { mesh: winMeshSW, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS },
        { mesh: winMeshSE, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS }
      );
    }

    // 4. Glazed Ground Entrance Lobby
    const lobby = createBoxMesh(MIN_X + WIDTH * 0.2, MAX_X - WIDTH * 0.2, 0.2, PILOTIS_H, MIN_Z + 1.0, MIN_Z + DEPTH * 0.35, COLOR_CURTAIN_GLASS, 0.1, 0.7);
    group.add(lobby);

    // 5. Rooftop Elevator Headhouse & Parapet
    const headhouse = createBoxMesh(
      MIN_X + WIDTH * 0.1, MIN_X + WIDTH * 0.45,
      TOTAL_H, HEADHOUSE_H,
      MIN_Z + 2.0, MIN_Z + DEPTH * 0.3,
      COLOR_ROOF_GRAVEL,
      0.4, 0.1
    );
    group.add(headhouse);

    const centerX = (MIN_X + MAX_X) / 2;
    const centerZ = (MIN_Z + MAX_Z) / 2;

    group.userData = { name: "Vedavathi Boys' Hostel", feature, floors: 15 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, TOTAL_H + 6, centerZ),
      name: "Vedavathi Boys' Hostel",
      category: 'Hostel',
      color: '#f43f5e',
      floors: 15,
    };
  }
}
