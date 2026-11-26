import * as THREE from 'three';
import { ArchitecturalMeshResult } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for "The Flying Umbrella"
 * - Monumental space-frame canopy arching over the central breezeway canyon
 *   between Vikram Sarabhai Block (V-Block) and Homi J. Bhabha Block.
 * - Anchored dynamically using the verified GeoJSON coordinates of V-Block and Homi Bhabha.
 * - Supported by tubular steel tree pylon columns (#E2E8F0)
 * - Aerodynamic curved roof surface in warm grey metal (#8F9CA8)
 */
export class AcademicComplexCanopyGenerator {
  static generate(buildingsGeoJSON?: any): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const CANOPY_COLOR = 0x8f9ca8;
    const STEEL_TRUSS_COLOR = 0x718096;
    const TREE_COLUMN_COLOR = 0xe2e8f0;

    // Default or dynamically computed bounding center between V-Block and Homi Bhabha
    let vMinX = 28.5, vMaxX = 110.4, minZ = -113.4, maxZ = -53.0;
    let hMinX = 126.9, hMaxX = 168.5;

    if (buildingsGeoJSON && buildingsGeoJSON.features) {
      const v = buildingsGeoJSON.features.find((f: any) => f.properties?.name?.includes('V Block') || f.properties?.name?.includes('Sarabhai'));
      const h = buildingsGeoJSON.features.find((f: any) => f.properties?.name?.includes('Bhabha') || f.properties?.name?.includes('Homi'));

      if (v?.geometry?.coordinates?.[0]) {
        const coords = v.geometry.coordinates[0];
        vMinX = Math.min(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).x));
        vMaxX = Math.max(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).x));
        minZ = Math.min(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).z));
        maxZ = Math.max(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).z));
      }
      if (h?.geometry?.coordinates?.[0]) {
        const coords = h.geometry.coordinates[0];
        hMinX = Math.min(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).x));
        hMaxX = Math.max(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).x));
      }
    }

    // Canyon center and span
    const CANOPY_CX = (vMaxX + hMinX) / 2; // ~118.6m
    const CANOPY_CZ = (minZ + maxZ) / 2;  // ~ -83.2m
    const CANOPY_W = (hMaxX - vMinX) * 0.75; // Arch width spanning both inner blocks
    const CANOPY_L = (maxZ - minZ) + 12.0;   // Length along breezeway canyon

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
      metalness: 0.5,
      side: THREE.DoubleSide,
    });
    const canopyMesh = new THREE.Mesh(canopyGeo, canopyMat);
    canopyMesh.position.set(CANOPY_CX, 21.0, CANOPY_CZ);
    canopyMesh.rotation.z = Math.PI / 2;
    canopyMesh.rotation.y = Math.PI / 2;
    canopyMesh.scale.set(0.25, 1.0, 1.0); // Flatten cylinder into an aerodynamic arch
    canopyMesh.castShadow = true;
    group.add(canopyMesh);

    // 2. Tubular Steel Tree Pylon Columns (Branching Supports)
    const treeColPositions = [
      { x: CANOPY_CX - CANOPY_W * 0.28, z: CANOPY_CZ - CANOPY_L * 0.3 },
      { x: CANOPY_CX - CANOPY_W * 0.28, z: CANOPY_CZ + CANOPY_L * 0.3 },
      { x: CANOPY_CX + CANOPY_W * 0.28, z: CANOPY_CZ - CANOPY_L * 0.3 },
      { x: CANOPY_CX + CANOPY_W * 0.28, z: CANOPY_CZ + CANOPY_L * 0.3 },
    ];

    const trunkGeo = new THREE.CylinderGeometry(0.55, 0.65, 17.5, 16);
    const trunkMat = new THREE.MeshStandardMaterial({
      color: TREE_COLUMN_COLOR,
      roughness: 0.3,
      metalness: 0.4,
    });

    for (const pos of treeColPositions) {
      // Main trunk
      const trunk = new THREE.Mesh(trunkGeo, trunkMat);
      trunk.position.set(pos.x, 8.75, pos.z);
      trunk.castShadow = true;
      group.add(trunk);

      // 4-Way Branching Structural Struts
      for (let angle = 0; angle < Math.PI * 2; angle += Math.PI / 2) {
        const branchGeo = new THREE.CylinderGeometry(0.2, 0.28, 6.0, 12);
        const branch = new THREE.Mesh(branchGeo, trunkMat);
        branch.position.set(
          pos.x + Math.cos(angle) * 1.8,
          18.8,
          pos.z + Math.sin(angle) * 1.8
        );
        branch.rotation.z = Math.cos(angle) * 0.35;
        branch.rotation.x = Math.sin(angle) * 0.35;
        branch.castShadow = true;
        group.add(branch);
      }
    }

    // 3. Structural Perimeter Trusses
    const trussMat = new THREE.MeshStandardMaterial({ color: STEEL_TRUSS_COLOR, metalness: 0.6 });
    const spineTrussGeo = new THREE.BoxGeometry(0.6, 0.6, CANOPY_L);
    const spineTruss = new THREE.Mesh(spineTrussGeo, trussMat);
    spineTruss.position.set(CANOPY_CX, 23.5, CANOPY_CZ);
    group.add(spineTruss);

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(CANOPY_CX, 27.0, CANOPY_CZ),
      name: 'The Flying Umbrella (Canopy)',
      category: 'Academic',
      color: '#38bdf8',
      floors: 0,
    };
  }
}
