package com.summitcodeworks.arcore

import android.content.Context
import android.opengl.GLSurfaceView

class CustomGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: GLRenderer

    init {
        // Set OpenGL ES version
        setEGLContextClientVersion(2)

        renderer = GLRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
}