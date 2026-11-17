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
    private val buildingGpuMeshes = mutableListOf<Pair<String, GpuMesh>>()
    private var routeGpuMesh: GpuMesh? = null

    @Volatile
    private var needsRouteMeshRebuild = false

    fun updateNavigationState(
        selectedBuilding: CampusBuilding?,
        route: PedestrianRoute?,
        userLoc: UserLocationState?
    ) {
        this.selectedBuildingId = selectedBuilding?.id
        this.activeRoute = route
        this.userLocation = userLoc
        this.needsRouteMeshRebuild = true

        cameraController.updateTarget(userLoc, route)
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

        // Update First-Person Camera Pose
        val pose = cameraController.tick()
        setupViewMatrix(pose)

        GLES30.glUseProgram(programId)

        // Sunlight source from overhead Southeast
        GLES30.glUniform3f(uLightPosHandle, 200f, 350f, -150f)

        // 1. Draw Ground
        groundGpuMesh?.let { drawMesh(it) }

        // 2. Draw Sports Grounds (Cricket, Soccer, Track, Courts)
        sportsGpuMesh?.let { drawMesh(it) }

        // 3. Draw Walkways
        walkwaysGpuMesh?.let { drawMesh(it) }

        // 4. Draw Procedural 3D Trees
        treesGpuMesh?.let { drawMesh(it) }

        // 5. Draw Active Route Chevron Strip
        routeGpuMesh?.let { drawMesh(it) }

        // 6. Draw Buildings
        for ((_, mesh) in buildingGpuMeshes) {
            drawMesh(mesh)
        }
    }

    private fun setupViewMatrix(pose: CameraPose) {
        val eyeX = pose.position.x
        val eyeY = pose.position.y
        val eyeZ = pose.position.z

        // Convert yaw & pitch into forward look vector
        val yawRad = Math.toRadians(pose.yawDegrees.toDouble())
        val pitchRad = Math.toRadians(pose.pitchDegrees.toDouble())

        // In our ENU: Forward along 0 yaw is North (-Z)
        val dirX = (kotlin.math.sin(yawRad) * kotlin.math.cos(pitchRad)).toFloat()
        val dirY = kotlin.math.sin(pitchRad).toFloat()
        val dirZ = (-kotlin.math.cos(yawRad) * kotlin.math.cos(pitchRad)).toFloat()

        val centerX = eyeX + dirX
        val centerY = eyeY + dirY
        val centerZ = eyeZ + dirZ

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            centerX, centerY, centerZ,
            0f, 1f, 0f // Up vector
        )
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

        // Buildings
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
        val meshData = RoadMeshGenerator.generateRoutePolylineMesh(pts)
        routeGpuMesh = if (meshData != null) uploadToGpu(meshData) else null
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

        programId = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vs)
            GLES30.glAttachShader(it, fs)
            GLES30.glLinkProgram(it)
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
        }
    }
}
