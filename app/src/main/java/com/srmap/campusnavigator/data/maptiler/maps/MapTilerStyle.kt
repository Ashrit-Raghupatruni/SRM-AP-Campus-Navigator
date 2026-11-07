package com.srmap.campusnavigator.data.maptiler.maps

enum class MapTilerStyle(val displayName: String, val styleId: String, val subtitle: String) {
    STREETS("Default Vector", "streets-v2", "Clean vector campus design"),
    SATELLITE("Satellite", "satellite", "High-res aerial imagery"),
    HYBRID("Hybrid", "hybrid", "Satellite with roads & labels"),
    TOPO("Topographic", "topo-v2", "Contour terrain & elevation lines"),
    OUTDOOR("Outdoor", "outdoor-v2", "High contrast pedestrian landscape");

    fun getStyleUrl(apiKey: String): String {
        return "https://api.maptiler.com/maps/$styleId/style.json?key=$apiKey"
    }

    fun getRasterTileUrl(z: Int, x: Int, y: Int, apiKey: String): String {
        return "https://api.maptiler.com/maps/$styleId/$z/$x/$y.png?key=$apiKey"
    }
}
