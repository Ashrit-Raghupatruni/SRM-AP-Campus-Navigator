package com.srmap.campusnavigator.core.rendering3d

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.srmap.campusnavigator.core.geometry3d.*
import com.srmap.campusnavigator.core.location.UserLocationState
import com.srmap.campusnavigator.domain.graph.CampusGraph
import com.srmap.campusnavigator.domain.models.CampusBuilding
import com.srmap.campusnavigator.domain.models.PedestrianRoute
import com.srmap.campusnavigator.domain.navigation3d.CameraPose
import com.srmap.campusnavigator.domain.navigation3d.FirstPersonCameraController
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Campus3DRenderer(
    private val context: Context,
    private val buildings: List<CampusBuilding>,
    private val graph: CampusGraph?,
    private val features: List<com.srmap.campusnavigator.domain.models.CampusFeature> = emptyList()
) : GLSurfaceView.Renderer {

    val cameraController = FirstPersonCameraController()

    private var selectedBuildingId: String? = null
    private var activeRoute: PedestrianRoute? = null
    private var userLocation: UserLocationState? = null

    // OpenGL Matrices
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    // Shaders & Program
    private var programId: Int = 0
    private var uMVPMatrixHandle: Int = -1
    private var uLightPosHandle: Int = -1
    private var aPositionHandle: Int = -1
    private var aNormalHandle: Int = -1
    private var aColorHandle: Int = -1

    // Compiled GPU Meshes
    private class GpuMesh(
        val vertexBuffer: FloatBuffer,
        val normalBuffer: FloatBuffer,
        val colorBuffer: FloatBuffer,
        val indexBuffer: ShortBuffer,
        val indexCount: Int
    )

    private var groundGpuMesh: GpuMesh? = null
    private var sportsGpuMesh: GpuMesh? = null
    private var walkwaysGpuMesh: GpuMesh? = null
    private var treesGpuMesh: GpuMesh? = null
    private var avatarGpuMesh: GpuMesh? = null
    private var complexCanopyGpuMesh: GpuMesh? = null
    private var centralBreezewayGpuMesh: GpuMesh? = null
    private var treeCourtGpuMesh: GpuMesh? = null
    private var gate1GpuMesh: GpuMesh? = null
    private var hostelPlazaGpuMesh: GpuMesh? = null
    private var centralPlazaGpuMesh: GpuMesh? = null
    private val buildingGpuMeshes = mutableListOf<Pair<String, GpuMesh>>()
    private var completedRouteGpuMesh: GpuMesh? = null
    private var remainingRouteGpuMesh: GpuMesh? = null

    private var previousSelectedBuildingId: String? = null
    private var previousRoute: PedestrianRoute? = null
    private var activeSegmentIndex: Int = 0

    // Animated gait cycle
    private var gaitPhase: Float = 0f
    private var lastFrameTimeMs: Long = 0L

    @Volatile
    private var needsRouteMeshRebuild = false
    @Volatile
    private var needsBuildingMeshRebuild = false

    fun updateNavigationState(
        selectedBuilding: CampusBuilding?,
        route: PedestrianRoute?,
        userLoc: UserLocationState?,
        currentSegmentIndex: Int = 0,
        mapMatchedBearing: Float? = null
    ) {
        val bldgChanged = (selectedBuilding?.id != this.selectedBuildingId)
        val routeChanged = (route != this.previousRoute)
        val segChanged = (currentSegmentIndex != this.activeSegmentIndex)

        this.selectedBuildingId = selectedBuilding?.id
        this.activeRoute = route
        this.userLocation = userLoc
        this.activeSegmentIndex = currentSegmentIndex

        if (bldgChanged) {
            this.needsBuildingMeshRebuild = true
        }
        if (routeChanged || segChanged) {
            this.needsRouteMeshRebuild = true
            this.previousRoute = route
        }

        cameraController.updateTarget(userLoc, route, currentSegmentIndex, mapMatchedBearing)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.88f, 0.94f, 0.98f, 1.0f) // SRM Campus Sky Blue
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)

        initShaders()
        buildStaticGeometry()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        // 65 deg vertical FOV perspective projection
        Matrix.perspectiveM(projectionMatrix, 0, 65.0f, aspect, 0.3f, 500.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        if (needsRouteMeshRebuild) {
            rebuildRouteMesh()
            needsRouteMeshRebuild = false
        }
        if (needsBuildingMeshRebuild) {
            rebuildBuildingMeshes()
            needsBuildingMeshRebuild = false
        }

        // Update Camera Pose (supports 1st-person and 3rd-person with smooth transition)
        val pose = cameraController.tick(buildings)
        setupViewMatrix(pose)

        GLES30.glUseProgram(programId)

        // Sunlight source from overhead Southeast
        GLES30.glUniform3f(uLightPosHandle, 200f, 350f, -150f)

        // 1. Draw Ground
        groundGpuMesh?.let { drawMesh(it) }

        // 2. Draw Sports Grounds (Cricket, Soccer, Track, Courts)
        sportsGpuMesh?.let { drawMesh(it) }

        // 3. Draw Walkways & Central Breezeway Spine
        walkwaysGpuMesh?.let { drawMesh(it) }
        centralBreezewayGpuMesh?.let { drawMesh(it) }
        centralPlazaGpuMesh?.let { drawMesh(it) }

        // 4. Draw Procedural 3D Trees
        treesGpuMesh?.let { drawMesh(it) }

        // 5. Draw Dynamic Split Route: Completed (Muted Grey) & Remaining (Glowing Cyan)
        completedRouteGpuMesh?.let { drawMesh(it) }
        remainingRouteGpuMesh?.let { drawMesh(it) }

        // 6. Draw Animated 3D User Avatar & Ground Beacon
        val now = System.currentTimeMillis()
        val dt = if (lastFrameTimeMs > 0L) ((now - lastFrameTimeMs) / 1000f).coerceIn(0.001f, 0.1f) else 0.016f
        lastFrameTimeMs = now

        val speed = userLocation?.speedMps ?: 0f
        val isWalking = speed > 0.4f || activeRoute != null
        val gaitSpeed = if (isWalking) (speed.coerceAtLeast(1.2f) * 4.2f) else 1.8f
        gaitPhase = (gaitPhase + gaitSpeed * dt) % (2f * Math.PI.toFloat())

        if (pose.transitionProgress > 0.05f) {
            val animMesh = com.srmap.campusnavigator.core.geometry3d.UserAvatarMeshGenerator.generateAvatarMesh(gaitPhase, isWalking)
            val tempGpu = uploadToGpu(animMesh)
            drawAvatar(tempGpu, pose)
        }

        // 7. Draw Buildings (Vikram Sarabhai Block, Homi J Bhabha Block, etc.)
        for ((_, mesh) in buildingGpuMeshes) {
            drawMesh(mesh)
        }

        // 8. Draw Monumental Space-Frame Umbrella Canopy & Tree Columns (Overarching V Block & Homi J Bhabha)
        complexCanopyGpuMesh?.let { drawMesh(it) }

        // 9. Draw Tree Court Real Landscape
        treeCourtGpuMesh?.let { drawMesh(it) }

        // 10. Draw Gate 1 Monumental Entrance Complex
        gate1GpuMesh?.let { drawMesh(it) }
    }

    private fun setupViewMatrix(pose: CameraPose) {
        val eyeX = pose.eyePosition.x
        val eyeY = pose.eyePosition.y
        val eyeZ = pose.eyePosition.z

        val targetX: Float
        val targetY: Float
        val targetZ: Float

        if (pose.viewMode == com.srmap.campusnavigator.domain.navigation3d.CameraViewMode.THIRD_PERSON || pose.transitionProgress > 0.1f) {
            // In 3rd-person, the camera looks directly at the user's focus target
            targetX = pose.targetPosition.x
            targetY = pose.targetPosition.y
            targetZ = pose.targetPosition.z
        } else {
            // In 1st-person, the camera looks forward along the look vector from the eye
            val yawRad = Math.toRadians(pose.yawDegrees.toDouble())
            val pitchRad = Math.toRadians(pose.pitchDegrees.toDouble())

            val dirX = (kotlin.math.sin(yawRad) * kotlin.math.cos(pitchRad)).toFloat()
            val dirY = kotlin.math.sin(pitchRad).toFloat()
            val dirZ = (-kotlin.math.cos(yawRad) * kotlin.math.cos(pitchRad)).toFloat()

            targetX = eyeX + dirX
            targetY = eyeY + dirY
            targetZ = eyeZ + dirZ
        }

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            targetX, targetY, targetZ,
            0f, 1f, 0f // Up vector
        )
    }

    private fun drawAvatar(mesh: GpuMesh, pose: CameraPose) {
        Matrix.setIdentityM(modelMatrix, 0)
        // Position at ground target
        Matrix.translateM(modelMatrix, 0, pose.targetPosition.x, 0f, pose.targetPosition.z)
        // Rotate avatar strictly along walking heading
        Matrix.rotateM(modelMatrix, 0, -pose.walkingHeadingDegrees, 0f, 1f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)

        GLES30.glEnableVertexAttribArray(aPositionHandle)
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, mesh.vertexBuffer)

        GLES30.glEnableVertexAttribArray(aNormalHandle)
        GLES30.glVertexAttribPointer(aNormalHandle, 3, GLES30.GL_FLOAT, false, 0, mesh.normalBuffer)

        GLES30.glEnableVertexAttribArray(aColorHandle)
        GLES30.glVertexAttribPointer(aColorHandle, 4, GLES30.GL_FLOAT, false, 0, mesh.colorBuffer)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES30.glDisableVertexAttribArray(aPositionHandle)
        GLES30.glDisableVertexAttribArray(aNormalHandle)
        GLES30.glDisableVertexAttribArray(aColorHandle)
    }

    private fun drawMesh(mesh: GpuMesh) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)

        GLES30.glEnableVertexAttribArray(aPositionHandle)
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, mesh.vertexBuffer)

        GLES30.glEnableVertexAttribArray(aNormalHandle)
        GLES30.glVertexAttribPointer(aNormalHandle, 3, GLES30.GL_FLOAT, false, 0, mesh.normalBuffer)

        GLES30.glEnableVertexAttribArray(aColorHandle)
        GLES30.glVertexAttribPointer(aColorHandle, 4, GLES30.GL_FLOAT, false, 0, mesh.colorBuffer)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES30.glDisableVertexAttribArray(aPositionHandle)
        GLES30.glDisableVertexAttribArray(aNormalHandle)
        GLES30.glDisableVertexAttribArray(aColorHandle)
    }

    private fun buildStaticGeometry() {
        // Ground
        val groundData = GroundMeshGenerator.generateGroundMesh()
        groundGpuMesh = uploadToGpu(groundData)

        // Sports Grounds (Cricket Oval, Soccer Field, Running Track, Courts)
        if (features.isNotEmpty()) {
            SportsGroundMeshGenerator.generateSportsMesh(features)?.let {
                sportsGpuMesh = uploadToGpu(it)
            }
        }

        // Walkways
        graph?.let {
            val walkwayData = RoadMeshGenerator.generateWalkwayMesh(it)
            walkwaysGpuMesh = uploadToGpu(walkwayData)
        }

        // Procedural 3D Trees in forest & landscaped areas
        if (features.isNotEmpty()) {
            val pathPolylines = activeRoute?.path?.let { route ->
                listOf(route.map { LocalCoordinateConverter.toLocal3D(it) })
            } ?: emptyList()

            TreeMeshGenerator.generateTreesMesh(features, pathPolylines)?.let {
                treesGpuMesh = uploadToGpu(it)
            }
        }

        // 3D Third-Person Navigation Avatar & Ground Beacon
        val avatarData = com.srmap.campusnavigator.core.geometry3d.UserAvatarMeshGenerator.generateAvatarMesh()
        avatarGpuMesh = uploadToGpu(avatarData)

        // Central Breezeway Pedestrian Spine & Elevated Skywalk Bridges
        val breezewayData = com.srmap.campusnavigator.core.geometry3d.CentralBreezewayGenerator.generateBreezewayMesh()
        centralBreezewayGpuMesh = uploadToGpu(breezewayData)

        // Monumental Space-Frame Umbrella Canopy & Slender Tree Columns
        val canopyData = com.srmap.campusnavigator.core.geometry3d.AcademicComplexCanopyGenerator.generateCanopyMesh()
        complexCanopyGpuMesh = uploadToGpu(canopyData)

        // Tree Court Mature Centerpiece Shade Tree, Planter Ring & Tiered Boxwood Hedges
        val treeCourtData = com.srmap.campusnavigator.core.geometry3d.TreeCourtMeshGenerator.generateTreeCourtMesh()
        treeCourtGpuMesh = uploadToGpu(treeCourtData)

        // Gate 1 Monumental Entrance Complex: 42m Canopy, 16.5m Sculptural Fin & Security Cabin
        val gate1Data = com.srmap.campusnavigator.core.geometry3d.Gate1EntranceMeshGenerator.generateGate1Mesh()
        gate1GpuMesh = uploadToGpu(gate1Data)

        // Central Sandstone Pedestrian Plaza & Avenue (Between SR Block & CV Raman)
        val centralPlazaData = com.srmap.campusnavigator.core.geometry3d.CentralPlazaMeshGenerator.generatePlazaMesh()
        centralPlazaGpuMesh = uploadToGpu(centralPlazaData)

        // Buildings
        buildingGpuMeshes.clear()
        for (bldg in buildings) {
            val isHighlighted = (bldg.id == selectedBuildingId)
            val meshData = BuildingMeshGenerator.generateBuildingMesh(bldg, isHighlighted) ?: continue
            val gpuMesh = uploadToGpu(meshData)
            buildingGpuMeshes.add(Pair(bldg.id, gpuMesh))
        }
    }

    private fun rebuildBuildingMeshes() {
        buildingGpuMeshes.clear()
        for (bldg in buildings) {
            val isHighlighted = (bldg.id == selectedBuildingId)
            val meshData = BuildingMeshGenerator.generateBuildingMesh(bldg, isHighlighted) ?: continue
            val gpuMesh = uploadToGpu(meshData)
            buildingGpuMeshes.add(Pair(bldg.id, gpuMesh))
        }
    }

    private fun rebuildRouteMesh() {
        val pts = activeRoute?.path ?: emptyList()
        val (completedData, remainingData) = RoadMeshGenerator.generateSplitRouteMesh(pts, activeSegmentIndex)
        completedRouteGpuMesh = if (completedData != null) uploadToGpu(completedData) else null
        remainingRouteGpuMesh = if (remainingData != null) uploadToGpu(remainingData) else null
    }

    private fun uploadToGpu(data: MeshData): GpuMesh {
        return GpuMesh(
            vertexBuffer = data.createVertexBuffer(),
            normalBuffer = data.createNormalBuffer(),
            colorBuffer = data.createColorBuffer(),
            indexBuffer = data.createIndexBuffer(),
            indexCount = data.indices.size
        )
    }

    private fun initShaders() {
        val vertexShaderCode = """
            #version 300 es
            uniform mat4 uMVPMatrix;
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec4 aColor;
            
            out vec4 vColor;
            out vec3 vNormal;
            
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vNormal = aNormal;
                vColor = aColor;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            #version 300 es
            precision mediump float;
            uniform vec3 uLightPos;
            in vec4 vColor;
            in vec3 vNormal;
            out vec4 fragColor;
            
            void main() {
                vec3 norm = normalize(vNormal);
                vec3 lightDir = normalize(uLightPos);
                float diff = max(dot(norm, lightDir), 0.0);
                float ambient = 0.45;
                float lighting = min(ambient + diff * 0.65, 1.0);
                
                fragColor = vec4(vColor.rgb * lighting, vColor.a);
            }
        """.trimIndent()

        val vs = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fs = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES30.glCreateProgram().also { prog ->
            GLES30.glAttachShader(prog, vs)
            GLES30.glAttachShader(prog, fs)
            GLES30.glLinkProgram(prog)

            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val errorLog = GLES30.glGetProgramInfoLog(prog)
                android.util.Log.e("Campus3DRenderer", "OpenGL Program Link Error: $errorLog")
                GLES30.glDeleteProgram(prog)
            }
        }

        uMVPMatrixHandle = GLES30.glGetUniformLocation(programId, "uMVPMatrix")
        uLightPosHandle = GLES30.glGetUniformLocation(programId, "uLightPos")
        aPositionHandle = 0
        aNormalHandle = 1
        aColorHandle = 2
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val errorLog = GLES30.glGetShaderInfoLog(shader)
                android.util.Log.e("Campus3DRenderer", "OpenGL Shader Compile Error ($type): $errorLog")
                GLES30.glDeleteShader(shader)
            }
        }
    }
}
