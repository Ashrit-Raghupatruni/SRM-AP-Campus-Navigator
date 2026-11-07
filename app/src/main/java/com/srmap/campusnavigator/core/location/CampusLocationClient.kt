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
    val provider: String = "gps"
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
                trySend(UserLocationState(GeoPoint(best.latitude, best.longitude), best.accuracy, best.provider ?: "cache"))
            }
        } catch (e: Exception) {
            Log.w("LocationClient", "Error reading last location: ${e.message}")
        }

        // Fused Location Provider listener
        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    Log.d("LocationClient", "Fused fix: ${loc.latitude}, ${loc.longitude} (acc=${loc.accuracy})")
                    trySend(UserLocationState(GeoPoint(loc.latitude, loc.longitude), loc.accuracy, "fused"))
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("LocationClient", "SecurityException requesting fused updates", e)
        }

        // Hardware GPS & Network fallback listener
        val nativeListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                Log.d("LocationClient", "Native fix: ${loc.latitude}, ${loc.longitude} (acc=${loc.accuracy})")
                trySend(UserLocationState(GeoPoint(loc.latitude, loc.longitude), loc.accuracy, loc.provider ?: "gps"))
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
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, intervalMs, 0f, nativeListener, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            Log.e("LocationClient", "SecurityException requesting native location", e)
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
