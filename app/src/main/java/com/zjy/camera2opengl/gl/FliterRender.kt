package com.zjy.camera2opengl.gl

import android.opengl.GLES20
import com.zjy.camera2opengl.R
import com.zjy.camera2opengl.utils.GLUtil

class FliterRender {

    companion object {
        // Attribute constants.
        private const val POSITION_ATTRIBUTE = "a_Position"
        private const val TEXTURE_COORDINATE_ATTRIBUTE = "a_TextureCoordinate"

        // Uniform constants.
        private const val TEXTURE_SAMPLER_UNIFORM = "u_TextureSampler"
        private const val LOOKUP_TABLE = "u_LookupTable"
        private const val INTENSITY = "u_Intensity"
    }

    var program: Int = 0
    // Uniform locations
    var uTextureSamplerLocation = -1
    var uLookupTableLocation = -1
    var uIntensityLocation = -1

    // Attribute locations
    var aPositionLocation = -1
    var aTextureCoordinateLocation = -1

    var lookupTableId = -1

    fun initProgram() {
        program = GLUtil.createAndLinkProgram(R.raw.vertex_filter, R.raw.fragment_filter)

        initAttribute()
    }

    fun initAttribute() {
        aPositionLocation = GLES20.glGetAttribLocation(program, POSITION_ATTRIBUTE)
        aTextureCoordinateLocation = GLES20.glGetAttribLocation(program, TEXTURE_COORDINATE_ATTRIBUTE)

        // Retrieve uniform locations for the shader program
        uTextureSamplerLocation = GLES20.glGetUniformLocation(program, TEXTURE_SAMPLER_UNIFORM)
        uLookupTableLocation = GLES20.glGetUniformLocation(program, LOOKUP_TABLE)
        uIntensityLocation = GLES20.glGetUniformLocation(program, INTENSITY)
    }


}