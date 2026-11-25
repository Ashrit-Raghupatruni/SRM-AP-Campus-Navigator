package com.srmap.campusnavigator.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun SearchResultCard(
    result: UnifiedSearchResult,
    onSelect: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint, badge) = when (result) {
        is UnifiedSearchResult.CampusBuildingResult -> Triple(Icons.Default.LocationOn, AccentAcademic, "Building")
        is UnifiedSearchResult.RoomResult -> {
            val isLab = result.roomType.contains("Lab", ignoreCase = true)
            val isCabin = result.roomType.contains("Cabin", ignoreCase = true)
            if (isLab) Triple(Icons.Default.Science, AccentLab, "Lab")
            else if (isCabin) Triple(Icons.Default.Person, AccentFaculty, "Cabin")
            else Triple(Icons.Default.MeetingRoom, AccentAcademic, "Classroom")
        }
        is UnifiedSearchResult.FacultyResult -> Triple(Icons.Default.Person, AccentFaculty, "Faculty")
        is UnifiedSearchResult.EquipmentResult -> Triple(Icons.Default.Science, AccentLab, "Equipment")
        is UnifiedSearchResult.AmenityResult -> {
            when (result.category) {
                com.srmap.campusnavigator.domain.models.AmenityCategory.WATER_COOLER -> Triple(Icons.Default.WaterDrop, AccentWater, "Water")
                com.srmap.campusnavigator.domain.models.AmenityCategory.FOOD_CAFE -> Triple(Icons.Default.Restaurant, AccentFood, "Food")
                com.srmap.campusnavigator.domain.models.AmenityCategory.ATM -> Triple(Icons.Default.AccountBalance, AccentAtm, "ATM")
                com.srmap.campusnavigator.domain.models.AmenityCategory.PRINTING -> Triple(Icons.Default.Print, AccentPrint, "Print")
                com.srmap.campusnavigator.domain.models.AmenityCategory.HEALTHCARE -> Triple(Icons.Default.LocalHospital, AccentHealth, "Clinic")
                com.srmap.campusnavigator.domain.models.AmenityCategory.STATIONERY -> Triple(Icons.Default.Store, AccentPrint, "Store")
                com.srmap.campusnavigator.domain.models.AmenityCategory.FACILITY -> Triple(Icons.Default.Business, AccentAcademic, "Facility")
            }
        }
        else -> Triple(Icons.Default.LocationOn, SlateGrey, "Place")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
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
                        shape = RoundedCornerShape(6.dp),
                        color = tint.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tint,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = result.subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onNavigate) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = "Navigate",
                    tint = SrmCrimson
                )
            }
        }
    }
}
