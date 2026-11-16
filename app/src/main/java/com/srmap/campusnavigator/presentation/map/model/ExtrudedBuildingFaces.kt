package com.srmap.campusnavigator.presentation.map.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

data class SideFaceQuad(
    val path: Path,
    val color: Color,
    val normalAngleDeg: Float
)

data class ExtrudedBuildingFaces(
    val shadowPath: Path,
    val sideFaces: List<SideFaceQuad>,
    val topFacePath: Path,
    val topFaceOutlinePath: Path
)
