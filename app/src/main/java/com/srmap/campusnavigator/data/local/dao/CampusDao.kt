package com.srmap.campusnavigator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.srmap.campusnavigator.data.local.entities.*
import kotlinx.coroutines.flow.Flow

data class RoomWithDetails(
    val roomId: String,
    val blockId: String,
    val roomNo: String,
    val normalizedRoomNo: String,
    val floor: Int,
    val roomType: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationStatus: String,
    val description: String?,
    val capacity: Int?,
    val facultyName: String?,
    val facultyDesignation: String?,
    val facultyDepartment: String?,
    val equipmentList: String?
)

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

    // --- Blocks, Rooms, Faculty & Equipment Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<BlockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaculty(facultyList: List<FacultyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipmentList: List<EquipmentEntity>)

    @Query("SELECT * FROM rooms WHERE blockId = :blockId AND floor = :floor")
    suspend fun getRoomsByBlockAndFloor(blockId: String, floor: Int): List<RoomEntity>

    @Query("SELECT * FROM rooms WHERE blockId = :blockId")
    suspend fun getRoomsByBlock(blockId: String): List<RoomEntity>

    @Query("""
        SELECT r.roomId, r.blockId, r.roomNo, r.normalizedRoomNo, r.floor, r.roomType,
               r.latitude, r.longitude, r.locationStatus, r.description, r.capacity,
               f.name AS facultyName, f.designation AS facultyDesignation, f.department AS facultyDepartment,
               (SELECT GROUP_CONCAT(e.name, ', ') FROM equipment e WHERE e.roomId = r.roomId) AS equipmentList
        FROM rooms r
        LEFT JOIN faculty f ON r.roomId = f.roomId
        WHERE r.normalizedRoomNo LIKE '%' || :query || '%' 
           OR r.roomNo LIKE '%' || :query || '%'
           OR r.roomType LIKE '%' || :query || '%'
           OR (f.name IS NOT NULL AND f.name LIKE '%' || :query || '%')
           OR (f.department IS NOT NULL AND f.department LIKE '%' || :query || '%')
           OR r.roomId IN (SELECT e.roomId FROM equipment e WHERE e.name LIKE '%' || :query || '%')
        ORDER BY 
            CASE 
                WHEN r.normalizedRoomNo = :query THEN 1
                WHEN f.name LIKE :query || '%' THEN 2
                ELSE 3
            END
        LIMIT 25
    """)
    suspend fun searchCampusRoomsUnified(query: String): List<RoomWithDetails>

    @Query("SELECT COUNT(*) FROM rooms")
    suspend fun getRoomCount(): Int

    @Query("SELECT * FROM rooms WHERE blockId = :blockId ORDER BY floor ASC, roomNo ASC")
    suspend fun getRoomsForBlock(blockId: String): List<RoomEntity>

    @Query("""
        SELECT f.* FROM faculty f 
        INNER JOIN rooms r ON f.roomId = r.roomId 
        WHERE r.blockId = :blockId 
        ORDER BY f.name ASC
    """)
    suspend fun getFacultyForBlock(blockId: String): List<FacultyEntity>

    @Query("""
        SELECT e.* FROM equipment e 
        INNER JOIN rooms r ON e.roomId = r.roomId 
        WHERE r.blockId = :blockId 
        ORDER BY e.name ASC
    """)
    suspend fun getEquipmentForBlock(blockId: String): List<EquipmentEntity>

    @Query("SELECT * FROM faculty ORDER BY name ASC")
    suspend fun getAllFaculty(): List<FacultyEntity>

    @Query("SELECT * FROM equipment ORDER BY name ASC")
    suspend fun getAllEquipment(): List<EquipmentEntity>
}
