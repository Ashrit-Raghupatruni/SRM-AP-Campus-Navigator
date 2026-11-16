package com.srmap.campusnavigator.presentation.map.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.srmap.campusnavigator.presentation.map.model.ExtrudedBuildingFaces
import com.srmap.campusnavigator.presentation.map.model.ExtrusionConfig
import com.srmap.campusnavigator.presentation.map.model.SideFaceQuad
import kotlin.math.*

object BuildingGeometryUtils {

    /**
     * Calculates the offset vector for isometric extrusion
     */
    fun calculateExtrusionOffset(depthPx: Float, angleDegrees: Float): Offset {
        val rad = Math.toRadians(angleDegrees.toDouble())
        val dx = (depthPx * cos(rad)).toFloat()
        val dy = (depthPx * sin(rad)).toFloat()
        return Offset(dx, dy)
    }

    /**
     * Extrudes a 2D projected polygon into 3D isometric faces (shadow, side quads, raised top)
     */
    fun createExtrudedBuildingFaces(
        baseVertices: List<Offset>,
        config: ExtrusionConfig,
        progress: Float
    ): ExtrudedBuildingFaces? {
        if (baseVertices.size < 3) return null

        val currentHeight = config.baseHeightPx * progress
        val offset = calculateExtrusionOffset(currentHeight, config.angleDegrees)

        // 1. Raised Top Face
        val topVertices = baseVertices.map { Offset(it.x + offset.x, it.y + offset.y) }
        val topPath = createPolygonPath(topVertices)
        val topOutline = createPolygonPath(topVertices)

        // 2. Ground Contact Drop Shadow
        val shadowOffset = Offset(-offset.x * 0.4f, abs(offset.y) * 0.35f + 4f * progress)
        val shadowVertices = baseVertices.map { Offset(it.x + shadowOffset.x, it.y + shadowOffset.y) }
        val shadowPath = createPolygonPath(shadowVertices)

        // 3. Side Face Quads (Connecting base edge to top edge)
        val n = baseVertices.size
        val sideQuads = mutableListOf<SideFaceQuad>()

        for (i in 0 until n) {
            val nextIdx = (i + 1) % n
            val b1 = baseVertices[i]
            val b2 = baseVertices[nextIdx]
            val t1 = topVertices[i]
            val t2 = topVertices[nextIdx]

            // Calculate face normal in 2D screen space
            val edgeDx = b2.x - b1.x
            val edgeDy = b2.y - b1.y
            // Outward normal (-dy, dx)
            val nx = -edgeDy
            val ny = edgeDx

            // Only draw faces visible in current projection (facing toward viewer / light)
            // Dot product with viewing direction (-offset.x, -offset.y)
            val dot = nx * (-offset.x) + ny * (-offset.y)

            // Calculate edge angle for directional lighting
            val edgeAngle = Math.toDegrees(atan2(edgeDy.toDouble(), edgeDx.toDouble())).toFloat()
            val normalizedAngle = ((edgeAngle % 360f) + 360f) % 360f

            // Blend side face color between light and dark according to orientation
            val lightFactor = (sin(Math.toRadians(normalizedAngle.toDouble())).toFloat().coerceIn(-1f, 1f) + 1f) / 2f
            val faceColor = lerpColor(config.sideFaceDarkColor, config.sideFaceLightColor, lightFactor)

            val quadPath = Path().apply {
                moveTo(b1.x, b1.y)
                lineTo(b2.x, b2.y)
                lineTo(t2.x, t2.y)
                lineTo(t1.x, t1.y)
                close()
            }

            sideQuads.add(
                SideFaceQuad(
                    path = quadPath,
                    color = faceColor,
                    normalAngleDeg = normalizedAngle
                )
            )
        }

        // Sort side quads by average Y to maintain correct front-to-back painter's order
        return ExtrudedBuildingFaces(
            shadowPath = shadowPath,
            sideFaces = sideQuads,
            topFacePath = topPath,
            topFaceOutlinePath = topOutline
        )
    }

    private fun createPolygonPath(vertices: List<Offset>): Path {
        val path = Path()
        if (vertices.isEmpty()) return path
        path.moveTo(vertices[0].x, vertices[0].y)
        for (i in 1 until vertices.size) {
            path.lineTo(vertices[i].x, vertices[i].y)
        }
        path.close()
        return path
    }

    private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return Color(
            red = c1.red + (c2.red - c1.red) * f,
            green = c1.green + (c2.green - c1.green) * f,
            blue = c1.blue + (c2.blue - c1.blue) * f,
            alpha = c1.alpha + (c2.alpha - c1.alpha) * f
        )
    }
}
