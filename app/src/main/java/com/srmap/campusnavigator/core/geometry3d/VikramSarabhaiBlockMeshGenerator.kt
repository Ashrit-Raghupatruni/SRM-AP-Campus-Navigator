package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.building3d.VikramSarabhaiBlockSpec

/**
 * Procedural architectural reconstruction generator for Vikram Sarabhai Block (V Block).
 * Builds:
 * 1. Perimeter walls broken down by 5 floors with horizontal spandrels.
 * 2. Recessed architectural window modules with frames and solar glazing.
 * 3. Ground-level physical entrance portal facing the South pedestrian walkway.
 * 4. Flat roof deck with 0.9m perimeter parapet safety wall.
 */
object VikramSarabhaiBlockMeshGenerator {

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

        val minX = VikramSarabhaiBlockSpec.MIN_X
        val maxX = VikramSarabhaiBlockSpec.MAX_X
        val minZ = VikramSarabhaiBlockSpec.MIN_Z
        val maxZ = VikramSarabhaiBlockSpec.MAX_Z
        val levels = VikramSarabhaiBlockSpec.LEVELS_COUNT
        val floorH = VikramSarabhaiBlockSpec.FLOOR_HEIGHT_METERS
        val roofH = VikramSarabhaiBlockSpec.TOTAL_ROOF_HEIGHT_METERS
        val parapetH = VikramSarabhaiBlockSpec.PARAPET_HEIGHT_METERS

        val wallC = VikramSarabhaiBlockSpec.wallColor
        val spandrelC = VikramSarabhaiBlockSpec.spandrelColor
        val frameC = VikramSarabhaiBlockSpec.frameColor
        val glassC = VikramSarabhaiBlockSpec.glassColor
        val portalC = VikramSarabhaiBlockSpec.portalColor
        val roofC = VikramSarabhaiBlockSpec.roofColor
        val parapetC = VikramSarabhaiBlockSpec.parapetColor

        // 1. FLOOR-BY-FLOOR WALL FACADES & RECESSED WINDOWS
        for (floor in 0 until levels) {
            val yBottom = floor * floorH
            val ySill = yBottom + 0.9f
            val yLintel = yBottom + 2.5f
            val yTop = yBottom + floorH

            // Spandrel band below windows (0 to 0.9m)
            // North Wall
            addQuad(Vector3(minX, yBottom, minZ), Vector3(maxX, yBottom, minZ), Vector3(maxX, ySill, minZ), Vector3(minX, ySill, minZ), Vector3(0f, 0f, -1f), spandrelC)
            // South Wall (Skip center on floor 0 for entrance)
            if (floor > 0) {
                addQuad(Vector3(maxX, yBottom, maxZ), Vector3(minX, yBottom, maxZ), Vector3(minX, ySill, maxZ), Vector3(maxX, ySill, maxZ), Vector3(0f, 0f, 1f), spandrelC)
            } else {
                // Ground floor split around entrance
                val midX = (minX + maxX) / 2f
                val entranceHalfW = 4.0f
                addQuad(Vector3(maxX, yBottom, maxZ), Vector3(midX + entranceHalfW, yBottom, maxZ), Vector3(midX + entranceHalfW, ySill, maxZ), Vector3(maxX, ySill, maxZ), Vector3(0f, 0f, 1f), spandrelC)
                addQuad(Vector3(midX - entranceHalfW, yBottom, maxZ), Vector3(minX, yBottom, maxZ), Vector3(minX, ySill, maxZ), Vector3(midX - entranceHalfW, ySill, maxZ), Vector3(0f, 0f, 1f), spandrelC)
            }
            // East Wall
            addQuad(Vector3(maxX, yBottom, minZ), Vector3(maxX, yBottom, maxZ), Vector3(maxX, ySill, maxZ), Vector3(maxX, ySill, minZ), Vector3(1f, 0f, 0f), spandrelC)
            // West Wall
            addQuad(Vector3(minX, yBottom, maxZ), Vector3(minX, yBottom, minZ), Vector3(minX, ySill, minZ), Vector3(minX, ySill, maxZ), Vector3(-1f, 0f, 0f), spandrelC)

            // Header band above windows (2.5m to 3.2m)
            addQuad(Vector3(minX, yLintel, minZ), Vector3(maxX, yLintel, minZ), Vector3(maxX, yTop, minZ), Vector3(minX, yTop, minZ), Vector3(0f, 0f, -1f), wallC)
            addQuad(Vector3(maxX, yLintel, maxZ), Vector3(minX, yLintel, maxZ), Vector3(minX, yTop, maxZ), Vector3(maxX, yTop, maxZ), Vector3(0f, 0f, 1f), wallC)
            addQuad(Vector3(maxX, yLintel, minZ), Vector3(maxX, yLintel, maxZ), Vector3(maxX, yTop, maxZ), Vector3(maxX, yTop, minZ), Vector3(1f, 0f, 0f), wallC)
            addQuad(Vector3(minX, yLintel, maxZ), Vector3(minX, yLintel, minZ), Vector3(minX, yTop, minZ), Vector3(minX, yTop, maxZ), Vector3(-1f, 0f, 0f), wallC)

            // Window Zone with Recesses (ySill to yLintel, 1.6m high)
            // Long facades (North & South): 14 bays of windows
            val numBaysNS = 14
            val bayStepX = (maxX - minX) / numBaysNS
            val windowW = bayStepX * 0.72f
            val pierW = bayStepX - windowW
            val recessD = 0.15f

            for (b in 0 until numBaysNS) {
                val bx1 = minX + b * bayStepX
                val wx1 = bx1 + pierW / 2f
                val wx2 = wx1 + windowW
                val bx2 = bx1 + bayStepX

                // Vertical Pier on outer wall plane
                addQuad(Vector3(bx1, ySill, minZ), Vector3(wx1, ySill, minZ), Vector3(wx1, yLintel, minZ), Vector3(bx1, yLintel, minZ), Vector3(0f, 0f, -1f), wallC)
                addQuad(Vector3(wx2, ySill, minZ), Vector3(bx2, ySill, minZ), Vector3(bx2, yLintel, minZ), Vector3(wx2, yLintel, minZ), Vector3(0f, 0f, -1f), wallC)

                // North Recessed Window Glass & Reveals
                val nzRecessed = minZ + recessD
                // Reveals
                addQuad(Vector3(wx1, ySill, minZ), Vector3(wx1, ySill, nzRecessed), Vector3(wx1, yLintel, nzRecessed), Vector3(wx1, yLintel, minZ), Vector3(1f, 0f, 0f), frameC)
                addQuad(Vector3(wx2, ySill, nzRecessed), Vector3(wx2, ySill, minZ), Vector3(wx2, yLintel, minZ), Vector3(wx2, yLintel, nzRecessed), Vector3(-1f, 0f, 0f), frameC)
                addQuad(Vector3(wx1, ySill, minZ), Vector3(wx2, ySill, minZ), Vector3(wx2, ySill, nzRecessed), Vector3(wx1, ySill, nzRecessed), Vector3(0f, 1f, 0f), frameC)
                addQuad(Vector3(wx1, yLintel, nzRecessed), Vector3(wx2, yLintel, nzRecessed), Vector3(wx2, yLintel, minZ), Vector3(wx1, yLintel, minZ), Vector3(0f, -1f, 0f), frameC)
                // Recessed Glazing
                addQuad(Vector3(wx1, ySill, nzRecessed), Vector3(wx2, ySill, nzRecessed), Vector3(wx2, yLintel, nzRecessed), Vector3(wx1, yLintel, nzRecessed), Vector3(0f, 0f, -1f), glassC)

                // South Facade (Skip center bay on floor 0 for entrance)
                val isEntranceBay = (floor == 0 && kotlin.math.abs((wx1 + wx2) / 2f - (minX + maxX) / 2f) < 4.5f)
                if (!isEntranceBay) {
                    addQuad(Vector3(bx2, ySill, maxZ), Vector3(wx2, ySill, maxZ), Vector3(wx2, yLintel, maxZ), Vector3(bx2, yLintel, maxZ), Vector3(0f, 0f, 1f), wallC)
                    addQuad(Vector3(wx1, ySill, maxZ), Vector3(bx1, ySill, maxZ), Vector3(bx1, yLintel, maxZ), Vector3(wx1, yLintel, maxZ), Vector3(0f, 0f, 1f), wallC)

                    val szRecessed = maxZ - recessD
                    addQuad(Vector3(wx2, ySill, maxZ), Vector3(wx2, ySill, szRecessed), Vector3(wx2, yLintel, szRecessed), Vector3(wx2, yLintel, maxZ), Vector3(-1f, 0f, 0f), frameC)
                    addQuad(Vector3(wx1, ySill, szRecessed), Vector3(wx1, ySill, maxZ), Vector3(wx1, yLintel, maxZ), Vector3(wx1, yLintel, szRecessed), Vector3(1f, 0f, 0f), frameC)
                    addQuad(Vector3(wx2, ySill, maxZ), Vector3(wx1, ySill, maxZ), Vector3(wx1, ySill, szRecessed), Vector3(wx2, ySill, szRecessed), Vector3(0f, 1f, 0f), frameC)
                    addQuad(Vector3(wx2, yLintel, szRecessed), Vector3(wx1, yLintel, szRecessed), Vector3(wx1, yLintel, maxZ), Vector3(wx2, yLintel, maxZ), Vector3(0f, -1f, 0f), frameC)
                    addQuad(Vector3(wx2, ySill, szRecessed), Vector3(wx1, ySill, szRecessed), Vector3(wx1, yLintel, szRecessed), Vector3(wx2, yLintel, szRecessed), Vector3(0f, 0f, 1f), glassC)
                }
            }

            // East & West Facades: 10 bays of windows
            val numBaysEW = 10
            val bayStepZ = (maxZ - minZ) / numBaysEW
            val windowEW = bayStepZ * 0.70f
            val pierEW = bayStepZ - windowEW

            for (b in 0 until numBaysEW) {
                val bz1 = minZ + b * bayStepZ
                val wz1 = bz1 + pierEW / 2f
                val wz2 = wz1 + windowEW
                val bz2 = bz1 + bayStepZ

                // East Wall
                addQuad(Vector3(maxX, ySill, bz1), Vector3(maxX, ySill, wz1), Vector3(maxX, yLintel, wz1), Vector3(maxX, yLintel, bz1), Vector3(1f, 0f, 0f), wallC)
                addQuad(Vector3(maxX, ySill, wz2), Vector3(maxX, ySill, bz2), Vector3(maxX, yLintel, bz2), Vector3(maxX, yLintel, wz2), Vector3(1f, 0f, 0f), wallC)
                val exRecessed = maxX - recessD
                addQuad(Vector3(exRecessed, ySill, wz1), Vector3(exRecessed, ySill, wz2), Vector3(exRecessed, yLintel, wz2), Vector3(exRecessed, yLintel, wz1), Vector3(1f, 0f, 0f), glassC)

                // West Wall
                addQuad(Vector3(minX, ySill, wz1), Vector3(minX, ySill, bz1), Vector3(minX, yLintel, bz1), Vector3(minX, yLintel, wz1), Vector3(-1f, 0f, 0f), wallC)
                addQuad(Vector3(minX, ySill, bz2), Vector3(minX, ySill, wz2), Vector3(minX, yLintel, wz2), Vector3(minX, yLintel, bz2), Vector3(-1f, 0f, 0f), wallC)
                val wxRecessed = minX + recessD
                addQuad(Vector3(wxRecessed, ySill, wz2), Vector3(wxRecessed, ySill, wz1), Vector3(wxRecessed, yLintel, wz1), Vector3(wxRecessed, yLintel, wz2), Vector3(-1f, 0f, 0f), glassC)
            }
        }

        // 2. MAIN ENTRANCE PORTAL (South Facade, Ground Floor)
        val midX = (minX + maxX) / 2f
        val portalW = 3.8f
        val portalH = 2.7f
        val pX1 = midX - portalW / 2f
        val pX2 = midX + portalW / 2f
        val portalD = 0.35f

        // Recessed Entrance Frame
        addQuad(Vector3(pX2, 0f, maxZ), Vector3(pX2, 0f, maxZ - portalD), Vector3(pX2, portalH, maxZ - portalD), Vector3(pX2, portalH, maxZ), Vector3(-1f, 0f, 0f), portalC)
        addQuad(Vector3(pX1, 0f, maxZ - portalD), Vector3(pX1, 0f, maxZ), Vector3(pX1, portalH, maxZ), Vector3(pX1, portalH, maxZ - portalD), Vector3(1f, 0f, 0f), portalC)
        addQuad(Vector3(pX2, portalH, maxZ - portalD), Vector3(pX1, portalH, maxZ - portalD), Vector3(pX1, portalH, maxZ), Vector3(pX2, portalH, maxZ), Vector3(0f, -1f, 0f), portalC)
        // Glass Entrance Door (Recessed)
        addQuad(Vector3(pX2, 0f, maxZ - portalD), Vector3(pX1, 0f, maxZ - portalD), Vector3(pX1, portalH, maxZ - portalD), Vector3(pX2, portalH, maxZ - portalD), Vector3(0f, 0f, 1f), glassC)

        // 3. ROOF SURFACE & PERIMETER SAFETY PARAPET (16.0m to 16.9m)
        // Flat roof surface (inset by 0.4m)
        val pThickness = 0.4f
        val rX1 = minX + pThickness
        val rX2 = maxX - pThickness
        val rZ1 = minZ + pThickness
        val rZ2 = maxZ - pThickness
        addQuad(Vector3(rX1, roofH, rZ1), Vector3(rX2, roofH, rZ1), Vector3(rX2, roofH, rZ2), Vector3(rX1, roofH, rZ2), Vector3(0f, 1f, 0f), roofC)

        // Outer Parapet Walls
        val pTop = roofH + parapetH
        addQuad(Vector3(minX, roofH, minZ), Vector3(maxX, roofH, minZ), Vector3(maxX, pTop, minZ), Vector3(minX, pTop, minZ), Vector3(0f, 0f, -1f), parapetC)
        addQuad(Vector3(maxX, roofH, maxZ), Vector3(minX, roofH, maxZ), Vector3(minX, pTop, maxZ), Vector3(maxX, pTop, maxZ), Vector3(0f, 0f, 1f), parapetC)
        addQuad(Vector3(maxX, roofH, minZ), Vector3(maxX, roofH, maxZ), Vector3(maxX, pTop, maxZ), Vector3(maxX, pTop, minZ), Vector3(1f, 0f, 0f), parapetC)
        addQuad(Vector3(minX, roofH, maxZ), Vector3(minX, roofH, minZ), Vector3(minX, pTop, minZ), Vector3(minX, pTop, maxZ), Vector3(-1f, 0f, 0f), parapetC)

        // Parapet Coping (Top Cap)
        addQuad(Vector3(minX, pTop, minZ), Vector3(maxX, pTop, minZ), Vector3(rX2, pTop, rZ1), Vector3(rX1, pTop, rZ1), Vector3(0f, 1f, 0f), spandrelC)
        addQuad(Vector3(maxX, pTop, maxZ), Vector3(minX, pTop, maxZ), Vector3(rX1, pTop, rZ2), Vector3(rX2, pTop, rZ2), Vector3(0f, 1f, 0f), spandrelC)
        addQuad(Vector3(maxX, pTop, minZ), Vector3(maxX, pTop, maxZ), Vector3(rX2, pTop, rZ2), Vector3(rX2, pTop, rZ1), Vector3(0f, 1f, 0f), spandrelC)
        addQuad(Vector3(minX, pTop, maxZ), Vector3(minX, pTop, minZ), Vector3(rX1, pTop, rZ1), Vector3(rX1, pTop, rZ2), Vector3(0f, 1f, 0f), spandrelC)

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
