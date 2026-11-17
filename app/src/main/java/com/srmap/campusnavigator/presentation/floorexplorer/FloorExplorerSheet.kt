package com.srmap.campusnavigator.presentation.floorexplorer

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
import androidx.compose.material.icons.filled.MeetingRoom
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
import com.srmap.campusnavigator.data.local.entities.RoomEntity
import com.srmap.campusnavigator.domain.models.CampusBuilding

@Composable
fun FloorExplorerSheet(
    visible: Boolean,
    buildings: List<CampusBuilding>,
    selectedBuilding: CampusBuilding?,
    rooms: List<RoomEntity>,
    onSelectBuilding: (CampusBuilding) -> Unit,
    onSelectRoom: (RoomEntity) -> Unit,
    onNavigateRoom: (RoomEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFloor by remember { mutableIntStateOf(1) }
    var selectedRoomTypeFilter by remember { mutableStateOf("All") }

    val currentBuilding = selectedBuilding ?: buildings.firstOrNull { it.name.contains("SR", ignoreCase = true) } ?: buildings.firstOrNull()

    val maxFloors = (currentBuilding?.levels?.toIntOrNull() ?: 3).coerceIn(1, 26)

    val floorRooms = rooms.filter { it.floor == selectedFloor }
    val filteredRooms = if (selectedRoomTypeFilter == "All") {
        floorRooms
    } else {
        floorRooms.filter { it.roomType.contains(selectedRoomTypeFilter, ignoreCase = true) }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 24.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Building & Floor Explorer",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Browse multi-level rooms, faculty suites & laboratories",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Building Selector Pills
                Text("Select Campus Block", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateGrey)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(buildings) { bldg ->
                        val isSelected = bldg.id == currentBuilding?.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MidnightNavy else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable {
                                onSelectBuilding(bldg)
                                selectedFloor = 1
                            }
                        ) {
                            Text(
                                text = bldg.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Floor Level Chips (1..maxFloors)
                Text("Select Floor Level", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateGrey)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(maxFloors) { idx ->
                        val flr = idx + 1
                        val isSelected = flr == selectedFloor
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) SrmCrimson else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { selectedFloor = flr }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "F$flr",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Room Type Filters: All, Classrooms, Cabins, Labs, Special
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf("All", "Classroom", "Cabin", "Lab", "Library", "Theatre", "Wellness", "Gym")) { filter ->
                        val isSel = filter == selectedRoomTypeFilter
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) DeepTeal else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.clickable { selectedRoomTypeFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Room List for Floor
                if (filteredRooms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No rooms matching '$selectedRoomTypeFilter' on Floor $selectedFloor.",
                            color = SlateGrey,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredRooms) { rm ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectRoom(rm) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (icon, tint) = when {
                                        rm.roomType.contains("Lab", ignoreCase = true) -> Pair(Icons.Default.Science, AccentLab)
                                        rm.roomType.contains("Cabin", ignoreCase = true) -> Pair(Icons.Default.Person, AccentFaculty)
                                        else -> Pair(Icons.Default.MeetingRoom, AccentAcademic)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(tint.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Room ${rm.roomNo}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = tint.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "Floor ${rm.floor}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = tint,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = rm.roomType,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (rm.capacity != null && rm.capacity > 0) {
                                            Text(
                                                text = "Capacity: ${rm.capacity} seats",
                                                fontSize = 12.sp,
                                                color = SlateGrey
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onNavigateRoom(rm) }) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsWalk,
                                            contentDescription = "Directions",
                                            tint = SrmCrimson
                                        )
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
