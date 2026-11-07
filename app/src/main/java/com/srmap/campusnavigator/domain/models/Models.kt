package com.srmap.campusnavigator.domain.models

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    fun distanceTo(other: GeoPoint): Double {
        val r = 6371000.0 // Earth radius in meters
        val lat1Rad = Math.toRadians(latitude)
        val lat2Rad = Math.toRadians(other.latitude)
        val deltaLat = Math.toRadians(other.latitude - latitude)
        val deltaLon = Math.toRadians(other.longitude - longitude)

        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun bearingTo(other: GeoPoint): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val deltaLon = Math.toRadians(other.longitude - longitude)

        val y = Math.sin(deltaLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon)
        val bearing = Math.toDegrees(Math.atan2(y, x))
        return (bearing + 360.0) % 360.0
    }
}

enum class LocationCategory {
    ACADEMIC,
    HOSTEL,
    DINING,
    FACILITY,
    SPORTS,
    LANDSCAPE,
    OTHER
}

data class CampusBuilding(
    val id: String,
    val name: String,
    val category: LocationCategory,
    val centroid: GeoPoint,
    val polygon: List<GeoPoint>,
    val levels: String? = null,
    val rawProperties: Map<String, String> = emptyMap()
) {
    /**
     * Ray-casting algorithm to test whether a given lat/lon point lies inside the polygon
     */
    fun containsPoint(pt: GeoPoint): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.latitude > pt.latitude) != (pj.latitude > pt.latitude) &&
                pt.longitude < (pj.longitude - pi.longitude) * (pt.latitude - pi.latitude) / (pj.latitude - pi.latitude) + pi.longitude
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}

data class CampusFeature(
    val id: String,
    val name: String?,
    val category: LocationCategory,
    val geometryType: String,
    val coordinates: List<GeoPoint>,
    val rawProperties: Map<String, String> = emptyMap()
)

data class RouteStep(
    val instruction: String,
    val distanceMeters: Double,
    val startPoint: GeoPoint,
    val endPoint: GeoPoint
)

data class PedestrianRoute(
    val path: List<GeoPoint>,
    val totalDistanceMeters: Double,
    val estimatedTimeSeconds: Int,
    val steps: List<RouteStep>
)
