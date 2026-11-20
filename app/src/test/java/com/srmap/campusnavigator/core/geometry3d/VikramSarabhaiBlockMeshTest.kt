package com.srmap.campusnavigator.core.geometry3d

import com.srmap.campusnavigator.domain.building3d.VikramSarabhaiBlockSpec
import org.junit.Assert.*
import org.junit.Test

class VikramSarabhaiBlockMeshTest {

    @Test
    fun testMeshGenerationIntegrity() {
        val mesh = VikramSarabhaiBlockMeshGenerator.generateMesh()

        // 1. Buffers must be non-empty
        assertTrue("Positions buffer must have vertices", mesh.positions.isNotEmpty())
        assertTrue("Normals buffer must match positions", mesh.normals.size == mesh.positions.size)
        assertTrue("Colors buffer must match vertices (4 floats per vertex)", mesh.colors.size == (mesh.positions.size / 3) * 4)
        assertTrue("Indices buffer must have triangle entries", mesh.indices.isNotEmpty())
        assertEquals(0, mesh.indices.size % 3)

        // 2. Vertex bounds must match surveyed dimensions
        val vertexCount = mesh.positions.size / 3
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (i in 0 until vertexCount) {
            val x = mesh.positions[i * 3]
            val y = mesh.positions[i * 3 + 1]
            val z = mesh.positions[i * 3 + 2]

            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)
            minZ = minOf(minZ, z)
            maxZ = maxOf(maxZ, z)

            // Normals must be unit length
            val nx = mesh.normals[i * 3]
            val ny = mesh.normals[i * 3 + 1]
            val nz = mesh.normals[i * 3 + 2]
            val lenSq = nx * nx + ny * ny + nz * nz
            assertEquals("Normal vector must be normalized", 1.0f, lenSq, 0.05f)
        }

        // Check bounding coordinates match AcademicComplexSpec (West block portion of complex)
        assertEquals(com.srmap.campusnavigator.domain.building3d.AcademicComplexSpec.V_MIN_X, minX, 0.5f)
        assertEquals(com.srmap.campusnavigator.domain.building3d.AcademicComplexSpec.V_MAX_X, maxX, 0.5f)
        assertEquals(0f, minY, 0.01f)
        assertTrue(maxY >= com.srmap.campusnavigator.domain.building3d.AcademicComplexSpec.V_HEIGHT)
        assertEquals(com.srmap.campusnavigator.domain.building3d.AcademicComplexSpec.V_MIN_Z, minZ, 0.5f)
        assertEquals(com.srmap.campusnavigator.domain.building3d.AcademicComplexSpec.V_MAX_Z, maxZ, 0.5f)
    }

    @Test
    fun testViewpointsConfiguration() {
        val viewpoints = VikramSarabhaiBlockSpec.getViewpoints()
        assertEquals(6, viewpoints.size)

        for (vp in viewpoints) {
            assertNotNull(vp.id)
            assertNotNull(vp.name)
            // Eye must be outside the building envelope
            val eye = vp.eyePosition
            val isOutside = eye.x < VikramSarabhaiBlockSpec.MIN_X || eye.x > VikramSarabhaiBlockSpec.MAX_X ||
                    eye.z < VikramSarabhaiBlockSpec.MIN_Z || eye.z > VikramSarabhaiBlockSpec.MAX_Z ||
                    eye.y > VikramSarabhaiBlockSpec.TOTAL_ROOF_HEIGHT_METERS
            assertTrue("Viewpoint eye must be positioned outside the building structure", isOutside)
        }
    }
}
