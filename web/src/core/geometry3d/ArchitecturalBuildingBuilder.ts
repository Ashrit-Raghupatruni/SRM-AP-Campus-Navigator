import * as THREE from 'three';
import { ArchitecturalMeshResult } from './MeshUtils';
import { CVRamanBlockGenerator } from './CVRamanBlockGenerator';
import { SRBlockGenerator } from './SRBlockGenerator';
import { GangaHostelGenerator } from './GangaHostelGenerator';
import { VedavathiHostelGenerator } from './VedavathiHostelGenerator';
import { AnnapurnaMessGenerator } from './AnnapurnaMessGenerator';
import { AcademicComplexCanopyGenerator } from './AcademicComplexCanopyGenerator';
import { Gate1EntranceGenerator } from './Gate1EntranceGenerator';
import { ModularHostelGenerator } from './ModularHostelGenerator';
import { latLngToLocal } from '../geo';
import { getBuildingFloors, getBuildingPalette } from '../../components/map3d/CampusMap3D';

export class ArchitecturalBuildingBuilder {
  static buildCampus(
    buildingsGeoJSON: any,
    isNight: boolean,
    facilitiesGeoJSON?: any
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

    // 1. Build Landmark Monument: Gate 1 Entrance at southern campus boulevard
    const gate1 = Gate1EntranceGenerator.generate(facilitiesGeoJSON);
    rootGroup.add(gate1.group);
    labels.push({
      id: 'gate_1',
      name: gate1.name,
      category: gate1.category,
      color: gate1.color,
      pos: gate1.labelPos,
      feature: null,
    });

    // 2. Build Landmark Architectural Canopy ("The Flying Umbrella") bridging V-Block & Homi Bhabha
    const canopy = AcademicComplexCanopyGenerator.generate(buildingsGeoJSON);
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
      } else if (lower.includes('vedavathi')) {
        result = VedavathiHostelGenerator.generate(feature, isNight);
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

      // Default Academic & Administration Blocks (V-Block, X-Lab, JC Bose, Homi J Bhabha, Offices)
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

      // Floor horizontal division bands (cantilever slabs)
      for (let f = 1; f < floors; f++) {
        const slabShapeGeo = new THREE.ShapeGeometry(shape);
        slabShapeGeo.rotateX(Math.PI / 2);
        const slabMat = new THREE.MeshStandardMaterial({
          color: palette.slabColor,
          roughness: 0.35,
          metalness: 0.1,
        });
        const slabMesh = new THREE.Mesh(slabShapeGeo, slabMat);
        slabMesh.position.y = f * floorH;
        rootGroup.add(slabMesh);
      }

      // Parapet roof rim
      const roofShapeGeo = new THREE.ShapeGeometry(shape);
      roofShapeGeo.rotateX(Math.PI / 2);
      const roofMat = new THREE.MeshStandardMaterial({
        color: palette.roofColor,
        roughness: 0.7,
        metalness: 0.1,
      });
      const roofMesh = new THREE.Mesh(roofShapeGeo, roofMat);
      roofMesh.position.y = totalHeight + 0.1;
      roofMesh.receiveShadow = true;
      rootGroup.add(roofMesh);

      // Window matrices along the perimeter walls
      for (let f = 0; f < floors; f++) {
        const winY = f * floorH + 1.2;
        for (let i = 0; i < coords.length - 1; i++) {
          const [lng1, lat1] = coords[i];
          const [lng2, lat2] = coords[i + 1];
          const p1 = latLngToLocal(lat1, lng1);
          const p2 = latLngToLocal(lat2, lng2);

          const dx = p2.x - p1.x;
          const dz = p2.z - p1.z;
          const dist = Math.hypot(dx, dz);
          if (dist > 8.0) {
            const numWindows = Math.floor(dist / 5.0);
            for (let w = 1; w <= numWindows; w++) {
              const t = w / (numWindows + 1);
              const wx = p1.x + dx * t;
              const wz = p1.z + dz * t;

              const angle = Math.atan2(dz, dx);
              const winGeo = new THREE.BoxGeometry(2.4, 1.8, 0.4);
              const winMat = new THREE.MeshStandardMaterial({
                color: palette.windowColor,
                roughness: 0.1,
                metalness: 0.9,
                emissive: isNight ? palette.nightWindow : 0x000000,
                emissiveIntensity: isNight ? 0.8 : 0.0,
              });
              const winMesh = new THREE.Mesh(winGeo, winMat);
              winMesh.position.set(wx, winY, wz);
              winMesh.rotation.y = -angle;
              rootGroup.add(winMesh);
              windows.push({ mesh: winMesh, nightColor: palette.nightWindow, dayColor: palette.windowColor });
            }
          }
        }
      }

      // Compute label position
      let labelCategory = 'Academic';
      let labelColor = '#38bdf8';
      if (lower.includes('hostel') || lower.includes('kaveri')) {
        labelCategory = 'Hostels';
        labelColor = '#f43f5e';
      } else if (lower.includes('mess') || lower.includes('annapurna')) {
        labelCategory = 'Food';
        labelColor = '#f59e0b';
      }

      labels.push({
        id: `label_${idx}`,
        name: `${name} (${floors}F)`,
        category: labelCategory,
        color: labelColor,
        pos: new THREE.Vector3(centerX, totalHeight + 4.5, centerZ),
        feature,
      });
    });

    return { buildingMeshes, windows, labels, rootGroup };
  }
}
