package com.srmap.campusnavigator.presentation.navigation3d

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.srmap.campusnavigator.core.designsystem.DeepTeal
import com.srmap.campusnavigator.core.designsystem.MidnightNavy
import com.srmap.campusnavigator.core.designsystem.MintAccent
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.core.rendering3d.Campus3DRenderer
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import com.srmap.campusnavigator.domain.navigation3d.CameraViewMode

@Composable
fun FirstPersonNavigationScreen(
    modifier: Modifier = Modifier,
    buildings: List<CampusBuilding>,
    graph: CampusGraph?,
    features: List<com.srmap.campusnavigator.domain.models.CampusFeature> = emptyList(),
    selectedBuilding: CampusBuilding?,
    route: PedestrianRoute?,
    routeProgress: com.srmap.campusnavigator.domain.routing.DynamicRouteProgress? = null,
    userLocation: UserLocationState?,
    mapMatchedBearing: Float? = null,
    onSwitchTo2D: () -> Unit,
    onEndNavigation: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    var currentViewMode by remember { mutableStateOf(CameraViewMode.FIRST_PERSON) }

    val glView = remember(buildings, graph, features) {
        val r = Campus3DRenderer(context, buildings, graph, features)
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(r)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        } to r
    }
    val surfaceView = glView.first
    val renderer = glView.second

    // Lifecycle-aware GLSurfaceView management (onPause / onResume)
    DisposableEffect(lifecycleOwner, surfaceView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> surfaceView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> surfaceView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            surfaceView.onPause()
        }
    }

    // Keep 3D engine state in sync with ViewModel navigation state
    LaunchedEffect(selectedBuilding, route, userLocation, routeProgress, mapMatchedBearing) {
        val segIdx = routeProgress?.currentSegmentIndex ?: 0
        renderer.updateNavigationState(selectedBuilding, route, userLocation, segIdx, mapMatchedBearing)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Native OpenGL ES 3.0 Surface View with Multi-Touch Gestures (Pan, Orbit, Tilt, Zoom)
        AndroidView(
            factory = { glView.first },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentViewMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (pan.x != 0f || pan.y != 0f) {
                            renderer.cameraController.onUserDrag(pan.x, pan.y)
                        }
                        if (zoom != 1f) {
                            renderer.cameraController.onUserZoom(zoom)
                        }
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

        // Camera POV Mode Switcher Pill (| 1st POV | 3rd POV |) positioned under the top HUD
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp),
            shape = RoundedCornerShape(24.dp),
            color = MidnightNavy.copy(alpha = 0.92f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val is1st = currentViewMode == CameraViewMode.FIRST_PERSON
                // 1st POV Segment
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (is1st) MintAccent else Color.Transparent)
                        .clickable {
                            currentViewMode = CameraViewMode.FIRST_PERSON
                            renderer.cameraController.setMode(CameraViewMode.FIRST_PERSON)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1st POV",
                        fontSize = 12.sp,
                        fontWeight = if (is1st) FontWeight.Bold else FontWeight.Medium,
                        color = if (is1st) MidnightNavy else Color.White
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // 3rd POV Segment
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!is1st) MintAccent else Color.Transparent)
                        .clickable {
                            currentViewMode = CameraViewMode.THIRD_PERSON
                            renderer.cameraController.setMode(CameraViewMode.THIRD_PERSON)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3rd POV",
                        fontSize = 12.sp,
                        fontWeight = if (!is1st) FontWeight.Bold else FontWeight.Medium,
                        color = if (!is1st) MidnightNavy else Color.White
                    )
                }
            }
        }

        // SR Block Viewpoint Inspection Bar (Visible when SR Block is selected)
        val isSRBlockTarget = selectedBuilding?.id == "srmap_bldg_16" || selectedBuilding?.name?.contains("SR Block", ignoreCase = true) == true
        if (isSRBlockTarget) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 165.dp),
                shape = RoundedCornerShape(18.dp),
                color = MidnightNavy.copy(alpha = 0.90f),
                shadowElevation = 5.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val viewpoints = remember { com.srmap.campusnavigator.domain.building3d.SRBlockSpec.getSRBlockViewpoints() }
                    viewpoints.forEach { vp ->
                        val shortLabel = when (vp.id) {
                            "photo1_sunset_sports" -> "Sunset"
                            "photo2_corner_ribbon" -> "Corner"
                            "photo3_aerial_master" -> "Aerial"
                            "sr_main_entrance" -> "Entrance"
                            "east_elevation_courts" -> "Courts"
                            "top_down_roof" -> "Roof"
                            else -> vp.name
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    currentViewMode = CameraViewMode.THIRD_PERSON
                                    renderer.cameraController.applyViewpoint(vp)
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = shortLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MintAccent
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Buttons (Recenter Heading & 2D Mode Switch)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 105.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { renderer.cameraController.resetOrientation() },
                containerColor = Color.White,
                contentColor = MidnightNavy
            ) {
                Icon(Icons.Default.Explore, contentDescription = "Recenter Orbit / Heading")
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
                        text = if (currentViewMode == CameraViewMode.FIRST_PERSON) {
                            "1.6m First-Person Eye Level"
                        } else {
                            "3rd-Person Chase & 360° Orbit View"
                        },
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

