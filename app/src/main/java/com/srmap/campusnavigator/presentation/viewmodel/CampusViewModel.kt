package com.srmap.campusnavigator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srmap.campusnavigator.data.local.entities.EquipmentEntity
import com.srmap.campusnavigator.data.local.entities.FacultyEntity
import com.srmap.campusnavigator.data.local.entities.RoomEntity
import com.srmap.campusnavigator.data.maptiler.maps.MapTilerStyle
import com.srmap.campusnavigator.data.repository.CampusRepository
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.UnifiedSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CampusUiState(
    val buildings: List<CampusBuilding> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val selectedBuilding: CampusBuilding? = null,
    val selectedRoom: UnifiedSearchResult.RoomResult? = null,
    val buildingRooms: List<RoomEntity> = emptyList(),
    val buildingFaculty: List<FacultyEntity> = emptyList(),
    val buildingEquipment: List<EquipmentEntity> = emptyList(),
    val activeFloorFilter: Int? = null,
    val currentMapStyle: MapTilerStyle = MapTilerStyle.STREETS,
    val showLayersSheet: Boolean = false,
    val showFloorExplorerSheet: Boolean = false
)

class CampusViewModel(
    private val campusRepository: CampusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampusUiState())
    val uiState: StateFlow<CampusUiState> = _uiState.asStateFlow()

    init {
        loadCampusData()
    }

    private fun loadCampusData() {
        viewModelScope.launch(Dispatchers.IO) {
            campusRepository.getBuildings().collect { bldgs ->
                _uiState.value = _uiState.value.copy(buildings = bldgs)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            campusRepository.getFavoriteIds().collect { favs ->
                _uiState.value = _uiState.value.copy(favoriteIds = favs.toSet())
            }
        }
    }

    fun selectBuilding(building: CampusBuilding?) {
        _uiState.value = _uiState.value.copy(
            selectedBuilding = building,
            selectedRoom = null
        )
        if (building != null) {
            loadBuildingDetails(building)
        } else {
            _uiState.value = _uiState.value.copy(
                buildingRooms = emptyList(),
                buildingFaculty = emptyList(),
                buildingEquipment = emptyList(),
                activeFloorFilter = null
            )
        }
    }

    private fun loadBuildingDetails(building: CampusBuilding) {
        val canonicalBlockId = when {
            building.name.contains("SR", ignoreCase = true) -> "SR1"
            building.name.contains("CV", ignoreCase = true) || building.name.contains("Raman", ignoreCase = true) -> "CV"
            building.name.contains("V Block", ignoreCase = true) || building.name.equals("V", ignoreCase = true) -> "V"
            building.name.contains("X Lab", ignoreCase = true) || building.name.contains("X-Lab", ignoreCase = true) -> "X-LAB"
            building.name.contains("Homi", ignoreCase = true) -> "HOMI"
            else -> building.name
        }
        viewModelScope.launch(Dispatchers.IO) {
            val rms = campusRepository.getRoomsForBlock(canonicalBlockId)
            val facs = campusRepository.getFacultyForBlock(canonicalBlockId)
            val eqs = campusRepository.getEquipmentForBlock(canonicalBlockId)
            _uiState.value = _uiState.value.copy(
                buildingRooms = rms,
                buildingFaculty = facs,
                buildingEquipment = eqs
            )
        }
    }

    fun selectRoom(room: UnifiedSearchResult.RoomResult?) {
        _uiState.value = _uiState.value.copy(
            selectedRoom = room,
            selectedBuilding = null
        )
    }

    fun setFloorFilter(floor: Int?) {
        _uiState.value = _uiState.value.copy(activeFloorFilter = floor)
    }

    fun setMapStyle(style: MapTilerStyle) {
        _uiState.value = _uiState.value.copy(currentMapStyle = style)
    }

    fun setShowLayersSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLayersSheet = show)
    }

    fun setShowFloorExplorerSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFloorExplorerSheet = show)
    }

    fun toggleFavorite(building: CampusBuilding) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = building.id in _uiState.value.favoriteIds
            campusRepository.toggleFavorite(building.id, !isFav)
        }
    }
}
