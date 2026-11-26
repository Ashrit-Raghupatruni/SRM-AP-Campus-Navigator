package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import com.srmap.campusnavigator.domain.building3d.SRBlockSpec

/**
 * Procedural Generator for the Central Pedestrian Plaza & Landscaped Court
 * between SR Block and CV Raman Block, accurately modeled from photographic
 * evidence in IMG_20261125_115637.jpg and IMG_20261125_115642.jpg.
 *
 * Ground-Truth Elements Captured:
 * 1. Warm sandstone interlocking concrete pavers (#DFD9CC / #D5CEC1) spanning ~28m width.
 * 2. Black & white alternating safety perimeter kerbs.
 * 3. Raised rectangular concrete planter beds with rich red/green shrubbery.
 * 4. Dense linear avenue of pagoda-style tiered False Ashoka trees (Bucida spinosa).
 * 5. Central geometric granite water feature / monument basin.
 */
object CentralPlazaMeshGenerator {

    // Plaza Coordinates in Local ENU (spanning corridor between SR Block and CV Raman)
    // SR Block East Edge is approx X in [-4.6, 52.0], Z in [25.7, 138.2]
    // CV Raman Block North Edge is at Z ~ 209.0, X in [-125.0, -40.0]
    // The main pedestrian plaza lies in X in [0.0, 48.0], Z in [138.0, 206.0]
    private const val PLAZA_MIN_X = -5.0f
    private const val PLAZA_MAX_X = 46.0f
    private const val PLAZA_MIN_Z = 138.0f
    private const val PLAZA_MAX_Z = 208.0f
    private const val PLAZA_Y = 0.045f // Slightly elevated above ground plane to prevent Z-fighting

    // Authentic Material Colors from Photos
    private val COLOR_SANDSTONE_PAVER = Color(0xFFDFD9CC)
    private val COLOR_PAVER_ACCENT = Color(0xFFCFC7B8)
    private val COLOR_KERB_BLACK = Color(0xFF1E293B)
    private val COLOR_KERB_WHITE = Color(0xFFF8FAFC)
    private val COLOR_PLANTER_WALL = Color(0xFFE2E8F0)
    private val COLOR_SOIL = Color(0xFF3E2723)
    private val COLOR_SHRUB_RED = Color(0xFF991B1B) // Cordyline / Acalypha red foliage
    private val COLOR_SHRUB_GREEN = Color(0xFF15803D)
    private val COLOR_TREE_TRUNK = Color(0xFF4A3E38)
    private val COLOR_TREE_CANOPY_TIER = Color(0xFF2E7D32)
    private val COLOR_FOUNTAIN_GRANITE = Color(0xFF334155)
    private val COLOR_FOUNTAIN_WATER = Color(0xFF0284C7)

    fun generatePlazaMesh(): MeshData {
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

        fun addBox(minP: Vector3, maxP: Vector3, c: Color) {
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

        // 1. MAIN WIDE SANDSTONE PAVER CONCOURSE
        addQuad(
            Vector3(PLAZA_MIN_X, PLAZA_Y, PLAZA_MIN_Z),
            Vector3(PLAZA_MAX_X, PLAZA_Y, PLAZA_MIN_Z),
            Vector3(PLAZA_MAX_X, PLAZA_Y, PLAZA_MAX_Z),
            Vector3(PLAZA_MIN_X, PLAZA_Y, PLAZA_MAX_Z),
            Vector3(0f, 1f, 0f),
            COLOR_SANDSTONE_PAVER
        )

        // Subtle geometric paver band accents
        var bz = PLAZA_MIN_Z + 10.0f
        while (bz < PLAZA_MAX_Z) {
            addQuad(
                Vector3(PLAZA_MIN_X, PLAZA_Y + 0.005f, bz),
                Vector3(PLAZA_MAX_X, PLAZA_Y + 0.005f, bz),
                Vector3(PLAZA_MAX_X, PLAZA_Y + 0.005f, bz + 1.2f),
                Vector3(PLAZA_MIN_X, PLAZA_Y + 0.005f, bz + 1.2f),
                Vector3(0f, 1f, 0f),
                COLOR_PAVER_ACCENT
            )
            bz += 14.0f
        }

        // 2. BLACK & WHITE STRIPED PERIMETER SAFETY KERBS (Eastern Road Edge)
        val kerbX = PLAZA_MAX_X
        val kerbW = 0.45f
        val kerbH = 0.22f
        var kz = PLAZA_MIN_Z
        var isWhite = false
        while (kz < PLAZA_MAX_Z) {
            val kEnd = minOf(kz + 2.0f, PLAZA_MAX_Z)
            val c = if (isWhite) COLOR_KERB_WHITE else COLOR_KERB_BLACK
            addBox(
                Vector3(kerbX, 0f, kz),
                Vector3(kerbX + kerbW, kerbH, kEnd),
                c
            )
            isWhite = !isWhite
            kz += 2.0f
        }

        // 3. RAISED PLANTER ISLANDS WITH DUAL-TONE SHRUB BEDS
        // Symmetrically positioned beds down the center-left plaza matching Photo 115642
        val planterZOffsets = floatArrayOf(146.0f, 162.0f, 178.0f, 194.0f)
        val planterX = 14.0f
        val planterW = 7.0f
        val planterL = 3.8f
        val planterH = 0.5f

        for (pz in planterZOffsets) {
            // Concrete retaining box
            addBox(
                Vector3(planterX, 0f, pz),
                Vector3(planterX + planterW, planterH, pz + planterL),
                COLOR_PLANTER_WALL
            )
            // Rich soil top
            addBox(
                Vector3(planterX + 0.3f, planterH - 0.05f, pz + 0.3f),
                Vector3(planterX + planterW - 0.3f, planterH, pz + planterL - 0.3f),
                COLOR_SOIL
            )
            // Lush shrub foliage (mounded red-and-green tropical vegetation)
            val shrubC = if ((pz.toInt() % 2) == 0) COLOR_SHRUB_RED else COLOR_SHRUB_GREEN
            addBox(
                Vector3(planterX + 0.5f, planterH, pz + 0.5f),
                Vector3(planterX + planterW - 0.5f, planterH + 0.65f, pz + planterL - 0.5f),
                shrubC
            )
        }

        // 4. CENTRAL GEOMETRIC WATER FEATURE / FOUNTAIN BASIN (Photo 115642)
        val fMinX = 12.0f
        val fMaxX = 23.0f
        val fMinZ = 169.0f
        val fMaxZ = 175.0f
        val fHeight = 0.65f
        // Dark granite border coping
        addBox(Vector3(fMinX, 0f, fMinZ), Vector3(fMaxX, fHeight, fMaxZ), COLOR_FOUNTAIN_GRANITE)
        // Reflective water surface inside
        addQuad(
            Vector3(fMinX + 0.6f, fHeight - 0.08f, fMinZ + 0.6f),
            Vector3(fMaxX - 0.6f, fHeight - 0.08f, fMinZ + 0.6f),
            Vector3(fMaxX - 0.6f, fHeight - 0.08f, fMaxZ - 0.6f),
            Vector3(fMinX + 0.6f, fHeight - 0.08f, fMaxZ - 0.6f),
            Vector3(0f, 1f, 0f),
            COLOR_FOUNTAIN_WATER
        )

        // 5. LINEAR AVENUE OF TIERED FALSE ASHOKA / PAGODA TREES (Along East Kerb Edge)
        val treeX = PLAZA_MAX_X - 3.5f
        var tz = PLAZA_MIN_Z + 4.0f
        while (tz < PLAZA_MAX_Z - 3.0f) {
            // Slender straight trunk
            val trunkRadius = 0.22f
            val trunkH = 5.5f
            addBox(
                Vector3(treeX - trunkRadius, 0f, tz - trunkRadius),
                Vector3(treeX + trunkRadius, trunkH, tz + trunkRadius),
                COLOR_TREE_TRUNK
            )

            // Multi-tiered umbrella/pagoda foliage layers (3 horizontal tiers)
            // Tier 1 (Lower)
            addBox(
                Vector3(treeX - 2.8f, 2.5f, tz - 2.8f),
                Vector3(treeX + 2.8f, 3.2f, tz + 2.8f),
                COLOR_TREE_CANOPY_TIER
            )
            // Tier 2 (Middle)
            addBox(
                Vector3(treeX - 2.1f, 3.8f, tz - 2.1f),
                Vector3(treeX + 2.1f, 4.4f, tz + 2.1f),
                COLOR_TREE_CANOPY_TIER
            )
            // Tier 3 (Crown)
            addBox(
                Vector3(treeX - 1.4f, 5.0f, tz - 1.4f),
                Vector3(treeX + 1.4f, 6.2f, tz + 1.4f),
                COLOR_TREE_CANOPY_TIER
            )

            tz += 8.5f // Regular spacing matching tree avenue in photos
        }

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
