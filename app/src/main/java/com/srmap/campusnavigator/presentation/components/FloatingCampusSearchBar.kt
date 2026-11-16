package com.srmap.campusnavigator.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srmap.campusnavigator.core.designsystem.SrmCrimson
import com.srmap.campusnavigator.domain.models.LocationCategory
import com.srmap.campusnavigator.domain.models.UnifiedSearchResult

@Composable
fun FloatingCampusSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: SearchCategoryFilter,
    onFilterChange: (SearchCategoryFilter) -> Unit,
    searchResults: List<UnifiedSearchResult>,
    onSelectResult: (UnifiedSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            text = "Search rooms, faculty, labs, equipment...",
                            fontSize = 14.sp
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SrmCrimson
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                // Category Filter Chips Row
                SearchCategoryChipsRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterChange,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        // Filtered Results Dropdown
        val filteredResults = searchResults.filter { res ->
            when (selectedFilter) {
                SearchCategoryFilter.ALL -> true
                SearchCategoryFilter.BUILDINGS -> res is UnifiedSearchResult.CampusBuildingResult
                SearchCategoryFilter.ROOMS -> res is UnifiedSearchResult.RoomResult && res.roomType.contains("Classroom", ignoreCase = true)
                SearchCategoryFilter.FACULTY -> res is UnifiedSearchResult.FacultyResult || (res is UnifiedSearchResult.RoomResult && !res.facultyName.isNullOrBlank())
                SearchCategoryFilter.LABS -> (res is UnifiedSearchResult.RoomResult && (res.roomType.contains("Lab", ignoreCase = true) || res.roomNo.contains("Lab", ignoreCase = true)))
                SearchCategoryFilter.EQUIPMENT -> res is UnifiedSearchResult.EquipmentResult || (res is UnifiedSearchResult.RoomResult && !res.equipmentList.isNullOrBlank())
            }
        }

        AnimatedVisibility(
            visible = query.isNotBlank() && filteredResults.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 320.dp)
            ) {
                LazyColumn {
                    items(filteredResults) { result ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectResult(result) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            val (iconVector, iconColor, badgeText) = when (result) {
                                is UnifiedSearchResult.CampusBuildingResult -> {
                                    Triple(Icons.Default.LocationOn, getCategoryColor(result.building.category), "Building")
                                }
                                is UnifiedSearchResult.RoomResult -> {
                                    val isLab = result.roomType.contains("Lab", ignoreCase = true)
                                    val isCabin = result.roomType.contains("Cabin", ignoreCase = true)
                                    if (isLab) Triple(Icons.Default.Science, Color(0xFF059669), "Lab")
                                    else if (isCabin) Triple(Icons.Default.Person, Color(0xFFD97706), "Cabin")
                                    else Triple(Icons.Default.MeetingRoom, Color(0xFF0284C7), "Room")
                                }
                                is UnifiedSearchResult.FacultyResult -> {
                                    Triple(Icons.Default.Person, Color(0xFFD97706), "Faculty")
                                }
                                is UnifiedSearchResult.EquipmentResult -> {
                                    Triple(Icons.Default.Science, Color(0xFF059669), "Equipment")
                                }
                                else -> {
                                    Triple(Icons.Default.LocationOn, Color(0xFF6B7280), "Place")
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(iconColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = result.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = iconColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = iconColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = result.subtitle,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryColor(category: LocationCategory): Color {
    return when (category) {
        LocationCategory.ACADEMIC -> Color(0xFF0284C7)
        LocationCategory.HOSTEL -> Color(0xFF7C3AED)
        LocationCategory.DINING -> Color(0xFFF59E0B)
        LocationCategory.SPORTS -> Color(0xFF10B981)
        else -> SrmCrimson
    }
}
