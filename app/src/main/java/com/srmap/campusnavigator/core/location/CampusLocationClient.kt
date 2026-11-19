package com.srmap.campusnavigator.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.srmap.campusnavigator.domain.models.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class UserLocationState(
    val point: GeoPoint,
    val accuracyMeters: Float = 10f,
    val provider: String = "gps",
    val bearingDeg: Float? = null,
    val speedMps: Float? = null
)

class CampusLocationClient(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 1500L): Flow<UserLocationState> = callbackFlow {
        if (!isLocationEnabled()) {
            Log.d("LocationClient", "Location is turned off on device")
        }
        // Query last known location immediately
        try {
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = when {
                lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                else -> lastNet
            }
            if (best != null) {
                Log.d("LocationClient", "Initial location: ${best.latitude}, ${best.longitude} (acc=${best.accuracy})")
                trySend(
                    UserLocationState(
                        point = GeoPoint(best.latitude, best.longitude),
                        accuracyMeters = best.accuracy,
                        provider = best.provider ?: "cache",
                        bearingDeg = if (best.hasBearing()) best.bearing else null,
                        speedMps = if (best.hasSpeed()) best.speed else null
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("LocationClient", "Error reading last location: ${e.message}")
        }

        var fusedActive = false

        // Primary: Fused Location Provider listener
        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    fusedActive = true
                    Log.d("LocationClient", "Fused fix: ${loc.latitude}, ${loc.longitude} (acc=${loc.accuracy})")
                    trySend(
                        UserLocationState(
                            point = GeoPoint(loc.latitude, loc.longitude),
                            accuracyMeters = loc.accuracy,
                            provider = "fused",
                            bearingDeg = if (loc.hasBearing()) loc.bearing else null,
                            speedMps = if (loc.hasSpeed()) loc.speed else null
                        )
                    )
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("LocationClient", "Exception requesting fused updates", e)
        }

        // Controlled fallback: Only use native GPS if fused location is unavailable
        val nativeListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (!fusedActive) {
                    Log.d("LocationClient", "Native fallback fix: ${loc.latitude}, ${loc.longitude} (acc=${loc.accuracy})")
                    trySend(
                        UserLocationState(
                            point = GeoPoint(loc.latitude, loc.longitude),
                            accuracyMeters = loc.accuracy,
                            provider = loc.provider ?: "gps",
                            bearingDeg = if (loc.hasBearing()) loc.bearing else null,
                            speedMps = if (loc.hasSpeed()) loc.speed else null
                        )
                    )
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalMs, 0f, nativeListener, Looper.getMainLooper())
            }
        } catch (e: Exception) {
            Log.e("LocationClient", "Exception requesting native location fallback", e)
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(fusedCallback)
                locationManager.removeUpdates(nativeListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
