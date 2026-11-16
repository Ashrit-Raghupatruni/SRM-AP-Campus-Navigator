package com.srmap.campusnavigator.geometry3d

import com.srmap.campusnavigator.core.geometry3d.LocalCoordinateConverter
import com.srmap.campusnavigator.core.geometry3d.PolygonTriangulator
import com.srmap.campusnavigator.core.geometry3d.Vector3
import com.srmap.campusnavigator.domain.models.GeoPoint
import org.junit.Assert.*
import org.junit.Test

class LocalCoordinateConverterTest {

    @Test
    fun testOriginConversion() {
        val origin = GeoPoint(LocalCoordinateConverter.ORIGIN_LAT, LocalCoordinateConverter.ORIGIN_LON)
        val pos3D = LocalCoordinateConverter.toLocal3D(origin, altitude = 0f)

        assertEquals("X at origin should be 0", 0f, pos3D.x, 0.001f)
        assertEquals("Y at origin should be 0", 0f, pos3D.y, 0.001f)
        assertEquals("Z at origin should be 0", 0f, pos3D.z, 0.001f)
    }

    @Test
    fun testBidirectionalConversionAccuracy() {
        val testGeo = GeoPoint(16.46450, 80.50750)
        val pos3D = LocalCoordinateConverter.toLocal3D(testGeo)
        val reconGeo = LocalCoordinateConverter.toGeoPoint(pos3D)

        assertEquals(testGeo.latitude, reconGeo.latitude, 0.00001)
        assertEquals(testGeo.longitude, reconGeo.longitude, 0.00001)
    }

    @Test
    fun testTriangleTriangulation() {
        val triangle = listOf(
            Vector3(0f, 0f, 0f),
            Vector3(10f, 0f, 0f),
            Vector3(0f, 0f, 10f)
        )
        val indices = PolygonTriangulator.triangulate(triangle)
        assertEquals("Should yield 3 indices for 1 triangle", 3, indices.size)
    }

    @Test
    fun testQuadTriangulation() {
        val quad = listOf(
            Vector3(0f, 0f, 0f),
            Vector3(10f, 0f, 0f),
            Vector3(10f, 0f, 10f),
            Vector3(0f, 0f, 10f)
        )
        val indices = PolygonTriangulator.triangulate(quad)
        assertEquals("Should yield 6 indices for 2 triangles", 6, indices.size)
    }
}
