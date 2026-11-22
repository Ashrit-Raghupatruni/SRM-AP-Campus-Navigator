package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural 3D Architectural Reconstruction for Annapurna Mess (srmap_bldg_8).
 * Features:
 * - Articulated 3-tier stepped dining pavilion (West Wing 4.5m, Central Spine 8.5m, North-East Wing 6.5m)
 * - Continuous stepped living green sod/turf roof terrace with crisp white perimeter fascia coping
 * - Continuous clerestory dining ribbon windows and full-height curtain glazing
 * - Signature Coral/Terracotta entrance portal block (#E06D53)
 * - Semicircular stepped granite amphitheater seating ledges wrapping into the grass lawn bowl
 */
object AnnapurnaMessMeshGenerator {

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

        val whiteWall = Color(0xFFF8FAFC)
        val greenRoofLawn = Color(0xFF22C55E) // Living Turf Green
        val greenRoofLawnDark = Color(0xFF16A34A)
        val curtainGlass = Color(0xFF38BDF8) // High-transparency cyan glass
        val coralPortal = Color(0xFFE06D53) // Signature Coral / Terracotta Portal
        val darkGranite = Color(0xFF1E293B) // Dark granite amphitheater steps
        val lawnBowl = Color(0xFF4ADE80) // Amphitheater inner lawn

        // ==========================================
        // 1. TIER 1: WEST LOW PAVILION (Y: 0 to 4.5m)
        // Facing Central Promenade, X: 168.8m to 184m, Z: -268m to -235m
        // ==========================================
        addBox(168.8f, 184.0f, 0f, 4.5f, -268.0f, -235.0f, whiteWall)
        // Glazing ribbons on West & South facades
        addBox(168.6f, 168.85f, 0.8f, 3.8f, -266.0f, -237.0f, curtainGlass)
        addBox(170.0f, 182.0f, 0.8f, 3.8f, -235.2f, -234.9f, curtainGlass)
        // Green Living Sod Roof on Tier 1
        addBox(168.6f, 184.2f, 4.5f, 4.65f, -268.2f, -234.8f, greenRoofLawn)
        // White perimeter fascia band
        addBox(168.4f, 184.4f, 4.45f, 4.8f, -268.4f, -268.1f, whiteWall)
        addBox(168.4f, 168.7f, 4.45f, 4.8f, -268.4f, -234.6f, whiteWall)

        // ==========================================
        // 2. TIER 2: CENTRAL MAIN DINING SPINE (Y: 0 to 8.5m)
        // X: 182m to 204m, Z: -290m to -245m
        // ==========================================
        addBox(182.0f, 204.0f, 0f, 8.5f, -290.0f, -245.0f, whiteWall)
        // Clerestory Ribbon Windows along upper floor (Y: 5.2m to 7.8m)
        addBox(181.8f, 182.1f, 5.2f, 7.8f, -288.0f, -247.0f, curtainGlass)
        addBox(203.9f, 204.2f, 5.2f, 7.8f, -288.0f, -247.0f, curtainGlass)
        addBox(184.0f, 202.0f, 5.2f, 7.8f, -245.2f, -244.8f, curtainGlass)
        // Lower ground glass windows (Y: 0.8m to 4.2m)
        addBox(184.0f, 202.0f, 0.8f, 4.2f, -245.2f, -244.8f, curtainGlass)
        // Green Living Sod Roof on Central Spine (Y: 8.5m to 8.68m)
        addBox(181.8f, 204.2f, 8.5f, 8.68f, -290.2f, -244.8f, greenRoofLawnDark)
        // White roof parapet fascia
        addBox(181.6f, 204.4f, 8.5f, 9.1f, -290.4f, -290.0f, whiteWall)
        addBox(181.6f, 204.4f, 8.5f, 9.1f, -245.0f, -244.6f, whiteWall)
        addBox(181.6f, 182.0f, 8.5f, 9.1f, -290.4f, -244.6f, whiteWall)
        addBox(204.0f, 204.4f, 8.5f, 9.1f, -290.4f, -244.6f, whiteWall)

        // Rooftop Ventilation / Mechanical Penthouse on Spine
        addBox(190.0f, 196.0f, 8.68f, 11.2f, -275.0f, -265.0f, whiteWall)

        // ==========================================
        // 3. TIER 3: NORTH-EAST DINING HALL (Y: 0 to 6.5m)
        // X: 198m to 216.7m, Z: -299.3m to -260m
        // ==========================================
        addBox(198.0f, 216.7f, 0f, 6.5f, -299.3f, -260.0f, whiteWall)
        // Upper clerestory windows
        addBox(198.0f, 215.0f, 4.0f, 6.0f, -299.5f, -299.1f, curtainGlass)
        addBox(216.5f, 216.9f, 1.2f, 5.8f, -297.0f, -262.0f, curtainGlass)
        // Green Living Sod Roof
        addBox(197.8f, 216.9f, 6.5f, 6.66f, -299.5f, -259.8f, greenRoofLawn)
        // White perimeter fascia
        addBox(197.6f, 217.1f, 6.5f, 7.0f, -299.7f, -299.2f, whiteWall)
        addBox(216.6f, 217.1f, 6.5f, 7.0f, -299.7f, -259.6f, whiteWall)

        // ==========================================
        // 4. SIGNATURE CORAL / TERRACOTTA ENTRANCE PORTAL
        // South-West Entrance Box, X: 168.8m to 175.0m, Z: -268.0m to -258.0m, H: 5.5m
        // ==========================================
        addBox(168.6f, 175.2f, 0f, 5.5f, -268.2f, -258.0f, coralPortal)
        // Portal glazed entryway
        addBox(168.4f, 168.7f, 0f, 3.8f, -266.0f, -260.0f, curtainGlass)

        // ==========================================
        // 5. SEMICIRCULAR STEPPED AMPHITHEATER & LAWN BOWL
        // Wrapping in front of the South-East courtyard
        // ==========================================
        val amphitheaterCenterX = 192.0f
        val amphitheaterCenterZ = -238.0f

        // Stepped Granite Seating Tiers (3 Curved Tiers)
        val stepTiers = 3
        val segments = 12
        for (tier in 1..stepTiers) {
            val r = 16.0f + tier * 1.8f
            val y = 0.04f + tier * 0.35f

            for (i in 0 until segments) {
                val a1 = (-PI * 0.1f + i * (PI * 0.8f / segments)).toFloat()
                val a2 = (-PI * 0.1f + (i + 1) * (PI * 0.8f / segments)).toFloat()

                val p1 = Vector3(amphitheaterCenterX + cos(a1) * r, y, amphitheaterCenterZ + sin(a1) * r)
                val p2 = Vector3(amphitheaterCenterX + cos(a2) * r, y, amphitheaterCenterZ + sin(a2) * r)
                val p3 = Vector3(amphitheaterCenterX + cos(a2) * (r + 1.2f), y, amphitheaterCenterZ + sin(a2) * (r + 1.2f))
                val p4 = Vector3(amphitheaterCenterX + cos(a1) * (r + 1.2f), y, amphitheaterCenterZ + sin(a1) * (r + 1.2f))

                addQuad(p1, p2, p3, p4, darkGranite, Vector3(0f, 1f, 0f))
            }
        }

        // Inner Manicured Lawn Bowl
        val innerR = 15.5f
        for (i in 0 until segments) {
            val a1 = (-PI * 0.1f + i * (PI * 0.8f / segments)).toFloat()
            val a2 = (-PI * 0.1f + (i + 1) * (PI * 0.8f / segments)).toFloat()

            val p1 = Vector3(amphitheaterCenterX, 0.04f, amphitheaterCenterZ)
            val p2 = Vector3(amphitheaterCenterX + cos(a1) * innerR, 0.04f, amphitheaterCenterZ + sin(a1) * innerR)
            val p3 = Vector3(amphitheaterCenterX + cos(a2) * innerR, 0.04f, amphitheaterCenterZ + sin(a2) * innerR)

            addQuad(p1, p2, p3, p1, lawnBowl, Vector3(0f, 1f, 0f))
        }

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
