import * as THREE from 'three';
import { ArchitecturalMeshResult } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Modular Hostels (Yamuna, Narmada, Godavari, Krishna, Kaveri).
 * - 9 Floors (Total height: 29.5m)
 * - Solid crisp pure architectural white facade walls (#FFFFFF / #F8FAFC)
 * - Continuous horizontal white floor slab plates separating each floor tier
 * - Recessed residential dual-sash dormitory windows (#334155 / solar glass) with warm amber glow (#FBBF24) at night
 * - 3D Solid parapet coping around the roof terrace
 */
export class ModularHostelGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_WALL_WHITE = 0xffffff;     // Pure crisp architectural white
    const COLOR_SLAB_WHITE = 0xf1f5f9;     // Smooth off-white slab ribbons
    const COLOR_WINDOW_GLASS = 0x2b394a;   // Deep residential solar window
    const COLOR_NIGHT_WINDOW = 0xfbbf24;   // Warm incandescent night glow
    const COLOR_ROOF_PARAPET = 0xe2e8f0;   // Perimeter coping
    const COLOR_ROOF_DECK = 0x94a3b8;      // Gravel roof deck

    const coords: [number, number][] = feature.geometry?.coordinates?.[0] || [];
    const name = feature.properties?.name || 'Hostel Block';

    if (coords.length < 3) {
      return {
        group,
        windows,
        labelPos: new THREE.Vector3(0, 35, 0),
        name: `${name} (9F)`,
        category: 'Hostels',
        color: '#ec4899',
        floors: 9,
      };
    }

    const localPts: { x: number; z: number }[] = [];
    let sumX = 0;
    let sumZ = 0;

    for (let i = 0; i < coords.length; i++) {
      const [lng, lat] = coords[i];
      const local = latLngToLocal(lat, lng);
      localPts.push(local);
      sumX += local.x;
      sumZ += local.z;
    }

    const centerX = sumX / localPts.length;
    const centerZ = sumZ / localPts.length;
    const floors = 9;
    const totalHeight = 29.5;
    const floorH = totalHeight / floors; // ~3.27m

    const wallMat = new THREE.MeshStandardMaterial({
      color: COLOR_WALL_WHITE,
      roughness: 0.55,
      metalness: 0.05,
      side: THREE.DoubleSide,
    });

    const slabMat = new THREE.MeshStandardMaterial({
      color: COLOR_SLAB_WHITE,
      roughness: 0.4,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const glassMat = new THREE.MeshStandardMaterial({
      color: COLOR_WINDOW_GLASS,
      roughness: 0.15,
      metalness: 0.85,
      emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
      emissiveIntensity: isNight ? 0.95 : 0.0,
      side: THREE.DoubleSide,
    });

    const roofMat = new THREE.MeshStandardMaterial({
      color: COLOR_ROOF_DECK,
      roughness: 0.8,
      metalness: 0.05,
      side: THREE.DoubleSide,
    });

    const parapetMat = new THREE.MeshStandardMaterial({
      color: COLOR_ROOF_PARAPET,
      roughness: 0.5,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    // 1. Build Base Plinth & Horizontal Floor Slabs for all 9 Tiers
    const shape = new THREE.Shape();
    localPts.forEach((p, i) => {
      if (i === 0) shape.moveTo(p.x, p.z);
      else shape.lineTo(p.x, p.z);
    });

    // Base Slab
    const baseGeo = new THREE.ShapeGeometry(shape);
    baseGeo.rotateX(Math.PI / 2);
    const baseMesh = new THREE.Mesh(baseGeo, slabMat);
    baseMesh.position.y = 0.05;
    baseMesh.receiveShadow = true;
    group.add(baseMesh);

    // Floor Eyebrow Slabs
    for (let f = 1; f <= floors; f++) {
      const slabGeo = new THREE.ShapeGeometry(shape);
      slabGeo.rotateX(Math.PI / 2);
      const slabMesh = new THREE.Mesh(slabGeo, slabMat);
      slabMesh.position.y = f * floorH;
      slabMesh.receiveShadow = true;
      group.add(slabMesh);

      // Floor slab perimeter edge band
      for (let i = 0; i < localPts.length - 1; i++) {
        const p1 = localPts[i];
        const p2 = localPts[i + 1];
        const dx = p2.x - p1.x;
        const dz = p2.z - p1.z;
        const len = Math.hypot(dx, dz);
        if (len < 0.2) continue;

        const angle = Math.atan2(dz, dx);
        const bandGeo = new THREE.BoxGeometry(len + 0.1, 0.35, 0.6);
        const bandMesh = new THREE.Mesh(bandGeo, slabMat);
        bandMesh.position.set((p1.x + p2.x) / 2, f * floorH, (p1.z + p2.z) / 2);
        bandMesh.rotation.y = -angle;
        group.add(bandMesh);
      }
    }

    // 2. Rooftop Terrace Deck & Parapet Coping
    const roofGeo = new THREE.ShapeGeometry(shape);
    roofGeo.rotateX(Math.PI / 2);
    const roofMesh = new THREE.Mesh(roofGeo, roofMat);
    roofMesh.position.y = totalHeight + 0.05;
    roofMesh.receiveShadow = true;
    group.add(roofMesh);

    // Solid Parapet (0.9m high)
    for (let i = 0; i < localPts.length - 1; i++) {
      const p1 = localPts[i];
      const p2 = localPts[i + 1];
      const dx = p2.x - p1.x;
      const dz = p2.z - p1.z;
      const len = Math.hypot(dx, dz);
      if (len < 0.2) continue;

      const angle = Math.atan2(dz, dx);
      const paraGeo = new THREE.BoxGeometry(len, 0.9, 0.4);
      const paraMesh = new THREE.Mesh(paraGeo, parapetMat);
      paraMesh.position.set((p1.x + p2.x) / 2, totalHeight + 0.45, (p1.z + p2.z) / 2);
      paraMesh.rotation.y = -angle;
      group.add(paraMesh);
    }

    // 3. Solid Pure White Walls & Recessed Dormitory Windows for All 9 Floors
    for (let i = 0; i < localPts.length - 1; i++) {
      const p1 = localPts[i];
      const p2 = localPts[i + 1];
      const dx = p2.x - p1.x;
      const dz = p2.z - p1.z;
      const edgeLen = Math.hypot(dx, dz);
      if (edgeLen < 0.4) continue;

      const mx = (p1.x + p2.x) / 2;
      const mz = (p1.z + p2.z) / 2;
      const angle = Math.atan2(dz, dx);
      const nx = -dz / edgeLen;
      const nz = dx / edgeLen;

      for (let f = 0; f < floors; f++) {
        const baseY = f * floorH;

        // a) Lower solid white spandrel wall (0.9m)
        const lowH = 0.9;
        const lowGeo = new THREE.BoxGeometry(edgeLen, lowH, 0.4);
        const lowMesh = new THREE.Mesh(lowGeo, wallMat);
        lowMesh.position.set(mx, baseY + lowH / 2, mz);
        lowMesh.rotation.y = -angle;
        group.add(lowMesh);

        // b) Recessed Dormitory Window Band (1.5m tall)
        const winH = 1.5;
        const winGeo = new THREE.BoxGeometry(edgeLen * 0.88, winH, 0.2);
        const winMesh = new THREE.Mesh(winGeo, glassMat);
        winMesh.position.set(mx - nx * 0.05, baseY + lowH + winH / 2, mz - nz * 0.05);
        winMesh.rotation.y = -angle;
        group.add(winMesh);
        windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS });

        // c) Upper solid white lintel wall
        const topH = floorH - lowH - winH; // ~0.87m
        const topGeo = new THREE.BoxGeometry(edgeLen, topH, 0.4);
        const topMesh = new THREE.Mesh(topGeo, wallMat);
        topMesh.position.set(mx, baseY + lowH + winH + topH / 2, mz);
        topMesh.rotation.y = -angle;
        group.add(topMesh);
      }
    }

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
