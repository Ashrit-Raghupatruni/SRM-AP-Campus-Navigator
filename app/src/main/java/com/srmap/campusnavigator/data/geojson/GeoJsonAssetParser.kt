package com.srmap.campusnavigator.data.geojson

import android.content.Context
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.graph.GraphEdge
import com.srmap.campusnavigator.domain.graph.GraphNode
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.CampusFeature
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.LocationCategory
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class GeoJsonAssetParser(private val context: Context) {

    fun loadAllCampusFeatures(): List<CampusFeature> {
        val features = mutableListOf<CampusFeature>()
        try {
            val json = readAssetString("campus_all.geojson")
            val root = JSONObject(json)
            val featArray = root.getJSONArray("features")

            for (i in 0 until featArray.length()) {
                val f = featArray.getJSONObject(i)
                val props = f.optJSONObject("properties") ?: JSONObject()
                val geom = f.optJSONObject("geometry") ?: continue
                val id = props.optString("@id", "feat_$i")
                val name = if (props.has("name")) props.getString("name") else null
                val geomType = geom.getString("type")

                val category = determineCategory(props)
                val coordinates = parseCoordinates(geom)

                features.add(
                    CampusFeature(
                        id = id,
                        name = name,
                        category = category,
                        geometryType = geomType,
                        coordinates = coordinates,
                        rawProperties = jsonObjectToMap(props)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return features
    }

    fun loadBuildings(): List<CampusBuilding> {
        val buildings = mutableListOf<CampusBuilding>()
        try {
            val json = readAssetString("buildings.geojson")
            val root = JSONObject(json)
            val featArray = root.getJSONArray("features")

            for (i in 0 until featArray.length()) {
                val f = featArray.getJSONObject(i)
                val props = f.optJSONObject("properties") ?: JSONObject()
                val geom = f.optJSONObject("geometry") ?: continue
                val id = props.optString("@id", "bldg_$i")
                val name = props.optString("name", "")
                val levels = if (props.has("building:levels")) props.getString("building:levels") else null

                val polygon = parseFirstPolygonRing(geom)
                if (polygon.isNotEmpty()) {
                    val centroid = calculateCentroid(polygon)
                    val category = determineCategory(props)

                    buildings.add(
                        CampusBuilding(
                            id = id,
                            name = if (name.isNotBlank()) name else "Facility ($id)",
                            category = category,
                            centroid = centroid,
                            polygon = polygon,
                            levels = levels,
                            rawProperties = jsonObjectToMap(props)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return buildings
    }

    fun buildRoutingGraph(): CampusGraph {
        val graph = CampusGraph()
        try {
            val files = listOf("roads.geojson", "paths.geojson")
            var edgeCounter = 0

            for (file in files) {
                val json = readAssetString(file)
                val root = JSONObject(json)
                val featArray = root.getJSONArray("features")

                for (i in 0 until featArray.length()) {
                    val f = featArray.getJSONObject(i)
                    val props = f.optJSONObject("properties") ?: JSONObject()
                    val geom = f.optJSONObject("geometry") ?: continue
                    val highwayType = props.optString("highway", "path")
                    val isConstruction = props.optString("construction", "").isNotBlank()

                    val coords = parseLineString(geom)
                    if (coords.size >= 2) {
                        for (idx in 0 until coords.size - 1) {
                            val p1 = coords[idx]
                            val p2 = coords[idx + 1]

                            val node1Id = "node_${String.format("%.6f", p1.latitude)}_${String.format("%.6f", p1.longitude)}"
                            val node2Id = "node_${String.format("%.6f", p2.latitude)}_${String.format("%.6f", p2.longitude)}"

                            graph.addNode(GraphNode(node1Id, p1))
                            graph.addNode(GraphNode(node2Id, p2))

                            val dist = p1.distanceTo(p2)
                            val edge = GraphEdge(
                                id = "edge_${edgeCounter++}",
                                fromNodeId = node1Id,
                                toNodeId = node2Id,
                                distanceMeters = dist,
                                highwayType = highwayType,
                                walkable = !isConstruction
                            )
                            graph.addEdge(edge)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return graph
    }

    private fun determineCategory(props: JSONObject): LocationCategory {
        val name = props.optString("name", "").lowercase()
        val tourism = props.optString("tourism", "").lowercase()
        val amenity = props.optString("amenity", "").lowercase()
        val leisure = props.optString("leisure", "").lowercase()

        return when {
            tourism == "hostel" || name.contains("hostel") ||
                    listOf("kaveri", "godavari", "krishna", "yamuna", "narmada", "vedavathi", "ganga").any { name.contains(it) } -> LocationCategory.HOSTEL

            name.contains("mess") || name.contains("annapurna") || amenity == "food_court" -> LocationCategory.DINING

            name.contains("block") || name.contains("lab") || name.contains("raman") ||
                    name.contains("sarabhai") || name.contains("bose") || amenity == "university" -> LocationCategory.ACADEMIC

            leisure == "pitch" || props.has("sport") -> LocationCategory.SPORTS

            props.optString("office", "").isNotBlank() || name.contains("gate") || name.contains("plant") -> LocationCategory.FACILITY

            leisure.isNotBlank() || props.optString("landuse", "").isNotBlank() -> LocationCategory.LANDSCAPE

            else -> LocationCategory.OTHER
        }
    }

    private fun parseFirstPolygonRing(geom: JSONObject): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val type = geom.getString("type")
        val coords = geom.getJSONArray("coordinates")
        if (type == "Polygon" && coords.length() > 0) {
            val ring = coords.getJSONArray(0)
            for (i in 0 until ring.length()) {
                val pt = ring.getJSONArray(i)
                val lon = pt.getDouble(0)
                val lat = pt.getDouble(1)
                points.add(GeoPoint(lat, lon))
            }
        }
        return points
    }

    private fun parseLineString(geom: JSONObject): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val type = geom.getString("type")
        val coords = geom.getJSONArray("coordinates")
        if (type == "LineString") {
            for (i in 0 until coords.length()) {
                val pt = coords.getJSONArray(i)
                points.add(GeoPoint(pt.getDouble(1), pt.getDouble(0)))
            }
        }
        return points
    }

    private fun parseCoordinates(geom: JSONObject): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        when (geom.getString("type")) {
            "Point" -> {
                val pt = geom.getJSONArray("coordinates")
                points.add(GeoPoint(pt.getDouble(1), pt.getDouble(0)))
            }
            "LineString" -> {
                points.addAll(parseLineString(geom))
            }
            "Polygon" -> {
                points.addAll(parseFirstPolygonRing(geom))
            }
        }
        return points
    }

    private fun calculateCentroid(polygon: List<GeoPoint>): GeoPoint {
        if (polygon.isEmpty()) return GeoPoint(16.4632311, 80.5064108)
        var sumLat = 0.0
        var sumLon = 0.0
        for (p in polygon) {
            sumLat += p.latitude
            sumLon += p.longitude
        }
        return GeoPoint(sumLat / polygon.size, sumLon / polygon.size)
    }

    private fun jsonObjectToMap(obj: JSONObject): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.optString(key, "")
        }
        return map
    }

    private fun readAssetString(fileName: String): String {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val sb = StringBuilder()
        var line: String? = reader.readLine()
        while (line != null) {
            sb.append(line).append("\n")
            line = reader.readLine()
        }
        reader.close()
        return sb.toString()
    }
}
