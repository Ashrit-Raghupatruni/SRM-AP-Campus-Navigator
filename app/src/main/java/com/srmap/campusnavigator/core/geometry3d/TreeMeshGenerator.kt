package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.models.CampusFeature
import com.srmap.campusnavigator.domain.models.GeoPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, clean procedural 3D Tree Mesh Generator for SRM AP Campus.
 * Creates clean, elegant canopy trees with hexagonal trunks and layered low-poly foliage crowns,
 * strictly maintaining walkway clearances and preventing vertex/index misalignments.
 */
object TreeMeshGenerator {

    private const val TRUNK_RADIUS = 0.25f
    private const val TRUNK_HEIGHT = 2.2f
    private const val CROWN_RADIUS = 2.0f
    private const val CROWN_HEIGHT = 3.2f

    fun generateTreesMesh(
        features: List<CampusFeature>,
        roadPolylines: List<List<Vector3>> = emptyList()
    ): MeshData? {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        var currentIndex: Short = 0

        val trunkColor = Color(0xFF6B4423) // Natural Bark Brown
        val leavesDark = Color(0xFF1E6B37) // Deep Foliage Green
        val leavesLight = Color(0xFF2D8A4E) // Lush Canopy Green

        // Collect all forest and tree areas
        val treePolygons = features.filter {
            val landuse = it.rawProperties["landuse"]?.toString()
            val seating = it.rawProperties["outdoor_seating"]?.toString()
            val name = it.name ?: ""
            landuse == "forest" || seating == "outdoor_seating" || name.contains("Tree", ignoreCase = true)
        }

        val treeLocations = mutableListOf<Vector3>()

        for (feat in treePolygons) {
            val coords = feat.coordinates
            if (coords.size < 3) continue

            val localPoly = coords.map { LocalCoordinateConverter.toLocal3D(it) }

            val minX = localPoly.minOf { it.x }
            val maxX = localPoly.maxOf { it.x }
            val minZ = localPoly.minOf { it.z }
            val maxZ = localPoly.maxOf { it.z }

            // Deterministic grid sampling (step = 8.0m for natural tree spacing)
            var z = minZ + 4.0f
            while (z < maxZ) {
                var x = minX + 4.0f
                while (x < maxX) {
                    val candidate = Vector3(x, 0f, z)
                    if (isPointInsidePolygonXZ(candidate, localPoly)) {
                        // Keep minimum 3.0m clearance from any walking path
                        val tooCloseToPath = roadPolylines.any { path ->
                            path.any { pt -> LocalCoordinateConverter.distance(pt, candidate) < 3.0f }
                        }
                        if (!tooCloseToPath) {
                            treeLocations.add(candidate)
                        }
                    }
                    x += 8.0f
                }
                z += 8.0f
            }
        }

        val sampledTrees = treeLocations.take(380)

        for (treeLoc in sampledTrees) {
            if (currentIndex > 31000) break

            val segments = 6 // Hexagonal prism

            // 1. Procedural Trunk
            for (i in 0 until segments) {
                val a1 = (i * 2 * PI / segments).toFloat()
                val a2 = ((i + 1) * 2 * PI / segments).toFloat()

                val cos1 = cos(a1)
                val sin1 = sin(a1)
                val cos2 = cos(a2)
                val sin2 = sin(a2)

                val b1 = Vector3(treeLoc.x + cos1 * TRUNK_RADIUS, 0f, treeLoc.z + sin1 * TRUNK_RADIUS)
                val b2 = Vector3(treeLoc.x + cos2 * TRUNK_RADIUS, 0f, treeLoc.z + sin2 * TRUNK_RADIUS)
                val t1 = Vector3(b1.x, TRUNK_HEIGHT, b1.z)
                val t2 = Vector3(b2.x, TRUNK_HEIGHT, b2.z)

                // Face normal
                val midAngle = (a1 + a2) / 2f
                val nx = cos(midAngle)
                val nz = sin(midAngle)

                val quad = listOf(b1, b2, t2, t1)
                for (v in quad) {
                    positions.add(v.x); positions.add(v.y); positions.add(v.z)
                    normals.add(nx); normals.add(0f); normals.add(nz)
                    colors.add(trunkColor.red); colors.add(trunkColor.green); colors.add(trunkColor.blue); colors.add(1f)
                }

                indices.add(currentIndex)
                indices.add((currentIndex + 1).toShort())
                indices.add((currentIndex + 2).toShort())

                indices.add(currentIndex)
                indices.add((currentIndex + 2).toShort())
                indices.add((currentIndex + 3).toShort())

                currentIndex = (currentIndex + 4).toShort()
            }

            // 2. Procedural Clean Double-Tier Canopy
            val tiers = 2
            val tierHeight = CROWN_HEIGHT / tiers

            for (t in 0 until tiers) {
                val baseY = TRUNK_HEIGHT + t * (tierHeight * 0.7f)
                val apexY = baseY + tierHeight * 1.3f
                val radius = CROWN_RADIUS * (1.0f - t * 0.25f)
                val apex = Vector3(treeLoc.x, apexY, treeLoc.z)

                for (i in 0 until segments) {
                    val a1 = (i * 2 * PI / segments).toFloat()
                    val a2 = ((i + 1) * 2 * PI / segments).toFloat()

                    val c1 = Vector3(treeLoc.x + cos(a1) * radius, baseY, treeLoc.z + sin(a1) * radius)
                    val c2 = Vector3(treeLoc.x + cos(a2) * radius, baseY, treeLoc.z + sin(a2) * radius)

                    val leafColor = if ((i + t) % 2 == 0) leavesDark else leavesLight

                    // Calculate triangle normal
                    val edge1 = c2 - c1
                    val edge2 = apex - c1
                    val n = edge1.cross(edge2).normalized()

                    val tri = listOf(c1, c2, apex)
                    for (v in tri) {
                        positions.add(v.x); positions.add(v.y); positions.add(v.z)
                        normals.add(n.x); normals.add(n.y); normals.add(n.z)
                        colors.add(leafColor.red); colors.add(leafColor.green); colors.add(leafColor.blue); colors.add(1f)
                    }

                    indices.add(currentIndex)
                    indices.add((currentIndex + 1).toShort())
                    indices.add((currentIndex + 2).toShort())

                    currentIndex = (currentIndex + 3).toShort()
                }
            }
        }

        if (positions.isEmpty()) return null

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }

    private fun isPointInsidePolygonXZ(pt: Vector3, poly: List<Vector3>): Boolean {
        var inside = false
        val n = poly.size
        var j = n - 1
        for (i in 0 until n) {
            val xi = poly[i].x; val zi = poly[i].z
            val xj = poly[j].x; val zj = poly[j].z

            if (((zi > pt.z) != (zj > pt.z)) && (pt.x < (xj - xi) * (pt.z - zi) / (zj - zi) + xi)) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
