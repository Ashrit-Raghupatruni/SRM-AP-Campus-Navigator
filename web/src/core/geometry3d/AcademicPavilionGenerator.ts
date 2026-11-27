import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Academic Pavilions:
 * Vikram Sarabhai Block (V-Block), Homi J Bhabha Block, X-Lab, and JC Bose Block.
 *
 * Real-world Architectural Features (Derived directly from aerial & ground photos):
 * - Short End Walls & Shear Masses: Monolithic solid crisp white architectural finish (#FFFFFF / #F8FAFC)
 *   with dark slate ground plinth (#64748B) - ZERO protruding horizontal cantilever slabs!
 * - Long Primary Facades: Vertical bay rhythm composed of:
 *   1. Floor-to-ceiling vertical solar-reflective glass ribbon bays (#2B5876)
 *   2. Textured vertical sandstone sunbreaker louver bays (#D9D2C5)
 *   3. Full-height solid white precast structural piers running continuously from Floor 1 to Floor 7
 * - Recessed ground-floor entrance arcade with pilotis columns (#475569) and glazed lobby
 * - Rooftop parapet coping enclosing gravel terrace and centered mechanical headhouse.
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
    const COLOR_SLAB = 0xffffff;            // Solid white floor slab plates
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

    const plinthMat = new THREE.MeshStandardMaterial({
      color: 0x64748b, // Dark slate precast ground plinth
      roughness: 0.7,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    // 1. Build Solid Base Polygon Floor Slabs (Internal floor structure, zero cantilever slabs)
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

    // Internal floor slabs at each level (Flush with exterior, NO horizontal cantilever projections!)
    for (let f = 1; f <= levels; f++) {
      const slabGeo = new THREE.ShapeGeometry(shape);
      slabGeo.rotateX(Math.PI / 2);
      const slabMesh = new THREE.Mesh(slabGeo, slabMat);
      slabMesh.position.y = f * floorH;
      slabMesh.receiveShadow = true;
      group.add(slabMesh);
    }

    // 2. Rooftop Parapet and Gravel Deck
    const roofGeo = new THREE.ShapeGeometry(shape);
    roofGeo.rotateX(Math.PI / 2);
    const roofMesh = new THREE.Mesh(roofGeo, roofMat);
    roofMesh.position.y = totalHeight + 0.02;
    roofMesh.receiveShadow = true;
    group.add(roofMesh);

    // Parapet perimeter coping (0.8m flush parapet edge)
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

      const paraGeo = new THREE.BoxGeometry(len, 0.8, 0.35);
      const paraMesh = new THREE.Mesh(paraGeo, parapetMat);
      paraMesh.position.set(mx, totalHeight + 0.4, mz);
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

    // 3. Facade Articulation: Distinct End Shear Walls vs Long Vertical Bay Facades (Photo 4)
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

      // Classify edge: Short end walls vs Long primary facades
      const isShortEndWall = edgeLen < 28.0;

      if (isShortEndWall) {
        // MONOLITHIC SOLID CRISP WHITE END WALL (Photo 4 & Photo 1)
        // Completely solid precast white wall from ground to roof with ground plinth. Zero horizontal cantilever slabs!
        const plinthH = floorH * 0.9;
        const plinthGeo = new THREE.BoxGeometry(edgeLen, plinthH, 0.4);
        const plinthMesh = new THREE.Mesh(plinthGeo, plinthMat);
        plinthMesh.position.set(mx, plinthH / 2, mz);
        plinthMesh.rotation.y = -angle;
        group.add(plinthMesh);

        // Solid upper white wall mass
        const upperWallH = totalHeight - plinthH;
        const upperWallGeo = new THREE.BoxGeometry(edgeLen, upperWallH, 0.4);
        const upperWallMesh = new THREE.Mesh(upperWallGeo, wallMat);
        upperWallMesh.position.set(mx, plinthH + upperWallH / 2, mz);
        upperWallMesh.rotation.y = -angle;
        group.add(upperWallMesh);

        // Subtle vertical architectural expansion joints (clean modern detailing)
        if (edgeLen > 8.0) {
          const jointSpacing = 4.5;
          const numJoints = Math.floor(edgeLen / jointSpacing);
          for (let j = 1; j <= numJoints; j++) {
            const jt = j / (numJoints + 1);
            const jx = p1.x + dx * jt + nx * 0.22;
            const jz = p1.z + dz * jt + nz * 0.22;
            const joint = new THREE.Mesh(
              new THREE.BoxGeometry(0.08, totalHeight, 0.08),
              plinthMat
            );
            joint.position.set(jx, totalHeight / 2, jz);
            joint.rotation.y = -angle;
            group.add(joint);
          }
        }
      } else {
        // LONG PRIMARY FACADE: VERTICAL BAY ARCHITECTURE (Photos 1, 2, 4)
        // Ground Floor: Recessed glass lobby arcade with structural circular columns
        const groundPilotisSpacing = 6.5;
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

        const groundWallGeo = new THREE.BoxGeometry(edgeLen * 0.98, floorH * 0.88, 0.2);
        const groundWallMesh = new THREE.Mesh(groundWallGeo, glassMat);
        groundWallMesh.position.set(mx - nx * 0.5, floorH / 2, mz - nz * 0.5);
        groundWallMesh.rotation.y = -angle;
        group.add(groundWallMesh);
        windows.push({ mesh: groundWallMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_GLAZING });

        // Upper Floors (1 to levels - 1):
        // Segmented into distinct vertical bays:
        // - Floor-to-ceiling vertical glass ribbon bays with thin dark frames
        // - Vertical textured sandstone sunbreaker screen panels (#D9D2C5)
        // - Full-height vertical white precast structural piers running continuously from Floor 1 to Floor 7
        const bayW = 5.6;
        const numBays = Math.max(1, Math.floor(edgeLen / bayW));

        for (let b = 0; b < numBays; b++) {
          const tCenter = (b + 0.5) / numBays;
          const bx = p1.x + dx * tCenter;
          const bz = p1.z + dz * tCenter;
          const actualBayW = edgeLen / numBays;

          const isScreenBay = (b % 2 === 0);

          for (let f = 1; f < levels; f++) {
            const floorBaseY = f * floorH;

            if (isScreenBay) {
              // Textured Vertical Sandstone Screen Louver Bay (Photo 1 & Photo 4)
              // Glazed backdrop
              const winGeo = new THREE.BoxGeometry(actualBayW * 0.96, floorH * 0.9, 0.2);
              const winMesh = new THREE.Mesh(winGeo, glassMat);
              winMesh.position.set(bx - nx * 0.05, floorBaseY + floorH / 2, bz - nz * 0.05);
              winMesh.rotation.y = -angle;
              group.add(winMesh);
              windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_GLAZING });

              // Dense array of vertical sandstone louvers (4 vertical blades per bay)
              const blades = 4;
              for (let bl = 1; bl <= blades; bl++) {
                const bOffset = (bl - (blades + 1) / 2) * (actualBayW / (blades + 1));
                const blX = bx + (dx / edgeLen) * bOffset + nx * 0.22;
                const blZ = bz + (dz / edgeLen) * bOffset + nz * 0.22;

                const louverBlade = new THREE.Mesh(
                  new THREE.BoxGeometry(0.18, floorH * 0.94, 0.45),
                  louverMat
                );
                louverBlade.position.set(blX, floorBaseY + floorH / 2, blZ);
                louverBlade.rotation.y = -angle;
                group.add(louverBlade);
              }
            } else {
              // Vertical Ribbon Glazing Bay with crisp white spandrel lintels (Photo 1 & Photo 4)
              const spandrelH = 0.75;
              const spandrel = new THREE.Mesh(
                new THREE.BoxGeometry(actualBayW, spandrelH, 0.35),
                wallMat
              );
              spandrel.position.set(bx, floorBaseY + spandrelH / 2, bz);
              spandrel.rotation.y = -angle;
              group.add(spandrel);

              const winH = floorH - spandrelH;
              const winMesh = new THREE.Mesh(
                new THREE.BoxGeometry(actualBayW * 0.96, winH * 0.94, 0.2),
                glassMat
              );
              winMesh.position.set(bx - nx * 0.05, floorBaseY + spandrelH + winH / 2, bz - nz * 0.05);
              winMesh.rotation.y = -angle;
              group.add(winMesh);
              windows.push({ mesh: winMesh, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_GLAZING });
            }
          }

          // Full-Height Solid White Structural Pier between bays (Photo 1, Photo 4)
          const pierT = (b + 1) / numBays;
          const pierX = p1.x + dx * pierT + nx * 0.15;
          const pierZ = p1.z + dz * pierT + nz * 0.15;
          const pier = new THREE.Mesh(
            new THREE.BoxGeometry(0.4, totalHeight - floorH, 0.5),
            wallMat
          );
          pier.position.set(pierX, floorH + (totalHeight - floorH) / 2, pierZ);
          pier.rotation.y = -angle;
          group.add(pier);
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
