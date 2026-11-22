package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural 3D Architectural Generator for Gate 1 Main Entrance Complex.
 *
 * Evidence-Driven Reconstruction based on official SRM University-AP photography:
 * 1. Monumental 42-Meter Cantilever Canopy: Deep anthracite gray (#1E293B) with warm wooden soffit (#78350F).
 * 2. 16.5-Meter Soaring Sculptural Pylon Fin: Knife-edge tapered dark stone fin with circular golden SRM insignia.
 * 3. Central Glazed Security Cabin: Octagonal/rectangular security control cabin with aluminum frame.
 * 4. Dual Driveway Lanes & Planter Medians: Manicured approach medians, curbs, and security boom posts.
 */
object Gate1EntranceMeshGenerator {

    // Anchor at the southern entrance boulevard terminal
    private const val CENTER_X = 51.64f
    private const val CENTER_Z = 411.21f

    // Canopy dimensions
    private const val CANOPY_WIDTH_X = 44.0f
    private const val CANOPY_DEPTH_Z = 12.0f
    private const val CANOPY_CLEARANCE_Y = 5.2f
    private const val CANOPY_THICKNESS = 1.2f
    private const val PYLON_HEIGHT = 16.5f

    // Authentic architectural materials
    private val ANTHRACITE_DARK = Color(0xFF1E293B)
    private val ANTHRACITE_MID = Color(0xFF334155)
    private val WOOD_SOFFIT = Color(0xFF78350F)
    private val PYLON_STONE = Color(0xFF0F172A)
    private val GOLD_EMBLEM = Color(0xFFF59E0B)
    private val CABIN_GLASS = Color(0xFF38BDF8)
    private val CABIN_MULLION = Color(0xFF1E293B)
    private val CURB_GRANITE = Color(0xFF64748B)
    private val MEDIAN_GRASS = Color(0xFF15803D)
    private val MARIGOLD_BORDER = Color(0xFFFBBF24)
    private val ROAD_ASPHALT = Color(0xFF334155)

    fun generateGate1Mesh(): MeshData {
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

            // Top
            addQuad(minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0f, 1f, 0f, 1.0f)
            // Bottom (Underside)
            addQuad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0f, -1f, 0f, 0.7f)
            // Front (+Z)
            addQuad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0f, 0f, 1f, 0.95f)
            // Back (-Z)
            addQuad(maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0f, 0f, -1f, 0.85f)
            // Left (-X)
            addQuad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1f, 0f, 0f, 0.9f)
            // Right (+X)
            addQuad(maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1f, 0f, 0f, 0.9f)
        }

        // 1. Entrance Apron Pavement Slab
        addBox(
            CENTER_X - CANOPY_WIDTH_X / 2 - 4.0f, 0.01f, CENTER_Z - CANOPY_DEPTH_Z / 2 - 6.0f,
            CENTER_X + CANOPY_WIDTH_X / 2 + 4.0f, 0.08f, CENTER_Z + CANOPY_DEPTH_Z / 2 + 6.0f,
            ROAD_ASPHALT
        )

        // 2. Main Cantilevered Canopy Roof Slab (H = 5.2m to 6.4m)
        val canopyHalfW = CANOPY_WIDTH_X / 2
        val canopyHalfD = CANOPY_DEPTH_Z / 2

        // Main canopy frame
        addBox(
            CENTER_X - canopyHalfW, CANOPY_CLEARANCE_Y, CENTER_Z - canopyHalfD,
            CENTER_X + canopyHalfW, CANOPY_CLEARANCE_Y + CANOPY_THICKNESS, CENTER_Z + canopyHalfD,
            ANTHRACITE_DARK
        )

        // Wood-panel soffit inlay on underside
        addBox(
            CENTER_X - canopyHalfW + 1.2f, CANOPY_CLEARANCE_Y - 0.05f, CENTER_Z - canopyHalfD + 0.8f,
            CENTER_X + canopyHalfW - 1.2f, CANOPY_CLEARANCE_Y, CENTER_Z + canopyHalfD - 0.8f,
            WOOD_SOFFIT
        )

        // 3. Flanking Structural Pillars
        val pillarW = 2.4f
        val pillarD = 4.0f
        // Left main pillar
        addBox(
            CENTER_X - canopyHalfW + 1.5f, 0f, CENTER_Z - pillarD / 2,
            CENTER_X - canopyHalfW + 1.5f + pillarW, CANOPY_CLEARANCE_Y, CENTER_Z + pillarD / 2,
            ANTHRACITE_MID
        )
        // Right main pillar
        addBox(
            CENTER_X + canopyHalfW - 1.5f - pillarW, 0f, CENTER_Z - pillarD / 2,
            CENTER_X + canopyHalfW - 1.5f, CANOPY_CLEARANCE_Y, CENTER_Z + pillarD / 2,
            ANTHRACITE_MID
        )

        // 4. Central Security Cabin Complex (Between inbound and outbound driveways)
        val cabinW = 6.4f
        val cabinD = 5.0f
        val cabinH = 3.6f

        // Cabin base plinth
        addBox(
            CENTER_X - cabinW / 2, 0f, CENTER_Z - cabinD / 2,
            CENTER_X + cabinW / 2, 0.4f, CENTER_Z + cabinD / 2,
            CURB_GRANITE
        )
        // Cabin glazed walls
        addBox(
            CENTER_X - cabinW / 2 + 0.3f, 0.4f, CENTER_Z - cabinD / 2 + 0.3f,
            CENTER_X + cabinW / 2 - 0.3f, cabinH, CENTER_Z + cabinD / 2 - 0.3f,
            CABIN_GLASS
        )
        // Cabin ceiling cap
        addBox(
            CENTER_X - cabinW / 2, cabinH, CENTER_Z - cabinD / 2,
            CENTER_X + cabinW / 2, cabinH + 0.4f, CENTER_Z + cabinD / 2,
            CABIN_MULLION
        )

        // 5. Towering Sculptural Pylon Fin (Soaring to 16.5m)
        // Rising from central cabin through canopy with chamfered knife-edge silhouette
        val pylonW = 3.2f
        val pylonD = 4.5f
        addBox(
            CENTER_X - pylonW / 2, CANOPY_CLEARANCE_Y, CENTER_Z - pylonD / 2,
            CENTER_X + pylonW / 2, PYLON_HEIGHT - 3.0f, CENTER_Z + pylonD / 2,
            PYLON_STONE
        )
        // Tapered fin top
        addBox(
            CENTER_X - pylonW / 4, PYLON_HEIGHT - 3.0f, CENTER_Z - pylonD / 3,
            CENTER_X + pylonW / 4, PYLON_HEIGHT, CENTER_Z + pylonD / 3,
            PYLON_STONE
        )

        // Golden SRM Insignia / Round Seal Disk on the Pylon Face (+Z direction)
        addBox(
            CENTER_X - 1.0f, PYLON_HEIGHT - 5.5f, CENTER_Z + pylonD / 2 + 0.05f,
            CENTER_X + 1.0f, PYLON_HEIGHT - 3.5f, CENTER_Z + pylonD / 2 + 0.25f,
            GOLD_EMBLEM
        )

        // Golden "SRM UNIVERSITY - AP" Plinth Inscription at driveway entrance
        addBox(
            CENTER_X - 2.8f, 0.1f, CENTER_Z + canopyHalfD + 2.5f,
            CENTER_X + 2.8f, 0.9f, CENTER_Z + canopyHalfD + 3.2f,
            ANTHRACITE_DARK
        )
        addBox(
            CENTER_X - 2.2f, 0.3f, CENTER_Z + canopyHalfD + 3.2f,
            CENTER_X + 2.2f, 0.7f, CENTER_Z + canopyHalfD + 3.3f,
            GOLD_EMBLEM
        )

        // 6. Approach Planter Medians with Floral Borders
        // Front elliptical entrance planter
        addBox(
            CENTER_X - 3.5f, 0.1f, CENTER_Z + canopyHalfD + 4.5f,
            CENTER_X + 3.5f, 0.5f, CENTER_Z + canopyHalfD + 8.5f,
            CURB_GRANITE
        )
        addBox(
            CENTER_X - 3.1f, 0.5f, CENTER_Z + canopyHalfD + 4.9f,
            CENTER_X + 3.1f, 0.7f, CENTER_Z + canopyHalfD + 8.1f,
            MEDIAN_GRASS
        )
        addBox(
            CENTER_X - 2.8f, 0.7f, CENTER_Z + canopyHalfD + 5.2f,
            CENTER_X + 2.8f, 0.85f, CENTER_Z + canopyHalfD + 7.8f,
            MARIGOLD_BORDER
        )

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
