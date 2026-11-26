package com.srmap.campusnavigator.core.geometry3d

import com.srmap.campusnavigator.domain.building3d.SRBlockSpec
import org.junit.Assert.*
import org.junit.Test

class SRBlockMeshTest {

    @Test
    fun testSRBlockMeshGeneration() {
        val mesh = SRBlockMeshGenerator.generateMesh()
        assertTrue("SR Block must have vertices", mesh.positions.isNotEmpty())
        assertEquals(mesh.positions.size, mesh.normals.size)
        assertEquals(0, mesh.indices.size % 3)

        // Verify height matches 7 levels + penthouse
        var maxY = 0f
        for (i in 0 until mesh.positions.size / 3) {
            val y = mesh.positions[i * 3 + 1]
            if (y > maxY) maxY = y
        }
        assertTrue("SR Block roof/penthouse must reach at least 25m", maxY >= 25.0f)
    }

    @Test
    fun testSRBlockViewpoints() {
        val vps = SRBlockSpec.getSRBlockViewpoints()
        assertTrue("Must have at least 6 viewpoints", vps.size >= 6)
        assertTrue(vps.any { it.id == "photo1_sunset_sports" })
        assertTrue(vps.any { it.id == "photo2_corner_ribbon" })
        assertTrue(vps.any { it.id == "photo3_aerial_master" })
        assertTrue(vps.any { it.id == "sr_to_cv_high_plaza" })
    }
}
