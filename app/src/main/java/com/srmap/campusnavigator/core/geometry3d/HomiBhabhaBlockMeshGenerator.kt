package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.building3d.AcademicComplexSpec

/**
 * Procedural architectural generator for Homi J. Bhabha Block (Right Wing).
 * Mirrored twin building under the unified space-frame canopy:
 * - Solid crisp white corner tower volume on the South-East corner
 * - Multi-tiered vertical louver fins (brise-soleil) across facade bays
 * - Recessed solar reflective blue curtain glazing strips
 * - West facade interfacing with the central breezeway and skywalk bridges
 */
object HomiBhabhaBlockMeshGenerator {

    fun generateMesh(): MeshData {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        var currentIndex: Short = 0

        fun addTri(
            p1: Vector3, p2: Vector3, p3: Vector3,
            n: Vector3,
            c: Color
        ) {
            positions.add(p1.x); positions.add(p1.y); positions.add(p1.z)
            positions.add(p2.x); positions.add(p2.y); positions.add(p2.z)
            positions.add(p3.x); positions.add(p3.y); positions.add(p3.z)

            for (i in 0 until 3) {
                normals.add(n.x); normals.add(n.y); normals.add(n.z)
                colors.add(c.red); colors.add(c.green); colors.add(c.blue); colors.add(1f)
            }

            indices.add(currentIndex)
            indices.add((currentIndex + 1).toShort())
            indices.add((currentIndex + 2).toShort())
            currentIndex = (currentIndex + 3).toShort()
        }

        fun addQuad(
            p1: Vector3, p2: Vector3, p3: Vector3, p4: Vector3,
            n: Vector3,
            c: Color
        ) {
            addTri(p1, p2, p3, n, c)
            addTri(p1, p3, p4, n, c)
        }

        fun addBox(
            minP: Vector3, maxP: Vector3, c: Color
        ) {
            // Front (South +Z)
            addQuad(Vector3(maxP.x, minP.y, maxP.z), Vector3(minP.x, minP.y, maxP.z), Vector3(minP.x, maxP.y, maxP.z), Vector3(maxP.x, maxP.y, maxP.z), Vector3(0f, 0f, 1f), c)
            // Back (North -Z)
            addQuad(Vector3(minP.x, minP.y, minP.z), Vector3(maxP.x, minP.y, minP.z), Vector3(maxP.x, maxP.y, minP.z), Vector3(minP.x, maxP.y, minP.z), Vector3(0f, 0f, -1f), c)
            // Left (West -X)
            addQuad(Vector3(minP.x, minP.y, maxP.z), Vector3(minP.x, minP.y, minP.z), Vector3(minP.x, maxP.y, minP.z), Vector3(minP.x, maxP.y, maxP.z), Vector3(-1f, 0f, 0f), c)
            // Right (East +X)
            addQuad(Vector3(maxP.x, minP.y, minP.z), Vector3(maxP.x, minP.y, maxP.z), Vector3(maxP.x, maxP.y, maxP.z), Vector3(maxP.x, maxP.y, minP.z), Vector3(1f, 0f, 0f), c)
            // Top (+Y)
            addQuad(Vector3(minP.x, maxP.y, minP.z), Vector3(maxP.x, maxP.y, minP.z), Vector3(maxP.x, maxP.y, maxP.z), Vector3(minP.x, maxP.y, maxP.z), Vector3(0f, 1f, 0f), c)
        }

        val minX = AcademicComplexSpec.HOMI_MIN_X
        val maxX = AcademicComplexSpec.HOMI_MAX_X
        val minZ = AcademicComplexSpec.HOMI_MIN_Z
        val maxZ = AcademicComplexSpec.HOMI_MAX_Z
        val levels = AcademicComplexSpec.HOMI_LEVELS
        val height = AcademicComplexSpec.HOMI_HEIGHT

        val whiteTowerC = AcademicComplexSpec.COLOR_SOLID_WHITE_TOWER
        val louverC     = AcademicComplexSpec.COLOR_LOUVER_FINS
        val glassC      = AcademicComplexSpec.COLOR_CURTAIN_GLAZING
        val spandrelC   = AcademicComplexSpec.COLOR_CANOPY_TRUSS

        // 1. SOLID CRISP WHITE CORNER TOWER VOLUME (South-East Corner)
        val towerWidth = 14.0f
        val towerDepth = 18.0f
        val towerX1 = maxX - towerWidth
        val towerZ1 = maxZ - towerDepth
        addBox(Vector3(towerX1, 0f, towerZ1), Vector3(maxX, height + 0.8f, maxZ), whiteTowerC)

        // 2. MAIN ENVELOPE: RECESSED CURTAIN GLAZING & FLOOR SLABS
        // Running from the Breezeway edge (minX) to the tower edge (towerX1)
        val glassZ = maxZ - 0.4f
        addQuad(
            Vector3(towerX1, 0f, glassZ),
            Vector3(minX, 0f, glassZ),
            Vector3(minX, height, glassZ),
            Vector3(towerX1, height, glassZ),
            Vector3(0f, 0f, 1f),
            glassC
        )

        // Horizontal Floor Spandrels between levels
        val floorH = height / levels
        for (lvl in 0..levels) {
            val y = lvl * floorH
            addQuad(
                Vector3(towerX1, (y - 0.25f).coerceAtLeast(0f), maxZ - 0.1f),
                Vector3(minX, (y - 0.25f).coerceAtLeast(0f), maxZ - 0.1f),
                Vector3(minX, (y + 0.25f).coerceAtMost(height), maxZ - 0.1f),
                Vector3(towerX1, (y + 0.25f).coerceAtMost(height), maxZ - 0.1f),
                Vector3(0f, 0f, 1f),
                spandrelC
            )
        }

        // 3. VERTICAL SUNSHADE LOUVER FINS
        val louverSpacing = 1.35f
        var curX = minX + 1.2f
        val louverProj = 0.55f
        val louverThick = 0.08f

        while (curX < towerX1 - 1.0f) {
            addBox(
                Vector3(curX, 1.2f, maxZ - louverProj),
                Vector3(curX + louverThick, height - 0.4f, maxZ),
                louverC
            )
            curX += louverSpacing
        }

        // 4. NORTH FACADE (Tree Court facing)
        addQuad(
            Vector3(minX, 0f, minZ),
            Vector3(maxX, 0f, minZ),
            Vector3(maxX, height, minZ),
            Vector3(minX, height, minZ),
            Vector3(0f, 0f, -1f),
            glassC
        )
        curX = minX + 2.0f
        while (curX < maxX - 2.0f) {
            addBox(
                Vector3(curX, 1.2f, minZ),
                Vector3(curX + louverThick, height - 0.4f, minZ + louverProj),
                louverC
            )
            curX += (louverSpacing * 1.5f)
        }

        // 5. EAST FACADE (Campus Perimeter)
        addQuad(
            Vector3(maxX, 0f, minZ),
            Vector3(maxX, 0f, maxZ),
            Vector3(maxX, height, maxZ),
            Vector3(maxX, height, minZ),
            Vector3(1f, 0f, 0f),
            whiteTowerC
        )

        // 6. WEST FACADE (Facing Central Breezeway Spine)
        addQuad(
            Vector3(minX, 0f, maxZ),
            Vector3(minX, 0f, minZ),
            Vector3(minX, height, minZ),
            Vector3(minX, height, maxZ),
            Vector3(-1f, 0f, 0f),
            whiteTowerC
        )

        // 7. ROOF PARAPET & DECK
        addQuad(
            Vector3(minX, height, minZ),
            Vector3(maxX, height, minZ),
            Vector3(maxX, height, maxZ),
            Vector3(minX, height, maxZ),
            Vector3(0f, 1f, 0f),
            AcademicComplexSpec.COLOR_CANOPY_TRUSS
        )

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
