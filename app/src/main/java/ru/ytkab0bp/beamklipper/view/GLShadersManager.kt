package ru.ytkab0bp.beamklipper.view

import android.util.Log
import ru.ytkab0bp.beamklipper.KlipperApp
import java.nio.charset.StandardCharsets
import java.util.*

class GLShadersManager {
    companion object {
        const val KEY_INTRO = "beam_intro"
    }

    private val shaders = HashMap<String, GLShader>()
    internal val shaderStack = Stack<GLShader>()

    fun get(key: String): GLShader {
        var shader = shaders[key]
        if (shader == null) {
            var tries = 0
            while (tries <= 30) {
                try {
                    shader = GLShader(this,
                        KlipperApp.INSTANCE.assets.open("shaders/$key.vs").use { it.readBytes().toString(StandardCharsets.UTF_8) },
                        KlipperApp.INSTANCE.assets.open("shaders/$key.fs").use { it.readBytes().toString(StandardCharsets.UTF_8) })
                    break
                } catch (e: Exception) {
                    Log.w("GLShaders", "Failed to load shader $key", e)
                    tries++
                }
            }
            if (shader != null) shaders[key] = shader
        }
        return shader!!
    }

    fun getCurrent(): GLShader? = if (shaderStack.isEmpty()) null else shaderStack.peek()

    fun release() {
        for (shader in shaders.values) shader.release()
        shaders.clear()
        shaderStack.clear()
    }
}
