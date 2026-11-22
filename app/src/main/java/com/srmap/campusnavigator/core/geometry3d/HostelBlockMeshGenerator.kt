package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.models.CampusBuilding

/**
 * Procedural 3D Architectural Reconstruction for the 9-story modular hostel blocks:
 * - Yamuna Girls' Hostel (srmap_bldg_2)
 * - Narmada Girls' Hostel (srmap_bldg_1)
 * - Kaveri Faculty Residence (srmap_bldg_6)
 * - Godavari Hostel (srmap_bldg_4)
 * - Krishna Hostel (srmap_bldg_5)
 *
 * Features:
 * - Continuous 9-story crisp white modular structural egg-crate grid (3.1m x 2.8m cells)
 * - Recessed balcony niches with vibrant primary colors: Vermillion Red (#EF4444), Sunburst Amber (#F59E0B), Cobalt Blue (#3B82F6)
 * - Monumental double-height red entrance portal cutout on lower floors
 * - Full-height vertical glass curtain stair/elevator core flanked by white shear walls
 */
object HostelBlockMeshGenerator {

    fun generateMesh(building: CampusBuilding): MeshData {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        var currentIndex: Short = 0

        fun addQuad(
            p1: Vector3, p2: Vector3, p3: Vector3, p4: Vector3,
            color: Color,
            normal: Vector3? = null
        ) {
            val n = normal ?: run {
                val edge1 = p2 - p1
                val edge2 = p3 - p1
                edge1.cross(edge2).normalized()
            }

            val quad = listOf(p1, p2, p3, p4)
            for (v in quad) {
                positions.add(v.x); positions.add(v.y); positions.add(v.z)
                normals.add(n.x); normals.add(n.y); normals.add(n.z)
                colors.add(color.red); colors.add(color.green); colors.add(color.blue); colors.add(1f)
            }

            indices.add(currentIndex)
            indices.add((currentIndex + 1).toShort())
            indices.add((currentIndex + 2).toShort())

            indices.add(currentIndex)
            indices.add((currentIndex + 2).toShort())
            indices.add((currentIndex + 3).toShort())

            currentIndex = (currentIndex + 4).toShort()
        }

        fun addBox(
            minX: Float, maxX: Float,
            minY: Float, maxY: Float,
            minZ: Float, maxZ: Float,
            color: Color
        ) {
            // Front (Z+)
            addQuad(Vector3(minX, minY, maxZ), Vector3(maxX, minY, maxZ), Vector3(maxX, maxY, maxZ), Vector3(minX, maxY, maxZ), color, Vector3(0f, 0f, 1f))
            // Back (Z-)
            addQuad(Vector3(maxX, minY, minZ), Vector3(minX, minY, minZ), Vector3(minX, maxY, minZ), Vector3(maxX, maxY, minZ), color, Vector3(0f, 0f, -1f))
            // Left (X-)
            addQuad(Vector3(minX, minY, minZ), Vector3(minX, minY, maxZ), Vector3(minX, maxY, maxZ), Vector3(minX, maxY, minZ), color, Vector3(-1f, 0f, 0f))
            // Right (X+)
            addQuad(Vector3(maxX, minY, maxZ), Vector3(maxX, minY, minZ), Vector3(maxX, maxY, minZ), Vector3(maxX, maxY, maxZ), color, Vector3(1f, 0f, 0f))
            // Top (Y+)
            addQuad(Vector3(minX, maxY, maxZ), Vector3(maxX, maxY, maxZ), Vector3(maxX, maxY, minZ), Vector3(minX, maxY, minZ), color, Vector3(0f, 1f, 0f))
            // Bottom (Y-)
            addQuad(Vector3(minX, minY, minZ), Vector3(maxX, minY, minZ), Vector3(maxX, minY, maxZ), Vector3(minX, minY, maxZ), color, Vector3(0f, -1f, 0f))
        }

        val polygon = building.polygon
        val localBasePoints = polygon.map { LocalCoordinateConverter.toLocal3D(it, altitude = 0f) }
        val minX = localBasePoints.minOf { it.x }
        val maxX = localBasePoints.maxOf { it.x }
        val minZ = localBasePoints.minOf { it.z }
        val maxZ = localBasePoints.maxOf { it.z }

        val totalLevels = 9
        val floorHeight = 3.1f
        val totalHeight = totalLevels * floorHeight // ~27.9m

        val whiteGridColor = Color(0xFFF8FAFC)
        val deepRedPortal = Color(0xFFB91C1C)
        val curtainGlass = Color(0xFF38BDF8)
        val windowGlass = Color(0xFF64748B)

        // Color palette for recessed balcony niches
        val nicheColors = listOf(
            Color(0xFFEF4444), // Vermillion Red
            Color(0xFFF59E0B), // Sunburst Amber
            Color(0xFF3B82F6), // Cobalt Blue
            Color(0xFFF8FAFC)  // White neutral
        )

        // 1. Core Mass Body (Offset inside to allow facade reveals)
        addBox(minX + 0.4f, maxX - 0.4f, 0f, totalHeight, minZ + 0.4f, maxZ - 0.4f, whiteGridColor)

        // 2. Continuous White Modular Structural Egg-Crate Grid
        // Floor Slabs
        for (lvl in 0..totalLevels) {
            val y = lvl * floorHeight
            addBox(minX - 0.15f, maxX + 0.15f, y - 0.16f, y + 0.16f, minZ - 0.15f, maxZ + 0.15f, whiteGridColor)
        }

        // Vertical Mullion Columns (Dividing balcony bays)
        val bayWidth = 3.2f
        var x = minX + 2.0f
        var bayIndex = 0
        while (x <= maxX - 2.0f) {
            // Front facade vertical column
            addBox(x - 0.18f, x + 0.18f, 0f, totalHeight, maxZ - 0.15f, maxZ + 0.2f, whiteGridColor)
            // Rear facade vertical column
            addBox(x - 0.18f, x + 0.18f, 0f, totalHeight, minZ - 0.2f, minZ + 0.15f, whiteGridColor)

            // Fill individual balcony niche reveals with vibrant accent colors
            for (lvl in 0 until totalLevels) {
                val y1 = lvl * floorHeight + 0.2f
                val y2 = (lvl + 1) * floorHeight - 0.2f
                val color = nicheColors[(bayIndex + lvl * 3) % nicheColors.size]

                // Balcony back reveal & window
                addBox(x + 0.25f, x + bayWidth - 0.25f, y1, y2, maxZ - 0.4f, maxZ - 0.15f, color)
                addBox(x + 0.4f, x + bayWidth - 0.4f, y1 + 0.4f, y2 - 0.4f, maxZ - 0.16f, maxZ - 0.14f, windowGlass)

                addBox(x + 0.25f, x + bayWidth - 0.25f, y1, y2, minZ + 0.15f, minZ + 0.4f, color)
                addBox(x + 0.4f, x + bayWidth - 0.4f, y1 + 0.4f, y2 - 0.4f, minZ + 0.14f, minZ + 0.16f, windowGlass)
            }

            x += bayWidth
            bayIndex++
        }

        // 3. Double-Height Feature Portal Cutout (Floors 1-2, Red framed portal)
        val portalMinX = minX + (maxX - minX) * 0.25f
        val portalMaxX = portalMinX + 7.5f
        addBox(portalMinX, portalMaxX, 0f, floorHeight * 2.2f, maxZ - 0.5f, maxZ + 0.25f, deepRedPortal)
        addBox(portalMinX + 0.8f, portalMaxX - 0.8f, 0f, floorHeight * 2.0f, maxZ - 0.4f, maxZ + 0.1f, curtainGlass)

        // 4. Vertical Glass Curtain Stair/Elevator Core (Building Ends)
        addBox(maxX - 4.5f, maxX - 1.5f, 0f, totalHeight + 1.8f, maxZ - 0.3f, maxZ + 0.35f, curtainGlass)
        addBox(maxX - 4.8f, maxX - 4.4f, 0f, totalHeight + 2.2f, maxZ - 0.4f, maxZ + 0.4f, whiteGridColor)
        addBox(maxX - 1.4f, maxX - 1.0f, 0f, totalHeight + 2.2f, maxZ - 0.4f, maxZ + 0.4f, whiteGridColor)

        // 5. Rooftop Parapet Wall & Mechanical Headhouse
        addBox(minX - 0.2f, maxX + 0.2f, totalHeight, totalHeight + 1.2f, minZ - 0.2f, maxZ + 0.2f, whiteGridColor)
        addBox(minX + 8f, minX + 16f, totalHeight, totalHeight + 3.2f, minZ + 4f, maxZ - 4f, whiteGridColor)

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
