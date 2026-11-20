package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.models.CampusFeature
import com.srmap.campusnavigator.domain.models.GeoPoint

object TreeMeshGenerator {

    private const val TREE_TRUNK_RADIUS = 0.22f
    private const val TREE_TRUNK_HEIGHT = 1.8f
    private const val TREE_CROWN_RADIUS = 1.6f
    private const val TREE_CROWN_HEIGHT = 3.6f

    // Palm / Avenue Tree Specifications (for formal boulevards like SR Block East concourse)
    private const val PALM_TRUNK_RADIUS = 0.16f
    private const val PALM_TRUNK_HEIGHT = 4.2f
    private const val PALM_CROWN_RADIUS = 2.2f
    private const val PALM_CROWN_HEIGHT = 1.2f

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
        val palmLeafGreen = Color(0xFF22C55E) // Bright Palm Frond Green

        // Collect all forest and tree areas
        val treePolygons = features.filter {
            val landuse = it.rawProperties["landuse"]?.toString()
            val seating = it.rawProperties["outdoor_seating"]?.toString()
            val name = it.name ?: ""
            landuse == "forest" || seating == "outdoor_seating" || name.contains("Tree", ignoreCase = true) || name.contains("Buffer", ignoreCase = true)
        }

        data class TreeSpec(val pos: Vector3, val isAvenueTree: Boolean)
        val treeLocations = mutableListOf<TreeSpec>()

        for (feat in treePolygons) {
            val coords = feat.coordinates
            if (coords.size < 3) continue

            val localPoly = coords.map { LocalCoordinateConverter.toLocal3D(it) }
            val name = feat.name ?: ""
            val isAvenue = name.contains("Buffer", ignoreCase = true) || name.contains("Parkway", ignoreCase = true)

            // Bounding box on XZ plane
            val minX = localPoly.minOf { it.x }
            val maxX = localPoly.maxOf { it.x }
            val minZ = localPoly.minOf { it.z }
            val maxZ = localPoly.maxOf { it.z }

            // Deterministic grid sampling (step ~ 6.5m for lush parkways, 7.5m for standard forest)
            val step = if (isAvenue) 6.5f else 7.5f
            var z = minZ + 3.0f
            while (z < maxZ) {
                var x = minX + 3.0f
                while (x < maxX) {
                    val candidate = Vector3(x, 0f, z)
                    if (isPointInsidePolygonXZ(candidate, localPoly)) {
                        // Check path exclusion buffer (keep walkways open)
                        val tooCloseToPath = roadPolylines.any { path ->
                            path.any { pt -> LocalCoordinateConverter.distance(pt, candidate) < 2.0f }
                        }
                        if (!tooCloseToPath) {
                            treeLocations.add(TreeSpec(candidate, isAvenue))
                        }
                    }
                    x += step
                }
                z += step
            }
        }

        // Limit tree count to preserve smooth 60 FPS
        val sampledTrees = treeLocations.take(500)

        for (spec in sampledTrees) {
            if (currentIndex > 31000) break

            val treeLoc = spec.pos
            val isPalm = spec.isAvenueTree

            val trunkRadius = if (isPalm) PALM_TRUNK_RADIUS else TREE_TRUNK_RADIUS
            val trunkHeight = if (isPalm) PALM_TRUNK_HEIGHT else TREE_TRUNK_HEIGHT
            val crownRadius = if (isPalm) PALM_CROWN_RADIUS else TREE_CROWN_RADIUS
            val crownHeight = if (isPalm) PALM_CROWN_HEIGHT else TREE_CROWN_HEIGHT

            // 1. Procedural Trunk (Octagonal Prism)
            val segments = 6
            for (i in 0 until segments) {
                val angle = (i * 2 * Math.PI / segments).toFloat()
                val nextAngle = ((i + 1) * 2 * Math.PI / segments).toFloat()

                val cos1 = kotlin.math.cos(angle)
                val sin1 = kotlin.math.sin(angle)
                val cos2 = kotlin.math.cos(nextAngle)
                val sin2 = kotlin.math.sin(nextAngle)

                val b1 = Vector3(treeLoc.x + cos1 * trunkRadius, 0f, treeLoc.z + sin1 * trunkRadius)
                val b2 = Vector3(treeLoc.x + cos2 * trunkRadius, 0f, treeLoc.z + sin2 * trunkRadius)
                val t1 = Vector3(b1.x, trunkHeight, b1.z)
                val t2 = Vector3(b2.x, trunkHeight, b2.z)

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

            // 2. Procedural Crown (Cone Pyramids or Umbrella Palm Canopy)
            val crownBaseY = trunkHeight * 0.85f
            val crownApexY = crownBaseY + crownHeight
            val apex = Vector3(treeLoc.x, crownApexY, treeLoc.z)

            for (i in 0 until segments) {
                val angle = (i * 2 * Math.PI / segments).toFloat()
                val nextAngle = ((i + 1) * 2 * Math.PI / segments).toFloat()

                val c1 = Vector3(treeLoc.x + kotlin.math.cos(angle) * crownRadius, crownBaseY, treeLoc.z + kotlin.math.sin(angle) * crownRadius)
                val c2 = Vector3(treeLoc.x + kotlin.math.cos(nextAngle) * crownRadius, crownBaseY, treeLoc.z + kotlin.math.sin(nextAngle) * crownRadius)

                val leafColor = if (isPalm) {
                    if (i % 2 == 0) palmLeafGreen else leavesLight
                } else {
                    if (i % 2 == 0) leavesDark else leavesLight
                }

                val tri = listOf(c1, c2, apex)
                for (v in tri) {
                    positions.add(v.x); positions.add(v.y); positions.add(v.z)
                    normals.add(0f); normals.add(1f); normals.add(0f)
                    colors.add(leafColor.red); colors.add(leafColor.green); colors.add(leafColor.blue); colors.add(1f)
                }

                indices.add(currentIndex)
                indices.add((currentIndex + 1).toShort())
                indices.add((currentIndex + 2).toShort())

                indices.add(currentIndex)
                indices.add((currentIndex + 2).toShort())
                indices.add((currentIndex + 3).toShort())
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
