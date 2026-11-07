package com.srmap.campusnavigator.routing

import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.graph.GraphEdge
import com.srmap.campusnavigator.domain.graph.GraphNode
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.routing.RoutingEngine
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ComprehensiveAuditTest {

    private val graph = CampusGraph()
    private val buildings = mutableListOf<Pair<String, GeoPoint>>()

    @Before
    fun setup() {
        val assetsDir = File("src/main/assets")
        val roadsFile = File(assetsDir, "roads.geojson")
        val pathsFile = File(assetsDir, "paths.geojson")
        val bldgsFile = File(assetsDir, "buildings.geojson")

        // Parse roads & paths into graph
        listOf(roadsFile, pathsFile).forEach { file ->
            if (file.exists()) {
                val json = JSONObject(file.readText())
                val features = json.getJSONArray("features")
                for (i in 0 until features.length()) {
                    val f = features.getJSONObject(i)
                    val geom = f.getJSONObject("geometry")
                    val type = geom.getString("type")
                    val highwayType = f.getJSONObject("properties").optString("highway", "path")
                    if (type == "LineString") {
                        val coords = geom.getJSONArray("coordinates")
                        var prevNodeId: String? = null
                        for (j in 0 until coords.length()) {
                            val pt = coords.getJSONArray(j)
                            val lon = pt.getDouble(0)
                            val lat = pt.getDouble(1)
                            val nodeId = "node_%.5f_%.5f".format(lat, lon)
                            val node = GraphNode(nodeId, GeoPoint(lat, lon))
                            graph.addNode(node)

                            if (prevNodeId != null) {
                                val prevNode = graph.nodes[prevNodeId]!!
                                val dist = prevNode.point.distanceTo(node.point)
                                val edgeId = "edge_${prevNodeId}_$nodeId"
                                graph.addEdge(GraphEdge(edgeId, prevNodeId, nodeId, dist, highwayType, true))
                            }
                            prevNodeId = nodeId
                        }
                    }
                }
            }
        }

        if (bldgsFile.exists()) {
            val json = JSONObject(bldgsFile.readText())
            val features = json.getJSONArray("features")
            for (i in 0 until features.length()) {
                val f = features.getJSONObject(i)
                val props = f.getJSONObject("properties")
                val name = props.optString("name", "")
                val geom = f.getJSONObject("geometry")
                val type = geom.getString("type")
                if (type == "Polygon" && name.isNotBlank()) {
                    val ring = geom.getJSONArray("coordinates").getJSONArray(0)
                    var latSum = 0.0
                    var lonSum = 0.0
                    for (j in 0 until ring.length()) {
                        val pt = ring.getJSONArray(j)
                        lonSum += pt.getDouble(0)
                        latSum += pt.getDouble(1)
                    }
                    val centroid = GeoPoint(latSum / ring.length(), lonSum / ring.length())
                    buildings.add(name to centroid)
                }
            }
        }
    }

    @Test
    fun audit1_validateGraphNodesAndEdgesCount() {
        println("AUDIT: Total Graph Nodes = ${graph.nodes.size}")
        println("AUDIT: Total Graph Node Adjacencies = ${graph.edges.size}")
        assertTrue("Graph must have at least 100 nodes", graph.nodes.size >= 100)
    }

    @Test
    fun audit2_crossBuildingRoutingAStarVsDijkstra() {
        println("AUDIT: Testing pairwise building routes across ${buildings.size} campus buildings...")
        var testedPairs = 0
        for (i in 0 until buildings.size) {
            for (j in i + 1 until buildings.size) {
                val b1 = buildings[i]
                val b2 = buildings[j]

                val aStarRoute = RoutingEngine.findAStarPath(graph, b1.second, b2.second)
                val dijkstraRoute = RoutingEngine.findDijkstraPath(graph, b1.second, b2.second)

                assertNotNull("Route between ${b1.first} and ${b2.first} should be found", aStarRoute)
                assertNotNull("Dijkstra route between ${b1.first} and ${b2.first} should be found", dijkstraRoute)

                assertEquals(
                    "A* distance must match Dijkstra optimal distance between ${b1.first} and ${b2.first}",
                    dijkstraRoute!!.totalDistanceMeters,
                    aStarRoute!!.totalDistanceMeters,
                    0.5
                )

                assertTrue("Walking duration should be realistic", aStarRoute.estimatedTimeSeconds > 0)
                assertTrue("Steps should be generated", aStarRoute.steps.isNotEmpty())
                testedPairs++
            }
        }
        println("AUDIT: Verified $testedPairs pairs of buildings with 100% path parity between A* and Dijkstra.")
    }

    @Test
    fun audit3_buildingCentroidSnappingDistance() {
        for (b in buildings) {
            val nearest = graph.findNearestNode(b.second)
            assertNotNull("Nearest node to ${b.first} must exist", nearest)
            val dist = b.second.distanceTo(nearest!!.point)
            println("Building: ${b.first} -> Nearest Node: ${nearest.id} (distance: %.2fm)".format(dist))
            assertTrue("Building ${b.first} centroid snap distance (${dist}m) must be under 120m", dist < 120.0)
        }
    }
}
