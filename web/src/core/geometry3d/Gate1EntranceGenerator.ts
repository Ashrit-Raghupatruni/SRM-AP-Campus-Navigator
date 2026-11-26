import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';

/**
 * Procedural 3D Architectural Reconstruction for Gate 1 Main Entrance Complex (srmap_gate_1).
 * Deconstructivist Campus Gateway:
 * - Sweeping 42-meter white cantilevered wing canopy (H = 6.2m)
 * - Soaring 16.5-meter University Crimson sculptural fin (#A61B35)
 * - Security cabin with tempered security glass (#38BDF8)
 */
export class Gate1EntranceGenerator {
  static generate(): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_SRM_CRIMSON = 0xa61b35;
    const COLOR_CANOPY_WHITE = 0xffffff;
    const COLOR_CABIN_SLATE = 0x334155;
    const COLOR_GLASS = 0x38bdf8;

    // Gate 1 coordinates at southern campus entry road (near Z ~ 380m)
    const GATE_X = 95.0;
    const GATE_Z = 390.0;

    // 1. Soaring 16.5m SRM Crimson Sculptural Fin
    const finGeo = new THREE.BoxGeometry(1.4, 16.5, 4.5);
    const finMat = new THREE.MeshStandardMaterial({
      color: COLOR_SRM_CRIMSON,
      roughness: 0.2,
      metalness: 0.4,
    });
    const finMesh = new THREE.Mesh(finGeo, finMat);
    finMesh.position.set(GATE_X - 12.0, 8.25, GATE_Z);
    finMesh.rotation.z = -0.08; // Deconstructivist slant
    finMesh.castShadow = true;
    group.add(finMesh);

    // 2. Sweeping 42m Cantilevered Canopy Wing
    const wingGeo = new THREE.BoxGeometry(42.0, 0.8, 12.0);
    const wingMat = new THREE.MeshStandardMaterial({
      color: COLOR_CANOPY_WHITE,
      roughness: 0.3,
      metalness: 0.2,
    });
    const wingMesh = new THREE.Mesh(wingGeo, wingMat);
    wingMesh.position.set(GATE_X, 6.2, GATE_Z);
    wingMesh.rotation.x = -0.05; // Gentle aerodynamic slope
    wingMesh.castShadow = true;
    wingMesh.receiveShadow = true;
    group.add(wingMesh);

    // 3. Security Checkpoint Cabins
    const cabinEast = createBoxMesh(GATE_X + 8.0, GATE_X + 13.0, 0, 3.8, GATE_Z - 3.0, GATE_Z + 3.0, COLOR_CABIN_SLATE);
    const cabinWest = createBoxMesh(GATE_X - 16.0, GATE_X - 11.0, 0, 3.8, GATE_Z - 3.0, GATE_Z + 3.0, COLOR_CABIN_SLATE);
    // Security Windows
    const winEast = createBoxMesh(GATE_X + 8.2, GATE_X + 12.8, 1.2, 3.2, GATE_Z - 3.1, GATE_Z + 3.1, COLOR_GLASS, 0.1, 0.8);
    group.add(cabinEast, cabinWest, winEast);

    group.userData = { name: 'Gate 1 Main Entrance', floors: 1 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(GATE_X, 18.0, GATE_Z),
      name: 'Gate 1 Main Entrance',
      category: 'Useful spots',
      color: '#a61b35',
      floors: 1,
    };
  }
}
