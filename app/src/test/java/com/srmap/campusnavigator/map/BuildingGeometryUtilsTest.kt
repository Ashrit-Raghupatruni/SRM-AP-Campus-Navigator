package com.srmap.campusnavigator.map

import androidx.compose.ui.geometry.Offset
import com.srmap.campusnavigator.presentation.map.model.ExtrusionConfig
import com.srmap.campusnavigator.presentation.map.renderer.BuildingGeometryUtils
import org.junit.Assert.*
import org.junit.Test

class BuildingGeometryUtilsTest {

    @Test
    fun testExtrusionOffsetCalculation() {
        val offset = BuildingGeometryUtils.calculateExtrusionOffset(depthPx = 20f, angleDegrees = 315f)
        assertTrue("X offset should be positive for 315 deg", offset.x > 0)
        assertTrue("Y offset should be negative for 315 deg", offset.y < 0)
    }

    @Test
    fun testFourVertexPolygonExtrusion() {
        val rectVertices = listOf(
            Offset(0f, 0f),
            Offset(100f, 0f),
            Offset(100f, 80f),
            Offset(0f, 80f)
        )
        val config = ExtrusionConfig(baseHeightPx = 15f)
        val faces = BuildingGeometryUtils.createExtrudedBuildingFaces(
            baseVertices = rectVertices,
            config = config,
            progress = 1.0f
        )

        assertNotNull("Faces should not be null for valid 4-vertex polygon", faces)
        assertEquals("Should create 4 side quads for a rectangle", 4, faces!!.sideFaces.size)
        assertNotNull("Shadow path should be generated", faces.shadowPath)
        assertNotNull("Top face path should be generated", faces.topFacePath)
    }

    @Test
    fun testMultiVertexPolygonExtrusion() {
        // L-shaped polygon with 6 vertices
        val lShapeVertices = listOf(
            Offset(0f, 0f),
            Offset(120f, 0f),
            Offset(120f, 40f),
            Offset(40f, 40f),
            Offset(40f, 100f),
            Offset(0f, 100f)
        )
        val config = ExtrusionConfig(baseHeightPx = 18f)
        val faces = BuildingGeometryUtils.createExtrudedBuildingFaces(
            baseVertices = lShapeVertices,
            config = config,
            progress = 1.0f
        )

        assertNotNull("Faces should not be null for 6-vertex L-shape", faces)
        assertEquals("Should create 6 side quads for 6-vertex polygon", 6, faces!!.sideFaces.size)
    }

    @Test
    fun testDegeneratePolygonHandling() {
        val emptyList = emptyList<Offset>()
        val twoPoints = listOf(Offset(0f, 0f), Offset(10f, 10f))

        val facesEmpty = BuildingGeometryUtils.createExtrudedBuildingFaces(emptyList, ExtrusionConfig(), 1f)
        val facesTwo = BuildingGeometryUtils.createExtrudedBuildingFaces(twoPoints, ExtrusionConfig(), 1f)

        assertNull("Empty polygon must safely return null", facesEmpty)
        assertNull("2-point line must safely return null", facesTwo)
    }

    @Test
    fun testZeroProgressElevation() {
        val square = listOf(
            Offset(10f, 10f),
            Offset(50f, 10f),
            Offset(50f, 50f),
            Offset(10f, 50f)
        )
        val config = ExtrusionConfig(baseHeightPx = 20f)
        val facesZero = BuildingGeometryUtils.createExtrudedBuildingFaces(square, config, progress = 0.0f)

        assertNotNull(facesZero)
        assertEquals(4, facesZero!!.sideFaces.size)
    }
}
