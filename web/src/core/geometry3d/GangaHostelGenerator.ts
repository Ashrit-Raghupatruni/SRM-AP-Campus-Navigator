import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Ganga Boys' Hostel (srmap_bldg_15).
 * - 26-story monumental residential skyscraper tower (Height: 80.6m / 83.5m headhouse)
 * - Built directly on top of verified GeoJSON polygon footprint
 * - 5.5m high double-height ground pilotis colonnade with glazed reception lobby
 * - 5-story podium base with crisp white slab eyebrows
 * - Slender 21-story upper skyscraper tower in authentic Indian Terracotta Ochre (#966A55)
 * - Dark burnt umber vertical louver fins (#7C5543)
 * - 360-degree punched dormitory bedroom window matrices across North, South, East, and West facades
 * - Full horizontal slab bands wrapping around all 4 sides so no facade is left unclad/black
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
    const COLOR_NIGHT_WINDOW = 0xfbbf24;

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

    // Skyscraper tower core section
    const TOWER_MIN_X = MIN_X + WIDTH * 0.28;
    const TOWER_MAX_X = MAX_X - WIDTH * 0.05;
    const TOWER_MIN_Z = MIN_Z;
    const TOWER_MAX_Z = MIN_Z + DEPTH * 0.72;

    const PILOTIS_H = 5.5;
    const PODIUM_H = 16.5; // 5 floors
    const TOTAL_H = 80.6;  // 26 floors
    const HEADHOUSE_H = 83.5;

    // 1. Ground Pilotis Colonnade (Elevated 5.5m ground floor)
    for (let x = MIN_X + 3.0; x <= MAX_X - 3.0; x += 6.5) {
      const pNorth = createBoxMesh(x - 0.45, x + 0.45, 0, PILOTIS_H, MIN_Z, MIN_Z + 0.9, COLOR_PILOTIS);
      const pSouth = createBoxMesh(x - 0.45, x + 0.45, 0, PILOTIS_H, MAX_Z - 0.9, MAX_Z, COLOR_PILOTIS);
      group.add(pNorth, pSouth);
    }
    // Glazed double-height entrance reception lobby
    const lobby = createBoxMesh(MIN_X + 6.0, MAX_X - 6.0, 0.2, PILOTIS_H, MIN_Z + 1.2, MAX_Z - 1.2, COLOR_CURTAIN_GLASS, 0.1, 0.7);
    group.add(lobby);

    // 2. 5-Story Podium Base (Floors 1-5, Y: 5.5m to 16.5m)
    const podiumBody = createBoxMesh(MIN_X, MAX_X, PILOTIS_H, PODIUM_H, MIN_Z, MAX_Z, COLOR_TERRACOTTA_OCHRE, 0.4, 0.1);
    group.add(podiumBody);

    // Horizontal White Slab Eyebrows for Podium Floors
    for (let f = 1; f <= 5; f++) {
      const y = PILOTIS_H + (f / 5) * (PODIUM_H - PILOTIS_H);
      const slab = createBoxMesh(MIN_X - 0.6, MAX_X + 0.6, y - 0.3, y, MIN_Z - 0.6, MAX_Z + 0.6, COLOR_WHITE_SLAB, 0.3, 0.1);
      group.add(slab);

      // Podium Windows on East and South Facades
      for (let z = MIN_Z + 4.0; z <= MAX_Z - 4.0; z += 6.0) {
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

      for (let x = MIN_X + 4.0; x <= MAX_X - 4.0; x += 6.0) {
        const winGeo = new THREE.BoxGeometry(2.2, 1.6, 0.3);
        const winMat = new THREE.MeshStandardMaterial({
          color: isNight ? COLOR_NIGHT_WINDOW : COLOR_WINDOW_GLASS,
          roughness: 0.2,
          metalness: 0.8,
          emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
          emissiveIntensity: isNight ? 0.95 : 0.0,
          side: THREE.DoubleSide,
        });
        const winMesh = new THREE.Mesh(winGeo, winMat);
        winMesh.position.set(x, y - 1.0, MAX_Z + 0.1);
        group.add(winMesh);
        windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS });
      }
    }

    // 3. 26-Story Residential Tower Core (Floors 6-26, Y: 16.5m to 80.6m)
    const towerCore = createBoxMesh(TOWER_MIN_X, TOWER_MAX_X, PODIUM_H, TOTAL_H, TOWER_MIN_Z, TOWER_MAX_Z, COLOR_TERRACOTTA_OCHRE, 0.4, 0.1);
    group.add(towerCore);

    // Continuous horizontal floor bands on tower (Floors 6 to 26) across all facades
    const towerFloorH = (TOTAL_H - PODIUM_H) / 21;
    for (let f = 1; f <= 21; f++) {
      const y = PODIUM_H + f * towerFloorH;
      const towerSlab = createBoxMesh(
        TOWER_MIN_X - 0.6,
        TOWER_MAX_X + 0.6,
        y - 0.3,
        y,
        TOWER_MIN_Z - 0.6,
        TOWER_MAX_Z + 0.6,
        COLOR_WHITE_SLAB,
        0.3,
        0.1
      );
      group.add(towerSlab);

      // East Facade Windows (Facing central campus promenade)
      for (let z = TOWER_MIN_Z + 3.0; z <= TOWER_MAX_Z - 3.0; z += 5.5) {
        const winGeo = new THREE.BoxGeometry(0.3, 1.8, 2.4);
        const winMat = new THREE.MeshStandardMaterial({
          color: isNight ? COLOR_NIGHT_WINDOW : COLOR_WINDOW_GLASS,
          roughness: 0.2,
          metalness: 0.8,
          emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
          emissiveIntensity: isNight ? 0.95 : 0.0,
          side: THREE.DoubleSide,
        });
        const winMesh = new THREE.Mesh(winGeo, winMat);
        winMesh.position.set(TOWER_MAX_X + 0.1, y - 1.2, z);
        group.add(winMesh);
        windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS });
      }

      // North Facade Windows
      for (let x = TOWER_MIN_X + 3.0; x <= TOWER_MAX_X - 3.0; x += 5.5) {
        const winGeo = new THREE.BoxGeometry(2.4, 1.8, 0.3);
        const winMat = new THREE.MeshStandardMaterial({
          color: isNight ? COLOR_NIGHT_WINDOW : COLOR_WINDOW_GLASS,
          roughness: 0.2,
          metalness: 0.8,
          emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
          emissiveIntensity: isNight ? 0.95 : 0.0,
          side: THREE.DoubleSide,
        });
        const winMesh = new THREE.Mesh(winGeo, winMat);
        winMesh.position.set(x, y - 1.2, TOWER_MIN_Z - 0.1);
        group.add(winMesh);
        windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS });
      }

      // South Facade Windows (Facing directly toward SR Block & Viewer)
      for (let x = TOWER_MIN_X + 3.0; x <= TOWER_MAX_X - 3.0; x += 5.5) {
        const winGeo = new THREE.BoxGeometry(2.4, 1.8, 0.3);
        const winMat = new THREE.MeshStandardMaterial({
          color: isNight ? COLOR_NIGHT_WINDOW : COLOR_WINDOW_GLASS,
          roughness: 0.2,
          metalness: 0.8,
          emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
          emissiveIntensity: isNight ? 0.95 : 0.0,
          side: THREE.DoubleSide,
        });
        const winMesh = new THREE.Mesh(winGeo, winMat);
        winMesh.position.set(x, y - 1.2, TOWER_MAX_Z + 0.1);
        group.add(winMesh);
        windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS });
      }
    }

    // Punched vertical fins on East, North, and South facades
    for (let z = TOWER_MIN_Z + 3.0; z <= TOWER_MAX_Z - 3.0; z += 5.5) {
      const fin = createBoxMesh(TOWER_MAX_X, TOWER_MAX_X + 0.6, PODIUM_H, TOTAL_H, z - 0.15, z + 0.15, COLOR_TERRACOTTA_DARK, 0.35, 0.1);
      group.add(fin);
    }
    for (let x = TOWER_MIN_X + 3.0; x <= TOWER_MAX_X - 3.0; x += 5.5) {
      const finNorth = createBoxMesh(x - 0.15, x + 0.15, PODIUM_H, TOTAL_H, TOWER_MIN_Z - 0.6, TOWER_MIN_Z, COLOR_TERRACOTTA_DARK, 0.35, 0.1);
      const finSouth = createBoxMesh(x - 0.15, x + 0.15, PODIUM_H, TOTAL_H, TOWER_MAX_Z, TOWER_MAX_Z + 0.6, COLOR_TERRACOTTA_DARK, 0.35, 0.1);
      group.add(finNorth, finSouth);
    }

    // 4. Mid-Level Double-Height Glazed Sky Lounge (Floor 14, Y: ~44m)
    const skyLounge = createBoxMesh(
      TOWER_MIN_X + 1.5, TOWER_MAX_X - 1.5,
      44.0, 50.0,
      TOWER_MIN_Z - 0.5, TOWER_MIN_Z + 0.2,
      COLOR_CURTAIN_GLASS,
      0.1, 0.8,
      isNight ? COLOR_NIGHT_WINDOW : 0x000000,
      isNight ? 0.9 : 0.0
    );
    group.add(skyLounge);
    windows.push({ mesh: skyLounge, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_CURTAIN_GLASS });

    // 5. Rooftop Elevator Headhouse & Mechanical Chiller Screen (Y: 80.6m to 83.5m)
    const headhouse = createBoxMesh(
      TOWER_MIN_X + 4.0, TOWER_MAX_X - 4.0,
      TOTAL_H, HEADHOUSE_H,
      TOWER_MIN_Z + 4.0, TOWER_MAX_Z - 4.0,
      0xcbd5e1,
      0.4, 0.1
    );
    // Parapet coping around tower roof
    const towerParapet = createBoxMesh(TOWER_MIN_X, TOWER_MAX_X, TOTAL_H, TOTAL_H + 1.2, TOWER_MIN_Z, TOWER_MAX_Z, COLOR_WHITE_SLAB, 0.3, 0.1);
    group.add(headhouse, towerParapet);

    const centerX = (MIN_X + MAX_X) / 2;
    const centerZ = (MIN_Z + MAX_Z) / 2;

    group.userData = { name: "Ganga Boys' Hostel", feature, floors: 26 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, TOTAL_H + 6, centerZ),
      name: "Ganga Boys' Hostel",
      category: 'Hostel',
      color: '#f43f5e',
      floors: 26,
    };
  }
}
