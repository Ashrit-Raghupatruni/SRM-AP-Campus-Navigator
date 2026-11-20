package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.building3d.AcademicComplexSpec
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural generator for the iconic floating space-frame umbrella canopy
 * and tall slender tree columns with 4-way diagonal branching capitals.
 */
object AcademicComplexCanopyGenerator {

    fun generateCanopyMesh(): MeshData {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        var currentIndex: Short = 0

        fun addTri(
            p1: Vector3, p2: Vector3, p3: Vector3,
            n: Vector3,
            c: Color
        ) {
            positions.add(p1.x); positions.add(p1.y); positions.add(p1.z)
            positions.add(p2.x); positions.add(p2.y); positions.add(p2.z)
            positions.add(p3.x); positions.add(p3.y); positions.add(p3.z)

            for (i in 0 until 3) {
                normals.add(n.x); normals.add(n.y); normals.add(n.z)
                colors.add(c.red); colors.add(c.green); colors.add(c.blue); colors.add(1f)
            }

            indices.add(currentIndex)
            indices.add((currentIndex + 1).toShort())
            indices.add((currentIndex + 2).toShort())
            currentIndex = (currentIndex + 3).toShort()
        }

        fun addQuad(
            p1: Vector3, p2: Vector3, p3: Vector3, p4: Vector3,
            n: Vector3,
            c: Color
        ) {
            addTri(p1, p2, p3, n, c)
            addTri(p1, p3, p4, n, c)
        }

        fun addCylinder(
            bottom: Vector3, top: Vector3, radius: Float, segments: Int, c: Color
        ) {
            val step = (2f * Math.PI.toFloat()) / segments
            val h = top.y - bottom.y

            for (i in 0 until segments) {
                val a1 = i * step
                val a2 = (i + 1) * step
                val c1 = cos(a1) * radius
                val s1 = sin(a1) * radius
                val c2 = cos(a2) * radius
                val s2 = sin(a2) * radius

                val p1 = Vector3(bottom.x + c1, bottom.y, bottom.z + s1)
                val p2 = Vector3(bottom.x + c2, bottom.y, bottom.z + s2)
                val p3 = Vector3(top.x + c2, top.y, top.z + s2)
                val p4 = Vector3(top.x + c1, top.y, top.z + s1)

                val n = Vector3((c1 + c2) / 2f, 0f, (s1 + s2) / 2f).normalized()
                addQuad(p1, p2, p3, p4, n, c)
            }
        }

        val minX = AcademicComplexSpec.CANOPY_MIN_X
        val maxX = AcademicComplexSpec.CANOPY_MAX_X
        val minZ = AcademicComplexSpec.CANOPY_MIN_Z
        val maxZ = AcademicComplexSpec.CANOPY_MAX_Z
        val soffitY = AcademicComplexSpec.CANOPY_SOFFIT_Y
        val peakY = AcademicComplexSpec.CANOPY_ROOF_PEAK_Y

        val deckC = AcademicComplexSpec.COLOR_CANOPY_DECK
        val trussC = AcademicComplexSpec.COLOR_CANOPY_TRUSS
        val treeC = AcademicComplexSpec.COLOR_TREE_COLUMNS

        // 1. SPACE-FRAME CANOPY ROOF DECK (Upper Aerodynamic Faceted Shell)
        val midX = (minX + maxX) / 2f
        val midZ = (minZ + maxZ) / 2f

        // Ridge running along center with sloped pitch
        val pRidge1 = Vector3(midX, peakY, minZ)
        val pRidge2 = Vector3(midX, peakY, maxZ)

        // West roof slope
        val pNW = Vector3(minX, soffitY + 0.6f, minZ)
        val pSW = Vector3(minX, soffitY + 0.6f, maxZ)
        val nWest = Vector3(-(peakY - soffitY), (maxX - minX) / 2f, 0f).normalized()
        addQuad(pSW, pNW, pRidge1, pRidge2, nWest, deckC)

        // East roof slope
        val pNE = Vector3(maxX, soffitY + 0.6f, minZ)
        val pSE = Vector3(maxX, soffitY + 0.6f, maxZ)
        val nEast = Vector3((peakY - soffitY), (maxX - minX) / 2f, 0f).normalized()
        addQuad(pRidge1, pNE, pSE, pRidge2, nEast, deckC)

        // Gables (North & South)
        addTri(pRidge1, pNW, pNE, Vector3(0f, 0f, -1f), deckC)
        addTri(pRidge2, pSE, pSW, Vector3(0f, 0f, 1f), deckC)

        // 2. SPACE-FRAME UNDERBELLY SOFFIT (Looking up from ground)
        addQuad(
            Vector3(minX, soffitY, minZ),
            Vector3(maxX, soffitY, minZ),
            Vector3(maxX, soffitY, maxZ),
            Vector3(minX, soffitY, maxZ),
            Vector3(0f, -1f, 0f),
            trussC
        )

        // 3. SLENDER TREE COLUMNS WITH 4-WAY BRANCHING CAPITALS
        // Strategically placed along South perimeter, Breezeway, and North perimeter
        val columnLocations = listOf(
            Vector3(88.0f, 0f, -143.0f),  // V Block South West
            Vector3(120.0f, 0f, -143.0f), // V Block South Center
            Vector3(147.7f, 0f, -143.0f), // Breezeway South West edge
            Vector3(160.5f, 0f, -143.0f), // Breezeway South East edge
            Vector3(188.0f, 0f, -143.0f), // Homi South Center
            Vector3(208.0f, 0f, -143.0f), // Homi South East

            Vector3(88.0f, 0f, -212.0f),  // V Block North West
            Vector3(120.0f, 0f, -212.0f), // V Block North Center
            Vector3(147.7f, 0f, -212.0f), // Breezeway North West edge
            Vector3(160.5f, 0f, -212.0f), // Breezeway North East edge
            Vector3(188.0f, 0f, -212.0f), // Homi North Center
            Vector3(208.0f, 0f, -212.0f)  // Homi North East
        )

        val trunkRadius = 0.45f
        val branchRadius = 0.22f
        val branchStartH = soffitY - 3.8f // Branches split 3.8m below canopy soffit
        val branchSpread = 2.4f

        for (col in columnLocations) {
            // Main vertical trunk from ground (Y=0) up to branch split point
            val trunkTop = Vector3(col.x, branchStartH, col.z)
            addCylinder(col, trunkTop, trunkRadius, segments = 8, treeC)

            // 4-way diagonal branches to the canopy soffit
            val branchTargets = listOf(
                Vector3(col.x - branchSpread, soffitY, col.z),
                Vector3(col.x + branchSpread, soffitY, col.z),
                Vector3(col.x, soffitY, col.z - branchSpread),
                Vector3(col.x, soffitY, col.z + branchSpread)
            )

            for (target in branchTargets) {
                addCylinder(trunkTop, target, branchRadius, segments = 6, treeC)
            }
        }

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
