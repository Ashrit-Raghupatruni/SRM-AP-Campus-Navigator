package com.srmap.campusnavigator.data.repository

import com.srmap.campusnavigator.data.local.dao.CampusDao
import com.srmap.campusnavigator.data.local.entities.RecentSearchEntity
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.UnifiedSearchResult
import com.srmap.campusnavigator.presentation.components.SearchCategoryFilter

interface SearchRepository {
    suspend fun searchCampus(query: String, filter: SearchCategoryFilter): List<UnifiedSearchResult>
    suspend fun getRecentSearches(): List<String>
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
}

class SearchRepositoryImpl(
    private val campusRepository: CampusRepository,
    private val dao: CampusDao
) : SearchRepository {

    override suspend fun searchCampus(query: String, filter: SearchCategoryFilter): List<UnifiedSearchResult> {
        val q = query.trim()
        val allRooms = campusRepository.getAllRooms()
        val allFaculty = campusRepository.getAllFaculty()
        val allEquipment = campusRepository.getAllEquipment()

        var buildingsList: List<com.srmap.campusnavigator.domain.models.CampusBuilding> = emptyList()
        campusRepository.getBuildings().collect { buildingsList = it }

        val buildingResults = buildingsList.map {
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

        val combined: List<UnifiedSearchResult> = when (filter) {
            SearchCategoryFilter.ALL -> buildingResults + roomResults + facultyResults + equipmentResults
            SearchCategoryFilter.BUILDINGS -> buildingResults
            SearchCategoryFilter.ROOMS -> roomResults
            SearchCategoryFilter.FACULTY -> facultyResults
            SearchCategoryFilter.LABS -> roomResults.filter { it.roomType.contains("Lab", ignoreCase = true) || it.roomNo.contains("Lab", ignoreCase = true) }
            SearchCategoryFilter.EQUIPMENT -> equipmentResults
        }

        if (q.isBlank()) return combined

        val normQ = q.replace("-", "").replace(" ", "").uppercase()

        // Filter and Rank
        return combined.filter { res ->
            when (res) {
                is UnifiedSearchResult.CampusBuildingResult -> res.building.name.contains(q, ignoreCase = true)
                is UnifiedSearchResult.RoomResult -> {
                    res.roomNo.contains(q, ignoreCase = true) ||
                            res.roomNo.replace("-", "").contains(normQ, ignoreCase = true) ||
                            res.blockId.contains(q, ignoreCase = true) ||
                            res.roomType.contains(q, ignoreCase = true) ||
                            (res.facultyName != null && res.facultyName.contains(q, ignoreCase = true)) ||
                            (res.equipmentList != null && res.equipmentList.contains(q, ignoreCase = true))
                }
                is UnifiedSearchResult.FacultyResult -> {
                    res.facultyName.contains(q, ignoreCase = true) ||
                            (res.department != null && res.department.contains(q, ignoreCase = true)) ||
                            (res.designation != null && res.designation.contains(q, ignoreCase = true)) ||
                            res.roomNo.contains(q, ignoreCase = true)
                }
                is UnifiedSearchResult.EquipmentResult -> {
                    res.equipmentName.contains(q, ignoreCase = true) ||
                            res.blockId.contains(q, ignoreCase = true) ||
                            res.roomNo.contains(q, ignoreCase = true)
                }
                else -> false
            }
        }.sortedBy { res ->
            when (res) {
                is UnifiedSearchResult.RoomResult -> if (res.roomNo.equals(q, ignoreCase = true)) 1 else 2
                is UnifiedSearchResult.FacultyResult -> if (res.facultyName.startsWith(q, ignoreCase = true)) 1 else 3
                is UnifiedSearchResult.EquipmentResult -> if (res.equipmentName.startsWith(q, ignoreCase = true)) 1 else 3
                is UnifiedSearchResult.CampusBuildingResult -> if (res.building.name.startsWith(q, ignoreCase = true)) 1 else 4
                else -> 5
            }
        }.distinctBy { it.title + it.subtitle }
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
