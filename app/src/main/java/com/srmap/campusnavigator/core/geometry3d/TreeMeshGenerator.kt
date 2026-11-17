package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.models.CampusFeature
import com.srmap.campusnavigator.domain.models.GeoPoint

object TreeMeshGenerator {

    private const val TREE_TRUNK_RADIUS = 0.22f
    private const val TREE_TRUNK_HEIGHT = 1.8f
    private const val TREE_CROWN_RADIUS = 1.6f
    private const val TREE_CROWN_HEIGHT = 3.6f

    fun generateTreesMesh(
        features: List<CampusFeature>,
        roadPolylines: List<List<Vector3>> = emptyList()
    ): MeshData? {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        var currentIndex: Short = 0

        val trunkColor = Color(0xFF78350F) // Bark Brown
        val leavesDark = Color(0xFF166534) // Forest Green
        val leavesLight = Color(0xFF15803D) // Leaf Green

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

            // Bounding box on XZ plane
            val minX = localPoly.minOf { it.x }
            val maxX = localPoly.maxOf { it.x }
            val minZ = localPoly.minOf { it.z }
            val maxZ = localPoly.maxOf { it.z }

            // Deterministic grid sampling (step ~ 7.5m)
            var z = minZ + 3.5f
            while (z < maxZ) {
                var x = minX + 3.5f
                while (x < maxX) {
                    val candidate = Vector3(x, 0f, z)
                    if (isPointInsidePolygonXZ(candidate, localPoly)) {
                        // Check path exclusion buffer (keep walkways open)
                        val tooCloseToPath = roadPolylines.any { path ->
                            path.any { pt -> LocalCoordinateConverter.distance(pt, candidate) < 2.5f }
                        }
                        if (!tooCloseToPath) {
                            treeLocations.add(candidate)
                        }
                    }
                    x += 7.5f
                }
                z += 7.5f
            }
        }

        // Limit tree count to preserve smooth 60 FPS
        val sampledTrees = treeLocations.take(450)

        for (treeLoc in sampledTrees) {
            if (currentIndex > 31000) break

            // 1. Procedural Trunk (Octagonal Prism)
            val trunkStart = currentIndex
            val segments = 6
            for (i in 0 until segments) {
                val angle = (i * 2 * Math.PI / segments).toFloat()
                val nextAngle = ((i + 1) * 2 * Math.PI / segments).toFloat()

                val cos1 = kotlin.math.cos(angle)
                val sin1 = kotlin.math.sin(angle)
                val cos2 = kotlin.math.cos(nextAngle)
                val sin2 = kotlin.math.sin(nextAngle)

                val b1 = Vector3(treeLoc.x + cos1 * TREE_TRUNK_RADIUS, 0f, treeLoc.z + sin1 * TREE_TRUNK_RADIUS)
                val b2 = Vector3(treeLoc.x + cos2 * TREE_TRUNK_RADIUS, 0f, treeLoc.z + sin2 * TREE_TRUNK_RADIUS)
                val t1 = Vector3(b1.x, TREE_TRUNK_HEIGHT, b1.z)
                val t2 = Vector3(b2.x, TREE_TRUNK_HEIGHT, b2.z)

                val quad = listOf(b1, b2, t2, t1)
                for (v in quad) {
                    positions.add(v.x); positions.add(v.y); positions.add(v.z)
                    normals.add(0f); normals.add(0f); normals.add(1f)
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

            // 2. Procedural Crown (Cone Pyramids)
            val crownBaseY = TREE_TRUNK_HEIGHT * 0.8f
            val crownApexY = crownBaseY + TREE_CROWN_HEIGHT
            val apex = Vector3(treeLoc.x, crownApexY, treeLoc.z)

            for (i in 0 until segments) {
                val angle = (i * 2 * Math.PI / segments).toFloat()
                val nextAngle = ((i + 1) * 2 * Math.PI / segments).toFloat()

                val c1 = Vector3(treeLoc.x + kotlin.math.cos(angle) * TREE_CROWN_RADIUS, crownBaseY, treeLoc.z + kotlin.math.sin(angle) * TREE_CROWN_RADIUS)
                val c2 = Vector3(treeLoc.x + kotlin.math.cos(nextAngle) * TREE_CROWN_RADIUS, crownBaseY, treeLoc.z + kotlin.math.sin(nextAngle) * TREE_CROWN_RADIUS)

                val leafColor = if (i % 2 == 0) leavesDark else leavesLight

                val tri = listOf(c1, c2, apex)
                for (v in tri) {
                    positions.add(v.x); positions.add(v.y); positions.add(v.z)
                    normals.add(0f); normals.add(1f); normals.add(0f)
                    colors.add(leafColor.red); colors.add(leafColor.green); colors.add(leafColor.blue); colors.add(1f)
                }

                indices.add(currentIndex)
                indices.add((currentIndex + 1).toShort())
                indices.add((currentIndex + 2).toShort())

                currentIndex = (currentIndex + 3).toShort()
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
