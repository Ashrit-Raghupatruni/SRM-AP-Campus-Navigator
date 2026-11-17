package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.models.CampusFeature
import com.srmap.campusnavigator.domain.models.GeoPoint

object SportsGroundMeshGenerator {

    private const val SPORTS_ELEVATION_Y = 0.02f // Slightly above ground to prevent Z-fighting

    fun generateSportsMesh(features: List<CampusFeature>): MeshData? {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        var currentIndex: Short = 0

        for (feature in features) {
            val sport = feature.rawProperties["sport"]?.toString()
            val leisure = feature.rawProperties["leisure"]?.toString()
            if (sport == null && leisure != "track" && leisure != "pitch") continue

            val coords = feature.coordinates
            if (coords.size < 3) continue

            val (baseColor, isRunningTrack) = when (sport) {
                "cricket" -> Color(0xFF4ADE80) to false // Lush Cricket Oval Green
                "soccer" -> Color(0xFF22C55E) to false  // Football emerald turf
                "running" -> Color(0xFFDC2626) to true  // Brick Red Running Track
                "basketball" -> Color(0xFF38BDF8) to false // Blue Court
                "tennis" -> Color(0xFF0284C7) to false
                "volleyball" -> Color(0xFFF97316) to false // Orange court
                else -> Color(0xFF86EFAC) to false
            }

            val localPoints = coords.map { 
                LocalCoordinateConverter.toLocal3D(it, altitude = if (isRunningTrack) SPORTS_ELEVATION_Y else SPORTS_ELEVATION_Y + 0.01f)
            }

            val triIndices = PolygonTriangulator.triangulate(localPoints)
            if (triIndices.isEmpty()) continue

            val startIdx = currentIndex
            for (p in localPoints) {
                positions.add(p.x); positions.add(p.y); positions.add(p.z)
                normals.add(0f); normals.add(1f); normals.add(0f)
                colors.add(baseColor.red); colors.add(baseColor.green); colors.add(baseColor.blue); colors.add(1f)
                currentIndex = (currentIndex + 1).toShort()
            }

            for (idx in triIndices) {
                indices.add((startIdx + idx).toShort())
            }

            // If Cricket Ground, add center pitch strip
            if (sport == "cricket") {
                val centroid = calculateCentroid(localPoints)
                val pitchW = 3.2f // meters
                val pitchL = 20.12f // 22 yards
                val pitchColor = Color(0xFFFDE68A) // Earthy beige pitch
                val pY = SPORTS_ELEVATION_Y + 0.02f

                val pitchCorners = listOf(
                    Vector3(centroid.x - pitchW / 2, pY, centroid.z - pitchL / 2),
                    Vector3(centroid.x + pitchW / 2, pY, centroid.z - pitchL / 2),
                    Vector3(centroid.x + pitchW / 2, pY, centroid.z + pitchL / 2),
                    Vector3(centroid.x - pitchW / 2, pY, centroid.z + pitchL / 2)
                )

                val pStart = currentIndex
                for (v in pitchCorners) {
                    positions.add(v.x); positions.add(v.y); positions.add(v.z)
                    normals.add(0f); normals.add(1f); normals.add(0f)
                    colors.add(pitchColor.red); colors.add(pitchColor.green); colors.add(pitchColor.blue); colors.add(1f)
                    currentIndex = (currentIndex + 1).toShort()
                }

                indices.add(pStart)
                indices.add((pStart + 1).toShort())
                indices.add((pStart + 2).toShort())

                indices.add(pStart)
                indices.add((pStart + 2).toShort())
                indices.add((pStart + 3).toShort())
            }

            if (currentIndex > 32000) break
        }

        if (positions.isEmpty()) return null

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }

    private fun calculateCentroid(points: List<Vector3>): Vector3 {
        var x = 0f; var y = 0f; var z = 0f
        for (p in points) {
            x += p.x; y += p.y; z += p.z
        }
        val n = points.size.toFloat()
        return Vector3(x / n, y / n, z / n)
    }
}
