package com.srmap.campusnavigator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import com.srmap.campusnavigator.domain.routing.DynamicRouteProgress
import com.srmap.campusnavigator.domain.routing.NavigationProgressTracker
import com.srmap.campusnavigator.domain.routing.PedestrianMapMatcher
import com.srmap.campusnavigator.domain.routing.RoutingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NavigationUiState(
    val activeRoute: PedestrianRoute? = null,
    val activeDestination: GeoPoint? = null,
    val activeDestinationName: String = "",
    val navTargetBlockName: String? = null,
    val navTargetFloor: Int? = null,
    val navTargetRoomNo: String? = null,
    val dynamicProgress: DynamicRouteProgress? = null,
    val userLocation: UserLocationState? = null,
    val isNavigating: Boolean = false,
    val is3DMode: Boolean = false
)

class NavigationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var campusGraph: CampusGraph? = null
    private var progressTracker: NavigationProgressTracker? = null
    private var mapMatcher: PedestrianMapMatcher? = null

    fun setGraph(graph: CampusGraph) {
        campusGraph = graph
        progressTracker = NavigationProgressTracker(graph)
        mapMatcher = PedestrianMapMatcher(graph)
    }

    fun getGraph(): CampusGraph? = campusGraph

    fun startNavigation(
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        destinationName: String,
        blockName: String? = null,
        floor: Int? = null,
        roomNo: String? = null
    ): Boolean {
        val g = campusGraph ?: return false
        val route = RoutingEngine.findAStarPath(
            graph = g,
            startPoint = startPoint,
            endPoint = endPoint,
            targetBlockName = blockName,
            targetFloor = floor,
            targetRoomNo = roomNo
        ) ?: return false

        progressTracker?.startNavigation(route, endPoint)
        val initialProgress = progressTracker?.updateProgress(startPoint)

        _uiState.value = _uiState.value.copy(
            activeRoute = route,
            activeDestination = endPoint,
            activeDestinationName = destinationName,
            navTargetBlockName = blockName,
            navTargetFloor = floor,
            navTargetRoomNo = roomNo,
            dynamicProgress = initialProgress,
            isNavigating = true
        )
        return true
    }

    fun updateLocation(location: UserLocationState) {
        _uiState.value = _uiState.value.copy(userLocation = location)

        if (_uiState.value.isNavigating && progressTracker != null) {
            val progress = progressTracker?.updateProgress(location.point)
            if (progress != null) {
                _uiState.value = _uiState.value.copy(dynamicProgress = progress)
            }
        }
    }

    fun toggle3DMode() {
        _uiState.value = _uiState.value.copy(is3DMode = !_uiState.value.is3DMode)
    }

    fun set3DMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(is3DMode = enabled)
    }

    fun endNavigation() {
        progressTracker?.stopNavigation()
        _uiState.value = _uiState.value.copy(
            activeRoute = null,
            activeDestination = null,
            activeDestinationName = "",
            navTargetBlockName = null,
            navTargetFloor = null,
            navTargetRoomNo = null,
            dynamicProgress = null,
            isNavigating = false,
            is3DMode = false
        )
    }
}
