package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color

object GroundMeshGenerator {

    private const val CAMPUS_HALF_EXTENT = 600f // 1.2km x 1.2km ground coverage

    fun generateGroundMesh(): MeshData {
        val groundColor = Color(0xFFDCFCE7) // Campus Lawn Green
        val y = 0.0f

        val p1 = Vector3(-CAMPUS_HALF_EXTENT, y, -CAMPUS_HALF_EXTENT)
        val p2 = Vector3(CAMPUS_HALF_EXTENT, y, -CAMPUS_HALF_EXTENT)
        val p3 = Vector3(CAMPUS_HALF_EXTENT, y, CAMPUS_HALF_EXTENT)
        val p4 = Vector3(-CAMPUS_HALF_EXTENT, y, CAMPUS_HALF_EXTENT)

        val positions = floatArrayOf(
            p1.x, p1.y, p1.z,
            p2.x, p2.y, p2.z,
            p3.x, p3.y, p3.z,
            p4.x, p4.y, p4.z
        )

        val normals = floatArrayOf(
            0f, 1f, 0f,
            0f, 1f, 0f,
            0f, 1f, 0f,
            0f, 1f, 0f
        )

        val colors = floatArrayOf(
            groundColor.red, groundColor.green, groundColor.blue, 1f,
            groundColor.red, groundColor.green, groundColor.blue, 1f,
            groundColor.red, groundColor.green, groundColor.blue, 1f,
            groundColor.red, groundColor.green, groundColor.blue, 1f
        )

        val indices = shortArrayOf(
            0, 1, 2,
            0, 2, 3
        )

        return MeshData(positions, normals, colors, indices)
    }
}
