package com.srmap.campusnavigator.routing

import com.srmap.campusnavigator.core.location.GpsFilterEngine
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.graph.GraphEdge
import com.srmap.campusnavigator.domain.graph.GraphNode
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.routing.NavigationProgressTracker
import com.srmap.campusnavigator.domain.routing.NavigationStatus
import com.srmap.campusnavigator.domain.routing.PedestrianMapMatcher
import com.srmap.campusnavigator.domain.routing.RoutingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GpsTrackingAndMapMatchingTest {

    private lateinit var graph: CampusGraph
    private lateinit var matcher: PedestrianMapMatcher
    private lateinit var gpsFilter: GpsFilterEngine
    private lateinit var tracker: NavigationProgressTracker

    @Before
    fun setup() {
        graph = CampusGraph()

        // Two parallel walkways 10 meters apart going North-South
        // Path 1 (West path): Lon 80.50600, Lat from 16.46300 to 16.46400
        val w1 = GraphNode("W1", GeoPoint(16.46300, 80.50600))
        val w2 = GraphNode("W2", GeoPoint(16.46350, 80.50600))
        val w3 = GraphNode("W3", GeoPoint(16.46400, 80.50600))
        graph.addNode(w1); graph.addNode(w2); graph.addNode(w3)
        graph.addEdge(GraphEdge("edge_w1_w2", "W1", "W2", w1.point.distanceTo(w2.point), "path"))
        graph.addEdge(GraphEdge("edge_w2_w3", "W2", "W3", w2.point.distanceTo(w3.point), "path"))

        // Path 2 (East path): Lon 80.50610 (~10m east), Lat from 16.46300 to 16.46400
        val e1 = GraphNode("E1", GeoPoint(16.46300, 80.50610))
        val e2 = GraphNode("E2", GeoPoint(16.46350, 80.50610))
        val e3 = GraphNode("E3", GeoPoint(16.46400, 80.50610))
        graph.addNode(e1); graph.addNode(e2); graph.addNode(e3)
        graph.addEdge(GraphEdge("edge_e1_e2", "E1", "E2", e1.point.distanceTo(e2.point), "path"))
        graph.addEdge(GraphEdge("edge_e2_e3", "E2", "E3", e2.point.distanceTo(e3.point), "path"))

        matcher = PedestrianMapMatcher(graph, maxSnapDistanceMeters = 30.0, parallelPathHysteresisBonusMeters = 12.0)
        gpsFilter = GpsFilterEngine()
        tracker = NavigationProgressTracker(graph)
    }

    @Test
    fun testParallelPathwayHysteresisPreventsJumping() {
        // Step 1: User starts walking on West Path
        val fix1 = GeoPoint(16.46320, 80.50601) // 1m from West path
        val match1 = matcher.matchLocation(fix1)
        assertNotNull(match1)
        assertTrue(match1!!.matchedEdge.id.startsWith("edge_w"))

        // Step 2: Sudden noisy GPS jitter pushes reading 6m towards East path (midline is 5m)
        val noisyFix = GeoPoint(16.46325, 80.50606) // Slightly closer to East path
        val match2 = matcher.matchLocation(noisyFix)
        assertNotNull(match2)

        // Hysteresis must stick to the West path instead of hopping back and forth
        assertTrue("Hysteresis must keep user on current path during jitter", match2!!.matchedEdge.id.startsWith("edge_w"))
    }

    @Test
    fun testGpsOutlierFiltering() {
        val initialFix = UserLocationState(GeoPoint(16.46320, 80.50600), accuracyMeters = 10f)
        val smoothed1 = gpsFilter.filterAndSmooth(initialFix, 1000L)
        assertNotNull(smoothed1)

        // Outlier 1: Accuracy > 35m
        val badAccFix = UserLocationState(GeoPoint(16.46330, 80.50600), accuracyMeters = 65f)
        val smoothed2 = gpsFilter.filterAndSmooth(badAccFix, 2000L)
        assertEquals("Fix with poor accuracy should return previous valid position", smoothed1!!.point.latitude, smoothed2!!.point.latitude, 0.0001)

        // Outlier 2: Speed anomaly (>6 m/s teleport)
        val teleportFix = UserLocationState(GeoPoint(16.46500, 80.50600), accuracyMeters = 8f) // ~200m away in 1 sec
        val smoothed3 = gpsFilter.filterAndSmooth(teleportFix, 3000L)
        assertEquals("Teleport jump should be rejected", smoothed1.point.latitude, smoothed3!!.point.latitude, 0.0001)
    }

    @Test
    fun testDynamicRouteProgressSlicing() {
        val route = RoutingEngine.findAStarPath(graph, GeoPoint(16.46300, 80.50600), GeoPoint(16.46400, 80.50600))
        assertNotNull(route)
        tracker.startNavigation(route!!, GeoPoint(16.46400, 80.50600))

        // Initial progress at start
        val prog0 = tracker.updateProgress(GeoPoint(16.46300, 80.50600))
        assertNotNull(prog0)
        assertEquals(NavigationStatus.NAVIGATING, prog0!!.status)
        assertTrue(prog0.progressFraction < 0.1f)

        // Walk half-way
        val progHalf = tracker.updateProgress(GeoPoint(16.46350, 80.50600))
        assertNotNull(progHalf)
        assertTrue("Remaining distance must decrease", progHalf!!.remainingDistanceMeters < prog0.remainingDistanceMeters)
        assertTrue("Progress fraction should be ~0.5", progHalf.progressFraction in 0.4f..0.6f)
        assertTrue("Completed path must contain points", progHalf.completedPath.isNotEmpty())
        assertTrue("Remaining path must lead to destination", progHalf.remainingPath.isNotEmpty())

        // Arrive near destination (<12m)
        val progArrival = tracker.updateProgress(GeoPoint(16.46398, 80.50600))
        assertNotNull(progArrival)
        assertEquals(NavigationStatus.ARRIVED, progArrival!!.status)
        assertEquals(1.0f, progArrival.progressFraction, 0.01f)
    }
}
