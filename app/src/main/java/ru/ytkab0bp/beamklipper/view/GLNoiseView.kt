package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import androidx.core.graphics.ColorUtils
import ru.ytkab0bp.beamklipper.utils.ViewUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLNoiseView(context: Context) : GLSurfaceView(context) {
    companion object {
        private const val COORDINATES_PER_VERTEX = 2
        private val QUADRANT_COORDINATES = floatArrayOf(
            -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f
        )
        private val TEXTURE_COORDINATES = floatArrayOf(
            1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f
        )
        private val DRAW_ORDER = shortArrayOf(
            0, 1, 2, 0, 2, 3
        )
    }

    private val quadrantCoordinatesBuffer = ByteBuffer.allocateDirect(QUADRANT_COORDINATES.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(QUADRANT_COORDINATES).also { it.position(0) }

    private val textureCoordinatesBuffer = ByteBuffer.allocateDirect(TEXTURE_COORDINATES.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(TEXTURE_COORDINATES).also { it.position(0) }

    private val drawOrderBuffer = ByteBuffer.allocateDirect(DRAW_ORDER.size * 4)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .put(DRAW_ORDER).also { it.position(0) }

    private val shadersManager = GLShadersManager()
    private var created = false
    private var time = 0f
    private var lastDraw = 0L

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.RGBA_8888)
        setRenderer(object : Renderer {
            override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {}

            override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
                if (created) onDestroy()
                glViewport(0, 0, width, height)
                onCreate(width, height)
            }

            override fun onDrawFrame(gl: GL10) {
                if (!isAttachedToWindow) return
                val dt = Math.min(System.currentTimeMillis() - lastDraw, 16)
                lastDraw = System.currentTimeMillis()
                time += dt / 1000f

                glBindFramebuffer(GL_FRAMEBUFFER, 0)
                glClearColor(0f, 0f, 0f, 0f)
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

                val shader = shadersManager.get(GLShadersManager.KEY_INTRO)
                shader.startUsing()
                val topColor = ViewUtils.resolveColor(context, android.R.attr.colorAccent)
                val bottomColor = ColorUtils.blendARGB(
                    ViewUtils.resolveColor(context, android.R.attr.windowBackground),
                    topColor, 0.5f)

                shader.uniformColor4f("top_color", topColor)
                shader.uniformColor4f("bottom_color", bottomColor)
                shader.uniform1f("progress", -0.4f)
                shader.uniform1f("time", time)
                drawTexture()
                shader.stopUsing()
            }
        })
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private fun drawTexture() {
        val shader = shadersManager.getCurrent() ?: return
        val posHandle = shader.getAttribLocation("v_position")
        if (posHandle != -1) {
            glVertexAttribPointer(posHandle, COORDINATES_PER_VERTEX, GL_FLOAT, false,
                COORDINATES_PER_VERTEX * 4, quadrantCoordinatesBuffer)
            glEnableVertexAttribArray(posHandle)
        }
        val texHandle = shader.getAttribLocation("v_tex_coord")
        if (texHandle != -1) {
            glVertexAttribPointer(texHandle, COORDINATES_PER_VERTEX, GL_FLOAT, false,
                COORDINATES_PER_VERTEX * 4, textureCoordinatesBuffer)
            glEnableVertexAttribArray(texHandle)
        }
        glDrawElements(GL_TRIANGLES, DRAW_ORDER.size, GL_UNSIGNED_SHORT, drawOrderBuffer)
        if (posHandle != -1) glDisableVertexAttribArray(posHandle)
        if (texHandle != -1) glDisableVertexAttribArray(texHandle)
    }

    private fun onCreate(width: Int, height: Int) { created = true }
    private fun onDestroy() { created = false; shadersManager.release() }
}
