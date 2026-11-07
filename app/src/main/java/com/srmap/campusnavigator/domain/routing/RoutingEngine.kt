package com.srmap.campusnavigator.domain.routing

import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.graph.GraphNode
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import com.srmap.campusnavigator.domain.models.RouteStep
import java.util.PriorityQueue

object RoutingEngine {

    private const val WALKING_SPEED_METERS_PER_SEC = 1.3

    fun findAStarPath(
        graph: CampusGraph,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): PedestrianRoute? {
        val startNode = graph.findNearestNode(startPoint) ?: return null
        val endNode = graph.findNearestNode(endPoint) ?: return null

        if (startNode.id == endNode.id) {
            val path = listOf(startPoint, startNode.point, endPoint)
            val dist = startPoint.distanceTo(startNode.point) + startNode.point.distanceTo(endPoint)
            return PedestrianRoute(
                path = path,
                totalDistanceMeters = dist,
                estimatedTimeSeconds = (dist / WALKING_SPEED_METERS_PER_SEC).toInt().coerceAtLeast(10),
                steps = listOf(RouteStep("Arrive at destination", dist, startPoint, endPoint))
            )
        }

        val openSet = PriorityQueue<AStarNode>(compareBy { it.fScore })
        val gScore = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
        val cameFrom = mutableMapOf<String, String>()

        gScore[startNode.id] = 0.0
        openSet.add(AStarNode(startNode.id, startNode.point.distanceTo(endNode.point)))
        val closedSet = mutableSetOf<String>()

        while (openSet.isNotEmpty()) {
            val current = openSet.poll() ?: break
            val currentId = current.nodeId

            if (currentId == endNode.id) {
                return reconstructRoute(graph, cameFrom, currentId, startPoint, endPoint)
            }

            closedSet.add(currentId)

            val neighbors = graph.edges[currentId] ?: emptyList()
            for (edge in neighbors) {
                if (!edge.walkable) continue
                val neighborId = edge.toNodeId
                if (closedSet.contains(neighborId)) continue

                val tentativeGScore = (gScore[currentId] ?: Double.MAX_VALUE) + edge.distanceMeters
                if (tentativeGScore < (gScore[neighborId] ?: Double.MAX_VALUE)) {
                    cameFrom[neighborId] = currentId
                    gScore[neighborId] = tentativeGScore
                    val neighborNode = graph.nodes[neighborId] ?: continue
                    val fScore = tentativeGScore + neighborNode.point.distanceTo(endNode.point)
                    openSet.add(AStarNode(neighborId, fScore))
                }
            }
        }

        return null
    }

    fun findDijkstraPath(
        graph: CampusGraph,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): PedestrianRoute? {
        val startNode = graph.findNearestNode(startPoint) ?: return null
        val endNode = graph.findNearestNode(endPoint) ?: return null

        val openSet = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        val distances = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
        val cameFrom = mutableMapOf<String, String>()

        distances[startNode.id] = 0.0
        openSet.add(Pair(startNode.id, 0.0))
        val visited = mutableSetOf<String>()

        while (openSet.isNotEmpty()) {
            val (currentId, dist) = openSet.poll() ?: break
            if (visited.contains(currentId)) continue
            visited.add(currentId)

            if (currentId == endNode.id) {
                return reconstructRoute(graph, cameFrom, currentId, startPoint, endPoint)
            }

            for (edge in graph.edges[currentId] ?: emptyList()) {
                if (!edge.walkable) continue
                val neighborId = edge.toNodeId
                val newDist = dist + edge.distanceMeters
                if (newDist < (distances[neighborId] ?: Double.MAX_VALUE)) {
                    distances[neighborId] = newDist
                    cameFrom[neighborId] = currentId
                    openSet.add(Pair(neighborId, newDist))
                }
            }
        }
        return null
    }

    private fun reconstructRoute(
        graph: CampusGraph,
        cameFrom: Map<String, String>,
        endNodeId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): PedestrianRoute {
        val nodePath = mutableListOf<String>()
        var curr: String? = endNodeId
        while (curr != null) {
            nodePath.add(curr)
            curr = cameFrom[curr]
        }
        nodePath.reverse()

        val points = mutableListOf<GeoPoint>()
        points.add(startPoint)
        for (id in nodePath) {
            graph.nodes[id]?.let { points.add(it.point) }
        }
        points.add(endPoint)

        var totalDist = 0.0
        for (i in 0 until points.size - 1) {
            totalDist += points[i].distanceTo(points[i + 1])
        }

        val steps = generateTurnInstructions(points)
        val estimatedTime = (totalDist / WALKING_SPEED_METERS_PER_SEC).toInt().coerceAtLeast(15)

        return PedestrianRoute(
            path = points,
            totalDistanceMeters = totalDist,
            estimatedTimeSeconds = estimatedTime,
            steps = steps
        )
    }

    private fun generateTurnInstructions(points: List<GeoPoint>): List<RouteStep> {
        if (points.size < 2) return emptyList()
        val steps = mutableListOf<RouteStep>()

        if (points.size == 2) {
            val d = points[0].distanceTo(points[1])
            return listOf(RouteStep("Walk straight to destination", d, points[0], points[1]))
        }

        val firstDist = points[0].distanceTo(points[1])
        steps.add(RouteStep("Start along campus walkway", firstDist, points[0], points[1]))

        for (i in 1 until points.size - 1) {
            val pPrev = points[i - 1]
            val pCurr = points[i]
            val pNext = points[i + 1]

            val b1 = pPrev.bearingTo(pCurr)
            val b2 = pCurr.bearingTo(pNext)
            var delta = (b2 - b1)
            while (delta > 180) delta -= 360
            while (delta < -180) delta += 360

            val dist = pCurr.distanceTo(pNext)
            val instruction = when {
                delta > 35.0 && delta < 135.0 -> "Turn right onto walkway"
                delta >= 135.0 -> "Turn sharp right"
                delta < -35.0 && delta > -135.0 -> "Turn left onto walkway"
                delta <= -135.0 -> "Turn sharp left"
                else -> "Continue straight"
            }
            steps.add(RouteStep(instruction, dist, pCurr, pNext))
        }

        val lastIdx = points.size - 1
        val lastDist = points[lastIdx - 1].distanceTo(points[lastIdx])
        steps.add(RouteStep("Arrive at your campus destination", lastDist, points[lastIdx - 1], points[lastIdx]))

        return steps
    }

    private data class AStarNode(
        val nodeId: String,
        val fScore: Double
    )
}
