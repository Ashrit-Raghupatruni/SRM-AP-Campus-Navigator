package com.srmap.campusnavigator.domain.building3d

import com.srmap.campusnavigator.core.geometry3d.Vector3

data class BuildingViewpoint(
    val id: String,
    val name: String,
    val eyePosition: Vector3,
    val targetPosition: Vector3,
    val pitchDegrees: Float,
    val yawDegrees: Float
)
