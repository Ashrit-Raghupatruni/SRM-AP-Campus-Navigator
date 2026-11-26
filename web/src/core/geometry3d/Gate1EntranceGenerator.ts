import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Gate 1 Main Entrance Complex (srmap_gate_1).
 * Deconstructivist Campus Gateway:
 * - Sweeping 42-meter white cantilevered wing canopy (H = 6.2m)
 * - Soaring 16.5-meter University Crimson sculptural fin (#A61B35)
 * - Security cabin with tempered security glass (#38BDF8)
 * - Accurately positioned at southern entrance boulevard (lat: 16.4599, lng: 80.5070 -> local X: ~16, Z: ~460)
 */
export class Gate1EntranceGenerator {
  static generate(facilitiesGeoJSON?: any): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_SRM_CRIMSON = 0xa61b35;
    const COLOR_CANOPY_WHITE = 0xffffff;
    const COLOR_CABIN_SLATE = 0x334155;
    const COLOR_GLASS = 0x38bdf8;

    // Default Gate 1 coordinates at southern campus entrance
    let gateX = 16.0;
    let gateZ = 460.0;

    if (facilitiesGeoJSON && facilitiesGeoJSON.features) {
      const gateFeature = facilitiesGeoJSON.features.find((f: any) =>
        f.id === 'way/1472648751' ||
        f.properties?.office === 'security' && f.geometry?.coordinates?.[0]?.[0]?.[1] < 16.4605
      );
      if (gateFeature?.geometry?.coordinates?.[0]) {
        let sumX = 0, sumZ = 0, count = 0;
        for (const [lng, lat] of gateFeature.geometry.coordinates[0]) {
          const p = latLngToLocal(lat, lng);
          sumX += p.x;
          sumZ += p.z;
          count++;
        }
        if (count > 0) {
          gateX = sumX / count;
          gateZ = sumZ / count;
        }
      }
    }

    const GATE_X = gateX;
    const GATE_Z = gateZ;

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

    // 2. Sweeping 42m Aerodynamic Cantilevered Canopy Wing
    const canopyShape = new THREE.Shape();
    canopyShape.moveTo(-22, -0.6);
    canopyShape.lineTo(20, -0.6);
    canopyShape.lineTo(18, 0.6);
    canopyShape.lineTo(-20, 0.6);
    canopyShape.closePath();

    const canopyGeo = new THREE.ExtrudeGeometry(canopyShape, {
      depth: 9.0,
      bevelEnabled: true,
      bevelSegments: 2,
      bevelSize: 0.2,
      bevelThickness: 0.2,
    });
    // Rotate to lie horizontally
    canopyGeo.rotateX(Math.PI / 2);

    const canopyMat = new THREE.MeshStandardMaterial({
      color: COLOR_CANOPY_WHITE,
      roughness: 0.3,
      metalness: 0.2,
    });
    const canopyMesh = new THREE.Mesh(canopyGeo, canopyMat);
    canopyMesh.position.set(GATE_X, 6.2, GATE_Z - 4.5);
    canopyMesh.castShadow = true;
    group.add(canopyMesh);

    // 3. Central Glazed Security Guardhouse / Control Cabin
    const cabin = createBoxMesh(
      GATE_X - 2.5, GATE_X + 2.5,
      0, 3.8,
      GATE_Z - 3.0, GATE_Z + 3.0,
      COLOR_CABIN_SLATE,
      0.4, 0.2
    );
    // Security tempered wrap-around viewing window
    const cabinWindow = createBoxMesh(
      GATE_X - 2.6, GATE_X + 2.6,
      1.2, 2.6,
      GATE_Z - 3.1, GATE_Z + 3.1,
      COLOR_GLASS,
      0.1, 0.8
    );
    group.add(cabin, cabinWindow);

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(GATE_X, 18.0, GATE_Z),
      name: 'Gate 1 Main Entrance',
      category: 'Useful spots',
      color: '#a61b35',
      floors: 0,
    };
  }
}
