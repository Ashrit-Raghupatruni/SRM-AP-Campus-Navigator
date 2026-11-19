package com.srmap.campusnavigator.domain.routing

import com.srmap.campusnavigator.core.geometry3d.LocalCoordinateConverter
import com.srmap.campusnavigator.core.geometry3d.Vector3
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.graph.GraphEdge
import com.srmap.campusnavigator.domain.models.GeoPoint

data class SnappedPosition(
    val snappedPoint: GeoPoint,
    val matchedEdge: GraphEdge,
    val perpendicularDistanceMeters: Double,
    val segmentFraction: Double,
    val edgeBearingDegrees: Float
)

/**
 * Pedestrian Map-Matching Engine with Metric Coordinate Projection & Hysteresis.
 * Prevents blue dot jumping between parallel pathways by introducing sticky edge weighting,
 * metric Euclidean segment projection, and topological graph continuity.
 */
class PedestrianMapMatcher(
    private val graph: CampusGraph,
    private val maxSnapDistanceMeters: Double = 40.0,
    private val parallelPathHysteresisBonusMeters: Double = 14.0 // "Sticky" bonus for current path
) {
    private var lastMatchedEdge: GraphEdge? = null

    /**
     * Map matches GPS coordinate onto the campus pedestrian graph using local metric coordinates.
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

            val (projectedPt, fraction, rawDist) = projectPointOnSegmentMetric(rawPoint, fromNode.point, toNode.point)

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

                val edgeBearing = fromNode.point.bearingTo(toNode.point).toFloat()

            // 4. Heading alignment bonus if walking with verified bearing
            if (userHeadingDeg != null && userSpeedMps != null && userSpeedMps > 0.8f) {
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
                bestMatch = SnappedPosition(projectedPt, edge, rawDist, fraction, edgeBearing)
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
         * Projects point P onto line segment (A, B) using metric Cartesian tangent coordinates (meters).
         * Eliminates latitude-longitude degree compression distortion.
         * Returns (Projected GeoPoint, Segment Fraction [0..1], Perpendicular Distance in meters)
         */
        fun projectPointOnSegmentMetric(p: GeoPoint, a: GeoPoint, b: GeoPoint): Triple<GeoPoint, Double, Double> {
            val vP = LocalCoordinateConverter.toLocal3D(p)
            val vA = LocalCoordinateConverter.toLocal3D(a)
            val vB = LocalCoordinateConverter.toLocal3D(b)

            val dX = (vB.x - vA.x).toDouble()
            val dZ = (vB.z - vA.z).toDouble()
            val lenSq = dX * dX + dZ * dZ

            if (lenSq < 1e-6) {
                return Triple(a, 0.0, p.distanceTo(a))
            }

            val t = (((vP.x - vA.x) * dX + (vP.z - vA.z) * dZ) / lenSq).coerceIn(0.0, 1.0)

            val projLat = a.latitude + t * (b.latitude - a.latitude)
            val projLon = a.longitude + t * (b.longitude - a.longitude)
            val projectedPoint = GeoPoint(projLat, projLon)

            val dist = p.distanceTo(projectedPoint)
            return Triple(projectedPoint, t, dist)
        }
    }
}
