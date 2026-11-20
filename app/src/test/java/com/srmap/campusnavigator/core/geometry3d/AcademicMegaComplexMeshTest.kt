package com.srmap.campusnavigator.core.geometry3d

import com.srmap.campusnavigator.domain.building3d.AcademicComplexSpec
import org.junit.Assert.*
import org.junit.Test

class AcademicMegaComplexMeshTest {

    @Test
    fun testCanopyMeshGeneration() {
        val canopy = AcademicComplexCanopyGenerator.generateCanopyMesh()
        assertTrue("Canopy must have vertices", canopy.positions.isNotEmpty())
        assertEquals(canopy.positions.size, canopy.normals.size)
        assertEquals(0, canopy.indices.size % 3)

        // Check canopy roof height exceeds building height (>22m)
        var maxY = 0f
        for (i in 0 until canopy.positions.size / 3) {
            val y = canopy.positions[i * 3 + 1]
            if (y > maxY) maxY = y
        }
        assertTrue("Canopy roof peak must exceed 24 meters", maxY >= 24.5f)
    }

    @Test
    fun testCentralBreezewayGeneration() {
        val breezeway = CentralBreezewayGenerator.generateBreezewayMesh()
        assertTrue("Breezeway must have vertices", breezeway.positions.isNotEmpty())
        assertEquals(breezeway.positions.size, breezeway.normals.size)
    }

    @Test
    fun testHomiBhabhaBlockGeneration() {
        val homi = HomiBhabhaBlockMeshGenerator.generateMesh()
        assertTrue("Homi Bhabha block must have vertices", homi.positions.isNotEmpty())
        assertEquals(homi.positions.size, homi.normals.size)
    }

    @Test
    fun testComplexViewpoints() {
        val vps = AcademicComplexSpec.getComplexViewpoints()
        assertEquals(6, vps.size)
        assertTrue(vps.any { it.id == "photo1_front_perspective" })
        assertTrue(vps.any { it.id == "photo2_aerial_complex" })
        assertTrue(vps.any { it.id == "breezeway_highway" })
    }
}
