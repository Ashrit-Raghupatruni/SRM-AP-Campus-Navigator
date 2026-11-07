package com.srmap.campusnavigator.presentation.map

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.srmap.campusnavigator.core.designsystem.*
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.CampusFeature
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.LocationCategory
import com.srmap.campusnavigator.data.maptiler.maps.MapTilerStyle
import com.srmap.campusnavigator.domain.models.PedestrianRoute

import com.srmap.campusnavigator.domain.routing.DynamicRouteProgress

@Composable
fun InteractiveCampusCanvasMap(
    modifier: Modifier = Modifier,
    buildings: List<CampusBuilding>,
    features: List<CampusFeature>,
    selectedBuilding: CampusBuilding?,
    route: PedestrianRoute?,
    routeProgress: DynamicRouteProgress? = null,
    userLocation: UserLocationState?,
    mapStyle: MapTilerStyle = MapTilerStyle.STREETS,
    zoomLevel: Float = 1.0f,
    focusTarget: GeoPoint? = null,
    onBuildingSelected: (CampusBuilding) -> Unit
) {
    // Exact SRM AP Campus Core Bounds
    val campusMinLat = 16.4595519
    val campusMaxLat = 16.4669104
    val campusMinLon = 80.5038838
    val campusMaxLon = 80.5089378

    val centerLat = (campusMinLat + campusMaxLat) / 2.0 // ~16.4632311
    val centerLon = (campusMinLon + campusMaxLon) / 2.0 // ~80.5064108

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Sync external zoomLevel changes (from + and - buttons)
    LaunchedEffect(zoomLevel) {
        scale = zoomLevel.coerceIn(0.6f, 6.0f)
    }

    // Pulsing animation for user GPS dot
    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Compute projection scales dynamically tailored to screen resolution
    fun getProjectionScales(w: Float, h: Float): Pair<Float, Float> {
        val latSpan = (campusMaxLat - campusMinLat).toFloat()
        val lonSpan = (campusMaxLon - campusMinLon).toFloat()

        // Reserve top 90dp for search bar and bottom 100dp for controls
        val availableH = (h - 220f).coerceAtLeast(600f)
        val availableW = (w - 60f).coerceAtLeast(400f)

        val pxPerDegLat = (availableH / latSpan) * scale
        val pxPerDegLon = (availableW / lonSpan) * scale
        return Pair(pxPerDegLat, pxPerDegLon)
    }

    fun project(point: GeoPoint, width: Float, height: Float): Offset {
        val (pxPerDegLat, pxPerDegLon) = getProjectionScales(width, height)
        val x = (point.longitude - centerLon).toFloat() * pxPerDegLon + (width / 2f) + offsetX
        val y = -(point.latitude - centerLat).toFloat() * pxPerDegLat + (height / 2f) + offsetY
        return Offset(x, y)
    }

    fun unproject(screenOffset: Offset, width: Float, height: Float): GeoPoint {
        val (pxPerDegLat, pxPerDegLon) = getProjectionScales(width, height)
        val lon = (screenOffset.x - (width / 2f) - offsetX) / pxPerDegLon + centerLon
        val lat = -((screenOffset.y - (height / 2f) - offsetY) / pxPerDegLat) + centerLat
        return GeoPoint(lat, lon)
    }

    // Camera animation when focusTarget changes (e.g. from search selection)
    LaunchedEffect(focusTarget) {
        focusTarget?.let { _ ->
            offsetX = 0f
            offsetY = 0f
        }
    }

    val canvasBg = when (mapStyle) {
        MapTilerStyle.SATELLITE -> Color(0xFF0F172A)
        MapTilerStyle.HYBRID -> Color(0xFF0F172A)
        MapTilerStyle.TOPO -> Color(0xFFFBF8EE)
        MapTilerStyle.OUTDOOR -> Color(0xFFE2E8F0)
        else -> Color(0xFFF1F5F9)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.7f, 5.0f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(buildings, scale, offsetX, offsetY) {
                detectTapGestures { tapOffset ->
                    val clickedGeoPoint = unproject(tapOffset, size.width.toFloat(), size.height.toFloat())

                    // 1. Precise Ray-Casting Polygon Hit-Testing
                    var found = buildings.find { it.containsPoint(clickedGeoPoint) }

                    // 2. Proximity fallback to centroid
                    if (found == null) {
                        found = buildings.find { bldg ->
                            val centerPx = project(bldg.centroid, size.width.toFloat(), size.height.toFloat())
                            (tapOffset - centerPx).getDistance() < (45f * scale).coerceAtLeast(35f)
                        }
                    }

                    if (found != null) {
                        onBuildingSelected(found)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw Campus Boundary Background
        val isSatelliteOrDark = mapStyle == MapTilerStyle.SATELLITE || mapStyle == MapTilerStyle.HYBRID
        val boundaryFill = when (mapStyle) {
            MapTilerStyle.SATELLITE, MapTilerStyle.HYBRID -> Color(0xFF1E293B).copy(alpha = 0.85f)
            MapTilerStyle.TOPO -> Color(0xFFFAF7EE)
            MapTilerStyle.OUTDOOR -> Color(0xFFF8FAFC)
            else -> Color(0xFFFFFFFF)
        }

        for (f in features) {
            if (f.rawProperties.containsKey("amenity") && f.rawProperties["amenity"] == "university") {
                if (f.coordinates.size >= 3) {
                    val bPath = Path()
                    val first = project(f.coordinates[0], w, h)
                    bPath.moveTo(first.x, first.y)
                    for (i in 1 until f.coordinates.size) {
                        val pt = project(f.coordinates[i], w, h)
                        bPath.lineTo(pt.x, pt.y)
                    }
                    bPath.close()
                    drawPath(path = bPath, color = boundaryFill, style = Fill)
                    drawPath(
                        path = bPath,
                        color = if (isSatelliteOrDark) Color(0xFF38BDF8) else SrmCrimson.copy(alpha = 0.45f),
                        style = Stroke(
                            width = 3f * scale,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                        )
                    )
                }
            }
        }

        // 2. Draw Grounds & Sports Pitches
        for (f in features) {
            val isSports = f.rawProperties["leisure"] == "pitch" || f.rawProperties["leisure"] == "track" || f.category == LocationCategory.SPORTS
            val isGarden = f.rawProperties["leisure"] == "park" || f.rawProperties["leisure"] == "outdoor_seating" || f.rawProperties["landuse"] == "forest"
            if (isSports || isGarden) {
                if (f.coordinates.size >= 3) {
                    val gPath = Path()
                    val first = project(f.coordinates[0], w, h)
                    gPath.moveTo(first.x, first.y)
                    for (i in 1 until f.coordinates.size) {
                        val pt = project(f.coordinates[i], w, h)
                        gPath.lineTo(pt.x, pt.y)
                    }
                    gPath.close()
                    val fillColor = if (isSports) Color(0xFFDCFCE7) else Color(0xFFF0FDF4)
                    drawPath(path = gPath, color = fillColor, style = Fill)
                    drawPath(
                        path = gPath,
                        color = if (isSports) Color(0xFF86EFAC) else Color(0xFFBBF7D0),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }

        // 3. Draw Campus Roads (Clean Asphalt lines)
        for (f in features) {
            if (f.geometryType == "LineString" && f.rawProperties.containsKey("highway")) {
                val hw = f.rawProperties["highway"]
                if (hw != "path" && hw != "footway") {
                    val rPath = Path()
                    val pts = f.coordinates
                    if (pts.isNotEmpty()) {
                        val first = project(pts[0], w, h)
                        rPath.moveTo(first.x, first.y)
                        for (i in 1 until pts.size) {
                            val pt = project(pts[i], w, h)
                            rPath.lineTo(pt.x, pt.y)
                        }
                        // Road Casing
                        drawPath(
                            path = rPath,
                            color = Color(0xFF475569),
                            style = Stroke(width = 9f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        // Road Center
                        drawPath(
                            path = rPath,
                            color = Color(0xFF94A3B8),
                            style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }

        // 4. Draw Pedestrian Pathways (De-emphasized neutral slate-gray dashes)
        for (f in features) {
            val hw = f.rawProperties["highway"]
            if (hw == "path" || hw == "footway") {
                val pPath = Path()
                val pts = f.coordinates
                if (pts.isNotEmpty()) {
                    val first = project(pts[0], w, h)
                    pPath.moveTo(first.x, first.y)
                    for (i in 1 until pts.size) {
                        val pt = project(pts[i], w, h)
                        pPath.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = pPath,
                        color = Color(0xFF64748B).copy(alpha = 0.55f),
                        style = Stroke(
                            width = 2.5f * scale,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }

        // 5. Draw Buildings with Category Colors
        for (bldg in buildings) {
            val isSelected = bldg.id == selectedBuilding?.id
            if (bldg.polygon.size >= 3) {
                val bPath = Path()
                val first = project(bldg.polygon[0], w, h)
                bPath.moveTo(first.x, first.y)
                for (i in 1 until bldg.polygon.size) {
                    val pt = project(bldg.polygon[i], w, h)
                    bPath.lineTo(pt.x, pt.y)
                }
                bPath.close()

                val fillColor = when {
                    isSelected -> SrmCrimson.copy(alpha = 0.5f)
                    isSatelliteOrDark -> when (bldg.category) {
                        LocationCategory.ACADEMIC -> Color(0xFF334155).copy(alpha = 0.9f)
                        LocationCategory.HOSTEL -> Color(0xFF4C1D95).copy(alpha = 0.85f)
                        LocationCategory.DINING -> Color(0xFF854D0E).copy(alpha = 0.85f)
                        LocationCategory.SPORTS -> Color(0xFF065F46).copy(alpha = 0.85f)
                        else -> Color(0xFF1E293B).copy(alpha = 0.85f)
                    }
                    bldg.category == LocationCategory.ACADEMIC -> Color(0xFFE2E8F0)
                    bldg.category == LocationCategory.HOSTEL -> Color(0xFFEDE9FE)
                    bldg.category == LocationCategory.DINING -> Color(0xFFFEF08A)
                    else -> Color(0xFFF1F5F9)
                }
                val strokeColor = when {
                    isSelected -> SrmCrimson
                    isSatelliteOrDark -> Color(0xFF94A3B8)
                    else -> Color(0xFF475569)
                }

                drawPath(path = bPath, color = fillColor, style = Fill)
                drawPath(
                    path = bPath,
                    color = strokeColor,
                    style = Stroke(width = if (isSelected) 5.5f * scale else 2f * scale, join = StrokeJoin.Round)
                )
            }
        }

        // 6. Draw Building Labels with Collision Prevention (Show ALL named buildings)
        val textPaint = Paint().apply {
            textSize = (12f * scale).coerceIn(14f, 32f)
            color = android.graphics.Color.parseColor("#0F172A")
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val haloPaint = Paint(textPaint).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        val renderedLabelBoxes = mutableListOf<RectF>()

        // Prioritize: selected building first, then hostels & academic blocks
        val sortedForLabels = buildings.filter { it.name.isNotBlank() && !it.name.startsWith("Facility (") }
            .sortedByDescending { it.id == selectedBuilding?.id }

        for (bldg in sortedForLabels) {
            val isSelected = bldg.id == selectedBuilding?.id
            val cOffset = project(bldg.centroid, w, h)

            val textBounds = Rect()
            textPaint.getTextBounds(bldg.name, 0, bldg.name.length, textBounds)
            val labelBox = RectF(
                cOffset.x - textBounds.width() / 2f - 4,
                cOffset.y - textBounds.height() - 2,
                cOffset.x + textBounds.width() / 2f + 4,
                cOffset.y + 4
            )

            val collides = renderedLabelBoxes.any { existing -> RectF.intersects(existing, labelBox) }

            if (!collides || isSelected) {
                // White high-contrast outline
                drawContext.canvas.nativeCanvas.drawText(bldg.name, cOffset.x, cOffset.y + 4f, haloPaint)
                // Dark slate text
                drawContext.canvas.nativeCanvas.drawText(bldg.name, cOffset.x, cOffset.y + 4f, textPaint)
                renderedLabelBoxes.add(labelBox)
            }
        }

        // 7. Draw Dynamic Pedestrian Navigation Route (Completed Muted Slate + Remaining Vibrant Blue)
        val activeRemaining = routeProgress?.remainingPath ?: route?.path
        val activeCompleted = routeProgress?.completedPath

        // A. Draw Completed Traveled Route (Muted Slate Grey with soft dash)
        if (activeCompleted != null && activeCompleted.size >= 2) {
            val compPath = Path()
            val first = project(activeCompleted[0], w, h)
            compPath.moveTo(first.x, first.y)
            for (i in 1 until activeCompleted.size) {
                val pt = project(activeCompleted[i], w, h)
                compPath.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = compPath,
                color = Color(0xFF64748B).copy(alpha = 0.65f),
                style = Stroke(width = 6f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // B. Draw Remaining Navigation Route (Vibrant Cyan / Blue)
        if (activeRemaining != null && activeRemaining.size >= 2) {
            val routePath = Path()
            val first = project(activeRemaining[0], w, h)
            routePath.moveTo(first.x, first.y)
            for (i in 1 until activeRemaining.size) {
                val pt = project(activeRemaining[i], w, h)
                routePath.lineTo(pt.x, pt.y)
            }

            // Vibrant cyan glow
            drawPath(
                path = routePath,
                color = Color(0x660284C7),
                style = Stroke(width = 16f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Vibrant electric blue route line
            drawPath(
                path = routePath,
                color = Color(0xFF0284C7),
                style = Stroke(width = 7.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Start Marker (Green circle)
            drawCircle(color = SrmWalkingGreen, radius = 9f * scale, center = first)
            drawCircle(color = Color.White, radius = 4.5f * scale, center = first)

            // Destination Marker (Crimson circle)
            val dest = project(activeRemaining.last(), w, h)
            drawCircle(color = SrmCrimson, radius = 12f * scale, center = dest)
            drawCircle(color = Color.White, radius = 6f * scale, center = dest)
        }

        // 8. Draw Live GPS User Dot with Real Accuracy Circle
        userLocation?.let { locState ->
            val userPx = project(locState.point, w, h)
            val (pxPerDegLat, _) = getProjectionScales(w, h)
            // 1 degree latitude = ~111,320 meters
            val accuracyPx = (locState.accuracyMeters * (pxPerDegLat / 111320f)).coerceIn(16f, 180f)

            // Accuracy radius circle
            drawCircle(
                color = MapUserDot.copy(alpha = 0.15f),
                radius = accuracyPx,
                center = userPx
            )
            drawCircle(
                color = MapUserDot.copy(alpha = 0.35f),
                radius = accuracyPx,
                center = userPx,
                style = Stroke(width = 1.5f)
            )

            // Pulse ring
            drawCircle(
                color = MapUserDot.copy(alpha = pulseAlpha),
                radius = 16f * scale * pulseScale,
                center = userPx
            )

            // Solid user dot with white ring
            drawCircle(color = Color.White, radius = 9f * scale, center = userPx)
            drawCircle(color = MapUserDot, radius = 6.5f * scale, center = userPx)
        }
    }
}
