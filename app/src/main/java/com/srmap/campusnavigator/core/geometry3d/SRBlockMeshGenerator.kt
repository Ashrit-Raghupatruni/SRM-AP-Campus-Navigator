package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.building3d.SRBlockSpec
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural architectural generator for SR Block (Sarvepalli Radhakrishnan Block).
 * Accurately models the features visible in the campus reference photographs:
 * - 7 Floors of continuous horizontal cantilevered eyebrow slabs
 * - Recessed dark charcoal spandrel sub-bands
 * - Continuous wrap-around ribbon curtain glazing (including corner wrapping)
 * - Solid warm sandstone vertical shear core towers
 * - Recessed ground-floor colonnade
 * - Rooftop mechanical penthouse enclosure
 * - Landscaped entrance plaza and approach parterres
 */
object SRBlockMeshGenerator {

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
            // Front (+Z)
            addQuad(Vector3(maxP.x, minP.y, maxP.z), Vector3(minP.x, minP.y, maxP.z), Vector3(minP.x, maxP.y, maxP.z), Vector3(maxP.x, maxP.y, maxP.z), Vector3(0f, 0f, 1f), c)
            // Back (-Z)
            addQuad(Vector3(minP.x, minP.y, minP.z), Vector3(maxP.x, minP.y, minP.z), Vector3(maxP.x, maxP.y, minP.z), Vector3(minP.x, maxP.y, minP.z), Vector3(0f, 0f, -1f), c)
            // Left (-X)
            addQuad(Vector3(minP.x, minP.y, maxP.z), Vector3(minP.x, minP.y, minP.z), Vector3(minP.x, maxP.y, minP.z), Vector3(minP.x, maxP.y, maxP.z), Vector3(-1f, 0f, 0f), c)
            // Right (+X)
            addQuad(Vector3(maxP.x, minP.y, minP.z), Vector3(maxP.x, minP.y, maxP.z), Vector3(maxP.x, maxP.y, maxP.z), Vector3(maxP.x, maxP.y, minP.z), Vector3(1f, 0f, 0f), c)
            // Top (+Y)
            addQuad(Vector3(minP.x, maxP.y, minP.z), Vector3(maxP.x, maxP.y, minP.z), Vector3(maxP.x, maxP.y, maxP.z), Vector3(minP.x, maxP.y, maxP.z), Vector3(0f, 1f, 0f), c)
        }

        fun addCylinder(bottom: Vector3, top: Vector3, radius: Float, segments: Int, c: Color) {
            val step = (2f * Math.PI.toFloat()) / segments
            for (i in 0 until segments) {
                val a1 = i * step
                val a2 = (i + 1) * step
                val c1 = cos(a1) * radius
                val s1 = sin(a1) * radius
                val c2 = cos(a2) * radius
                val s2 = sin(a2) * radius

                val p1 = Vector3(bottom.x + c1, bottom.y, bottom.z + s1)
                val p2 = Vector3(bottom.x + c2, bottom.y, bottom.z + s2)
                val p3 = Vector3(top.x + c2, top.y, top.z + s2)
                val p4 = Vector3(top.x + c1, top.y, top.z + s1)

                val n = Vector3((c1 + c2) / 2f, 0f, (s1 + s2) / 2f).normalized()
                addQuad(p1, p2, p3, p4, n, c)
            }
        }

        val minX = SRBlockSpec.MIN_X
        val maxX = SRBlockSpec.MAX_X
        val minZ = SRBlockSpec.MIN_Z
        val maxZ = SRBlockSpec.MAX_Z
        val levels = SRBlockSpec.LEVELS_COUNT
        val floorH = SRBlockSpec.FLOOR_HEIGHT_METERS
        val roofH  = SRBlockSpec.TOTAL_ROOF_HEIGHT_METERS

        val slabC       = SRBlockSpec.COLOR_SLAB_EYEBROW
        val spandrelC   = SRBlockSpec.COLOR_DARK_SPANDREL
        val glassC      = SRBlockSpec.COLOR_RIBBON_GLAZING
        val coreTowerC  = SRBlockSpec.COLOR_VERTICAL_CORE
        val pillarC     = SRBlockSpec.COLOR_COLONNADE_PILLARS
        val roofC       = SRBlockSpec.COLOR_ROOF_DECK
        val penthouseC  = SRBlockSpec.COLOR_PENTHOUSE_WALL
        val paverC      = SRBlockSpec.COLOR_PLAZA_PAVERS
        val greenC      = SRBlockSpec.COLOR_GREEN_PARTERRE

        // 1. FLOOR-BY-FLOOR HORIZONTAL RIBBON TIERS (Floors 0 to 6)
        val slabOverhang = 0.85f // Cantilevered eyebrow fin extension
        val slabThick    = 0.45f // Thickness of horizontal concrete slab
        val spandrelH    = 0.35f // Height of dark charcoal metal spandrel
        val glassInset   = 0.25f // Inset behind the slab face

        // Building main box bounds
        val bMinX = minX + 2.0f
        val bMaxX = maxX - 2.0f
        val bMinZ = minZ + 2.0f
        val bMaxZ = maxZ - 2.0f

        for (lvl in 0 until levels) {
            val yFloor = lvl * floorH
            val ySlabBottom = (yFloor + floorH - slabThick)
            val ySlabTop    = yFloor + floorH

            // A. Continuous Cantilevered Floor Eyebrow Slab (wrapping around entire perimeter)
            val sMinX = bMinX - slabOverhang
            val sMaxX = bMaxX + slabOverhang
            val sMinZ = bMinZ - slabOverhang
            val sMaxZ = bMaxZ + slabOverhang
            addBox(Vector3(sMinX, ySlabBottom, sMinZ), Vector3(sMaxX, ySlabTop, sMaxZ), slabC)

            // B. Ground Floor vs Upper Floors Glazing
            if (lvl == 0) {
                // Ground floor: Recessed colonnade arcade with structural columns
                val colInset = 1.8f
                val cMinX = bMinX + colInset
                val cMaxX = bMaxX - colInset
                val cMinZ = bMinZ + colInset
                val cMaxZ = bMaxZ - colInset

                // Recessed Ground Glass
                addBox(Vector3(cMinX, 0f, cMinZ), Vector3(cMaxX, ySlabBottom, cMaxZ), glassC)

                // Structural Colonnade Pillars along the perimeter
                var colZ = bMinZ + 4.0f
                while (colZ < bMaxZ - 2.0f) {
                    addCylinder(Vector3(bMaxX - 0.4f, 0f, colZ), Vector3(bMaxX - 0.4f, ySlabBottom, colZ), 0.35f, 6, pillarC)
                    addCylinder(Vector3(bMinX + 0.4f, 0f, colZ), Vector3(bMinX + 0.4f, ySlabBottom, colZ), 0.35f, 6, pillarC)
                    colZ += 7.0f
                }
            } else {
                // Upper Floors: Continuous Ribbon Glazing with Dark Spandrel Band
                val ySpandrelTop = yFloor + spandrelH
                val yGlassTop    = ySlabBottom

                // Dark Charcoal Spandrel Sub-Band (Levels 1 to 6)
                addBox(Vector3(bMinX, yFloor, bMinZ), Vector3(bMaxX, ySpandrelTop, bMaxZ), spandrelC)

                // Continuous Ribbon Glazing (wrapping around all 4 sides with corner glass)
                val gMinX = bMinX + glassInset
                val gMaxX = bMaxX - glassInset
                val gMinZ = bMinZ + glassInset
                val gMaxZ = bMaxZ - glassInset
                addBox(Vector3(gMinX, ySpandrelTop, gMinZ), Vector3(gMaxX, yGlassTop, gMaxZ), glassC)
            }
        }

        // 2. SOLID WARM SANDSTONE VERTICAL SHEAR CORE TOWERS
        // Architectural vertical accents interrupting the horizontal ribbons (as seen in Photo 1 & 2)
        val coreWidth = 8.5f
        val coreDepth = 5.0f

        // East Core Tower (Sports Court facing)
        val eCoreZ1 = (minZ + maxZ) / 2f - coreWidth / 2f
        val eCoreZ2 = eCoreZ1 + coreWidth
        addBox(
            Vector3(bMaxX - coreDepth, 0f, eCoreZ1),
            Vector3(bMaxX + slabOverhang + 0.2f, roofH + 1.2f, eCoreZ2),
            coreTowerC
        )

        // West Core Tower (Perimeter facing)
        addBox(
            Vector3(bMinX - slabOverhang - 0.2f, 0f, eCoreZ1),
            Vector3(bMinX + coreDepth, 0f + roofH + 1.2f, eCoreZ2),
            coreTowerC
        )

        // North Stair Tower
        addBox(
            Vector3((minX + maxX) / 2f - 4.0f, 0f, bMinZ - slabOverhang - 0.2f),
            Vector3((minX + maxX) / 2f + 4.0f, roofH + 1.2f, bMinZ + coreDepth),
            coreTowerC
        )

        // 3. ROOF SURFACE & MECHANICAL PENTHOUSE (Y=23.1m to 26.5m)
        // Main Roof Deck
        addBox(Vector3(bMinX, roofH, bMinZ), Vector3(bMaxX, roofH + 0.3f, bMaxZ), roofC)

        // Perimeter Roof Parapet Wall (0.9m)
        val parH = roofH + 0.9f
        addQuad(Vector3(bMinX, roofH, bMinZ), Vector3(bMaxX, roofH, bMinZ), Vector3(bMaxX, parH, bMinZ), Vector3(bMinX, parH, bMinZ), Vector3(0f, 0f, -1f), slabC)
        addQuad(Vector3(bMaxX, roofH, bMaxZ), Vector3(bMinX, roofH, bMaxZ), Vector3(bMinX, parH, bMaxZ), Vector3(bMaxX, parH, bMaxZ), Vector3(0f, 0f, 1f), slabC)
        addQuad(Vector3(bMaxX, roofH, bMinZ), Vector3(bMaxX, roofH, bMaxZ), Vector3(bMaxX, parH, bMaxZ), Vector3(bMaxX, parH, bMinZ), Vector3(1f, 0f, 0f), slabC)
        addQuad(Vector3(bMinX, roofH, bMaxZ), Vector3(bMinX, roofH, bMinZ), Vector3(bMinX, parH, bMinZ), Vector3(bMinX, parH, bMaxZ), Vector3(-1f, 0f, 0f), slabC)

        // Recessed Rooftop Mechanical Penthouse (Prominent in Photo 1 & 3)
        val pentW = 16.0f
        val pentL = 32.0f
        val pentX1 = (bMinX + bMaxX) / 2f - pentW / 2f
        val pentX2 = pentX1 + pentW
        val pentZ1 = (bMinZ + bMaxZ) / 2f - pentL / 2f
        val pentZ2 = pentZ1 + pentL
        val pentTop = roofH + SRBlockSpec.PENTHOUSE_HEIGHT_METERS
        addBox(Vector3(pentX1, roofH, pentZ1), Vector3(pentX2, pentTop, pentZ2), penthouseC)

        // 4. LANDSCAPED ENTRANCE PLAZA & APPROACH PARTERRES (Photo 3 Ground Interface)
        val plazaZ1 = minZ - 8.0f
        val plazaZ2 = maxZ + 8.0f
        val plazaX1 = maxX + 0.5f
        val plazaX2 = maxX + 14.0f
        val pY = 0.035f

        // Paved approach concourse
        addQuad(
            Vector3(plazaX1, pY, plazaZ1),
            Vector3(plazaX2, pY, plazaZ1),
            Vector3(plazaX2, pY, plazaZ2),
            Vector3(plazaX1, pY, plazaZ2),
            Vector3(0f, 1f, 0f),
            paverC
        )

        // Geometric Green Lawn Parterres alongside the entrance
        val gY = 0.05f
        val lawnW = 8.0f
        val lawnL = 22.0f
        for (i in 0..2) {
            val lZ1 = minZ + 12.0f + i * 30.0f
            val lZ2 = lZ1 + lawnL
            if (lZ2 < maxZ - 5.0f) {
                addBox(Vector3(plazaX2 + 1.0f, 0f, lZ1), Vector3(plazaX2 + 1.0f + lawnW, gY, lZ2), greenC)
            }
        }

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
