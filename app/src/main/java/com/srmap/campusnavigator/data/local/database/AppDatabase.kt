package com.srmap.campusnavigator.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.srmap.campusnavigator.data.local.dao.CampusDao
import com.srmap.campusnavigator.data.local.entities.*

@Database(
    entities = [
        LocationEntity::class,
        RecentSearchEntity::class,
        BlockEntity::class,
        RoomEntity::class,
        FacultyEntity::class,
        EquipmentEntity::class,
        DepartmentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun campusDao(): CampusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS blocks (
                        blockId TEXT PRIMARY KEY NOT NULL,
                        blockName TEXT NOT NULL,
                        canonicalName TEXT NOT NULL,
                        floorCount INTEGER NOT NULL,
                        description TEXT
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        roomId TEXT PRIMARY KEY NOT NULL,
                        blockId TEXT NOT NULL,
                        roomNo TEXT NOT NULL,
                        normalizedRoomNo TEXT NOT NULL,
                        floor INTEGER NOT NULL,
                        roomType TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        locationStatus TEXT NOT NULL,
                        sourceDataset TEXT,
                        description TEXT,
                        capacity INTEGER,
                        FOREIGN KEY(blockId) REFERENCES blocks(blockId) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_rooms_blockId ON rooms(blockId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_rooms_normalizedRoomNo ON rooms(normalizedRoomNo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_rooms_floor ON rooms(floor)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS faculty (
                        facultyId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        roomId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        normalizedName TEXT NOT NULL,
                        designation TEXT,
                        department TEXT,
                        email TEXT,
                        FOREIGN KEY(roomId) REFERENCES rooms(roomId) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_faculty_roomId ON faculty(roomId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_faculty_normalizedName ON faculty(normalizedName)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS equipment (
                        equipmentId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        roomId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        normalizedName TEXT NOT NULL,
                        category TEXT,
                        specifications TEXT,
                        FOREIGN KEY(roomId) REFERENCES rooms(roomId) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_roomId ON equipment(roomId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_normalizedName ON equipment(normalizedName)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS departments (
                        departmentId TEXT PRIMARY KEY NOT NULL,
                        departmentName TEXT NOT NULL,
                        normalizedName TEXT NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "srmap_campus_nav.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
