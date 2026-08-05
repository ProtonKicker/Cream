package ru.ytkab0bp.beamklipper.view

import android.graphics.Color
import android.opengl.GLES20.*

class GLShader(internal val manager: GLShadersManager, vertex: String, fragment: String) {
    private var program: Int
    private var vertex: Int
    private var fragment: Int

    init {
        this.vertex = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(this.vertex, vertex)
        glCompileShader(this.vertex)
        val params = IntArray(1)
        glGetShaderiv(this.vertex, GL_COMPILE_STATUS, params, 0)
        if (params[0] != GL_TRUE) {
            throw IllegalArgumentException("Failed to compile vertex shader: ${glGetShaderInfoLog(this.vertex)}")
        }
        this.fragment = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(this.fragment, fragment)
        glCompileShader(this.fragment)
        glGetShaderiv(this.fragment, GL_COMPILE_STATUS, params, 0)
        if (params[0] != GL_TRUE) {
            throw IllegalArgumentException("Failed to compile fragment shader: ${glGetShaderInfoLog(this.fragment)}")
        }
        program = glCreateProgram()
        glAttachShader(program, this.vertex)
        glAttachShader(program, this.fragment)
        glLinkProgram(program)
        glGetShaderiv(this.fragment, GL_LINK_STATUS, params, 0)
        if (params[0] != GL_TRUE) {
            throw IllegalArgumentException("Failed to link program: ${glGetProgramInfoLog(program)}")
        }
    }

    fun getAttribLocation(name: String): Int = glGetAttribLocation(program, name)

    fun uniform1i(name: String, v: Int) {
        val loc = glGetUniformLocation(program, name)
        if (loc != -1) glUniform1i(loc, v)
    }

    fun uniform1f(name: String, v: Float) {
        val loc = glGetUniformLocation(program, name)
        if (loc != -1) glUniform1f(loc, v)
    }

    fun uniform2f(name: String, a: Float, b: Float) {
        val loc = glGetUniformLocation(program, name)
        if (loc != -1) glUniform2f(loc, a, b)
    }

    fun uniform3f(name: String, a: Float, b: Float, c: Float) {
        val loc = glGetUniformLocation(program, name)
        if (loc != -1) glUniform3f(loc, a, b, c)
    }

    fun uniformColor4f(name: String, color: Int) {
        val loc = glGetUniformLocation(program, name)
        if (loc != -1) glUniform4f(loc,
            Color.red(color) / 255f, Color.green(color) / 255f,
            Color.blue(color) / 255f, Color.alpha(color) / 255f)
    }

    fun uniform4f(name: String, a: Float, b: Float, c: Float, d: Float) {
        val loc = glGetUniformLocation(program, name)
        if (loc != -1) glUniform4f(loc, a, b, c, d)
    }

    fun startUsing() {
        manager.shaderStack.push(this)
        glUseProgram(program)
    }

    fun stopUsing() {
        manager.shaderStack.remove(this)
        if (!manager.shaderStack.isEmpty()) {
            glUseProgram(manager.shaderStack.peek().program)
        } else {
            glUseProgram(0)
        }
    }

    fun release() {
        glDeleteProgram(program)
        glDeleteShader(vertex)
        glDeleteShader(fragment)
    }
}
