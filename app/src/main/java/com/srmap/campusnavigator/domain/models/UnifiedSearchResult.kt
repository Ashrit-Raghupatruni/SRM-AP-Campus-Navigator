package com.srmap.campusnavigator.domain.models

sealed class UnifiedSearchResult {
    abstract val title: String
    abstract val subtitle: String
    abstract val point: GeoPoint

    data class CampusBuildingResult(
        val building: CampusBuilding
    ) : UnifiedSearchResult() {
        override val title: String get() = building.name
        override val subtitle: String get() = "SRM AP Campus • ${building.category.name}"
        override val point: GeoPoint get() = building.centroid
    }

    data class MapTilerPlaceResult(
        val id: String,
        override val title: String,
        override val subtitle: String,
        override val point: GeoPoint
    ) : UnifiedSearchResult()
}
