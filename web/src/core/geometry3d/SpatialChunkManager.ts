import * as THREE from 'three';
import { ChunkData } from '../../workers/campusWorker';
import { ArchitecturalBuildingBuilder } from './ArchitecturalBuildingBuilder';

/**
 * SpatialChunkManager
 * Controls 3D visual groups for each of the 5 campus zones.
 * Enables/disables rendering and controls LOD visibility based on camera distance.
 */
export class SpatialChunkManager {
  private chunks: Map<string, { group: THREE.Group; data: ChunkData; active: boolean }> = new Map();
  private scene: THREE.Scene;
  private isNight: boolean;

  constructor(scene: THREE.Scene, isNight: boolean = false) {
    this.scene = scene;
    this.isNight = isNight;
  }

  setNight(isNight: boolean) {
    this.isNight = isNight;
  }

  /**
   * Mount or update a spatial chunk in 3D scene space
   */
  ingestChunk(chunk: ChunkData, facilitiesGeoJSON?: any): {
    buildingMeshes: THREE.Mesh[];
    windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[];
    labels: { id: string; name: string; category: string; color: string; pos: THREE.Vector3; feature: any }[];
  } {
    // If chunk was already built, clear previous group
    const existing = this.chunks.get(chunk.id);
    if (existing) {
      this.scene.remove(existing.group);
      existing.group.clear();
    }

    const chunkGroup = new THREE.Group();
    chunkGroup.name = chunk.id;
    this.scene.add(chunkGroup);

    // Build building meshes for this chunk
    const fakeGeoJSON = { type: 'FeatureCollection', features: chunk.features };
    const built = ArchitecturalBuildingBuilder.buildCampus(fakeGeoJSON, this.isNight, facilitiesGeoJSON);

    chunkGroup.add(built.rootGroup);
    this.chunks.set(chunk.id, { group: chunkGroup, data: chunk, active: true });

    return {
      buildingMeshes: built.buildingMeshes,
      windows: built.windows,
      labels: built.labels,
    };
  }

  /**
   * Update visibility of chunks based on distance to camera focus
   */
  updateLOD(camFocusX: number, camFocusZ: number, zoomRadius: number) {
    // Distance threshold: if zoomed in, distant chunks can switch to lower detail or be hidden
    this.chunks.forEach((chunkEntry) => {
      // Chunk center estimate
      let cx = 0, cz = 0;
      if (chunkEntry.data.features.length > 0) {
        // Average coordinates
        cx = chunkEntry.group.position.x;
        cz = chunkEntry.group.position.z;
      }

      const dist = Math.hypot(cx - camFocusX, cz - camFocusZ);
      // Keep chunks visible unless extremely far (> 650m and zoomed in close)
      const shouldBeVisible = zoomRadius > 350 || dist < 450;
      chunkEntry.group.visible = shouldBeVisible;
    });
  }

  clear() {
    this.chunks.forEach((c) => {
      this.scene.remove(c.group);
      c.group.clear();
    });
    this.chunks.clear();
  }
}
