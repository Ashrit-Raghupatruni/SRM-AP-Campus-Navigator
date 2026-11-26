import * as THREE from 'three';
import { ArchitecturalMeshResult } from './MeshUtils';
import { CVRamanBlockGenerator } from './CVRamanBlockGenerator';
import { SRBlockGenerator } from './SRBlockGenerator';
import { GangaHostelGenerator } from './GangaHostelGenerator';
import { AnnapurnaMessGenerator } from './AnnapurnaMessGenerator';
import { AcademicComplexCanopyGenerator } from './AcademicComplexCanopyGenerator';
import { Gate1EntranceGenerator } from './Gate1EntranceGenerator';
import { ModularHostelGenerator } from './ModularHostelGenerator';
import { latLngToLocal } from '../geo';
import { getBuildingFloors, getBuildingPalette } from '../../components/map3d/CampusMap3D';

export class ArchitecturalBuildingBuilder {
  static buildCampus(
    buildingsGeoJSON: any,
    isNight: boolean
  ): {
    buildingMeshes: THREE.Mesh[];
    windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[];
    labels: { id: string; name: string; category: string; color: string; pos: THREE.Vector3; feature: any }[];
    rootGroup: THREE.Group;
  } {
    const rootGroup = new THREE.Group();
    const buildingMeshes: THREE.Mesh[] = [];
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];
    const labels: { id: string; name: string; category: string; color: string; pos: THREE.Vector3; feature: any }[] = [];

    if (!buildingsGeoJSON || !buildingsGeoJSON.features) {
      return { buildingMeshes, windows, labels, rootGroup };
    }

    // 1. Build Landmark Monument: Gate 1 Entrance
    const gate1 = Gate1EntranceGenerator.generate();
    rootGroup.add(gate1.group);
    labels.push({
      id: 'gate_1',
      name: gate1.name,
      category: gate1.category,
      color: gate1.color,
      pos: gate1.labelPos,
      feature: null,
    });

    // 2. Build Landmark Architectural Canopy ("The Flying Umbrella")
    const canopy = AcademicComplexCanopyGenerator.generate();
    rootGroup.add(canopy.group);
    labels.push({
      id: 'canopy_academic',
      name: canopy.name,
      category: canopy.category,
      color: canopy.color,
      pos: canopy.labelPos,
      feature: null,
    });

    // 3. Process All Polygons in buildings.geojson
    buildingsGeoJSON.features.forEach((feature: any, idx: number) => {
      const geom = feature.geometry;
      if (!geom || geom.type !== 'Polygon') return;

      const name = feature.properties?.name || `Academic Block ${idx + 1}`;
      const lower = name.toLowerCase();

      let result: ArchitecturalMeshResult | null = null;

      if (lower.includes('cv raman')) {
        result = CVRamanBlockGenerator.generate(feature, isNight);
      } else if (lower.includes('sr block') || lower.includes('radhakrishnan')) {
        result = SRBlockGenerator.generate(feature, isNight);
      } else if (lower.includes('ganga')) {
        result = GangaHostelGenerator.generate(feature, isNight);
      } else if (lower.includes('annapurna') || lower.includes('mess')) {
        result = AnnapurnaMessGenerator.generate(feature, isNight);
      } else if (
        lower.includes('yamuna') ||
        lower.includes('narmada') ||
        lower.includes('godavari') ||
        lower.includes('krishna') ||
        lower.includes('kaveri')
      ) {
        result = ModularHostelGenerator.generate(feature, isNight);
      }

      if (result) {
        rootGroup.add(result.group);
        windows.push(...result.windows);

        // Gather all meshes for raycasting
        result.group.traverse((child) => {
          if (child instanceof THREE.Mesh) {
            child.userData = { name: result!.name, feature, floors: result!.floors };
            buildingMeshes.push(child);
          }
        });

        labels.push({
          id: `label_${idx}`,
          name: `${result.name} (${result.floors}F)`,
          category: result.category,
          color: result.color,
          pos: result.labelPos,
          feature,
        });
        return;
      }

      // Default Academic Blocks (V-Block, X-Lab, JC Bose, Vedavathi, Office)
      const coords = geom.coordinates[0];
      if (!coords || coords.length < 3) return;

      const floors = getBuildingFloors(name);
      const floorH = 3.8;
      const totalHeight = floors * floorH;
      const palette = getBuildingPalette(name);

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

      const extrudeSettings: THREE.ExtrudeGeometryOptions = {
        depth: totalHeight,
        bevelEnabled: true,
        bevelSegments: 2,
        steps: floors,
        bevelSize: 0.5,
        bevelThickness: 0.5,
      };

      const extrudeGeo = new THREE.ExtrudeGeometry(shape, extrudeSettings);
      extrudeGeo.rotateX(Math.PI / 2);

      const buildingMat = new THREE.MeshStandardMaterial({
        color: palette.wallColor,
        roughness: 0.45,
        metalness: 0.15,
      });

      const buildingMesh = new THREE.Mesh(extrudeGeo, buildingMat);
      buildingMesh.castShadow = true;
      buildingMesh.receiveShadow = true;
      buildingMesh.userData = { name, feature, floors };
      rootGroup.add(buildingMesh);
      buildingMeshes.push(buildingMesh);

      // Floor Eyebrow Slabs
      for (let f = 1; f <= floors; f++) {
        const floorY = f * floorH;
        const slabPoints: THREE.Vector3[] = [];
        for (let i = 0; i < coords.length; i++) {
          const [lng, lat] = coords[i];
          const local = latLngToLocal(lat, lng);
          slabPoints.push(new THREE.Vector3(local.x, floorY, local.z));
        }
        slabPoints.push(slabPoints[0]);
        const sGeo = new THREE.BufferGeometry().setFromPoints(slabPoints);
        const sLine = new THREE.Line(sGeo, new THREE.LineBasicMaterial({ color: palette.slabColor, linewidth: 2 }));
        rootGroup.add(sLine);

        // Windows along facades
        for (let i = 0; i < coords.length - 1; i++) {
          const [lng1, lat1] = coords[i];
          const [lng2, lat2] = coords[i + 1];
          const p1 = latLngToLocal(lat1, lng1);
          const p2 = latLngToLocal(lat2, lng2);
          const edgeVec = new THREE.Vector3(p2.x - p1.x, 0, p2.z - p1.z);
          const len = edgeVec.length();
          if (len < 6) continue;

          const winGeo = new THREE.PlaneGeometry(len * 0.72, 1.5);
          const winMat = new THREE.MeshBasicMaterial({
            color: isNight ? palette.nightWindow : palette.windowColor,
            transparent: true,
            opacity: isNight ? 0.95 : 0.65,
          });
          const winMesh = new THREE.Mesh(winGeo, winMat);
          const midX = (p1.x + p2.x) / 2;
          const midZ = (p1.z + p2.z) / 2;
          winMesh.position.set(midX, floorY - 1.8, midZ);
          const angle = Math.atan2(p2.z - p1.z, p2.x - p1.x);
          winMesh.rotation.y = -angle;

          rootGroup.add(winMesh);
          windows.push({ mesh: winMesh, nightColor: palette.nightWindow, dayColor: palette.windowColor });
        }
      }

      // Rooftop Parapet Border
      const roofPoints: THREE.Vector3[] = [];
      for (let i = 0; i < coords.length; i++) {
        const [lng, lat] = coords[i];
        const local = latLngToLocal(lat, lng);
        roofPoints.push(new THREE.Vector3(local.x, totalHeight + 1.2, local.z));
      }
      roofPoints.push(roofPoints[0]);
      const rGeo = new THREE.BufferGeometry().setFromPoints(roofPoints);
      const rLine = new THREE.Line(rGeo, new THREE.LineBasicMaterial({ color: palette.accentColor, linewidth: 3 }));
      rootGroup.add(rLine);

      let category = 'Academic';
      let color = '#38bdf8';
      if (lower.includes('hostel') || lower.includes('vedavathi')) {
        category = 'Hostels';
        color = '#ec4899';
      }

      labels.push({
        id: `label_${idx}`,
        name: `${name} (${floors}F)`,
        category,
        color,
        pos: new THREE.Vector3(centerX, totalHeight + 6, centerZ),
        feature,
      });
    });

    return { buildingMeshes, windows, labels, rootGroup };
  }
}
