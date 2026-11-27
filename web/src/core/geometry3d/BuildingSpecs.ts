// Authentic building floor counts & palettes for SRM University-AP buildings
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
      wallColor: 0xf8fafc,      // Crisp white stucco
      accentColor: 0x3b82f6,    // Blue architectural fins
      slabColor: 0xe2e8f0,      // Crisp white floor eyebrows
      roofColor: 0x64748b,      // Rooftop solar racking
      windowColor: 0x1e293b,    // Punched window frames
      nightWindow: 0xfef08a,
    };
  }
  if (lower.includes('mess') || lower.includes('annapurna') || lower.includes('dining')) {
    return {
      wallColor: 0xf8fafc,      // Clean white pavilion walls
      accentColor: 0x0284c7,    // Deep ocean blue double-height dining facade
      slabColor: 0x0284c7,      // Blue architectural portal canopy
      roofColor: 0x64748b,      // Kitchen vent mechanical penthouse
      windowColor: 0x38bdf8,    // High-transmittance glazed curtain
      nightWindow: 0xfef08a,    // Warm evening cafeteria glow
    };
  }
  return {
    wallColor: 0xf1f5f9,
    accentColor: 0x334155,
    slabColor: 0xe2e8f0,
    roofColor: 0x94a3b8,
    windowColor: 0x1e293b,
    nightWindow: 0xfef08a,
  };
}
