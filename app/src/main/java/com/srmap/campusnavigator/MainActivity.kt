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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.srmap.campusnavigator.core.designsystem.SrmCampusTheme
import com.srmap.campusnavigator.core.location.CampusLocationClient
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.data.geojson.GeoJsonAssetParser
import com.srmap.campusnavigator.data.local.entities.LocationEntity
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.CampusFeature
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import com.srmap.campusnavigator.domain.routing.RoutingEngine
import com.srmap.campusnavigator.presentation.bottomsheet.BuildingDetailsSheet
import com.srmap.campusnavigator.presentation.bottomsheet.RouteNavigationSheet
import com.srmap.campusnavigator.presentation.components.CampusBottomNavigationBar
import com.srmap.campusnavigator.presentation.components.FloatingCampusSearchBar
import com.srmap.campusnavigator.presentation.components.MapFloatingControls
import com.srmap.campusnavigator.presentation.directions.DirectionsScreen
import com.srmap.campusnavigator.presentation.explore.ExploreScreen
import com.srmap.campusnavigator.presentation.map.InteractiveCampusCanvasMap
import com.srmap.campusnavigator.presentation.navigation.CampusTab
import com.srmap.campusnavigator.presentation.profile.ProfileScreen
import com.srmap.campusnavigator.presentation.saved.SavedLocationsScreen
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
    val locationClient = remember { CampusLocationClient(context) }
    val geocodingClient = remember { com.srmap.campusnavigator.data.maptiler.geocoding.MapTilerGeocodingClient() }
    val gpsFilter = remember { com.srmap.campusnavigator.core.location.GpsFilterEngine() }

    var currentTab by remember { mutableStateOf(CampusTab.EXPLORE) }

    var buildings by remember { mutableStateOf<List<CampusBuilding>>(emptyList()) }
    var features by remember { mutableStateOf<List<CampusFeature>>(emptyList()) }
    var graph by remember { mutableStateOf<CampusGraph?>(null) }

    var mapMatcher by remember { mutableStateOf<com.srmap.campusnavigator.domain.routing.PedestrianMapMatcher?>(null) }
    var progressTracker by remember { mutableStateOf<com.srmap.campusnavigator.domain.routing.NavigationProgressTracker?>(null) }

    var selectedBuilding by remember { mutableStateOf<CampusBuilding?>(null) }
    var selectedRoom by remember { mutableStateOf<com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult?>(null) }
    var activeRoute by remember { mutableStateOf<PedestrianRoute?>(null) }
    var activeDestination by remember { mutableStateOf<GeoPoint?>(null) }
    var activeDestinationName by remember { mutableStateOf("") }
    var navTargetBlockName by remember { mutableStateOf<String?>(null) }
    var navTargetFloor by remember { mutableStateOf<Int?>(null) }
    var navTargetRoomNo by remember { mutableStateOf<String?>(null) }
    var dynamicProgress by remember { mutableStateOf<com.srmap.campusnavigator.domain.routing.DynamicRouteProgress?>(null) }

    var userLocation by remember { mutableStateOf<UserLocationState?>(null) }
    var focusTarget by remember { mutableStateOf<GeoPoint?>(null) }
    var mapZoomLevel by remember { mutableFloatStateOf(1.0f) }
    var currentMapStyle by remember { mutableStateOf(com.srmap.campusnavigator.data.maptiler.maps.MapTilerStyle.STREETS) }
    var showLayersSheet by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSearchFilter by remember { mutableStateOf(com.srmap.campusnavigator.presentation.components.SearchCategoryFilter.ALL) }
    var activeFloorFilter by remember { mutableStateOf<Int?>(null) }
    var roomsOnActiveFloor by remember { mutableStateOf<List<com.srmap.campusnavigator.data.local.entities.RoomEntity>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<com.srmap.campusnavigator.domain.models.UnifiedSearchResult>>(emptyList()) }
    var defaultRooms by remember { mutableStateOf<List<com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult>>(emptyList()) }
    var defaultFaculty by remember { mutableStateOf<List<com.srmap.campusnavigator.domain.models.UnifiedSearchResult.FacultyResult>>(emptyList()) }
    var defaultEquipment by remember { mutableStateOf<List<com.srmap.campusnavigator.domain.models.UnifiedSearchResult.EquipmentResult>>(emptyList()) }
    var buildingRooms by remember { mutableStateOf<List<com.srmap.campusnavigator.data.local.entities.RoomEntity>>(emptyList()) }
    var buildingFaculty by remember { mutableStateOf<List<com.srmap.campusnavigator.data.local.entities.FacultyEntity>>(emptyList()) }
    var buildingEquipment by remember { mutableStateOf<List<com.srmap.campusnavigator.data.local.entities.EquipmentEntity>>(emptyList()) }
    var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Helper to process location updates through GPS Filter + Map Matcher + Dynamic Route Progress
    fun onNewRawLocation(rawState: UserLocationState) {
        if (!locationClient.isLocationEnabled()) {
            userLocation = null
            return
        }
        val smoothed = gpsFilter.filterAndSmooth(rawState) ?: return

        // Snap to pedestrian graph if matcher initialized
        val snapped = mapMatcher?.matchLocation(smoothed.point)
        val finalPoint = snapped?.snappedPoint ?: smoothed.point

        userLocation = UserLocationState(
            point = finalPoint,
            accuracyMeters = smoothed.accuracyMeters,
            provider = smoothed.provider
        )

        // Update dynamic route progress if currently navigating
        if (activeRoute != null) {
            val prog = progressTracker?.updateProgress(finalPoint)
            if (prog != null) {
                dynamicProgress = prog
                if (prog.status == com.srmap.campusnavigator.domain.routing.NavigationStatus.ARRIVED) {
                    Toast.makeText(context, "You have reached $activeDestinationName!", Toast.LENGTH_LONG).show()
                }
            }
        }
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
            val bldgs = parser.loadBuildings()
            val feats = parser.loadAllCampusFeatures()
            val g = parser.buildRoutingGraph()

            withContext(Dispatchers.Main) {
                buildings = bldgs
                features = feats
                graph = g
                mapMatcher = com.srmap.campusnavigator.domain.routing.PedestrianMapMatcher(g)
                progressTracker = com.srmap.campusnavigator.domain.routing.NavigationProgressTracker(g)
            }

            val entities = bldgs.map {
                LocationEntity(
                    id = it.id,
                    name = it.name,
                    category = it.category.name,
                    latitude = it.centroid.latitude,
                    longitude = it.centroid.longitude,
                    levels = it.levels
                )
            }
            dao.insertLocations(entities)

            // Auto-ingest rooms, faculty, and equipment from enriched GeoJSON
            val blockEntities = listOf(
                com.srmap.campusnavigator.data.local.entities.BlockEntity("SR1", "SR1 Academic Block", "SR1", 3, "Academic & Faculty Cabins"),
                com.srmap.campusnavigator.data.local.entities.BlockEntity("V", "V-Block", "V", 3, "Research & Quantum Labs"),
                com.srmap.campusnavigator.data.local.entities.BlockEntity("X-LAB", "X-Lab Complex", "X-LAB", 3, "Engineering Labs & Auditorium"),
                com.srmap.campusnavigator.data.local.entities.BlockEntity("CV", "CV Raman Block", "CV", 10, "Academic Tower"),
                com.srmap.campusnavigator.data.local.entities.BlockEntity("HOMI", "Homi J Bhabha Block", "HOMI", 5, "Science & Research Block")
            )
            dao.insertBlocks(blockEntities)
            val enrichedData = parser.loadAllEnrichedData()
            dao.insertRooms(enrichedData.rooms)
            dao.insertFaculty(enrichedData.faculty)
            dao.insertEquipment(enrichedData.equipment)

            // Pre-populate default directory lists for categorized search view
            val defRooms = dao.searchCampusRoomsUnified("").mapNotNull { r ->
                if (r.latitude != null && r.longitude != null) {
                    com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult(
                        roomId = r.roomId,
                        blockId = r.blockId,
                        floor = r.floor,
                        roomNo = r.roomNo,
                        roomType = r.roomType,
                        point = GeoPoint(r.latitude, r.longitude),
                        facultyName = r.facultyName,
                        department = r.facultyDepartment,
                        equipmentList = r.equipmentList,
                        locationStatus = r.locationStatus
                    )
                } else null
            }
            val defFaculty = dao.getAllFaculty().mapNotNull { f ->
                val associatedRoom = enrichedData.rooms.firstOrNull { it.roomId == f.roomId }
                if (associatedRoom?.latitude != null && associatedRoom.longitude != null) {
                    com.srmap.campusnavigator.domain.models.UnifiedSearchResult.FacultyResult(
                        facultyName = f.name,
                        designation = f.designation ?: "Faculty",
                        department = f.department ?: "Academic",
                        blockId = associatedRoom.blockId,
                        floor = associatedRoom.floor,
                        roomNo = associatedRoom.roomNo,
                        point = GeoPoint(associatedRoom.latitude, associatedRoom.longitude)
                    )
                } else null
            }
            val defEquipment = dao.getAllEquipment().mapNotNull { eq ->
                val associatedRoom = enrichedData.rooms.firstOrNull { it.roomId == eq.roomId }
                if (associatedRoom?.latitude != null && associatedRoom.longitude != null) {
                    com.srmap.campusnavigator.domain.models.UnifiedSearchResult.EquipmentResult(
                        equipmentName = eq.name,
                        blockId = associatedRoom.blockId,
                        floor = associatedRoom.floor,
                        roomNo = associatedRoom.roomNo,
                        labInCharge = associatedRoom.roomType,
                        point = GeoPoint(associatedRoom.latitude, associatedRoom.longitude)
                    )
                } else null
            }

            withContext(Dispatchers.Main) {
                defaultRooms = defRooms
                defaultFaculty = defFaculty
                defaultEquipment = defEquipment
            }
        }
    }

    // Update building rooms, faculty, and equipment when selectedBuilding changes
    LaunchedEffect(selectedBuilding) {
        val bldg = selectedBuilding
        if (bldg != null) {
            val canonicalBlockId = when {
                bldg.name.contains("SR", ignoreCase = true) -> "SR1"
                bldg.name.contains("CV", ignoreCase = true) || bldg.name.contains("Raman", ignoreCase = true) -> "CV"
                bldg.name.contains("V Block", ignoreCase = true) || bldg.name.equals("V", ignoreCase = true) -> "V"
                bldg.name.contains("X Lab", ignoreCase = true) || bldg.name.contains("X-Lab", ignoreCase = true) -> "X-LAB"
                bldg.name.contains("Homi", ignoreCase = true) -> "HOMI"
                else -> bldg.name
            }
            withContext(Dispatchers.IO) {
                val rms = dao.getRoomsForBlock(canonicalBlockId)
                val facs = dao.getFacultyForBlock(canonicalBlockId)
                val eqs = dao.getEquipmentForBlock(canonicalBlockId)
                withContext(Dispatchers.Main) {
                    buildingRooms = rms
                    buildingFaculty = facs
                    buildingEquipment = eqs
                }
            }
        } else {
            buildingRooms = emptyList()
            buildingFaculty = emptyList()
            buildingEquipment = emptyList()
        }
    }
    LaunchedEffect(activeFloorFilter) {
        val flr = activeFloorFilter
        if (flr != null) {
            withContext(Dispatchers.IO) {
                val rms = dao.searchCampusRoomsUnified("").filter { it.floor == flr }.map { r ->
                    com.srmap.campusnavigator.data.local.entities.RoomEntity(
                        roomId = r.roomId,
                        blockId = r.blockId,
                        roomNo = r.roomNo,
                        normalizedRoomNo = r.normalizedRoomNo,
                        floor = r.floor,
                        roomType = r.roomType,
                        latitude = r.latitude,
                        longitude = r.longitude,
                        locationStatus = r.locationStatus,
                        sourceDataset = null,
                        description = r.description,
                        capacity = r.capacity
                    )
                }
                withContext(Dispatchers.Main) {
                    roomsOnActiveFloor = rms
                }
            }
        } else {
            roomsOnActiveFloor = emptyList()
        }
    }
    var recentSearchesList by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load recent searches from DB
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val recents = dao.getRecentSearches()
            withContext(Dispatchers.Main) {
                recentSearchesList = recents
            }
        }
    }

    LaunchedEffect(Unit) {
        dao.getFavoriteLocations().collectLatest { favList ->
            favoriteIds = favList.map { it.id }.toSet()
        }
    }

    // Unified Search: Combines local campus buildings with debounced MapTiler Geocoding
    LaunchedEffect(searchQuery, buildings) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
        } else {
            val q = searchQuery.trim()
            val campusMatches = buildings.filter {
                it.name.contains(q, ignoreCase = true) ||
                        it.category.name.contains(q, ignoreCase = true)
            }.map { com.srmap.campusnavigator.domain.models.UnifiedSearchResult.CampusBuildingResult(it) }

            // Query Offline Room Database for Room / Faculty / Equipment matches
            val roomMatches = withContext(Dispatchers.IO) {
                dao.searchCampusRoomsUnified(q).mapNotNull { r ->
                    if (r.latitude != null && r.longitude != null) {
                        com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult(
                            roomId = r.roomId,
                            blockId = r.blockId,
                            floor = r.floor,
                            roomNo = r.roomNo,
                            roomType = r.roomType,
                            point = GeoPoint(r.latitude, r.longitude),
                            facultyName = r.facultyName,
                            department = r.facultyDepartment,
                            equipmentList = r.equipmentList,
                            locationStatus = r.locationStatus
                        )
                    } else null
                }
            }

            searchResults = roomMatches + campusMatches

            // Debounced external Geocoding for regional locations (Amaravati, Guntur, Vijayawada)
            searchJob?.cancel()
            searchJob = scope.launch {
                kotlinx.coroutines.delay(350L)
                val maptilerMatches = geocodingClient.forwardGeocode(q).map { geo ->
                    com.srmap.campusnavigator.domain.models.UnifiedSearchResult.MapTilerPlaceResult(
                        id = geo.id,
                        title = geo.text,
                        subtitle = geo.placeName,
                        point = geo.point
                    )
                }
                if (q == searchQuery.trim()) {
                    // Query Offline Room Database for Room / Faculty / Equipment matches
            val roomMatches = withContext(Dispatchers.IO) {
                dao.searchCampusRoomsUnified(q).mapNotNull { r ->
                    if (r.latitude != null && r.longitude != null) {
                        com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult(
                            roomId = r.roomId,
                            blockId = r.blockId,
                            floor = r.floor,
                            roomNo = r.roomNo,
                            roomType = r.roomType,
                            point = GeoPoint(r.latitude, r.longitude),
                            facultyName = r.facultyName,
                            department = r.facultyDepartment,
                            equipmentList = r.equipmentList,
                            locationStatus = r.locationStatus
                        )
                    } else null
                }
            }

            searchResults = roomMatches + campusMatches + maptilerMatches
                }
            }
        }
    }

    val isFavorite = selectedBuilding?.id in favoriteIds
    val favoriteBuildings = buildings.filter { it.id in favoriteIds }

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
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Interactive Canvas Map
                            InteractiveCampusCanvasMap(
                                buildings = buildings,
                                features = features,
                                selectedBuilding = selectedBuilding,
                                route = activeRoute,
                                routeProgress = dynamicProgress,
                                userLocation = userLocation,
                                mapStyle = currentMapStyle,
                                zoomLevel = mapZoomLevel,
                                focusTarget = focusTarget,
                                highlightedRoom = selectedRoom,
                                activeFloor = activeFloorFilter,
                                roomsOnFloor = roomsOnActiveFloor,
                                onBuildingSelected = { bldg ->
                                    selectedBuilding = bldg
                                    selectedRoom = null
                                    focusTarget = bldg.centroid
                                    activeRoute = null
                                    dynamicProgress = null
                                }
                            )

                            // Floating Search Bar with Autocomplete & Category Filter Chips
                            FloatingCampusSearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                selectedFilter = selectedSearchFilter,
                                onFilterChange = { selectedSearchFilter = it },
                                searchResults = searchResults,
                                onSelectResult = { res ->
                                    when (res) {
                                        is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult -> {
                                            selectedRoom = res
                                            selectedBuilding = null
                                            focusTarget = res.point
                                            activeRoute = null
                                            dynamicProgress = null
                                        }
                                        is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.FacultyResult -> {
                                            selectedRoom = com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult(
                                                roomId = "${res.blockId}_${res.roomNo}",
                                                blockId = res.blockId,
                                                floor = res.floor,
                                                roomNo = res.roomNo,
                                                roomType = "Faculty Cabin",
                                                point = res.point,
                                                facultyName = res.facultyName,
                                                department = res.department
                                            )
                                            selectedBuilding = null
                                            focusTarget = res.point
                                            activeRoute = null
                                            dynamicProgress = null
                                        }
                                        is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.EquipmentResult -> {
                                            selectedRoom = com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult(
                                                roomId = "${res.blockId}_${res.roomNo}",
                                                blockId = res.blockId,
                                                floor = res.floor,
                                                roomNo = res.roomNo,
                                                roomType = "Research Lab",
                                                point = res.point,
                                                facultyName = res.labInCharge,
                                                equipmentList = res.equipmentName
                                            )
                                            selectedBuilding = null
                                            focusTarget = res.point
                                            activeRoute = null
                                            dynamicProgress = null
                                        }
                                        is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.CampusBuildingResult -> {
                                            selectedBuilding = res.building
                                            selectedRoom = null
                                            focusTarget = res.building.centroid
                                            activeRoute = null
                                            dynamicProgress = null
                                        }
                                        is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.MapTilerPlaceResult -> {
                                            focusTarget = res.point
                                            selectedRoom = null
                                            selectedBuilding = null
                                            activeRoute = null
                                            dynamicProgress = null
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
                                    if (userLocation != null) {
                                        focusTarget = userLocation!!.point
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
                                    showLayersSheet = true
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
                            selectedRoom?.let { room ->
                                com.srmap.campusnavigator.presentation.bottomsheet.RoomDetailsSheet(
                                    room = room,
                                    onClose = { selectedRoom = null },
                                    onNavigate = {
                                        val start = userLocation?.point ?: GeoPoint(16.4632311, 80.5064108)
                                        graph?.let { g ->
                                            val r = RoutingEngine.findAStarPath(
                                                graph = g,
                                                startPoint = start,
                                                endPoint = room.point,
                                                targetBlockName = "${room.blockId} Block",
                                                targetFloor = room.floor,
                                                targetRoomNo = room.roomNo
                                            )
                                            if (r != null) {
                                                activeRoute = r
                                                activeDestination = room.point
                                                activeDestinationName = "${room.blockId} - Room ${room.roomNo}"
                                                navTargetBlockName = "${room.blockId} Block"
                                                navTargetFloor = room.floor
                                                navTargetRoomNo = room.roomNo
                                                progressTracker?.startNavigation(r, room.point)
                                                dynamicProgress = progressTracker?.updateProgress(start)
                                            }
                                        }
                                        selectedRoom = null
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }

                            // Building Details Bottom Sheet
                            BuildingDetailsSheet(
                                building = selectedBuilding,
                                isFavorite = isFavorite,
                                roomsInBlock = buildingRooms,
                                facultyInBlock = buildingFaculty,
                                equipmentInBlock = buildingEquipment,
                                onNavigateClick = { destination ->
                                    val start = userLocation?.point ?: GeoPoint(16.4632311, 80.5064108)
                                    val currentGraph = graph
                                    if (currentGraph != null) {
                                        val r = RoutingEngine.findAStarPath(
                                            graph = currentGraph,
                                            startPoint = start,
                                            endPoint = destination.centroid,
                                            targetBlockName = destination.name,
                                            targetFloor = 1
                                        )
                                        if (r != null) {
                                            activeRoute = r
                                            activeDestination = destination.centroid
                                            activeDestinationName = destination.name
                                            navTargetBlockName = destination.name
                                            navTargetFloor = 1
                                            navTargetRoomNo = null
                                            progressTracker?.startNavigation(r, destination.centroid)
                                            dynamicProgress = progressTracker?.updateProgress(start)
                                        }
                                    }
                                    selectedBuilding = null
                                },
                                onSelectRoom = { rm ->
                                    if (rm.latitude != null && rm.longitude != null) {
                                        selectedRoom = com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult(
                                            roomId = rm.roomId,
                                            blockId = rm.blockId,
                                            floor = rm.floor,
                                            roomNo = rm.roomNo,
                                            roomType = rm.roomType,
                                            point = GeoPoint(rm.latitude, rm.longitude),
                                            locationStatus = rm.locationStatus
                                        )
                                        selectedBuilding = null
                                        focusTarget = GeoPoint(rm.latitude, rm.longitude)
                                    }
                                },
                                onToggleFavorite = { bldg ->
                                    scope.launch(Dispatchers.IO) {
                                        val newFav = !isFavorite
                                        dao.updateFavorite(bldg.id, newFav)
                                    }
                                },
                                onDismiss = { selectedBuilding = null },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )

                            // Route Guidance Bottom Sheet
                            RouteNavigationSheet(
                                route = activeRoute,
                                routeProgress = dynamicProgress,
                                destinationName = activeDestinationName.ifBlank { "Destination" },
                                targetBlockName = navTargetBlockName,
                                targetFloor = navTargetFloor,
                                targetRoomNo = navTargetRoomNo,
                                onEndNavigation = {
                                    activeRoute = null
                                    activeDestination = null
                                    activeDestinationName = ""
                                    navTargetBlockName = null
                                    navTargetFloor = null
                                    navTargetRoomNo = null
                                    dynamicProgress = null
                                    progressTracker?.stopNavigation()
                                },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )

                            // MapTiler Layer Selector Bottom Sheet
                            if (showLayersSheet) {
                                com.srmap.campusnavigator.presentation.layers.MapLayerBottomSheet(
                                    currentStyle = currentMapStyle,
                                    onSelectStyle = { style -> currentMapStyle = style },
                                    onDismiss = { showLayersSheet = false }
                                )
                            }
                        }
                    }

                    CampusTab.SEARCH -> {
                        com.srmap.campusnavigator.presentation.search.SearchScreen(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            selectedFilter = selectedSearchFilter,
                            onFilterChange = { selectedSearchFilter = it },
                            searchResults = searchResults,
                            allRooms = defaultRooms,
                            allFaculty = defaultFaculty,
                            allEquipment = defaultEquipment,
                            recentSearches = recentSearchesList,
                            onClearRecentSearches = {
                                scope.launch(Dispatchers.IO) {
                                    dao.clearRecentSearches()
                                    recentSearchesList = emptyList()
                                }
                            },
                            onSelectResult = { res ->
                                when (res) {
                                    is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult -> {
                                        selectedRoom = res
                                        selectedBuilding = null
                                        focusTarget = res.point
                                    }
                                    is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.FacultyResult -> {
                                        selectedRoom = com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult(
                                            roomId = "${res.blockId}_${res.roomNo}",
                                            blockId = res.blockId,
                                            floor = res.floor,
                                            roomNo = res.roomNo,
                                            roomType = "Faculty Cabin",
                                            point = res.point,
                                            facultyName = res.facultyName,
                                            department = res.department
                                        )
                                        selectedBuilding = null
                                        focusTarget = res.point
                                    }
                                    is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.EquipmentResult -> {
                                        selectedRoom = com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult(
                                            roomId = "${res.blockId}_${res.roomNo}",
                                            blockId = res.blockId,
                                            floor = res.floor,
                                            roomNo = res.roomNo,
                                            roomType = "Research Lab",
                                            point = res.point,
                                            facultyName = res.labInCharge,
                                            equipmentList = res.equipmentName
                                        )
                                        selectedBuilding = null
                                        focusTarget = res.point
                                    }
                                    is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.CampusBuildingResult -> {
                                        selectedBuilding = res.building
                                        selectedRoom = null
                                        focusTarget = res.building.centroid
                                    }
                                    is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.MapTilerPlaceResult -> {
                                        focusTarget = res.point
                                        selectedRoom = null
                                        selectedBuilding = null
                                    }
                                }
                                currentTab = CampusTab.EXPLORE
                            },
                            onNavigateResult = { res ->
                                val pt = res.point
                                val start = userLocation?.point ?: GeoPoint(16.4632311, 80.5064108)
                                val (bName, flr, rNo) = when (res) {
                                    is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult -> Triple("${res.blockId} Block", res.floor, res.roomNo)
                                    is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.FacultyResult -> Triple("${res.blockId} Block", res.floor, res.roomNo)
                                    is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.EquipmentResult -> Triple("${res.blockId} Block", res.floor, res.roomNo)
                                    is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.CampusBuildingResult -> Triple(res.building.name, 1, null)
                                    else -> Triple(null, null, null)
                                }
                                graph?.let { g ->
                                    val r = RoutingEngine.findAStarPath(
                                        graph = g,
                                        startPoint = start,
                                        endPoint = pt,
                                        targetBlockName = bName,
                                        targetFloor = flr,
                                        targetRoomNo = rNo
                                    )
                                    if (r != null) {
                                        activeRoute = r
                                        activeDestination = pt
                                        activeDestinationName = res.title
                                        navTargetBlockName = bName
                                        navTargetFloor = flr
                                        navTargetRoomNo = rNo
                                        progressTracker?.startNavigation(r, pt)
                                        dynamicProgress = progressTracker?.updateProgress(start)
                                    }
                                }
                                currentTab = CampusTab.EXPLORE
                            }
                        )
                    }

                    CampusTab.SAVED -> {
                        SavedLocationsScreen(
                            favoriteBuildings = favoriteBuildings,
                            onRemoveFavorite = { bldg ->
                                scope.launch(Dispatchers.IO) {
                                    dao.updateFavorite(bldg.id, false)
                                }
                            },
                            onNavigate = { bldg ->
                                val start = userLocation?.point ?: GeoPoint(16.4632311, 80.5064108)
                                graph?.let { g ->
                                    val r = RoutingEngine.findAStarPath(g, start, bldg.centroid)
                                    activeRoute = r
                                    activeDestination = bldg.centroid
                                    activeDestinationName = bldg.name
                                    if (r != null) {
                                        progressTracker?.startNavigation(r, bldg.centroid)
                                        dynamicProgress = progressTracker?.updateProgress(start)
                                    }
                                    currentTab = CampusTab.EXPLORE
                                }
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
