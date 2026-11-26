import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Modular Hostels (Yamuna, Narmada, Godavari, Krishna, Kaveri).
 * - 9 Floors (Height: 29.5m)
 * - Clean Slate Off-White primary facade (#F1F5F9)
 * - Charcoal grey recessed window slots (#334155)
 * - Flat roof terrace with parapet coping (#CBD5E1)
 */
export class ModularHostelGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_WALL_SLATE = 0xf1f5f9;
    const COLOR_SPANDREL_CHARCOAL = 0x334155;
    const COLOR_WINDOW_GLASS = 0x64748b;
    const COLOR_ROOF_PARAPET = 0xcbd5e1;

    const coords = feature.geometry?.coordinates[0];
    const name = feature.properties?.name || 'Hostel Block';

    const shape = new THREE.Shape();
    let sumX = 0;
    let sumZ = 0;

    for (let i = 0; i < coords.length; i++) {
      const [lng, lat] = coords[i];
      const local = latLngToLocal(lat, lng);
      sumX += local.x;
      sumZ += local.z;
      if (i === 0) shape.moveTo(local.x, local.z);
      else shape.lineTo(local.x, local.z);
    }

    const centerX = sumX / coords.length;
    const centerZ = sumZ / coords.length;
    const floors = 9;
    const totalHeight = 29.5;
    const floorH = totalHeight / floors;

    // 1. Solid Main Extrusion
    const extrudeSettings: THREE.ExtrudeGeometryOptions = {
      depth: totalHeight,
      bevelEnabled: true,
      bevelSegments: 2,
      steps: floors,
      bevelSize: 0.45,
      bevelThickness: 0.45,
    };

    const extrudeGeo = new THREE.ExtrudeGeometry(shape, extrudeSettings);
    extrudeGeo.rotateX(Math.PI / 2);

    const mat = new THREE.MeshStandardMaterial({
      color: COLOR_WALL_SLATE,
      roughness: 0.45,
      metalness: 0.15,
    });

    const mesh = new THREE.Mesh(extrudeGeo, mat);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    mesh.userData = { name, feature, floors };
    group.add(mesh);

    // 2. Regular Grid of Dual-Sash Dormitory Windows with Charcoal Spandrels per Floor
    for (let f = 1; f <= floors; f++) {
      const slabY = f * floorH;

      for (let i = 0; i < coords.length - 1; i++) {
        const [lng1, lat1] = coords[i];
        const [lng2, lat2] = coords[i + 1];
        const p1 = latLngToLocal(lat1, lng1);
        const p2 = latLngToLocal(lat2, lng2);

        const edgeVec = new THREE.Vector3(p2.x - p1.x, 0, p2.z - p1.z);
        const len = edgeVec.length();
        if (len < 6) continue;

        const winGeo = new THREE.PlaneGeometry(len * 0.72, 1.4);
        const winMat = new THREE.MeshBasicMaterial({
          color: isNight ? 0xfbbf24 : COLOR_WINDOW_GLASS,
          transparent: true,
          opacity: isNight ? 0.95 : 0.65,
        });
        const winMesh = new THREE.Mesh(winGeo, winMat);

        const midX = (p1.x + p2.x) / 2;
        const midZ = (p1.z + p2.z) / 2;
        winMesh.position.set(midX, slabY - 1.6, midZ);

        const angle = Math.atan2(p2.z - p1.z, p2.x - p1.x);
        winMesh.rotation.y = -angle;

        group.add(winMesh);
        windows.push({ mesh: winMesh, nightColor: 0xfbbf24, dayColor: COLOR_WINDOW_GLASS });
      }
    }

    // 3. Rooftop Parapet Line
    const roofPoints: THREE.Vector3[] = [];
    for (let i = 0; i < coords.length; i++) {
      const [lng, lat] = coords[i];
      const local = latLngToLocal(lat, lng);
      roofPoints.push(new THREE.Vector3(local.x, totalHeight + 1.1, local.z));
    }
    roofPoints.push(roofPoints[0]);
    const rGeo = new THREE.BufferGeometry().setFromPoints(roofPoints);
    const rLine = new THREE.Line(rGeo, new THREE.LineBasicMaterial({ color: COLOR_ROOF_PARAPET, linewidth: 2 }));
    group.add(rLine);

    group.userData = { name, feature, floors };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, totalHeight + 6, centerZ),
      name: `${name} (9F)`,
      category: 'Hostels',
      color: '#ec4899',
      floors: 9,
    };
  }
}
