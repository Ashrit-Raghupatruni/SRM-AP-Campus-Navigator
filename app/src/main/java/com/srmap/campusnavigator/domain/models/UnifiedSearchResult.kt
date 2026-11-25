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

    data class RoomResult(
        val roomId: String,
        val blockId: String,
        val floor: Int,
        val roomNo: String,
        val roomType: String,
        override val point: GeoPoint,
        val facultyName: String? = null,
        val department: String? = null,
        val equipmentList: String? = null,
        val locationStatus: String = "EXACT_MATCH"
    ) : UnifiedSearchResult() {
        override val title: String get() = "$blockId - $roomNo ($roomType)"
        override val subtitle: String get() = "$blockId Block • Floor $floor" +
                (if (!facultyName.isNullOrBlank()) " • $facultyName" else "") +
                (if (!equipmentList.isNullOrBlank()) " • Eq: $equipmentList" else "")
    }

    data class FacultyResult(
        val facultyName: String,
        val designation: String?,
        val department: String?,
        val blockId: String,
        val floor: Int,
        val roomNo: String,
        override val point: GeoPoint,
        val email: String? = null,
        val education: String? = null,
        val profileUrl: String? = null
    ) : UnifiedSearchResult() {
        override val title: String get() = facultyName
        override val subtitle: String get() {
            val role = designation ?: "Faculty"
            val dept = department ?: "SRM University-AP"
            val loc = if (roomNo.isNotBlank()) " • $blockId F$floor ($roomNo)" else " • $blockId Block"
            return "$role • $dept$loc"
        }
    }

    data class EquipmentResult(
        val equipmentName: String,
        val roomNo: String,
        val blockId: String,
        val floor: Int,
        val labInCharge: String?,
        override val point: GeoPoint
    ) : UnifiedSearchResult() {
        override val title: String get() = equipmentName
        override val subtitle: String get() = "Available in $blockId F$floor ($roomNo)" +
                (if (!labInCharge.isNullOrBlank()) " • Lab In-Charge: $labInCharge" else "")
    }

    data class AmenityResult(
        val amenityId: String,
        val name: String,
        val category: AmenityCategory,
        val blockId: String,
        val floor: Int,
        val locationDescription: String,
        override val point: GeoPoint,
        val isVerified: Boolean = true,
        val openingHours: String? = null
    ) : UnifiedSearchResult() {
        override val title: String get() = name
        override val subtitle: String get() = "${category.displayName} • $locationDescription (Floor $floor)" +
                (if (!openingHours.isNullOrBlank()) " • $openingHours" else "")
    }

    data class MapTilerPlaceResult(
        val id: String,
        override val title: String,
        override val subtitle: String,
        override val point: GeoPoint
    ) : UnifiedSearchResult()
}
