package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.building3d.HostelComplexSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural 3D Architectural Reconstruction for Vedavathi Boys' Hostel (srmap_bldg_14).
 * Features:
 * - Double-height ground pilotis (colonnade stilts) lifting the residential mass
 * - Distinct U-shaped residential footprint wrapping an open interior landscaped garden courtyard
 * - 5-story lower podium base with continuous horizontal white floor bands
 * - 15-story residential mid-rise block rising to 46.5m
 * - Rooftop elevator headhouse, parapets, and solar arrays
 */
object VedavathiBlockMeshGenerator {

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
        val pilotisHeight = HostelComplexSpec.VEDAVATHI_PILOTIS_HEIGHT
        val columnRadius = 0.45f

        // Structural columns under North Wing, West Wing, and East Wing
        // North Wing
        var px = HostelComplexSpec.VEDAVATHI_MIN_X + 3.0f
        while (px <= HostelComplexSpec.VEDAVATHI_MAX_X - 3.0f) {
            addCylinderColumn(px, HostelComplexSpec.VEDAVATHI_MIN_Z + 3.0f, columnRadius, 0f, pilotisHeight, HostelComplexSpec.COLOR_PILOTIS_COLUMNS)
            addCylinderColumn(px, HostelComplexSpec.VEDAVATHI_COURTYARD_MIN_Z - 2.0f, columnRadius, 0f, pilotisHeight, HostelComplexSpec.COLOR_PILOTIS_COLUMNS)
            px += 7.0f
        }
        // West Wing
        var pz = HostelComplexSpec.VEDAVATHI_COURTYARD_MIN_Z
        while (pz <= HostelComplexSpec.VEDAVATHI_MAX_Z - 3.0f) {
            addCylinderColumn(HostelComplexSpec.VEDAVATHI_MIN_X + 3.0f, pz, columnRadius, 0f, pilotisHeight, HostelComplexSpec.COLOR_PILOTIS_COLUMNS)
            addCylinderColumn(HostelComplexSpec.VEDAVATHI_COURTYARD_MIN_X - 2.0f, pz, columnRadius, 0f, pilotisHeight, HostelComplexSpec.COLOR_PILOTIS_COLUMNS)
            pz += 7.0f
        }
        // East Wing
        pz = HostelComplexSpec.VEDAVATHI_COURTYARD_MIN_Z
        while (pz <= HostelComplexSpec.VEDAVATHI_MAX_Z - 3.0f) {
            addCylinderColumn(HostelComplexSpec.VEDAVATHI_COURTYARD_MAX_X + 2.0f, pz, columnRadius, 0f, pilotisHeight, HostelComplexSpec.COLOR_PILOTIS_COLUMNS)
            addCylinderColumn(HostelComplexSpec.VEDAVATHI_MAX_X - 3.0f, pz, columnRadius, 0f, pilotisHeight, HostelComplexSpec.COLOR_PILOTIS_COLUMNS)
            pz += 7.0f
        }

        // Glazed Entrance Lobby on South Facade facing the central fountain plaza
        addBox(
            minX = 14.0f, maxX = 30.0f,
            minY = 0f, maxY = pilotisHeight,
            minZ = HostelComplexSpec.VEDAVATHI_MAX_Z - 5.5f, maxZ = HostelComplexSpec.VEDAVATHI_MAX_Z,
            color = HostelComplexSpec.COLOR_CURTAIN_GLASS
        )

        // ==========================================
        // 2. U-SHAPED RESIDENTIAL MASSING
        // ==========================================
        // North Spine Block: spans X in [MIN_X, MAX_X], Z in [MIN_Z, COURTYARD_MIN_Z]
        val northMinZ = HostelComplexSpec.VEDAVATHI_MIN_Z
        val northMaxZ = HostelComplexSpec.VEDAVATHI_COURTYARD_MIN_Z

        // West Flank Block: spans X in [MIN_X, COURTYARD_MIN_X], Z in [COURTYARD_MIN_Z, MAX_Z]
        val westMinX = HostelComplexSpec.VEDAVATHI_MIN_X
        val westMaxX = HostelComplexSpec.VEDAVATHI_COURTYARD_MIN_X

        // East Flank Block: spans X in [COURTYARD_MAX_X, MAX_X], Z in [COURTYARD_MIN_Z, MAX_Z]
        val eastMinX = HostelComplexSpec.VEDAVATHI_COURTYARD_MAX_X
        val eastMaxX = HostelComplexSpec.VEDAVATHI_MAX_X

        val flankMaxZ = HostelComplexSpec.VEDAVATHI_MAX_Z

        // Helper to add U-shaped box
        fun addUBlock(minY: Float, maxY: Float, color: Color) {
            // North Wing
            addBox(HostelComplexSpec.VEDAVATHI_MIN_X, HostelComplexSpec.VEDAVATHI_MAX_X, minY, maxY, northMinZ, northMaxZ, color)
            // West Wing
            addBox(westMinX, westMaxX, minY, maxY, northMaxZ, flankMaxZ, color)
            // East Wing
            addBox(eastMinX, eastMaxX, minY, maxY, northMaxZ, flankMaxZ, color)
        }

        // Lower Podium Slabs & Eyebrows (5 Floors: Y in 5.5m to 16.5m)
        addUBlock(pilotisHeight, HostelComplexSpec.VEDAVATHI_PODIUM_HEIGHT, HostelComplexSpec.COLOR_TERRACOTTA_OCHRE)

        val podiumFloorHeight = (HostelComplexSpec.VEDAVATHI_PODIUM_HEIGHT - pilotisHeight) / 5f
        for (i in 0..5) {
            val slabY = pilotisHeight + i * podiumFloorHeight
            // White horizontal slab band
            addBox(HostelComplexSpec.VEDAVATHI_MIN_X - 0.25f, HostelComplexSpec.VEDAVATHI_MAX_X + 0.25f, slabY - 0.18f, slabY + 0.18f, northMinZ - 0.25f, northMaxZ, HostelComplexSpec.COLOR_WHITE_SLAB)
            addBox(westMinX - 0.25f, westMaxX + 0.25f, slabY - 0.18f, slabY + 0.18f, northMaxZ, flankMaxZ + 0.25f, HostelComplexSpec.COLOR_WHITE_SLAB)
            addBox(eastMinX - 0.25f, eastMaxX + 0.25f, slabY - 0.18f, slabY + 0.18f, northMaxZ, flankMaxZ + 0.25f, HostelComplexSpec.COLOR_WHITE_SLAB)
        }

        // ==========================================
        // 3. UPPER RESIDENTIAL SLAB BLOCK (Levels 6 to 15, Y: 16.5m to 46.5m)
        // ==========================================
        val midRiseHeight = HostelComplexSpec.VEDAVATHI_TOTAL_HEIGHT
        val upperLevels = HostelComplexSpec.VEDAVATHI_LEVELS - HostelComplexSpec.VEDAVATHI_PODIUM_LEVELS
        val upperFloorHeight = (midRiseHeight - HostelComplexSpec.VEDAVATHI_PODIUM_HEIGHT) / upperLevels

        // Upper U-shaped residential tower
        addUBlock(HostelComplexSpec.VEDAVATHI_PODIUM_HEIGHT, midRiseHeight, HostelComplexSpec.COLOR_TERRACOTTA_OCHRE)

        // Upper Floor Horizontal White Slabs
        for (lvl in 1..upperLevels) {
            val slabY = HostelComplexSpec.VEDAVATHI_PODIUM_HEIGHT + lvl * upperFloorHeight
            addBox(HostelComplexSpec.VEDAVATHI_MIN_X - 0.25f, HostelComplexSpec.VEDAVATHI_MAX_X + 0.25f, slabY - 0.18f, slabY + 0.18f, northMinZ - 0.25f, northMaxZ, HostelComplexSpec.COLOR_WHITE_SLAB)
            addBox(westMinX - 0.25f, westMaxX + 0.25f, slabY - 0.18f, slabY + 0.18f, northMaxZ, flankMaxZ + 0.25f, HostelComplexSpec.COLOR_WHITE_SLAB)
            addBox(eastMinX - 0.25f, eastMaxX + 0.25f, slabY - 0.18f, slabY + 0.18f, northMaxZ, flankMaxZ + 0.25f, HostelComplexSpec.COLOR_WHITE_SLAB)
        }

        // Punched Bedroom Window Glazing Bands
        for (lvl in 1..HostelComplexSpec.VEDAVATHI_LEVELS) {
            val floorY = pilotisHeight + (lvl - 1) * upperFloorHeight + 0.6f
            val winH = upperFloorHeight - 1.0f

            // North facade windows
            addBox(HostelComplexSpec.VEDAVATHI_MIN_X + 2f, HostelComplexSpec.VEDAVATHI_MAX_X - 2f, floorY, floorY + winH, northMinZ - 0.15f, northMinZ + 0.1f, HostelComplexSpec.COLOR_WINDOW_GLASS)
            // East facade windows
            addBox(eastMaxX - 0.1f, eastMaxX + 0.15f, floorY, floorY + winH, northMinZ + 2f, flankMaxZ - 2f, HostelComplexSpec.COLOR_WINDOW_GLASS)
            // West facade windows
            addBox(westMinX - 0.15f, westMinX + 0.1f, floorY, floorY + winH, northMinZ + 2f, flankMaxZ - 2f, HostelComplexSpec.COLOR_WINDOW_GLASS)
            // Courtyard facing windows
            addBox(westMaxX - 0.1f, westMaxX + 0.15f, floorY, floorY + winH, northMaxZ + 2f, flankMaxZ - 2f, HostelComplexSpec.COLOR_WINDOW_GLASS)
            addBox(eastMinX - 0.15f, eastMinX + 0.1f, floorY, floorY + winH, northMaxZ + 2f, flankMaxZ - 2f, HostelComplexSpec.COLOR_WINDOW_GLASS)
        }

        // Mid-Level Double-Height Sky Lounges (Level 10)
        val skyLoungeY = HostelComplexSpec.VEDAVATHI_PODIUM_HEIGHT + 5 * upperFloorHeight
        addBox(HostelComplexSpec.VEDAVATHI_MIN_X + 12f, HostelComplexSpec.VEDAVATHI_MAX_X - 12f, skyLoungeY, skyLoungeY + upperFloorHeight * 2, northMinZ - 0.45f, northMinZ + 0.2f, HostelComplexSpec.COLOR_CURTAIN_GLASS)

        // ==========================================
        // 4. ROOFTOP STRUCTURES & SOLAR ARRAYS
        // ==========================================
        // Rooftop Parapet
        addBox(HostelComplexSpec.VEDAVATHI_MIN_X - 0.2f, HostelComplexSpec.VEDAVATHI_MAX_X + 0.2f, midRiseHeight, midRiseHeight + 1.1f, northMinZ - 0.2f, northMaxZ + 0.2f, HostelComplexSpec.COLOR_WHITE_SLAB)
        addBox(westMinX - 0.2f, westMaxX + 0.2f, midRiseHeight, midRiseHeight + 1.1f, northMaxZ, flankMaxZ + 0.2f, HostelComplexSpec.COLOR_WHITE_SLAB)
        addBox(eastMinX - 0.2f, eastMaxX + 0.2f, midRiseHeight, midRiseHeight + 1.1f, northMaxZ, flankMaxZ + 0.2f, HostelComplexSpec.COLOR_WHITE_SLAB)

        // Elevator Headhouse on North Wing
        addBox(
            minX = 15.0f, maxX = 29.0f,
            minY = midRiseHeight, maxY = HostelComplexSpec.VEDAVATHI_HEADHOUSE_HEIGHT,
            minZ = northMinZ + 3.0f, maxZ = northMaxZ - 3.0f,
            color = HostelComplexSpec.COLOR_WHITE_SLAB
        )

        // Photovoltaic Solar Arrays on East & West Rooftops
        addBox(westMinX + 2f, westMaxX - 2f, midRiseHeight + 0.4f, midRiseHeight + 0.65f, northMaxZ + 4f, flankMaxZ - 4f, HostelComplexSpec.COLOR_DARK_SPANDREL)
        addBox(eastMinX + 2f, eastMaxX - 2f, midRiseHeight + 0.4f, midRiseHeight + 0.65f, northMaxZ + 4f, flankMaxZ - 4f, HostelComplexSpec.COLOR_DARK_SPANDREL)

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
