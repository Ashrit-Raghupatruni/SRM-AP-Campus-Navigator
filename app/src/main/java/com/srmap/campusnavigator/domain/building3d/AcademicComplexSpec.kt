package com.srmap.campusnavigator.domain.building3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.core.geometry3d.Vector3

/**
 * Architectural specification for the SRM AP Academic Mega-Complex:
 * - Vikram Sarabhai Block (Left / West wing)
 * - Homi J. Bhabha Block (Right / East wing)
 * - Central Breezeway axis connecting JC Bose Block (South) to Tree Court (North)
 * - Monumental space-frame umbrella canopy with 4-way branching tree columns
 */
object AcademicComplexSpec {

    // 1. Vikram Sarabhai Block (Left Wing)
    const val V_BLOCK_ID = "srmap_bldg_9"
    const val V_MIN_X = 78.56f
    const val V_MAX_X = 147.71f
    const val V_MIN_Z = -207.89f // North edge (Tree Court)
    const val V_MAX_Z = -147.54f // South edge (JC Bose facing)
    const val V_LEVELS = 5
    const val V_HEIGHT = 16.0f

    // 2. Central Breezeway Spine (Highway connecting JC Bose to Tree Court)
    const val BREEZEWAY_MIN_X = 147.71f
    const val BREEZEWAY_MAX_X = 160.52f // ~12.8m wide canyon
    const val BREEZEWAY_MIN_Z = -207.89f
    const val BREEZEWAY_MAX_Z = -147.54f

    // 3. Homi J. Bhabha Block (Right Wing)
    const val HOMI_BLOCK_ID = "srmap_bldg_30"
    const val HOMI_MIN_X = 160.52f
    const val HOMI_MAX_X = 208.50f
    const val HOMI_MIN_Z = -207.89f
    const val HOMI_MAX_Z = -147.54f
    const val HOMI_LEVELS = 7
    const val HOMI_HEIGHT = 22.4f

    // 4. Overarching Monumental Space-Frame Canopy ("The Flying Umbrella")
    const val CANOPY_MIN_X = 70.0f
    const val CANOPY_MAX_X = 218.0f // Spans 148m over both blocks and breezeway
    const val CANOPY_MIN_Z = -216.0f
    const val CANOPY_MAX_Z = -139.5f // Spans 76.5m depth
    const val CANOPY_SOFFIT_Y = 22.8f
    const val CANOPY_ROOF_PEAK_Y = 25.0f

    // 5. Materials Sampled Directly from Real Campus Photographs
    val COLOR_SOLID_WHITE_TOWER = Color(0xFFF8FAFC) // Crisp monolithic white corner volumes
    val COLOR_LOUVER_FINS       = Color(0xFFD9D2C5) // Warm sandstone vertical brise-soleil sunshades
    val COLOR_CURTAIN_GLAZING   = Color(0xFF2B5876) // Deep reflective solar blue tinted glass
    val COLOR_TREE_COLUMNS      = Color(0xFFE2E8F0) // Slender steel tubular pylons & branches
    val COLOR_CANOPY_DECK       = Color(0xFF8F9CA8) // Aerodynamic warm-grey corrugated canopy roof
    val COLOR_CANOPY_TRUSS      = Color(0xFF718096) // Space-frame lattice underbelly
    val COLOR_BREEZEWAY_PAVERS  = Color(0xFFE2E8F0) // Light campus pedestrian plaza pavement
    val COLOR_LINK_BRIDGE_SLAB  = Color(0xFFFFFFFF) // White composite bridge enclosure

    // 6. Calibrated Architectural Viewpoints
    fun getComplexViewpoints(): List<BuildingViewpoint> {
        val complexCenterX = (V_MIN_X + HOMI_MAX_X) / 2f // ~143.5m
        val complexCenterZ = (V_MIN_Z + V_MAX_Z) / 2f    // ~-177.7m

        return listOf(
            BuildingViewpoint(
                id = "photo1_front_perspective",
                name = "Front Perspective (Photo 1 Angle)",
                eyePosition = Vector3(185.0f, 4.0f, -100.0f),
                targetPosition = Vector3(143.5f, 12.0f, -147.54f),
                pitchDegrees = 8.0f,
                yawDegrees = 335.0f
            ),
            BuildingViewpoint(
                id = "photo2_aerial_complex",
                name = "Birds-Eye Aerial (Photo 2 Angle)",
                eyePosition = Vector3(143.5f, 75.0f, -70.0f),
                targetPosition = Vector3(143.5f, 15.0f, -177.7f),
                pitchDegrees = 38.0f,
                yawDegrees = 0.0f
            ),
            BuildingViewpoint(
                id = "breezeway_highway",
                name = "Breezeway Spine (JC Bose -> Tree Court)",
                eyePosition = Vector3(154.1f, 1.6f, -125.0f),
                targetPosition = Vector3(154.1f, 3.0f, -207.89f),
                pitchDegrees = 2.0f,
                yawDegrees = 0.0f
            ),
            BuildingViewpoint(
                id = "vblock_front",
                name = "Vikram Sarabhai Block (Left)",
                eyePosition = Vector3(113.1f, 5.0f, -115.0f),
                targetPosition = Vector3(113.1f, 8.0f, -147.54f),
                pitchDegrees = 4.0f,
                yawDegrees = 0.0f
            ),
            BuildingViewpoint(
                id = "homi_front",
                name = "Homi J Bhabha Block (Right)",
                eyePosition = Vector3(184.5f, 5.0f, -115.0f),
                targetPosition = Vector3(184.5f, 8.0f, -147.54f),
                pitchDegrees = 4.0f,
                yawDegrees = 0.0f
            ),
            BuildingViewpoint(
                id = "tree_court_view",
                name = "Tree Court Looking South",
                eyePosition = Vector3(154.1f, 4.0f, -235.0f),
                targetPosition = Vector3(154.1f, 8.0f, -177.7f),
                pitchDegrees = 5.0f,
                yawDegrees = 180.0f
            )
        )
    }
}
