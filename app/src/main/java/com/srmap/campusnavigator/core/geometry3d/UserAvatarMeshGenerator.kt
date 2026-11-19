package com.srmap.campusnavigator.core.geometry3d

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural mesh generator for a stylized 3D Human Stickman Avatar with soft rounded edges.
 * Generates:
 * - Spherical Head with smooth vertex normals
 * - Rounded capsule Torso & articulated limbs in natural walking posture
 * - Soft glowing orientation ground disc with forward direction indicator
 */
object UserAvatarMeshGenerator {

    fun generateAvatarMesh(gaitPhase: Float = 0f, isMoving: Boolean = false): MeshData {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        var currentIndex: Short = 0

        val mintHead = Color(0xFF35C6B0)  // Mint Accent glow for head & hands
        val navyBody = Color(0xFF14243B)  // Midnight Navy sleek body
        val tealJoints = Color(0xFF087E83) // Deep Teal articulated joint accents
        val groundGlow = Color(0xFF35C6B0) // Soft directional ground pulse

        // Helper to add smooth triangles with calculated or provided normals
        fun addTri(
            p1: Vector3, p2: Vector3, p3: Vector3,
            n1: Vector3, n2: Vector3, n3: Vector3,
            c: Color
        ) {
            positions.add(p1.x); positions.add(p1.y); positions.add(p1.z)
            positions.add(p2.x); positions.add(p2.y); positions.add(p2.z)
            positions.add(p3.x); positions.add(p3.y); positions.add(p3.z)

            normals.add(n1.x); normals.add(n1.y); normals.add(n1.z)
            normals.add(n2.x); normals.add(n2.y); normals.add(n2.z)
            normals.add(n3.x); normals.add(n3.y); normals.add(n3.z)

            for (i in 0 until 3) {
                colors.add(c.red); colors.add(c.green); colors.add(c.blue); colors.add(1f)
            }

            indices.add(currentIndex)
            indices.add((currentIndex + 1).toShort())
            indices.add((currentIndex + 2).toShort())
            currentIndex = (currentIndex + 3).toShort()
        }

        // 1. Procedural UV Sphere (for Head and Joint Caps) with smooth per-vertex normals
        fun addSmoothSphere(center: Vector3, radius: Float, rings: Int, sectors: Int, c: Color) {
            val rStep = Math.PI.toFloat() / rings
            val sStep = (2f * Math.PI.toFloat()) / sectors

            for (r in 0 until rings) {
                val phi1 = r * rStep
                val phi2 = (r + 1) * rStep

                for (s in 0 until sectors) {
                    val theta1 = s * sStep
                    val theta2 = (s + 1) * sStep

                    fun getPointAndNormal(phi: Float, theta: Float): Pair<Vector3, Vector3> {
                        val nx = sin(phi) * cos(theta)
                        val ny = cos(phi)
                        val nz = sin(phi) * sin(theta)
                        val pos = Vector3(center.x + radius * nx, center.y + radius * ny, center.z + radius * nz)
                        return pos to Vector3(nx, ny, nz)
                    }

                    val (p1, n1) = getPointAndNormal(phi1, theta1)
                    val (p2, n2) = getPointAndNormal(phi2, theta1)
                    val (p3, n3) = getPointAndNormal(phi2, theta2)
                    val (p4, n4) = getPointAndNormal(phi1, theta2)

                    if (r == 0) {
                        addTri(p1, p2, p3, n1, n2, n3, c)
                    } else if (r == rings - 1) {
                        addTri(p1, p3, p4, n1, n3, n4, c)
                    } else {
                        addTri(p1, p2, p3, n1, n2, n3, c)
                        addTri(p1, p3, p4, n1, n3, n4, c)
                    }
                }
            }
        }

        // 2. Procedural Smooth Capsule / Rounded Cylinder (for Limbs & Torso)
        fun addSmoothCapsule(start: Vector3, end: Vector3, radius: Float, segments: Int, c: Color) {
            val dir = (end - start).normalized()
            // Find perpendicular vectors to construct cylinder ring
            val up = if (kotlin.math.abs(dir.y) < 0.9f) Vector3(0f, 1f, 0f) else Vector3(1f, 0f, 0f)
            val side = Vector3(
                dir.y * up.z - dir.z * up.y,
                dir.z * up.x - dir.x * up.z,
                dir.x * up.y - dir.y * up.x
            ).normalized()
            val normal2 = Vector3(
                dir.y * side.z - dir.z * side.y,
                dir.z * side.x - dir.x * side.z,
                dir.x * side.y - dir.y * side.x
            ).normalized()

            val step = (2f * Math.PI.toFloat()) / segments
            for (i in 0 until segments) {
                val a1 = i * step
                val a2 = (i + 1) * step

                val cos1 = cos(a1); val sin1 = sin(a1)
                val cos2 = cos(a2); val sin2 = sin(a2)

                val n1 = (side * cos1 + normal2 * sin1).normalized()
                val n2 = (side * cos2 + normal2 * sin2).normalized()

                val b1 = start + n1 * radius
                val b2 = start + n2 * radius
                val t1 = end + n1 * radius
                val t2 = end + n2 * radius

                addTri(b1, t1, t2, n1, n1, n2, c)
                addTri(b1, t2, b2, n1, n2, n2, c)
            }

            // Rounded Hemisphere Caps on both ends
            addSmoothSphere(start, radius, 4, segments, c)
            addSmoothSphere(end, radius, 4, segments, c)
        }

        // --- ASSEMBLE 3D STICKMAN HUMAN BODY ---

        // Animated gait variables
        val (armSwing, legSwing, bounceY) = if (isMoving) {
            val arm = (sin(gaitPhase.toDouble()) * 0.28).toFloat()
            val leg = (sin(gaitPhase.toDouble()) * 0.24).toFloat()
            val bounce = (kotlin.math.abs(sin(gaitPhase.toDouble())) * 0.04).toFloat()
            Triple(arm, leg, bounce)
        } else {
            // Gentle idle breathing
            val breath = (sin(gaitPhase.toDouble() * 0.5) * 0.015).toFloat()
            Triple(0f, 0f, breath)
        }

        // A. Head (Smooth rounded sphere at Y = 1.62m, radius 0.16m)
        val headPos = Vector3(0f, 1.60f + bounceY, 0f)
        addSmoothSphere(headPos, 0.16f, rings = 8, sectors = 12, mintHead)

        // B. Torso (Vertical capsule from neck Y = 1.40m down to pelvis Y = 0.95m, radius 0.13m)
        val neckPos = Vector3(0f, 1.40f + bounceY, 0f)
        val pelvisPos = Vector3(0f, 0.95f + bounceY, 0f)
        addSmoothCapsule(pelvisPos, neckPos, radius = 0.12f, segments = 8, navyBody)

        // C. Left Arm (Animated swing)
        val leftShoulder = Vector3(-0.20f, 1.34f + bounceY, 0f)
        val leftElbow = Vector3(-0.25f, 1.12f + bounceY, -armSwing * 0.6f)
        val leftHand = Vector3(-0.28f, 0.90f + bounceY, -armSwing)
        addSmoothCapsule(leftShoulder, leftElbow, radius = 0.05f, segments = 6, tealJoints)
        addSmoothCapsule(leftElbow, leftHand, radius = 0.045f, segments = 6, mintHead)

        // D. Right Arm (Opposite swing)
        val rightShoulder = Vector3(0.20f, 1.34f + bounceY, 0f)
        val rightElbow = Vector3(0.25f, 1.12f + bounceY, armSwing * 0.6f)
        val rightHand = Vector3(0.28f, 0.90f + bounceY, armSwing)
        addSmoothCapsule(rightShoulder, rightElbow, radius = 0.05f, segments = 6, tealJoints)
        addSmoothCapsule(rightElbow, rightHand, radius = 0.045f, segments = 6, mintHead)

        // E. Left Leg (Stepping forward with legSwing)
        val leftHip = Vector3(-0.10f, 0.94f + bounceY, 0f)
        val leftKnee = Vector3(-0.11f, 0.52f, -legSwing * 0.5f)
        val leftFoot = Vector3(-0.12f, 0.08f, -legSwing)
        addSmoothCapsule(leftHip, leftKnee, radius = 0.06f, segments = 6, navyBody)
        addSmoothCapsule(leftKnee, leftFoot, radius = 0.055f, segments = 6, tealJoints)

        // F. Right Leg (Trailing backward)
        val rightHip = Vector3(0.10f, 0.94f + bounceY, 0f)
        val rightKnee = Vector3(0.11f, 0.52f, legSwing * 0.5f)
        val rightFoot = Vector3(0.12f, 0.08f, legSwing)
        addSmoothCapsule(rightHip, rightKnee, radius = 0.06f, segments = 6, navyBody)
        addSmoothCapsule(rightKnee, rightFoot, radius = 0.055f, segments = 6, tealJoints)

        // G. Soft Directional Ground Disc with Forward Chevron (Y = 0.03m on ground)
        val ringOuter = 0.75f
        val ringInner = 0.55f
        val discY = 0.03f
        val ringSegs = 16
        val step = (2f * Math.PI.toFloat()) / ringSegs

        for (i in 0 until ringSegs) {
            val a1 = i * step
            val a2 = (i + 1) * step

            val o1 = Vector3(cos(a1) * ringOuter, discY, sin(a1) * ringOuter)
            val o2 = Vector3(cos(a2) * ringOuter, discY, sin(a2) * ringOuter)
            val i1 = Vector3(cos(a1) * ringInner, discY, sin(a1) * ringInner)
            val i2 = Vector3(cos(a2) * ringInner, discY, sin(a2) * ringInner)
            val nUp = Vector3(0f, 1f, 0f)

            addTri(i1, o1, o2, nUp, nUp, nUp, groundGlow)
            addTri(i1, o2, i2, nUp, nUp, nUp, groundGlow)
        }

        // Forward directional pointer chevron on the disc (pointing along -Z)
        val tip = Vector3(0f, discY + 0.01f, -0.95f)
        val leftBase = Vector3(-0.25f, discY + 0.01f, -0.65f)
        val rightBase = Vector3(0.25f, discY + 0.01f, -0.65f)
        val nUp = Vector3(0f, 1f, 0f)
        addTri(tip, leftBase, rightBase, nUp, nUp, nUp, mintHead)

        return MeshData(
            positions = positions.toFloatArray(),
            normals = normals.toFloatArray(),
            colors = colors.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
}
