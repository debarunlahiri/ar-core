package com.summitcodeworks.arcore

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class GLModel(private val context: Context, modelPath: String) {

    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    private var program: Int = 0
    private var modelMatrix = FloatArray(16)
    private var projectionMatrix = FloatArray(16)

    // Define your shader source code here
    private val vertexShaderSource = """
        attribute vec4 a_Position;
        uniform mat4 u_ModelMatrix;
        uniform mat4 u_ProjectionMatrix;
        void main() {
            gl_Position = u_ProjectionMatrix * u_ModelMatrix * a_Position;
        }
    """

    private val fragmentShaderSource = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); // Red color
        }
    """

    init {
        // Parse the .glb file manually (replace with actual parsing logic)
        val modelData = parseGLB(context, modelPath)

        // Extract vertex and index data
        vertexBuffer = createFloatBuffer(modelData.vertices)
        indexBuffer = createShortBuffer(modelData.indices)

        // Load shaders
        program = createProgram(vertexShaderSource, fragmentShaderSource)

        // Initialize model matrix
        Matrix.setIdentityM(modelMatrix, 0)
    }

    fun setViewport(width: Int, height: Int) {
        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    fun draw() {
        GLES20.glUseProgram(program)

        // Pass vertex data
        val positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Pass projection and model matrices
        val modelMatrixHandle = GLES20.glGetUniformLocation(program, "u_ModelMatrix")
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)

        val projectionMatrixHandle = GLES20.glGetUniformLocation(program, "u_ProjectionMatrix")
        GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        // Draw the model
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexBuffer.capacity(), GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }

    private fun parseGLB(context: Context, path: String): ModelData {
        val inputStream = context.assets.open(path)
        val byteBuffer = ByteBuffer.wrap(inputStream.readBytes())
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        // Read the header (this is for the .glb format)
        val magic = byteBuffer.int
        if (magic != 0x46546C67) {
            throw IllegalArgumentException("Not a valid GLB file")
        }

        val version = byteBuffer.int
        val length = byteBuffer.int

        // Skip the JSON chunk (the rest of the file contains binary data)
        byteBuffer.position(byteBuffer.position() + 12) // Skip magic + version + length
        val jsonLength = byteBuffer.int
        val jsonType = byteBuffer.int // This should be 0x4E4F534A (JSON)

        val jsonBytes = ByteArray(jsonLength)
        byteBuffer.get(jsonBytes)
        val jsonString = String(jsonBytes)

        // Assuming the JSON contains an array of meshes, extract it and proceed
        // You would normally parse this JSON to extract mesh data
        // Example: Extract vertices and indices manually from the binary data

        val vertices = FloatArray(0) // Replace with actual vertex data
        val indices = ShortArray(0)  // Replace with actual index data

        return ModelData(vertices, indices)
    }

    private fun createFloatBuffer(vertices: FloatArray): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
        val floatBuffer = buffer.asFloatBuffer()
        floatBuffer.put(vertices)
        floatBuffer.position(0)
        return floatBuffer
    }

    private fun createShortBuffer(indices: ShortArray): ShortBuffer {
        val buffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
        val shortBuffer = buffer.asShortBuffer()
        shortBuffer.put(indices)
        shortBuffer.position(0)
        return shortBuffer
    }

    private fun createProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            throw RuntimeException("Error linking program: " + GLES20.glGetProgramInfoLog(program))
        }

        GLES20.glUseProgram(program)
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            throw RuntimeException("Error compiling shader: " + GLES20.glGetShaderInfoLog(shader))
        }

        return shader
    }

    data class ModelData(val vertices: FloatArray, val indices: ShortArray)
}
