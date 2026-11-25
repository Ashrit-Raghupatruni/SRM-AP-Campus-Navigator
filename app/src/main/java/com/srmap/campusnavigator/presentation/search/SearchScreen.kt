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
import androidx.compose.material.icons.filled.History
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
import com.srmap.campusnavigator.core.designsystem.*
import com.srmap.campusnavigator.domain.models.UnifiedSearchResult
import com.srmap.campusnavigator.presentation.components.CampusEmptyStateView
import com.srmap.campusnavigator.presentation.components.SearchCategoryChipsRow
import com.srmap.campusnavigator.presentation.components.SearchCategoryFilter
import com.srmap.campusnavigator.presentation.components.SearchResultCard

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: SearchCategoryFilter,
    onFilterChange: (SearchCategoryFilter) -> Unit,
    searchResults: List<UnifiedSearchResult>,
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
        // Campus Directory Header
        Text(
            text = "Campus Directory",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Instant room numbers, faculty suites, research labs & equipment",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Modern Outlined Omni-Search Bar
        Surface(
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search rooms, water, food, ATMs, faculty, clinic...",
                        fontSize = 14.sp,
                        color = SlateGrey
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
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = SlateGrey)
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

        // Category Filter Chips: ALL, BUILDINGS, ROOMS, FACULTY, LABS, EQUIPMENT
        SearchCategoryChipsRow(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Results or Default Directory View
        if (query.isNotBlank()) {
            if (searchResults.isEmpty()) {
                CampusEmptyStateView(
                    icon = Icons.Default.Search,
                    title = "No results found",
                    subtitle = "We couldn't find any campus records matching '$query'. Check spelling or browse category tabs above."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp)
                ) {
                    items(searchResults) { result ->
                        SearchResultCard(
                            result = result,
                            onSelect = { onSelectResult(result) },
                            onNavigate = { onNavigateResult(result) }
                        )
                    }
                }
            }
        } else {
            // Default View: Recent Searches & Suggestion Pills
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp)
            ) {
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
                                color = SlateGrey
                            )
                            if (onClearRecentSearches != null) {
                                TextButton(onClick = onClearRecentSearches) {
                                    Text("Clear All", fontSize = 12.sp, color = SrmCrimson)
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
                            Icon(Icons.Default.History, contentDescription = null, tint = SlateGrey, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(rec, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    item {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // Quick Suggestion Pills
                item {
                    Text(
                        text = "Quick Search Suggestions",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = SlateGrey,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                item {
                    val suggestions = listOf("S-201", "Dr. Ramesh Kumar", "Electron Microscope", "AUD-1", "CV Raman", "V-101", "3D Printer")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions) { sugg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.clickable { onQueryChange(sugg) }
                            ) {
                                Text(
                                    text = sugg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                // Default Browse Preview Cards
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Browse ${selectedFilter.label} Records (${searchResults.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(searchResults) { result ->
                    SearchResultCard(
                        result = result,
                        onSelect = { onSelectResult(result) },
                        onNavigate = { onNavigateResult(result) }
                    )
                }
            }
        }
    }
}
