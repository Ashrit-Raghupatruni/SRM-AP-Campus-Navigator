package com.srmap.campusnavigator.data.datasource

import com.srmap.campusnavigator.data.local.entities.BlockEntity
import com.srmap.campusnavigator.data.local.entities.EquipmentEntity
import com.srmap.campusnavigator.data.local.entities.FacultyEntity
import com.srmap.campusnavigator.data.local.entities.RoomEntity
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.GeoPoint
import com.srmap.campusnavigator.domain.models.LocationCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface CampusDataSource {
    fun getBuildings(): Flow<List<CampusBuilding>>
    fun getBlocks(): List<BlockEntity>
    fun getRooms(): List<RoomEntity>
    fun getFaculty(): List<FacultyEntity>
    fun getEquipment(): List<EquipmentEntity>
    fun getRoomsByBlock(blockId: String): List<RoomEntity>
    fun getRoomsByBlockAndFloor(blockId: String, floor: Int): List<RoomEntity>
    fun getFacultyByBlock(blockId: String): List<FacultyEntity>
    fun getEquipmentByBlock(blockId: String): List<EquipmentEntity>
}

/**
 * StaticCampusDataSource: Centralized offline demo data provider for SRM AP Campus.
 * Decoupled from raw GIS processing so presentation layers remain purely stable.
 * Swap this with LiveUniversityDataSource in the future without modifying UI.
 */
object StaticCampusDataSource : CampusDataSource {

    private val staticBlocks = listOf(
        BlockEntity(
            blockId = "SR1",
            blockName = "SR1 Academic Block",
            canonicalName = "SR1",
            floorCount = 3,
            description = "Main academic complex housing engineering lecture halls, faculty offices, and academic administration."
        ),
        BlockEntity(
            blockId = "CV",
            blockName = "CV Raman Block",
            canonicalName = "CV",
            floorCount = 10,
            description = "10-storey landmark academic tower featuring high-capacity computing labs, faculty suites, and seminar halls."
        ),
        BlockEntity(
            blockId = "V",
            blockName = "V-Block",
            canonicalName = "V",
            floorCount = 3,
            description = "Dedicated science and quantum research facility with advanced computational physics laboratories."
        ),
        BlockEntity(
            blockId = "X-LAB",
            blockName = "X-Lab Complex",
            canonicalName = "X-LAB",
            floorCount = 3,
            description = "Multi-disciplinary engineering innovation center with fabrication labs, robotics bays, and auditorium AUD-1."
        ),
        BlockEntity(
            blockId = "HOMI",
            blockName = "Homi J. Bhabha Block",
            canonicalName = "HOMI",
            floorCount = 5,
            description = "Advanced research center for interdisciplinary sciences and graduate faculty suites."
        )
    )

    private val staticRooms: List<RoomEntity> = run {
        val list = mutableListOf<RoomEntity>()

        // SR1 Block (Floors 1-3)
        for (flr in 1..3) {
            for (r in 1..6) {
                val roomNo = "S-$flr" + String.format("%02d", r)
                val type = when (r) {
                    1, 2 -> "Lecture Hall"
                    3, 4 -> "Smart Classroom"
                    5 -> "Faculty Cabin"
                    else -> "Tutorial Room"
                }
                list.add(
                    RoomEntity(
                        roomId = "SR1_F${flr}_$roomNo",
                        blockId = "SR1",
                        roomNo = roomNo,
                        normalizedRoomNo = roomNo.replace("-", "").uppercase(),
                        floor = flr,
                        roomType = type,
                        latitude = 16.46305 + (flr * 0.00008) + (r * 0.00004),
                        longitude = 80.50740 + (r * 0.00005),
                        locationStatus = "VERIFIED_DEMO",
                        sourceDataset = "StaticCampusDataSource",
                        description = if (r <= 2) "Equipped with dual projectors and surround audio" else "Standard smart board room",
                        capacity = if (r <= 2) 80 else 50
                    )
                )
            }
        }

        // CV Raman Block (Floors 1-10)
        for (flr in 1..10) {
            for (r in 1..8) {
                val roomNo = "C-$flr" + String.format("%02d", r)
                val type = when {
                    r <= 3 -> "High-Capacity Classroom"
                    r in 4..6 -> "Computing Laboratory"
                    else -> "Faculty Suite"
                }
                list.add(
                    RoomEntity(
                        roomId = "CV_F${flr}_$roomNo",
                        blockId = "CV",
                        roomNo = roomNo,
                        normalizedRoomNo = roomNo.replace("-", "").uppercase(),
                        floor = flr,
                        roomType = type,
                        latitude = 16.46245 + (flr * 0.00004),
                        longitude = 80.50550 + (r * 0.00006),
                        locationStatus = "VERIFIED_DEMO",
                        sourceDataset = "StaticCampusDataSource",
                        description = if (type.contains("Computing")) "Workstation cluster with Gigabit LAN" else "Tiered lecture space",
                        capacity = if (type.contains("Computing")) 60 else 100
                    )
                )
            }
        }

        // V-Block (Floors 1-3)
        for (flr in 1..3) {
            for (r in 1..5) {
                val roomNo = "V-$flr" + String.format("%02d", r)
                val type = if (r == 1) "Quantum Research Lab" else if (r == 2) "Optics & Photonics Lab" else "Faculty Suite"
                list.add(
                    RoomEntity(
                        roomId = "V_F${flr}_$roomNo",
                        blockId = "V",
                        roomNo = roomNo,
                        normalizedRoomNo = roomNo.replace("-", "").uppercase(),
                        floor = flr,
                        roomType = type,
                        latitude = 16.46380 + (flr * 0.00005),
                        longitude = 80.50610 + (r * 0.00004),
                        locationStatus = "VERIFIED_DEMO",
                        sourceDataset = "StaticCampusDataSource",
                        description = "High precision vibration-isolated laboratory space",
                        capacity = 35
                    )
                )
            }
        }

        // X-Lab Complex (Floors 1-3)
        list.add(
            RoomEntity(
                roomId = "X_F1_AUD1",
                blockId = "X-LAB",
                roomNo = "AUD-1",
                normalizedRoomNo = "AUD1",
                floor = 1,
                roomType = "University Auditorium",
                latitude = 16.46465,
                longitude = 80.50715,
                locationStatus = "VERIFIED_DEMO",
                sourceDataset = "StaticCampusDataSource",
                description = "Main University Auditorium for conferences and symposiums",
                capacity = 500
            )
        )
        for (flr in 1..3) {
            for (r in 1..4) {
                val roomNo = "X-$flr" + String.format("%02d", r)
                list.add(
                    RoomEntity(
                        roomId = "X_F${flr}_$roomNo",
                        blockId = "X-LAB",
                        roomNo = roomNo,
                        normalizedRoomNo = roomNo.replace("-", "").uppercase(),
                        floor = flr,
                        roomType = if (r == 1) "Robotics & AI Bay" else "Prototyping Facility",
                        latitude = 16.46480 + (flr * 0.00004),
                        longitude = 80.50730 + (r * 0.00005),
                        locationStatus = "VERIFIED_DEMO",
                        sourceDataset = "StaticCampusDataSource",
                        description = "Industrial fabrication tools & testing equipment",
                        capacity = 40
                    )
                )
            }
        }

        list
    }

    private val staticFaculty = listOf(
        FacultyEntity(facultyId = 1, roomId = "SR1_F2_S-205", name = "Dr. Ramesh Kumar", normalizedName = "DR RAMESH KUMAR", designation = "Associate Professor", department = "Computer Science & Engineering", email = "ramesh.k@srmap.edu.in"),
        FacultyEntity(facultyId = 2, roomId = "SR1_F2_S-206", name = "Dr. Priya Sharma", normalizedName = "DR PRIYA SHARMA", designation = "Assistant Professor", department = "Electronics & Communication", email = "priya.s@srmap.edu.in"),
        FacultyEntity(facultyId = 3, roomId = "CV_F3_C-307", name = "Prof. Anita Roy", normalizedName = "PROF ANITA ROY", designation = "Professor & Head", department = "Computer Science & Engineering", email = "anita.r@srmap.edu.in"),
        FacultyEntity(facultyId = 4, roomId = "CV_F4_C-408", name = "Dr. Vikram Seth", normalizedName = "DR VIKRAM SETH", designation = "Associate Professor", department = "Data Science & AI", email = "vikram.s@srmap.edu.in"),
        FacultyEntity(facultyId = 5, roomId = "V_F1_V-103", name = "Dr. K. S. Rao", normalizedName = "DR K S RAO", designation = "Senior Professor", department = "Physics & Quantum Sciences", email = "ksrao@srmap.edu.in"),
        FacultyEntity(facultyId = 6, roomId = "V_F2_V-204", name = "Dr. Sneha Reddy", normalizedName = "DR SNEHA REDDY", designation = "Assistant Professor", department = "Chemistry & Material Sciences", email = "sneha.r@srmap.edu.in"),
        FacultyEntity(facultyId = 7, roomId = "X_F2_X-203", name = "Dr. Alok Verma", normalizedName = "DR ALOK VERMA", designation = "Associate Professor", department = "Mechanical Engineering", email = "alok.v@srmap.edu.in")
    )

    private val staticEquipment = listOf(
        EquipmentEntity(equipmentId = 1, roomId = "V_F1_V-101", name = "Electron Microscope", normalizedName = "ELECTRON MICROSCOPE", category = "Microscopy", specifications = "High-vacuum FE-SEM with EDX"),
        EquipmentEntity(equipmentId = 2, roomId = "V_F1_V-101", name = "UV-Vis Spectrophotometer", normalizedName = "UV-VIS SPECTROPHOTOMETER", category = "Spectroscopy", specifications = "Dual-beam 190nm to 1100nm"),
        EquipmentEntity(equipmentId = 3, roomId = "V_F1_V-102", name = "Optical Laser Bench", normalizedName = "OPTICAL LASER BENCH", category = "Photonics", specifications = "Stabilized He-Ne laser table"),
        EquipmentEntity(equipmentId = 4, roomId = "X_F1_X-101", name = "Industrial 3D Printer", normalizedName = "INDUSTRIAL 3D PRINTER", category = "Additive Manufacturing", specifications = "Dual-extrusion SLA/FDM system"),
        EquipmentEntity(equipmentId = 5, roomId = "X_F1_X-102", name = "CNC 5-Axis Milling Machine", normalizedName = "CNC 5-AXIS MILLING MACHINE", category = "Machining", specifications = "Precision high-speed spindle"),
        EquipmentEntity(equipmentId = 6, roomId = "CV_F2_C-204", name = "NVIDIA DGX GPU Server", normalizedName = "NVIDIA DGX GPU SERVER", category = "Compute", specifications = "A100 Tensor Core 8x 80GB AI cluster"),
        EquipmentEntity(equipmentId = 7, roomId = "SR1_F2_S-201", name = "Interactive Smart Board 86\"", normalizedName = "INTERACTIVE SMART BOARD 86\"", category = "Smart Classroom", specifications = "4K UHD multi-touch display")
    )

    override fun getBuildings(): Flow<List<CampusBuilding>> {
        return flowOf(
            listOf(
                CampusBuilding(
                    id = "srmap_bldg_16",
                    name = "SR Block",
                    category = LocationCategory.ACADEMIC,
                    centroid = GeoPoint(16.46310, 80.50750),
                    polygon = listOf(
                        GeoPoint(16.46330, 80.50720),
                        GeoPoint(16.46330, 80.50780),
                        GeoPoint(16.46290, 80.50780),
                        GeoPoint(16.46290, 80.50720)
                    ),
                    levels = "3"
                ),
                CampusBuilding(
                    id = "srmap_bldg_20",
                    name = "CV Raman",
                    category = LocationCategory.ACADEMIC,
                    centroid = GeoPoint(16.46250, 80.50560),
                    polygon = listOf(
                        GeoPoint(16.46270, 80.50530),
                        GeoPoint(16.46270, 80.50590),
                        GeoPoint(16.46230, 80.50590),
                        GeoPoint(16.46230, 80.50530)
                    ),
                    levels = "10"
                ),
                CampusBuilding(
                    id = "srmap_bldg_9",
                    name = "V Block",
                    category = LocationCategory.ACADEMIC,
                    centroid = GeoPoint(16.46390, 80.50620),
                    polygon = listOf(
                        GeoPoint(16.46410, 80.50590),
                        GeoPoint(16.46410, 80.50650),
                        GeoPoint(16.46370, 80.50650),
                        GeoPoint(16.46370, 80.50590)
                    ),
                    levels = "3"
                ),
                CampusBuilding(
                    id = "srmap_bldg_11",
                    name = "X Lab",
                    category = LocationCategory.ACADEMIC,
                    centroid = GeoPoint(16.46475, 80.50725),
                    polygon = listOf(
                        GeoPoint(16.46500, 80.50690),
                        GeoPoint(16.46500, 80.50760),
                        GeoPoint(16.46450, 80.50760),
                        GeoPoint(16.46450, 80.50690)
                    ),
                    levels = "3"
                ),
                CampusBuilding(
                    id = "srmap_bldg_30",
                    name = "Homi J Baba Block",
                    category = LocationCategory.ACADEMIC,
                    centroid = GeoPoint(16.46190, 80.50670),
                    polygon = listOf(
                        GeoPoint(16.46210, 80.50640),
                        GeoPoint(16.46210, 80.50700),
                        GeoPoint(16.46170, 80.50700),
                        GeoPoint(16.46170, 80.50640)
                    ),
                    levels = "5"
                ),
                CampusBuilding(
                    id = "srmap_bldg_8",
                    name = "Annapurna Mess",
                    category = LocationCategory.DINING,
                    centroid = GeoPoint(16.46140, 80.50850),
                    polygon = listOf(
                        GeoPoint(16.46160, 80.50820),
                        GeoPoint(16.46160, 80.50880),
                        GeoPoint(16.46120, 80.50880),
                        GeoPoint(16.46120, 80.50820)
                    ),
                    levels = "2"
                ),
                CampusBuilding(
                    id = "srmap_bldg_6",
                    name = "Kaveri Hostel",
                    category = LocationCategory.HOSTEL,
                    centroid = GeoPoint(16.46080, 80.50720),
                    polygon = listOf(
                        GeoPoint(16.46100, 80.50700),
                        GeoPoint(16.46100, 80.50740),
                        GeoPoint(16.46060, 80.50740),
                        GeoPoint(16.46060, 80.50700)
                    ),
                    levels = "8"
                ),
                CampusBuilding(
                    id = "srmap_bldg_4",
                    name = "Godavari Hostel",
                    category = LocationCategory.HOSTEL,
                    centroid = GeoPoint(16.46040, 80.50730),
                    polygon = listOf(
                        GeoPoint(16.46060, 80.50710),
                        GeoPoint(16.46060, 80.50750),
                        GeoPoint(16.46020, 80.50750),
                        GeoPoint(16.46020, 80.50710)
                    ),
                    levels = "8"
                )
            )
        )
    }

    override fun getBlocks(): List<BlockEntity> = staticBlocks
    override fun getRooms(): List<RoomEntity> = staticRooms
    override fun getFaculty(): List<FacultyEntity> = staticFaculty
    override fun getEquipment(): List<EquipmentEntity> = staticEquipment

    override fun getRoomsByBlock(blockId: String): List<RoomEntity> {
        return staticRooms.filter { it.blockId.equals(blockId, ignoreCase = true) }
    }

    override fun getRoomsByBlockAndFloor(blockId: String, floor: Int): List<RoomEntity> {
        return staticRooms.filter {
            it.blockId.equals(blockId, ignoreCase = true) && it.floor == floor
        }
    }

    override fun getFacultyByBlock(blockId: String): List<FacultyEntity> {
        val roomIds = getRoomsByBlock(blockId).map { it.roomId }.toSet()
        return staticFaculty.filter { it.roomId in roomIds }
    }

    override fun getEquipmentByBlock(blockId: String): List<EquipmentEntity> {
        val roomIds = getRoomsByBlock(blockId).map { it.roomId }.toSet()
        return staticEquipment.filter { it.roomId in roomIds }
    }
}
