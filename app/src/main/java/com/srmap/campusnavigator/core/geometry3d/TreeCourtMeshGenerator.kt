package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural 3D Mesh Generator for the authentic Tree Court Area landscape (way/1472466587):
 * - Centerpiece mature spreading shade tree in an elevated circular granite planter ring
 * - Tiered manicured boxwood hedges (#15803D) and flowering border beds (#F59E0B)
 * - Contrast rectilinear civic stone paving bands dividing the central promenade
 */
object TreeCourtMeshGenerator {

    fun generateTreeCourtMesh(): MeshData {
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

        val treeCenterX = 146.0f
        val treeCenterZ = -244.0f

        val darkStone = Color(0xFF1E293B)
        val boxHedgeGreen = Color(0xFF15803D)
        val flowerAmber = Color(0xFFF59E0B)
        val trunkBrown = Color(0xFF6B4423)
        val deepLeafGreen = Color(0xFF166534)
        val brightLeafGreen = Color(0xFF22C55E)

        // 1. Elevated Circular Granite Planter Ring (Radius 3.8m, Height 0.55m)
        val segments = 16
        val rOuter = 4.2f
        val rInner = 3.6f
        val planterH = 0.55f

        for (i in 0 until segments) {
            val a1 = (i * 2 * PI / segments).toFloat()
            val a2 = ((i + 1) * 2 * PI / segments).toFloat()

            val p1 = Vector3(treeCenterX + cos(a1) * rOuter, 0.04f, treeCenterZ + sin(a1) * rOuter)
            val p2 = Vector3(treeCenterX + cos(a2) * rOuter, 0.04f, treeCenterZ + sin(a2) * rOuter)
            val p3 = Vector3(treeCenterX + cos(a2) * rOuter, planterH, treeCenterZ + sin(a2) * rOuter)
            val p4 = Vector3(treeCenterX + cos(a1) * rOuter, planterH, treeCenterZ + sin(a1) * rOuter)

            // Outer ring face
            addQuad(p1, p2, p3, p4, darkStone)

            // Planter top curb
            val t1 = Vector3(treeCenterX + cos(a1) * rInner, planterH, treeCenterZ + sin(a1) * rInner)
            val t2 = Vector3(treeCenterX + cos(a2) * rInner, planterH, treeCenterZ + sin(a2) * rInner)
            addQuad(p4, p3, t2, t1, darkStone, Vector3(0f, 1f, 0f))

            // Soil / ground inside planter
            val cPt = Vector3(treeCenterX, planterH - 0.1f, treeCenterZ)
            val s1 = Vector3(treeCenterX + cos(a1) * rInner, planterH - 0.1f, treeCenterZ + sin(a1) * rInner)
            val s2 = Vector3(treeCenterX + cos(a2) * rInner, planterH - 0.1f, treeCenterZ + sin(a2) * rInner)
            addQuad(cPt, s1, s2, cPt, Color(0xFF3F2E1E), Vector3(0f, 1f, 0f))
        }

        // 2. Centerpiece Mature Spreading Canopy Tree
        // Heavy Sturdy Trunk (Radius 0.45m, Height 3.2m)
        val trunkSegments = 8
        for (i in 0 until trunkSegments) {
            val a1 = (i * 2 * PI / trunkSegments).toFloat()
            val a2 = ((i + 1) * 2 * PI / trunkSegments).toFloat()

            val b1 = Vector3(treeCenterX + cos(a1) * 0.45f, planterH, treeCenterZ + sin(a1) * 0.45f)
            val b2 = Vector3(treeCenterX + cos(a2) * 0.45f, planterH, treeCenterZ + sin(a2) * 0.45f)
            val t1 = Vector3(b1.x, planterH + 3.2f, b1.z)
            val t2 = Vector3(b2.x, planterH + 3.2f, b2.z)

            addQuad(b1, b2, t2, t1, trunkBrown)
        }

        // Spreading Layered Umbrella Canopy (Radius 4.8m, Height 4.5m)
        val crownBaseY = planterH + 2.8f
        val crownMidY = crownBaseY + 2.2f
        val crownTopY = crownMidY + 1.8f
        val crownR = 4.8f

        val canopyApex = Vector3(treeCenterX, crownTopY, treeCenterZ)
        for (i in 0 until segments) {
            val a1 = (i * 2 * PI / segments).toFloat()
            val a2 = ((i + 1) * 2 * PI / segments).toFloat()

            val c1 = Vector3(treeCenterX + cos(a1) * crownR, crownBaseY, treeCenterZ + sin(a1) * crownR)
            val c2 = Vector3(treeCenterX + cos(a2) * crownR, crownBaseY, treeCenterZ + sin(a2) * crownR)
            val m1 = Vector3(treeCenterX + cos(a1) * (crownR * 0.75f), crownMidY, treeCenterZ + sin(a1) * (crownR * 0.75f))
            val m2 = Vector3(treeCenterX + cos(a2) * (crownR * 0.75f), crownMidY, treeCenterZ + sin(a2) * (crownR * 0.75f))

            val leafColor = if (i % 2 == 0) deepLeafGreen else brightLeafGreen

            // Lower skirt quad
            addQuad(c1, c2, m2, m1, leafColor)

            // Upper crown triangle to apex
            val triNormal = (m2 - m1).cross(canopyApex - m1).normalized()
            val tri = listOf(m1, m2, canopyApex)
            for (v in tri) {
                positions.add(v.x); positions.add(v.y); positions.add(v.z)
                normals.add(triNormal.x); normals.add(triNormal.y); normals.add(triNormal.z)
                colors.add(leafColor.red); colors.add(leafColor.green); colors.add(leafColor.blue); colors.add(1f)
            }
            indices.add(currentIndex)
            indices.add((currentIndex + 1).toShort())
            indices.add((currentIndex + 2).toShort())
            currentIndex = (currentIndex + 3).toShort()
        }

        // 3. Flanking Geometric Boxwood Hedges & Floral Beds
        // South Hedge Bed
        addBox(treeCenterX - 6.5f, treeCenterX + 6.5f, 0.04f, 0.65f, treeCenterZ + 5.5f, treeCenterZ + 7.5f, boxHedgeGreen)
        addBox(treeCenterX - 5.5f, treeCenterX + 5.5f, 0.65f, 0.78f, treeCenterZ + 5.8f, treeCenterZ + 7.2f, flowerAmber)

        // North Hedge Bed
        addBox(treeCenterX - 6.5f, treeCenterX + 6.5f, 0.04f, 0.65f, treeCenterZ - 7.5f, treeCenterZ - 5.5f, boxHedgeGreen)
        addBox(treeCenterX - 5.5f, treeCenterX + 5.5f, 0.65f, 0.78f, treeCenterZ - 7.2f, treeCenterZ - 5.8f, flowerAmber)

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
