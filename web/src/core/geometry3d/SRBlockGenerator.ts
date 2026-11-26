import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for SR Block (Sarvepalli Radhakrishnan Block / srmap_bldg_16).
 * Perkins+Will Continuous Horizontal Ribbon Architecture:
 * - 7 Floors (Height: 23.1m + 3.4m mechanical penthouse = 26.5m)
 * - Built DIRECTLY on its exact 29-vertex GeoJSON polygon footprint (preserving all setbacks & courtyards)
 * - 1.35m Cantilevered concrete floor slab eyebrows (Color: #E2E8F0)
 * - Dark charcoal horizontal spandrel sub-bands (#1E293B)
 * - Wrap-around continuous solar-control ribbon glazing (#3B5971)
 * - Ground floor colonnade with structural cylindrical columns (#475569)
 * - Rooftop mechanical penthouse and waterproof gravel deck (#94A3B8)
 */
export class SRBlockGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_SLAB_EYEBROW = 0xe2e8f0;
    const COLOR_DARK_SPANDREL = 0x1e293b;
    const COLOR_RIBBON_GLAZING = 0x3b5971;
    const COLOR_NIGHT_WINDOW = 0xfbbf24;
    const COLOR_WALL_MASS = 0xd3cbbe;
    const COLOR_COLONNADE_PILLARS = 0x475569;
    const COLOR_PENTHOUSE_WALL = 0x64748b;
    const COLOR_ROOF_GRAVEL = 0x94a3b8;

    const coords: [number, number][] = feature.geometry?.coordinates?.[0] || [];
    if (coords.length < 3) {
      return {
        group,
        windows,
        floors: 7,
        name: 'SR Block',
        category: 'Academic',
        color: '#38bdf8',
        labelPos: new THREE.Vector3(0, 25, 0),
      };
    }

    const localPts: { x: number; z: number }[] = [];
    let minX = Infinity, maxX = -Infinity, minZ = Infinity, maxZ = -Infinity;
    let sumX = 0, sumZ = 0;

    for (let i = 0; i < coords.length; i++) {
      const [lng, lat] = coords[i];
      const p = latLngToLocal(lat, lng);
      localPts.push(p);
      minX = Math.min(minX, p.x);
      maxX = Math.max(maxX, p.x);
      minZ = Math.min(minZ, p.z);
      maxZ = Math.max(maxZ, p.z);
      sumX += p.x;
      sumZ += p.z;
    }

    const centerX = sumX / localPts.length;
    const centerZ = sumZ / localPts.length;
    const LEVELS = 7;
    const FLOOR_H = 3.3;
    const ROOF_H = LEVELS * FLOOR_H; // 23.1m

    // Materials - DoubleSide to guarantee solid walls without culling
    const slabMat = new THREE.MeshStandardMaterial({
      color: COLOR_SLAB_EYEBROW,
      roughness: 0.45,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const spandrelMat = new THREE.MeshStandardMaterial({
      color: COLOR_DARK_SPANDREL,
      roughness: 0.35,
      metalness: 0.25,
      side: THREE.DoubleSide,
    });

    const glassMat = new THREE.MeshStandardMaterial({
      color: COLOR_RIBBON_GLAZING,
      roughness: 0.15,
      metalness: 0.85,
      emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
      emissiveIntensity: isNight ? 0.85 : 0.0,
      side: THREE.DoubleSide,
    });

    const wallMat = new THREE.MeshStandardMaterial({
      color: COLOR_WALL_MASS,
      roughness: 0.6,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const roofMat = new THREE.MeshStandardMaterial({
      color: COLOR_ROOF_GRAVEL,
      roughness: 0.8,
      metalness: 0.05,
      side: THREE.DoubleSide,
    });

    // 1. Build Exact 2D Polygon Floor Slabs (Perkins+Will Ribbon Slabs)
    const shape = new THREE.Shape();
    localPts.forEach((p, i) => {
      if (i === 0) shape.moveTo(p.x, p.z);
      else shape.lineTo(p.x, p.z);
    });

    // Base Plinth
    const baseGeo = new THREE.ShapeGeometry(shape);
    baseGeo.rotateX(Math.PI / 2);
    const baseMesh = new THREE.Mesh(baseGeo, slabMat);
    baseMesh.position.y = 0.05;
    baseMesh.receiveShadow = true;
    group.add(baseMesh);

    // Floor Eyebrow Slabs at every level
    for (let f = 1; f <= LEVELS; f++) {
      const slabGeo = new THREE.ShapeGeometry(shape);
      slabGeo.rotateX(Math.PI / 2);
      const slabMesh = new THREE.Mesh(slabGeo, slabMat);
      slabMesh.position.y = f * FLOOR_H;
      slabMesh.receiveShadow = true;
      group.add(slabMesh);

      // Cantilever edge band along polygon perimeter
      for (let i = 0; i < localPts.length - 1; i++) {
        const p1 = localPts[i];
        const p2 = localPts[i + 1];
        const dx = p2.x - p1.x;
        const dz = p2.z - p1.z;
        const len = Math.hypot(dx, dz);
        if (len < 0.1) continue;

        const mx = (p1.x + p2.x) / 2;
        const mz = (p1.z + p2.z) / 2;
        const angle = Math.atan2(dz, dx);

        const edgeBandGeo = new THREE.BoxGeometry(len + 0.1, 0.35, 0.8);
        const edgeBand = new THREE.Mesh(edgeBandGeo, slabMat);
        edgeBand.position.set(mx, f * FLOOR_H, mz);
        edgeBand.rotation.y = -angle;
        group.add(edgeBand);
      }
    }

    // 2. Rooftop Parapet and Roof Deck
    const roofGeo = new THREE.ShapeGeometry(shape);
    roofGeo.rotateX(Math.PI / 2);
    const roofMesh = new THREE.Mesh(roofGeo, roofMat);
    roofMesh.position.y = ROOF_H + 0.05;
    roofMesh.receiveShadow = true;
    group.add(roofMesh);

    // Parapet along true 29-vertex perimeter
    for (let i = 0; i < localPts.length - 1; i++) {
      const p1 = localPts[i];
      const p2 = localPts[i + 1];
      const dx = p2.x - p1.x;
      const dz = p2.z - p1.z;
      const len = Math.hypot(dx, dz);
      if (len < 0.1) continue;

      const angle = Math.atan2(dz, dx);
      const paraGeo = new THREE.BoxGeometry(len, 0.9, 0.35);
      const paraMesh = new THREE.Mesh(paraGeo, slabMat);
      paraMesh.position.set((p1.x + p2.x) / 2, ROOF_H + 0.45, (p1.z + p2.z) / 2);
      paraMesh.rotation.y = -angle;
      group.add(paraMesh);
    }

    // 3. Central Rooftop Mechanical Penthouse
    const pW = Math.min(28.0, (maxX - minX) * 0.45);
    const pL = Math.min(42.0, (maxZ - minZ) * 0.45);
    if (pW > 4 && pL > 4) {
      const penthouse = createBoxMesh(
        centerX - pW / 2, centerX + pW / 2,
        ROOF_H, ROOF_H + 3.4,
        centerZ - pL / 2, centerZ + pL / 2,
        COLOR_PENTHOUSE_WALL
      );
      group.add(penthouse);
    }

    // 4. Wrap-Around Continuous Ribbon Glass & Colonnade along True Polygon Segments
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

      // Ground Floor Colonnade Pillars
      if (edgeLen > 5.0) {
        const pillarSpacing = 7.0;
        const numPillars = Math.floor(edgeLen / pillarSpacing);
        for (let c = 0; c <= numPillars; c++) {
          const t = c / numPillars;
          const px = p1.x + dx * t;
          const pz = p1.z + dz * t;
          const pillarGeo = new THREE.CylinderGeometry(0.35, 0.35, FLOOR_H, 12);
          const pillarMat = new THREE.MeshStandardMaterial({ color: COLOR_COLONNADE_PILLARS, roughness: 0.5 });
          const pillarMesh = new THREE.Mesh(pillarGeo, pillarMat);
          pillarMesh.position.set(px, FLOOR_H / 2, pz);
          group.add(pillarMesh);
        }
      }

      // Ground Floor Recessed Glass Corridor
      const groundGlassGeo = new THREE.BoxGeometry(edgeLen, FLOOR_H * 0.88, 0.2);
      const groundGlass = new THREE.Mesh(groundGlassGeo, glassMat);
      groundGlass.position.set(mx - nx * 0.5, FLOOR_H / 2, mz - nz * 0.5);
      groundGlass.rotation.y = -angle;
      group.add(groundGlass);
      windows.push({ mesh: groundGlass, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_RIBBON_GLAZING });

      // Upper Floors (1 through 6):
      // Each floor has:
      // a) Solid wall/spandrel base (0.8m)
      // b) Continuous wrap-around ribbon glass (1.7m)
      // c) Dark spandrel band (0.8m)
      for (let f = 1; f < LEVELS; f++) {
        const baseY = f * FLOOR_H;

        // a) Lower solid precast spandrel
        const lowH = 0.8;
        const lowGeo = new THREE.BoxGeometry(edgeLen, lowH, 0.35);
        const lowMesh = new THREE.Mesh(lowGeo, wallMat);
        lowMesh.position.set(mx, baseY + lowH / 2, mz);
        lowMesh.rotation.y = -angle;
        group.add(lowMesh);

        // b) Continuous Solar Ribbon Glazing
        const winH = 1.7;
        const winGeo = new THREE.BoxGeometry(edgeLen * 0.98, winH, 0.2);
        const winMesh = new THREE.Mesh(winGeo, glassMat);
        winMesh.position.set(mx - nx * 0.05, baseY + lowH + winH / 2, mz - nz * 0.05);
        winMesh.rotation.y = -angle;
        group.add(winMesh);
        windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_RIBBON_GLAZING });

        // c) Dark horizontal spandrel upper sub-band
        const topH = FLOOR_H - lowH - winH; // ~0.8m
        const topGeo = new THREE.BoxGeometry(edgeLen, topH, 0.35);
        const topMesh = new THREE.Mesh(topGeo, spandrelMat);
        topMesh.position.set(mx, baseY + lowH + winH + topH / 2, mz);
        topMesh.rotation.y = -angle;
        group.add(topMesh);
      }
    }

    group.userData = { name: 'SR Block', feature, floors: LEVELS };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, ROOF_H + 5, centerZ),
      name: 'SR Block',
      category: 'Academic',
      color: '#38bdf8',
      floors: LEVELS,
    };
  }
}
