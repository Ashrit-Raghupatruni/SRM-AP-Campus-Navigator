package com.srmap.campusnavigator.domain.routing

import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import com.srmap.campusnavigator.domain.models.RouteStep

enum class NavigationStatus {
    IDLE,
    NAVIGATING,
    OFF_ROUTE,
    RECALCULATING,
    ARRIVED
}

data class DynamicRouteProgress(
    val status: NavigationStatus,
    val completedPath: List<GeoPoint>,
    val remainingPath: List<GeoPoint>,
    val currentPosition: GeoPoint,
    val totalDistanceMeters: Double,
    val remainingDistanceMeters: Double,
    val completedDistanceMeters: Double,
    val progressFraction: Float,
    val estimatedRemainingSeconds: Int,
    val nextInstruction: String,
    val distanceToNextTurnMeters: Double
)

/**
 * Dynamic Route Progress Tracker.
 * Manages route slicing (completed vs remaining), progress monotonic advancement,
 * off-route detection (>25m for 3 consecutive fixes), and arrival detection (<12m).
 */
class NavigationProgressTracker(
    private val graph: CampusGraph,
    private val offRouteDistanceThresholdMeters: Double = 25.0,
    private val arrivalDistanceThresholdMeters: Double = 12.0
) {
    private var baseRoute: PedestrianRoute? = null
    private var destinationPoint: GeoPoint? = null
    private var currentSegmentIndex = 0
    private var consecutiveOffRouteCount = 0
    private var completedDistanceAcc = 0.0

    fun startNavigation(route: PedestrianRoute, destination: GeoPoint) {
        baseRoute = route
        destinationPoint = destination
        currentSegmentIndex = 0
        consecutiveOffRouteCount = 0
        completedDistanceAcc = 0.0
    }

    fun stopNavigation() {
        baseRoute = null
        destinationPoint = null
        currentSegmentIndex = 0
        consecutiveOffRouteCount = 0
        completedDistanceAcc = 0.0
    }

    /**
     * Updates user's progress along the route given their current snapped position.
     * Returns DynamicRouteProgress or null if not navigating.
     */
    fun updateProgress(currentPoint: GeoPoint): DynamicRouteProgress? {
        val route = baseRoute ?: return null
        val fullPath = route.path
        if (fullPath.size < 2) return null

        val dest = destinationPoint ?: fullPath.last()
        val distToDest = currentPoint.distanceTo(dest)

        // 1. Check Arrival
        if (distToDest < arrivalDistanceThresholdMeters) {
            return DynamicRouteProgress(
                status = NavigationStatus.ARRIVED,
                completedPath = fullPath,
                remainingPath = listOf(currentPoint, dest),
                currentPosition = currentPoint,
                totalDistanceMeters = route.totalDistanceMeters,
                remainingDistanceMeters = 0.0,
                completedDistanceMeters = route.totalDistanceMeters,
                progressFraction = 1.0f,
                estimatedRemainingSeconds = 0,
                nextInstruction = "You have arrived at your destination!",
                distanceToNextTurnMeters = 0.0
            )
        }

        // 2. Find nearest point along the remaining route geometry
        var closestDist = Double.MAX_VALUE
        var bestSegment = currentSegmentIndex
        var bestProjected = currentPoint

        // Search from current segment forward to prevent backwards jumping
        val searchStart = (currentSegmentIndex - 1).coerceAtLeast(0)
        val searchEnd = (currentSegmentIndex + 3).coerceAtMost(fullPath.size - 2)

        for (i in searchStart..searchEnd) {
            val p1 = fullPath[i]
            val p2 = fullPath[i + 1]
            val (proj, _, dist) = PedestrianMapMatcher.projectPointOnSegment(currentPoint, p1, p2)
            if (dist < closestDist) {
                closestDist = dist
                bestSegment = i
                bestProjected = proj
            }
        }

        // 3. Off-route detection
        if (closestDist > offRouteDistanceThresholdMeters) {
            consecutiveOffRouteCount++
            if (consecutiveOffRouteCount >= 3) {
                // Trigger Recalculation
                val newRoute = RoutingEngine.findAStarPath(graph, currentPoint, dest)
                if (newRoute != null) {
                    baseRoute = newRoute
                    currentSegmentIndex = 0
                    consecutiveOffRouteCount = 0
                    return updateProgress(currentPoint)
                } else {
                    return DynamicRouteProgress(
                        status = NavigationStatus.OFF_ROUTE,
                        completedPath = emptyList(),
                        remainingPath = fullPath,
                        currentPosition = currentPoint,
                        totalDistanceMeters = route.totalDistanceMeters,
                        remainingDistanceMeters = route.totalDistanceMeters,
                        completedDistanceMeters = 0.0,
                        progressFraction = 0.0f,
                        estimatedRemainingSeconds = route.estimatedTimeSeconds,
                        nextInstruction = "Off route. Please turn back towards pathway.",
                        distanceToNextTurnMeters = closestDist
                    )
                }
            }
        } else {
            consecutiveOffRouteCount = 0
            if (bestSegment > currentSegmentIndex) {
                currentSegmentIndex = bestSegment
            }
        }

        // 4. Construct Completed and Remaining polyline lists
        val completedList = mutableListOf<GeoPoint>()
        for (i in 0..bestSegment) {
            completedList.add(fullPath[i])
        }
        completedList.add(bestProjected)

        val remainingList = mutableListOf<GeoPoint>()
        remainingList.add(bestProjected)
        for (i in (bestSegment + 1) until fullPath.size) {
            remainingList.add(fullPath[i])
        }

        // 5. Calculate remaining distance
        var remainingMeters = 0.0
        for (i in 0 until remainingList.size - 1) {
            remainingMeters += remainingList[i].distanceTo(remainingList[i + 1])
        }

        val totalMeters = route.totalDistanceMeters.coerceAtLeast(remainingMeters)
        val completedMeters = (totalMeters - remainingMeters).coerceAtLeast(0.0)
        val progressFraction = (completedMeters / totalMeters).toFloat().coerceIn(0.0f, 1.0f)
        val remainingSeconds = (remainingMeters / 1.35).toInt() // Pedestrian walking speed ~1.35 m/s

        // 6. Turn instructions from route steps
        val nextStep = route.steps.firstOrNull { it.distanceMeters > 5.0 }
        val instruction = nextStep?.instruction ?: "Continue along the campus pathway"
        val distToTurn = remainingList.getOrNull(1)?.let { bestProjected.distanceTo(it) } ?: remainingMeters

        return DynamicRouteProgress(
            status = NavigationStatus.NAVIGATING,
            completedPath = completedList,
            remainingPath = remainingList,
            currentPosition = bestProjected,
            totalDistanceMeters = totalMeters,
            remainingDistanceMeters = remainingMeters,
            completedDistanceMeters = completedMeters,
            progressFraction = progressFraction,
            estimatedRemainingSeconds = remainingSeconds,
            nextInstruction = instruction,
            distanceToNextTurnMeters = distToTurn
        )
    }
}
