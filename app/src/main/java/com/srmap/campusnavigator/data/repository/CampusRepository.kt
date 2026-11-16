package com.srmap.campusnavigator.data.repository

import com.srmap.campusnavigator.data.datasource.StaticCampusDataSource
import com.srmap.campusnavigator.data.local.dao.CampusDao
import com.srmap.campusnavigator.data.local.entities.BlockEntity
import com.srmap.campusnavigator.data.local.entities.EquipmentEntity
import com.srmap.campusnavigator.data.local.entities.FacultyEntity
import com.srmap.campusnavigator.data.local.entities.RoomEntity
import com.srmap.campusnavigator.domain.models.CampusBuilding
import kotlinx.coroutines.flow.Flow

interface CampusRepository {
    fun getBuildings(): Flow<List<CampusBuilding>>
    suspend fun getAllBlocks(): List<BlockEntity>
    suspend fun getRoomsForBlock(blockId: String): List<RoomEntity>
    suspend fun getRoomsForBlockAndFloor(blockId: String, floor: Int): List<RoomEntity>
    suspend fun getFacultyForBlock(blockId: String): List<FacultyEntity>
    suspend fun getEquipmentForBlock(blockId: String): List<EquipmentEntity>
    suspend fun getAllRooms(): List<RoomEntity>
    suspend fun getAllFaculty(): List<FacultyEntity>
    suspend fun getAllEquipment(): List<EquipmentEntity>
    fun getFavoriteIds(): Flow<List<String>>
    suspend fun toggleFavorite(buildingId: String, isFavorite: Boolean)
}

class CampusRepositoryImpl(
    private val dao: CampusDao,
    private val staticSource: StaticCampusDataSource = StaticCampusDataSource
) : CampusRepository {

    override fun getBuildings(): Flow<List<CampusBuilding>> = staticSource.getBuildings()

    override suspend fun getAllBlocks(): List<BlockEntity> = staticSource.getBlocks()

    override suspend fun getRoomsForBlock(blockId: String): List<RoomEntity> {
        val staticRes = staticSource.getRoomsByBlock(blockId)
        return if (staticRes.isNotEmpty()) staticRes else dao.getRoomsForBlock(blockId)
    }

    override suspend fun getRoomsForBlockAndFloor(blockId: String, floor: Int): List<RoomEntity> {
        val staticRes = staticSource.getRoomsByBlockAndFloor(blockId, floor)
        return if (staticRes.isNotEmpty()) staticRes else dao.getRoomsByBlockAndFloor(blockId, floor)
    }

    override suspend fun getFacultyForBlock(blockId: String): List<FacultyEntity> {
        val staticRes = staticSource.getFacultyByBlock(blockId)
        return if (staticRes.isNotEmpty()) staticRes else dao.getFacultyForBlock(blockId)
    }

    override suspend fun getEquipmentForBlock(blockId: String): List<EquipmentEntity> {
        val staticRes = staticSource.getEquipmentByBlock(blockId)
        return if (staticRes.isNotEmpty()) staticRes else dao.getEquipmentForBlock(blockId)
    }

    override suspend fun getAllRooms(): List<RoomEntity> {
        val staticRooms = staticSource.getRooms()
        return if (staticRooms.isNotEmpty()) staticRooms else dao.searchCampusRoomsUnified("").map {
            RoomEntity(
                roomId = it.roomId,
                blockId = it.blockId,
                roomNo = it.roomNo,
                normalizedRoomNo = it.normalizedRoomNo,
                floor = it.floor,
                roomType = it.roomType,
                latitude = it.latitude,
                longitude = it.longitude,
                locationStatus = it.locationStatus,
                sourceDataset = null,
                description = it.description,
                capacity = it.capacity
            )
        }
    }

    override suspend fun getAllFaculty(): List<FacultyEntity> {
        val staticFaculty = staticSource.getFaculty()
        return if (staticFaculty.isNotEmpty()) staticFaculty else dao.getAllFaculty()
    }

    override suspend fun getAllEquipment(): List<EquipmentEntity> {
        val staticEq = staticSource.getEquipment()
        return if (staticEq.isNotEmpty()) staticEq else dao.getAllEquipment()
    }

    override fun getFavoriteIds(): Flow<List<String>> = kotlinx.coroutines.flow.flow {
        dao.getFavoriteLocations().collect { list ->
            emit(list.map { it.id })
        }
    }

    override suspend fun toggleFavorite(buildingId: String, isFavorite: Boolean) {
        dao.updateFavorite(buildingId, isFavorite)
    }
}
