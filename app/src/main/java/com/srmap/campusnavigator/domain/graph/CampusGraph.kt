package com.srmap.campusnavigator.domain.graph

import com.srmap.campusnavigator.domain.models.GeoPoint

data class GraphNode(
    val id: String,
    val point: GeoPoint
)

data class GraphEdge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Double,
    val highwayType: String,
    val walkable: Boolean = true
)

class CampusGraph {
    private val _nodes = mutableMapOf<String, GraphNode>()
    private val _edges = mutableMapOf<String, MutableList<GraphEdge>>()

    val nodes: Map<String, GraphNode> get() = _nodes
    val edges: Map<String, List<GraphEdge>> get() = _edges

    fun addNode(node: GraphNode) {
        _nodes[node.id] = node
        if (!_edges.containsKey(node.id)) {
            _edges[node.id] = mutableListOf()
        }
    }

    fun addEdge(edge: GraphEdge) {
        _edges.getOrPut(edge.fromNodeId) { mutableListOf() }.add(edge)
        val reverse = GraphEdge(
            id = "${edge.id}_rev",
            fromNodeId = edge.toNodeId,
            toNodeId = edge.fromNodeId,
            distanceMeters = edge.distanceMeters,
            highwayType = edge.highwayType,
            walkable = edge.walkable
        )
        _edges.getOrPut(edge.toNodeId) { mutableListOf() }.add(reverse)
    }

    fun findNearestNode(point: GeoPoint): GraphNode? {
        if (_nodes.isEmpty()) return null
        return _nodes.values.minByOrNull { it.point.distanceTo(point) }
    }
}
