package com.srmap.campusnavigator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.srmap.campusnavigator.core.designsystem.*
import com.srmap.campusnavigator.core.location.CampusLocationClient
import com.srmap.campusnavigator.core.location.GpsFilterEngine
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.data.geojson.GeoJsonAssetParser
import com.srmap.campusnavigator.data.repository.CampusRepositoryImpl
import com.srmap.campusnavigator.data.repository.SearchRepositoryImpl
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.models.CampusFeature
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.UnifiedSearchResult
import com.srmap.campusnavigator.presentation.bottomsheet.BuildingDetailsSheet
import com.srmap.campusnavigator.presentation.bottomsheet.RoomDetailsSheet
import com.srmap.campusnavigator.presentation.bottomsheet.RouteNavigationSheet
import com.srmap.campusnavigator.presentation.components.*
import com.srmap.campusnavigator.presentation.floorexplorer.FloorExplorerSheet
import com.srmap.campusnavigator.presentation.map.InteractiveCampusCanvasMap
import com.srmap.campusnavigator.presentation.navigation.CampusTab
import com.srmap.campusnavigator.presentation.profile.ProfileScreen
import com.srmap.campusnavigator.presentation.saved.SavedLocationsScreen
import com.srmap.campusnavigator.presentation.search.SearchScreen
import com.srmap.campusnavigator.presentation.viewmodel.CampusViewModel
import com.srmap.campusnavigator.presentation.viewmodel.NavigationViewModel
import com.srmap.campusnavigator.presentation.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var darkThemeEnabled by remember { mutableStateOf(false) }

            SrmCampusTheme(darkTheme = darkThemeEnabled) {
                CampusNavigatorMainScreen(
                    isDarkTheme = darkThemeEnabled,
                    onToggleDarkTheme = { darkThemeEnabled = it }
                )
            }
        }
    }
}

@Composable
fun CampusNavigatorMainScreen(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as CampusApplication
    val dao = app.database.campusDao()

    val parser = remember { GeoJsonAssetParser(context) }

    // Architecture: Instantiate Repositories
    val campusRepository = remember { CampusRepositoryImpl(dao, parser) }
    val searchRepository = remember { SearchRepositoryImpl(campusRepository, dao) }

    // Architecture: Instantiate ViewModels
    val campusViewModel = remember { CampusViewModel(campusRepository) }
    val searchViewModel = remember { SearchViewModel(searchRepository) }
    val navigationViewModel = remember { NavigationViewModel() }

    val campusState by campusViewModel.uiState.collectAsState()
    val searchState by searchViewModel.uiState.collectAsState()
    val navState by navigationViewModel.uiState.collectAsState()

    val locationClient = remember { CampusLocationClient(context) }
    val gpsFilter = remember { GpsFilterEngine() }

    var currentTab by remember { mutableStateOf(CampusTab.EXPLORE) }
    var features by remember { mutableStateOf<List<CampusFeature>>(emptyList()) }
    var focusTarget by remember { mutableStateOf<GeoPoint?>(null) }
    var mapZoomLevel by remember { mutableFloatStateOf(1.0f) }
    var selectedShortcutId by remember { mutableStateOf<String?>(null) }

    val shortcuts = listOf(
        CategoryShortcut("academic", "Academic", Icons.Default.School, AccentAcademic),
        CategoryShortcut("classrooms", "Classrooms", Icons.Default.MeetingRoom, AccentAcademic),
        CategoryShortcut("faculty", "Faculty", Icons.Default.Person, AccentFaculty),
        CategoryShortcut("labs", "Labs", Icons.Default.Science, AccentLab),
        CategoryShortcut("hostels", "Hostels", Icons.Default.Home, AccentHostel),
        CategoryShortcut("dining", "Dining", Icons.Default.Restaurant, AccentDining),
        CategoryShortcut("sports", "Sports", Icons.Default.SportsBasketball, AccentSports)
    )

    // Helper to process location updates
    fun onNewRawLocation(rawState: UserLocationState) {
        if (!locationClient.isLocationEnabled()) {
            return
        }
        val smoothed = gpsFilter.filterAndSmooth(rawState) ?: return
        navigationViewModel.updateLocation(smoothed)
    }

    // Location Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            scope.launch {
                locationClient.getLocationUpdates().collectLatest { state ->
                    onNewRawLocation(state)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            scope.launch {
                locationClient.getLocationUpdates().collectLatest { state ->
                    onNewRawLocation(state)
                }
            }
        }

        withContext(Dispatchers.IO) {
            val feats = parser.loadAllCampusFeatures()
            val g = parser.buildRoutingGraph()

            withContext(Dispatchers.Main) {
                features = feats
                navigationViewModel.setGraph(g)
            }
        }
    }

    val isFavorite = campusState.selectedBuilding?.id in campusState.favoriteIds
    val favoriteBuildings = campusState.buildings.filter { it.id in campusState.favoriteIds }

    Scaffold(
        bottomBar = {
            CampusBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    CampusTab.EXPLORE -> {
                        if (navState.is3DMode && navState.activeRoute != null) {
                            com.srmap.campusnavigator.presentation.navigation3d.FirstPersonNavigationScreen(
                                buildings = campusState.buildings,
                                graph = navigationViewModel.getGraph(),
                                selectedBuilding = campusState.selectedBuilding,
                                route = navState.activeRoute,
                                userLocation = navState.userLocation,
                                onSwitchTo2D = { navigationViewModel.set3DMode(false) },
                                onEndNavigation = { navigationViewModel.endNavigation() }
                            )
                        } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Interactive Canvas Map
                            InteractiveCampusCanvasMap(
                                buildings = campusState.buildings,
                                features = features,
                                selectedBuilding = campusState.selectedBuilding,
                                route = navState.activeRoute,
                                routeProgress = navState.dynamicProgress,
                                userLocation = navState.userLocation,
                                mapStyle = campusState.currentMapStyle,
                                zoomLevel = mapZoomLevel,
                                focusTarget = focusTarget,
                                highlightedRoom = campusState.selectedRoom,
                                activeFloor = campusState.activeFloorFilter,
                                roomsOnFloor = campusState.buildingRooms.filter { it.floor == campusState.activeFloorFilter },
                                onBuildingSelected = { bldg ->
                                    campusViewModel.selectBuilding(bldg)
                                    focusTarget = bldg.centroid
                                }
                            )

                            // Explore Top Bar (Search + Quick Shortcuts)
                            ExploreTopBar(
                                query = searchState.query,
                                onQueryChange = { searchViewModel.onQueryChange(it) },
                                onSearchClick = { currentTab = CampusTab.SEARCH },
                                shortcuts = shortcuts,
                                selectedShortcut = selectedShortcutId,
                                onSelectShortcut = { scId ->
                                    selectedShortcutId = if (selectedShortcutId == scId) null else scId
                                    when (scId) {
                                        "classrooms", "faculty", "labs" -> campusViewModel.setShowFloorExplorerSheet(true)
                                        "academic" -> {
                                            val b = campusState.buildings.firstOrNull { it.name.contains("SR", ignoreCase = true) }
                                            if (b != null) campusViewModel.selectBuilding(b)
                                        }
                                        "hostels" -> {
                                            val h = campusState.buildings.firstOrNull { it.category == com.srmap.campusnavigator.domain.models.LocationCategory.HOSTEL }
                                            if (h != null) campusViewModel.selectBuilding(h)
                                        }
                                        "dining" -> {
                                            val d = campusState.buildings.firstOrNull { it.category == com.srmap.campusnavigator.domain.models.LocationCategory.DINING }
                                            if (d != null) campusViewModel.selectBuilding(d)
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.TopCenter)
                            )

                            // Floating Controls Stack (+, -, Compass, Layers, Recenter)
                            MapFloatingControls(
                                onZoomIn = {
                                    mapZoomLevel = (mapZoomLevel * 1.35f).coerceAtMost(5.0f)
                                },
                                onZoomOut = {
                                    mapZoomLevel = (mapZoomLevel / 1.35f).coerceAtLeast(0.7f)
                                },
                                onRecenter = {
                                    val loc = navState.userLocation
                                    if (loc != null) {
                                        focusTarget = loc.point
                                    } else {
                                        Toast.makeText(context, "Location disabled. Please turn on device GPS.", Toast.LENGTH_SHORT).show()
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                                onToggleLayers = {
                                    campusViewModel.setShowLayersSheet(true)
                                },
                                onResetNorth = {
                                    mapZoomLevel = 1.0f
                                    focusTarget = GeoPoint(16.4632311, 80.5064108)
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 16.dp)
                            )

                            // Room Details Bottom Sheet
                            campusState.selectedRoom?.let { room ->
                                RoomDetailsSheet(
                                    room = room,
                                    onClose = { campusViewModel.selectRoom(null) },
                                    onNavigate = {
                                        val start = navState.userLocation?.point ?: GeoPoint(16.4632311, 80.5064108)
                                        val started = navigationViewModel.startNavigation(
                                            startPoint = start,
                                            endPoint = room.point,
                                            destinationName = "${room.blockId} - Room ${room.roomNo}",
                                            blockName = "${room.blockId} Block",
                                            floor = room.floor,
                                            roomNo = room.roomNo
                                        )
                                        if (started) {
                                            campusViewModel.selectRoom(null)
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }

                            // Building Details Bottom Sheet
                            BuildingDetailsSheet(
                                building = campusState.selectedBuilding,
                                isFavorite = isFavorite,
                                roomsInBlock = campusState.buildingRooms,
                                facultyInBlock = campusState.buildingFaculty,
                                equipmentInBlock = campusState.buildingEquipment,
                                onNavigateClick = { destination ->
                                    val start = navState.userLocation?.point ?: GeoPoint(16.4632311, 80.5064108)
                                    navigationViewModel.startNavigation(
                                        startPoint = start,
                                        endPoint = destination.centroid,
                                        destinationName = destination.name,
                                        blockName = destination.name,
                                        floor = 1,
                                        roomNo = null
                                    )
                                    campusViewModel.selectBuilding(null)
                                },
                                onOpenFloorExplorer = {
                                    campusViewModel.setShowFloorExplorerSheet(true)
                                },
                                onSelectRoom = { rm ->
                                    if (rm.latitude != null && rm.longitude != null) {
                                        campusViewModel.selectRoom(
                                            UnifiedSearchResult.RoomResult(
                                                roomId = rm.roomId,
                                                blockId = rm.blockId,
                                                floor = rm.floor,
                                                roomNo = rm.roomNo,
                                                roomType = rm.roomType,
                                                point = GeoPoint(rm.latitude, rm.longitude),
                                                locationStatus = rm.locationStatus
                                            )
                                        )
                                        focusTarget = GeoPoint(rm.latitude, rm.longitude)
                                    }
                                },
                                onToggleFavorite = { bldg ->
                                    campusViewModel.toggleFavorite(bldg)
                                },
                                onDismiss = { campusViewModel.selectBuilding(null) },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )

                            // Building & Floor Explorer Bottom Sheet
                            FloorExplorerSheet(
                                visible = campusState.showFloorExplorerSheet,
                                buildings = campusState.buildings,
                                selectedBuilding = campusState.selectedBuilding,
                                rooms = campusState.buildingRooms,
                                onSelectBuilding = { bldg -> campusViewModel.selectBuilding(bldg) },
                                onSelectRoom = { rm ->
                                    if (rm.latitude != null && rm.longitude != null) {
                                        campusViewModel.selectRoom(
                                            UnifiedSearchResult.RoomResult(
                                                roomId = rm.roomId,
                                                blockId = rm.blockId,
                                                floor = rm.floor,
                                                roomNo = rm.roomNo,
                                                roomType = rm.roomType,
                                                point = GeoPoint(rm.latitude, rm.longitude),
                                                locationStatus = rm.locationStatus
                                            )
                                        )
                                        campusViewModel.setShowFloorExplorerSheet(false)
                                        focusTarget = GeoPoint(rm.latitude, rm.longitude)
                                    }
                                },
                                onNavigateRoom = { rm ->
                                    if (rm.latitude != null && rm.longitude != null) {
                                        val start = navState.userLocation?.point ?: GeoPoint(16.4632311, 80.5064108)
                                        navigationViewModel.startNavigation(
                                            startPoint = start,
                                            endPoint = GeoPoint(rm.latitude, rm.longitude),
                                            destinationName = "${rm.blockId} - Room ${rm.roomNo}",
                                            blockName = "${rm.blockId} Block",
                                            floor = rm.floor,
                                            roomNo = rm.roomNo
                                        )
                                        campusViewModel.setShowFloorExplorerSheet(false)
                                    }
                                },
                                onDismiss = { campusViewModel.setShowFloorExplorerSheet(false) },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )

                            // Route Guidance Bottom Sheet
                            RouteNavigationSheet(
                                route = navState.activeRoute,
                                routeProgress = navState.dynamicProgress,
                                destinationName = navState.activeDestinationName.ifBlank { "Destination" },
                                targetBlockName = navState.navTargetBlockName,
                                targetFloor = navState.navTargetFloor,
                                targetRoomNo = navState.navTargetRoomNo,
                                onEndNavigation = {
                                    navigationViewModel.endNavigation()
                                },
                                onSwitchTo3D = {
                                    navigationViewModel.set3DMode(true)
                                },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )

                            // MapTiler Layer Selector Bottom Sheet
                            if (campusState.showLayersSheet) {
                                com.srmap.campusnavigator.presentation.layers.MapLayerBottomSheet(
                                    currentStyle = campusState.currentMapStyle,
                                    onSelectStyle = { style -> campusViewModel.setMapStyle(style) },
                                    onDismiss = { campusViewModel.setShowLayersSheet(false) }
                                )
                            }
                        }
                        }
                    }

                    CampusTab.SEARCH -> {
                        SearchScreen(
                            query = searchState.query,
                            onQueryChange = { searchViewModel.onQueryChange(it) },
                            selectedFilter = searchState.selectedFilter,
                            onFilterChange = { searchViewModel.onFilterChange(it) },
                            searchResults = searchState.searchResults,
                            recentSearches = searchState.recentSearches,
                            onClearRecentSearches = { searchViewModel.clearRecentSearches() },
                            onSelectResult = { res ->
                                searchViewModel.addRecentSearch(res.title)
                                when (res) {
                                    is UnifiedSearchResult.RoomResult -> {
                                        campusViewModel.selectRoom(res)
                                        focusTarget = res.point
                                    }
                                    is UnifiedSearchResult.FacultyResult -> {
                                        campusViewModel.selectRoom(
                                            UnifiedSearchResult.RoomResult(
                                                roomId = "${res.blockId}_${res.roomNo}",
                                                blockId = res.blockId,
                                                floor = res.floor,
                                                roomNo = res.roomNo,
                                                roomType = "Faculty Cabin",
                                                point = res.point,
                                                facultyName = res.facultyName,
                                                department = res.department
                                            )
                                        )
                                        focusTarget = res.point
                                    }
                                    is UnifiedSearchResult.EquipmentResult -> {
                                        campusViewModel.selectRoom(
                                            UnifiedSearchResult.RoomResult(
                                                roomId = "${res.blockId}_${res.roomNo}",
                                                blockId = res.blockId,
                                                floor = res.floor,
                                                roomNo = res.roomNo,
                                                roomType = "Research Lab",
                                                point = res.point,
                                                facultyName = res.labInCharge,
                                                equipmentList = res.equipmentName
                                            )
                                        )
                                        focusTarget = res.point
                                    }
                                    is UnifiedSearchResult.CampusBuildingResult -> {
                                        campusViewModel.selectBuilding(res.building)
                                        focusTarget = res.building.centroid
                                    }
                                    is UnifiedSearchResult.MapTilerPlaceResult -> {
                                        focusTarget = res.point
                                    }
                                }
                                currentTab = CampusTab.EXPLORE
                            },
                            onNavigateResult = { res ->
                                searchViewModel.addRecentSearch(res.title)
                                val pt = res.point
                                val start = navState.userLocation?.point ?: GeoPoint(16.4632311, 80.5064108)
                                val (bName, flr, rNo) = when (res) {
                                    is UnifiedSearchResult.RoomResult -> Triple("${res.blockId} Block", res.floor, res.roomNo)
                                    is UnifiedSearchResult.FacultyResult -> Triple("${res.blockId} Block", res.floor, res.roomNo)
                                    is UnifiedSearchResult.EquipmentResult -> Triple("${res.blockId} Block", res.floor, res.roomNo)
                                    is UnifiedSearchResult.CampusBuildingResult -> Triple(res.building.name, 1, null)
                                    else -> Triple(null, null, null)
                                }
                                navigationViewModel.startNavigation(
                                    startPoint = start,
                                    endPoint = pt,
                                    destinationName = res.title,
                                    blockName = bName,
                                    floor = flr,
                                    roomNo = rNo
                                )
                                currentTab = CampusTab.EXPLORE
                            }
                        )
                    }

                    CampusTab.SAVED -> {
                        SavedLocationsScreen(
                            favoriteBuildings = favoriteBuildings,
                            onRemoveFavorite = { bldg ->
                                campusViewModel.toggleFavorite(bldg)
                            },
                            onNavigate = { bldg ->
                                val start = navState.userLocation?.point ?: GeoPoint(16.4632311, 80.5064108)
                                navigationViewModel.startNavigation(
                                    startPoint = start,
                                    endPoint = bldg.centroid,
                                    destinationName = bldg.name,
                                    blockName = bldg.name,
                                    floor = 1,
                                    roomNo = null
                                )
                                currentTab = CampusTab.EXPLORE
                            }
                        )
                    }

                    CampusTab.PROFILE -> {
                        ProfileScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = onToggleDarkTheme
                        )
                    }
                }
            }
        }
    }
}
