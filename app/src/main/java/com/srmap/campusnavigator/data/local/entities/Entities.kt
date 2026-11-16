package com.srmap.campusnavigator.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "campus_locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val levels: String?,
    val isFavorite: Boolean = false
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey val blockId: String,
    val blockName: String,
    val canonicalName: String,
    val floorCount: Int,
    val description: String?
)

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = BlockEntity::class,
            parentColumns = ["blockId"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("blockId"),
        Index("normalizedRoomNo"),
        Index("floor")
    ]
)
data class RoomEntity(
    @PrimaryKey val roomId: String,
    val blockId: String,
    val roomNo: String,
    val normalizedRoomNo: String,
    val floor: Int,
    val roomType: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationStatus: String,
    val sourceDataset: String?,
    val description: String?,
    val capacity: Int?
)

@Entity(
    tableName = "faculty",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["roomId"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("roomId"),
        Index("normalizedName")
    ]
)
data class FacultyEntity(
    @PrimaryKey(autoGenerate = true) val facultyId: Long = 0,
    val roomId: String,
    val name: String,
    val normalizedName: String,
    val designation: String?,
    val department: String?,
    val email: String? = null
)

@Entity(
    tableName = "equipment",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["roomId"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("roomId"),
        Index("normalizedName")
    ]
)
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val equipmentId: Long = 0,
    val roomId: String,
    val name: String,
    val normalizedName: String,
    val category: String?,
    val specifications: String? = null
)

@Entity(tableName = "departments")
data class DepartmentEntity(
    @PrimaryKey val departmentId: String,
    val departmentName: String,
    val normalizedName: String
)
