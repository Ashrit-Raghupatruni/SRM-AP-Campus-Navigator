package com.srmap.campusnavigator.presentation.search

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
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
import com.srmap.campusnavigator.domain.models.UnifiedSearchResult
import com.srmap.campusnavigator.presentation.components.SearchCategoryFilter
import com.srmap.campusnavigator.presentation.components.SearchCategoryChipsRow
import com.srmap.campusnavigator.presentation.components.getCategoryColor

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: SearchCategoryFilter,
    onFilterChange: (SearchCategoryFilter) -> Unit,
    searchResults: List<UnifiedSearchResult>,
    allRooms: List<UnifiedSearchResult.RoomResult> = emptyList(),
    allFaculty: List<UnifiedSearchResult.FacultyResult> = emptyList(),
    allEquipment: List<UnifiedSearchResult.EquipmentResult> = emptyList(),
    onSelectResult: (UnifiedSearchResult) -> Unit,
    onNavigateResult: (UnifiedSearchResult) -> Unit,
    recentSearches: List<String> = emptyList(),
    onClearRecentSearches: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Campus Directory",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Search classrooms, faculty cabins, labs & equipment",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Search Input
        Surface(
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search rooms, faculty, labs, equipment...") },
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
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Category Chips (ALL, ROOMS, FACULTY, LABS, EQUIPMENT)
        SearchCategoryChipsRow(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Active vs. Category-Wise Default Directory
        if (query.isNotBlank()) {
            val filteredLiveList = searchResults.filter { res ->
                when (selectedFilter) {
                    SearchCategoryFilter.ALL -> true
                    SearchCategoryFilter.ROOMS -> res is UnifiedSearchResult.RoomResult && res.roomType.contains("Classroom", ignoreCase = true)
                    SearchCategoryFilter.FACULTY -> res is UnifiedSearchResult.FacultyResult || (res is UnifiedSearchResult.RoomResult && !res.facultyName.isNullOrBlank())
                    SearchCategoryFilter.LABS -> (res is UnifiedSearchResult.RoomResult && (res.roomType.contains("Lab", ignoreCase = true) || res.roomNo.contains("Lab", ignoreCase = true)))
                    SearchCategoryFilter.EQUIPMENT -> res is UnifiedSearchResult.EquipmentResult || (res is UnifiedSearchResult.RoomResult && !res.equipmentList.isNullOrBlank())
                }
            }

            if (filteredLiveList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching results found for '$query'", color = Color.Gray, fontSize = 15.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)
                ) {
                    items(filteredLiveList) { result ->
                        SearchResultItemCard(result, onSelectResult, onNavigateResult)
                    }
                }
            }
        } else {
            // DEFAULT VIEW: Recent Searches + Category-Wise Browsing
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)
            ) {
                // 1. Recent Searches
                if (recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Searches",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            if (onClearRecentSearches != null) {
                                TextButton(onClick = onClearRecentSearches) {
                                    Text("Clear", fontSize = 12.sp, color = SrmCrimson)
                                }
                            }
                        }
                    }
                    items(recentSearches) { rec ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onQueryChange(rec) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(rec, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    item {
                        Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                // 2. Default Categorized Listings depending on selectedFilter
                when (selectedFilter) {
                    SearchCategoryFilter.ALL -> {
                        // Section: Classrooms
                        item {
                            Text(
                                "🎓 Classrooms & Lecture Halls",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        val sampleRooms = allRooms.take(5)
                        if (sampleRooms.isEmpty()) {
                            item { Text("No classrooms available", fontSize = 13.sp, color = Color.Gray) }
                        } else {
                            items(sampleRooms) { r ->
                                SearchResultItemCard(r, onSelectResult, onNavigateResult)
                            }
                        }

                        // Section: Faculty
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "👨‍🏫 Faculty Members",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        val sampleFaculty = allFaculty.take(5)
                        if (sampleFaculty.isEmpty()) {
                            item { Text("No faculty records available", fontSize = 13.sp, color = Color.Gray) }
                        } else {
                            items(sampleFaculty) { f ->
                                SearchResultItemCard(f, onSelectResult, onNavigateResult)
                            }
                        }

                        // Section: Labs & Equipment
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "🔬 Research Labs & Equipment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        val sampleEquip = allEquipment.take(5)
                        if (sampleEquip.isEmpty()) {
                            item { Text("No equipment records available", fontSize = 13.sp, color = Color.Gray) }
                        } else {
                            items(sampleEquip) { e ->
                                SearchResultItemCard(e, onSelectResult, onNavigateResult)
                            }
                        }
                    }

                    SearchCategoryFilter.ROOMS -> {
                        item {
                            Text("🎓 All Classrooms (${allRooms.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        items(allRooms) { r ->
                            SearchResultItemCard(r, onSelectResult, onNavigateResult)
                        }
                    }

                    SearchCategoryFilter.FACULTY -> {
                        item {
                            Text("👨‍🏫 All Faculty Members (${allFaculty.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        items(allFaculty) { f ->
                            SearchResultItemCard(f, onSelectResult, onNavigateResult)
                        }
                    }

                    SearchCategoryFilter.LABS -> {
                        val labRooms = allRooms.filter { it.roomType.contains("Lab", ignoreCase = true) || it.roomNo.contains("Lab", ignoreCase = true) }
                        item {
                            Text("🔬 Campus Laboratories (${labRooms.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        items(labRooms) { l ->
                            SearchResultItemCard(l, onSelectResult, onNavigateResult)
                        }
                    }

                    SearchCategoryFilter.EQUIPMENT -> {
                        item {
                            Text("🔬 All Equipment & Instruments (${allEquipment.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        items(allEquipment) { e ->
                            SearchResultItemCard(e, onSelectResult, onNavigateResult)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItemCard(
    result: UnifiedSearchResult,
    onSelectResult: (UnifiedSearchResult) -> Unit,
    onNavigateResult: (UnifiedSearchResult) -> Unit
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
            else Triple(Icons.Default.MeetingRoom, Color(0xFF0284C7), "Classroom")
        }
        is UnifiedSearchResult.FacultyResult -> {
            Triple(Icons.Default.Person, Color(0xFFD97706), "Faculty")
        }
        is UnifiedSearchResult.EquipmentResult -> {
            Triple(Icons.Default.Science, Color(0xFF059669), "Equipment")
        }
        else -> {
            Triple(Icons.Default.LocationOn, Color(0xFF6B7280), "Location")
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectResult(result) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                            fontWeight = FontWeight.Bold,
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iconColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.subtitle,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = { onNavigateResult(result) }) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = "Navigate",
                        tint = SrmCrimson
                    )
                }
            }
        }
    }
}
