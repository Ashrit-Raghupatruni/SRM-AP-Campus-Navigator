package com.srmap.campusnavigator.data.repository

import com.srmap.campusnavigator.data.datasource.VerifiedAmenitiesDataSource
import com.srmap.campusnavigator.data.datasource.VerifiedFacultyDataSource
import com.srmap.campusnavigator.data.local.dao.CampusDao
import com.srmap.campusnavigator.data.local.entities.RecentSearchEntity
import com.srmap.campusnavigator.domain.models.AmenityCategory
import com.srmap.campusnavigator.domain.models.CampusAmenity
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.UnifiedSearchResult
import com.srmap.campusnavigator.presentation.components.SearchCategoryFilter

interface SearchRepository {
    suspend fun searchCampus(query: String, filter: SearchCategoryFilter): List<UnifiedSearchResult>
    suspend fun getAllAmenities(): List<CampusAmenity>
    suspend fun getRecentSearches(): List<String>
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
}

class SearchRepositoryImpl(
    private val campusRepository: CampusRepository,
    private val dao: CampusDao
) : SearchRepository {

    // Cached pre-built index components for instant zero-lag queries
    @Volatile
    private var isIndexWarm = false
    private val cachedBuildings = mutableListOf<UnifiedSearchResult.CampusBuildingResult>()
    private val cachedRooms = mutableListOf<UnifiedSearchResult.RoomResult>()
    private val cachedFaculty = mutableListOf<UnifiedSearchResult.FacultyResult>()
    private val cachedEquipment = mutableListOf<UnifiedSearchResult.EquipmentResult>()
    private val cachedAmenities = mutableListOf<UnifiedSearchResult.AmenityResult>()

    private suspend fun ensureIndexWarmed() {
        if (isIndexWarm) return
        synchronized(this) {
            if (isIndexWarm) return
        }

        val allRooms = campusRepository.getAllRooms()
        val allFaculty = campusRepository.getAllFaculty()
        val allEquipment = campusRepository.getAllEquipment()

        var buildingsList: List<com.srmap.campusnavigator.domain.models.CampusBuilding> = emptyList()
        campusRepository.getBuildings().collect { buildingsList = it }

        val bldgResults = buildingsList.map {
            UnifiedSearchResult.CampusBuildingResult(it)
        }

        val facultyMap = allFaculty.associateBy { it.roomId }
        val equipmentMap = allEquipment.groupBy { it.roomId }

        val roomResults = allRooms.mapNotNull { r ->
            if (r.latitude != null && r.longitude != null) {
                UnifiedSearchResult.RoomResult(
                    roomId = r.roomId,
                    blockId = r.blockId,
                    floor = r.floor,
                    roomNo = r.roomNo,
                    roomType = r.roomType,
                    point = GeoPoint(r.latitude, r.longitude),
                    facultyName = facultyMap[r.roomId]?.name,
                    department = facultyMap[r.roomId]?.department,
                    equipmentList = equipmentMap[r.roomId]?.joinToString(", ") { it.name },
                    locationStatus = r.locationStatus
                )
            } else null
        }

        val roomMap = allRooms.associateBy { it.roomId }

        val facultyResults = allFaculty.mapNotNull { f ->
            val room = roomMap[f.roomId]
            if (room?.latitude != null && room.longitude != null) {
                UnifiedSearchResult.FacultyResult(
                    facultyName = f.name,
                    designation = f.designation ?: "Faculty",
                    department = f.department ?: "Academic",
                    blockId = room.blockId,
                    floor = room.floor,
                    roomNo = room.roomNo,
                    point = GeoPoint(room.latitude, room.longitude)
                )
            } else null
        }

        val verifiedFacultyResults = VerifiedFacultyDataSource.verifiedFaculty

        val equipmentResults = allEquipment.mapNotNull { eq ->
            val room = roomMap[eq.roomId]
            if (room?.latitude != null && room.longitude != null) {
                UnifiedSearchResult.EquipmentResult(
                    equipmentName = eq.name,
                    blockId = room.blockId,
                    floor = room.floor,
                    roomNo = room.roomNo,
                    labInCharge = room.roomType,
                    point = GeoPoint(room.latitude, room.longitude)
                )
            } else null
        }

        val amenityResults = VerifiedAmenitiesDataSource.verifiedAmenities.map { a ->
            UnifiedSearchResult.AmenityResult(
                amenityId = a.id,
                name = a.name,
                category = a.category,
                blockId = a.blockId,
                floor = a.floor,
                locationDescription = a.locationDescription,
                point = GeoPoint(a.latitude, a.longitude),
                isVerified = a.isVerified,
                openingHours = a.openingHours
            )
        }

        synchronized(this) {
            cachedBuildings.clear()
            cachedBuildings.addAll(bldgResults)

            cachedRooms.clear()
            cachedRooms.addAll(roomResults)

            cachedFaculty.clear()
            cachedFaculty.addAll(facultyResults)
            cachedFaculty.addAll(verifiedFacultyResults)

            cachedEquipment.clear()
            cachedEquipment.addAll(equipmentResults)

            cachedAmenities.clear()
            cachedAmenities.addAll(amenityResults)

            isIndexWarm = true
        }
    }

    override suspend fun getAllAmenities(): List<CampusAmenity> {
        return VerifiedAmenitiesDataSource.verifiedAmenities
    }

    override suspend fun searchCampus(query: String, filter: SearchCategoryFilter): List<UnifiedSearchResult> {
        ensureIndexWarmed()

        val q = query.trim()
        val normQ = q.replace("-", "").replace(" ", "").uppercase()

        // 1. Filter candidates by category filter
        val candidates: List<UnifiedSearchResult> = when (filter) {
            SearchCategoryFilter.ALL -> cachedBuildings + cachedAmenities + cachedRooms + cachedFaculty + cachedEquipment
            SearchCategoryFilter.BUILDINGS -> cachedBuildings
            SearchCategoryFilter.ROOMS -> cachedRooms
            SearchCategoryFilter.FACULTY -> cachedFaculty
            SearchCategoryFilter.AMENITIES -> cachedAmenities
            SearchCategoryFilter.WATER -> cachedAmenities.filter { it.category == AmenityCategory.WATER_COOLER }
            SearchCategoryFilter.FOOD -> cachedAmenities.filter { it.category == AmenityCategory.FOOD_CAFE }
            SearchCategoryFilter.ATM -> cachedAmenities.filter { it.category == AmenityCategory.ATM }
            SearchCategoryFilter.PRINTING -> cachedAmenities.filter { it.category == AmenityCategory.PRINTING || it.category == AmenityCategory.STATIONERY }
            SearchCategoryFilter.HEALTH -> cachedAmenities.filter { it.category == AmenityCategory.HEALTHCARE }
            SearchCategoryFilter.EQUIPMENT -> cachedEquipment
            SearchCategoryFilter.LABS -> cachedRooms.filter {
                it.roomType.contains("Lab", ignoreCase = true) || it.roomNo.contains("Lab", ignoreCase = true)
            }
        }

        if (q.isBlank()) return candidates

        // 2. High-Performance Multi-Stage Match & Rank Pipeline:
        //    Tier 1: Exact matches (Score: 100)
        //    Tier 2: Prefix matches (Score: 80)
        //    Tier 3: Word-boundary substring matches (Score: 60)
        //    Tier 4: Substring / Normalized token matches (Score: 40)
        return candidates.mapNotNull { res ->
            val score = computeMatchScore(res, q, normQ)
            if (score > 0) Pair(res, score) else null
        }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.title + it.subtitle }
    }

    private fun computeMatchScore(res: UnifiedSearchResult, q: String, normQ: String): Int {
        return when (res) {
            is UnifiedSearchResult.RoomResult -> {
                val cleanRoom = res.roomNo.replace("-", "").uppercase()
                when {
                    res.roomNo.equals(q, ignoreCase = true) || cleanRoom == normQ -> 100
                    res.roomNo.startsWith(q, ignoreCase = true) || cleanRoom.startsWith(normQ) -> 85
                    res.roomNo.contains(q, ignoreCase = true) || cleanRoom.contains(normQ) -> 65
                    res.blockId.contains(q, ignoreCase = true) -> 45
                    res.roomType.contains(q, ignoreCase = true) -> 40
                    res.facultyName != null && res.facultyName.contains(q, ignoreCase = true) -> 55
                    res.equipmentList != null && res.equipmentList.contains(q, ignoreCase = true) -> 35
                    else -> 0
                }
            }
            is UnifiedSearchResult.AmenityResult -> {
                when {
                    res.name.equals(q, ignoreCase = true) -> 100
                    res.name.startsWith(q, ignoreCase = true) -> 90
                    res.name.contains(q, ignoreCase = true) -> 75
                    res.category.displayName.contains(q, ignoreCase = true) -> 60
                    res.locationDescription.contains(q, ignoreCase = true) -> 50
                    else -> 0
                }
            }
            is UnifiedSearchResult.FacultyResult -> {
                when {
                    res.facultyName.equals(q, ignoreCase = true) -> 100
                    res.facultyName.startsWith(q, ignoreCase = true) -> 90
                    res.facultyName.contains(q, ignoreCase = true) -> 70
                    res.department != null && res.department.contains(q, ignoreCase = true) -> 50
                    res.roomNo.contains(q, ignoreCase = true) -> 40
                    else -> 0
                }
            }
            is UnifiedSearchResult.CampusBuildingResult -> {
                when {
                    res.building.name.equals(q, ignoreCase = true) -> 100
                    res.building.name.startsWith(q, ignoreCase = true) -> 85
                    res.building.name.contains(q, ignoreCase = true) -> 65
                    else -> 0
                }
            }
            is UnifiedSearchResult.EquipmentResult -> {
                when {
                    res.equipmentName.equals(q, ignoreCase = true) -> 100
                    res.equipmentName.startsWith(q, ignoreCase = true) -> 80
                    res.equipmentName.contains(q, ignoreCase = true) -> 60
                    else -> 0
                }
            }
            else -> 0
        }
    }

    override suspend fun getRecentSearches(): List<String> {
        return dao.getRecentSearches()
    }

    override suspend fun addRecentSearch(query: String) {
        if (query.isNotBlank()) {
            dao.insertRecentSearch(RecentSearchEntity(query = query.trim()))
        }
    }

    override suspend fun clearRecentSearches() {
        dao.clearRecentSearches()
    }
}
