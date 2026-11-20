package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.building3d.HostelComplexSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural 3D Mesh Generator for the Central Civic Plaza & Fountain between
 * Ganga Boys' Hostel and Vedavathi Boys' Hostel.
 * Features:
 * - Multi-tiered dark granite water fountain with vertical jet sprayers
 * - Azure reflective water pool surface
 * - Surrounding decorative floral planter parterres with marigold and shrub accents
 */
object HostelPlazaMeshGenerator {

    fun generatePlazaMesh(): MeshData {
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

        val cx = HostelComplexSpec.FOUNTAIN_CENTER_X
        val cz = HostelComplexSpec.FOUNTAIN_CENTER_Z
        val halfSize = HostelComplexSpec.FOUNTAIN_SIZE / 2f

        // 1. Lower Fountain Granite Basin Coping (Height 0.55m)
        addBox(
            minX = cx - halfSize, maxX = cx + halfSize,
            minY = 0.04f, maxY = 0.55f,
            minZ = cz - halfSize, maxZ = cz + halfSize,
            color = HostelComplexSpec.COLOR_FOUNTAIN_GRANITE
        )

        // 2. Water Pool Surface (Height 0.45m)
        val poolMargin = 0.45f
        addQuad(
            Vector3(cx - halfSize + poolMargin, 0.45f, cz + halfSize - poolMargin),
            Vector3(cx + halfSize - poolMargin, 0.45f, cz + halfSize - poolMargin),
            Vector3(cx + halfSize - poolMargin, 0.45f, cz - halfSize + poolMargin),
            Vector3(cx - halfSize + poolMargin, 0.45f, cz - halfSize + poolMargin),
            color = HostelComplexSpec.COLOR_FOUNTAIN_WATER,
            normal = Vector3(0f, 1f, 0f)
        )

        // 3. Central Tier Pedestal (Height 1.1m)
        val tierHalf = 1.6f
        addBox(
            minX = cx - tierHalf, maxX = cx + tierHalf,
            minY = 0.45f, maxY = 1.1f,
            minZ = cz - tierHalf, maxZ = cz + tierHalf,
            color = HostelComplexSpec.COLOR_FOUNTAIN_GRANITE
        )

        // Upper Water Jet Nozzle (Height 1.7m)
        addBox(
            minX = cx - 0.25f, maxX = cx + 0.25f,
            minY = 1.1f, maxY = 1.75f,
            minZ = cz - 0.25f, maxZ = cz + 0.25f,
            color = Color(0xFFBAE6FD) // Frosted water jet
        )

        // 4. Four Flanking Floral Planter Boxes (Corners of the Plaza)
        val planterColor = Color(0xFF15803D) // Lush shrub foliage
        val marigoldAccent = Color(0xFFF59E0B) // Vibrant gold/orange marigolds
        val curbColor = Color(0xFF475569) // Slate stone curb

        val planterOffsets = listOf(
            Pair(-5.5f, -5.5f),
            Pair(5.5f, -5.5f),
            Pair(-5.5f, 5.5f),
            Pair(5.5f, 5.5f)
        )

        for (offset in planterOffsets) {
            val px = cx + offset.first
            val pz = cz + offset.second

            // Curb box
            addBox(px - 1.6f, px + 1.6f, 0.04f, 0.35f, pz - 1.6f, pz + 1.6f, curbColor)
            // Green shrub top
            addBox(px - 1.4f, px + 1.4f, 0.35f, 0.65f, pz - 1.4f, pz + 1.4f, planterColor)
            // Marigold flowering center
            addBox(px - 0.8f, px + 0.8f, 0.65f, 0.78f, pz - 0.8f, pz + 0.8f, marigoldAccent)
        }

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
