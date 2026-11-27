/**
 * Dedicated Campus Background Web Worker
 * Offloads heavy GeoJSON parsing, spatial chunk classification, and indexing from the main thread.
 */

export interface ChunkData {
  id: string;
  name: string;
  features: any[];
  bounds: { minX: number; maxX: number; minZ: number; maxZ: number };
}

// Map building names to designated spatial zones
function getChunkIdForBuilding(name: string): string {
  const lower = (name || '').toLowerCase();
  if (
    lower.includes('sr block') ||
    lower.includes('v block') ||
    lower.includes('homi') ||
    lower.includes('x-lab') ||
    lower.includes('jc bose') ||
    lower.includes('cv raman') ||
    lower.includes('academic') ||
    lower.includes('canopy') ||
    lower.includes('office')
  ) {
    return 'chunk_academic_core';
  }
  if (lower.includes('ganga') || lower.includes('vedavathi')) {
    return 'chunk_boys_hostels';
  }
  if (
    lower.includes('yamuna') ||
    lower.includes('narmada') ||
    lower.includes('godavari') ||
    lower.includes('krishna') ||
    lower.includes('kaveri') ||
    lower.includes('girls')
  ) {
    return 'chunk_girls_hostels';
  }
  if (
    lower.includes('mess') ||
    lower.includes('annapurna') ||
    lower.includes('sport') ||
    lower.includes('ground') ||
    lower.includes('court') ||
    lower.includes('track')
  ) {
    return 'chunk_sports_amenities';
  }
  return 'chunk_periphery';
}

self.onmessage = async (e: MessageEvent) => {
  const { type, payload } = e.data;

  if (type === 'PARSE_AND_CHUNK_CAMPUS') {
    const { buildingsGeoJSON } = payload;
    if (!buildingsGeoJSON || !buildingsGeoJSON.features) {
      self.postMessage({ type: 'CHUNK_PROCESSING_COMPLETE', chunks: {} });
      return;
    }

    const chunkBuckets: Record<string, ChunkData> = {
      chunk_academic_core: {
        id: 'chunk_academic_core',
        name: 'Academic Central Core',
        features: [],
        bounds: { minX: Infinity, maxX: -Infinity, minZ: Infinity, maxZ: -Infinity },
      },
      chunk_boys_hostels: {
        id: 'chunk_boys_hostels',
        name: "Boys' Hostels & Dining",
        features: [],
        bounds: { minX: Infinity, maxX: -Infinity, minZ: Infinity, maxZ: -Infinity },
      },
      chunk_girls_hostels: {
        id: 'chunk_girls_hostels',
        name: "Girls' Hostels Residential Complex",
        features: [],
        bounds: { minX: Infinity, maxX: -Infinity, minZ: Infinity, maxZ: -Infinity },
      },
      chunk_sports_amenities: {
        id: 'chunk_sports_amenities',
        name: 'Sports Complex & Central Dining',
        features: [],
        bounds: { minX: Infinity, maxX: -Infinity, minZ: Infinity, maxZ: -Infinity },
      },
      chunk_periphery: {
        id: 'chunk_periphery',
        name: 'Perimeter Grounds & Entrances',
        features: [],
        bounds: { minX: Infinity, maxX: -Infinity, minZ: Infinity, maxZ: -Infinity },
      },
    };

    // Sort features into spatial chunks
    for (const feat of buildingsGeoJSON.features) {
      const name = feat.properties?.name || '';
      const chunkId = getChunkIdForBuilding(name);
      chunkBuckets[chunkId].features.push(feat);
    }

    // Stream out chunks progressively so priority chunks render first
    self.postMessage({
      type: 'CHUNK_READY',
      chunkId: 'chunk_academic_core',
      chunk: chunkBuckets.chunk_academic_core,
    });

    self.postMessage({
      type: 'CHUNK_READY',
      chunkId: 'chunk_boys_hostels',
      chunk: chunkBuckets.chunk_boys_hostels,
    });

    self.postMessage({
      type: 'CHUNK_READY',
      chunkId: 'chunk_girls_hostels',
      chunk: chunkBuckets.chunk_girls_hostels,
    });

    self.postMessage({
      type: 'CHUNK_READY',
      chunkId: 'chunk_sports_amenities',
      chunk: chunkBuckets.chunk_sports_amenities,
    });

    self.postMessage({
      type: 'CHUNK_READY',
      chunkId: 'chunk_periphery',
      chunk: chunkBuckets.chunk_periphery,
    });

    self.postMessage({
      type: 'ALL_CHUNKS_COMPLETE',
      totalBuildings: buildingsGeoJSON.features.length,
    });
  }
};
