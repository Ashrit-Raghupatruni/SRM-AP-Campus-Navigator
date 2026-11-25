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
  const windowMeshesRef = useRef<THREE.InstancedMesh | null>(null);
  const streetLampsGroupRef = useRef<THREE.Group | null>(null);
  const treesGroupRef = useRef<THREE.Group | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null);

  // Label anchors in 3D world space
  const labelAnchorsRef = useRef<{ id: string; name: string; category: string; color: string; pos: THREE.Vector3; feature: any }[]>([]);

  // Orbit camera control state
  const isDraggingRef = useRef(false);
  const previousMousePositionRef = useRef({ x: 0, y: 0 });
  // Initial isometric aerial view matching IITK 3D
  const cameraAngleRef = useRef({ theta: Math.PI / 4, phi: Math.PI / 3.4, radius: 280 });
  const panOffsetRef = useRef({ x: 0, z: 0 });

  useEffect(() => {
    if (!mountRef.current) return;

    // Scene
    const scene = new THREE.Scene();
    sceneRef.current = scene;

    const width = mountRef.current.clientWidth;
    const height = mountRef.current.clientHeight;

    // Camera
    const camera = new THREE.PerspectiveCamera(40, width / height, 1, 3000);
    cameraRef.current = camera;
    updateCamera();

    // Renderer with soft shadows
    const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.05;
    mountRef.current.appendChild(renderer.domElement);
    rendererRef.current = renderer;

    // Dynamic Atmosphere & Lights
    const hemiLight = new THREE.HemisphereLight(0xffffff, 0x182416, 0.5);
    hemiLight.position.set(0, 500, 0);
    scene.add(hemiLight);
    hemiLightRef.current = hemiLight;

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.25);
    scene.add(ambientLight);
    ambientLightRef.current = ambientLight;

    const sunLight = new THREE.DirectionalLight(0xffffff, 1.3);
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
    sunLight.shadow.bias = -0.0005;
    scene.add(sunLight);
    sunLightRef.current = sunLight;

    // Campus Dark Green Turf Ground Mesh (1.8km x 1.8km) like IITK 3D
    const groundGeo = new THREE.PlaneGeometry(1800, 1800);
    const groundMat = new THREE.MeshStandardMaterial({
      color: 0x142817, // Rich dark campus lawn grass
      roughness: 0.95,
      metalness: 0.05,
    });
    const ground = new THREE.Mesh(groundGeo, groundMat);
    ground.rotation.x = -Math.PI / 2;
    ground.receiveShadow = true;
    scene.add(ground);

    // Groups for street lamps and trees
    const streetLampsGroup = new THREE.Group();
    scene.add(streetLampsGroup);
    streetLampsGroupRef.current = streetLampsGroup;

    const treesGroup = new THREE.Group();
    scene.add(treesGroup);
    treesGroupRef.current = treesGroup;

    // Night Stars
    const starCount = 1000;
    const starGeo = new THREE.BufferGeometry();
    const starPositions = new Float32Array(starCount * 3);
    for (let i = 0; i < starCount * 3; i += 3) {
      starPositions[i] = (Math.random() - 0.5) * 2000;
      starPositions[i + 1] = Math.random() * 600 + 100;
      starPositions[i + 2] = (Math.random() - 0.5) * 2000;
    }
    starGeo.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
    const starMat = new THREE.PointsMaterial({ color: 0xffffff, size: 2.2, transparent: true, opacity: 0.75 });
    const stars = new THREE.Points(starGeo, starMat);
    scene.add(stars);
    starsRef.current = stars;

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
        // Pan
        const factor = 0.4;
        const forward = new THREE.Vector3(-Math.sin(cameraAngleRef.current.theta), 0, -Math.cos(cameraAngleRef.current.theta));
        const right = new THREE.Vector3(Math.cos(cameraAngleRef.current.theta), 0, -Math.sin(cameraAngleRef.current.theta));
        panOffsetRef.current.x += (-right.x * deltaX + forward.x * deltaY) * factor;
        panOffsetRef.current.z += (-right.z * deltaX + forward.z * deltaY) * factor;
      } else if (!isAngleLocked) {
        // Rotate
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
      cameraAngleRef.current.radius = Math.max(70, Math.min(500, cameraAngleRef.current.radius + e.deltaY * 0.3));
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

      // Project 3D building coordinates to 2D screen pills
      if (onLabelsUpdate && mountRef.current) {
        const w = mountRef.current.clientWidth;
        const h = mountRef.current.clientHeight;
        const halfW = w / 2;
        const halfH = h / 2;

        const projectedLabels: CampusLabel[] = [];
        for (const item of labelAnchorsRef.current) {
          const clonePos = item.pos.clone();
          clonePos.project(camera);

          // Visible if within frustum and in front of camera
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
    cameraRef.current.lookAt(cx, 15, cz);
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
      sunLightRef.current.intensity = 0.2;

      ambientLightRef.current.color.setHex(0x0f172a);
      ambientLightRef.current.intensity = 0.45;

      hemiLightRef.current.color.setHex(0x1e1b4b);
      hemiLightRef.current.groundColor.setHex(0x030712);

      if (starsRef.current) starsRef.current.visible = true;

      // Illuminated warm yellow/amber windows at night
      buildingMeshesRef.current.forEach((m) => {
        if (m.material instanceof THREE.MeshStandardMaterial) {
          m.material.color.setHex(0x1a2333);
          m.material.emissive.setHex(0xfbbf24);
          m.material.emissiveIntensity = 0.22;
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
          m.material.color.setHex(0xe2e8f0);
          m.material.emissive.setHex(0x000000);
          m.material.emissiveIntensity = 0;
        }
      });
      if (streetLampsGroupRef.current) streetLampsGroupRef.current.visible = false;
    }
  }, [solarInfo]);

  // Generate 3D Buildings with Dark Slate & Rooftop details like IITK
  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene || !buildingsGeoJSON || !buildingsGeoJSON.features) return;

    buildingMeshesRef.current.forEach((mesh) => scene.remove(mesh));
    buildingMeshesRef.current = [];
    labelAnchorsRef.current = [];

    buildingsGeoJSON.features.forEach((feature: any, idx: number) => {
      const geom = feature.geometry;
      if (!geom || geom.type !== 'Polygon') return;

      const coords = geom.coordinates[0];
      if (!coords || coords.length < 3) return;

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

      const levels = parseInt(feature.properties?.levels || '4', 10);
      const height = Math.max(12, levels * 4.5);

      const extrudeSettings: THREE.ExtrudeGeometryOptions = {
        depth: height,
        bevelEnabled: true,
        bevelSegments: 2,
        steps: 1,
        bevelSize: 0.6,
        bevelThickness: 0.6,
      };

      const extrudeGeo = new THREE.ExtrudeGeometry(shape, extrudeSettings);
      extrudeGeo.rotateX(Math.PI / 2);

      const buildingMat = new THREE.MeshStandardMaterial({
        color: solarInfo.isNight ? 0x1a2333 : 0x223249, // Dark corporate slate matching IITK 3D
        roughness: 0.45,
        metalness: 0.35,
      });

      const mesh = new THREE.Mesh(extrudeGeo, buildingMat);
      mesh.castShadow = true;
      mesh.receiveShadow = true;
      scene.add(mesh);
      buildingMeshesRef.current.push(mesh);

      // Register label anchor
      const name = feature.properties?.name || `Block ${idx + 1}`;
      let category = 'Academic';
      let color = '#38bdf8';

      const lowerName = name.toLowerCase();
      if (lowerName.includes('hostel')) {
        category = 'Hostels';
        color = '#ec4899';
      } else if (lowerName.includes('sports') || lowerName.includes('ground')) {
        category = 'Sports';
        color = '#10b981';
      } else if (lowerName.includes('canteen') || lowerName.includes('dining')) {
        category = 'Food';
        color = '#f59e0b';
      }

      labelAnchorsRef.current.push({
        id: `label_${idx}`,
        name,
        category,
        color,
        pos: new THREE.Vector3(centerX, height + 4, centerZ),
        feature,
      });
    });
  }, [buildingsGeoJSON, solarInfo.isNight]);

  // Roads, Paths & Street Light Glow Orbs (warm glowing spheres along pathways like IITK)
  useEffect(() => {
    const scene = sceneRef.current;
    const lampsGroup = streetLampsGroupRef.current;
    if (!scene || !lampsGroup) return;

    // Clear old lamps
    while (lampsGroup.children.length > 0) {
      lampsGroup.remove(lampsGroup.children[0]);
    }

    // Place warm glowing street lamp spheres along pedestrian paths
    if (pathsGeoJSON && pathsGeoJSON.features) {
      const lampSphereGeo = new THREE.SphereGeometry(1.4, 8, 8);
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
          lampMesh.position.set(local.x, 3.2, local.z);
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
      const lineMat = new THREE.LineBasicMaterial({ color: 0x38bdf8, linewidth: 5 });
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
