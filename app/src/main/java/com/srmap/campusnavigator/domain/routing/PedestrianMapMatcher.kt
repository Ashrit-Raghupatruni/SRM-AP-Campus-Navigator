package com.srmap.campusnavigator.domain.routing

import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.graph.GraphEdge
import com.srmap.campusnavigator.domain.models.GeoPoint

data class SnappedPosition(
    val snappedPoint: GeoPoint,
    val matchedEdge: GraphEdge,
    val perpendicularDistanceMeters: Double,
    val segmentFraction: Double
)

/**
 * Pedestrian Map-Matching Engine with Hysteresis.
 * Prevents blue dot jumping between parallel pathways by introducing sticky edge weighting
 * and topological graph continuity.
 */
class PedestrianMapMatcher(
    private val graph: CampusGraph,
    private val maxSnapDistanceMeters: Double = 40.0,
    private val parallelPathHysteresisBonusMeters: Double = 14.0 // "Sticky" bonus for current path
) {
    private var lastMatchedEdge: GraphEdge? = null

    /**
     * Map matches GPS coordinate onto the campus pedestrian graph.
     */
    fun matchLocation(
        rawPoint: GeoPoint,
        userHeadingDeg: Float? = null,
        userSpeedMps: Float? = null
    ): SnappedPosition? {
        val allEdges = graph.getAllEdges()
        if (allEdges.isEmpty()) return null

        var bestMatch: SnappedPosition? = null
        var minAdjustedDistance = Double.MAX_VALUE

        for (edge in allEdges) {
            val fromNode = graph.nodes[edge.fromNodeId] ?: continue
            val toNode = graph.nodes[edge.toNodeId] ?: continue

            val (projectedPt, fraction, rawDist) = projectPointOnSegment(rawPoint, fromNode.point, toNode.point)

            if (rawDist > maxSnapDistanceMeters) continue

            // Compute adjusted distance using Hysteresis & Topological Continuity
            var penaltyOrBonus = 0.0

            // 1. Sticky bonus if evaluating the currently matched edge
            if (lastMatchedEdge != null && (edge.id == lastMatchedEdge!!.id || edge.id == "${lastMatchedEdge!!.id}_rev")) {
                penaltyOrBonus -= parallelPathHysteresisBonusMeters
            }
            // 2. Continuity bonus if connected to the last matched node
            else if (lastMatchedEdge != null &&
                (edge.fromNodeId == lastMatchedEdge!!.toNodeId || edge.toNodeId == lastMatchedEdge!!.toNodeId ||
                 edge.fromNodeId == lastMatchedEdge!!.fromNodeId || edge.toNodeId == lastMatchedEdge!!.fromNodeId)) {
                penaltyOrBonus -= (parallelPathHysteresisBonusMeters * 0.6)
            }
            // 3. Penalty if switching to a disconnected parallel edge
            else if (lastMatchedEdge != null) {
                penaltyOrBonus += (parallelPathHysteresisBonusMeters * 0.5)
            }

            // 4. Heading alignment bonus if walking with verified bearing
            if (userHeadingDeg != null && userSpeedMps != null && userSpeedMps > 0.8f) {
                val edgeBearing = fromNode.point.bearingTo(toNode.point)
                val diff = Math.abs(userHeadingDeg - edgeBearing).let { if (it > 180) 360 - it else it }
                if (diff < 35.0 || diff > 145.0) {
                    penaltyOrBonus -= 4.0 // Edge aligns with user movement direction
                } else if (diff in 70.0..110.0) {
                    penaltyOrBonus += 6.0 // Perpendicular edge penalty
                }
            }

            val score = rawDist + penaltyOrBonus
            if (score < minAdjustedDistance) {
                minAdjustedDistance = score
                bestMatch = SnappedPosition(projectedPt, edge, rawDist, fraction)
            }
        }

        bestMatch?.let {
            lastMatchedEdge = it.matchedEdge
        }

        return bestMatch
    }

    fun reset() {
        lastMatchedEdge = null
    }

    companion object {
        /**
         * Projects point P onto line segment (A, B).
         * Returns (Projected Point, Segment Fraction [0..1], Perpendicular Distance in meters)
         */
        fun projectPointOnSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Triple<GeoPoint, Double, Double> {
            val dLat = b.latitude - a.latitude
            val dLon = b.longitude - a.longitude
            val lengthSq = dLat * dLat + dLon * dLon

            if (lengthSq < 1e-12) {
                return Triple(a, 0.0, p.distanceTo(a))
            }

            // Vector dot product
            val t = (((p.latitude - a.latitude) * dLat + (p.longitude - a.longitude) * dLon) / lengthSq)
                .coerceIn(0.0, 1.0)

            val projLat = a.latitude + t * dLat
            val projLon = a.longitude + t * dLon
            val projectedPoint = GeoPoint(projLat, projLon)

            val distance = p.distanceTo(projectedPoint)
            return Triple(projectedPoint, t, distance)
        }
    }
}
