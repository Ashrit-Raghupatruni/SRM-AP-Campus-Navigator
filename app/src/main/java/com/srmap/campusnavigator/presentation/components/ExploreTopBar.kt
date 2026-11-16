package com.srmap.campusnavigator.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srmap.campusnavigator.core.designsystem.MidnightNavy
import com.srmap.campusnavigator.core.designsystem.PureWhite
import com.srmap.campusnavigator.core.designsystem.SrmCrimson

data class CategoryShortcut(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun ExploreTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    shortcuts: List<CategoryShortcut>,
    selectedShortcut: String?,
    onSelectShortcut: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Floating Search Bar
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSearchClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SrmCrimson,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (query.isNotBlank()) query else "Search classrooms, faculty, labs, blocks...",
                    fontSize = 14.sp,
                    color = if (query.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Category Shortcuts
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(shortcuts) { shortcut ->
                val isSelected = shortcut.id == selectedShortcut
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MidnightNavy else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = if (isSelected) 6.dp else 2.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectShortcut(shortcut.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PureWhite.copy(alpha = 0.2f) else shortcut.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = shortcut.icon,
                                contentDescription = null,
                                tint = if (isSelected) PureWhite else shortcut.color,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = shortcut.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
