package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Triangulates 2D simple polygons using the Robust Ear Clipping Algorithm.
 * Handles clockwise or counter-clockwise vertex orders and concave polygons.
 */
object PolygonTriangulator {

    data class Point2D(val x: Float, val y: Float, val originalIndex: Int)

    fun triangulate(vertices: List<Vector3>): List<Short> {
        val n = vertices.size
        if (n < 3) return emptyList()
        if (n == 3) return listOf(0, 1, 2)

        // Flatten to 2D on XZ plane
        val poly = vertices.mapIndexed { idx, v -> Point2D(v.x, v.z, idx) }.toMutableList()

        // Ensure CCW orientation
        if (computeArea2D(poly) < 0f) {
            poly.reverse()
        }

        val indices = mutableListOf<Short>()
        var count = poly.size

        var attempts = 0
        val maxAttempts = count * count

        while (count > 2 && attempts < maxAttempts) {
            attempts++
            var earFound = false

            for (i in 0 until count) {
                val prev = (i - 1 + count) % count
                val next = (i + 1) % count

                val a = poly[prev]
                val b = poly[i]
                val c = poly[next]

                if (isEar(a, b, c, poly)) {
                    indices.add(b.originalIndex.toShort())
                    indices.add(a.originalIndex.toShort())
                    indices.add(c.originalIndex.toShort())

                    poly.removeAt(i)
                    count--
                    earFound = true
                    break
                }
            }

            if (!earFound) {
                // Fallback fan triangulation if self-intersecting or degenerate
                for (i in 1 until count - 1) {
                    indices.add(poly[0].originalIndex.toShort())
                    indices.add(poly[i].originalIndex.toShort())
                    indices.add(poly[i + 1].originalIndex.toShort())
                }
                break
            }
        }

        return indices
    }

    private fun computeArea2D(poly: List<Point2D>): Float {
        var area = 0f
        val n = poly.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += poly[i].x * poly[j].y - poly[j].x * poly[i].y
        }
        return area * 0.5f
    }

    private fun isEar(a: Point2D, b: Point2D, c: Point2D, poly: List<Point2D>): Boolean {
        // Must be convex corner
        val cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
        if (cross <= 0.00001f) return false

        // Check if any other point is inside the triangle abc
        for (p in poly) {
            if (p == a || p == b || p == c) continue
            if (isPointInsideTriangle(p, a, b, c)) return false
        }
        return true
    }

    private fun isPointInsideTriangle(p: Point2D, a: Point2D, b: Point2D, c: Point2D): Boolean {
        fun sign(p1: Point2D, p2: Point2D, p3: Point2D): Float {
            return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
        }

        val d1 = sign(p, a, b)
        val d2 = sign(p, b, c)
        val d3 = sign(p, c, a)

        val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
        val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)

        return !(hasNeg && hasPos)
    }
}

/**
 * Standard 3D Mesh container holding vertex, normal, color, and index buffers for OpenGL.
 */
data class MeshData(
    val positions: FloatArray, // x, y, z
    val normals: FloatArray,   // nx, ny, nz
    val colors: FloatArray,    // r, g, b, a
    val indices: ShortArray
) {
    fun createVertexBuffer(): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(positions)
        buffer.position(0)
        return buffer
    }

    fun createNormalBuffer(): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(normals.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(normals)
        buffer.position(0)
        return buffer
    }

    fun createColorBuffer(): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(colors)
        buffer.position(0)
        return buffer
    }

    fun createIndexBuffer(): ShortBuffer {
        val buffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        buffer.put(indices)
        buffer.position(0)
        return buffer
    }
}
