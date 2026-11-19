package com.srmap.campusnavigator.core.location

import com.srmap.campusnavigator.domain.models.GeoPoint

/**
 * Robust GPS Filter Engine combining outlier rejection and adaptive 1D/2D smoothing.
 * Reduces pedestrian GPS jitter without introducing excessive lag.
 */
class GpsFilterEngine(
    private val maxAcceptableAccuracyMeters: Float = 35.0f,
    private val maxSpeedMps: Float = 6.0f, // Max realistic human running/walking speed
    private val staleThresholdMs: Long = 10_000L
) {
    private var lastValidFix: UserLocationState? = null
    private var lastFixTimestampMs: Long = 0L

    // Independent 1D Kalman estimates and variances for latitude & longitude
    private var latEstimate: Double? = null
    private var lonEstimate: Double? = null
    private var varianceLat: Double = 1.0
    private var varianceLon: Double = 1.0

    // Process noise variance for pedestrian movement (~1.5 m/s) in degrees squared
    private val processNoiseLat = 0.0000000005
    private val processNoiseLon = 0.0000000005

    fun filterAndSmooth(rawState: UserLocationState, timestampMs: Long = System.currentTimeMillis()): UserLocationState? {
        val pt = rawState.point

        // 1. Sanity check: coordinates must be non-zero
        if (pt.latitude == 0.0 && pt.longitude == 0.0) return null

        // 2. Reject fixes with poor accuracy
        if (rawState.accuracyMeters > maxAcceptableAccuracyMeters) {
            return lastValidFix // Return last known good or keep previous
        }

        // 3. Stale check
        if (lastFixTimestampMs > 0 && (timestampMs - lastFixTimestampMs) > staleThresholdMs) {
            // Reset filter on stale gap
            latEstimate = null
            lonEstimate = null
            varianceLat = 1.0
            varianceLon = 1.0
        }

        // 4. Speed anomaly check
        lastValidFix?.let { last ->
            val dist = last.point.distanceTo(pt)
            val elapsedSec = (timestampMs - lastFixTimestampMs).coerceAtLeast(100L) / 1000.0
            val speed = dist / elapsedSec
            if (speed > maxSpeedMps && elapsedSec < 5.0) {
                // Suspicious instantaneous teleport jump -> discard and retain last
                return lastValidFix
            }
        }

        // 5. Adaptive Dual-Covariance Kalman Smoothing based on reported accuracy with longitude scaling at campus latitude
        val latRad = Math.toRadians(pt.latitude)
        val metersPerDegLon = 111320.0 * kotlin.math.cos(latRad)
        val measurementVarianceLat = (rawState.accuracyMeters / 110650.0).let { it * it }.coerceAtLeast(1e-12)
        val measurementVarianceLon = (rawState.accuracyMeters / metersPerDegLon).let { it * it }.coerceAtLeast(1e-12)

        val smoothedPoint = if (latEstimate == null || lonEstimate == null) {
            latEstimate = pt.latitude
            lonEstimate = pt.longitude
            varianceLat = measurementVarianceLat
            varianceLon = measurementVarianceLon
            pt
        } else {
            // Predict step with independent process noises
            val pPredLat = varianceLat + processNoiseLat
            val pPredLon = varianceLon + processNoiseLon

            // Update step with independent Kalman gains
            val kalmanGainLat = pPredLat / (pPredLat + measurementVarianceLat)
            val kalmanGainLon = pPredLon / (pPredLon + measurementVarianceLon)

            latEstimate = latEstimate!! + kalmanGainLat * (pt.latitude - latEstimate!!)
            lonEstimate = lonEstimate!! + kalmanGainLon * (pt.longitude - lonEstimate!!)

            varianceLat = (1.0 - kalmanGainLat) * pPredLat
            varianceLon = (1.0 - kalmanGainLon) * pPredLon

            GeoPoint(latEstimate!!, lonEstimate!!)
        }

        val result = UserLocationState(
            point = smoothedPoint,
            accuracyMeters = rawState.accuracyMeters,
            provider = rawState.provider
        )

        lastValidFix = result
        lastFixTimestampMs = timestampMs
        return result
    }

    fun reset() {
        lastValidFix = null
        lastFixTimestampMs = 0L
        latEstimate = null
        lonEstimate = null
        varianceLat = 1.0
        varianceLon = 1.0
    }
}
