package com.srmap.campusnavigator.navigation3d

import com.srmap.campusnavigator.core.geometry3d.LocalCoordinateConverter
import com.srmap.campusnavigator.core.geometry3d.Vector3
import com.srmap.campusnavigator.core.location.GpsFilterEngine
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import com.srmap.campusnavigator.domain.models.RouteStep
import com.srmap.campusnavigator.domain.navigation3d.FirstPersonCameraController
import org.junit.Assert.*
import org.junit.Test

class Campus3DNavigationSyncTest {

    @Test
    fun testCameraTurnAtRouteJunction() {
        val controller = FirstPersonCameraController()

        // Waypoints: Step 1 (North), Step 2 (East)
        val p1 = GeoPoint(16.46300, 80.5064108)
        val p2 = GeoPoint(16.46350, 80.5064108) // North
        val p3 = GeoPoint(16.46350, 80.5070000) // East

        val route = PedestrianRoute(
            path = listOf(p1, p2, p3),
            totalDistanceMeters = 100.0,
            estimatedTimeSeconds = 70,
            steps = listOf(
                RouteStep("Walk North", 50.0, p1, p2),
                RouteStep("Turn East", 50.0, p2, p3)
            )
        )

        // 1. Moving along segment 0
        controller.updateTarget(
            userLocation = UserLocationState(p1, 5f, "test"),
            route = route,
            routeSegmentIndex = 0
        )
        val poseSeg0 = controller.tick()
        // Facing North in ENU (0 deg)
        assertEquals(0f, poseSeg0.yawDegrees, 2.0f)

        // 2. Advancing to segment 1 (Turn East ~90 deg)
        controller.updateTarget(
            userLocation = UserLocationState(p2, 5f, "test"),
            route = route,
            routeSegmentIndex = 1
        )
        // Tick multiple frames to let shortest-path lerp converge
        for (i in 0 until 40) {
            controller.tick()
        }
        val poseSeg1 = controller.tick()
        assertEquals("Camera yaw should converge toward East (90 deg)", 90f, poseSeg1.yawDegrees, 5.0f)
    }

    @Test
    fun testGpsFilterEngineCosineScaling() {
        val filter = GpsFilterEngine()
        val loc1 = UserLocationState(GeoPoint(16.46300, 80.50640), 6f, "gps")
        val smoothed = filter.filterAndSmooth(loc1)

        assertNotNull(smoothed)
        assertEquals(16.46300, smoothed!!.point.latitude, 0.0001)
        assertEquals(80.50640, smoothed.point.longitude, 0.0001)
    }
}
