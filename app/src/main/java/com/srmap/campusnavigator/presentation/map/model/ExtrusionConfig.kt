package com.srmap.campusnavigator.presentation.map.model

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.core.designsystem.DeepTeal
import com.srmap.campusnavigator.core.designsystem.MidnightNavy
import com.srmap.campusnavigator.core.designsystem.SrmCrimson

data class ExtrusionConfig(
    val baseHeightPx: Float = 16f,
    val angleDegrees: Float = 315f, // Top-left isometric elevation (up & slightly left)
    val topFaceColor: Color = SrmCrimson,
    val sideFaceLightColor: Color = Color(0xFF781824),
    val sideFaceDarkColor: Color = Color(0xFF520E17),
    val outlineColor: Color = Color.White.copy(alpha = 0.85f),
    val shadowColor: Color = Color(0x38000000),
    val shadowBlurPx: Float = 6f
) {
    companion object {
        fun defaultForCategory(category: com.srmap.campusnavigator.domain.models.LocationCategory): ExtrusionConfig {
            return when (category) {
                com.srmap.campusnavigator.domain.models.LocationCategory.ACADEMIC -> ExtrusionConfig(
                    topFaceColor = SrmCrimson,
                    sideFaceLightColor = Color(0xFF8B1528),
                    sideFaceDarkColor = Color(0xFF5C0E1B)
                )
                com.srmap.campusnavigator.domain.models.LocationCategory.HOSTEL -> ExtrusionConfig(
                    topFaceColor = Color(0xFF7C3AED),
                    sideFaceLightColor = Color(0xFF5B21B6),
                    sideFaceDarkColor = Color(0xFF4C1D95)
                )
                com.srmap.campusnavigator.domain.models.LocationCategory.DINING -> ExtrusionConfig(
                    topFaceColor = Color(0xFFF59E0B),
                    sideFaceLightColor = Color(0xFFD97706),
                    sideFaceDarkColor = Color(0xFFB45309)
                )
                else -> ExtrusionConfig(
                    topFaceColor = DeepTeal,
                    sideFaceLightColor = Color(0xFF0D5D57),
                    sideFaceDarkColor = MidnightNavy
                )
            }
        }
    }
}
