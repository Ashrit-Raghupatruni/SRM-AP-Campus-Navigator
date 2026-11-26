import * as THREE from 'three';
import { latLngToLocal } from '../geo';

/**
 * Procedural Generator for Realistic Paved Walkways and Asphalt Vehicular Roads.
 * Translates campus paths and roads GeoJSON into 3D textured ribbon meshes:
 *
 * 1. Vehicular Roads (Asphalt #273244):
 *    - 7.0m wide roadway
 *    - Elevated concrete outer boundary kerbs (#CBD5E1)
 *    - Painted white dashed center-line thermoplastic markings (#F8FAFC)
 *
 * 2. Pedestrian Concourse & Walkways (Sandstone Interlocking Pavers #DFD9CC / #D4CEBF):
 *    - 3.4m wide pedestrian concourse
 *    - Modular ~1.4m paving tile blocks with alternating subtle luminance jitter
 *    - Transverse dark granite divider ribbons (#475569) every 12-14 meters
 *    - Elevated 3D border kerb stones (#CBD5E1) giving physical depth above grass
 */
export class ProceduralRoadBuilder {
  static buildRoadsAndPaths(
    pathsGeoJSON: any,
    roadsGeoJSON: any
  ): THREE.Group {
    const root = new THREE.Group();

    // Material definitions based on real photo library
    const PAVER_BASE = 0xdfd9cc;     // Sandstone interlocking paver
    const PAVER_ALT = 0xd4cebf;      // Alternate batch masonry tone
    const ACCENT_BAND = 0x475569;    // Dark granite divider ribbon
    const BORDER_KERB = 0xcbd5e1;    // Concrete boundary kerb
    const KERB_SHADOW = 0x94a3b8;    // Bevel shadow
    const ASPHALT_BASE = 0x273244;   // Bituminous road asphalt
    const ROAD_MARKING = 0xf8fafc;   // Painted white thermoplastic

    // 1. GENERATE VEHICULAR ASPHALT ROADS (roadsGeoJSON)
    if (roadsGeoJSON && roadsGeoJSON.features) {
      const roadW = 7.0; // Standard dual-lane width
      const halfW = roadW / 2;
      const roadElev = 0.04;
      const kerbW = 0.25;
      const kerbH = 0.12;

      const roadMat = new THREE.MeshStandardMaterial({
        color: ASPHALT_BASE,
        roughness: 0.85,
        metalness: 0.1,
      });
      const kerbMat = new THREE.MeshStandardMaterial({
        color: BORDER_KERB,
        roughness: 0.6,
        metalness: 0.1,
      });
      const markingMat = new THREE.MeshBasicMaterial({
        color: ROAD_MARKING,
      });

      const roadGeoList: THREE.BufferGeometry[] = [];
      const kerbGeoList: THREE.BufferGeometry[] = [];
      const markingGeoList: THREE.BufferGeometry[] = [];

      for (const feat of roadsGeoJSON.features) {
        const coords = feat.geometry?.coordinates;
        if (!coords || coords.length < 2) continue;

        for (let i = 0; i < coords.length - 1; i++) {
          const [lng1, lat1] = coords[i];
          const [lng2, lat2] = coords[i + 1];
          const p1 = latLngToLocal(lat1, lng1);
          const p2 = latLngToLocal(lat2, lng2);

          const dx = p2.x - p1.x;
          const dz = p2.z - p1.z;
          const len = Math.hypot(dx, dz);
          if (len < 0.5) continue;

          const dirX = dx / len;
          const dirZ = dz / len;
          const latX = -dirZ;
          const latZ = dirX;

          // Road Surface Quad
          const r1 = new THREE.Vector3(p1.x - latX * halfW, roadElev, p1.z - latZ * halfW);
          const r2 = new THREE.Vector3(p1.x + latX * halfW, roadElev, p1.z + latZ * halfW);
          const r3 = new THREE.Vector3(p2.x + latX * halfW, roadElev, p2.z + latZ * halfW);
          const r4 = new THREE.Vector3(p2.x - latX * halfW, roadElev, p2.z - latZ * halfW);

          const quadGeo = new THREE.BufferGeometry();
          const vertices = new Float32Array([
            r1.x, r1.y, r1.z,  r2.x, r2.y, r2.z,  r3.x, r3.y, r3.z,
            r1.x, r1.y, r1.z,  r3.x, r3.y, r3.z,  r4.x, r4.y, r4.z
          ]);
          quadGeo.setAttribute('position', new THREE.BufferAttribute(vertices, 3));
          quadGeo.computeVertexNormals();
          roadGeoList.push(quadGeo);

          // Elevated Side Kerbs (Left & Right)
          const lk1 = new THREE.Vector3(r1.x - latX * kerbW, roadElev + kerbH, r1.z - latZ * kerbW);
          const lk2 = new THREE.Vector3(r1.x, roadElev + kerbH, r1.z);
          const lk3 = new THREE.Vector3(r4.x, roadElev + kerbH, r4.z);
          const lk4 = new THREE.Vector3(r4.x - latX * kerbW, roadElev + kerbH, r4.z - latZ * kerbW);

          const lKerbGeo = new THREE.BufferGeometry();
          const lVerts = new Float32Array([
            lk1.x, lk1.y, lk1.z, lk2.x, lk2.y, lk2.z, lk3.x, lk3.y, lk3.z,
            lk1.x, lk1.y, lk1.z, lk3.x, lk3.y, lk3.z, lk4.x, lk4.y, lk4.z
          ]);
          lKerbGeo.setAttribute('position', new THREE.BufferAttribute(lVerts, 3));
          lKerbGeo.computeVertexNormals();
          kerbGeoList.push(lKerbGeo);

          const rk1 = new THREE.Vector3(r2.x, roadElev + kerbH, r2.z);
          const rk2 = new THREE.Vector3(r2.x + latX * kerbW, roadElev + kerbH, r2.z + latZ * kerbW);
          const rk3 = new THREE.Vector3(r3.x + latX * kerbW, roadElev + kerbH, r3.z + latZ * kerbW);
          const rk4 = new THREE.Vector3(r3.x, roadElev + kerbH, r3.z);

          const rKerbGeo = new THREE.BufferGeometry();
          const rVerts = new Float32Array([
            rk1.x, rk1.y, rk1.z, rk2.x, rk2.y, rk2.z, rk3.x, rk3.y, rk3.z,
            rk1.x, rk1.y, rk1.z, rk3.x, rk3.y, rk3.z, rk4.x, rk4.y, rk4.z
          ]);
          rKerbGeo.setAttribute('position', new THREE.BufferAttribute(rVerts, 3));
          rKerbGeo.computeVertexNormals();
          kerbGeoList.push(rKerbGeo);

          // Center-Line Dashes (White painted stripes every 3m on road surface)
          const dashLen = 1.8;
          const gapLen = 1.8;
          const step = dashLen + gapLen;
          const numDashes = Math.floor(len / step);
          for (let d = 0; d < numDashes; d++) {
            const startDist = d * step;
            const endDist = startDist + dashLen;
            const c1 = new THREE.Vector3(p1.x + dirX * startDist, roadElev + 0.005, p1.z + dirZ * startDist);
            const c2 = new THREE.Vector3(p1.x + dirX * endDist, roadElev + 0.005, p1.z + dirZ * endDist);
            const dw = 0.12;

            const m1 = new THREE.Vector3(c1.x - latX * dw, c1.y, c1.z - latZ * dw);
            const m2 = new THREE.Vector3(c1.x + latX * dw, c1.y, c1.z + latZ * dw);
            const m3 = new THREE.Vector3(c2.x + latX * dw, c2.y, c2.z + latZ * dw);
            const m4 = new THREE.Vector3(c2.x - latX * dw, c2.y, c2.z - latZ * dw);

            const dashGeo = new THREE.BufferGeometry();
            const dashVerts = new Float32Array([
              m1.x, m1.y, m1.z, m2.x, m2.y, m2.z, m3.x, m3.y, m3.z,
              m1.x, m1.y, m1.z, m3.x, m3.y, m3.z, m4.x, m4.y, m4.z
            ]);
            dashGeo.setAttribute('position', new THREE.BufferAttribute(dashVerts, 3));
            markingGeoList.push(dashGeo);
          }
        }
      }

      // Add merged or aggregated meshes
      for (const geo of roadGeoList) {
        root.add(new THREE.Mesh(geo, roadMat));
      }
      for (const geo of kerbGeoList) {
        root.add(new THREE.Mesh(geo, kerbMat));
      }
      for (const geo of markingGeoList) {
        root.add(new THREE.Mesh(geo, markingMat));
      }
    }

    // 2. GENERATE PEDESTRIAN WALKWAYS (pathsGeoJSON)
    if (pathsGeoJSON && pathsGeoJSON.features) {
      const walkW = 3.4; // Concourse width
      const halfW = walkW / 2;
      const walkElev = 0.045;
      const kerbW = 0.18;
      const kerbH = 0.08;

      const paverBaseMat = new THREE.MeshStandardMaterial({ color: PAVER_BASE, roughness: 0.8 });
      const paverAltMat = new THREE.MeshStandardMaterial({ color: PAVER_ALT, roughness: 0.8 });
      const accentMat = new THREE.MeshStandardMaterial({ color: ACCENT_BAND, roughness: 0.7 });
      const kerbMat = new THREE.MeshStandardMaterial({ color: BORDER_KERB, roughness: 0.6 });
      const kerbShadowMat = new THREE.MeshStandardMaterial({ color: KERB_SHADOW, roughness: 0.7 });

      for (const feat of pathsGeoJSON.features) {
        const coords = feat.geometry?.coordinates;
        if (!coords || coords.length < 2) continue;

        for (let i = 0; i < coords.length - 1; i++) {
          const [lng1, lat1] = coords[i];
          const [lng2, lat2] = coords[i + 1];
          const p1 = latLngToLocal(lat1, lng1);
          const p2 = latLngToLocal(lat2, lng2);

          const dx = p2.x - p1.x;
          const dz = p2.z - p1.z;
          const len = Math.hypot(dx, dz);
          if (len < 0.4) continue;

          const dirX = dx / len;
          const dirZ = dz / len;
          const latX = -dirZ;
          const latZ = dirX;

          // Procedural modular tiling along path length: 1.4m per tile block
          const tileStep = 1.4;
          const numTiles = Math.max(1, Math.floor(len / tileStep));
          const actualStep = len / numTiles;

          for (let t = 0; t < numTiles; t++) {
            const d1 = t * actualStep;
            const d2 = (t + 1) * actualStep;

            const c1 = new THREE.Vector3(p1.x + dirX * d1, walkElev, p1.z + dirZ * d1);
            const c2 = new THREE.Vector3(p1.x + dirX * d2, walkElev, p1.z + dirZ * d2);

            const v1 = new THREE.Vector3(c1.x - latX * halfW, c1.y, c1.z - latZ * halfW);
            const v2 = new THREE.Vector3(c1.x + latX * halfW, c1.y, c1.z + latZ * halfW);
            const v3 = new THREE.Vector3(c2.x + latX * halfW, c2.y, c2.z + latZ * halfW);
            const v4 = new THREE.Vector3(c2.x - latX * halfW, c2.y, c2.z - latZ * halfW);

            // Material selection: Every 9th tile (~12.6m) is a dark granite dividing ribbon
            const isAccent = t % 9 === 0 && t > 0;
            const mat = isAccent ? accentMat : (t % 2 === 0 ? paverBaseMat : paverAltMat);

            const tileGeo = new THREE.BufferGeometry();
            const verts = new Float32Array([
              v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, v3.x, v3.y, v3.z,
              v1.x, v1.y, v1.z, v3.x, v3.y, v3.z, v4.x, v4.y, v4.z
            ]);
            tileGeo.setAttribute('position', new THREE.BufferAttribute(verts, 3));
            tileGeo.computeVertexNormals();

            const tileMesh = new THREE.Mesh(tileGeo, mat);
            tileMesh.receiveShadow = true;
            root.add(tileMesh);

            // Left Elevated Border Kerb
            const lk1 = new THREE.Vector3(v1.x - latX * kerbW, walkElev + kerbH, v1.z - latZ * kerbW);
            const lk2 = new THREE.Vector3(v1.x, walkElev + kerbH, v1.z);
            const lk3 = new THREE.Vector3(v4.x, walkElev + kerbH, v4.z);
            const lk4 = new THREE.Vector3(v4.x - latX * kerbW, walkElev + kerbH, v4.z - latZ * kerbW);

            const lKerbGeo = new THREE.BufferGeometry();
            const lVerts = new Float32Array([
              lk1.x, lk1.y, lk1.z, lk2.x, lk2.y, lk2.z, lk3.x, lk3.y, lk3.z,
              lk1.x, lk1.y, lk1.z, lk3.x, lk3.y, lk3.z, lk4.x, lk4.y, lk4.z
            ]);
            lKerbGeo.setAttribute('position', new THREE.BufferAttribute(lVerts, 3));
            lKerbGeo.computeVertexNormals();
            root.add(new THREE.Mesh(lKerbGeo, kerbMat));

            // Right Elevated Border Kerb
            const rk1 = new THREE.Vector3(v2.x, walkElev + kerbH, v2.z);
            const rk2 = new THREE.Vector3(v2.x + latX * kerbW, walkElev + kerbH, v2.z + latZ * kerbW);
            const rk3 = new THREE.Vector3(v3.x + latX * kerbW, walkElev + kerbH, v3.z + latZ * kerbW);
            const rk4 = new THREE.Vector3(v3.x, walkElev + kerbH, v3.z);

            const rKerbGeo = new THREE.BufferGeometry();
            const rVerts = new Float32Array([
              rk1.x, rk1.y, rk1.z, rk2.x, rk2.y, rk2.z, rk3.x, rk3.y, rk3.z,
              rk1.x, rk1.y, rk1.z, rk3.x, rk3.y, rk3.z, rk4.x, rk4.y, rk4.z
            ]);
            rKerbGeo.setAttribute('position', new THREE.BufferAttribute(rVerts, 3));
            rKerbGeo.computeVertexNormals();
            root.add(new THREE.Mesh(rKerbGeo, kerbMat));
          }
        }
      }
    }

    return root;
  }
}
