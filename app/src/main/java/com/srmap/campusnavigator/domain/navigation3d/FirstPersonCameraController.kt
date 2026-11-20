package com.srmap.campusnavigator.domain.navigation3d

import com.srmap.campusnavigator.core.geometry3d.LocalCoordinateConverter
import com.srmap.campusnavigator.core.geometry3d.Vector3
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import kotlin.math.cos
import kotlin.math.sin

enum class CameraViewMode {
    FIRST_PERSON,
    THIRD_PERSON
}

data class CameraPose(
    val eyePosition: Vector3,
    val targetPosition: Vector3,
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val walkingHeadingDegrees: Float,
    val viewMode: CameraViewMode,
    val transitionProgress: Float // 0f = full first-person, 1f = full third-person
)

/**
 * Manages dual-mode (First-Person & Third-Person) 3D camera navigation.
 * Incorporates:
 * - Independent camera orbit vs. walking heading.
 * - Smooth camera position/angle transition lerping.
 * - Camera building collision clearance.
 * - Multi-touch gestures (pan, tilt, zoom).
 */
class FirstPersonCameraController {

    companion object {
        const val EYE_HEIGHT_METERS = 1.6f
        const val TARGET_HEIGHT_METERS = 1.0f

        const val DEFAULT_THIRD_PERSON_DISTANCE = 9.0f
        const val MIN_DISTANCE = 3.0f
        const val MAX_DISTANCE = 25.0f

        const val MIN_PITCH_DEG = 10.0f
        const val MAX_PITCH_DEG = 65.0f
        const val DEFAULT_THIRD_PERSON_PITCH = 24.0f
        const val FIRST_PERSON_PITCH = -4.0f

        private const val POSITION_LERP_FACTOR = 0.18f
        private const val HEADING_LERP_FACTOR = 0.14f
        private const val TRANSITION_SPEED = 0.08f
    }

    var viewMode: CameraViewMode = CameraViewMode.FIRST_PERSON
        private set

    // User's physical location in 3D ENU
    private var currentUserGroundPos: Vector3 = Vector3(0f, 0f, 0f)
    private var targetUserGroundPos: Vector3 = Vector3(0f, 0f, 0f)

    // Heading of walking direction
    private var currentWalkingHeading: Float = 0f
    private var targetWalkingHeading: Float = 0f

    // Orbital adjustments in 3rd-person
    private var orbitYawOffset: Float = 0f
    private var orbitPitch: Float = DEFAULT_THIRD_PERSON_PITCH
    private var targetDistance: Float = DEFAULT_THIRD_PERSON_DISTANCE
    private var currentDistance: Float = 0f // 0m in 1st person, expands in 3rd person

    // Transition progress: 0f = 1st person, 1f = 3rd person
    private var transitionProgress: Float = 0f

    // 1st-person look-around offset
    private var firstPersonLookYaw: Float = 0f
    private var firstPersonLookPitch: Float = FIRST_PERSON_PITCH

    private var lastManualInteractionTime = 0L

    fun setMode(mode: CameraViewMode) {
        if (this.viewMode != mode) {
            this.viewMode = mode
            lastManualInteractionTime = System.currentTimeMillis()
        }
    }

    fun updateTarget(
        userLocation: UserLocationState?,
        route: PedestrianRoute?,
        routeSegmentIndex: Int = 0,
        mapMatchedBearing: Float? = null
    ) {
        val userGeo = userLocation?.point ?: route?.path?.firstOrNull() ?: return
        val local3D = LocalCoordinateConverter.toLocal3D(userGeo, altitude = 0f)
        targetUserGroundPos = local3D

        // Determine forward walking heading (independent from camera orbit)
        if (mapMatchedBearing != null) {
            targetWalkingHeading = mapMatchedBearing
        } else if (route != null && route.path.size > routeSegmentIndex + 1) {
            val p1 = route.path[routeSegmentIndex]
            val p2 = route.path[routeSegmentIndex + 1]
            val v1 = LocalCoordinateConverter.toLocal3D(p1)
            val v2 = LocalCoordinateConverter.toLocal3D(p2)
            val dx = v2.x - v1.x
            val dz = v2.z - v1.z
            val angleRad = kotlin.math.atan2(dx.toDouble(), -dz.toDouble())
            targetWalkingHeading = ((Math.toDegrees(angleRad).toFloat() % 360f) + 360f) % 360f
        }
    }

    /**
     * One-finger drag:
     * In 3rd-person: orbits yaw (horizontal) and tilts pitch (vertical).
     * In 1st-person: looks around.
     */
    fun onUserDrag(deltaX: Float, deltaY: Float) {
        lastManualInteractionTime = System.currentTimeMillis()
        if (viewMode == CameraViewMode.THIRD_PERSON) {
            orbitYawOffset += deltaX * 0.35f
            orbitPitch = (orbitPitch - deltaY * 0.25f).coerceIn(MIN_PITCH_DEG, MAX_PITCH_DEG)
        } else {
            firstPersonLookYaw += deltaX * 0.25f
            firstPersonLookPitch = (firstPersonLookPitch - deltaY * 0.15f).coerceIn(-30f, 30f)
        }
    }

    /**
     * Two-finger pinch zoom (3rd person distance adjustment)
     */
    fun onUserZoom(zoomFactor: Float) {
        lastManualInteractionTime = System.currentTimeMillis()
        if (viewMode == CameraViewMode.THIRD_PERSON) {
            targetDistance = (targetDistance / zoomFactor).coerceIn(MIN_DISTANCE, MAX_DISTANCE)
        }
    }

    fun applyViewpoint(viewpoint: com.srmap.campusnavigator.domain.building3d.BuildingViewpoint) {
        viewMode = CameraViewMode.THIRD_PERSON
        targetUserGroundPos = Vector3(viewpoint.targetPosition.x, 0f, viewpoint.targetPosition.z)
        currentUserGroundPos = targetUserGroundPos
        targetWalkingHeading = viewpoint.yawDegrees
        currentWalkingHeading = viewpoint.yawDegrees
        orbitYawOffset = 0f
        orbitPitch = viewpoint.pitchDegrees
        val dx = viewpoint.eyePosition.x - viewpoint.targetPosition.x
        val dy = viewpoint.eyePosition.y - viewpoint.targetPosition.y
        val dz = viewpoint.eyePosition.z - viewpoint.targetPosition.z
        targetDistance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz).coerceIn(MIN_DISTANCE, 120.0f)
        currentDistance = targetDistance
        lastManualInteractionTime = System.currentTimeMillis()
    }

    fun resetOrientation() {
        orbitYawOffset = 0f
        orbitPitch = DEFAULT_THIRD_PERSON_PITCH
        targetDistance = DEFAULT_THIRD_PERSON_DISTANCE
        firstPersonLookYaw = 0f
        firstPersonLookPitch = FIRST_PERSON_PITCH
        lastManualInteractionTime = System.currentTimeMillis()
    }

    fun tick(buildings: List<CampusBuilding> = emptyList()): CameraPose {
        // Auto-realign after 4.5 seconds of inactivity in 1st person
        if (viewMode == CameraViewMode.FIRST_PERSON && firstPersonLookYaw != 0f) {
            if (System.currentTimeMillis() - lastManualInteractionTime > 4500) {
                firstPersonLookYaw *= 0.92f
                if (kotlin.math.abs(firstPersonLookYaw) < 0.1f) firstPersonLookYaw = 0f
            }
        }

        // Smooth position lerp
        currentUserGroundPos = Vector3(
            lerp(currentUserGroundPos.x, targetUserGroundPos.x, POSITION_LERP_FACTOR),
            0f,
            lerp(currentUserGroundPos.z, targetUserGroundPos.z, POSITION_LERP_FACTOR)
        )

        // Smooth walking heading lerp
        currentWalkingHeading = lerpAngle(currentWalkingHeading, targetWalkingHeading, HEADING_LERP_FACTOR)

        // Transition progress lerp
        val targetProgress = if (viewMode == CameraViewMode.THIRD_PERSON) 1.0f else 0.0f
        transitionProgress = lerp(transitionProgress, targetProgress, TRANSITION_SPEED)

        // Distance lerp (0m in 1st person, targetDistance in 3rd person)
        val desiredDist = targetDistance * transitionProgress
        currentDistance = lerp(currentDistance, desiredDist, 0.2f)

        // Target look-at point: Interpolates between eye level (1.6m) and avatar center (1.0m)
        val targetY = lerp(EYE_HEIGHT_METERS, TARGET_HEIGHT_METERS, transitionProgress)
        val targetPoint = Vector3(currentUserGroundPos.x, targetY, currentUserGroundPos.z)

        // Active yaw:
        // In 3rd person: currentWalkingHeading + orbitYawOffset
        // In 1st person: currentWalkingHeading + firstPersonLookYaw
        val activeYaw = if (transitionProgress > 0.5f) {
            (currentWalkingHeading + orbitYawOffset + 360f) % 360f
        } else {
            (currentWalkingHeading + firstPersonLookYaw + 360f) % 360f
        }

        // Active pitch:
        val activePitch = lerp(firstPersonLookPitch, orbitPitch, transitionProgress)

        // Compute 3rd-person eye point using positive pitch convention
        val yawRad = Math.toRadians(activeYaw.toDouble())
        val pitchRad = Math.toRadians(activePitch.toDouble())

        var effectiveDist = currentDistance

        // Building collision clamping for 3rd-person camera
        if (effectiveDist > 1.0f && buildings.isNotEmpty()) {
            effectiveDist = clampDistanceForBuildingCollisions(targetPoint, yawRad, pitchRad, effectiveDist, buildings)
        }

        val eyeX = (targetPoint.x - effectiveDist * sin(yawRad) * cos(pitchRad)).toFloat()
        val eyeY = (targetPoint.y + effectiveDist * sin(pitchRad)).toFloat()
        val eyeZ = (targetPoint.z + effectiveDist * cos(yawRad) * cos(pitchRad)).toFloat()

        return CameraPose(
            eyePosition = Vector3(eyeX, eyeY, eyeZ),
            targetPosition = targetPoint,
            yawDegrees = activeYaw,
            pitchDegrees = activePitch,
            walkingHeadingDegrees = currentWalkingHeading,
            viewMode = viewMode,
            transitionProgress = transitionProgress
        )
    }

    /**
     * Prevents the 3rd person camera from passing through building walls or roofs (SpringArm3D).
     * Tests ray from character target point toward desired eye position against 3D building bounds.
     */
    private fun clampDistanceForBuildingCollisions(
        target: Vector3,
        yawRad: Double,
        pitchRad: Double,
        maxDist: Float,
        buildings: List<CampusBuilding>
    ): Float {
        var clampedDist = maxDist
        val eyeDirX = (-sin(yawRad) * cos(pitchRad)).toFloat()
        val eyeDirY = (sin(pitchRad)).toFloat()
        val eyeDirZ = (cos(yawRad) * cos(pitchRad)).toFloat()

        for (bldg in buildings) {
            val poly = bldg.polygon
            if (poly.size < 3) continue
            val bldgCentroid = LocalCoordinateConverter.toLocal3D(bldg.centroid)
            // Fast 3D sphere check (60m radius)
            val dToCentroidSq = (target.x - bldgCentroid.x) * (target.x - bldgCentroid.x) + (target.z - bldgCentroid.z) * (target.z - bldgCentroid.z)
            if (dToCentroidSq > 3600f) continue // > 60m away, skip detailed test

            val parsedLevels = bldg.levels?.toFloatOrNull() ?: 2.0f
            val bldgHeight = (parsedLevels * 3.0f).coerceAtLeast(6.0f)
            val localPoly = poly.map { LocalCoordinateConverter.toLocal3D(it) }

            // 1. Check vertical wall faces
            for (i in localPoly.indices) {
                val p1 = localPoly[i]
                val p2 = localPoly[(i + 1) % localPoly.size]
                val hitDist = raySegmentIntersection(target.x, target.z, eyeDirX, eyeDirZ, p1.x, p1.z, p2.x, p2.z)
                if (hitDist != null && hitDist > 0.4f && hitDist < clampedDist) {
                    val hitY = target.y + hitDist * eyeDirY
                    // Only clamp if the ray hit occurs below the building's roof height
                    if (hitY in 0f..bldgHeight) {
                        clampedDist = (hitDist - 0.7f).coerceAtLeast(MIN_DISTANCE)
                    }
                }
            }

            // 2. Check roof plane collision if camera is elevated above roof
            if (eyeDirY > 0.05f) {
                val tRoof = (bldgHeight - target.y) / eyeDirY
                if (tRoof > 0.4f && tRoof < clampedDist) {
                    val roofX = target.x + tRoof * eyeDirX
                    val roofZ = target.z + tRoof * eyeDirZ
                    if (isPointInsidePolygon(roofX, roofZ, localPoly)) {
                        clampedDist = (tRoof - 0.7f).coerceAtLeast(MIN_DISTANCE)
                    }
                }
            }
        }
        return clampedDist
    }

    private fun isPointInsidePolygon(x: Float, z: Float, poly: List<Vector3>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val pi = poly[i]
            val pj = poly[j]
            if ((pi.z > z) != (pj.z > z) &&
                (x < (pj.x - pi.x) * (z - pi.z) / (pj.z - pi.z + 1e-6f) + pi.x)) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun raySegmentIntersection(
        rx: Float, rz: Float, dx: Float, dz: Float,
        x1: Float, z1: Float, x2: Float, z2: Float
    ): Float? {
        val sx = x2 - x1
        val sz = z2 - z1
        val det = dx * (-sz) - dz * (-sx)
        if (kotlin.math.abs(det) < 0.0001f) return null

        val t = ((x1 - rx) * (-sz) - (z1 - rz) * (-sx)) / det
        val u = (dx * (z1 - rz) - dz * (x1 - rx)) / det

        return if (t > 0f && u in 0f..1f) t else null
    }

    private fun lerp(start: Float, end: Float, factor: Float): Float {
        return start + factor * (end - start)
    }

    private fun lerpAngle(start: Float, end: Float, factor: Float): Float {
        var diff = (end - start) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return (start + factor * diff + 360f) % 360f
    }
}
