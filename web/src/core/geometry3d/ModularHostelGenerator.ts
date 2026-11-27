import * as THREE from 'three';
import { ArchitecturalMeshResult } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Modular Girls' Hostels
 * (Yamuna, Narmada, Godavari, Krishna, Kaveri).
 *
 * Real-world Architectural Features (Derived directly from Photo 4 aerial shot):
 * - 9-story tall monolithic crisp white residential towers (#FFFFFF)
 * - FLUSH exterior facades (NO horizontal cantilever slab eyebrows sticking out!)
 * - Regular modular punched rectangular dormitory bedroom window matrix (9 rows x dense regular columns)
 * - Vertical stairwell / elevator lift cores with vertical service slot louvers
 * - Rooftop elevator headhouse towers flanked by elevated photovoltaic solar panel arrays (#1E293B)
 */
export class ModularHostelGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_WALL_WHITE = 0xffffff;     // Pure crisp architectural white
    const COLOR_WINDOW_FRAME = 0x334155;   // Thin dark architectural window frames
    const COLOR_WINDOW_GLASS = 0x2b394a;   // Deep residential solar window
    const COLOR_NIGHT_WINDOW = 0xfbbf24;   // Warm incandescent night glow
    const COLOR_ROOF_PARAPET = 0xe2e8f0;   // Perimeter coping
    const COLOR_ROOF_DECK = 0x94a3b8;      // Gravel roof deck
    const COLOR_SOLAR_PANEL = 0x1e293b;    // Photovoltaic solar array panels
    const COLOR_SOLAR_FRAME = 0x64748b;    // Galvanized structural steel racking

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
    const floors = 9;
    const totalHeight = 29.5;
    const floorH = totalHeight / floors; // ~3.27m

    const wallMat = new THREE.MeshStandardMaterial({
      color: COLOR_WALL_WHITE,
      roughness: 0.55,
      metalness: 0.05,
      side: THREE.DoubleSide,
    });

    const frameMat = new THREE.MeshStandardMaterial({
      color: COLOR_WINDOW_FRAME,
      roughness: 0.4,
      metalness: 0.2,
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

    // 1. Build Internal Floor Slabs (Flush with exterior, zero cantilever slab eyebrows)
    const shape = new THREE.Shape();
    localPts.forEach((p, i) => {
      if (i === 0) shape.moveTo(p.x, p.z);
      else shape.lineTo(p.x, p.z);
    });

    // Base Slab
    const baseGeo = new THREE.ShapeGeometry(shape);
    baseGeo.rotateX(Math.PI / 2);
    const baseMesh = new THREE.Mesh(baseGeo, wallMat);
    baseMesh.position.y = 0.05;
    baseMesh.receiveShadow = true;
    group.add(baseMesh);

    // Internal floor slabs
    for (let f = 1; f <= floors; f++) {
      const slabGeo = new THREE.ShapeGeometry(shape);
      slabGeo.rotateX(Math.PI / 2);
      const slabMesh = new THREE.Mesh(slabGeo, wallMat);
      slabMesh.position.y = f * floorH;
      slabMesh.receiveShadow = true;
      group.add(slabMesh);
    }

    // 2. Rooftop Terrace Deck & Parapet Coping
    const roofGeo = new THREE.ShapeGeometry(shape);
    roofGeo.rotateX(Math.PI / 2);
    const roofMesh = new THREE.Mesh(roofGeo, roofMat);
    roofMesh.position.y = totalHeight + 0.05;
    roofMesh.receiveShadow = true;
    group.add(roofMesh);

    // Parapet Wall (0.9m flush coping around the entire perimeter)
    for (let i = 0; i < localPts.length - 1; i++) {
      const p1 = localPts[i];
      const p2 = localPts[i + 1];
      const dx = p2.x - p1.x;
      const dz = p2.z - p1.z;
      const len = Math.hypot(dx, dz);
      if (len < 0.2) continue;

      const angle = Math.atan2(dz, dx);
      const paraGeo = new THREE.BoxGeometry(len, 0.9, 0.35);
      const paraMesh = new THREE.Mesh(paraGeo, parapetMat);
      paraMesh.position.set((p1.x + p2.x) / 2, totalHeight + 0.45, (p1.z + p2.z) / 2);
      paraMesh.rotation.y = -angle;
      group.add(paraMesh);
    }

    // Rooftop Elevator Headhouse
    const headW = Math.min((maxX - minX) * 0.3, 12.0);
    const headD = Math.min((maxZ - minZ) * 0.35, 10.0);
    const headhouseGeo = new THREE.BoxGeometry(headW, 3.2, headD);
    const headhouse = new THREE.Mesh(headhouseGeo, wallMat);
    headhouse.position.set(centerX, totalHeight + 1.6, centerZ);
    group.add(headhouse);

    // Rooftop Photovoltaic Solar Arrays (Authentic feature clearly visible in Photo 4 aerial shot!)
    const solarMat = new THREE.MeshStandardMaterial({
      color: COLOR_SOLAR_PANEL,
      roughness: 0.2,
      metalness: 0.7,
      side: THREE.DoubleSide,
    });
    const solarRackMat = new THREE.MeshStandardMaterial({
      color: COLOR_SOLAR_FRAME,
      roughness: 0.4,
      metalness: 0.6,
    });

    const bldgW = maxX - minX;
    const bldgD = maxZ - minZ;
    if (bldgW > 15 && bldgD > 12) {
      // 2 banks of angled solar racks
      const rackW = bldgW * 0.7;
      const rackD = 4.2;
      const rackZPositions = [centerZ - bldgD * 0.25, centerZ + bldgD * 0.25];

      for (const rz of rackZPositions) {
        // Solar panels
        const panelGeo = new THREE.PlaneGeometry(rackW, rackD);
        panelGeo.rotateX(-Math.PI / 2 + 0.25); // 15-degree tilt facing south
        const panelMesh = new THREE.Mesh(panelGeo, solarMat);
        panelMesh.position.set(centerX, totalHeight + 1.8, rz);
        group.add(panelMesh);

        // Rack support stilts
        for (let rx = centerX - rackW / 2 + 3; rx <= centerX + rackW / 2 - 3; rx += 6) {
          const stilt = new THREE.Mesh(
            new THREE.CylinderGeometry(0.08, 0.08, 1.6, 8),
            solarRackMat
          );
          stilt.position.set(rx, totalHeight + 0.8, rz);
          group.add(stilt);
        }
      }
    }

    // 3. Facade Articulation: Flush White Walls with Modular Punched Dormitory Window Matrix (Photo 4)
    for (let i = 0; i < localPts.length - 1; i++) {
      const p1 = localPts[i];
      const p2 = localPts[i + 1];
      const dx = p2.x - p1.x;
      const dz = p2.z - p1.z;
      const edgeLen = Math.hypot(dx, dz);
      if (edgeLen < 0.5) continue;

      const mx = (p1.x + p2.x) / 2;
      const mz = (p1.z + p2.z) / 2;
      const angle = Math.atan2(dz, dx);

      // Determine correct outward normal vector pointing away from building center
      let nx = -dz / edgeLen;
      let nz = dx / edgeLen;
      // Dot product test with vector from center to edge midpoint:
      // if normal points inward towards center, flip it outward
      const toMidX = mx - centerX;
      const toMidZ = mz - centerZ;
      if (nx * toMidX + nz * toMidZ < 0) {
        nx = -nx;
        nz = -nz;
      }

      // Solid baseline wall mesh spanning the entire edge from ground to roof
      const fullWallGeo = new THREE.BoxGeometry(edgeLen, totalHeight, 0.4);
      const fullWallMesh = new THREE.Mesh(fullWallGeo, wallMat);
      fullWallMesh.position.set(mx, totalHeight / 2, mz);
      fullWallMesh.rotation.y = -angle;
      group.add(fullWallMesh);

      // Check if edge is long enough for a punched window grid (residential dormitory facades)
      if (edgeLen > 5.0) {
        const windowSpacing = 3.2; // Modular dormitory bedroom bay spacing
        const numWindows = Math.max(1, Math.floor((edgeLen - 2.0) / windowSpacing));

        const winW = 1.9; // Punched dual-sash window width
        const winH = 1.6; // Punched window height

        for (let w = 1; w <= numWindows; w++) {
          const t = w / (numWindows + 1);
          // Position cleanly on the outer surface (wall thickness is 0.4, half is 0.2, offset by 0.22)
          const wx = p1.x + dx * t + nx * 0.22;
          const wz = p1.z + dz * t + nz * 0.22;

          for (let f = 0; f < floors; f++) {
            const winY = f * floorH + floorH / 2;

            // Thin dark window reveal surround frame
            const frameGeo = new THREE.BoxGeometry(winW + 0.16, winH + 0.16, 0.12);
            const frameMesh = new THREE.Mesh(frameGeo, frameMat);
            frameMesh.position.set(wx, winY, wz);
            frameMesh.rotation.y = -angle;
            group.add(frameMesh);

            // Reflective punched dormitory window glass protruding outward so it is prominently visible
            const winGeo = new THREE.BoxGeometry(winW, winH, 0.14);
            const winMesh = new THREE.Mesh(winGeo, glassMat);
            winMesh.position.set(wx + nx * 0.04, winY, wz + nz * 0.04);
            winMesh.rotation.y = -angle;
            group.add(winMesh);
            windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_WINDOW_GLASS });
          }
        }
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
