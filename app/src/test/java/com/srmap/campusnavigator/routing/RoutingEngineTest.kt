package com.srmap.campusnavigator.routing

import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.graph.GraphEdge
import com.srmap.campusnavigator.domain.graph.GraphNode
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.routing.RoutingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoutingEngineTest {

    private lateinit var testGraph: CampusGraph

    @Before
    fun setup() {
        testGraph = CampusGraph()
        // Create simple triangle graph
        // Node A: (16.4630, 80.5060)
        // Node B: (16.4635, 80.5060)
        // Node C: (16.4635, 80.5065)
        val pA = GeoPoint(16.4630, 80.5060)
        val pB = GeoPoint(16.4635, 80.5060)
        val pC = GeoPoint(16.4635, 80.5065)

        testGraph.addNode(GraphNode("A", pA))
        testGraph.addNode(GraphNode("B", pB))
        testGraph.addNode(GraphNode("C", pC))

        testGraph.addEdge(GraphEdge("e1", "A", "B", pA.distanceTo(pB), "path", true))
        testGraph.addEdge(GraphEdge("e2", "B", "C", pB.distanceTo(pC), "path", true))
        testGraph.addEdge(GraphEdge("e3", "A", "C", pA.distanceTo(pC) * 1.5, "road", true))
    }

    @Test
    fun testHaversineDistanceCalculation() {
        val p1 = GeoPoint(16.4632311, 80.5064108)
        val p2 = GeoPoint(16.4632311, 80.5074108) // ~106 meters east
        val dist = p1.distanceTo(p2)
        assertTrue(dist > 90.0 && dist < 120.0)
    }

    @Test
    fun testAStarPathFindingMatchesDijkstra() {
        val start = GeoPoint(16.4629, 80.5060) // Near A
        val end = GeoPoint(16.4636, 80.5065)   // Near C

        val aStarResult = RoutingEngine.findAStarPath(testGraph, start, end)
        val dijkstraResult = RoutingEngine.findDijkstraPath(testGraph, start, end)

        assertNotNull("A* route must not be null", aStarResult)
        assertNotNull("Dijkstra route must not be null", dijkstraResult)

        // Compare total distances within 0.1 meter tolerance
        assertEquals(
            "A* shortest distance should equal Dijkstra shortest distance",
            dijkstraResult!!.totalDistanceMeters,
            aStarResult!!.totalDistanceMeters,
            0.1
        )

        // Verify turn steps generated
        assertTrue(aStarResult.steps.isNotEmpty())
    }
}
