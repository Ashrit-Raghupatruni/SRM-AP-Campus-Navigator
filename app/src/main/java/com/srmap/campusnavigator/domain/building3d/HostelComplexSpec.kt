package com.srmap.campusnavigator.domain.building3d

import androidx.compose.ui.graphics.Color

/**
 * Architectural specification for Ganga Boys' Hostel (srmap_bldg_15) and
 * Vedavathi Boys' Hostel (srmap_bldg_14) at SRM University-AP.
 *
 * Source of truth: Real on-site photos and official campus spatial data.
 */
object HostelComplexSpec {

    // === Ganga Boys' Hostel (26 Floors - Iconic Skyscraper Tower) ===
    const val GANGA_LEVELS = 26
    const val GANGA_PODIUM_LEVELS = 5
    const val GANGA_PILOTIS_HEIGHT = 5.5f
    const val GANGA_PODIUM_HEIGHT = 16.5f
    const val GANGA_TOTAL_HEIGHT = 80.6f
    const val GANGA_HEADHOUSE_HEIGHT = 83.5f

    // Ganga Footprint Envelope in Local Coordinates
    const val GANGA_MIN_X = -5.0f
    const val GANGA_MAX_X = 49.6f
    const val GANGA_MIN_Z = -54.3f
    const val GANGA_MAX_Z = 0.3f

    // Ganga Tower Core (Rising to 26 floors)
    const val GANGA_TOWER_MIN_X = 15.0f
    const val GANGA_TOWER_MAX_X = 48.0f
    const val GANGA_TOWER_MIN_Z = -54.0f
    const val GANGA_TOWER_MAX_Z = -15.0f

    // === Vedavathi Boys' Hostel (15 Floors - Mid-rise with Open Courtyard) ===
    const val VEDAVATHI_LEVELS = 15
    const val VEDAVATHI_PODIUM_LEVELS = 5
    const val VEDAVATHI_PILOTIS_HEIGHT = 5.5f
    const val VEDAVATHI_PODIUM_HEIGHT = 16.5f
    const val VEDAVATHI_TOTAL_HEIGHT = 46.5f
    const val VEDAVATHI_HEADHOUSE_HEIGHT = 48.5f

    // Vedavathi Footprint Envelope in Local Coordinates
    const val VEDAVATHI_MIN_X = -4.7f
    const val VEDAVATHI_MAX_X = 49.7f
    const val VEDAVATHI_MIN_Z = -126.9f
    const val VEDAVATHI_MAX_Z = -71.9f

    // Vedavathi Interior Courtyard (U-shape opening south)
    const val VEDAVATHI_COURTYARD_MIN_X = 11.0f
    const val VEDAVATHI_COURTYARD_MAX_X = 33.0f
    const val VEDAVATHI_COURTYARD_MIN_Z = -110.6f
    const val VEDAVATHI_COURTYARD_MAX_Z = -90.2f

    // === Civic Plaza & Fountain Coordinates (Between Ganga & Vedavathi) ===
    const val FOUNTAIN_CENTER_X = 22.0f
    const val FOUNTAIN_CENTER_Z = -63.0f
    const val FOUNTAIN_SIZE = 7.0f // 7m x 7m square tiered fountain

    // === Architectural Color Palette ===
    val COLOR_TERRACOTTA_OCHRE = Color(0xFF966A55) // Warm earthen terracotta wall panels
    val COLOR_TERRACOTTA_DARK  = Color(0xFF7C5543) // Accent window surround fins
    val COLOR_WHITE_SLAB       = Color(0xFFF8FAFC) // Crisp white horizontal floor slabs & eyebrows
    val COLOR_PILOTIS_COLUMNS  = Color(0xFFE2E8F0) // Structural white cylindrical pilotis
    val COLOR_CURTAIN_GLASS    = Color(0xFF38BDF8) // Double-height entrance lobby & sky lounge glazing
    val COLOR_WINDOW_GLASS     = Color(0xFF64748B) // Residential punched window glass
    val COLOR_DARK_SPANDREL    = Color(0xFF334155) // Window recess spandrel frames
    val COLOR_ROOF_GRAVEL      = Color(0xFFCBD5E1) // Rooftop terrace deck
    val COLOR_FOUNTAIN_GRANITE = Color(0xFF1E293B) // Dark granite tiered fountain coping
    val COLOR_FOUNTAIN_WATER   = Color(0xFF0284C7) // Vibrant azure fountain water
}
