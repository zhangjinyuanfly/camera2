package com.zjy.camera2opengl.gl

import android.opengl.GLES20
import android.util.Log
import com.zjy.camera2opengl.R
import com.zjy.camera2opengl.utils.GLUtil

class CameraRender {

    companion object {
        // Uniform constants.
        private const val POSITION_ATTRIBUTE = "a_Position"
        private const val TEXTURE_COORDINATE_ATTRIBUTE = "a_TextureCoordinate"

        // Attribute constants.
        private const val TEXTURE_MATRIX_UNIFORM = "u_TextureMatrix"
        private const val TEXTURE_SAMPLER_UNIFORM = "u_TextureSampler"
    }

    var program: Int = 0


    // Uniform locations
    var uTextureMatrixLocation = -1
    var uTextureSamplerLocation = -1

    // Attribute locations
    var aPositionLocation = -1
    var aTextureCoordinateLocation = -1

    fun initProgram() {
        program = GLUtil.createAndLinkProgram(R.raw.vertex_camera, R.raw.fragment_camera)

        initAttribute()
    }

    fun initAttribute() {
        // Retrieve attribute locations for the shader program.
        aPositionLocation = GLES20.glGetAttribLocation(program, POSITION_ATTRIBUTE)
        Log.d("CameraRender","aPositionLocation = $aPositionLocation")
        aTextureCoordinateLocation = GLES20.glGetAttribLocation(program, TEXTURE_COORDINATE_ATTRIBUTE)
        Log.d("CameraRender","aTextureCoordinateLocation = $aTextureCoordinateLocation")
        // Retrieve uniform locations for the shader program
        uTextureMatrixLocation = GLES20.glGetUniformLocation(program, TEXTURE_MATRIX_UNIFORM)
        uTextureSamplerLocation = GLES20.glGetUniformLocation(program, TEXTURE_SAMPLER_UNIFORM)
        Log.d("CameraRender","uTextureMatrixLocation = $uTextureMatrixLocation")
        Log.d("CameraRender","uTextureSamplerLocation = $uTextureSamplerLocation")
    }


}