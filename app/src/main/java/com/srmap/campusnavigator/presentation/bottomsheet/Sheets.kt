package com.srmap.campusnavigator.presentation.bottomsheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srmap.campusnavigator.core.designsystem.*
import com.srmap.campusnavigator.data.local.entities.EquipmentEntity
import com.srmap.campusnavigator.data.local.entities.FacultyEntity
import com.srmap.campusnavigator.data.local.entities.RoomEntity
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.LocationCategory
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import com.srmap.campusnavigator.domain.routing.DynamicRouteProgress
import com.srmap.campusnavigator.domain.routing.NavigationStatus
import com.srmap.campusnavigator.presentation.components.CampusPrimaryButton
import com.srmap.campusnavigator.presentation.components.CampusSecondaryButton

enum class BlockDetailTab {
    OVERVIEW, ROOMS, FACULTY, LABS
}

@Composable
fun BuildingDetailsSheet(
    building: CampusBuilding?,
    isFavorite: Boolean,
    roomsInBlock: List<RoomEntity> = emptyList(),
    facultyInBlock: List<FacultyEntity> = emptyList(),
    equipmentInBlock: List<EquipmentEntity> = emptyList(),
    onNavigateClick: (CampusBuilding) -> Unit,
    onOpenFloorExplorer: (() -> Unit)? = null,
    onSelectRoom: ((RoomEntity) -> Unit)? = null,
    onToggleFavorite: (CampusBuilding) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(BlockDetailTab.OVERVIEW) }

    AnimatedVisibility(
        visible = building != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (building != null) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                shadowElevation = 24.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.70f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Drag Handle Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(44.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Title Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = building.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(getCategoryColor(building.category))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = building.category.name,
                                    fontSize = 13.sp,
                                    color = SlateGrey
                                )
                                building.levels?.let {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("•  $it Floors", fontSize = 13.sp, color = SlateGrey)
                                }
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row: Walk to Entrance + Floor Explorer + Favorite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CampusPrimaryButton(
                            text = "Walk to Entrance",
                            icon = Icons.Default.Navigation,
                            onClick = { onNavigateClick(building) },
                            modifier = Modifier.weight(1.3f)
                        )

                        if (onOpenFloorExplorer != null && building.category == LocationCategory.ACADEMIC) {
                            CampusSecondaryButton(
                                text = "Floor Map",
                                icon = Icons.Default.Layers,
                                onClick = onOpenFloorExplorer,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedButton(
                            onClick = { onToggleFavorite(building) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) SrmCrimson else SlateGrey
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Navigation
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        contentColor = SrmCrimson,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == BlockDetailTab.OVERVIEW,
                            onClick = { selectedTab = BlockDetailTab.OVERVIEW },
                            text = { Text("Overview (${roomsInBlock.size + facultyInBlock.size + equipmentInBlock.size})", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == BlockDetailTab.ROOMS,
                            onClick = { selectedTab = BlockDetailTab.ROOMS },
                            text = { Text("Rooms (${roomsInBlock.size})", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == BlockDetailTab.FACULTY,
                            onClick = { selectedTab = BlockDetailTab.FACULTY },
                            text = { Text("Faculty (${facultyInBlock.size})", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == BlockDetailTab.LABS,
                            onClick = { selectedTab = BlockDetailTab.LABS },
                            text = { Text("Labs (${equipmentInBlock.size})", fontSize = 13.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tab Content List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (selectedTab) {
                            BlockDetailTab.OVERVIEW -> {
                                if (facultyInBlock.isNotEmpty()) {
                                    item {
                                        Text(
                                            "👨‍🏫 Faculty Members",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AccentFaculty,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(facultyInBlock.take(3)) { fac ->
                                        FacultyCard(fac)
                                    }
                                }

                                if (equipmentInBlock.isNotEmpty()) {
                                    item {
                                        Text(
                                            "🔬 Research Labs & Equipment",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AccentLab,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(equipmentInBlock.take(3)) { eq ->
                                        EquipmentCard(eq)
                                    }
                                }

                                if (roomsInBlock.isNotEmpty()) {
                                    item {
                                        Text(
                                            "🎓 Classrooms & Facilities",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AccentAcademic,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(roomsInBlock.take(4)) { room ->
                                        RoomCard(room, onSelectRoom)
                                    }
                                }
                            }
                            BlockDetailTab.ROOMS -> {
                                items(roomsInBlock) { room ->
                                    RoomCard(room, onSelectRoom)
                                }
                            }
                            BlockDetailTab.FACULTY -> {
                                items(facultyInBlock) { fac ->
                                    FacultyCard(fac)
                                }
                            }
                            BlockDetailTab.LABS -> {
                                items(equipmentInBlock) { eq ->
                                    EquipmentCard(eq)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacultyCard(fac: FacultyEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentFaculty.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = AccentFaculty, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fac.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("${fac.designation ?: "Faculty"} • ${fac.department ?: ""}", fontSize = 13.sp, color = SlateGrey)
                if (!fac.email.isNullOrBlank()) {
                    Text(fac.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun EquipmentCard(eq: EquipmentEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentLab.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Science, contentDescription = null, tint = AccentLab, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(eq.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(eq.category ?: "Equipment", fontSize = 13.sp, color = SlateGrey)
                if (!eq.specifications.isNullOrBlank()) {
                    Text(eq.specifications, fontSize = 12.sp, color = AccentLab)
                }
            }
        }
    }
}

@Composable
private fun RoomCard(room: RoomEntity, onSelectRoom: ((RoomEntity) -> Unit)?) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectRoom?.invoke(room) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentAcademic.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = AccentAcademic, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Room ${room.roomNo}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentAcademic.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Floor ${room.floor}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentAcademic,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(room.roomType, fontSize = 13.sp, color = SlateGrey)
                if (!room.description.isNullOrBlank()) {
                    Text(room.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (room.latitude != null && room.longitude != null) {
                IconButton(onClick = { onSelectRoom?.invoke(room) }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Locate", tint = SrmCrimson)
                }
            }
        }
    }
}

@Composable
fun RouteNavigationSheet(
    route: PedestrianRoute?,
    routeProgress: DynamicRouteProgress? = null,
    destinationName: String,
    targetBlockName: String? = null,
    targetFloor: Int? = null,
    targetRoomNo: String? = null,
    onEndNavigation: () -> Unit,
    onSwitchTo3D: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = route != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (route != null) {
            val remainingMeters = routeProgress?.remainingDistanceMeters ?: route.totalDistanceMeters
            val remainingSec = routeProgress?.estimatedRemainingSeconds ?: route.estimatedTimeSeconds
            val minutes = (remainingSec / 60).coerceAtLeast(1)
            val progressFrac = routeProgress?.progressFraction ?: 0.0f
            val status = routeProgress?.status ?: NavigationStatus.NAVIGATING
            val instruction = routeProgress?.nextInstruction ?: route.steps.firstOrNull()?.instruction ?: "Proceed along route"

            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                shadowElevation = 24.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(44.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (status == NavigationStatus.ARRIVED) "Arrived at $destinationName" else "Navigating to $destinationName",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (targetFloor != null || !targetBlockName.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    if (!targetBlockName.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = targetBlockName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    if (targetFloor != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AccentAcademic.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Floor $targetFloor",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentAcademic,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (status == NavigationStatus.ARRIVED) "You have reached your destination" else "%.0f m remaining  •  approx. %d min walk".format(remainingMeters, minutes),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (status == NavigationStatus.ARRIVED) AccentSports else SrmCrimson
                            )
                        }

                        IconButton(onClick = onEndNavigation) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Navigation", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progressFrac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentAcademic,
                        trackColor = BorderGrey
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Instruction Step Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = SrmCrimson,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = instruction,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (targetFloor != null && targetFloor > 1 && instruction.contains("arrive", ignoreCase = true)) {
                                    Text(
                                        text = "Proceed inside to Floor $targetFloor for ${targetRoomNo ?: destinationName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AccentAcademic,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onSwitchTo3D != null) {
                            CampusSecondaryButton(
                                text = "3D View",
                                icon = Icons.Default.Navigation,
                                onClick = onSwitchTo3D,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        CampusPrimaryButton(
                            text = if (status == NavigationStatus.ARRIVED) "Done" else "End Navigation",
                            onClick = onEndNavigation,
                            containerColor = MidnightNavy,
                            modifier = Modifier.weight(if (onSwitchTo3D != null) 1.4f else 1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoomDetailsSheet(
    room: com.srmap.campusnavigator.domain.models.UnifiedSearchResult.RoomResult,
    onClose: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${room.blockId} Block • Room ${room.roomNo}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentAcademic.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Floor ${room.floor}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAcademic,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = room.roomType,
                                fontSize = 14.sp,
                                color = SlateGrey
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (!room.facultyName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Faculty In-Charge / Cabin:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateGrey
                    )
                    Text(
                        text = "${room.facultyName} (${room.department ?: "Faculty"})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!room.equipmentList.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Equipment & Facilities Available:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateGrey
                    )
                    Text(
                        text = room.equipmentList,
                        fontSize = 14.sp,
                        color = AccentLab
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                CampusPrimaryButton(
                    text = "Directions to ${room.blockId} (Floor ${room.floor})",
                    icon = Icons.Default.DirectionsWalk,
                    onClick = onNavigate,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun getCategoryColor(category: LocationCategory): Color {
    return when (category) {
        LocationCategory.ACADEMIC -> AccentAcademic
        LocationCategory.HOSTEL -> AccentHostel
        LocationCategory.DINING -> AccentDining
        LocationCategory.SPORTS -> AccentSports
        else -> SrmCrimson
    }
}
