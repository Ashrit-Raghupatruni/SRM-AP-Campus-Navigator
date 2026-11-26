package com.srmap.campusnavigator.domain.building3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.core.geometry3d.Vector3

/**
 * Architectural specification for SR Block (Sarvepalli Radhakrishnan Block / Academic Block).
 * Building 16 in campus dataset.
 * - 7 Floors (Total height 23.1m + rooftop penthouse to 26.5m)
 * - Perkins + Will Horizontal Ribbon Architecture with cantilevered floor slabs
 * - Wrap-around ribbon curtain glazing with dark charcoal spandrels
 * - Solid warm sandstone vertical shear core towers
 * - Ground-floor colonnade and entrance approach plaza
 */
object SRBlockSpec {

    const val BUILDING_ID = "srmap_bldg_16"
    const val OFFICIAL_NAME = "SR Block"
    const val FULL_NAME = "Sarvepalli Radhakrishnan Block"

    // 7 Documented Floors
    const val LEVELS_COUNT = 7
    const val FLOOR_HEIGHT_METERS = 3.3f
    const val TOTAL_ROOF_HEIGHT_METERS = LEVELS_COUNT * FLOOR_HEIGHT_METERS // 23.1m
    const val PENTHOUSE_HEIGHT_METERS = 3.4f // Top of penthouse = 26.5m

    // Metric Bounding Limits in Local Tangent ENU
    const val MIN_X = -4.63f
    const val MAX_X = 51.94f  // Width ~ 56.6m
    const val MIN_Z = 25.75f  // North edge
    const val MAX_Z = 138.18f // South edge, Length ~ 112.4m
    val CENTROID = Vector3(23.65f, TOTAL_ROOF_HEIGHT_METERS / 2f, 81.96f)

    // Materials Sampled Directly from Real Campus Photographs
    val COLOR_SLAB_EYEBROW      = Color(0xFFE2E8F0) // Light architectural concrete cantilevered sunshade slabs
    val COLOR_DARK_SPANDREL     = Color(0xFF1E293B) // Dark charcoal/slate horizontal spandrel bands
    val COLOR_RIBBON_GLAZING    = Color(0xFF3B5971) // Reflective solar-control wrap-around curtain glass
    val COLOR_VERTICAL_CORE     = Color(0xFFD3CBBE) // Warm precast sandstone vertical core towers
    val COLOR_COLONNADE_PILLARS = Color(0xFFE2E8F0) // Ground-floor structural concrete columns
    val COLOR_ROOF_DECK         = Color(0xFF94A3B8) // Flat waterproof roof surface
    val COLOR_PENTHOUSE_WALL    = Color(0xFF64748B) // Rooftop machine room mechanical enclosure
    val COLOR_PLAZA_PAVERS      = Color(0xFFCBD5E1) // Paved entrance approach walkways
    val COLOR_GREEN_PARTERRE    = Color(0xFF387A4C) // Geometric landscaped grass beds

    // Six Calibrated Architectural Viewpoints
    fun getSRBlockViewpoints(): List<BuildingViewpoint> {
        return listOf(
            BuildingViewpoint(
                id = "photo1_sunset_sports",
                name = "Sunset Perspective (Photo 1)",
                eyePosition = Vector3(85.0f, 10.0f, 175.0f),
                targetPosition = Vector3(23.65f, 12.0f, 81.96f),
                pitchDegrees = 7.0f,
                yawDegrees = 330.0f
            ),
            BuildingViewpoint(
                id = "photo2_corner_ribbon",
                name = "Corner Ribbon Glazing (Photo 2)",
                eyePosition = Vector3(72.0f, 14.0f, 38.0f),
                targetPosition = Vector3(45.0f, 12.0f, 48.0f),
                pitchDegrees = 6.0f,
                yawDegrees = 245.0f
            ),
            BuildingViewpoint(
                id = "photo3_aerial_master",
                name = "Aerial Overview (Photo 3)",
                eyePosition = Vector3(95.0f, 75.0f, 82.0f),
                targetPosition = Vector3(23.65f, 15.0f, 81.96f),
                pitchDegrees = 42.0f,
                yawDegrees = 270.0f
            ),
            BuildingViewpoint(
                id = "sr_main_entrance",
                name = "Main Entrance Plaza",
                eyePosition = Vector3(65.0f, 1.8f, 82.0f),
                targetPosition = Vector3(42.0f, 3.0f, 82.0f),
                pitchDegrees = 2.0f,
                yawDegrees = 270.0f
            ),
            BuildingViewpoint(
                id = "east_elevation_courts",
                name = "East Facade (Courts View)",
                eyePosition = Vector3(92.0f, 8.0f, 81.96f),
                targetPosition = Vector3(MAX_X, 11.5f, 81.96f),
                pitchDegrees = 4.0f,
                yawDegrees = 270.0f
            ),
            BuildingViewpoint(
                id = "sr_to_cv_high_plaza",
                name = "SR to CV Plaza (Photo 115637/42)",
                eyePosition = Vector3(12.0f, 15.0f, 138.0f),
                targetPosition = Vector3(-65.0f, 18.0f, 215.0f),
                pitchDegrees = 18.0f,
                yawDegrees = 220.0f
            ),
            BuildingViewpoint(
                id = "top_down_roof",
                name = "Top-Down Plan View",
                eyePosition = Vector3(23.65f, 110.0f, 81.96f),
                targetPosition = Vector3(23.65f, 0.0f, 81.96f),
                pitchDegrees = 89.0f,
                yawDegrees = 0.0f
            )
        )
    }
}
