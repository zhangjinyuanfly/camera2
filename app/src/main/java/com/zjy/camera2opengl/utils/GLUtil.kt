package com.zjy.camera2opengl.utils

import android.opengl.GLES20
import android.util.Log
import com.zjy.camera2opengl.App
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object GLUtil {


    fun createAndLinkProgram(vertextId: Int, fragmentId: Int): Int {
        var vertextShader = loadShader(GLES20.GL_VERTEX_SHADER, loadShaderSource(vertextId))

        var fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, loadShaderSource(fragmentId))

        var program = GLES20.glCreateProgram()

        Log.d("GLUtil","$vertextShader - $fragmentShader - $program")

        GLES20.glAttachShader(program, vertextShader)
        GLES20.glAttachShader(program, fragmentShader)

        GLES20.glLinkProgram(program)

        val linked = intArrayOf(0)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        if(program == 0) {
            GLES20.glDeleteProgram(program)
            return 0
        }

        return program

    }


    fun loadShader(type: Int, shaderRes:String): Int {
        val shaderType = GLES20.glCreateShader(type)
        Log.d("GLUtil","shaderType = $shaderType shaderRes = $shaderRes")
        if(shaderType == 0) {
            return 0
        }

        GLES20.glShaderSource(shaderType, shaderRes)
        GLES20.glCompileShader(shaderType);

        val complited = intArrayOf(0)
        GLES20.glGetShaderiv(shaderType, GLES20.GL_COMPILE_STATUS, complited, 0)
        if(complited[0] == 0) {
            Log.d("GLUtil","complited err ${GLES20.glGetError()} - ${GLES20.glGetShaderInfoLog(shaderType)}")
            GLES20.glDeleteShader(shaderType)
            return 0
        }

        return shaderType
    }


    /*********************** 着色器、程序  */
    fun loadShaderSource(resId: Int): String {
        val res = StringBuilder()
        val `is`: InputStream = App.context.getResources().openRawResource(resId)
        val isr = InputStreamReader(`is`)
        val br = BufferedReader(isr)
        var nextLine: String?
        try {
            while (br.readLine().also { nextLine = it } != null) {
                res.append(nextLine)
                res.append('\n')
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return res.toString()
    }

}