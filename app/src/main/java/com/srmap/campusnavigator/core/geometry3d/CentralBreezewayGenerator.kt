package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.building3d.AcademicComplexSpec

/**
 * Procedural generator for the central open-air Breezeway pedestrian spine
 * connecting JC Bose Block (South) directly through to Tree Court (North),
 * complete with multi-level covered skywalk bridges spanning between V Block and Homi Bhabha.
 */
object CentralBreezewayGenerator {

    fun generateBreezewayMesh(): MeshData {
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

        val minX = AcademicComplexSpec.BREEZEWAY_MIN_X
        val maxX = AcademicComplexSpec.BREEZEWAY_MAX_X
        val minZ = AcademicComplexSpec.BREEZEWAY_MIN_Z
        val maxZ = AcademicComplexSpec.BREEZEWAY_MAX_Z

        val paverC = AcademicComplexSpec.COLOR_BREEZEWAY_PAVERS
        val bridgeC = AcademicComplexSpec.COLOR_LINK_BRIDGE_SLAB
        val glassC = AcademicComplexSpec.COLOR_CURTAIN_GLAZING

        // 1. PEDESTRIAN BREEZEWAY SPINE PLAZA (Ground floor Y=0.04m)
        // Extends from JC Bose road (maxZ + 8m) through to Tree Court (minZ - 8m)
        val extNorth = minZ - 10.0f
        val extSouth = maxZ + 10.0f
        val groundY = 0.045f

        addQuad(
            Vector3(minX, groundY, extNorth),
            Vector3(maxX, groundY, extNorth),
            Vector3(maxX, groundY, extSouth),
            Vector3(minX, groundY, extSouth),
            Vector3(0f, 1f, 0f),
            paverC
        )

        // 2. MULTI-LEVEL ELEVATED SKYWALK LINK BRIDGES (Floors 3 & 4)
        // Spanning the 12.8m gap between V Block (minX) and Homi J Bhabha (maxX)
        val bridgeWidth = 4.8f // North-South depth of the bridge
        val bridgeCenterZ = (minZ + maxZ) / 2f
        val bZ1 = bridgeCenterZ - bridgeWidth / 2f
        val bZ2 = bridgeCenterZ + bridgeWidth / 2f

        val bridgeLevels = listOf(
            9.6f to 12.4f, // Floor 3 link bridge
            12.8f to 15.6f // Floor 4 link bridge
        )

        for ((flrBottom, flrTop) in bridgeLevels) {
            // Floor slab
            addQuad(
                Vector3(minX, flrBottom, bZ1),
                Vector3(maxX, flrBottom, bZ1),
                Vector3(maxX, flrBottom, bZ2),
                Vector3(minX, flrBottom, bZ2),
                Vector3(0f, -1f, 0f),
                bridgeC
            )
            // Roof slab
            addQuad(
                Vector3(minX, flrTop, bZ1),
                Vector3(maxX, flrTop, bZ1),
                Vector3(maxX, flrTop, bZ2),
                Vector3(minX, flrTop, bZ2),
                Vector3(0f, 1f, 0f),
                bridgeC
            )
            // South Glazed Enclosure
            addQuad(
                Vector3(minX, flrBottom, bZ2),
                Vector3(maxX, flrBottom, bZ2),
                Vector3(maxX, flrTop, bZ2),
                Vector3(minX, flrTop, bZ2),
                Vector3(0f, 0f, 1f),
                glassC
            )
            // North Glazed Enclosure
            addQuad(
                Vector3(maxX, flrBottom, bZ1),
                Vector3(minX, flrBottom, bZ1),
                Vector3(minX, flrTop, bZ1),
                Vector3(maxX, flrTop, bZ1),
                Vector3(0f, 0f, -1f),
                glassC
            )
        }

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
