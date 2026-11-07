package com.srmap.campusnavigator.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.srmap.campusnavigator.core.designsystem.SrmCrimson

@Composable
fun MapFloatingControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit,
    onToggleLayers: () -> Unit,
    onResetNorth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Zoom In / Out Stack
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onZoomIn, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.DarkGray)
                }
                Divider(modifier = Modifier.size(width = 30.dp, height = 1.dp), color = Color(0xFFE2E8F0))
                IconButton(onClick = onZoomOut, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.DarkGray)
                }
            }
        }

        // Compass / Reset North
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            IconButton(onClick = onResetNorth, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Navigation, contentDescription = "Reset North", tint = SrmCrimson)
            }
        }

        // Layer Selector
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            IconButton(onClick = onToggleLayers, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Layers, contentDescription = "Layers", tint = Color.DarkGray)
            }
        }

        // Recenter Location FAB
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp,
            color = SrmCrimson
        ) {
            IconButton(onClick = onRecenter, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter Location", tint = Color.White)
            }
        }
    }
}
