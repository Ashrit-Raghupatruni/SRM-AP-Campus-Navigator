package com.srmap.campusnavigator.data.maptiler.geocoding

import android.util.Log
import com.srmap.campusnavigator.BuildConfig
import com.srmap.campusnavigator.domain.models.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class GeocodingResult(
    val id: String,
    val placeName: String,
    val text: String,
    val point: GeoPoint,
    val placeType: List<String>
)

class MapTilerGeocodingClient(
    private val apiKey: String = BuildConfig.MAPTILER_API_KEY
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Bounding proximity bias around SRM University-AP (Amaravati)
    private val proximityLon = 80.5064
    private val proximityLat = 16.4632

    suspend fun forwardGeocode(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        if (query.isBlank() || apiKey.isBlank()) return@withContext emptyList()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api.maptiler.com/geocoding/$encodedQuery.json?key=$apiKey&proximity=$proximityLon,$proximityLat&limit=6"
            val request = Request.Builder().url(url).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(body)
            val features = root.optJSONArray("features") ?: return@withContext emptyList()

            val results = mutableListOf<GeocodingResult>()
            for (i in 0 until features.length()) {
                val f = features.getJSONObject(i)
                val id = f.optString("id", "geo_$i")
                val placeName = f.optString("place_name", "")
                val text = f.optString("text", "")
                val center = f.optJSONArray("center")
                if (center != null && center.length() >= 2) {
                    val lon = center.getDouble(0)
                    val lat = center.getDouble(1)
                    val typeArray = f.optJSONArray("place_type")
                    val types = mutableListOf<String>()
                    if (typeArray != null) {
                        for (j in 0 until typeArray.length()) {
                            types.add(typeArray.getString(j))
                        }
                    }
                    results.add(
                        GeocodingResult(
                            id = id,
                            placeName = placeName,
                            text = text,
                            point = GeoPoint(lat, lon),
                            placeType = types
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.w("MapTilerGeocoding", "Geocoding error: ${e.message}")
            emptyList()
        }
    }

    suspend fun reverseGeocode(point: GeoPoint): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        try {
            val url = "https://api.maptiler.com/geocoding/${point.longitude},${point.latitude}.json?key=$apiKey&limit=1"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val features = root.optJSONArray("features")
            if (features != null && features.length() > 0) {
                return@withContext features.getJSONObject(0).optString("place_name", null)
            }
            null
        } catch (e: Exception) {
            Log.w("MapTilerGeocoding", "Reverse geocoding error: ${e.message}")
            null
        }
    }
}
