import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';
import { latLngToLocal } from '../../core/geo';
import { SolarInfo } from '../../types/campus';
import { RouteResult } from '../../types/campus';

interface CampusMap3DProps {
  buildingsGeoJSON: any;
  pathsGeoJSON: any;
  roadsGeoJSON: any;
  solarInfo: SolarInfo;
  activeRoute: RouteResult | null;
  cameraMode: 'orbit' | 'first-person';
}

export const CampusMap3D: React.FC<CampusMap3DProps> = ({
  buildingsGeoJSON,
  pathsGeoJSON,
  roadsGeoJSON,
  solarInfo,
  activeRoute,
  cameraMode,
}) => {
  const mountRef = useRef<HTMLDivElement>(null);

  // References to dynamic lights and scene elements
  const sceneRef = useRef<THREE.Scene | null>(null);
  const sunLightRef = useRef<THREE.DirectionalLight | null>(null);
  const ambientLightRef = useRef<THREE.AmbientLight | null>(null);
  const hemiLightRef = useRef<THREE.HemisphereLight | null>(null);
  const starsRef = useRef<THREE.Points | null>(null);
  const buildingMeshesRef = useRef<THREE.Mesh[]>([]);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);

  // Orbit camera control state
  const isDraggingRef = useRef(false);
  const previousMousePositionRef = useRef({ x: 0, y: 0 });
  const cameraAngleRef = useRef({ theta: Math.PI / 4, phi: Math.PI / 4, radius: 450 });

  useEffect(() => {
    if (!mountRef.current) return;

    // Scene
    const scene = new THREE.Scene();
    sceneRef.current = scene;

    const width = mountRef.current.clientWidth;
    const height = mountRef.current.clientHeight;

    // Camera
    const camera = new THREE.PerspectiveCamera(45, width / height, 1, 3000);
    cameraRef.current = camera;
    cameraAngleRef.current.radius = 350; // Focused directly on campus buildings
    updateCameraPosition();

    // Renderer
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    mountRef.current.appendChild(renderer.domElement);

    // Dynamic Atmosphere & Lights
    const hemiLight = new THREE.HemisphereLight(0xffffff, 0x444444, 0.6);
    hemiLight.position.set(0, 500, 0);
    scene.add(hemiLight);
    hemiLightRef.current = hemiLight;

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.2);
    scene.add(ambientLight);
    ambientLightRef.current = ambientLight;

    const sunLight = new THREE.DirectionalLight(0xffffff, 1.2);
    sunLight.castShadow = true;
    sunLight.shadow.mapSize.width = 2048;
    sunLight.shadow.mapSize.height = 2048;
    sunLight.shadow.camera.near = 10;
    sunLight.shadow.camera.far = 1500;
    const d = 500;
    sunLight.shadow.camera.left = -d;
    sunLight.shadow.camera.right = d;
    sunLight.shadow.camera.top = d;
    sunLight.shadow.camera.bottom = -d;
    scene.add(sunLight);
    sunLightRef.current = sunLight;

    // Campus Ground Mesh (1.5km x 1.5km)
    const groundGeo = new THREE.PlaneGeometry(1600, 1600);
    const groundMat = new THREE.MeshStandardMaterial({
      color: 0x1f2937,
      roughness: 0.9,
      metalness: 0.1,
    });
    const ground = new THREE.Mesh(groundGeo, groundMat);
    ground.rotation.x = -Math.PI / 2;
    ground.receiveShadow = true;
    scene.add(ground);

    // Stars particle field for night sky
    const starCount = 1200;
    const starGeo = new THREE.BufferGeometry();
    const starPositions = new Float32Array(starCount * 3);
    for (let i = 0; i < starCount * 3; i += 3) {
      starPositions[i] = (Math.random() - 0.5) * 2000;
      starPositions[i + 1] = Math.random() * 800 + 100;
      starPositions[i + 2] = (Math.random() - 0.5) * 2000;
    }
    starGeo.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
    const starMat = new THREE.PointsMaterial({ color: 0xffffff, size: 2, transparent: true, opacity: 0.8 });
    const stars = new THREE.Points(starGeo, starMat);
    scene.add(stars);
    starsRef.current = stars;

    // Mouse handlers for Orbiting
    const dom = renderer.domElement;
    const onMouseDown = (e: MouseEvent) => {
      isDraggingRef.current = true;
      previousMousePositionRef.current = { x: e.clientX, y: e.clientY };
    };

    const onMouseMove = (e: MouseEvent) => {
      if (!isDraggingRef.current) return;
      const deltaX = e.clientX - previousMousePositionRef.current.x;
      const deltaY = e.clientY - previousMousePositionRef.current.y;

      cameraAngleRef.current.theta -= deltaX * 0.006;
      cameraAngleRef.current.phi = Math.max(0.1, Math.min(Math.PI / 2 - 0.05, cameraAngleRef.current.phi + deltaY * 0.006));

      previousMousePositionRef.current = { x: e.clientX, y: e.clientY };
      updateCameraPosition();
    };

    const onMouseUp = () => {
      isDraggingRef.current = false;
    };

    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      // Clamp zoom distance between 80m and 450m so user stays tightly focused on SRM AP
      cameraAngleRef.current.radius = Math.max(80, Math.min(450, cameraAngleRef.current.radius + e.deltaY * 0.3));
      updateCameraPosition();
    };

    dom.addEventListener('mousedown', onMouseDown);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    dom.addEventListener('wheel', onWheel);

    // Render loop
    let animId: number;
    const animate = () => {
      animId = requestAnimationFrame(animate);
      renderer.render(scene, camera);
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
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
      dom.removeEventListener('wheel', onWheel);
      window.removeEventListener('resize', handleResize);
      renderer.dispose();
      if (mountRef.current) {
        mountRef.current.innerHTML = '';
      }
    };
  }, []);

  function updateCameraPosition() {
    if (!cameraRef.current) return;
    const { theta, phi, radius } = cameraAngleRef.current;
    cameraRef.current.position.x = radius * Math.sin(phi) * Math.sin(theta);
    cameraRef.current.position.y = radius * Math.cos(phi);
    cameraRef.current.position.z = radius * Math.sin(phi) * Math.cos(theta);
    cameraRef.current.lookAt(0, 15, 0);
  }

  // Update Dynamic Lighting & Sky based on Solar Calculations
  useEffect(() => {
    if (!sceneRef.current || !sunLightRef.current || !ambientLightRef.current || !hemiLightRef.current) return;

    const { sunAltitude, sunAzimuth, isNight, isGoldenHour } = solarInfo;

    // Update Sun Position
    const dist = 600;
    const sunY = Math.sin(sunAltitude) * dist;
    const sunHorizontalDist = Math.cos(sunAltitude) * dist;
    const sunX = Math.sin(sunAzimuth) * sunHorizontalDist;
    const sunZ = -Math.cos(sunAzimuth) * sunHorizontalDist;

    sunLightRef.current.position.set(sunX, Math.max(sunY, -100), sunZ);

    if (isNight) {
      // Night Palette
      sceneRef.current.background = new THREE.Color(0x060913);
      sceneRef.current.fog = new THREE.FogExp2(0x060913, 0.001);

      sunLightRef.current.color.setHex(0x93c5fd); // Moonlight tint
      sunLightRef.current.intensity = 0.25;

      ambientLightRef.current.color.setHex(0x1e293b);
      ambientLightRef.current.intensity = 0.4;

      hemiLightRef.current.color.setHex(0x1e1b4b);
      hemiLightRef.current.groundColor.setHex(0x090d16);

      if (starsRef.current) starsRef.current.visible = true;

      // Glow building windows
      buildingMeshesRef.current.forEach((m) => {
        if (m.material instanceof THREE.MeshStandardMaterial) {
          m.material.color.setHex(0x1e293b);
          m.material.emissive.setHex(0x0ea5e9);
          m.material.emissiveIntensity = 0.3;
        }
      });
    } else if (isGoldenHour) {
      // Golden Hour Sunset/Sunrise Palette
      sceneRef.current.background = new THREE.Color(0xfdba74);
      sceneRef.current.fog = new THREE.FogExp2(0xfdba74, 0.0008);

      sunLightRef.current.color.setHex(0xf97316);
      sunLightRef.current.intensity = 1.6;

      ambientLightRef.current.color.setHex(0xfef08a);
      ambientLightRef.current.intensity = 0.6;

      hemiLightRef.current.color.setHex(0xfde047);
      hemiLightRef.current.groundColor.setHex(0x78350f);

      if (starsRef.current) starsRef.current.visible = false;

      buildingMeshesRef.current.forEach((m) => {
        if (m.material instanceof THREE.MeshStandardMaterial) {
          m.material.color.setHex(0xf8fafc);
          m.material.emissive.setHex(0xd97706);
          m.material.emissiveIntensity = 0.15;
        }
      });
    } else {
      // Crisp Bright Daylight
      sceneRef.current.background = new THREE.Color(0xbae6fd);
      sceneRef.current.fog = new THREE.FogExp2(0xe0f2fe, 0.0006);

      sunLightRef.current.color.setHex(0xffffff);
      sunLightRef.current.intensity = 1.4;

      ambientLightRef.current.color.setHex(0xffffff);
      ambientLightRef.current.intensity = 0.65;

      hemiLightRef.current.color.setHex(0x38bdf8);
      hemiLightRef.current.groundColor.setHex(0x334155);

      if (starsRef.current) starsRef.current.visible = false;

      buildingMeshesRef.current.forEach((m) => {
        if (m.material instanceof THREE.MeshStandardMaterial) {
          m.material.color.setHex(0xffffff);
          m.material.emissive.setHex(0x000000);
          m.material.emissiveIntensity = 0;
        }
      });
    }
  }, [solarInfo]);

  // Procedurally Extrude Buildings from GeoJSON
  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene || !buildingsGeoJSON || !buildingsGeoJSON.features) return;

    // Clear previous buildings
    buildingMeshesRef.current.forEach((mesh) => scene.remove(mesh));
    buildingMeshesRef.current = [];

    buildingsGeoJSON.features.forEach((feature: any) => {
      const geom = feature.geometry;
      if (!geom || geom.type !== 'Polygon') return;

      const coords = geom.coordinates[0]; // exterior ring
      if (!coords || coords.length < 3) return;

      const shape = new THREE.Shape();
      for (let i = 0; i < coords.length; i++) {
        const [lng, lat] = coords[i];
        const local = latLngToLocal(lat, lng);
        if (i === 0) {
          shape.moveTo(local.x, local.z);
        } else {
          shape.lineTo(local.x, local.z);
        }
      }

      const levels = parseInt(feature.properties?.levels || '4', 10);
      const height = Math.max(10, levels * 4.2); // ~4.2m per floor

      const extrudeSettings: THREE.ExtrudeGeometryOptions = {
        depth: height,
        bevelEnabled: true,
        bevelSegments: 2,
        steps: 1,
        bevelSize: 0.5,
        bevelThickness: 0.5,
      };

      const extrudeGeo = new THREE.ExtrudeGeometry(shape, extrudeSettings);
      extrudeGeo.rotateX(Math.PI / 2); // Three.js Y-up

      const buildingMat = new THREE.MeshStandardMaterial({
        color: 0xffffff,
        roughness: 0.3,
        metalness: 0.2,
      });

      const mesh = new THREE.Mesh(extrudeGeo, buildingMat);
      mesh.castShadow = true;
      mesh.receiveShadow = true;

      scene.add(mesh);
      buildingMeshesRef.current.push(mesh);
    });
  }, [buildingsGeoJSON]);

  // Render 3D Path Ribbons and Navigation Chevrons
  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene) return;

    // Render Active Navigation Route Ribbon
    let routeLine: THREE.Line | null = null;
    if (activeRoute && activeRoute.coordinates.length > 1) {
      const points: THREE.Vector3[] = [];
      for (const [lat, lng] of activeRoute.coordinates) {
        const local = latLngToLocal(lat, lng);
        points.push(new THREE.Vector3(local.x, 1.2, local.z));
      }
      const lineGeo = new THREE.BufferGeometry().setFromPoints(points);
      const lineMat = new THREE.LineBasicMaterial({ color: 0x0284c7, linewidth: 4 });
      routeLine = new THREE.Line(lineGeo, lineMat);
      scene.add(routeLine);
    }

    return () => {
      if (routeLine && scene) scene.remove(routeLine);
    };
  }, [activeRoute]);

  return (
    <div className="w-full h-full relative overflow-hidden">
      <div ref={mountRef} className="w-full h-full cursor-grab active:cursor-grabbing" />
      <div className="absolute top-4 left-4 bg-black/60 backdrop-blur-md text-white text-xs px-3 py-1.5 rounded-xl border border-white/10 pointer-events-none flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
        <span>Drag to rotate • Scroll to zoom 3D campus</span>
      </div>
    </div>
  );
};
