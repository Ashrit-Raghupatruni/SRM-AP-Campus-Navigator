package com.srmap.campusnavigator.presentation.map.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.presentation.map.model.ExtrusionConfig

object SelectedBuilding3DRenderer {

    fun draw3DSelectedBuilding(
        drawScope: DrawScope,
        building: CampusBuilding,
        projectedPoints: List<Offset>,
        scale: Float,
        animationProgress: Float,
        config: ExtrusionConfig = ExtrusionConfig.defaultForCategory(building.category)
    ) {
        val faces = BuildingGeometryUtils.createExtrudedBuildingFaces(
            baseVertices = projectedPoints,
            config = config,
            progress = animationProgress
        ) ?: return

        // 1. Draw Drop Contact Shadow
        if (animationProgress > 0.05f) {
            drawScope.drawPath(
                path = faces.shadowPath,
                color = config.shadowColor.copy(alpha = 0.35f * animationProgress),
                style = Fill
            )
        }

        // 2. Draw Side Face Quads (Wall Facades)
        for (side in faces.sideFaces) {
            drawScope.drawPath(
                path = side.path,
                color = side.color,
                style = Fill
            )
            // Subtle edge separator
            drawScope.drawPath(
                path = side.path,
                color = Color.Black.copy(alpha = 0.2f * animationProgress),
                style = Stroke(width = 1.2f * scale, join = StrokeJoin.Round)
            )
        }

        // 3. Draw Raised Top Roof
        drawScope.drawPath(
            path = faces.topFacePath,
            color = config.topFaceColor,
            style = Fill
        )

        // 4. Draw Crisp Architectural Boundary Outline
        drawScope.drawPath(
            path = faces.topFaceOutlinePath,
            color = config.outlineColor,
            style = Stroke(width = 3.5f * scale, join = StrokeJoin.Round)
        )
    }
}
