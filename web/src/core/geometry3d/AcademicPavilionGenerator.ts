import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Academic Pavilions:
 * Vikram Sarabhai Block (V-Block), Homi J Bhabha Block, X-Lab, and JC Bose Block.
 *
 * Real-world Architectural Features:
 * - Solid crisp monolithic off-white exterior wall mass (#F8FAFC) - 100% opaque, no see-through glitch.
 * - Warm sandstone brise-soleil vertical sunbreaker louvers (#D9D2C5).
 * - Continuous horizontal cantilevered floor slab eyebrows (#FFFFFF / #E2E8F0).
 * - Deep solar-reflective blue-grey ribbon glazing bands (#2B5876).
 * - Recessed ground-floor entrance arcade with pilotis columns (#475569).
 * - Rooftop parapet coping (#CBD5E1) enclosing flat gravel roof terrace (#94A3B8) and mechanical headhouses.
 */
export class AcademicPavilionGenerator {
  static generate(
    feature: any,
    isNight: boolean,
    buildingType: 'v_block' | 'homi_bhabha' | 'x_lab' | 'jc_bose' | 'office' = 'v_block'
  ): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    // Authentic Material & Color Palettes
    const COLOR_MONOLITHIC_WALL = 0xf8fafc; // Crisp off-white precast wall cladding
    const COLOR_LOUVERS = 0xd9d2c5;         // Natural sandstone vertical louvers
    const COLOR_SLAB = 0xffffff;            // Solid white cantilevered floor slab plates
    const COLOR_GLAZING = 0x2b5876;         // Deep solar-reflective blue-grey glazing
    const COLOR_NIGHT_WINDOW = 0x38bdf8;    // Sky-blue incandescent night glow
    const COLOR_PILOTIS = 0x475569;         // Dark charcoal ground stilt columns
    const COLOR_ROOF_GRAVEL = 0x94a3b8;     // Rooftop weatherproofing surface
    const COLOR_PARAPET = 0xcbd5e1;         // Perimeter parapet edge

    // Determine building levels and name
    let levels = 7;
    let bldgName = 'Academic Pavilion';
    let labelCategory = 'Academic';
    let labelColor = '#38bdf8';

    if (buildingType === 'x_lab') {
      levels = 7;
      bldgName = 'X-Lab';
    } else if (buildingType === 'jc_bose') {
      levels = 5;
      bldgName = 'JC Bose Block';
    } else if (buildingType === 'v_block') {
      levels = 7;
      bldgName = 'V Block';
    } else if (buildingType === 'homi_bhabha') {
      levels = 7;
      bldgName = 'Homi J Bhabha Block';
    } else if (buildingType === 'office') {
      levels = 2;
      bldgName = 'Office';
      labelCategory = 'Academic';
    }

    // Custom name from feature if available
    if (feature.properties?.name) {
      bldgName = feature.properties.name;
    }

    const coords: [number, number][] = feature.geometry?.coordinates?.[0] || [];
    if (coords.length < 3) {
      return {
        group,
        windows,
        floors: levels,
        name: bldgName,
        category: labelCategory,
        color: labelColor,
        labelPos: new THREE.Vector3(0, 20, 0),
      };
    }

    // Convert polygon coordinates to local 3D points
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
    const floorH = 3.6;
    const totalHeight = levels * floorH;

    // Materials - Using DoubleSide to guarantee zero backface culling or see-through walls
    const wallMat = new THREE.MeshStandardMaterial({
      color: COLOR_MONOLITHIC_WALL,
      roughness: 0.6,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const slabMat = new THREE.MeshStandardMaterial({
      color: COLOR_SLAB,
      roughness: 0.4,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const louverMat = new THREE.MeshStandardMaterial({
      color: COLOR_LOUVERS,
      roughness: 0.65,
      metalness: 0.15,
      side: THREE.DoubleSide,
    });

    const glassMat = new THREE.MeshStandardMaterial({
      color: COLOR_GLAZING,
      roughness: 0.15,
      metalness: 0.85,
      emissive: isNight ? COLOR_NIGHT_WINDOW : 0x000000,
      emissiveIntensity: isNight ? 0.75 : 0.0,
      side: THREE.DoubleSide,
    });

    const roofMat = new THREE.MeshStandardMaterial({
      color: COLOR_ROOF_GRAVEL,
      roughness: 0.8,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const parapetMat = new THREE.MeshStandardMaterial({
      color: COLOR_PARAPET,
      roughness: 0.5,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    // 1. Build Solid Base Polygon Floor Slabs for each tier
    const shape = new THREE.Shape();
    localPts.forEach((p, i) => {
      if (i === 0) shape.moveTo(p.x, p.z);
      else shape.lineTo(p.x, p.z);
    });

    // Solid base plinth (Ground level slab)
    const baseGeo = new THREE.ShapeGeometry(shape);
    baseGeo.rotateX(Math.PI / 2);
    const baseMesh = new THREE.Mesh(baseGeo, slabMat);
    baseMesh.position.y = 0.05;
    baseMesh.receiveShadow = true;
    group.add(baseMesh);

    // Cantilevered floor slabs at each level
    for (let f = 1; f <= levels; f++) {
      const slabGeo = new THREE.ShapeGeometry(shape);
      slabGeo.rotateX(Math.PI / 2);
      const slabMesh = new THREE.Mesh(slabGeo, slabMat);
      slabMesh.position.y = f * floorH;
      slabMesh.receiveShadow = true;
      group.add(slabMesh);

      // Slight edge slab thickness band around perimeter
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

        const bandGeo = new THREE.BoxGeometry(len + 0.1, 0.35, 0.5);
        const bandMesh = new THREE.Mesh(bandGeo, slabMat);
        bandMesh.position.set(mx, f * floorH, mz);
        bandMesh.rotation.y = -angle;
        group.add(bandMesh);
      }
    }

    // 2. Rooftop Parapet and Gravel Deck
    const roofGeo = new THREE.ShapeGeometry(shape);
    roofGeo.rotateX(Math.PI / 2);
    const roofMesh = new THREE.Mesh(roofGeo, roofMat);
    roofMesh.position.y = totalHeight + 0.02;
    roofMesh.receiveShadow = true;
    group.add(roofMesh);

    // Parapet perimeter wall (0.9m solid coping)
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

      const paraGeo = new THREE.BoxGeometry(len, 0.9, 0.35);
      const paraMesh = new THREE.Mesh(paraGeo, parapetMat);
      paraMesh.position.set(mx, totalHeight + 0.45, mz);
      paraMesh.rotation.y = -angle;
      group.add(paraMesh);
    }

    // Rooftop elevator / HVAC headhouse
    const headhouseW = Math.min((maxX - minX) * 0.35, 14.0);
    const headhouseD = Math.min((maxZ - minZ) * 0.35, 12.0);
    if (headhouseW > 3 && headhouseD > 3) {
      const headhouse = createBoxMesh(
        centerX - headhouseW / 2,
        centerX + headhouseW / 2,
        totalHeight,
        totalHeight + 3.2,
        centerZ - headhouseD / 2,
        centerZ + headhouseD / 2,
        COLOR_MONOLITHIC_WALL,
        0.5,
        0.1
      );
      group.add(headhouse);
    }

    // 3. Robust Solid Outer Wall Quads & Solar Ribbon Windows Along All Polygon Edges
    // This constructs 100% solid walls with real architectural depth between floors
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

      // Unit normal perpendicular to wall outward
      const nx = -dz / edgeLen;
      const nz = dx / edgeLen;

      // Ground Floor: Colonnade pilotis + recessed wall
      const groundPilotisSpacing = 6.0;
      const numCols = Math.max(1, Math.floor(edgeLen / groundPilotisSpacing));
      for (let c = 0; c <= numCols; c++) {
        const t = c / numCols;
        const cx = p1.x + dx * t;
        const cz = p1.z + dz * t;
        const colGeo = new THREE.CylinderGeometry(0.35, 0.35, floorH, 12);
        const colMat = new THREE.MeshStandardMaterial({ color: COLOR_PILOTIS, roughness: 0.4 });
        const colMesh = new THREE.Mesh(colGeo, colMat);
        colMesh.position.set(cx, floorH / 2, cz);
        group.add(colMesh);
      }

      // Ground floor recessed glass corridor
      const groundWallGeo = new THREE.BoxGeometry(edgeLen, floorH * 0.9, 0.3);
      const groundWallMesh = new THREE.Mesh(groundWallGeo, glassMat);
      groundWallMesh.position.set(mx - nx * 0.6, floorH / 2, mz - nz * 0.6);
      groundWallMesh.rotation.y = -angle;
      group.add(groundWallMesh);
      windows.push({ mesh: groundWallMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_GLAZING });

      // Upper Floors (1 to levels - 1):
      // In real photos (Photos 1, 2, 4), the facade has distinct alternating architectural rhythm:
      // - Sandstone vertical sunbreaker louver bays (#D9D2C5) with multiple vertical blades
      // - Glazed solar ribbon bays with crisp monolithic white spandrels (#F8FAFC)
      // - Solid monolithic shear corners
      for (let f = 1; f < levels; f++) {
        const floorBaseY = f * floorH;

        // a) Lower solid parapet spandrel wall
        const spandrelH = 0.85;
        const spandrelGeo = new THREE.BoxGeometry(edgeLen, spandrelH, 0.4);
        const spandrelMesh = new THREE.Mesh(spandrelGeo, wallMat);
        spandrelMesh.position.set(mx, floorBaseY + spandrelH / 2, mz);
        spandrelMesh.rotation.y = -angle;
        group.add(spandrelMesh);

        // b) Continuous Solar Glazing Ribbon (Recessed 0.08m inwards)
        const winH = 1.85;
        const winGeo = new THREE.BoxGeometry(edgeLen * 0.98, winH, 0.2);
        const winMesh = new THREE.Mesh(winGeo, glassMat);
        winMesh.position.set(mx - nx * 0.08, floorBaseY + spandrelH + winH / 2, mz - nz * 0.08);
        winMesh.rotation.y = -angle;
        group.add(winMesh);
        windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_GLAZING });

        // c) Solid Lintel Upper Wall
        const lintelH = floorH - spandrelH - winH; // ~0.9m
        const lintelGeo = new THREE.BoxGeometry(edgeLen, lintelH, 0.4);
        const lintelMesh = new THREE.Mesh(lintelGeo, wallMat);
        lintelMesh.position.set(mx, floorBaseY + spandrelH + winH + lintelH / 2, mz);
        lintelMesh.rotation.y = -angle;
        group.add(lintelMesh);

        // d) Vertical Sandstone Louvers (Brise-Soleil)
        // Grouped into rhythmic sunscreen bays with 0.9m dense blade spacing (Photo 1 & Photo 2)
        if (edgeLen > 5.0) {
          const louverSpacing = 1.0;
          const numLouvers = Math.floor(edgeLen / louverSpacing);
          for (let l = 1; l < numLouvers; l++) {
            // Group into 4-blade clusters every 6 meters
            const cluster = Math.floor(l / 6);
            if (l % 6 === 0 || l % 6 === 5) continue; // leave open glazed slot between louver bays

            const lt = l / numLouvers;
            const lx = p1.x + dx * lt + nx * 0.25;
            const lz = p1.z + dz * lt + nz * 0.25;

            // Louver blade: 0.16m wide, full aperture height + 0.3m projection
            const louverGeo = new THREE.BoxGeometry(0.16, winH + 0.35, 0.5);
            const louverMesh = new THREE.Mesh(louverGeo, louverMat);
            louverMesh.position.set(lx, floorBaseY + spandrelH + winH / 2, lz);
            louverMesh.rotation.y = -angle;
            group.add(louverMesh);
          }
        }
      }
    }

    return {
      group,
      windows,
      floors: levels,
      name: bldgName,
      category: labelCategory,
      color: labelColor,
      labelPos: new THREE.Vector3(centerX, totalHeight + 4.5, centerZ),
    };
  }
}
