import * as THREE from 'three';

export interface ArchitecturalMeshResult {
  group: THREE.Group;
  windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[];
  labelPos: THREE.Vector3;
  name: string;
  category: string;
  color: string;
  floors: number;
}

/**
 * Creates an oriented box mesh between two 3D corners with given material.
 */
export function createBoxMesh(
  minX: number, maxX: number,
  minY: number, maxY: number,
  minZ: number, maxZ: number,
  color: number,
  roughness = 0.45,
  metalness = 0.15,
  emissive = 0x000000,
  emissiveIntensity = 0.0
): THREE.Mesh {
  const width = Math.abs(maxX - minX);
  const height = Math.abs(maxY - minY);
  const depth = Math.abs(maxZ - minZ);

  const geo = new THREE.BoxGeometry(width, height, depth);
  const mat = new THREE.MeshStandardMaterial({
    color,
    roughness,
    metalness,
    emissive,
    emissiveIntensity,
  });

  const mesh = new THREE.Mesh(geo, mat);
  mesh.position.set((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  return mesh;
}
