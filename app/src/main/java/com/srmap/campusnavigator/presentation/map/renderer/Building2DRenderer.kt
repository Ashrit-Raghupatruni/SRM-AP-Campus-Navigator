package com.srmap.campusnavigator.presentation.map.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.LocationCategory

object Building2DRenderer {

    fun draw2DBuilding(
        drawScope: DrawScope,
        building: CampusBuilding,
        projectedPoints: List<Offset>,
        scale: Float,
        isSatelliteOrDark: Boolean
    ) {
        if (projectedPoints.size < 3) return

        val path = Path().apply {
            moveTo(projectedPoints[0].x, projectedPoints[0].y)
            for (i in 1 until projectedPoints.size) {
                lineTo(projectedPoints[i].x, projectedPoints[i].y)
            }
            close()
        }

        val fillColor = if (isSatelliteOrDark) {
            when (building.category) {
                LocationCategory.ACADEMIC -> Color(0xFF334155).copy(alpha = 0.9f)
                LocationCategory.HOSTEL -> Color(0xFF4C1D95).copy(alpha = 0.85f)
                LocationCategory.DINING -> Color(0xFF854D0E).copy(alpha = 0.85f)
                LocationCategory.SPORTS -> Color(0xFF065F46).copy(alpha = 0.85f)
                else -> Color(0xFF1E293B).copy(alpha = 0.85f)
            }
        } else {
            when (building.category) {
                LocationCategory.ACADEMIC -> Color(0xFFE2E8F0)
                LocationCategory.HOSTEL -> Color(0xFFEDE9FE)
                LocationCategory.DINING -> Color(0xFFFEF08A)
                else -> Color(0xFFF1F5F9)
            }
        }

        val strokeColor = if (isSatelliteOrDark) Color(0xFF94A3B8) else Color(0xFF64748B)

        drawScope.drawPath(path = path, color = fillColor, style = Fill)
        drawScope.drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2f * scale, join = StrokeJoin.Round)
        )
    }
}
