import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for Annapurna Mess (srmap_bldg_8).
 * - Stepped organic landscape pavilion integrated into the slope
 * - Built DIRECTLY on its exact 21-vertex GeoJSON curved polygon footprint
 * - Signature entrance portal in vibrant Terracotta/Coral red (#E06D53)
 * - LIVING GREEN ROOF (Sod/Turf #2E7D32) with white perimeter fascia (#F8FAFC)
 * - Floor-to-ceiling clerestory glass ribbons (#38BDF8)
 * - Solid double-sided materials with warm evening illumination
 */
export class AnnapurnaMessGenerator {
  static generate(feature: any, isNight: boolean): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const COLOR_CORAL_ENTRANCE = 0xe06d53;
    const COLOR_WHITE_FASCIA = 0xf8fafc;
    const COLOR_WALL_WARM = 0xfef3c7;
    const COLOR_LIVING_GREEN_ROOF = 0x2e7d32; // Living grass turf
    const COLOR_CLERESTORY_GLASS = 0x38bdf8;
    const COLOR_NIGHT_GLOW = 0xf59e0b;

    const coords: [number, number][] = feature.geometry?.coordinates?.[0] || [];
    if (coords.length < 3) {
      return {
        group,
        windows,
        floors: 2,
        name: 'Annapurna Mess',
        category: 'Food',
        color: '#f59e0b',
        labelPos: new THREE.Vector3(0, 12, 0),
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
    const TOTAL_H = 7.5; // 2 stories dining hall

    const wallMat = new THREE.MeshStandardMaterial({
      color: COLOR_WALL_WARM,
      roughness: 0.65,
      metalness: 0.1,
      side: THREE.DoubleSide,
    });

    const fasciaMat = new THREE.MeshStandardMaterial({
      color: COLOR_WHITE_FASCIA,
      roughness: 0.35,
      metalness: 0.15,
      side: THREE.DoubleSide,
    });

    const turfMat = new THREE.MeshStandardMaterial({
      color: COLOR_LIVING_GREEN_ROOF,
      roughness: 0.9,
      metalness: 0.05,
      side: THREE.DoubleSide,
    });

    const glassMat = new THREE.MeshStandardMaterial({
      color: COLOR_CLERESTORY_GLASS,
      roughness: 0.15,
      metalness: 0.85,
      emissive: isNight ? COLOR_NIGHT_GLOW : 0x000000,
      emissiveIntensity: isNight ? 0.9 : 0.0,
      side: THREE.DoubleSide,
    });

    // 1. Build Exact Curved 2D Polygon Base Plinth and Roof
    const shape = new THREE.Shape();
    localPts.forEach((p, i) => {
      if (i === 0) shape.moveTo(p.x, p.z);
      else shape.lineTo(p.x, p.z);
    });

    // Base Floor Slab
    const baseGeo = new THREE.ShapeGeometry(shape);
    baseGeo.rotateX(Math.PI / 2);
    const baseMesh = new THREE.Mesh(baseGeo, fasciaMat);
    baseMesh.position.y = 0.05;
    baseMesh.receiveShadow = true;
    group.add(baseMesh);

    // Living Green Roof Turf
    const roofTurfGeo = new THREE.ShapeGeometry(shape);
    roofTurfGeo.rotateX(Math.PI / 2);
    const roofTurf = new THREE.Mesh(roofTurfGeo, turfMat);
    roofTurf.position.y = TOTAL_H + 0.1;
    roofTurf.receiveShadow = true;
    group.add(roofTurf);

    // 2. White Perimeter Roof Fascia & Solid Walls along true curved contour
    for (let i = 0; i < localPts.length - 1; i++) {
      const p1 = localPts[i];
      const p2 = localPts[i + 1];
      const dx = p2.x - p1.x;
      const dz = p2.z - p1.z;
      const edgeLen = Math.hypot(dx, dz);
      if (edgeLen < 0.2) continue;

      const mx = (p1.x + p2.x) / 2;
      const mz = (p1.z + p2.z) / 2;
      const angle = Math.atan2(dz, dx);
      const nx = -dz / edgeLen;
      const nz = dx / edgeLen;

      // Roof Fascia Trim (0.6m high projecting white band)
      const fasciaGeo = new THREE.BoxGeometry(edgeLen + 0.2, 0.6, 0.6);
      const fasciaMesh = new THREE.Mesh(fasciaGeo, fasciaMat);
      fasciaMesh.position.set(mx, TOTAL_H + 0.3, mz);
      fasciaMesh.rotation.y = -angle;
      group.add(fasciaMesh);

      // Lower Solid Wall (0.9m)
      const lowerWallGeo = new THREE.BoxGeometry(edgeLen, 0.9, 0.35);
      const lowerWall = new THREE.Mesh(lowerWallGeo, wallMat);
      lowerWall.position.set(mx, 0.45, mz);
      lowerWall.rotation.y = -angle;
      group.add(lowerWall);

      // Floor-to-Ceiling Clerestory Ribbon Glazing (4.8m tall)
      const glassGeo = new THREE.BoxGeometry(edgeLen * 0.96, 4.8, 0.2);
      const glassMesh = new THREE.Mesh(glassGeo, glassMat);
      glassMesh.position.set(mx - nx * 0.05, 0.9 + 2.4, mz - nz * 0.05);
      glassMesh.rotation.y = -angle;
      group.add(glassMesh);
      windows.push({ mesh: glassMesh, nightColor: COLOR_NIGHT_GLOW, dayColor: COLOR_CLERESTORY_GLASS });

      // Upper Lintel Solid Wall (1.8m)
      const upperWallGeo = new THREE.BoxGeometry(edgeLen, 1.8, 0.35);
      const upperWall = new THREE.Mesh(upperWallGeo, wallMat);
      upperWall.position.set(mx, 0.9 + 4.8 + 0.9, mz);
      upperWall.rotation.y = -angle;
      group.add(upperWall);
    }

    // 3. Signature Coral/Terracotta Entrance Portal
    const entrancePortal = createBoxMesh(
      centerX - 4.0, centerX + 4.0,
      0, 6.0,
      maxZ - 2.5, maxZ + 1.5,
      COLOR_CORAL_ENTRANCE,
      0.4, 0.2
    );
    group.add(entrancePortal);

    group.userData = { name: 'Annapurna Mess', feature, floors: 2 };

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(centerX, TOTAL_H + 4.5, centerZ),
      name: 'Annapurna Mess',
      category: 'Food',
      color: '#f59e0b',
      floors: 2,
    };
  }
}
