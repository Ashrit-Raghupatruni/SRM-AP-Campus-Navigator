package com.srmap.campusnavigator.domain.navigation3d

import com.srmap.campusnavigator.core.geometry3d.LocalCoordinateConverter
import com.srmap.campusnavigator.core.geometry3d.Vector3
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.PedestrianRoute

data class CameraPose(
    val position: Vector3,
    val yawDegrees: Float,
    val pitchDegrees: Float = -4.0f
)

/**
 * Manages first-person camera positioning, eye-height constraint (1.6m),
 * and heading orientation synchronized with live GPS and pedestrian routes.
 */
class FirstPersonCameraController {

    companion object {
        const val EYE_HEIGHT_METERS = 1.6f
        private const val POSITION_LERP_FACTOR = 0.18f
        private const val HEADING_LERP_FACTOR = 0.12f
    }

    private var currentPosition: Vector3 = Vector3(0f, EYE_HEIGHT_METERS, 0f)
    private var targetPosition: Vector3 = currentPosition
    private var currentYaw: Float = 0f
    private var targetYaw: Float = 0f
    private var userLookOffset: Float = 0f
    private var lastManualInteractionTime = 0L

    fun updateTarget(
        userLocation: UserLocationState?,
        route: PedestrianRoute?,
        routeSegmentIndex: Int = 0,
        mapMatchedBearing: Float? = null
    ) {
        val userGeo = userLocation?.point ?: route?.path?.firstOrNull() ?: return
        val local3D = LocalCoordinateConverter.toLocal3D(userGeo, altitude = EYE_HEIGHT_METERS)
        targetPosition = local3D

        // Determine forward walking heading
        if (mapMatchedBearing != null) {
            targetYaw = mapMatchedBearing
        } else if (route != null && route.path.size > routeSegmentIndex + 1) {
            val p1 = route.path[routeSegmentIndex]
            val p2 = route.path[routeSegmentIndex + 1]
            val v1 = LocalCoordinateConverter.toLocal3D(p1)
            val v2 = LocalCoordinateConverter.toLocal3D(p2)
            val dx = v2.x - v1.x
            val dz = v2.z - v1.z
            // In ENU where +Z is South, -Z is North:
            val angleRad = kotlin.math.atan2(dx.toDouble(), -dz.toDouble())
            targetYaw = ((Math.toDegrees(angleRad).toFloat() % 360f) + 360f) % 360f
        }
    }

    fun onUserDragRotate(deltaAngle: Float) {
        userLookOffset += deltaAngle
        lastManualInteractionTime = System.currentTimeMillis()
    }

    fun resetOrientation() {
        userLookOffset = 0f
    }

    fun tick(): CameraPose {
        // Auto-realign manual look-around back to forward travel heading after 3.5 seconds
        if (userLookOffset != 0f && System.currentTimeMillis() - lastManualInteractionTime > 3500) {
            userLookOffset *= 0.92f
            if (kotlin.math.abs(userLookOffset) < 0.1f) userLookOffset = 0f
        }

        // Smooth position lerp
        currentPosition = Vector3(
            lerp(currentPosition.x, targetPosition.x, POSITION_LERP_FACTOR),
            EYE_HEIGHT_METERS,
            lerp(currentPosition.z, targetPosition.z, POSITION_LERP_FACTOR)
        )

        // Smooth shortest-path angle lerp for Yaw
        currentYaw = lerpAngle(currentYaw, targetYaw, HEADING_LERP_FACTOR)

        val finalYaw = (currentYaw + userLookOffset + 360f) % 360f
        return CameraPose(
            position = currentPosition,
            yawDegrees = finalYaw
        )
    }

    private fun lerp(start: Float, end: Float, factor: Float): Float {
        return start + factor * (end - start)
    }

    private fun lerpAngle(start: Float, end: Float, factor: Float): Float {
        var diff = (end - start) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return (start + diff * factor + 360f) % 360f
    }
}
