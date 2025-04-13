// I could not used an normal ImageWriter to draw into a MediaCodec's Input surface.
// Somehow I could not find the correct image/pixel format that works on the emulator and the pixel.
// Therefore I have imported the code from
//     https://www.bigflake.com/mediacodec/
//     https://www.bigflake.com/mediacodec/EncodeAndMuxTest.java.txt
// that contains a ImageWriter based on ELG and GLES. It can draw a simple pattern into a Surface.
// The code was converted from Java to kotlin and slightly modified.

/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.inovex.aosptraining.graphics

import android.opengl.*
import android.opengl.EGLExt.EGL_RECORDABLE_ANDROID
import android.view.Surface
import java.io.Closeable

// ImageWriter based on EGL and GLES
class GlesImageWriter(private val surface: Surface) : Closeable {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    fun setup() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("unable to initialize EGL14")
        }

        // Configure EGL for recording and OpenGL ES 2.0.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(
            eglDisplay, attribList, 0, configs, 0, configs.size,
            numConfigs, 0
        )
        checkEglError("eglCreateContext RGB888+recordable ES2")

        // Configure context for OpenGL ES 2.0.
        val attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            attrib_list, 0
        )
        checkEglError("eglCreateContext")

        // Create a window surface, and attach it to the Surface we received.
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, configs[0], surface,
            surfaceAttribs, 0
        )
        checkEglError("eglCreateWindowSurface")
    }

    /**
     * Discards all resources held by this class, notably the EGL context.  Also releases the
     * Surface that was passed to our constructor.
     */
    override fun close() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        surface.release()
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        checkEglError("eglMakeCurrent")
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    fun swapBuffers(): Boolean {
        val result = EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        checkEglError("eglSwapBuffers")
        if (!result)
             throw RuntimeException("Cannot swap buffers")
        return result
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    fun setPresentationTime(nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
        checkEglError("eglPresentationTimeANDROID")
    }

    /**
     * Checks for EGL errors.  Throws an exception if one is found.
     */
    private fun checkEglError(msg: String) {
        var error: Int
        if (EGL14.eglGetError().also { error = it } != EGL14.EGL_SUCCESS) {
            throw java.lang.RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
        }
    }
}

/**
 * Generates a frame of data using GL commands.  We have an 8-frame animation
 * sequence that wraps around.  It looks like this:
 * <pre>
 * 0 1 2 3
 * 7 6 5 4
</pre> *
 * We draw one of the eight rectangles and leave the rest set to the clear color.
 */
private const val TEST_R0: Int = 0
private const val TEST_G0 = 136
private const val TEST_B0 = 0
private const val TEST_R1 = 236
private const val TEST_G1 = 50
private const val TEST_B1 = 186
fun generateSurfaceFrame(width: Int, height : Int, frameIndexArg: Int) {
    var frameIndex = frameIndexArg
    frameIndex %= 8
    val startX: Int
    val startY: Int
    if (frameIndex < 4) {
        // (0,0) is bottom-left in GL
        startX = frameIndex * (width / 4)
        startY = height / 2
    } else {
        startX = (7 - frameIndex) * (width / 4)
        startY = 0
    }
    GLES20.glClearColor(TEST_R0 / 255.0f, TEST_G0 / 255.0f, TEST_B0 / 255.0f, 1.0f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
    GLES20.glScissor(startX, startY, width / 4, height / 2)
    GLES20.glClearColor(TEST_R1 / 255.0f, TEST_G1 / 255.0f, TEST_B1 / 255.0f, 1.0f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
}
