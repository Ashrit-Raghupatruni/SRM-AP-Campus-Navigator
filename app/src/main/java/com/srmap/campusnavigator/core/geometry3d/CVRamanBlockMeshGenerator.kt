package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color

/**
 * Procedural 3D Architectural Generator for CV Raman Block (Academic Block III / srmap_bldg_20).
 *
 * Precise Evidence-Driven Architectural Reconstruction based on:
 * - High-Res Daytime Elevation Photo (media_1795321559212_ca972b5f.jpg)
 * - Night Illuminations Photo (media_1795322032695_6699b34d.jpg)
 * - SRM AP Aerial & Ground vantage points from SR Block
 *
 * Facade Orientation:
 * - North Facade (Z = 209.0f): Faces the central campus lawn towards SR Block and the Academic Complex.
 *   THIS IS THE MONUMENTAL ELEVATION WITH THE ATRIUM VOIDS AND BREEZEWAY SKY DECKS.
 * - South Facade (Z = 244.1f): Faces Neerukonda / perimeter road.
 * - East End (X = -40.6f): Faces toward Gate 1 boulevard and sports grounds.
 * - West End (X = -124.9f): Features the 5-story western annex wing.
 *
 * Key Architectural Elements:
 * 1. 12-Story Concrete Monolith: Total height 42.0m, length 84.3m, width 35.1m.
 * 2. 5-Story Western Annex Wing: X in [-124.9, -104.0], H = 18.0m with punched rectangular windows.
 * 3. 12-Story Main Monolith: X in [-104.0, -40.6], H = 42.0m.
 * 4. Monumental Dual-Tier Breezeway Atrium Voids on the North Facade (Facing Campus Lawn):
 *    - Lower Void: Floors 4-6 (Y: 13.5m - 21.0m, X: -88.0m to -56.0m, depth into building 9.0m)
 *    - Upper Void: Floors 7-9 (Y: 23.5m - 31.0m, X: -88.0m to -56.0m, depth into building 9.0m)
 *    - Illumination / warm golden interior backwalls (#FEF08A / #F59E0B) matching night photography.
 * 5. Full-Height Vertical Sky-Blue Glass Curtain Spines:
 *    - Flanking both sides of the atrium voids from ground to roof.
 *    - Wrapping around the northeast corner.
 * 6. Regular Concrete Brise-Soleil / Louver Grid:
 *    - Vertical fins spacing out dark punched window bands across all floors.
 * 7. Top Floor Mechanical Parapet Screen & Overhanging Roof Brow.
 */
object CVRamanBlockMeshGenerator {

    // Building Bounding Box in Local Coordinates
    private const val BLDG_MIN_X = -124.9f
    private const val BLDG_MAX_X = -40.6f
    private const val NORTH_Z = 209.0f // Campus facing facade (front)
    private const val SOUTH_Z = 244.1f // Perimeter rear facade

    // Horizontal Partition: West Wing vs Main Tower
    private const val WING_DIVIDER_X = -104.0f
    private const val WING_HEIGHT = 18.0f // 5 floors
    private const val TOWER_HEIGHT = 42.0f // 12 floors

    // Monumental Atrium Voids (Piercing the North Facade)
    private const val VOID_MIN_X = -88.0f
    private const val VOID_MAX_X = -56.0f
    private const val VOID_DEPTH_Z = 10.0f // Inset into the building from NORTH_Z

    private const val LOWER_VOID_MIN_Y = 13.5f
    private const val LOWER_VOID_MAX_Y = 21.0f

    private const val UPPER_VOID_MIN_Y = 23.5f
    private const val UPPER_VOID_MAX_Y = 31.0f

    // Authentic Material Colors
    private val FAÇADE_PRECAST_WHITE = Color(0xFFF1F5F9)
    private val FAÇADE_PRECAST_SHADOW = Color(0xFFCBD5E1)
    private val FAÇADE_DARK_CONCRETE = Color(0xFF475569)
    private val WINDOW_DARK = Color(0xFF1E293B)
    private val CURTAIN_GLASS = Color(0xFF38BDF8)
    private val GLASS_MULLION = Color(0xFF0F172A)
    private val VOID_BACKWALL_WARM = Color(0xFFFDE047) // Warm interior glow seen in photos
    private val VOID_CEILING_DARK = Color(0xFF1E293B)
    private val ROOF_GRAVEL = Color(0xFF94A3B8)
    private val PILOTIS_COLOR = Color(0xFF334155)

    fun generateMesh(): MeshData {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        fun addBox(
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            color: Color
        ) {
            val minX = minOf(x1, x2)
            val maxX = maxOf(x1, x2)
            val minY = minOf(y1, y2)
            val maxY = maxOf(y1, y2)
            val minZ = minOf(z1, z2)
            val maxZ = maxOf(z1, z2)

            val r = color.red
            val g = color.green
            val b = color.blue
            val a = 1.0f

            fun addQuad(
                v0x: Float, v0y: Float, v0z: Float,
                v1x: Float, v1y: Float, v1z: Float,
                v2x: Float, v2y: Float, v2z: Float,
                v3x: Float, v3y: Float, v3z: Float,
                nx: Float, ny: Float, nz: Float,
                shade: Float = 1.0f
            ) {
                val start = (positions.size / 3).toShort()
                val sr = (r * shade).coerceIn(0f, 1f)
                val sg = (g * shade).coerceIn(0f, 1f)
                val sb = (b * shade).coerceIn(0f, 1f)

                val verts = floatArrayOf(
                    v0x, v0y, v0z,
                    v1x, v1y, v1z,
                    v2x, v2y, v2z,
                    v3x, v3y, v3z
                )

                for (i in 0 until 4) {
                    positions.add(verts[i * 3])
                    positions.add(verts[i * 3 + 1])
                    positions.add(verts[i * 3 + 2])
                    normals.add(nx); normals.add(ny); normals.add(nz)
                    colors.add(sr); colors.add(sg); colors.add(sb); colors.add(a)
                }

                indices.add(start)
                indices.add((start + 1).toShort())
                indices.add((start + 2).toShort())

                indices.add(start)
                indices.add((start + 2).toShort())
                indices.add((start + 3).toShort())
            }

            // Top (+Y)
            addQuad(minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0f, 1f, 0f, 1.0f)
            // Bottom (-Y)
            addQuad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0f, -1f, 0f, 0.6f)
            // Front (South, +Z)
            addQuad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0f, 0f, 1f, 0.92f)
            // Back (North, -Z) - FACING CAMPUS LAWN (PRIMARY FRONT)
            addQuad(maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0f, 0f, -1f, 1.0f)
            // Left (West, -X)
            addQuad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1f, 0f, 0f, 0.9f)
            // Right (East, +X)
            addQuad(maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1f, 0f, 0f, 0.9f)
        }

        // =========================================================================
        // 1. WEST ANNEX WING (5 Stories, X in [BLDG_MIN_X, WING_DIVIDER_X], H = 18m)
        // =========================================================================
        addBox(BLDG_MIN_X, 0f, NORTH_Z, WING_DIVIDER_X, WING_HEIGHT, SOUTH_Z, FAÇADE_PRECAST_WHITE)

        // Punched Classroom Windows on the North Facade of the West Wing
        val wingCols = 5
        val wingStepX = (WING_DIVIDER_X - BLDG_MIN_X - 4.0f) / wingCols
        for (f in 0 until 5) {
            val fy = f * 3.5f + 1.2f
            for (c in 0 until wingCols) {
                val wx = BLDG_MIN_X + 2.0f + c * wingStepX
                addBox(wx + 0.3f, fy, NORTH_Z - 0.15f, wx + wingStepX - 0.6f, fy + 1.8f, NORTH_Z + 0.2f, WINDOW_DARK)
            }
        }
        // West Wing Roof Parapet
        addBox(BLDG_MIN_X, WING_HEIGHT, NORTH_Z, WING_DIVIDER_X, WING_HEIGHT + 1.0f, SOUTH_Z, FAÇADE_DARK_CONCRETE)
        addBox(BLDG_MIN_X + 0.5f, WING_HEIGHT, NORTH_Z + 0.5f, WING_DIVIDER_X - 0.5f, WING_HEIGHT + 0.1f, SOUTH_Z - 0.5f, ROOF_GRAVEL)

        // =========================================================================
        // 2. MAIN 12-STORY TOWER BODY (X in [WING_DIVIDER_X, BLDG_MAX_X], H = 42m)
        // =========================================================================
        // A) Solid Left Tower Mass (WING_DIVIDER_X to VOID_MIN_X)
        addBox(WING_DIVIDER_X, 0f, NORTH_Z, VOID_MIN_X, TOWER_HEIGHT, SOUTH_Z, FAÇADE_PRECAST_WHITE)

        // B) Solid Right Tower Mass (VOID_MAX_X to BLDG_MAX_X)
        addBox(VOID_MAX_X, 0f, NORTH_Z, BLDG_MAX_X, TOWER_HEIGHT, SOUTH_Z, FAÇADE_PRECAST_WHITE)

        // C) Central Core Backwall behind the voids (VOID_MIN_X to VOID_MAX_X, depth NORTH_Z + VOID_DEPTH_Z to SOUTH_Z)
        addBox(VOID_MIN_X, 0f, NORTH_Z + VOID_DEPTH_Z, VOID_MAX_X, TOWER_HEIGHT, SOUTH_Z, FAÇADE_PRECAST_SHADOW)

        // D) Ground to Floor 3 Central Podium (Y in 0..13.5m)
        addBox(VOID_MIN_X, 0f, NORTH_Z, VOID_MAX_X, LOWER_VOID_MIN_Y, SOUTH_Z, FAÇADE_PRECAST_WHITE)

        // Glazed Lobby Entrance on North Facade
        addBox(VOID_MIN_X + 2.0f, 0.4f, NORTH_Z - 0.25f, VOID_MAX_X - 2.0f, LOWER_VOID_MIN_Y - 0.5f, NORTH_Z + 0.3f, CURTAIN_GLASS)

        // Entrance Pilotis Colonnade
        val colCount = 6
        val colStep = (VOID_MAX_X - VOID_MIN_X - 4.0f) / (colCount - 1)
        for (i in 0 until colCount) {
            val cx = VOID_MIN_X + 2.0f + i * colStep
            addBox(cx - 0.5f, 0f, NORTH_Z - 0.8f, cx + 0.5f, LOWER_VOID_MIN_Y, NORTH_Z + 0.2f, PILOTIS_COLOR)
        }

        // =========================================================================
        // 3. MONUMENTAL DUAL BREEZEWAY ATRIUM VOIDS (North Elevation Cutouts)
        // =========================================================================
        // Floor slab bridging between lower and upper voids (Floors 6-7 transition slab)
        addBox(VOID_MIN_X, LOWER_VOID_MAX_Y, NORTH_Z, VOID_MAX_X, UPPER_VOID_MIN_Y, SOUTH_Z, FAÇADE_PRECAST_WHITE)

        // Top structural bridge slab over upper void (Floors 9-12 crown)
        addBox(VOID_MIN_X, UPPER_VOID_MAX_Y, NORTH_Z, VOID_MAX_X, TOWER_HEIGHT, SOUTH_Z, FAÇADE_PRECAST_WHITE)

        // Lower Atrium Void (Y: 13.5m - 21.0m):
        // Illuminated warm interior backwall matching night photos
        addBox(VOID_MIN_X + 0.2f, LOWER_VOID_MIN_Y + 0.2f, NORTH_Z + VOID_DEPTH_Z - 0.2f, VOID_MAX_X - 0.2f, LOWER_VOID_MAX_Y - 0.2f, NORTH_Z + VOID_DEPTH_Z, VOID_BACKWALL_WARM)
        // Dark ceiling & floor deck
        addBox(VOID_MIN_X, LOWER_VOID_MAX_Y - 0.4f, NORTH_Z, VOID_MAX_X, LOWER_VOID_MAX_Y, NORTH_Z + VOID_DEPTH_Z, VOID_CEILING_DARK)
        addBox(VOID_MIN_X, LOWER_VOID_MIN_Y, NORTH_Z, VOID_MAX_X, LOWER_VOID_MIN_Y + 0.4f, NORTH_Z + VOID_DEPTH_Z, FAÇADE_DARK_CONCRETE)
        // Safety glass railing across lower void
        addBox(VOID_MIN_X, LOWER_VOID_MIN_Y + 0.4f, NORTH_Z - 0.1f, VOID_MAX_X, LOWER_VOID_MIN_Y + 1.4f, NORTH_Z + 0.1f, CURTAIN_GLASS)

        // Upper Atrium Void (Y: 23.5m - 31.0m):
        // Illuminated warm interior backwall
        addBox(VOID_MIN_X + 0.2f, UPPER_VOID_MIN_Y + 0.2f, NORTH_Z + VOID_DEPTH_Z - 0.2f, VOID_MAX_X - 0.2f, UPPER_VOID_MAX_Y - 0.2f, NORTH_Z + VOID_DEPTH_Z, VOID_BACKWALL_WARM)
        // Dark ceiling & floor deck
        addBox(VOID_MIN_X, UPPER_VOID_MAX_Y - 0.4f, NORTH_Z, VOID_MAX_X, UPPER_VOID_MAX_Y, NORTH_Z + VOID_DEPTH_Z, VOID_CEILING_DARK)
        addBox(VOID_MIN_X, UPPER_VOID_MIN_Y, NORTH_Z, VOID_MAX_X, UPPER_VOID_MIN_Y + 0.4f, NORTH_Z + VOID_DEPTH_Z, FAÇADE_DARK_CONCRETE)
        // Safety glass railing across upper void
        addBox(VOID_MIN_X, UPPER_VOID_MIN_Y + 0.4f, NORTH_Z - 0.1f, VOID_MAX_X, UPPER_VOID_MIN_Y + 1.4f, NORTH_Z + 0.1f, CURTAIN_GLASS)

        // =========================================================================
        // 4. VERTICAL SKY-BLUE GLASS CURTAIN WALL SPINES (Floor 1 to 12)
        // =========================================================================
        // Full-height glazed strips immediately flanking the atrium voids on North facade
        val glassSpineW = 3.8f
        // Left glass curtain spine
        addBox(VOID_MIN_X - glassSpineW, 1.0f, NORTH_Z - 0.3f, VOID_MIN_X, TOWER_HEIGHT - 1.0f, NORTH_Z + 0.2f, CURTAIN_GLASS)
        // Right glass curtain spine
        addBox(VOID_MAX_X, 1.0f, NORTH_Z - 0.3f, VOID_MAX_X + glassSpineW, TOWER_HEIGHT - 1.0f, NORTH_Z + 0.2f, CURTAIN_GLASS)

        // Northeast Corner Glass Curtain Wrap (Wrapping east corner)
        addBox(BLDG_MAX_X - 5.0f, 1.0f, NORTH_Z - 0.3f, BLDG_MAX_X - 0.5f, TOWER_HEIGHT - 1.0f, NORTH_Z + 0.2f, CURTAIN_GLASS)
        addBox(BLDG_MAX_X - 0.3f, 1.0f, NORTH_Z, BLDG_MAX_X + 0.2f, TOWER_HEIGHT - 1.0f, NORTH_Z + 5.0f, CURTAIN_GLASS)

        // =========================================================================
        // 5. PRECAST CONCRETE BRISE-SOLEIL / VERTICAL LOUVER GRID
        // =========================================================================
        // Left tower section louvers (between West Wing and left glass spine)
        val leftLouverCount = 6
        val leftStep = (VOID_MIN_X - glassSpineW - WING_DIVIDER_X - 1.0f) / leftLouverCount
        for (i in 0..leftLouverCount) {
            val lx = WING_DIVIDER_X + 0.5f + i * leftStep
            addBox(lx - 0.2f, 1.0f, NORTH_Z - 0.4f, lx + 0.2f, TOWER_HEIGHT - 1.0f, NORTH_Z + 0.2f, FAÇADE_PRECAST_SHADOW)
        }

        // Right tower section louvers (between right glass spine and northeast corner glass)
        val rightLouverCount = 7
        val rightStep = (BLDG_MAX_X - 5.0f - (VOID_MAX_X + glassSpineW)) / rightLouverCount
        for (i in 0..rightLouverCount) {
            val rx = VOID_MAX_X + glassSpineW + i * rightStep
            addBox(rx - 0.2f, 1.0f, NORTH_Z - 0.4f, rx + 0.2f, TOWER_HEIGHT - 1.0f, NORTH_Z + 0.2f, FAÇADE_PRECAST_SHADOW)
        }

        // Crown louvers on Floors 10-12 bridge above upper portal
        val crownCount = 10
        val crownStep = (VOID_MAX_X - VOID_MIN_X) / crownCount
        for (i in 0..crownCount) {
            val cx = VOID_MIN_X + i * crownStep
            addBox(cx - 0.2f, UPPER_VOID_MAX_Y, NORTH_Z - 0.4f, cx + 0.2f, TOWER_HEIGHT - 1.0f, NORTH_Z + 0.2f, FAÇADE_PRECAST_SHADOW)
        }

        // =========================================================================
        // 6. ROOFTOP MECHANICAL PENTHOUSE SCREEN & OVERHANGING BROW
        // =========================================================================
        // Overhanging Roof Brow Cap
        addBox(WING_DIVIDER_X - 0.8f, TOWER_HEIGHT, NORTH_Z - 1.2f, BLDG_MAX_X + 0.8f, TOWER_HEIGHT + 1.2f, SOUTH_Z + 0.8f, FAÇADE_DARK_CONCRETE)
        // Rooftop gravel deck
        addBox(WING_DIVIDER_X + 0.5f, TOWER_HEIGHT, NORTH_Z + 0.5f, BLDG_MAX_X - 0.5f, TOWER_HEIGHT + 0.1f, SOUTH_Z - 0.5f, ROOF_GRAVEL)
        // Recessed mechanical penthouse box
        addBox(WING_DIVIDER_X + 8.0f, TOWER_HEIGHT + 1.2f, NORTH_Z + 8.0f, BLDG_MAX_X - 8.0f, TOWER_HEIGHT + 4.2f, SOUTH_Z - 8.0f, FAÇADE_DARK_CONCRETE)

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
