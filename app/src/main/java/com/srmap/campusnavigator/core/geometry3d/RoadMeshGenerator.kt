package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.models.GeoPoint

object RoadMeshGenerator {

    private const val WALKWAY_WIDTH = 3.2f // Meters
    private const val ROAD_ELEVATION_Y = 0.04f // Slight elevation to prevent Z-fighting with ground

    fun generateWalkwayMesh(graph: CampusGraph): MeshData {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        val pathColor = Color(0xFFE2E8F0) // Clean walkway tone
        var currentIndex: Short = 0

        val processedEdges = mutableSetOf<String>()

        for ((nodeId, node) in graph.nodes) {
            val from3D = LocalCoordinateConverter.toLocal3D(node.point, altitude = ROAD_ELEVATION_Y)
            val nodeEdges = graph.edges[nodeId] ?: emptyList()

            for (edge in nodeEdges) {
                val edgeKey = if (nodeId < edge.toNodeId) "$nodeId-${edge.toNodeId}" else "${edge.toNodeId}-$nodeId"
                if (processedEdges.contains(edgeKey)) continue
                processedEdges.add(edgeKey)

                val toNode = graph.nodes[edge.toNodeId] ?: continue
                val to3D = LocalCoordinateConverter.toLocal3D(toNode.point, altitude = ROAD_ELEVATION_Y)

                val dir = (to3D - from3D).normalized()
                if (dir.length() < 0.001f) continue

                // Perpendicular lateral vector on XZ plane
                val lateral = Vector3(-dir.z, 0f, dir.x).normalized() * (WALKWAY_WIDTH / 2f)

                val p1 = from3D - lateral
                val p2 = from3D + lateral
                val p3 = to3D + lateral
                val p4 = to3D - lateral

                val quad = listOf(p1, p2, p3, p4)
                for (v in quad) {
                    positions.add(v.x); positions.add(v.y); positions.add(v.z)
                    normals.add(0f); normals.add(1f); normals.add(0f)
                    colors.add(pathColor.red); colors.add(pathColor.green); colors.add(pathColor.blue); colors.add(1f)
                }

                indices.add(currentIndex)
                indices.add((currentIndex + 1).toShort())
                indices.add((currentIndex + 2).toShort())

                indices.add(currentIndex)
                indices.add((currentIndex + 2).toShort())
                indices.add((currentIndex + 3).toShort())

                currentIndex = (currentIndex + 4).toShort()
                if (currentIndex > 32000) break // Safe index limit per batch
            }
        }

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }

    fun generateRoutePolylineMesh(routePoints: List<GeoPoint>): MeshData? {
        if (routePoints.size < 2) return null

        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        val routeColor = Color(0xFF0284C7) // Vivid Electric Blue navigation chevron
        val routeWidth = 1.6f
        val routeElev = ROAD_ELEVATION_Y + 0.04f

        val pts3D = routePoints.map { LocalCoordinateConverter.toLocal3D(it, altitude = routeElev) }
        var currentIndex: Short = 0

        for (i in 0 until pts3D.size - 1) {
            val pStart = pts3D[i]
            val pEnd = pts3D[i + 1]

            val dir = (pEnd - pStart).normalized()
            if (dir.length() < 0.001f) continue

            val lateral = Vector3(-dir.z, 0f, dir.x).normalized() * (routeWidth / 2f)

            val v1 = pStart - lateral
            val v2 = pStart + lateral
            val v3 = pEnd + lateral
            val v4 = pEnd - lateral

            for (v in listOf(v1, v2, v3, v4)) {
                positions.add(v.x); positions.add(v.y); positions.add(v.z)
                normals.add(0f); normals.add(1f); normals.add(0f)
                colors.add(routeColor.red); colors.add(routeColor.green); colors.add(routeColor.blue); colors.add(1f)
            }

            indices.add(currentIndex)
            indices.add((currentIndex + 1).toShort())
            indices.add((currentIndex + 2).toShort())

            indices.add(currentIndex)
            indices.add((currentIndex + 2).toShort())
            indices.add((currentIndex + 3).toShort())

            currentIndex = (currentIndex + 4).toShort()
        }

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
