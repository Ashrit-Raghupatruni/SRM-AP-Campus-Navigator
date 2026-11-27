import * as THREE from 'three';
import { ArchitecturalMeshResult } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Vedavathi Boys' Hostel (srmap_bldg_14).
 * Built DIRECTLY from the exact verified 2D GeoJSON polygon footprint to match 2D map:
 * - Real 15-story residential mid-rise
 * - Height: 46.5m (roof parapet: 47.4m, headhouse: 49.5m)
 * - Exact polygon boundary, courtyard recess, and orientation matching 2D map
 * - Authentic Indian Terracotta Ochre (#966A55) walls with crisp white floor slab eyebrows (#F8FAFC)
 * - Ground floor double-height pilotis and colonnade
 * - 360-degree punched dormitory bedroom window matrices across all exterior faces with night window glow
 * - Rooftop elevator headhouse and gravel terrace deck
 */
export class VedavathiHostelGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_TERRACOTTA_OCHRE = 0x966a55; // Warm terracotta ochre panels
    const COLOR_ACCENT_TERRACOTTA = 0x7c5543; // Window surround fins
    const COLOR_WHITE_SLAB = 0xf8fafc;        // Crisp white cantilevered floor slab eyebrows
    const COLOR_PILOTIS = 0xe2e8f0;           // Crisp white ground pilotis columns
    const COLOR_WINDOW_FRAME = 0x334155;      // Dark window frames
    const COLOR_WINDOW_GLASS = 0x2b394a;      // Deep solar window glass
    const COLOR_NIGHT_WINDOW = 0xfbbf24;      // Incandescent golden night glow
    const COLOR_ROOF_GRAVEL = 0xcbd5e1;       // Waterproof terrace deck
    const COLOR_PARAPET = 0xf1f5f9;           // White parapet coping

    const coords: [number, number][] = feature.geometry?.coordinates?.[0] || [];
    const name = feature.properties?.name || "Vedavathi Boys' Hostel";

    if (coords.length < 3) {
      return {
        group,
        windows,
        labelPos: new THREE.Vector3(0, 50, 0),
        name: "Vedavathi Boys' Hostel",
        category: 'Hostel',
        color: '#f43f5e',
        floors: 15,
      };
    }

    const localPts: { x: number; z: number }[] = [];
    let minX = Infinity, maxX = -Infinity, minZ = Infinity, maxZ = -Infinity;
    let sumX = 0;
    let sumZ = 0;

    for (let i = 0; i < coords.length; i++) {
      const [lng, lat] = coords[i];
      const local = latLngToLocal(lat, lng);
      localPts.push(local);
      minX = Math.min(minX, local.x);
      maxX = Math.max(maxX, local.x);
      minZ = Math.min(minZ, local.z);
      maxZ = Math.max(maxZ, local.z);
      sumX += local.x;
      sumZ += local.z;
    }

    const centerX = sumX / localPts.length;
    const centerZ = sumZ / localPts.length;
    const floors = 15;
    const totalHeight = 46.5;
    const floorH = totalHeight / floors; // 3.1m per floor

    // Materials matching Ganga Hostel exactly
    const COLOR_TERRACOTTA_DARK = 0x7c5543; // Dark terracotta vertical architectural fins

    const wallMat = new THREE.MeshStandardMaterial({
      color: COLOR_TERRACOTTA_OCHRE,
      roughness: 0.4,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const slabMat = new THREE.MeshStandardMaterial({
      color: COLOR_WHITE_SLAB,
      roughness: 0.3,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const pilotisMat = new THREE.MeshStandardMaterial({
      color: COLOR_PILOTIS,
      roughness: 0.4,
      metalness: 0.1,
    });

    const finMat = new THREE.MeshStandardMaterial({
      color: COLOR_TERRACOTTA_DARK,
      roughness: 0.35,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const winMat = new THREE.MeshStandardMaterial({
      color: isNight ? COLOR_NIGHT_WINDOW : COLOR_WINDOW_GLASS,
      roughness: 0.2,
      metalness: 0.8,
      emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
      emissiveIntensity: isNight ? 0.95 : 0.0,
      side: THREE.DoubleSide,
    });

    const roofMat = new THREE.MeshStandardMaterial({
      color: COLOR_ROOF_GRAVEL,
      roughness: 0.7,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const parapetMat = new THREE.MeshStandardMaterial({
      color: COLOR_PARAPET,
      roughness: 0.4,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    // 1. Build EXACT Horizontal Slabs matching 2D Polygon Boundary
    const shape = new THREE.Shape();
    localPts.forEach((p, i) => {
      if (i === 0) shape.moveTo(p.x, p.z);
      else shape.lineTo(p.x, p.z);
    });

    // Base slab
    const baseGeo = new THREE.ShapeGeometry(shape);
    baseGeo.rotateX(Math.PI / 2);
    const baseMesh = new THREE.Mesh(baseGeo, slabMat);
    baseMesh.position.y = 0.05;
    baseMesh.receiveShadow = true;
    group.add(baseMesh);

    // Cantilever Floor Eyebrow Slabs every floor (levels 1 through 15)
    for (let f = 1; f <= floors; f++) {
      const slabGeo = new THREE.ShapeGeometry(shape);
      slabGeo.rotateX(Math.PI / 2);
      const slabMesh = new THREE.Mesh(slabGeo, slabMat);
      slabMesh.position.y = f * floorH;
      slabMesh.receiveShadow = true;
      group.add(slabMesh);
    }

    // Rooftop Terrace Deck & Parapet Wall
    const roofGeo = new THREE.ShapeGeometry(shape);
    roofGeo.rotateX(Math.PI / 2);
    const roofMesh = new THREE.Mesh(roofGeo, roofMat);
    roofMesh.position.y = totalHeight + 0.05;
    roofMesh.receiveShadow = true;
    group.add(roofMesh);

    // Parapet along all polygon edges
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

    // 2. Build Facade Walls & Instanced Window Arrays along every polygon edge
    const winW = 2.4;
    const winH = 1.8;
    const windowTransforms: THREE.Matrix4[] = [];
    const finTransforms: THREE.Matrix4[] = [];
    const colTransforms: THREE.Matrix4[] = [];
    const dummy = new THREE.Object3D();

    for (let i = 0; i < localPts.length - 1; i++) {
      const p1 = localPts[i];
      const p2 = localPts[i + 1];
      const dx = p2.x - p1.x;
      const dz = p2.z - p1.z;
      const edgeLen = Math.hypot(dx, dz);
      if (edgeLen < 0.3) continue;

      const mx = (p1.x + p2.x) / 2;
      const mz = (p1.z + p2.z) / 2;
      const angle = Math.atan2(dz, dx);

      // Determine correct outward normal vector pointing away from building center
      let nx = -dz / edgeLen;
      let nz = dx / edgeLen;
      const toMidX = mx - centerX;
      const toMidZ = mz - centerZ;
      if (nx * toMidX + nz * toMidZ < 0) {
        nx = -nx;
        nz = -nz;
      }

      // Solid baseline terracotta wall spanning ground to roof
      const wallGeo = new THREE.BoxGeometry(edgeLen, totalHeight, 0.45);
      const wallMesh = new THREE.Mesh(wallGeo, wallMat);
      wallMesh.position.set(mx, totalHeight / 2, mz);
      wallMesh.rotation.y = -angle;
      group.add(wallMesh);

      // Ground floor pilotis columns along longer exterior edges
      if (edgeLen > 8.0) {
        const numCols = Math.floor(edgeLen / 5.5);
        for (let c = 1; c <= numCols; c++) {
          const ct = c / (numCols + 1);
          const cx = p1.x + dx * ct + nx * 0.26;
          const cz = p1.z + dz * ct + nz * 0.26;
          dummy.position.set(cx, (floorH * 1.5) / 2, cz);
          dummy.rotation.set(0, 0, 0);
          dummy.updateMatrix();
          colTransforms.push(dummy.matrix.clone());
        }
      }

      // Windows and Terracotta Fins matching Ganga architecture exactly
      if (edgeLen > 5.0) {
        const windowSpacing = 5.5;
        const numWindows = Math.max(1, Math.floor((edgeLen - 2.0) / windowSpacing));

        for (let w = 1; w <= numWindows; w++) {
          const t = w / (numWindows + 1);
          const wx = p1.x + dx * t;
          const wz = p1.z + dz * t;

          dummy.position.set(wx + nx * 0.3, totalHeight / 2, wz + nz * 0.3);
          dummy.rotation.set(0, -angle, 0);
          dummy.updateMatrix();
          finTransforms.push(dummy.matrix.clone());

          for (let f = 1; f < floors; f++) {
            const winY = f * floorH + floorH / 2;
            dummy.position.set(wx + nx * 0.18, winY, wz + nz * 0.18);
            dummy.rotation.set(0, -angle, 0);
            dummy.updateMatrix();
            windowTransforms.push(dummy.matrix.clone());
          }
        }
      }
    }

    if (colTransforms.length > 0) {
      const colGeo = new THREE.CylinderGeometry(0.35, 0.35, floorH * 1.5, 8);
      const colInstanced = new THREE.InstancedMesh(colGeo, pilotisMat, colTransforms.length);
      colTransforms.forEach((m, idx) => colInstanced.setMatrixAt(idx, m));
      colInstanced.instanceMatrix.needsUpdate = true;
      group.add(colInstanced);
    }

    if (finTransforms.length > 0) {
      const finGeo = new THREE.BoxGeometry(0.3, totalHeight, 0.6);
      const finInstanced = new THREE.InstancedMesh(finGeo, finMat, finTransforms.length);
      finTransforms.forEach((m, idx) => finInstanced.setMatrixAt(idx, m));
      finInstanced.instanceMatrix.needsUpdate = true;
      group.add(finInstanced);
    }

    if (windowTransforms.length > 0) {
      const winGeo = new THREE.BoxGeometry(winW, winH, 0.3);
      const winInstanced = new THREE.InstancedMesh(winGeo, winMat, windowTransforms.length);
      windowTransforms.forEach((m, idx) => winInstanced.setMatrixAt(idx, m));
      winInstanced.instanceMatrix.needsUpdate = true;
      group.add(winInstanced);
      windows.push({ mesh: winInstanced, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS });
    }

    // 3. Rooftop Elevator Headhouse & Service Tower
    const headW = Math.min((maxX - minX) * 0.25, 12.0);
    const headD = Math.min((maxZ - minZ) * 0.25, 10.0);
    const headhouseGeo = new THREE.BoxGeometry(headW, 3.5, headD);
    const headhouse = new THREE.Mesh(headhouseGeo, wallMat);
    headhouse.position.set(centerX, totalHeight + 1.75, centerZ);
    group.add(headhouse);

    group.userData = { name: "Vedavathi Boys' Hostel", feature, floors: 15 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, totalHeight + 6, centerZ),
      name: "Vedavathi Boys' Hostel",
      category: 'Hostel',
      color: '#f43f5e',
      floors: 15,
    };
  }
}
