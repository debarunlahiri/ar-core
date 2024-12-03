package com.summitcodeworks.arcore

import android.content.Context
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.io.File
import java.io.IOException
import javax.microedition.khronos.opengles.GL10

class GLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private var glModel: GLModel? = null

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Draw the model
        glModel?.draw()
    }

    override fun onSurfaceCreated(gl: GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("your_model.glb")

            // Create temporary file
            val tempFile = File.createTempFile("model", ".glb", context.cacheDir)

            // Copy asset to temporary file
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Load model from temporary file
            glModel = GLModel(context, tempFile.absolutePath)

            // Delete temporary file if no longer needed
            tempFile.delete()

        } catch (e: IOException) {
            Log.e("GLRenderer", "Error loading model: ${e.message}")
        }
    }
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        // Adjust the projection matrix
        glModel?.setViewport(width, height)
    }
}