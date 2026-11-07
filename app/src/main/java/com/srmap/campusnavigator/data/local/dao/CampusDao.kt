package com.srmap.campusnavigator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.srmap.campusnavigator.data.local.entities.LocationEntity
import com.srmap.campusnavigator.data.local.entities.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampusDao {

    @Query("SELECT * FROM campus_locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM campus_locations WHERE isFavorite = 1")
    fun getFavoriteLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM campus_locations WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun searchLocations(query: String): List<LocationEntity>

    @Query("SELECT * FROM campus_locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: String): LocationEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Query("UPDATE campus_locations SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query("SELECT DISTINCT query FROM recent_searches ORDER BY timestamp DESC LIMIT 5")
    suspend fun getRecentSearches(): List<String>

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}
