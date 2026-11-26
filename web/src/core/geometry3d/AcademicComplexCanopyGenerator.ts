import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';

/**
 * Procedural 3D Architectural Reconstruction for "The Flying Umbrella"
 * - 148m x 76.5m space-frame canopy arching over the 12.8m Central Breezeway canyon
 *   between Vikram Sarabhai Block (V-Block) and Homi J. Bhabha Block.
 * - Supported by tubular steel tree pylon columns (#E2E8F0)
 * - Aerodynamic curved roof surface in warm grey metal (#8F9CA8)
 */
export class AcademicComplexCanopyGenerator {
  static generate(): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const CANOPY_COLOR = 0x8f9ca8;
    const STEEL_TRUSS_COLOR = 0x718096;
    const TREE_COLUMN_COLOR = 0xe2e8f0;

    // Center coordinates between V-Block and Homi Bhabha
    const CANOPY_CX = 105.0;
    const CANOPY_CZ = -88.0;
    const CANOPY_W = 76.5;
    const CANOPY_L = 148.0;

    // 1. Aerodynamic Curving Space-Frame Canopy Shell
    // Parametric curved roof shell peaking at 25.0m
    const canopyGeo = new THREE.CylinderGeometry(
      CANOPY_W / 2, CANOPY_W / 2, CANOPY_L,
      32, 1, false,
      0, Math.PI
    );
    const canopyMat = new THREE.MeshStandardMaterial({
      color: CANOPY_COLOR,
      roughness: 0.35,
      metalness: 0.45,
      side: THREE.DoubleSide,
    });
    const canopyMesh = new THREE.Mesh(canopyGeo, canopyMat);
    canopyMesh.rotation.z = Math.PI / 2;
    canopyMesh.rotation.y = Math.PI / 2;
    canopyMesh.position.set(CANOPY_CX, 14.5, CANOPY_CZ);
    canopyMesh.scale.set(1.0, 0.32, 1.0); // Flattened arch
    canopyMesh.castShadow = true;
    canopyMesh.receiveShadow = true;
    group.add(canopyMesh);

    // 2. 4-Way Branching Steel Tree Pylon Columns
    const columnPositions = [
      { x: CANOPY_CX - 22.0, z: CANOPY_CZ - 45.0 },
      { x: CANOPY_CX + 22.0, z: CANOPY_CZ - 45.0 },
      { x: CANOPY_CX - 22.0, z: CANOPY_CZ + 45.0 },
      { x: CANOPY_CX + 22.0, z: CANOPY_CZ + 45.0 },
    ];

    const colGeo = new THREE.CylinderGeometry(0.8, 1.0, 22.0, 12);
    const colMat = new THREE.MeshStandardMaterial({
      color: TREE_COLUMN_COLOR,
      roughness: 0.3,
      metalness: 0.3,
    });

    for (const cp of columnPositions) {
      const colMesh = new THREE.Mesh(colGeo, colMat);
      colMesh.position.set(cp.x, 11.0, cp.z);
      colMesh.castShadow = true;
      group.add(colMesh);

      // Branching diagonal struts reaching out to the canopy
      for (let angle = 0; angle < Math.PI * 2; angle += Math.PI / 2) {
        const strutGeo = new THREE.CylinderGeometry(0.35, 0.35, 9.0, 8);
        const strutMesh = new THREE.Mesh(strutGeo, colMat);
        strutMesh.position.set(cp.x + Math.cos(angle) * 3.0, 22.0, cp.z + Math.sin(angle) * 3.0);
        strutMesh.rotation.z = Math.cos(angle) * 0.4;
        strutMesh.rotation.x = Math.sin(angle) * 0.4;
        group.add(strutMesh);
      }
    }

    // 3. Glazed Skywalk Link Bridges across the Breezeway
    for (const bridgeZ of [CANOPY_CZ - 25.0, CANOPY_CZ + 25.0]) {
      const bridge = createBoxMesh(
        CANOPY_CX - 15.0, CANOPY_CX + 15.0,
        10.0, 14.2,
        bridgeZ - 2.5, bridgeZ + 2.5,
        0x38bdf8,
        0.1, 0.7
      );
      group.add(bridge);
    }

    group.userData = { name: 'Academic Canopy ("The Flying Umbrella")', floors: 1 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(CANOPY_CX, 27.0, CANOPY_CZ),
      name: 'Academic Canopy',
      category: 'Academic',
      color: '#8f9ca8',
      floors: 1,
    };
  }
}
