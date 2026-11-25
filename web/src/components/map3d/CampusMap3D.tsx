import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';
import { latLngToLocal } from '../../core/geo';
import { SolarInfo } from '../../types/campus';
import { RouteResult } from '../../types/campus';

export interface CampusLabel {
  id: string;
  name: string;
  category: string;
  color: string;
  x: number;
  z: number;
  screenX: number;
  screenY: number;
  visible: boolean;
}

// Verified real-world floor counts for SRM University-AP buildings
const BUILDING_FLOOR_COUNTS: Record<string, number> = {
  'cv raman block': 12,        // 12-story high-rise academic tower (Central Library F12)
  'homi j bhabha block': 7,    // 7 levels research facility
  'homi j. bhabha block': 7,
  'sr block': 4,               // 4 stories (OE1/SR1 classrooms & faculty cabins)
  'v block': 5,                // 5 levels (Vikram Sarabhai Academic Block)
  'x-lab': 4,                  // 4 levels AI & fabrication labs & Auditorium
  'jc bose block': 5,          // 5 stories academic block
  'ganga boys\' hostel': 6,    // 6-story student residence
  'vedavathi boys\' hostel': 6,// 6-story student residence
  'narmada hostel': 5,
  'yamuna hostel': 5,
  'godavari hostel': 5,
  'krishna hostel': 5,
  'kaveri faculty residence': 5,
  'annapurna mess': 2,
  'office': 2,
};

function getBuildingFloors(name: string): number {
  const lower = (name || '').toLowerCase().trim();
  for (const [key, floors] of Object.entries(BUILDING_FLOOR_COUNTS)) {
    if (lower.includes(key) || key.includes(lower)) {
      return floors;
    }
  }
  return 4; // Default sensible fallback
}

interface CampusMap3DProps {
  buildingsGeoJSON: any;
  pathsGeoJSON: any;
  roadsGeoJSON: any;
  groundsGeoJSON: any;
  sportsGeoJSON: any;
  solarInfo: SolarInfo;
  activeRoute: RouteResult | null;
  activeFilter: string;
  isFirstPerson: boolean;
  isAngleLocked: boolean;
  onSelectBuilding: (name: string, feature: any) => void;
  onLabelsUpdate?: (labels: CampusLabel[]) => void;
}

export const CampusMap3D: React.FC<CampusMap3DProps> = ({
  buildingsGeoJSON,
  pathsGeoJSON,
  roadsGeoJSON,
  groundsGeoJSON,
  sportsGeoJSON,
  solarInfo,
  activeRoute,
  activeFilter,
  isFirstPerson,
  isAngleLocked,
  onSelectBuilding,
  onLabelsUpdate,
}) => {
  const mountRef = useRef<HTMLDivElement>(null);

  // References to dynamic lights and scene elements
  const sceneRef = useRef<THREE.Scene | null>(null);
  const sunLightRef = useRef<THREE.DirectionalLight | null>(null);
  const ambientLightRef = useRef<THREE.AmbientLight | null>(null);
  const hemiLightRef = useRef<THREE.HemisphereLight | null>(null);
  const starsRef = useRef<THREE.Points | null>(null);
  const buildingMeshesRef = useRef<THREE.Mesh[]>([]);
  const windowPanelsRef = useRef<THREE.Mesh[]>([]);
  const streetLampsGroupRef = useRef<THREE.Group | null>(null);
  const treesGroupRef = useRef<THREE.Group | null>(null);
  const sportsGroupRef = useRef<THREE.Group | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null);

  // Label anchors in 3D world space
  const labelAnchorsRef = useRef<{ id: string; name: string; category: string; color: string; pos: THREE.Vector3; feature: any }[]>([]);

  // Orbit camera control state
  const isDraggingRef = useRef(false);
  const previousMousePositionRef = useRef({ x: 0, y: 0 });
  const cameraAngleRef = useRef({ theta: Math.PI / 3.8, phi: Math.PI / 3.4, radius: 260 });
  const panOffsetRef = useRef({ x: 0, z: 0 });

  useEffect(() => {
    if (!mountRef.current) return;

    const scene = new THREE.Scene();
    sceneRef.current = scene;

    const width = mountRef.current.clientWidth;
    const height = mountRef.current.clientHeight;

    const camera = new THREE.PerspectiveCamera(40, width / height, 1, 3000);
    cameraRef.current = camera;
    updateCamera();

    const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.1;
    mountRef.current.appendChild(renderer.domElement);
    rendererRef.current = renderer;

    // Lights
    const hemiLight = new THREE.HemisphereLight(0xffffff, 0x142012, 0.6);
    hemiLight.position.set(0, 500, 0);
    scene.add(hemiLight);
    hemiLightRef.current = hemiLight;

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.3);
    scene.add(ambientLight);
    ambientLightRef.current = ambientLight;

    const sunLight = new THREE.DirectionalLight(0xffffff, 1.4);
    sunLight.castShadow = true;
    sunLight.shadow.mapSize.width = 2048;
    sunLight.shadow.mapSize.height = 2048;
    sunLight.shadow.camera.near = 10;
    sunLight.shadow.camera.far = 1200;
    const d = 350;
    sunLight.shadow.camera.left = -d;
    sunLight.shadow.camera.right = d;
    sunLight.shadow.camera.top = d;
    sunLight.shadow.camera.bottom = -d;
    sunLight.shadow.bias = -0.0004;
    scene.add(sunLight);
    sunLightRef.current = sunLight;

    // Dark Turf Ground Plane (1.8km x 1.8km)
    const groundGeo = new THREE.PlaneGeometry(1800, 1800);
    const groundMat = new THREE.MeshStandardMaterial({
      color: 0x122416, // Lush dark campus lawn turf
      roughness: 0.95,
      metalness: 0.05,
    });
    const ground = new THREE.Mesh(groundGeo, groundMat);
    ground.rotation.x = -Math.PI / 2;
    ground.receiveShadow = true;
    scene.add(ground);

    // Groups
    const streetLampsGroup = new THREE.Group();
    scene.add(streetLampsGroup);
    streetLampsGroupRef.current = streetLampsGroup;

    const treesGroup = new THREE.Group();
    scene.add(treesGroup);
    treesGroupRef.current = treesGroup;

    const sportsGroup = new THREE.Group();
    scene.add(sportsGroup);
    sportsGroupRef.current = sportsGroup;

    // Stars Field
    const starCount = 1200;
    const starGeo = new THREE.BufferGeometry();
    const starPositions = new Float32Array(starCount * 3);
    for (let i = 0; i < starCount * 3; i += 3) {
      starPositions[i] = (Math.random() - 0.5) * 2000;
      starPositions[i + 1] = Math.random() * 600 + 100;
      starPositions[i + 2] = (Math.random() - 0.5) * 2000;
    }
    starGeo.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
    const starMat = new THREE.PointsMaterial({ color: 0xffffff, size: 2.2, transparent: true, opacity: 0.8 });
    const stars = new THREE.Points(starGeo, starMat);
    scene.add(stars);
    starsRef.current = stars;

    // Procedural 3D Trees across campus
    const trunkGeo = new THREE.CylinderGeometry(0.4, 0.6, 3, 6);
    const trunkMat = new THREE.MeshStandardMaterial({ color: 0x422006, roughness: 0.9 });
    const foliageGeo = new THREE.DodecahedronGeometry(2.5, 1);
    const foliageMat = new THREE.MeshStandardMaterial({ color: 0x166534, roughness: 0.8 });

    // Plant trees along pathways and campus open grounds
    for (let i = 0; i < 180; i++) {
      const angle = Math.random() * Math.PI * 2;
      const radius = 60 + Math.random() * 200;
      const tx = Math.cos(angle) * radius;
      const tz = Math.sin(angle) * radius;

      const tree = new THREE.Group();
      const trunk = new THREE.Mesh(trunkGeo, trunkMat);
      trunk.position.y = 1.5;
      trunk.castShadow = true;
      trunk.receiveShadow = true;
      tree.add(trunk);

      const foliage = new THREE.Mesh(foliageGeo, foliageMat);
      foliage.position.y = 4.2;
      foliage.scale.set(1 + Math.random() * 0.4, 1 + Math.random() * 0.5, 1 + Math.random() * 0.4);
      foliage.castShadow = true;
      foliage.receiveShadow = true;
      tree.add(foliage);

      tree.position.set(tx, 0, tz);
      treesGroup.add(tree);
    }

    // Mouse handlers for Orbit & Pan
    const dom = renderer.domElement;
    let isRightClick = false;

    const onMouseDown = (e: MouseEvent) => {
      isDraggingRef.current = true;
      isRightClick = e.button === 2;
      previousMousePositionRef.current = { x: e.clientX, y: e.clientY };
    };

    const onContextMenu = (e: MouseEvent) => e.preventDefault();

    const onMouseMove = (e: MouseEvent) => {
      if (!isDraggingRef.current) return;
      const deltaX = e.clientX - previousMousePositionRef.current.x;
      const deltaY = e.clientY - previousMousePositionRef.current.y;

      if (isRightClick) {
        const factor = 0.35;
        const forward = new THREE.Vector3(-Math.sin(cameraAngleRef.current.theta), 0, -Math.cos(cameraAngleRef.current.theta));
        const right = new THREE.Vector3(Math.cos(cameraAngleRef.current.theta), 0, -Math.sin(cameraAngleRef.current.theta));
        panOffsetRef.current.x += (-right.x * deltaX + forward.x * deltaY) * factor;
        panOffsetRef.current.z += (-right.z * deltaX + forward.z * deltaY) * factor;
      } else if (!isAngleLocked) {
        cameraAngleRef.current.theta -= deltaX * 0.006;
        cameraAngleRef.current.phi = Math.max(0.2, Math.min(Math.PI / 2.1, cameraAngleRef.current.phi + deltaY * 0.006));
      }

      previousMousePositionRef.current = { x: e.clientX, y: e.clientY };
      updateCamera();
    };

    const onMouseUp = () => {
      isDraggingRef.current = false;
    };

    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      cameraAngleRef.current.radius = Math.max(60, Math.min(500, cameraAngleRef.current.radius + e.deltaY * 0.3));
      updateCamera();
    };

    dom.addEventListener('mousedown', onMouseDown);
    dom.addEventListener('contextmenu', onContextMenu);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    dom.addEventListener('wheel', onWheel);

    // Animation & Label projection loop
    let animId: number;
    const animate = () => {
      animId = requestAnimationFrame(animate);
      renderer.render(scene, camera);

      if (onLabelsUpdate && mountRef.current) {
        const w = mountRef.current.clientWidth;
        const h = mountRef.current.clientHeight;
        const halfW = w / 2;
        const halfH = h / 2;

        const projectedLabels: CampusLabel[] = [];
        for (const item of labelAnchorsRef.current) {
          const clonePos = item.pos.clone();
          clonePos.project(camera);

          const isVisible = clonePos.z < 1 && clonePos.x >= -1.1 && clonePos.x <= 1.1 && clonePos.y >= -1.1 && clonePos.y <= 1.1;

          projectedLabels.push({
            id: item.id,
            name: item.name,
            category: item.category,
            color: item.color,
            x: item.pos.x,
            z: item.pos.z,
            screenX: clonePos.x * halfW + halfW,
            screenY: -(clonePos.y * halfH) + halfH,
            visible: isVisible,
          });
        }
        onLabelsUpdate(projectedLabels);
      }
    };
    animate();

    const handleResize = () => {
      if (!mountRef.current) return;
      const w = mountRef.current.clientWidth;
      const h = mountRef.current.clientHeight;
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h);
    };
    window.addEventListener('resize', handleResize);

    return () => {
      cancelAnimationFrame(animId);
      dom.removeEventListener('mousedown', onMouseDown);
      dom.removeEventListener('contextmenu', onContextMenu);
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
      dom.removeEventListener('wheel', onWheel);
      window.removeEventListener('resize', handleResize);
      renderer.dispose();
      if (mountRef.current) {
        mountRef.current.innerHTML = '';
      }
    };
  }, [isAngleLocked]);

  function updateCamera() {
    if (!cameraRef.current) return;
    const { theta, phi, radius } = cameraAngleRef.current;
    const cx = panOffsetRef.current.x;
    const cz = panOffsetRef.current.z;

    cameraRef.current.position.x = cx + radius * Math.sin(phi) * Math.sin(theta);
    cameraRef.current.position.y = radius * Math.cos(phi);
    cameraRef.current.position.z = cz + radius * Math.sin(phi) * Math.cos(theta);
    cameraRef.current.lookAt(cx, 18, cz);
  }

  // Day & Night Solar lighting updates
  useEffect(() => {
    if (!sceneRef.current || !sunLightRef.current || !ambientLightRef.current || !hemiLightRef.current) return;

    const { sunAltitude, sunAzimuth, isNight, isGoldenHour } = solarInfo;
    const dist = 600;
    const sunY = Math.sin(sunAltitude) * dist;
    const sunHorizontalDist = Math.cos(sunAltitude) * dist;
    const sunX = Math.sin(sunAzimuth) * sunHorizontalDist;
    const sunZ = -Math.cos(sunAzimuth) * sunHorizontalDist;

    sunLightRef.current.position.set(sunX, Math.max(sunY, -100), sunZ);

    if (isNight) {
      sceneRef.current.background = new THREE.Color(0x040810);
      sceneRef.current.fog = new THREE.FogExp2(0x040810, 0.0018);

      sunLightRef.current.color.setHex(0x38bdf8);
      sunLightRef.current.intensity = 0.25;

      ambientLightRef.current.color.setHex(0x0f172a);
      ambientLightRef.current.intensity = 0.45;

      hemiLightRef.current.color.setHex(0x1e1b4b);
      hemiLightRef.current.groundColor.setHex(0x030712);

      if (starsRef.current) starsRef.current.visible = true;

      // Illuminated warm yellow/amber windows at night
      buildingMeshesRef.current.forEach((m) => {
        if (m.material instanceof THREE.MeshStandardMaterial) {
          m.material.color.setHex(0x182436);
        }
      });
      windowPanelsRef.current.forEach((w) => {
        if (w.material instanceof THREE.MeshBasicMaterial) {
          w.material.color.setHex(0xfbbf24);
          w.material.opacity = 0.95;
        }
      });
      if (streetLampsGroupRef.current) streetLampsGroupRef.current.visible = true;
    } else if (isGoldenHour) {
      sceneRef.current.background = new THREE.Color(0xfba262);
      sceneRef.current.fog = new THREE.FogExp2(0xfba262, 0.0012);

      sunLightRef.current.color.setHex(0xf97316);
      sunLightRef.current.intensity = 1.6;

      ambientLightRef.current.color.setHex(0xfef3c7);
      ambientLightRef.current.intensity = 0.6;

      hemiLightRef.current.color.setHex(0xfde047);
      hemiLightRef.current.groundColor.setHex(0x27272a);

      if (starsRef.current) starsRef.current.visible = false;
      if (streetLampsGroupRef.current) streetLampsGroupRef.current.visible = true;
    } else {
      sceneRef.current.background = new THREE.Color(0x7dd3fc);
      sceneRef.current.fog = new THREE.FogExp2(0xe0f2fe, 0.0008);

      sunLightRef.current.color.setHex(0xffffff);
      sunLightRef.current.intensity = 1.45;

      ambientLightRef.current.color.setHex(0xffffff);
      ambientLightRef.current.intensity = 0.7;

      hemiLightRef.current.color.setHex(0x38bdf8);
      hemiLightRef.current.groundColor.setHex(0x1e293b);

      if (starsRef.current) starsRef.current.visible = false;

      buildingMeshesRef.current.forEach((m) => {
        if (m.material instanceof THREE.MeshStandardMaterial) {
          m.material.color.setHex(0x2a3d58);
        }
      });
      windowPanelsRef.current.forEach((w) => {
        if (w.material instanceof THREE.MeshBasicMaterial) {
          w.material.color.setHex(0x60a5fa);
          w.material.opacity = 0.6;
        }
      });
      if (streetLampsGroupRef.current) streetLampsGroupRef.current.visible = false;
    }
  }, [solarInfo]);

  // Procedurally Build Multi-Floor Detailed 3D Buildings with Floor Slabs & Window Rows
  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene || !buildingsGeoJSON || !buildingsGeoJSON.features) return;

    // Clear old meshes
    buildingMeshesRef.current.forEach((m) => scene.remove(m));
    buildingMeshesRef.current = [];
    windowPanelsRef.current.forEach((w) => scene.remove(w));
    windowPanelsRef.current = [];
    labelAnchorsRef.current = [];

    const floorHeight = 4.2; // 4.2m per floor
    const slabThickness = 0.35; // Concrete floor separation slab

    buildingsGeoJSON.features.forEach((feature: any, idx: number) => {
      const geom = feature.geometry;
      if (!geom || geom.type !== 'Polygon') return;

      const coords = geom.coordinates[0];
      if (!coords || coords.length < 3) return;

      const name = feature.properties?.name || `Academic Block ${idx + 1}`;
      const floors = getBuildingFloors(name);
      const totalHeight = floors * floorHeight;

      // Exterior Shape
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

      // 1. Extrude Building Main Structure
      const extrudeSettings: THREE.ExtrudeGeometryOptions = {
        depth: totalHeight,
        bevelEnabled: true,
        bevelSegments: 2,
        steps: floors, // Segments per floor level
        bevelSize: 0.5,
        bevelThickness: 0.5,
      };

      const extrudeGeo = new THREE.ExtrudeGeometry(shape, extrudeSettings);
      extrudeGeo.rotateX(Math.PI / 2);

      const buildingMat = new THREE.MeshStandardMaterial({
        color: solarInfo.isNight ? 0x182436 : 0x2a3d58,
        roughness: 0.4,
        metalness: 0.3,
      });

      const buildingMesh = new THREE.Mesh(extrudeGeo, buildingMat);
      buildingMesh.castShadow = true;
      buildingMesh.receiveShadow = true;
      scene.add(buildingMesh);
      buildingMeshesRef.current.push(buildingMesh);

      // 2. Generate Horizontal Floor Slabs & Window Stripes per Floor
      const windowMat = new THREE.MeshBasicMaterial({
        color: solarInfo.isNight ? 0xfbbf24 : 0x60a5fa,
        transparent: true,
        opacity: solarInfo.isNight ? 0.95 : 0.6,
      });

      const slabMat = new THREE.MeshStandardMaterial({
        color: 0x334155, // Dark concrete slab divider
        roughness: 0.8,
        metalness: 0.1,
      });

      for (let f = 1; f <= floors; f++) {
        const floorY = f * floorHeight;

        // Concrete floor slab ribbon outline
        const slabPoints: THREE.Vector3[] = [];
        for (let i = 0; i < coords.length; i++) {
          const [lng, lat] = coords[i];
          const local = latLngToLocal(lat, lng);
          slabPoints.push(new THREE.Vector3(local.x, floorY, local.z));
        }
        slabPoints.push(slabPoints[0]);

        const slabLineGeo = new THREE.BufferGeometry().setFromPoints(slabPoints);
        const slabLine = new THREE.Line(slabLineGeo, new THREE.LineBasicMaterial({ color: 0x0ea5e9, linewidth: 2 }));
        scene.add(slabLine);

        // Window stripes placed along facades
        for (let i = 0; i < coords.length - 1; i++) {
          const [lng1, lat1] = coords[i];
          const [lng2, lat2] = coords[i + 1];
          const p1 = latLngToLocal(lat1, lng1);
          const p2 = latLngToLocal(lat2, lng2);

          const edgeVec = new THREE.Vector3(p2.x - p1.x, 0, p2.z - p1.z);
          const edgeLength = edgeVec.length();
          if (edgeLength < 8) continue;

          const windowW = edgeLength * 0.75;
          const windowH = 1.6;
          const winGeo = new THREE.PlaneGeometry(windowW, windowH);
          const winMesh = new THREE.Mesh(winGeo, windowMat);

          const midX = (p1.x + p2.x) / 2;
          const midZ = (p1.z + p2.z) / 2;
          winMesh.position.set(midX, floorY - 2.0, midZ);

          const angle = Math.atan2(p2.z - p1.z, p2.x - p1.x);
          winMesh.rotation.y = -angle;

          scene.add(winMesh);
          windowPanelsRef.current.push(winMesh);
        }
      }

      // 3. Rooftop Parapet Border & HVAC Units
      const roofPoints: THREE.Vector3[] = [];
      for (let i = 0; i < coords.length; i++) {
        const [lng, lat] = coords[i];
        const local = latLngToLocal(lat, lng);
        roofPoints.push(new THREE.Vector3(local.x, totalHeight + 1.2, local.z));
      }
      roofPoints.push(roofPoints[0]);
      const roofLineGeo = new THREE.BufferGeometry().setFromPoints(roofPoints);
      const roofLine = new THREE.Line(roofLineGeo, new THREE.LineBasicMaterial({ color: 0x38bdf8, linewidth: 3 }));
      scene.add(roofLine);

      // Register 3D Label Anchor
      let category = 'Academic';
      let color = '#38bdf8';
      const lowerName = name.toLowerCase();

      if (lowerName.includes('hostel')) {
        category = 'Hostels';
        color = '#ec4899';
      } else if (lowerName.includes('sports') || lowerName.includes('ground')) {
        category = 'Sports';
        color = '#10b981';
      } else if (lowerName.includes('mess') || lowerName.includes('canteen')) {
        category = 'Food';
        color = '#f97316';
      }

      labelAnchorsRef.current.push({
        id: `label_${idx}`,
        name: `${name} (${floors}F)`,
        category,
        color,
        pos: new THREE.Vector3(centerX, totalHeight + 6, centerZ),
        feature,
      });
    });
  }, [buildingsGeoJSON, solarInfo.isNight]);

  // Roads, Pedestrian Walkways & Street Lamp Orbs
  useEffect(() => {
    const scene = sceneRef.current;
    const lampsGroup = streetLampsGroupRef.current;
    if (!scene || !lampsGroup) return;

    while (lampsGroup.children.length > 0) {
      lampsGroup.remove(lampsGroup.children[0]);
    }

    // Warm street lamp spheres along pedestrian paths
    if (pathsGeoJSON && pathsGeoJSON.features) {
      const lampSphereGeo = new THREE.SphereGeometry(1.5, 8, 8);
      const lampGlowMat = new THREE.MeshBasicMaterial({
        color: 0xfef08a,
        transparent: true,
        opacity: 0.95,
      });

      let count = 0;
      for (const feat of pathsGeoJSON.features) {
        const coords = feat.geometry?.coordinates;
        if (!coords) continue;

        for (let i = 0; i < coords.length; i += 3) {
          const [lng, lat] = coords[i];
          const local = latLngToLocal(lat, lng);
          const lampMesh = new THREE.Mesh(lampSphereGeo, lampGlowMat);
          lampMesh.position.set(local.x, 3.4, local.z);
          lampsGroup.add(lampMesh);
          count++;
          if (count > 250) break;
        }
        if (count > 250) break;
      }
    }

    // Pedestrian Paths Ribbons
    let pathsLine: THREE.LineSegments | null = null;
    if (pathsGeoJSON && pathsGeoJSON.features) {
      const pathPoints: THREE.Vector3[] = [];
      for (const feat of pathsGeoJSON.features) {
        const coords = feat.geometry?.coordinates;
        if (!coords) continue;
        for (let i = 0; i < coords.length - 1; i++) {
          const [lng1, lat1] = coords[i];
          const [lng2, lat2] = coords[i + 1];
          const p1 = latLngToLocal(lat1, lng1);
          const p2 = latLngToLocal(lat2, lng2);
          pathPoints.push(new THREE.Vector3(p1.x, 0.4, p1.z));
          pathPoints.push(new THREE.Vector3(p2.x, 0.4, p2.z));
        }
      }
      const pGeo = new THREE.BufferGeometry().setFromPoints(pathPoints);
      const pMat = new THREE.LineBasicMaterial({ color: 0x334155, linewidth: 2 });
      pathsLine = new THREE.LineSegments(pGeo, pMat);
      scene.add(pathsLine);
    }

    return () => {
      if (pathsLine && scene) scene.remove(pathsLine);
    };
  }, [pathsGeoJSON]);

  // Sports Complex in 3D (Running Track & Sports fields)
  useEffect(() => {
    const group = sportsGroupRef.current;
    if (!group || !sportsGeoJSON || !sportsGeoJSON.features) return;

    while (group.children.length > 0) {
      group.remove(group.children[0]);
    }

    for (const feat of sportsGeoJSON.features) {
      const coords = feat.geometry?.coordinates;
      if (!coords || !coords[0]) continue;
      const ring = coords[0];

      const shape = new THREE.Shape();
      for (let i = 0; i < ring.length; i++) {
        const [lng, lat] = ring[i];
        const local = latLngToLocal(lat, lng);
        if (i === 0) shape.moveTo(local.x, local.z);
        else shape.lineTo(local.x, local.z);
      }

      const geo = new THREE.ShapeGeometry(shape);
      geo.rotateX(-Math.PI / 2);

      const isTrack = feat.properties?.sport === 'running' || (feat.properties?.leisure === 'pitch');
      const mat = new THREE.MeshStandardMaterial({
        color: isTrack ? 0x991b1b : 0x0284c7, // Crimson athletics track or cyan court
        roughness: 0.9,
      });

      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.y = 0.2;
      mesh.receiveShadow = true;
      group.add(mesh);
    }
  }, [sportsGeoJSON]);

  // Active navigation route ribbon in 3D
  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene) return;

    let routeLine: THREE.Line | null = null;
    if (activeRoute && activeRoute.coordinates.length > 1) {
      const points: THREE.Vector3[] = [];
      for (const [lat, lng] of activeRoute.coordinates) {
        const local = latLngToLocal(lat, lng);
        points.push(new THREE.Vector3(local.x, 1.2, local.z));
      }
      const lineGeo = new THREE.BufferGeometry().setFromPoints(points);
      const lineMat = new THREE.LineBasicMaterial({ color: 0x38bdf8, linewidth: 6 });
      routeLine = new THREE.Line(lineGeo, lineMat);
      scene.add(routeLine);
    }

    return () => {
      if (routeLine && scene) scene.remove(routeLine);
    };
  }, [activeRoute]);

  return (
    <div className="w-full h-full relative overflow-hidden select-none">
      <div ref={mountRef} className="w-full h-full cursor-grab active:cursor-grabbing" />
    </div>
  );
};
