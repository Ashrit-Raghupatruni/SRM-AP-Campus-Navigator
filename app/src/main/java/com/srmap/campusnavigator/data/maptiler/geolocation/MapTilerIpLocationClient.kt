package com.srmap.campusnavigator.data.maptiler.geolocation

import android.util.Log
import com.srmap.campusnavigator.BuildConfig
import com.srmap.campusnavigator.domain.models.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class IpLocationInfo(
    val country: String,
    val city: String,
    val point: GeoPoint
)

class MapTilerIpLocationClient(
    private val apiKey: String = BuildConfig.MAPTILER_API_KEY
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    suspend fun getApproximateLocation(): IpLocationInfo? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        try {
            val url = "https://api.maptiler.com/geolocation/ip.json?key=$apiKey"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val country = root.optString("country", "India")
            val city = root.optString("city", "Amaravati")
            val lat = root.optDouble("latitude", 16.4632)
            val lon = root.optDouble("longitude", 80.5064)

            IpLocationInfo(country, city, GeoPoint(lat, lon))
        } catch (e: Exception) {
            Log.w("MapTilerIpLocation", "IP location error: ${e.message}")
            null
        }
    }
}
