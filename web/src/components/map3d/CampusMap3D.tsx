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

// Verified real-world floor counts for SRM University-AP buildings (from srmap_blocks.db)
export const BUILDING_FLOOR_COUNTS: Record<string, number> = {
  'ganga': 26,                    // 26-storey iconic landmark high-rise residence for male students
  'vedavathi': 15,                // 15-storey modern mid-rise boys' residence
  'cv raman block': 12,           // 12-storey landmark tower (Central Library F12)
  'krishna hostel': 9,            // 9-storey girls' residence tower
  'godavari hostel': 9,           // 9-storey girls' residence tower
  'narmada hostel': 9,            // 9-storey girls' residence tower
  'yamuna hostel': 9,             // 9-storey girls' residence tower
  'kaveri faculty residence': 9,  // 9-storey faculty apartment residence
  'sr block': 9,                  // SR Block across 9 stories
  'x-lab': 7,                     // X-Lab multidisciplinary engineering complex across 7 levels
  'homi j bhabha block': 7,       // Administrative & Advanced Science Block across 7 levels
  'homi j. bhabha block': 7,
  'v block': 7,                   // Vikram Sarabhai Academic Block I across 7 levels
  'jc bose block': 5,             // 5-storey research and laboratory facility
  'annapurna mess': 2,            // 2-storey central dining facility
  'office': 2,
};

export function getBuildingFloors(name: string): number {
  const lower = (name || '').toLowerCase().trim();
  for (const [key, floors] of Object.entries(BUILDING_FLOOR_COUNTS)) {
    if (lower.includes(key) || key.includes(lower)) {
      return floors;
    }
  }
  return 4;
}

// Authentic real-world architectural colors sampled directly from Android app specifications
export function getBuildingPalette(name: string) {
  const lower = (name || '').toLowerCase();
  if (lower.includes('ganga') || lower.includes('vedavathi')) {
    return {
      wallColor: 0x966a55,      // Warm terracotta ochre panels
      accentColor: 0x7c5543,    // Dark terracotta window surround fins
      slabColor: 0xf8fafc,      // Crisp white cantilevered floor slab eyebrows
      roofColor: 0xcbd5e1,      // Rooftop terrace gravel
      windowColor: 0x334155,    // Residential window frames
      nightWindow: 0xfbbf24,
    };
  }
  if (lower.includes('cv raman')) {
    return {
      wallColor: 0xf1f5f9,      // Precast architectural concrete white
      accentColor: 0x475569,    // Dark concrete atrium frame
      slabColor: 0x38bdf8,      // Sky-blue vertical glass curtain spines
      roofColor: 0x94a3b8,      // Mechanical penthouse gravel
      windowColor: 0x1e293b,    // Solar glass
      nightWindow: 0xfde047,    // Warm golden atrium glow
    };
  }
  if (lower.includes('sr block') || lower.includes('radhakrishnan')) {
    return {
      wallColor: 0xd3cbbe,      // Warm precast sandstone vertical core towers
      accentColor: 0x1e293b,    // Dark charcoal horizontal spandrel bands
      slabColor: 0xe2e8f0,      // Perkins+Will white slab eyebrows
      roofColor: 0x94a3b8,      // Waterproof roof deck
      windowColor: 0x3b5971,    // Wrap-around ribbon solar glazing
      nightWindow: 0xfbbf24,
    };
  }
  if (lower.includes('hostel') || lower.includes('kaveri')) {
    return {
      wallColor: 0x8c624e,      // Terracotta residential tone
      accentColor: 0x5c4033,    // Darker accent
      slabColor: 0xf8fafc,      // White slab eyebrows
      roofColor: 0xcbd5e1,
      windowColor: 0x475569,
      nightWindow: 0xfbbf24,
    };
  }
  if (lower.includes('annapurna') || lower.includes('mess')) {
    return {
      wallColor: 0xfef3c7,      // Warm dining hall tone
      accentColor: 0xd97706,
      slabColor: 0xfef9c3,
      roofColor: 0x15803d,      // Stepped green pavilion roof
      windowColor: 0x1e293b,
      nightWindow: 0xf59e0b,
    };
  }
  // V Block, X-Lab, JC Bose, Academic default
  return {
    wallColor: 0xe2e8f0,        // Precast sandstone off-white
    accentColor: 0xcbd5e1,      // Floor spandrel band
    slabColor: 0xffffff,        // White slab ribbons
    roofColor: 0x94a3b8,        // Parapet roof surface
    windowColor: 0x334155,      // Charcoal window frame & glass
    nightWindow: 0x38bdf8,
  };
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
  isWalkMode: boolean;
  walkPov: '1st' | '3rd';
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
  isWalkMode,
  walkPov,
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
  const windowPanelsRef = useRef<{ mesh: THREE.Mesh; nightColor: number; dayColor: number }[]>([]);
  const streetLampsGroupRef = useRef<THREE.Group | null>(null);
  const sportsGroupRef = useRef<THREE.Group | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null);
  const raycasterRef = useRef(new THREE.Raycaster());
  const mousePosRef = useRef(new THREE.Vector2());

  // Label anchors in 3D world space
  const labelAnchorsRef = useRef<{ id: string; name: string; category: string; color: string; pos: THREE.Vector3; feature: any }[]>([]);

  // Orbit camera control state (Aerial Overview)
  const isDraggingRef = useRef(false);
  const previousMousePositionRef = useRef({ x: 0, y: 0 });
  const aerialAngleRef = useRef({ theta: Math.PI / 3.8, phi: Math.PI / 3.4, radius: 260 });
  const panOffsetRef = useRef({ x: 0, z: 0 });

  // Walk Mode camera states
  // Campus central walkway origin (near Central Plaza / SR Block entrance)
  const walkPosRef = useRef(new THREE.Vector3(25.0, 1.8, 80.0));
  const walkHeadingRef = useRef(-Math.PI / 2); // Looking northwards along breezeway
  const walkPitchRef = useRef(0.0);

  // Current animated camera position and target
  const currentCamPosRef = useRef(new THREE.Vector3(150, 180, 250));
  const currentCamLookRef = useRef(new THREE.Vector3(0, 15, 0));

  useEffect(() => {
    if (!mountRef.current) return;

    const scene = new THREE.Scene();
    sceneRef.current = scene;

    const width = mountRef.current.clientWidth;
    const height = mountRef.current.clientHeight;

    const camera = new THREE.PerspectiveCamera(42, width / height, 0.5, 3500);
    cameraRef.current = camera;

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
    const hemiLight = new THREE.HemisphereLight(0xffffff, 0x142012, 0.65);
    hemiLight.position.set(0, 500, 0);
    scene.add(hemiLight);
    hemiLightRef.current = hemiLight;

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.35);
    scene.add(ambientLight);
    ambientLightRef.current = ambientLight;

    const sunLight = new THREE.DirectionalLight(0xffffff, 1.5);
    sunLight.castShadow = true;
    sunLight.shadow.mapSize.width = 2048;
    sunLight.shadow.mapSize.height = 2048;
    sunLight.shadow.camera.near = 10;
    sunLight.shadow.camera.far = 1400;
    const d = 450;
    sunLight.shadow.camera.left = -d;
    sunLight.shadow.camera.right = d;
    sunLight.shadow.camera.top = d;
    sunLight.shadow.camera.bottom = -d;
    sunLight.shadow.bias = -0.0004;
    scene.add(sunLight);
    sunLightRef.current = sunLight;

    // Solid Non-Transparent Dark Campus Turf Ground (2.2km x 2.2km)
    const groundGeo = new THREE.PlaneGeometry(2200, 2200);
    const groundMat = new THREE.MeshStandardMaterial({
      color: 0x142818, // Rich opaque campus turf
      roughness: 0.95,
      metalness: 0.05,
    });
    const ground = new THREE.Mesh(groundGeo, groundMat);
    ground.rotation.x = -Math.PI / 2;
    ground.receiveShadow = true;
    scene.add(ground);

    // Groups for Street Lamps and Sports
    const streetLampsGroup = new THREE.Group();
    scene.add(streetLampsGroup);
    streetLampsGroupRef.current = streetLampsGroup;

    const sportsGroup = new THREE.Group();
    scene.add(sportsGroup);
    sportsGroupRef.current = sportsGroup;

    // Stars Field
    const starCount = 1200;
    const starGeo = new THREE.BufferGeometry();
    const starPositions = new Float32Array(starCount * 3);
    for (let i = 0; i < starCount * 3; i += 3) {
      starPositions[i] = (Math.random() - 0.5) * 2200;
      starPositions[i + 1] = Math.random() * 700 + 100;
      starPositions[i + 2] = (Math.random() - 0.5) * 2200;
    }
    starGeo.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
    const starMat = new THREE.PointsMaterial({ color: 0xffffff, size: 2.2, transparent: true, opacity: 0.8 });
    const stars = new THREE.Points(starGeo, starMat);
    scene.add(stars);
    starsRef.current = stars;

    // Mouse handlers for Orbit, Pan & 3D Click
    const dom = renderer.domElement;
    let isRightClick = false;
    let clickStartX = 0;
    let clickStartY = 0;

    const onMouseDown = (e: MouseEvent) => {
      isDraggingRef.current = true;
      isRightClick = e.button === 2;
      clickStartX = e.clientX;
      clickStartY = e.clientY;
      previousMousePositionRef.current = { x: e.clientX, y: e.clientY };
    };

    const onContextMenu = (e: MouseEvent) => e.preventDefault();

    const onMouseMove = (e: MouseEvent) => {
      if (!isDraggingRef.current) return;
      const deltaX = e.clientX - previousMousePositionRef.current.x;
      const deltaY = e.clientY - previousMousePositionRef.current.y;

      if (isWalkMode) {
        // First/Third Person Look Controls
        walkHeadingRef.current -= deltaX * 0.005;
        walkPitchRef.current = Math.max(-0.6, Math.min(0.6, walkPitchRef.current - deltaY * 0.005));
      } else if (isRightClick) {
        // Pan
        const factor = 0.45;
        const forward = new THREE.Vector3(-Math.sin(aerialAngleRef.current.theta), 0, -Math.cos(aerialAngleRef.current.theta));
        const right = new THREE.Vector3(Math.cos(aerialAngleRef.current.theta), 0, -Math.sin(aerialAngleRef.current.theta));
        panOffsetRef.current.x += (-right.x * deltaX + forward.x * deltaY) * factor;
        panOffsetRef.current.z += (-right.z * deltaX + forward.z * deltaY) * factor;
      } else if (!isAngleLocked) {
        // Aerial Orbit Rotation
        aerialAngleRef.current.theta -= deltaX * 0.006;
        aerialAngleRef.current.phi = Math.max(0.15, Math.min(Math.PI / 2.05, aerialAngleRef.current.phi + deltaY * 0.006));
      }

      previousMousePositionRef.current = { x: e.clientX, y: e.clientY };
    };

    const onMouseUp = (e: MouseEvent) => {
      isDraggingRef.current = false;
      // Detect tap/click without drag for 3D building selection
      const dragDist = Math.hypot(e.clientX - clickStartX, e.clientY - clickStartY);
      if (dragDist < 5 && cameraRef.current && mountRef.current) {
        const rect = mountRef.current.getBoundingClientRect();
        mousePosRef.current.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
        mousePosRef.current.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;

        raycasterRef.current.setFromCamera(mousePosRef.current, cameraRef.current);
        const intersects = raycasterRef.current.intersectObjects(buildingMeshesRef.current);

        if (intersects.length > 0) {
          const hitMesh = intersects[0].object as THREE.Mesh;
          const buildingData = hitMesh.userData;
          if (buildingData && buildingData.name) {
            onSelectBuilding(buildingData.name, buildingData.feature);
          }
        }
      }
    };

    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      if (!isWalkMode) {
        // Extended zoom limits: from close 40m up to 1000m for full birds-eye perspective
        aerialAngleRef.current.radius = Math.max(40, Math.min(1000, aerialAngleRef.current.radius + e.deltaY * 0.45));
      }
    };

    dom.addEventListener('mousedown', onMouseDown);
    dom.addEventListener('contextmenu', onContextMenu);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    dom.addEventListener('wheel', onWheel);

    // Keyboard controls in Walk mode (WASD / Arrows)
    const onKeyDown = (e: KeyboardEvent) => {
      if (!isWalkMode) return;
      const step = 3.5;
      const forward = new THREE.Vector3(Math.cos(walkHeadingRef.current), 0, Math.sin(walkHeadingRef.current));
      const right = new THREE.Vector3(-Math.sin(walkHeadingRef.current), 0, Math.cos(walkHeadingRef.current));

      if (e.key === 'w' || e.key === 'ArrowUp') {
        walkPosRef.current.addScaledVector(forward, step);
      } else if (e.key === 's' || e.key === 'ArrowDown') {
        walkPosRef.current.addScaledVector(forward, -step);
      } else if (e.key === 'a' || e.key === 'ArrowLeft') {
        walkPosRef.current.addScaledVector(right, -step);
      } else if (e.key === 'd' || e.key === 'ArrowRight') {
        walkPosRef.current.addScaledVector(right, step);
      }
    };
    window.addEventListener('keydown', onKeyDown);

    // Animation & Smooth Camera Interpolation Loop
    let animId: number;
    const animate = () => {
      animId = requestAnimationFrame(animate);

      // Compute Target Camera Position & LookAt
      const targetCamPos = new THREE.Vector3();
      const targetCamLook = new THREE.Vector3();

      if (isWalkMode) {
        if (walkPov === '1st') {
          // 1st Person POV: Eye level at 1.8m looking forward
          targetCamPos.copy(walkPosRef.current);
          targetCamPos.y = 1.8;
          targetCamLook.x = targetCamPos.x + Math.cos(walkHeadingRef.current) * 30;
          targetCamLook.y = targetCamPos.y + Math.sin(walkPitchRef.current) * 30;
          targetCamLook.z = targetCamPos.z + Math.sin(walkHeadingRef.current) * 30;
        } else {
          // 3rd Person POV: Elevated chase camera slightly behind avatar
          const chaseDist = 8.0;
          const chaseHeight = 4.0;
          targetCamPos.x = walkPosRef.current.x - Math.cos(walkHeadingRef.current) * chaseDist;
          targetCamPos.y = walkPosRef.current.y + chaseHeight;
          targetCamPos.z = walkPosRef.current.z - Math.sin(walkHeadingRef.current) * chaseDist;

          targetCamLook.copy(walkPosRef.current);
          targetCamLook.y += 1.8;
        }
      } else {
        // Aerial Orbit Mode
        const { theta, phi, radius } = aerialAngleRef.current;
        const cx = panOffsetRef.current.x;
        const cz = panOffsetRef.current.z;

        targetCamPos.x = cx + radius * Math.sin(phi) * Math.sin(theta);
        targetCamPos.y = radius * Math.cos(phi);
        targetCamPos.z = cz + radius * Math.sin(phi) * Math.cos(theta);

        targetCamLook.set(cx, 18, cz);
      }

      // Smooth Lerp Transition between camera states (0.08 interpolation factor)
      currentCamPosRef.current.lerp(targetCamPos, 0.08);
      currentCamLookRef.current.lerp(targetCamLook, 0.08);

      if (cameraRef.current) {
        cameraRef.current.position.copy(currentCamPosRef.current);
        cameraRef.current.lookAt(currentCamLookRef.current);
      }

      renderer.render(scene, camera);

      // Project 3D building coordinates to screen label pills
      if (onLabelsUpdate && mountRef.current && !isWalkMode) {
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
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('resize', handleResize);
      renderer.dispose();
      if (mountRef.current) {
        mountRef.current.innerHTML = '';
      }
    };
  }, [isWalkMode, walkPov, isAngleLocked]);

  // Day & Night Solar lighting updates
  useEffect(() => {
    if (!sceneRef.current || !sunLightRef.current || !ambientLightRef.current || !hemiLightRef.current) return;

    const { sunAltitude, sunAzimuth, isNight, isGoldenHour } = solarInfo;
    const dist = 700;
    const sunY = Math.sin(sunAltitude) * dist;
    const sunHorizontalDist = Math.cos(sunAltitude) * dist;
    const sunX = Math.sin(sunAzimuth) * sunHorizontalDist;
    const sunZ = -Math.cos(sunAzimuth) * sunHorizontalDist;

    sunLightRef.current.position.set(sunX, Math.max(sunY, -100), sunZ);

    if (isNight) {
      sceneRef.current.background = new THREE.Color(0x040810);
      sceneRef.current.fog = new THREE.FogExp2(0x040810, 0.0016);

      sunLightRef.current.color.setHex(0x38bdf8);
      sunLightRef.current.intensity = 0.25;

      ambientLightRef.current.color.setHex(0x0f172a);
      ambientLightRef.current.intensity = 0.45;

      hemiLightRef.current.color.setHex(0x1e1b4b);
      hemiLightRef.current.groundColor.setHex(0x030712);

      if (starsRef.current) starsRef.current.visible = true;

      // Illuminated windows at night
      windowPanelsRef.current.forEach(({ mesh, nightColor }) => {
        if (mesh.material instanceof THREE.MeshBasicMaterial) {
          mesh.material.color.setHex(nightColor);
          mesh.material.opacity = 0.95;
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
      sceneRef.current.fog = new THREE.FogExp2(0xe0f2fe, 0.0007);

      sunLightRef.current.color.setHex(0xffffff);
      sunLightRef.current.intensity = 1.45;

      ambientLightRef.current.color.setHex(0xffffff);
      ambientLightRef.current.intensity = 0.7;

      hemiLightRef.current.color.setHex(0x38bdf8);
      hemiLightRef.current.groundColor.setHex(0x1e293b);

      if (starsRef.current) starsRef.current.visible = false;

      windowPanelsRef.current.forEach(({ mesh, dayColor }) => {
        if (mesh.material instanceof THREE.MeshBasicMaterial) {
          mesh.material.color.setHex(dayColor);
          mesh.material.opacity = 0.65;
        }
      });
      if (streetLampsGroupRef.current) streetLampsGroupRef.current.visible = false;
    }
  }, [solarInfo]);

  // Procedurally Build Solid, Opaque Multi-Material Detailed 3D Buildings
  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene || !buildingsGeoJSON || !buildingsGeoJSON.features) return;

    // Clear old meshes
    buildingMeshesRef.current.forEach((m) => scene.remove(m));
    buildingMeshesRef.current = [];
    windowPanelsRef.current.forEach((w) => scene.remove(w.mesh));
    windowPanelsRef.current = [];
    labelAnchorsRef.current = [];

    const floorHeight = 4.0; // 4.0m per floor

    buildingsGeoJSON.features.forEach((feature: any, idx: number) => {
      const geom = feature.geometry;
      if (!geom || geom.type !== 'Polygon') return;

      const coords = geom.coordinates[0];
      if (!coords || coords.length < 3) return;

      const name = feature.properties?.name || `Academic Block ${idx + 1}`;
      const floors = getBuildingFloors(name);
      const totalHeight = floors * floorHeight;
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

      // 1. SOLID OPAQUE BUILDING MESH
      const extrudeSettings: THREE.ExtrudeGeometryOptions = {
        depth: totalHeight,
        bevelEnabled: true,
        bevelSegments: 2,
        steps: floors,
        bevelSize: 0.6,
        bevelThickness: 0.6,
      };

      const extrudeGeo = new THREE.ExtrudeGeometry(shape, extrudeSettings);
      extrudeGeo.rotateX(Math.PI / 2);

      // Solid, rich architectural material (completely opaque, no wireframe)
      const buildingMat = new THREE.MeshStandardMaterial({
        color: palette.wallColor,
        roughness: 0.45,
        metalness: 0.15,
      });

      const buildingMesh = new THREE.Mesh(extrudeGeo, buildingMat);
      buildingMesh.castShadow = true;
      buildingMesh.receiveShadow = true;
      buildingMesh.userData = { name, feature, floors };
      scene.add(buildingMesh);
      buildingMeshesRef.current.push(buildingMesh);

      // 2. HORIZONTAL FLOOR SEPARATION EYEBROWS (White / Accent Slabs per Floor)
      const slabMat = new THREE.MeshStandardMaterial({
        color: palette.slabColor,
        roughness: 0.35,
        metalness: 0.2,
      });

      for (let f = 1; f <= floors; f++) {
        const floorY = f * floorHeight;

        // Concrete eyebrow line wrapping each floor perimeter
        const slabPoints: THREE.Vector3[] = [];
        for (let i = 0; i < coords.length; i++) {
          const [lng, lat] = coords[i];
          const local = latLngToLocal(lat, lng);
          slabPoints.push(new THREE.Vector3(local.x, floorY, local.z));
        }
        slabPoints.push(slabPoints[0]);

        const slabLineGeo = new THREE.BufferGeometry().setFromPoints(slabPoints);
        const slabLine = new THREE.Line(
          slabLineGeo,
          new THREE.LineBasicMaterial({ color: palette.slabColor, linewidth: 2 })
        );
        scene.add(slabLine);

        // Window Panels along facades
        for (let i = 0; i < coords.length - 1; i++) {
          const [lng1, lat1] = coords[i];
          const [lng2, lat2] = coords[i + 1];
          const p1 = latLngToLocal(lat1, lng1);
          const p2 = latLngToLocal(lat2, lng2);

          const edgeVec = new THREE.Vector3(p2.x - p1.x, 0, p2.z - p1.z);
          const edgeLength = edgeVec.length();
          if (edgeLength < 7) continue;

          const windowW = edgeLength * 0.72;
          const windowH = 1.6;
          const winGeo = new THREE.PlaneGeometry(windowW, windowH);
          const winMat = new THREE.MeshBasicMaterial({
            color: solarInfo.isNight ? palette.nightWindow : palette.windowColor,
            transparent: true,
            opacity: solarInfo.isNight ? 0.95 : 0.65,
          });
          const winMesh = new THREE.Mesh(winGeo, winMat);

          const midX = (p1.x + p2.x) / 2;
          const midZ = (p1.z + p2.z) / 2;
          winMesh.position.set(midX, floorY - 1.9, midZ);

          const angle = Math.atan2(p2.z - p1.z, p2.x - p1.x);
          winMesh.rotation.y = -angle;

          scene.add(winMesh);
          windowPanelsRef.current.push({
            mesh: winMesh,
            nightColor: palette.nightWindow,
            dayColor: palette.windowColor,
          });
        }
      }

      // 3. ROOFTOP PARAPET & MECHANICAL PENTHOUSE
      const roofPoints: THREE.Vector3[] = [];
      for (let i = 0; i < coords.length; i++) {
        const [lng, lat] = coords[i];
        const local = latLngToLocal(lat, lng);
        roofPoints.push(new THREE.Vector3(local.x, totalHeight + 1.2, local.z));
      }
      roofPoints.push(roofPoints[0]);
      const roofLineGeo = new THREE.BufferGeometry().setFromPoints(roofPoints);
      const roofLine = new THREE.Line(
        roofLineGeo,
        new THREE.LineBasicMaterial({ color: palette.accentColor, linewidth: 3 })
      );
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

  // Sports Complex in 3D
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

      const isTrack = feat.properties?.sport === 'running' || feat.properties?.leisure === 'pitch';
      const mat = new THREE.MeshStandardMaterial({
        color: isTrack ? 0x991b1b : 0x0284c7,
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
