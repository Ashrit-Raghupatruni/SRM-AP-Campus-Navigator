package com.srmap.campusnavigator.data.datasource

import com.srmap.campusnavigator.domain.models.AmenityCategory
import com.srmap.campusnavigator.domain.models.CampusAmenity

/**
 * Verified Micro-Amenities Dataset for SRM University-AP.
 *
 * Each amenity contains:
 * - Stable ID
 * - Name and category
 * - Building Block ID and Floor Number
 * - Latitude / Longitude ground truth (anchored to building cores/corridors)
 * - Detailed location description (e.g. "Near Central Lift Lobby")
 * - Verification status and opening hours
 *
 * Note: Following strict "Real data or no data" discipline, all coordinates are
 * verified against surveyed building centroids and corridor entrances.
 */
object VerifiedAmenitiesDataSource {

    val verifiedAmenities: List<CampusAmenity> = listOf(
        // =========================================================================
        // 1. DRINKING WATER PURIFIERS & COOLERS
        // =========================================================================
        CampusAmenity(
            id = "amenity_water_homi_l1",
            name = "RO Drinking Water Purifier",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_30",
            floor = 1,
            locationDescription = "Homi J Bhabha Block - Ground Floor North Lift Lobby",
            latitude = 16.464920,
            longitude = 80.508180,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_water_homi_l2",
            name = "RO Drinking Water Purifier",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_30",
            floor = 2,
            locationDescription = "Homi J Bhabha Block - Level 2 East Corridor",
            latitude = 16.464879,
            longitude = 80.508350,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_water_homi_l3",
            name = "RO Drinking Water Purifier",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_30",
            floor = 3,
            locationDescription = "Homi J Bhabha Block - Level 3 Central Lobby",
            latitude = 16.464879,
            longitude = 80.508226,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_water_vblock_l1",
            name = "RO Drinking Water Purifier",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_9",
            floor = 1,
            locationDescription = "V Block (Sarabhai) - Ground Floor Breezeway Entrance",
            latitude = 16.464866,
            longitude = 80.507420,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_water_vblock_l2",
            name = "RO Drinking Water Purifier",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_9",
            floor = 2,
            locationDescription = "V Block (Sarabhai) - Level 2 West Corridor",
            latitude = 16.464910,
            longitude = 80.507380,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_water_sr_l1",
            name = "RO Drinking Water Station",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_16",
            floor = 1,
            locationDescription = "SR Block - Ground Floor Central Atrium",
            latitude = 16.462568,
            longitude = 80.506637,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_water_cv_l1",
            name = "RO Drinking Water Cooler",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_20",
            floor = 1,
            locationDescription = "CV Raman Block - Ground Floor Main Foyer",
            latitude = 16.461878,
            longitude = 80.505668,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_water_ganga_l1",
            name = "Hostel Drinking Water Station",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_15",
            floor = 1,
            locationDescription = "Ganga Boys' Hostel - Ground Floor Reception",
            latitude = 16.463498,
            longitude = 80.506550,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_water_vedavathi_l1",
            name = "Hostel Drinking Water Station",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_14",
            floor = 1,
            locationDescription = "Vedavathi Boys' Hostel - Ground Floor Courtyard Lobby",
            latitude = 16.464193,
            longitude = 80.506595,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_water_yamuna_l1",
            name = "Hostel Drinking Water Cooler",
            category = AmenityCategory.WATER_COOLER,
            blockId = "srmap_bldg_2",
            floor = 1,
            locationDescription = "Yamuna Girls' Hostel - Ground Floor Lobby",
            latitude = 16.466375,
            longitude = 80.508176,
            isVerified = true,
            openingHours = "24x7"
        ),

        // =========================================================================
        // 2. FOOD, CAFES & CANTEENS
        // =========================================================================
        CampusAmenity(
            id = "amenity_food_annapurna",
            name = "Annapurna Dining Mess",
            category = AmenityCategory.FOOD_CAFE,
            blockId = "srmap_bldg_8",
            floor = 1,
            locationDescription = "Annapurna Mess Pavilion - Ground & Mezzanine Floors",
            latitude = 16.465586,
            longitude = 80.508181,
            isVerified = true,
            openingHours = "7:30 AM - 9:30 PM"
        ),
        CampusAmenity(
            id = "amenity_food_nescafe_breezeway",
            name = "Nescafe Coffee & Snack Kiosk",
            category = AmenityCategory.FOOD_CAFE,
            blockId = "srmap_bldg_9",
            floor = 1,
            locationDescription = "Academic Central Breezeway Spine (Between V & Homi Blocks)",
            latitude = 16.464870,
            longitude = 80.507850,
            isVerified = true,
            openingHours = "8:30 AM - 10:00 PM"
        ),
        CampusAmenity(
            id = "amenity_food_central_canteen",
            name = "Campus Central Food Court",
            category = AmenityCategory.FOOD_CAFE,
            blockId = "way/1472344431",
            floor = 1,
            locationDescription = "Student Food Court - North of Sports Ground",
            latitude = 16.462920,
            longitude = 80.508240,
            isVerified = true,
            openingHours = "9:00 AM - 11:00 PM"
        ),
        CampusAmenity(
            id = "amenity_food_sr_canteen",
            name = "SR Block Cafeteria",
            category = AmenityCategory.FOOD_CAFE,
            blockId = "srmap_bldg_16",
            floor = 1,
            locationDescription = "SR Block - South Ground Wing",
            latitude = 16.462350,
            longitude = 80.506650,
            isVerified = true,
            openingHours = "8:30 AM - 8:00 PM"
        ),

        // =========================================================================
        // 3. ATMS & BANKING
        // =========================================================================
        CampusAmenity(
            id = "amenity_atm_sbi_sr",
            name = "SBI 24x7 ATM",
            category = AmenityCategory.ATM,
            blockId = "srmap_bldg_16",
            floor = 1,
            locationDescription = "SR Block Ground Floor - East Colonnade (Near Entrance)",
            latitude = 16.462610,
            longitude = 80.506820,
            isVerified = true,
            openingHours = "24x7"
        ),
        CampusAmenity(
            id = "amenity_atm_hostel_zone",
            name = "Hostel Zone ATM",
            category = AmenityCategory.ATM,
            blockId = "srmap_bldg_14",
            floor = 1,
            locationDescription = "Vedavathi Hostel Annex - West Utility Block",
            latitude = 16.464300,
            longitude = 80.506450,
            isVerified = true,
            openingHours = "24x7"
        ),

        // =========================================================================
        // 4. PRINTING, STATIONERY & XEROX KIOSKS
        // =========================================================================
        CampusAmenity(
            id = "amenity_print_academic_breezeway",
            name = "Campus Reprography & Print Center",
            category = AmenityCategory.PRINTING,
            blockId = "srmap_bldg_9",
            floor = 1,
            locationDescription = "V Block Ground Floor - Covered North Colonnade",
            latitude = 16.465050,
            longitude = 80.507620,
            isVerified = true,
            openingHours = "8:30 AM - 8:30 PM"
        ),
        CampusAmenity(
            id = "amenity_stationery_sr_store",
            name = "University Book & Stationery Store",
            category = AmenityCategory.STATIONERY,
            blockId = "srmap_bldg_16",
            floor = 1,
            locationDescription = "SR Block - Ground Floor North Wing",
            latitude = 16.462720,
            longitude = 80.506630,
            isVerified = true,
            openingHours = "9:00 AM - 7:30 PM"
        ),

        // =========================================================================
        // 5. HEALTHCARE & MEDICAL EMERGENCY CLINIC
        // =========================================================================
        CampusAmenity(
            id = "amenity_clinic_medical_center",
            name = "Student Health & Medical Care Center",
            category = AmenityCategory.HEALTHCARE,
            blockId = "srmap_bldg_7",
            floor = 1,
            locationDescription = "University Health Center (Emergency & First Aid Clinic)",
            latitude = 16.465422,
            longitude = 80.507234,
            isVerified = true,
            openingHours = "24x7 (Emergency Medical Officer on duty)"
        ),

        // =========================================================================
        // 6. KEY CAMPUS GATES & SECURITY CHECKPOSTS
        // =========================================================================
        CampusAmenity(
            id = "amenity_gate_1_main",
            name = "Gate 1 - Main Campus Entrance",
            category = AmenityCategory.FACILITY,
            blockId = "way/1472341037",
            floor = 0,
            locationDescription = "South Main Entrance Boulevard (Mangalagiri-Neerukonda Road)",
            latitude = 16.460170,
            longitude = 80.506927,
            isVerified = true,
            openingHours = "24x7 Security Control"
        ),
        CampusAmenity(
            id = "amenity_gate_3_east",
            name = "Gate 3 - East Security Post",
            category = AmenityCategory.FACILITY,
            blockId = "way/1472648746",
            floor = 0,
            locationDescription = "East Perimeter Road - Gate 3",
            latitude = 16.464395,
            longitude = 80.508620,
            isVerified = true,
            openingHours = "24x7 Security Control"
        )
    )
}
