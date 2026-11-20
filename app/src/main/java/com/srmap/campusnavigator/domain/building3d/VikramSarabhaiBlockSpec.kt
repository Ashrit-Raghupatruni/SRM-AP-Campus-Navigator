package com.srmap.campusnavigator.domain.building3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.core.geometry3d.Vector3
import com.srmap.campusnavigator.domain.models.GeoPoint

/**
 * Architectural specification for Vikram Sarabhai Block (Building 03 — Academic Block I / V Block).
 * Strictly maintains verification statuses:
 * - Footprint coordinates and levels are VERIFIED_SURVEY from campus data.
 * - Floor heights and approach portal are EVIDENCE_DERIVED.
 * - Material color codes are UNVERIFIED_PLACEHOLDER awaiting on-site photo sampling.
 */
object VikramSarabhaiBlockSpec {

    const val BUILDING_ID = "srmap_bldg_9"
    const val OFFICIAL_NAME = "Vikram Sarabhai Block"
    const val CODE_NAME = "V Block"

    // Verified Levels & Derived Height
    const val LEVELS_COUNT = 5
    const val FLOOR_HEIGHT_METERS = 3.2f // Evidence-derived standard floor clearance
    const val TOTAL_ROOF_HEIGHT_METERS = LEVELS_COUNT * FLOOR_HEIGHT_METERS // 16.0m
    const val PARAPET_HEIGHT_METERS = 0.9f // Perimeter safety barrier to 16.9m

    // Metric Bounding Limits in Local Tangent ENU (centered around x=119.5m, z=-177.7m)
    const val MIN_X = 78.56f
    const val MAX_X = 160.52f
    const val MIN_Z = -207.89f // North edge
    const val MAX_Z = -147.54f // South edge
    val CENTROID = Vector3(119.54f, TOTAL_ROOF_HEIGHT_METERS / 2f, -177.71f)

    // Material Colors with Verification Statuses
    val COLOR_WALL_CONCRETE = VerifiedMaterial("Wall Precast Sandstone", "#E2E8F0", VerificationStatus.UNVERIFIED_PLACEHOLDER)
    val COLOR_SPANDREL_BAND = VerifiedMaterial("Floor Spandrel Band", "#CBD5E1", VerificationStatus.UNVERIFIED_PLACEHOLDER)
    val COLOR_WINDOW_FRAME  = VerifiedMaterial("Charcoal Window Frame", "#334155", VerificationStatus.UNVERIFIED_PLACEHOLDER)
    val COLOR_WINDOW_GLASS  = VerifiedMaterial("Solar Reflective Glazing", "#64748B", VerificationStatus.UNVERIFIED_PLACEHOLDER)
    val COLOR_ENTRANCE_PORTAL = VerifiedMaterial("Entrance Portal Surround", "#1E293B", VerificationStatus.UNVERIFIED_PLACEHOLDER)
    val COLOR_ROOF_SURFACE  = VerifiedMaterial("Roof Waterproof Membrane", "#94A3B8", VerificationStatus.UNVERIFIED_PLACEHOLDER)
    val COLOR_ROOF_PARAPET  = VerifiedMaterial("Roof Parapet Wall", "#E2E8F0", VerificationStatus.UNVERIFIED_PLACEHOLDER)

    // Helper Color getters for OpenGL ES 3.0 shaders
    val wallColor: Color get() = Color(0xFFE2E8F0)
    val spandrelColor: Color get() = Color(0xFFCBD5E1)
    val frameColor: Color get() = Color(0xFF334155)
    val glassColor: Color get() = Color(0xFF64748B)
    val portalColor: Color get() = Color(0xFF1E293B)
    val roofColor: Color get() = Color(0xFF94A3B8)
    val parapetColor: Color get() = Color(0xFFE2E8F0)

    // Six Calibrated Architectural Viewpoints
    fun getViewpoints(): List<BuildingViewpoint> {
        val targetCenter = CENTROID
        val entranceTarget = Vector3(119.54f, 2.0f, MAX_Z)

        return listOf(
            BuildingViewpoint(
                id = "front_elevation",
                name = "Front Elevation (South)",
                eyePosition = Vector3(119.54f, 6.0f, -95.0f),
                targetPosition = Vector3(119.54f, 8.0f, MAX_Z),
                pitchDegrees = 4.0f,
                yawDegrees = 0.0f
            ),
            BuildingViewpoint(
                id = "rear_elevation",
                name = "Rear Elevation (North)",
                eyePosition = Vector3(119.54f, 6.0f, -260.0f),
                targetPosition = Vector3(119.54f, 8.0f, MIN_Z),
                pitchDegrees = 4.0f,
                yawDegrees = 180.0f
            ),
            BuildingViewpoint(
                id = "side_east",
                name = "Side Elevation (East Breezeway)",
                eyePosition = Vector3(215.0f, 6.0f, -177.71f),
                targetPosition = Vector3(MAX_X, 8.0f, -177.71f),
                pitchDegrees = 4.0f,
                yawDegrees = 270.0f
            ),
            BuildingViewpoint(
                id = "side_west",
                name = "Side Elevation (West Campus)",
                eyePosition = Vector3(25.0f, 6.0f, -177.71f),
                targetPosition = Vector3(MIN_X, 8.0f, -177.71f),
                pitchDegrees = 4.0f,
                yawDegrees = 90.0f
            ),
            BuildingViewpoint(
                id = "top_down",
                name = "Top-Down (Plan View)",
                eyePosition = Vector3(119.54f, 95.0f, -177.71f),
                targetPosition = Vector3(119.54f, 0.0f, -177.71f),
                pitchDegrees = 89.0f,
                yawDegrees = 0.0f
            ),
            BuildingViewpoint(
                id = "main_entrance",
                name = "Main Entrance Approach",
                eyePosition = Vector3(119.54f, 1.6f, -132.0f),
                targetPosition = entranceTarget,
                pitchDegrees = 2.0f,
                yawDegrees = 0.0f
            )
        )
    }
}
