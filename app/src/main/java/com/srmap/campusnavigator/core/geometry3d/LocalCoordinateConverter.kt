package com.srmap.campusnavigator.core.geometry3d

import com.srmap.campusnavigator.domain.models.GeoPoint
import kotlin.math.cos

/**
 * Local Tangent-Plane (ENU - East, North, Up) Coordinate Converter for SRM AP Campus.
 *
 * Converts WGS-84 geographic coordinates (lat, lon, altitude) to an orthogonal
 * metric Cartesian coordinate system with millimeter accuracy across the campus.
 *
 * Coordinate convention:
 * - X: Easting in meters (+X is East)
 * - Y: Elevation in meters (+Y is Up, ground plane is Y = 0)
 * - Z: Northing/Southing in meters (+Z is South, -Z is North to match OpenGL forward -Z convention)
 */
object LocalCoordinateConverter {

    // Campus reference origin (SRM AP Core Centroid)
    const val ORIGIN_LAT = 16.4632311
    const val ORIGIN_LON = 80.5064108

    // WGS-84 ellipsoid constants
    private const val EARTH_EQUATORIAL_RADIUS_A = 6378137.0 // semi-major axis in meters
    private const val EARTH_FLATTENING_F = 1.0 / 298.257223563
    private const val EARTH_ECCENTRICITY_SQ_E2 = 2.0 * EARTH_FLATTENING_F - EARTH_FLATTENING_F * EARTH_FLATTENING_F

    private val originLatRad = Math.toRadians(ORIGIN_LAT)
    private val sinLat = kotlin.math.sin(originLatRad)

    // Meridional radius of curvature (M) for Latitude
    private val radiusOfCurvatureM = EARTH_EQUATORIAL_RADIUS_A * (1.0 - EARTH_ECCENTRICITY_SQ_E2) /
            Math.pow(1.0 - EARTH_ECCENTRICITY_SQ_E2 * sinLat * sinLat, 1.5)

    // Prime vertical radius of curvature (N) for Longitude
    private val radiusOfCurvatureN = EARTH_EQUATORIAL_RADIUS_A /
            kotlin.math.sqrt(1.0 - EARTH_ECCENTRICITY_SQ_E2 * sinLat * sinLat)

    // Meters per degree latitude and longitude at campus origin
    val METERS_PER_DEG_LAT: Double = (Math.PI / 180.0) * radiusOfCurvatureM
    val METERS_PER_DEG_LON: Double = (Math.PI / 180.0) * radiusOfCurvatureN * cos(originLatRad)

    /**
     * Converts GeoPoint to 3D Local Metric coordinates.
     * In OpenGL camera space, facing North corresponds to -Z, facing South corresponds to +Z.
     */
    fun toLocal3D(geoPoint: GeoPoint, altitude: Float = 0f): Vector3 {
        val deltaLon = geoPoint.longitude - ORIGIN_LON
        val deltaLat = geoPoint.latitude - ORIGIN_LAT

        val x = (deltaLon * METERS_PER_DEG_LON).toFloat()
        val y = altitude
        val z = (-deltaLat * METERS_PER_DEG_LAT).toFloat() // North is -Z, South is +Z

        return Vector3(x, y, z)
    }

    /**
     * Converts 3D Local Metric coordinates back to geographic GeoPoint.
     */
    fun toGeoPoint(localPos: Vector3): GeoPoint {
        val deltaLon = (localPos.x.toDouble()) / METERS_PER_DEG_LON
        val deltaLat = -(localPos.z.toDouble()) / METERS_PER_DEG_LAT

        return GeoPoint(
            latitude = ORIGIN_LAT + deltaLat,
            longitude = ORIGIN_LON + deltaLon
        )
    }

    /**
     * Computes metric Euclidean distance between two 3D points.
     */
    fun distance(p1: Vector3, p2: Vector3): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dz = p1.z - p2.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(v: Vector3) = Vector3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)

    fun length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0.00001f) Vector3(x / len, y / len, z / len) else Vector3(0f, 0f, 0f)
    }

    fun cross(v: Vector3) = Vector3(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x
    )

    fun dot(v: Vector3) = x * v.x + y * v.y + z * v.z
}
