package com.srmap.campusnavigator.domain.building3d

enum class VerificationStatus {
    VERIFIED_SURVEY,        // Measured from GeoJSON coordinates or verified blueprints
    EVIDENCE_DERIVED,       // Inferred from official campus maps & path junctions
    UNVERIFIED_PLACEHOLDER  // Standard parameter awaiting calibrated on-site reference
}

data class VerifiedMaterial(
    val name: String,
    val colorHex: String,
    val status: VerificationStatus
)
