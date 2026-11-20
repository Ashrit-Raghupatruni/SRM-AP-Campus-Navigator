package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.LocationCategory

object BuildingMeshGenerator {

    private const val DEFAULT_FLOOR_HEIGHT_METERS = 3.0f

    fun generateBuildingMesh(
        building: CampusBuilding,
        isHighlighted: Boolean = false
    ): MeshData? {
        // High-Fidelity Evidence-Driven Architectural Reconstruction for Vikram Sarabhai Block (Academic Block I / V Block)
        if (!isHighlighted && (building.id == "srmap_bldg_9" || building.name.contains("Vikram Sarabhai", ignoreCase = true) || building.name.equals("V Block", ignoreCase = true))) {
            return VikramSarabhaiBlockMeshGenerator.generateMesh()
        }

        // High-Fidelity Evidence-Driven Architectural Reconstruction for Homi J. Bhabha Block (Academic Block II)
        if (!isHighlighted && (building.id == "srmap_bldg_30" || building.name.contains("Homi", ignoreCase = true) || building.name.contains("Bhabha", ignoreCase = true))) {
            return HomiBhabhaBlockMeshGenerator.generateMesh()
        }

        // High-Fidelity Evidence-Driven Architectural Reconstruction for SR Block (Sarvepalli Radhakrishnan Block)
        if (!isHighlighted && (building.id == "srmap_bldg_16" || building.name.contains("SR Block", ignoreCase = true) || building.name.contains("Radhakrishnan", ignoreCase = true))) {
            return SRBlockMeshGenerator.generateMesh()
        }

        // High-Fidelity Architectural Reconstruction for Ganga Boys' Hostel (srmap_bldg_15) - 26 Floors
        if (!isHighlighted && (building.id == "srmap_bldg_15" || building.name.contains("Ganga", ignoreCase = true))) {
            return GangaBlockMeshGenerator.generateMesh()
        }

        // High-Fidelity Architectural Reconstruction for Vedavathi Boys' Hostel (srmap_bldg_14) - 15 Floors U-Courtyard
        if (!isHighlighted && (building.id == "srmap_bldg_14" || building.name.contains("Vedavathi", ignoreCase = true))) {
            return VedavathiBlockMeshGenerator.generateMesh()
        }

        val polygon = building.polygon
        if (polygon.size < 3) return null

        val localBasePoints = polygon.map { LocalCoordinateConverter.toLocal3D(it, altitude = 0f) }
        val levels = building.levels?.toIntOrNull() ?: when (building.category) {
            LocationCategory.ACADEMIC -> 7
            LocationCategory.HOSTEL -> 9
            LocationCategory.DINING -> 2
            else -> 3
        }
        val height = levels * DEFAULT_FLOOR_HEIGHT_METERS

        val localTopPoints = localBasePoints.map { Vector3(it.x, height, it.z) }

        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        val baseColor = when {
            isHighlighted -> Color(0xFFA61B35) // SRM Crimson Highlight
            building.category == LocationCategory.ACADEMIC -> Color(0xFFE2E8F0) // Modern Off-white
            building.category == LocationCategory.HOSTEL -> Color(0xFFF1F5F9) // Slate White
            building.category == LocationCategory.DINING -> Color(0xFFFEF3C7) // Warm Mess
            else -> Color(0xFFCBD5E1)
        }

        val roofColor = when {
            isHighlighted -> Color(0xFF881337)
            building.category == LocationCategory.DINING -> Color(0xFFF59E0B) // Golden Roof
            else -> Color(0xFF94A3B8)
        }

        val n = localBasePoints.size
        var currentIndex: Short = 0

        // 1. Generate Wall Quads
        for (i in 0 until n) {
            val nextIdx = (i + 1) % n
            val b1 = localBasePoints[i]
            val b2 = localBasePoints[nextIdx]
            val t1 = localTopPoints[i]
            val t2 = localTopPoints[nextIdx]

            // Outward face normal
            val edge = b2 - b1
            val normal = Vector3(-edge.z, 0f, edge.x).normalized()

            // Subtle vertical shading for architectural contrast
            val wallShade = if (i % 2 == 0) 1.0f else 0.92f
            val r = (baseColor.red * wallShade).coerceIn(0f, 1f)
            val g = (baseColor.green * wallShade).coerceIn(0f, 1f)
            val b = (baseColor.blue * wallShade).coerceIn(0f, 1f)
            val a = 1.0f

            // Quad vertices: b1, b2, t2, t1
            val quadVerts = listOf(b1, b2, t2, t1)
            for (v in quadVerts) {
                positions.add(v.x); positions.add(v.y); positions.add(v.z)
                normals.add(normal.x); normals.add(normal.y); normals.add(normal.z)
                colors.add(r); colors.add(g); colors.add(b); colors.add(a)
            }

            // Triangle 1: b1, b2, t2
            indices.add(currentIndex)
            indices.add((currentIndex + 1).toShort())
            indices.add((currentIndex + 2).toShort())

            // Triangle 2: b1, t2, t1
            indices.add(currentIndex)
            indices.add((currentIndex + 2).toShort())
            indices.add((currentIndex + 3).toShort())

            currentIndex = (currentIndex + 4).toShort()
        }

        // 2. Generate Triangulated Roof
        val roofStartIdx = currentIndex
        for (t in localTopPoints) {
            positions.add(t.x); positions.add(t.y); positions.add(t.z)
            normals.add(0f); normals.add(1f); normals.add(0f) // Upwards normal
            colors.add(roofColor.red); colors.add(roofColor.green); colors.add(roofColor.blue); colors.add(1f)
            currentIndex = (currentIndex + 1).toShort()
        }

        val roofTriangles = PolygonTriangulator.triangulate(localTopPoints)
        for (triIdx in roofTriangles) {
            indices.add((roofStartIdx + triIdx).toShort())
        }

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
