import * as THREE from 'three';
import { ArchitecturalMeshResult, createBoxMesh } from './MeshUtils';
import { latLngToLocal } from '../geo';

/**
 * Procedural 3D Architectural Reconstruction for "The Flying Umbrella" (Academic Complex Spaceframe Canopy).
 * Real-world photo alignment (from Photos 1, 2, 3, and 4):
 * - Massive continuous rectangular cantilevered spaceframe roof covering both V-Block and Homi J Bhabha Block.
 * - Spans over the central breezeway atrium canyon, connecting V-Block (west) and Homi Bhabha (east).
 * - Multi-tier skybridges linking V-Block and Homi Bhabha at floors 3, 5, and roof deck across the atrium.
 * - Flared 4-way branching structural steel "tree" columns (#F8FAFC) supporting the canopy spaceframe.
 * - Monumental dark horizontal slatted "ACADEMIC BLOCK" wall with golden SRM University AP emblem.
 * - Gate 3 Entrance Monument with "SRM University AP" signage and perimeter landscape flowerbeds.
 */
export class AcademicComplexCanopyGenerator {
  static generate(buildingsGeoJSON?: any, isNight: boolean = false): ArchitecturalMeshResult {
    const group = new THREE.Group();
    const windows: { mesh: THREE.Mesh; nightColor: number; dayColor: number }[] = [];

    const CANOPY_COLOR = 0x9ca3af;       // Real metallic aluminium spaceframe roof deck
    const STEEL_TRUSS_COLOR = 0x64748b;  // Structural steel spaceframe webbing
    const TREE_COLUMN_COLOR = 0xf8fafc;  // Crisp white tubular tree columns
    const COLOR_MONUMENT_WALL = 0x1e293b;// Dark charcoal monument pylon
    const COLOR_GOLD_MEDALLION = 0xf59e0b;// Golden brass SRM emblem
    const COLOR_SIGN_WHITE = 0xffffff;   // Crisp white lettering / signage
    const COLOR_SKYBRIDGE_WALL = 0xf8fafc;
    const COLOR_SKYBRIDGE_GLASS = 0x38bdf8;
    const COLOR_NIGHT_WINDOW = 0x38bdf8;

    // Default or dynamically computed bounding center between V-Block and Homi Bhabha
    let vMinX = 28.5, vMaxX = 110.4, minZ = -113.4, maxZ = -53.0;
    let hMinX = 126.9, hMaxX = 168.5;

    if (buildingsGeoJSON && buildingsGeoJSON.features) {
      const v = buildingsGeoJSON.features.find((f: any) => f.properties?.name?.includes('V Block') || f.properties?.name?.includes('Sarabhai'));
      const h = buildingsGeoJSON.features.find((f: any) => f.properties?.name?.includes('Bhabha') || f.properties?.name?.includes('Homi'));

      if (v?.geometry?.coordinates?.[0]) {
        const coords = v.geometry.coordinates[0];
        vMinX = Math.min(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).x));
        vMaxX = Math.max(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).x));
        minZ = Math.min(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).z));
        maxZ = Math.max(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).z));
      }
      if (h?.geometry?.coordinates?.[0]) {
        const coords = h.geometry.coordinates[0];
        hMinX = Math.min(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).x));
        hMaxX = Math.max(...coords.map(([lng, lat]: [number, number]) => latLngToLocal(lat, lng).x));
      }
    }

    // Canyon & Mega Canopy Span (Photo 1 & Photo 4 show it extends generously past both blocks)
    const CANOPY_MIN_X = vMinX - 8.0;   // ~20.5m
    const CANOPY_MAX_X = hMaxX + 8.0;   // ~176.5m
    const CANOPY_MIN_Z = minZ - 10.0;   // ~ -123.4m
    const CANOPY_MAX_Z = maxZ + 12.0;   // ~ -41.0m
    const CANOPY_W = CANOPY_MAX_X - CANOPY_MIN_X; // ~156m
    const CANOPY_L = CANOPY_MAX_Z - CANOPY_MIN_Z; // ~82m
    const CANOPY_CX = (CANOPY_MIN_X + CANOPY_MAX_X) / 2; // ~98.5m
    const CANOPY_CZ = (CANOPY_MIN_Z + CANOPY_MAX_Z) / 2; // ~ -82.2m

    const CANOPY_TOP_Y = 28.5; // Elevated above 7-story (25.2m) blocks with airflow gap
    const CANOPY_BOT_Y = 26.5;

    // 1. Aerodynamic Space-Frame Top Roof Deck
    const roofDeckGeo = new THREE.BoxGeometry(CANOPY_W, 0.4, CANOPY_L);
    const roofDeckMat = new THREE.MeshStandardMaterial({
      color: CANOPY_COLOR,
      roughness: 0.35,
      metalness: 0.45,
      side: THREE.DoubleSide,
    });
    const roofDeck = new THREE.Mesh(roofDeckGeo, roofDeckMat);
    roofDeck.position.set(CANOPY_CX, CANOPY_TOP_Y, CANOPY_CZ);
    roofDeck.castShadow = true;
    roofDeck.receiveShadow = true;
    group.add(roofDeck);

    // Aerodynamic perimeter fascia bevel lip
    const fasciaMat = new THREE.MeshStandardMaterial({
      color: 0xe2e8f0,
      roughness: 0.3,
      metalness: 0.5,
    });
    const northFascia = createBoxMesh(CANOPY_MIN_X - 0.5, CANOPY_MAX_X + 0.5, CANOPY_BOT_Y, CANOPY_TOP_Y + 0.3, CANOPY_MIN_Z - 0.5, CANOPY_MIN_Z + 0.5, 0xe2e8f0);
    const southFascia = createBoxMesh(CANOPY_MIN_X - 0.5, CANOPY_MAX_X + 0.5, CANOPY_BOT_Y, CANOPY_TOP_Y + 0.3, CANOPY_MAX_Z - 0.5, CANOPY_MAX_Z + 0.5, 0xe2e8f0);
    const westFascia = createBoxMesh(CANOPY_MIN_X - 0.5, CANOPY_MIN_X + 0.5, CANOPY_BOT_Y, CANOPY_TOP_Y + 0.3, CANOPY_MIN_Z, CANOPY_MAX_Z, 0xe2e8f0);
    const eastFascia = createBoxMesh(CANOPY_MAX_X - 0.5, CANOPY_MAX_X + 0.5, CANOPY_BOT_Y, CANOPY_TOP_Y + 0.3, CANOPY_MIN_Z, CANOPY_MAX_Z, 0xe2e8f0);
    group.add(northFascia, southFascia, westFascia, eastFascia);

    // 2. 3D Triangular Pyramid Space-Frame Webbing (Visible underneath and from aerial view)
    const trussMat = new THREE.MeshStandardMaterial({
      color: STEEL_TRUSS_COLOR,
      roughness: 0.4,
      metalness: 0.6,
      wireframe: false,
    });

    const stepX = 14.0;
    const stepZ = 12.0;
    const pyramidGeo = new THREE.ConeGeometry(4.2, CANOPY_TOP_Y - CANOPY_BOT_Y, 4);
    pyramidGeo.rotateY(Math.PI / 4);

    for (let x = CANOPY_MIN_X + stepX; x < CANOPY_MAX_X - stepX / 2; x += stepX) {
      for (let z = CANOPY_MIN_Z + stepZ; z < CANOPY_MAX_Z - stepZ / 2; z += stepZ) {
        const node = new THREE.Mesh(pyramidGeo, trussMat);
        node.position.set(x, (CANOPY_TOP_Y + CANOPY_BOT_Y) / 2, z);
        group.add(node);
      }
    }

    // Longitudinal and transverse tubular chord beams
    for (let x = CANOPY_MIN_X + stepX; x < CANOPY_MAX_X; x += stepX * 2) {
      const beamGeo = new THREE.BoxGeometry(0.35, 0.35, CANOPY_L * 0.95);
      const beam = new THREE.Mesh(beamGeo, trussMat);
      beam.position.set(x, CANOPY_BOT_Y, CANOPY_CZ);
      group.add(beam);
    }
    for (let z = CANOPY_MIN_Z + stepZ; z < CANOPY_MAX_Z; z += stepZ * 2) {
      const beamGeo = new THREE.BoxGeometry(CANOPY_W * 0.95, 0.35, 0.35);
      const beam = new THREE.Mesh(beamGeo, trussMat);
      beam.position.set(CANOPY_CX, CANOPY_BOT_Y, z);
      group.add(beam);
    }

    // 3. Flared Tubular Steel "Tree" Columns (Authentic branching as seen in Photos 1 & 2)
    const treeColPositions = [
      // South entrance row (Photo 1 & Photo 2 foreground)
      { x: vMinX + 10.0, z: maxZ + 4.0 },
      { x: (vMinX + vMaxX) / 2, z: maxZ + 4.0 },
      { x: (vMaxX + hMinX) / 2 - 4.0, z: maxZ + 4.0 },
      { x: (vMaxX + hMinX) / 2 + 4.0, z: maxZ + 4.0 },
      { x: (hMinX + hMaxX) / 2, z: maxZ + 4.0 },
      { x: hMaxX - 6.0, z: maxZ + 4.0 },

      // Central Breezeway Atrium Columns (Photo 3)
      { x: (vMaxX + hMinX) / 2 - 4.5, z: CANOPY_CZ },
      { x: (vMaxX + hMinX) / 2 + 4.5, z: CANOPY_CZ },

      // North row
      { x: vMinX + 10.0, z: minZ - 4.0 },
      { x: (vMinX + vMaxX) / 2, z: minZ - 4.0 },
      { x: (vMaxX + hMinX) / 2, z: minZ - 4.0 },
      { x: (hMinX + hMaxX) / 2, z: minZ - 4.0 },
      { x: hMaxX - 6.0, z: minZ - 4.0 },
    ];

    const trunkMat = new THREE.MeshStandardMaterial({
      color: TREE_COLUMN_COLOR,
      roughness: 0.25,
      metalness: 0.3,
    });

    const trunkH = 20.5;
    const trunkGeo = new THREE.CylinderGeometry(0.55, 0.65, trunkH, 16);

    for (const pos of treeColPositions) {
      // Main vertical trunk
      const trunk = new THREE.Mesh(trunkGeo, trunkMat);
      trunk.position.set(pos.x, trunkH / 2, pos.z);
      trunk.castShadow = true;
      group.add(trunk);

      // Flared 4-Way Diagonal Structural Branches (Photos 1, 2)
      for (let angle = 0; angle < Math.PI * 2; angle += Math.PI / 2) {
        const branchL = 7.5;
        const branchGeo = new THREE.CylinderGeometry(0.2, 0.3, branchL, 12);
        const branch = new THREE.Mesh(branchGeo, trunkMat);
        const branchOffsetX = Math.cos(angle) * 2.4;
        const branchOffsetZ = Math.sin(angle) * 2.4;
        branch.position.set(pos.x + branchOffsetX, trunkH + 2.8, pos.z + branchOffsetZ);
        branch.rotation.z = Math.cos(angle) * 0.42;
        branch.rotation.x = Math.sin(angle) * 0.42;
        branch.castShadow = true;
        group.add(branch);
      }
    }

    // 4. Multi-Tier Skybridges Connecting V-Block and Homi J Bhabha Block (Photo 3 & Photo 4)
    // Connecting spans across the central breezeway from vMaxX (~110.4m) to hMinX (~126.9m)
    const atriumBridgeMinX = vMaxX;
    const atriumBridgeMaxX = hMinX;
    const atriumCZ = (minZ + maxZ) / 2; // ~ -83m

    const bridgeLevels = [
      { yMin: 9.5, yMax: 13.0 },   // Level 3 Skybridge
      { yMin: 16.5, yMax: 20.0 },  // Level 5 Skybridge
      { yMin: 23.5, yMax: 26.5 },  // Level 7 / Roof Promenade Bridge
    ];

    for (const b of bridgeLevels) {
      // Floor Slab
      const slabMesh = createBoxMesh(
        atriumBridgeMinX - 0.2, atriumBridgeMaxX + 0.2,
        b.yMin, b.yMin + 0.4,
        atriumCZ - 4.5, atriumCZ + 4.5,
        0xffffff,
        0.3, 0.1
      );
      // Roof / Ceiling Slab
      const roofMesh = createBoxMesh(
        atriumBridgeMinX - 0.2, atriumBridgeMaxX + 0.2,
        b.yMax - 0.4, b.yMax,
        atriumCZ - 4.5, atriumCZ + 4.5,
        0xf8fafc,
        0.3, 0.1
      );
      // Glazed North and South Panoramic Enclosures
      const northGlass = createBoxMesh(
        atriumBridgeMinX, atriumBridgeMaxX,
        b.yMin + 0.4, b.yMax - 0.4,
        atriumCZ - 4.5, atriumCZ - 4.3,
        COLOR_SKYBRIDGE_GLASS,
        0.1, 0.8,
        isNight ? COLOR_NIGHT_WINDOW : 0x000000,
        isNight ? 0.85 : 0.0
      );
      const southGlass = createBoxMesh(
        atriumBridgeMinX, atriumBridgeMaxX,
        b.yMin + 0.4, b.yMax - 0.4,
        atriumCZ + 4.3, atriumCZ + 4.5,
        COLOR_SKYBRIDGE_GLASS,
        0.1, 0.8,
        isNight ? COLOR_NIGHT_WINDOW : 0x000000,
        isNight ? 0.85 : 0.0
      );
      group.add(slabMesh, roofMesh, northGlass, southGlass);
      windows.push(
        { mesh: northGlass, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_SKYBRIDGE_GLASS },
        { mesh: southGlass, nightColor: COLOR_NIGHT_WINDOW, dayColor: COLOR_SKYBRIDGE_GLASS }
      );
    }

    // 5. Monumental "ACADEMIC BLOCK" Pylon Wall (Photo 3)
    // Dark horizontal charcoal wall with circular golden SRM medallion positioned in the entrance plaza
    const pylonX = (vMaxX + hMinX) / 2; // ~118.6m
    const pylonZ = maxZ + 2.5;         // ~ -50.5m in front of atrium entrance

    const pylonWall = createBoxMesh(
      pylonX - 9.0, pylonX + 9.0,
      0, 5.8,
      pylonZ - 0.6, pylonZ + 0.6,
      COLOR_MONUMENT_WALL,
      0.45, 0.2
    );
    group.add(pylonWall);

    // Golden SRM Emblem Medallion
    const emblemGeo = new THREE.CylinderGeometry(1.6, 1.6, 0.25, 24);
    emblemGeo.rotateX(Math.PI / 2);
    const emblemMat = new THREE.MeshStandardMaterial({
      color: COLOR_GOLD_MEDALLION,
      roughness: 0.25,
      metalness: 0.85,
      emissive: isNight ? 0xd97706 : 0x000000,
      emissiveIntensity: isNight ? 0.5 : 0.0,
    });
    const emblem = new THREE.Mesh(emblemGeo, emblemMat);
    emblem.position.set(pylonX, 3.2, pylonZ + 0.65);
    group.add(emblem);

    // White horizontal title band
    const titleBand = createBoxMesh(
      pylonX - 7.5, pylonX + 7.5,
      1.1, 1.5,
      pylonZ + 0.62, pylonZ + 0.68,
      COLOR_SIGN_WHITE,
      0.3, 0.1
    );
    group.add(titleBand);

    // 6. Gate 3 Entrance Sign ("SRM University AP" Signboard, Photo 2)
    // Positioned near Gate 3 approach (X: ~140m, Z: -36m)
    const signX = 138.0;
    const signZ = -36.0;
    const signPostW = createBoxMesh(signX - 0.2, signX + 0.2, 0, 3.2, signZ - 3.0, signZ - 2.6, 0x475569);
    const signPostE = createBoxMesh(signX - 0.2, signX + 0.2, 0, 3.2, signZ + 2.6, signZ + 3.0, 0x475569);
    const signBoard = createBoxMesh(
      signX - 0.15, signX + 0.15,
      1.2, 3.2,
      signZ - 2.8, signZ + 2.8,
      0xffffff,
      0.2, 0.1
    );
    // Green shrub planter bed base for Gate 3 sign
    const signPlanter = createBoxMesh(
      signX - 1.2, signX + 1.2,
      0, 0.5,
      signZ - 3.5, signZ + 3.5,
      0x15803d,
      0.9, 0.05
    );
    group.add(signPostW, signPostE, signBoard, signPlanter);

    // 7. Underside Warm Downlights (Night Glow)
    for (let x = CANOPY_MIN_X + 20.0; x <= CANOPY_MAX_X - 20.0; x += 22.0) {
      for (let z = CANOPY_MIN_Z + 15.0; z <= CANOPY_MAX_Z - 15.0; z += 18.0) {
        const lampGeo = new THREE.SphereGeometry(0.5, 8, 8);
        const lampMat = new THREE.MeshStandardMaterial({
          color: 0xfde047,
          emissive: 0xfbbf24,
          emissiveIntensity: isNight ? 0.95 : 0.0,
        });
        const lamp = new THREE.Mesh(lampGeo, lampMat);
        lamp.position.set(x, CANOPY_BOT_Y - 0.2, z);
        group.add(lamp);
      }
    }

    return {
      group,
      windows,
      labelPos: new THREE.Vector3(CANOPY_CX, CANOPY_TOP_Y + 5.0, CANOPY_CZ),
      name: 'Academic Complex (Mega Canopy)',
      category: 'Academic',
      color: '#38bdf8',
      floors: 7,
    };
  }
}
