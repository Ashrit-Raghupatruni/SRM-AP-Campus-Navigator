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

    // Simple 1D Kalman estimates for latitude & longitude
    private var latEstimate: Double? = null
    private var lonEstimate: Double? = null
    private var varianceEstimate: Double = 1.0

    // Process noise variance for human pedestrian movement (~1.5 m/s)
    private val processNoise = 0.000005

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

        // 5. Adaptive Kalman Smoothing based on reported accuracy
        val measurementVariance = (rawState.accuracyMeters / 111320.0).let { it * it }.coerceAtLeast(1e-10)

        val smoothedPoint = if (latEstimate == null || lonEstimate == null) {
            latEstimate = pt.latitude
            lonEstimate = pt.longitude
            varianceEstimate = measurementVariance
            pt
        } else {
            // Predict
            val pPred = varianceEstimate + processNoise

            // Update
            val kalmanGain = pPred / (pPred + measurementVariance)
            latEstimate = latEstimate!! + kalmanGain * (pt.latitude - latEstimate!!)
            lonEstimate = lonEstimate!! + kalmanGain * (pt.longitude - lonEstimate!!)
            varianceEstimate = (1.0 - kalmanGain) * pPred

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
        varianceEstimate = 1.0
    }
}
