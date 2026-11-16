package com.srmap.campusnavigator.presentation.bottomsheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.srmap.campusnavigator.core.designsystem.SrmCrimson
import com.srmap.campusnavigator.data.local.entities.EquipmentEntity
import com.srmap.campusnavigator.data.local.entities.FacultyEntity
import com.srmap.campusnavigator.data.local.entities.RoomEntity
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import com.srmap.campusnavigator.domain.routing.DynamicRouteProgress
import com.srmap.campusnavigator.domain.routing.NavigationStatus
import com.srmap.campusnavigator.presentation.components.getCategoryColor

enum class BlockDetailTab {
    ALL, CLASSROOMS, FACULTY, LABS_EQUIPMENT
}

@Composable
fun BuildingDetailsSheet(
    building: CampusBuilding?,
    isFavorite: Boolean,
    roomsInBlock: List<RoomEntity> = emptyList(),
    facultyInBlock: List<FacultyEntity> = emptyList(),
    equipmentInBlock: List<EquipmentEntity> = emptyList(),
    onNavigateClick: (CampusBuilding) -> Unit,
    onSelectRoom: ((RoomEntity) -> Unit)? = null,
    onToggleFavorite: (CampusBuilding) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(BlockDetailTab.ALL) }

    AnimatedVisibility(
        visible = building != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (building != null) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Drag Handle Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = building.name,
                                fontSize = 20.sp,
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
                                    color = Color.Gray
                                )
                                building.levels?.let {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("•  $it Floors", fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigateClick(building) },
                            colors = ButtonDefaults.buttonColors(containerColor = SrmCrimson),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Walk to Entrance")
                        }

                        OutlinedButton(
                            onClick = { onToggleFavorite(building) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        contentColor = SrmCrimson,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == BlockDetailTab.ALL,
                            onClick = { selectedTab = BlockDetailTab.ALL },
                            text = { Text("Overview (${roomsInBlock.size + facultyInBlock.size + equipmentInBlock.size})", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == BlockDetailTab.CLASSROOMS,
                            onClick = { selectedTab = BlockDetailTab.CLASSROOMS },
                            text = { Text("Classrooms (${roomsInBlock.size})", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == BlockDetailTab.FACULTY,
                            onClick = { selectedTab = BlockDetailTab.FACULTY },
                            text = { Text("Faculty (${facultyInBlock.size})", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == BlockDetailTab.LABS_EQUIPMENT,
                            onClick = { selectedTab = BlockDetailTab.LABS_EQUIPMENT },
                            text = { Text("Labs & Equip (${equipmentInBlock.size})", fontSize = 13.sp) }
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
                            BlockDetailTab.ALL -> {
                                if (facultyInBlock.isNotEmpty()) {
                                    item {
                                        Text(
                                            "👨‍🏫 Faculty Members",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(facultyInBlock) { fac ->
                                        FacultyBlockCard(fac)
                                    }
                                }

                                if (equipmentInBlock.isNotEmpty()) {
                                    item {
                                        Text(
                                            "🔬 Research Labs & Equipment",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(equipmentInBlock) { eq ->
                                        EquipmentBlockCard(eq)
                                    }
                                }

                                if (roomsInBlock.isNotEmpty()) {
                                    item {
                                        Text(
                                            "🎓 Classrooms & Facilities",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0284C7),
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(roomsInBlock) { room ->
                                        RoomBlockCard(room, onSelectRoom)
                                    }
                                }

                                if (facultyInBlock.isEmpty() && equipmentInBlock.isEmpty() && roomsInBlock.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No room metadata available for this block.", color = Color.Gray)
                                        }
                                    }
                                }
                            }
                            BlockDetailTab.CLASSROOMS -> {
                                if (roomsInBlock.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No classrooms registered for this block.", color = Color.Gray)
                                        }
                                    }
                                } else {
                                    items(roomsInBlock) { room ->
                                        RoomBlockCard(room, onSelectRoom)
                                    }
                                }
                            }
                            BlockDetailTab.FACULTY -> {
                                if (facultyInBlock.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No faculty cabins registered in this block.", color = Color.Gray)
                                        }
                                    }
                                } else {
                                    items(facultyInBlock) { fac ->
                                        FacultyBlockCard(fac)
                                    }
                                }
                            }
                            BlockDetailTab.LABS_EQUIPMENT -> {
                                if (equipmentInBlock.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No specialized equipment or labs registered in this block.", color = Color.Gray)
                                        }
                                    }
                                } else {
                                    items(equipmentInBlock) { eq ->
                                        EquipmentBlockCard(eq)
                                    }
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
private fun FacultyBlockCard(fac: FacultyEntity) {
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
                    .background(Color(0xFFD97706).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fac.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("${fac.designation ?: "Faculty"} • ${fac.department ?: ""}", fontSize = 13.sp, color = Color.Gray)
                if (!fac.email.isNullOrBlank()) {
                    Text(fac.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun EquipmentBlockCard(eq: EquipmentEntity) {
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
                    .background(Color(0xFF047857).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(eq.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(eq.category ?: "Equipment", fontSize = 13.sp, color = Color.Gray)
                if (!eq.specifications.isNullOrBlank()) {
                    Text(eq.specifications, fontSize = 12.sp, color = Color(0xFF047857))
                }
            }
        }
    }
}

@Composable
private fun RoomBlockCard(room: RoomEntity, onSelectRoom: ((RoomEntity) -> Unit)?) {
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
                    .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Room ${room.roomNo}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Floor ${room.floor}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(room.roomType, fontSize = 13.sp, color = Color.Gray)
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
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 20.dp,
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
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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
                                            color = Color(0xFF0284C7).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Floor $targetFloor",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7),
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
                                color = if (status == NavigationStatus.ARRIVED) Color(0xFF10B981) else SrmCrimson
                            )
                        }

                        IconButton(onClick = onEndNavigation) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Navigation")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progressFrac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF0284C7),
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Instruction Step Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
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
                                    color = Color(0xFF0F172A)
                                )
                                if (targetFloor != null && targetFloor > 1 && instruction.contains("arrive", ignoreCase = true)) {
                                    Text(
                                        text = "Proceed inside to Floor $targetFloor for ${targetRoomNo ?: destinationName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onEndNavigation,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (status == NavigationStatus.ARRIVED) "Done" else "End Navigation")
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
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                                color = Color(0xFF0284C7).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Floor ${room.floor}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = room.roomType,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (!room.facultyName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Faculty In-Charge / Cabin:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
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
                        color = Color.Gray
                    )
                    Text(
                        text = room.equipmentList,
                        fontSize = 14.sp,
                        color = Color(0xFF047857)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = SrmCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Directions to ${room.blockId} (Floor ${room.floor})")
                }
            }
        }
    }
}
