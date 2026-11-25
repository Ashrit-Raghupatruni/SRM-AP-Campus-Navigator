package com.srmap.campusnavigator.domain.models

enum class AmenityCategory(val displayName: String, val iconName: String) {
    WATER_COOLER("Water Purifier", "water_drop"),
    FOOD_CAFE("Food & Beverage", "restaurant"),
    PRINTING("Printing & Xerox", "print"),
    ATM("ATM & Banking", "account_balance"),
    HEALTHCARE("Campus Clinic", "medical_services"),
    STATIONERY("Stationery", "store"),
    FACILITY("Campus Facility", "business")
}

data class CampusAmenity(
    val id: String,
    val name: String,
    val category: AmenityCategory,
    val blockId: String,
    val floor: Int,
    val locationDescription: String,
    val latitude: Double,
    val longitude: Double,
    val isVerified: Boolean = true,
    val openingHours: String? = null,
    val surveyDate: String = "2026-11-25"
)
