package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.building3d.HostelComplexSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural 3D Architectural Reconstruction for Ganga Boys' Hostel (srmap_bldg_15).
 * Features:
 * - Double-height ground pilotis (stilts) with cylindrical columns and fully-glazed lobby
 * - 5-story U-podium base with crisp white slab eyebrows and punched residential windows
 * - 26-story sky-rise tower rising to 80.6m
 * - Mid-level recessed double-height sky lounge louvers
 * - Rooftop elevator headhouses, mechanical chillers, and parapets
 */
object GangaBlockMeshGenerator {

    fun generateMesh(): MeshData {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        var currentIndex: Short = 0

        fun addQuad(
            p1: Vector3, p2: Vector3, p3: Vector3, p4: Vector3,
            color: Color,
            normal: Vector3? = null
        ) {
            val n = normal ?: run {
                val edge1 = p2 - p1
                val edge2 = p3 - p1
                edge1.cross(edge2).normalized()
            }

            val quad = listOf(p1, p2, p3, p4)
            for (v in quad) {
                positions.add(v.x); positions.add(v.y); positions.add(v.z)
                normals.add(n.x); normals.add(n.y); normals.add(n.z)
                colors.add(color.red); colors.add(color.green); colors.add(color.blue); colors.add(1f)
            }

            indices.add(currentIndex)
            indices.add((currentIndex + 1).toShort())
            indices.add((currentIndex + 2).toShort())

            indices.add(currentIndex)
            indices.add((currentIndex + 2).toShort())
            indices.add((currentIndex + 3).toShort())

            currentIndex = (currentIndex + 4).toShort()
        }

        fun addBox(
            minX: Float, maxX: Float,
            minY: Float, maxY: Float,
            minZ: Float, maxZ: Float,
            color: Color
        ) {
            // Front (Z+)
            addQuad(Vector3(minX, minY, maxZ), Vector3(maxX, minY, maxZ), Vector3(maxX, maxY, maxZ), Vector3(minX, maxY, maxZ), color, Vector3(0f, 0f, 1f))
            // Back (Z-)
            addQuad(Vector3(maxX, minY, minZ), Vector3(minX, minY, minZ), Vector3(minX, maxY, minZ), Vector3(maxX, maxY, minZ), color, Vector3(0f, 0f, -1f))
            // Left (X-)
            addQuad(Vector3(minX, minY, minZ), Vector3(minX, minY, maxZ), Vector3(minX, maxY, maxZ), Vector3(minX, maxY, minZ), color, Vector3(-1f, 0f, 0f))
            // Right (X+)
            addQuad(Vector3(maxX, minY, maxZ), Vector3(maxX, minY, minZ), Vector3(maxX, maxY, minZ), Vector3(maxX, maxY, maxZ), color, Vector3(1f, 0f, 0f))
            // Top (Y+)
            addQuad(Vector3(minX, maxY, maxZ), Vector3(maxX, maxY, maxZ), Vector3(maxX, maxY, minZ), Vector3(minX, maxY, minZ), color, Vector3(0f, 1f, 0f))
            // Bottom (Y-)
            addQuad(Vector3(minX, minY, minZ), Vector3(maxX, minY, minZ), Vector3(maxX, minY, maxZ), Vector3(minX, minY, maxZ), color, Vector3(0f, -1f, 0f))
        }

        fun addCylinderColumn(cx: Float, cz: Float, radius: Float, minY: Float, maxY: Float, color: Color) {
            val segments = 8
            for (i in 0 until segments) {
                val a1 = (i * 2 * PI / segments).toFloat()
                val a2 = ((i + 1) * 2 * PI / segments).toFloat()

                val b1 = Vector3(cx + cos(a1) * radius, minY, cz + sin(a1) * radius)
                val b2 = Vector3(cx + cos(a2) * radius, minY, cz + sin(a2) * radius)
                val t1 = Vector3(b1.x, maxY, b1.z)
                val t2 = Vector3(b2.x, maxY, b2.z)

                addQuad(b1, b2, t2, t1, color)
            }
        }

        // ==========================================
        // 1. GROUND PILOTIS LEVEL (Y: 0 to 5.5m)
        // ==========================================
        val pilotisHeight = HostelComplexSpec.GANGA_PILOTIS_HEIGHT
        val columnRadius = 0.45f

        // Structural pilotis grid
        var pz = HostelComplexSpec.GANGA_MIN_Z + 3.0f
        while (pz <= HostelComplexSpec.GANGA_MAX_Z - 3.0f) {
            var px = HostelComplexSpec.GANGA_MIN_X + 3.0f
            while (px <= HostelComplexSpec.GANGA_MAX_X - 3.0f) {
                addCylinderColumn(px, pz, columnRadius, 0f, pilotisHeight, HostelComplexSpec.COLOR_PILOTIS_COLUMNS)
                px += 7.0f
            }
            pz += 7.0f
        }

        // Grand Double-Height Glazed Entrance Lobby on North Facade
        addBox(
            minX = 14.0f, maxX = 34.0f,
            minY = 0f, maxY = pilotisHeight,
            minZ = HostelComplexSpec.GANGA_MIN_Z + 1.5f, maxZ = HostelComplexSpec.GANGA_MIN_Z + 8.5f,
            color = HostelComplexSpec.COLOR_CURTAIN_GLASS
        )

        // Lobby Framing Portal (White)
        addBox(13.6f, 14.2f, 0f, pilotisHeight, HostelComplexSpec.GANGA_MIN_Z + 1.2f, HostelComplexSpec.GANGA_MIN_Z + 8.8f, HostelComplexSpec.COLOR_WHITE_SLAB)
        addBox(33.8f, 34.4f, 0f, pilotisHeight, HostelComplexSpec.GANGA_MIN_Z + 1.2f, HostelComplexSpec.GANGA_MIN_Z + 8.8f, HostelComplexSpec.COLOR_WHITE_SLAB)

        // ==========================================
        // 2. PODIUM BASE (Levels 1 to 5, Y: 5.5m to 16.5m)
        // ==========================================
        // Ground ceiling / First floor transition slab
        addBox(
            minX = HostelComplexSpec.GANGA_MIN_X - 0.4f, maxX = HostelComplexSpec.GANGA_MAX_X + 0.4f,
            minY = pilotisHeight - 0.35f, maxY = pilotisHeight + 0.35f,
            minZ = HostelComplexSpec.GANGA_MIN_Z - 0.4f, maxZ = HostelComplexSpec.GANGA_MAX_Z + 0.4f,
            color = HostelComplexSpec.COLOR_WHITE_SLAB
        )

        // Lower 5-floor residential block (U-shaped base)
        addBox(
            minX = HostelComplexSpec.GANGA_MIN_X, maxX = HostelComplexSpec.GANGA_MAX_X,
            minY = pilotisHeight + 0.35f, maxY = HostelComplexSpec.GANGA_PODIUM_HEIGHT,
            minZ = HostelComplexSpec.GANGA_MIN_Z, maxZ = HostelComplexSpec.GANGA_MAX_Z,
            color = HostelComplexSpec.COLOR_TERRACOTTA_OCHRE
        )

        // Horizontal white slab eyebrows on podium levels
        val podiumFloorHeight = (HostelComplexSpec.GANGA_PODIUM_HEIGHT - pilotisHeight) / 5f
        for (i in 1..4) {
            val slabY = pilotisHeight + i * podiumFloorHeight
            addBox(
                minX = HostelComplexSpec.GANGA_MIN_X - 0.25f, maxX = HostelComplexSpec.GANGA_MAX_X + 0.25f,
                minY = slabY - 0.18f, maxY = slabY + 0.18f,
                minZ = HostelComplexSpec.GANGA_MIN_Z - 0.25f, maxZ = HostelComplexSpec.GANGA_MAX_Z + 0.25f,
                color = HostelComplexSpec.COLOR_WHITE_SLAB
            )
        }

        // Podium Roof Slab / Terrace Deck
        addBox(
            minX = HostelComplexSpec.GANGA_MIN_X - 0.4f, maxX = HostelComplexSpec.GANGA_MAX_X + 0.4f,
            minY = HostelComplexSpec.GANGA_PODIUM_HEIGHT - 0.25f, maxY = HostelComplexSpec.GANGA_PODIUM_HEIGHT + 0.25f,
            minZ = HostelComplexSpec.GANGA_MIN_Z - 0.4f, maxZ = HostelComplexSpec.GANGA_MAX_Z + 0.4f,
            color = HostelComplexSpec.COLOR_WHITE_SLAB
        )
        // Podium Roof Parapet (0.9m)
        addBox(
            minX = HostelComplexSpec.GANGA_MIN_X, maxX = HostelComplexSpec.GANGA_MAX_X,
            minY = HostelComplexSpec.GANGA_PODIUM_HEIGHT + 0.25f, maxY = HostelComplexSpec.GANGA_PODIUM_HEIGHT + 1.15f,
            minZ = HostelComplexSpec.GANGA_MIN_Z, maxZ = HostelComplexSpec.GANGA_MAX_Z,
            color = HostelComplexSpec.COLOR_WHITE_SLAB
        )

        // ==========================================
        // 3. 26-STORY SKY-RISE TOWER (Y: 16.5m to 80.6m)
        // ==========================================
        val towerMinX = HostelComplexSpec.GANGA_TOWER_MIN_X
        val towerMaxX = HostelComplexSpec.GANGA_TOWER_MAX_X
        val towerMinZ = HostelComplexSpec.GANGA_TOWER_MIN_Z
        val towerMaxZ = HostelComplexSpec.GANGA_TOWER_MAX_Z
        val towerHeight = HostelComplexSpec.GANGA_TOTAL_HEIGHT

        // Main Tower Core Mass
        addBox(
            minX = towerMinX, maxX = towerMaxX,
            minY = HostelComplexSpec.GANGA_PODIUM_HEIGHT, maxY = towerHeight,
            minZ = towerMinZ, maxZ = towerMaxZ,
            color = HostelComplexSpec.COLOR_TERRACOTTA_OCHRE
        )

        // Continuous horizontal white floor slab bands (21 upper floors)
        val towerLevels = HostelComplexSpec.GANGA_LEVELS - HostelComplexSpec.GANGA_PODIUM_LEVELS
        val towerFloorHeight = (towerHeight - HostelComplexSpec.GANGA_PODIUM_HEIGHT) / towerLevels
        for (lvl in 1..towerLevels) {
            val slabY = HostelComplexSpec.GANGA_PODIUM_HEIGHT + lvl * towerFloorHeight
            addBox(
                minX = towerMinX - 0.35f, maxX = towerMaxX + 0.35f,
                minY = slabY - 0.2f, maxY = slabY + 0.2f,
                minZ = towerMinZ - 0.35f, maxZ = towerMaxZ + 0.35f,
                color = HostelComplexSpec.COLOR_WHITE_SLAB
            )
        }

        // Vertical Terracotta & Dark Louver Piers along North & South Facades
        var fx = towerMinX + 2.0f
        while (fx <= towerMaxX - 2.0f) {
            // North face vertical fin
            addBox(fx - 0.2f, fx + 0.2f, HostelComplexSpec.GANGA_PODIUM_HEIGHT, towerHeight, towerMinZ - 0.45f, towerMinZ - 0.35f, HostelComplexSpec.COLOR_TERRACOTTA_DARK)
            // South face vertical fin
            addBox(fx - 0.2f, fx + 0.2f, HostelComplexSpec.GANGA_PODIUM_HEIGHT, towerHeight, towerMaxZ + 0.35f, towerMaxZ + 0.45f, HostelComplexSpec.COLOR_TERRACOTTA_DARK)
            fx += 3.2f
        }

        // Punched Bedroom Window Bay Strips (Glazed panels)
        for (lvl in 1..towerLevels) {
            val winY = HostelComplexSpec.GANGA_PODIUM_HEIGHT + (lvl - 1) * towerFloorHeight + 0.6f
            val winHeight = towerFloorHeight - 1.0f
            // North window band
            addBox(towerMinX + 1.0f, towerMaxX - 1.0f, winY, winY + winHeight, towerMinZ - 0.15f, towerMinZ + 0.1f, HostelComplexSpec.COLOR_WINDOW_GLASS)
            // South window band
            addBox(towerMinX + 1.0f, towerMaxX - 1.0f, winY, winY + winHeight, towerMaxZ - 0.1f, towerMaxZ + 0.15f, HostelComplexSpec.COLOR_WINDOW_GLASS)
        }

        // Mid-Level Double-Height Sky Lounges (Levels 14 & 20)
        val skyLounge1Y = HostelComplexSpec.GANGA_PODIUM_HEIGHT + 9 * towerFloorHeight
        addBox(towerMinX + 6f, towerMaxX - 6f, skyLounge1Y, skyLounge1Y + towerFloorHeight * 2, towerMinZ - 0.6f, towerMinZ + 0.2f, HostelComplexSpec.COLOR_CURTAIN_GLASS)

        val skyLounge2Y = HostelComplexSpec.GANGA_PODIUM_HEIGHT + 15 * towerFloorHeight
        addBox(towerMinX + 6f, towerMaxX - 6f, skyLounge2Y, skyLounge2Y + towerFloorHeight * 2, towerMinZ - 0.6f, towerMinZ + 0.2f, HostelComplexSpec.COLOR_CURTAIN_GLASS)

        // ==========================================
        // 4. ROOFTOP SKYLINE & MECHANICAL PENTHOUSE (Y: 80.6m to 83.5m)
        // ==========================================
        // Main Roof Parapet Wall
        addBox(towerMinX - 0.3f, towerMaxX + 0.3f, towerHeight, towerHeight + 1.2f, towerMinZ - 0.3f, towerMaxZ + 0.3f, HostelComplexSpec.COLOR_WHITE_SLAB)

        // Elevator Motor Room / Stair Headhouse
        addBox(
            minX = towerMinX + 10f, maxX = towerMinX + 22f,
            minY = towerHeight, maxY = HostelComplexSpec.GANGA_HEADHOUSE_HEIGHT,
            minZ = towerMinZ + 8f, maxZ = towerMinZ + 24f,
            color = HostelComplexSpec.COLOR_WHITE_SLAB
        )

        // HVAC / Water Chiller Plant Banks on Roof
        addBox(towerMinX + 24f, towerMinX + 31f, towerHeight, towerHeight + 1.8f, towerMinZ + 10f, towerMinZ + 22f, HostelComplexSpec.COLOR_DARK_SPANDREL)

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
