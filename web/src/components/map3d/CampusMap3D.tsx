import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';
import { latLngToLocal } from '../../core/geo';
import { ArchitecturalBuildingBuilder } from '../../core/geometry3d/ArchitecturalBuildingBuilder';
import { ProceduralRoadBuilder } from '../../core/geometry3d/ProceduralRoadBuilder';
import { SolarInfo, RouteResult, CampusLabel } from '../../types/campus';
export type { CampusLabel };

import { getBuildingFloors, getBuildingPalette, BUILDING_FLOOR_COUNTS } from '../../core/geometry3d/BuildingSpecs';
export { getBuildingFloors, getBuildingPalette, BUILDING_FLOOR_COUNTS };

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
  const groundMeshRef = useRef<THREE.Mesh | null>(null);
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

  // Track live mouse position over viewport for cursor-aware features (zoom, walk mode entry)
  const lastCursorWorldPosRef = useRef<THREE.Vector3 | null>(null);

  // Walk Mode camera states
  // Campus central walkway origin (near Central Plaza / SR Block entrance)
  const walkPosRef = useRef(new THREE.Vector3(25.0, 1.8, 80.0));
  const walkHeadingRef = useRef(-Math.PI / 2); // Looking northwards along breezeway
  const walkPitchRef = useRef(0.0);
  const avatarGroupRef = useRef<THREE.Group | null>(null);

  // Keep live refs for isWalkMode, walkPov, and isAngleLocked to avoid stale closures & scene teardown
  const isWalkModeRef = useRef(isWalkMode);
  isWalkModeRef.current = isWalkMode;
  const walkPovRef = useRef(walkPov);
  walkPovRef.current = walkPov;
  const isAngleLockedRef = useRef(isAngleLocked);
  isAngleLockedRef.current = isAngleLocked;

  // Cache road and pathway line segments for exact street-level snapping
  const roadSegmentsRef = useRef<{ a: THREE.Vector3; b: THREE.Vector3 }[]>([]);

  // Collect road and pathway segments whenever roadsGeoJSON or pathsGeoJSON update
  useEffect(() => {
    const segs: { a: THREE.Vector3; b: THREE.Vector3 }[] = [];
    const collectSegments = (geo: any) => {
      if (!geo || !geo.features) return;
      for (const feat of geo.features) {
        const coords = feat.geometry?.coordinates;
        if (!coords) continue;
        const addLine = (line: any[]) => {
          for (let i = 0; i < line.length - 1; i++) {
            const [lng1, lat1] = line[i];
            const [lng2, lat2] = line[i + 1];
            const p1 = latLngToLocal(lat1, lng1);
            const p2 = latLngToLocal(lat2, lng2);
            segs.push({
              a: new THREE.Vector3(p1.x, 0, p1.z),
              b: new THREE.Vector3(p2.x, 0, p2.z),
            });
          }
        };
        if (feat.geometry.type === 'LineString') {
          addLine(coords);
        } else if (feat.geometry.type === 'MultiLineString') {
          for (const line of coords) {
            addLine(line);
          }
        }
      }
    };
    collectSegments(roadsGeoJSON);
    collectSegments(pathsGeoJSON);
    roadSegmentsRef.current = segs;
  }, [roadsGeoJSON, pathsGeoJSON]);

  // Helper: find closest point on line segment AB to point P in XZ plane
  const closestPointOnSegmentXZ = (px: number, pz: number, a: THREE.Vector3, b: THREE.Vector3) => {
    const abx = b.x - a.x;
    const abz = b.z - a.z;
    const lenSq = abx * abx + abz * abz;
    if (lenSq < 0.0001) return { x: a.x, z: a.z, dist: Math.hypot(px - a.x, pz - a.z) };
    const t = Math.max(0, Math.min(1, ((px - a.x) * abx + (pz - a.z) * abz) / lenSq));
    const cx = a.x + t * abx;
    const cz = a.z + t * abz;
    return { x: cx, z: cz, dist: Math.hypot(px - cx, pz - cz) };
  };

  // When Walk Mode toggles ON: snap position directly to nearest road/walkway near cursor or view focus
  useEffect(() => {
    if (isWalkMode) {
      let refX = panOffsetRef.current.x;
      let refZ = panOffsetRef.current.z;

      // Use cursor location if it was hovering within campus view (< 160m from view center)
      if (lastCursorWorldPosRef.current) {
        const dFromCenter = Math.hypot(
          lastCursorWorldPosRef.current.x - panOffsetRef.current.x,
          lastCursorWorldPosRef.current.z - panOffsetRef.current.z
        );
        if (dFromCenter < 160) {
          refX = lastCursorWorldPosRef.current.x;
          refZ = lastCursorWorldPosRef.current.z;
        }
      }

      let targetX = refX;
      let targetZ = refZ;
      let roadHeading = Math.atan2(-targetZ, -targetX);

      // Always snap to the closest point on ANY road or pedestrian path segment
      const segs = roadSegmentsRef.current;
      if (segs && segs.length > 0) {
        let bestDist = Infinity;
        let bestPt = { x: targetX, z: targetZ };
        let bestSeg = segs[0];

        for (const seg of segs) {
          const res = closestPointOnSegmentXZ(refX, refZ, seg.a, seg.b);
          if (res.dist < bestDist) {
            bestDist = res.dist;
            bestPt = { x: res.x, z: res.z };
            bestSeg = seg;
          }
        }

        targetX = bestPt.x;
        targetZ = bestPt.z;

        // Orient camera straight down the road segment towards central campus
        const dx = bestSeg.b.x - bestSeg.a.x;
        const dz = bestSeg.b.z - bestSeg.a.z;
        const h1 = Math.atan2(dz, dx);
        const h2 = Math.atan2(-dz, -dx);
        const toCenterAngle = Math.atan2(-targetZ, -targetX);
        const diff1 = Math.abs(Math.atan2(Math.sin(h1 - toCenterAngle), Math.cos(h1 - toCenterAngle)));
        const diff2 = Math.abs(Math.atan2(Math.sin(h2 - toCenterAngle), Math.cos(h2 - toCenterAngle)));
        roadHeading = diff1 <= diff2 ? h1 : h2;
      }

      // Position pedestrian avatar right at street level on the road
      walkPosRef.current.set(targetX, 0, targetZ);
      walkPitchRef.current = 0.0;
      walkHeadingRef.current = roadHeading;

      // Immediately synchronize currentCamPos so camera lands on the road without flying through void
      if (walkPovRef.current === '1st') {
        currentCamPosRef.current.set(targetX, 1.8, targetZ);
        currentCamLookRef.current.set(
          targetX + Math.cos(roadHeading) * 20,
          1.8,
          targetZ + Math.sin(roadHeading) * 20
        );
      } else {
        const chaseDist = 6.5;
        const chaseH = 3.5;
        currentCamPosRef.current.set(
          targetX - Math.cos(roadHeading) * chaseDist,
          chaseH,
          targetZ - Math.sin(roadHeading) * chaseDist
        );
        currentCamLookRef.current.set(
          targetX + Math.cos(roadHeading) * 6,
          1.8,
          targetZ + Math.sin(roadHeading) * 6
        );
      }
    } else if (onLabelsUpdate) {
      // Clear stale walk-mode state; labels will update in aerial loop
    }
  }, [isWalkMode]);

  // Track currently pressed keys for smooth real-time walking
  const keysPressedRef = useRef<{ [key: string]: boolean }>({});

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

    // Game-style conservative device profile: smooth 60fps without GPU thermal throttling
    const renderer = new THREE.WebGLRenderer({
      antialias: true,
      powerPreference: 'high-performance',
      stencil: false,
      depth: true,
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5));
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
    groundMeshRef.current = ground;

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

    const hoverGroundPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0);
    const hoverMouseVec = new THREE.Vector2();
    const hoverHitVec = new THREE.Vector3();

    const onMouseMove = (e: MouseEvent) => {
      // Only update hover raycast when pointer is directly over the 3D canvas (not over UI buttons)
      if (!isDraggingRef.current && e.target === dom && cameraRef.current && mountRef.current) {
        const rect = mountRef.current.getBoundingClientRect();
        hoverMouseVec.set(
          ((e.clientX - rect.left) / rect.width) * 2 - 1,
          -((e.clientY - rect.top) / rect.height) * 2 + 1
        );
        raycasterRef.current.setFromCamera(hoverMouseVec, cameraRef.current);
        if (raycasterRef.current.ray.intersectPlane(hoverGroundPlane, hoverHitVec)) {
          if (!lastCursorWorldPosRef.current) {
            lastCursorWorldPosRef.current = new THREE.Vector3();
          }
          lastCursorWorldPosRef.current.copy(hoverHitVec);
        }
      }

      if (!isDraggingRef.current) return;
      const deltaX = e.clientX - previousMousePositionRef.current.x;
      const deltaY = e.clientY - previousMousePositionRef.current.y;

      if (isWalkModeRef.current) {
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
      } else if (!isAngleLockedRef.current) {
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
      if (!isWalkModeRef.current && cameraRef.current && mountRef.current) {
        const oldRadius = aerialAngleRef.current.radius;
        // Determine zoom scale factor
        const zoomFactor = e.deltaY > 0 ? 1.12 : 0.88;
        const newRadius = Math.max(35, Math.min(1000, oldRadius * zoomFactor));
        
        if (newRadius === oldRadius) return;

        // Raycast from cursor position to find the 3D point the cursor is hovering over
        const rect = mountRef.current.getBoundingClientRect();
        const mouseX = ((e.clientX - rect.left) / rect.width) * 2 - 1;
        const mouseY = -((e.clientY - rect.top) / rect.height) * 2 + 1;

        const raycaster = new THREE.Raycaster();
        raycaster.setFromCamera(new THREE.Vector2(mouseX, mouseY), cameraRef.current);

        // Find intersection with buildings first, or fall back to ground plane (y = 0 or y = 18 target height)
        let hitPoint: THREE.Vector3 | null = null;
        if (buildingMeshesRef.current && buildingMeshesRef.current.length > 0) {
          const intersects = raycaster.intersectObjects(buildingMeshesRef.current, false);
          if (intersects.length > 0) {
            hitPoint = intersects[0].point;
          }
        }

        if (!hitPoint) {
          // Intersect with ground plane at y = 0
          const groundPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0);
          const target = new THREE.Vector3();
          if (raycaster.ray.intersectPlane(groundPlane, target)) {
            hitPoint = target;
          }
        }

        if (hitPoint) {
          // Current camera look target center on horizontal plane
          const cx = panOffsetRef.current.x;
          const cz = panOffsetRef.current.z;

          // Shift pan offset towards the cursor hit point proportionally to the zoom delta
          // When zooming in (newRadius < oldRadius), shift center towards cursor point
          // When zooming out (newRadius > oldRadius), pull center back slightly
          const deltaRatio = 1 - (newRadius / oldRadius);
          // Apply damping (0.65) to keep zoom-to-point smooth and controllable
          panOffsetRef.current.x += (hitPoint.x - cx) * deltaRatio * 0.65;
          panOffsetRef.current.z += (hitPoint.z - cz) * deltaRatio * 0.65;
        }

        aerialAngleRef.current.radius = newRadius;
      }
    };

    dom.addEventListener('mousedown', onMouseDown);
    dom.addEventListener('contextmenu', onContextMenu);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    dom.addEventListener('wheel', onWheel);

    // Create visual 3rd person walking avatar mesh
    const avatarGroup = new THREE.Group();
    const avatarBody = new THREE.Mesh(
      new THREE.CapsuleGeometry(0.35, 1.1, 8, 16),
      new THREE.MeshStandardMaterial({ color: 0x0284c7, roughness: 0.4 })
    );
    avatarBody.position.y = 0.9;
    avatarBody.castShadow = true;

    const avatarHead = new THREE.Mesh(
      new THREE.SphereGeometry(0.25, 12, 12),
      new THREE.MeshStandardMaterial({ color: 0xfde047, roughness: 0.5 })
    );
    avatarHead.position.y = 1.7;
    avatarHead.castShadow = true;
    avatarGroup.add(avatarBody, avatarHead);
    scene.add(avatarGroup);
    avatarGroupRef.current = avatarGroup;

    // Keyboard controls in Walk mode (WASD / Arrows)
    const onKeyDown = (e: KeyboardEvent) => {
      keysPressedRef.current[e.key.toLowerCase()] = true;
    };
    const onKeyUp = (e: KeyboardEvent) => {
      keysPressedRef.current[e.key.toLowerCase()] = false;
    };
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);

    // Animation & Smooth Camera Interpolation Loop with Page Visibility pause
    let animId: number;
    let isTabVisible = !document.hidden;
    let lastLabelUpdateTime = 0;

    const onVisibilityChange = () => {
      isTabVisible = !document.hidden;
      if (isTabVisible) {
        cancelAnimationFrame(animId);
        animate();
      }
    };
    document.addEventListener('visibilitychange', onVisibilityChange);

    const animate = () => {
      if (!isTabVisible) return;
      animId = requestAnimationFrame(animate);

      // Handle continuous smooth walking movement when keys are held
      if (isWalkModeRef.current) {
        const moveSpeed = 0.8;
        const heading = walkHeadingRef.current;
        const forward = new THREE.Vector3(Math.cos(heading), 0, Math.sin(heading));
        const right = new THREE.Vector3(-Math.sin(heading), 0, Math.cos(heading));

        const keys = keysPressedRef.current;
        if (keys['w'] || keys['arrowup']) {
          walkPosRef.current.addScaledVector(forward, moveSpeed);
        }
        if (keys['s'] || keys['arrowdown']) {
          walkPosRef.current.addScaledVector(forward, -moveSpeed);
        }
        if (keys['a'] || keys['arrowleft']) {
          walkPosRef.current.addScaledVector(right, -moveSpeed);
        }
        if (keys['d'] || keys['arrowright']) {
          walkPosRef.current.addScaledVector(right, moveSpeed);
        }
      }

      // Compute Target Camera Position & LookAt
      const targetCamPos = new THREE.Vector3();
      const targetCamLook = new THREE.Vector3();

      if (isWalkModeRef.current) {
        const wp = walkPosRef.current;
        const wh = walkHeadingRef.current;
        const pitch = walkPitchRef.current;

        // Position avatar at walk position
        if (avatarGroupRef.current) {
          avatarGroupRef.current.position.set(wp.x, 0, wp.z);
          avatarGroupRef.current.rotation.y = -wh + Math.PI / 2;
          avatarGroupRef.current.visible = walkPovRef.current === '3rd';
        }

        if (walkPovRef.current === '1st') {
          // 1st Person POV: Eye level at 1.8m looking forward
          targetCamPos.set(wp.x, 1.8, wp.z);
          targetCamLook.set(
            wp.x + Math.cos(wh) * 30,
            1.8 + Math.sin(pitch) * 30,
            wp.z + Math.sin(wh) * 30
          );
        } else {
          // 3rd Person POV: Elevated chase camera behind avatar
          const chaseDist = 7.5;
          const chaseHeight = 3.6;
          targetCamPos.set(
            wp.x - Math.cos(wh) * chaseDist,
            chaseHeight,
            wp.z - Math.sin(wh) * chaseDist
          );
          targetCamLook.set(
            wp.x + Math.cos(wh) * 5,
            1.6 + Math.sin(pitch) * 10,
            wp.z + Math.sin(wh) * 5
          );
        }
      } else {
        if (avatarGroupRef.current) {
          avatarGroupRef.current.visible = false;
        }

        // Aerial Orbit Mode
        const { theta, phi, radius } = aerialAngleRef.current;
        const cx = panOffsetRef.current.x;
        const cz = panOffsetRef.current.z;

        targetCamPos.x = cx + radius * Math.sin(phi) * Math.sin(theta);
        targetCamPos.y = radius * Math.cos(phi);
        targetCamPos.z = cz + radius * Math.sin(phi) * Math.cos(theta);

        targetCamLook.set(cx, 18, cz);
      }

      // Smooth Lerp Transition between camera states (higher response factor in walk mode)
      const lerpSpeed = isWalkModeRef.current ? 0.2 : 0.08;
      currentCamPosRef.current.lerp(targetCamPos, lerpSpeed);
      currentCamLookRef.current.lerp(targetCamLook, lerpSpeed);

      if (cameraRef.current) {
        cameraRef.current.position.copy(currentCamPosRef.current);
        cameraRef.current.lookAt(currentCamLookRef.current);
      }

      renderer.render(scene, camera);

      // Throttle 3D -> 2D screen label projections (~10fps) to prevent React re-render storms
      const now = performance.now();
      if (
        onLabelsUpdate &&
        mountRef.current &&
        !isWalkModeRef.current &&
        now - lastLabelUpdateTime > 100
      ) {
        lastLabelUpdateTime = now;
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
      window.removeEventListener('keyup', onKeyUp);
      window.removeEventListener('resize', handleResize);
      document.removeEventListener('visibilitychange', onVisibilityChange);
      renderer.dispose();
      if (mountRef.current) {
        mountRef.current.innerHTML = '';
      }
    };
  }, []);

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
      sceneRef.current.background = new THREE.Color(0x0a101d);
      sceneRef.current.fog = new THREE.FogExp2(0x0a101d, 0.0008);

      // Disable directional hard casting shadows and clear shadow map at night
      sunLightRef.current.castShadow = false;
      if (sunLightRef.current.shadow.map) {
        sunLightRef.current.shadow.map.dispose();
        sunLightRef.current.shadow.map = null as any;
      }
      if (groundMeshRef.current) {
        groundMeshRef.current.receiveShadow = false;
      }

      // Soft cool moonlight placed high overhead in celestial space
      sunLightRef.current.position.set(150, 480, -150);
      sunLightRef.current.color.setHex(0x93c5fd);
      sunLightRef.current.intensity = 0.85;

      // Generous ambient sky radiance so facades and ground stay clearly visible
      ambientLightRef.current.color.setHex(0x1e293b);
      ambientLightRef.current.intensity = 0.95;

      // Hemisphere light for clear vertical contrast between upper sky and ground
      hemiLightRef.current.color.setHex(0x38bdf8);
      hemiLightRef.current.groundColor.setHex(0x0f172a);

      if (starsRef.current) starsRef.current.visible = true;

      // Illuminated windows at night (supporting both Standard and Basic materials)
      windowPanelsRef.current.forEach(({ mesh, nightColor }) => {
        if (mesh.material instanceof THREE.MeshStandardMaterial) {
          mesh.material.emissive.setHex(nightColor);
          mesh.material.emissiveIntensity = 1.0;
        } else if (mesh.material instanceof THREE.MeshBasicMaterial) {
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

      sunLightRef.current.castShadow = true;
      if (groundMeshRef.current) {
        groundMeshRef.current.receiveShadow = true;
      }
      sunLightRef.current.color.setHex(0xffffff);
      sunLightRef.current.intensity = 1.45;

      ambientLightRef.current.color.setHex(0xffffff);
      ambientLightRef.current.intensity = 0.7;

      hemiLightRef.current.color.setHex(0x38bdf8);
      hemiLightRef.current.groundColor.setHex(0x1e293b);

      if (starsRef.current) starsRef.current.visible = false;

      windowPanelsRef.current.forEach(({ mesh, dayColor }) => {
        if (mesh.material instanceof THREE.MeshStandardMaterial) {
          mesh.material.emissive.setHex(0x000000);
          mesh.material.emissiveIntensity = 0.0;
        } else if (mesh.material instanceof THREE.MeshBasicMaterial) {
          mesh.material.color.setHex(dayColor);
          mesh.material.opacity = 0.65;
        }
      });
      if (streetLampsGroupRef.current) streetLampsGroupRef.current.visible = false;
    }
  }, [solarInfo]);

  // Procedurally Build Realistic 3D Buildings using Architectural Generators
  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene || !buildingsGeoJSON || !buildingsGeoJSON.features) return;

    // Clear old building groups and meshes
    buildingMeshesRef.current.forEach((m) => {
      if (m.parent) m.parent.remove(m);
      scene.remove(m);
    });
    buildingMeshesRef.current = [];
    windowPanelsRef.current = [];
    labelAnchorsRef.current = [];

    // Master Architectural Dispatcher
    const { buildingMeshes, windows, labels, rootGroup } = ArchitecturalBuildingBuilder.buildCampus(
      buildingsGeoJSON,
      solarInfo.isNight
    );

    scene.add(rootGroup);
    buildingMeshesRef.current = buildingMeshes;
    windowPanelsRef.current = windows;
    labelAnchorsRef.current = labels;
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

    // Realistic Procedural Paved Walkways and Asphalt Roads
    let roadsGroup: THREE.Group | null = null;
    if ((pathsGeoJSON && pathsGeoJSON.features) || (roadsGeoJSON && roadsGeoJSON.features)) {
      roadsGroup = ProceduralRoadBuilder.buildRoadsAndPaths(pathsGeoJSON, roadsGeoJSON);
      scene.add(roadsGroup);
    }

    return () => {
      if (roadsGroup && scene) scene.remove(roadsGroup);
    };
  }, [pathsGeoJSON, roadsGeoJSON]);

  // Sports Complex in 3D (Athletics Track, Cricket, Soccer, Courts)
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
      // Correct mathematical rotation: +PI/2 preserves +Z south and -Z north coordinates
      geo.rotateX(Math.PI / 2);

      const sport = feat.properties?.sport;
      let color = 0x0284c7; // Default court blue
      let yElev = 0.2;

      if (sport === 'running') {
        color = 0x991b1b; // Tartan red running track
        yElev = 0.22;
      } else if (sport === 'soccer') {
        color = 0x15803d; // Lush green soccer turf infield
        yElev = 0.24;
      } else if (sport === 'cricket') {
        color = 0xb45309; // Terracotta clay cricket outfield
        yElev = 0.2;
      } else if (sport === 'volleyball') {
        color = 0xea580c; // Vibrant orange court
        yElev = 0.22;
      } else if (sport === 'tennis' || sport === 'basketball') {
        color = 0x0284c7; // Synthetic blue court
        yElev = 0.22;
      }

      const mat = new THREE.MeshStandardMaterial({
        color,
        roughness: 0.85,
        metalness: 0.1,
      });

      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.y = yElev;
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
