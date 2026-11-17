package com.srmap.campusnavigator.presentation.navigation3d

import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.srmap.campusnavigator.core.designsystem.DeepTeal
import com.srmap.campusnavigator.core.designsystem.MidnightNavy
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.core.rendering3d.Campus3DRenderer
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.PedestrianRoute

@Composable
fun FirstPersonNavigationScreen(
    modifier: Modifier = Modifier,
    buildings: List<CampusBuilding>,
    graph: CampusGraph?,
    features: List<com.srmap.campusnavigator.domain.models.CampusFeature> = emptyList(),
    selectedBuilding: CampusBuilding?,
    route: PedestrianRoute?,
    userLocation: UserLocationState?,
    onSwitchTo2D: () -> Unit,
    onEndNavigation: () -> Unit
) {
    val context = LocalContext.current
    val glView = remember(buildings, graph, features) {
        val r = Campus3DRenderer(context, buildings, graph, features)
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(r)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        } to r
    }
    val renderer = glView.second

    // Keep 3D engine state in sync with ViewModel navigation state
    LaunchedEffect(selectedBuilding, route, userLocation) {
        renderer.updateNavigationState(selectedBuilding, route, userLocation)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Native OpenGL ES 3.0 Surface View with horizontal look gesture
        AndroidView(
            factory = { glView.first },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Sensitivity: 0.25 deg per pixel drag
                        renderer.cameraController.onUserDragRotate(-dragAmount.x * 0.25f)
                    }
                }
        )

        // Top Navigation HUD: Step Instructions
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            shape = MaterialTheme.shapes.large,
            color = MidnightNavy.copy(alpha = 0.94f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = DeepTeal,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val currentStepText = route?.steps?.firstOrNull()?.instruction
                        ?: "Navigating to ${selectedBuilding?.name ?: "Destination"}"
                    Text(
                        text = currentStepText,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    route?.let {
                        Text(
                            text = "${it.totalDistanceMeters.toInt()}m remaining • approx. ${it.estimatedTimeSeconds / 60} min walk",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Floating Action Buttons (Recenter & 2D Mode Switch)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { renderer.cameraController.resetOrientation() },
                containerColor = Color.White,
                contentColor = MidnightNavy
            ) {
                Icon(Icons.Default.Explore, contentDescription = "Recenter Heading")
            }

            FloatingActionButton(
                onClick = onSwitchTo2D,
                containerColor = DeepTeal,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Map, contentDescription = "Switch to 2D Map")
            }
        }

        // Bottom Navigation Bar with End Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedBuilding?.name ?: "Campus Destination",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightNavy
                    )
                    Text(
                        text = "1.6m First-Person Eye Level",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }

                Button(
                    onClick = onEndNavigation,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA61B35))
                ) {
                    Text("End Navigation")
                }
            }
        }
    }
}
