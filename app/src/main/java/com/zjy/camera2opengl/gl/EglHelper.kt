package com.zjy.camera2opengl.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.util.Size
import com.zjy.camera2opengl.utils.TextureUtil

class EglHelper(var cameraSurfaceTexture: SurfaceTexture?) {

    companion object {
        private const val TAG = "EglHelper"
        private const val M_INIT = 0
        private const val M_DRAW = 1
        private const val M_SIZE_CHANGE = 2
        private const val M_UN_INIT = 3
    }

    private val vertexData = floatArrayOf(
        -1f,  1f, 0f, 1f,
        -1f, -1f, 0f, 0f,
        1f,  1f, 1f, 1f,
        1f, -1f, 1f, 0f
    )

    val VERTEX_COMPONENT_COUNT = 2
    val COORDINATE_COMPONENT_COUNT = 2
    val STRIDE =
        (VERTEX_COMPONENT_COUNT + COORDINATE_COMPONENT_COUNT) * 4

    private val vertexArray = VertexArray(vertexData)

    private var cameraRender: CameraRender = CameraRender()// program 管理类

    private var filterRender: FliterRender = FliterRender()

    private var textureId: Int = 0
    private var width: Int = 0
    private var height: Int = 0
    private var eglDisplay: EGLDisplay? = null
    private var eglConfig: EGLConfig? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null

    private val mLock = Object()

    var inputSurfaceTexture: SurfaceTexture? = null


    fun getXuanranSurfaceTexture(): SurfaceTexture {
        if(inputSurfaceTexture == null) {
            synchronized(mLock) {
                mLock.wait()
            }
        }
        return inputSurfaceTexture!!
    }


    val mGELThread = HandlerThread("elg-thread")
    var mEGLHandler: Handler? = null


    private val frameBuffer = IntArray(1)
    private val frameTexture = IntArray(1)


    init {
        mGELThread.start()
        mEGLHandler = object:Handler(mGELThread.looper) {

            override fun handleMessage(msg: Message) {
                when(msg.what) {
                    M_INIT->{
                        initEGL()
                    }
                    M_SIZE_CHANGE->{
                        surfaceChange(width, height)
                    }
                    M_DRAW->{
                        drawFrame()
                    }
                }
            }
        }

        mEGLHandler?.sendEmptyMessage(M_INIT)
    }

    private var transformMatrix = FloatArray(16)
    private fun drawFrame() {

//        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        // 画每一帧
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//        GLES20.glClearColor(0f,0.0f,0f,0f)
        inputSurfaceTexture?.updateTexImage()
        inputSurfaceTexture?.getTransformMatrix(transformMatrix)
//        Log.d("zjy","transformMatrix = $transformMatrix")

        // todo zhangjinyuan
        Log.d("zjy","drawFrame.....$width - $height - ${cameraRender.program} - ${filterRender.program} - ${frameTexture[0]}")
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(cameraRender.program)

        GLES20.glUniformMatrix4fv(cameraRender.uTextureMatrixLocation, 1, false, transformMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(cameraRender.uTextureSamplerLocation, 0)
        vertexArray.setVertexAttributePointer(0, cameraRender.aPositionLocation, VERTEX_COMPONENT_COUNT,STRIDE)
        vertexArray.setVertexAttributePointer(2, cameraRender.aTextureCoordinateLocation, COORDINATE_COMPONENT_COUNT, STRIDE)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        vertexArray.disableVertexAttributeArray(cameraRender.aPositionLocation)
        vertexArray.disableVertexAttributeArray(cameraRender.aTextureCoordinateLocation)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)


        // 画目标
        GLES20.glUseProgram(filterRender.program)
        vertexArray.setVertexAttributePointer(0, filterRender.aPositionLocation, VERTEX_COMPONENT_COUNT,STRIDE)
        vertexArray.setVertexAttributePointer(2, filterRender.aTextureCoordinateLocation, COORDINATE_COMPONENT_COUNT, STRIDE)
//        GLES20.glViewport(0, 0, width, height)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTexture[0])
        GLES20.glUniform1i(filterRender.uTextureSamplerLocation, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        vertexArray.disableVertexAttributeArray(filterRender.aPositionLocation)
        vertexArray.disableVertexAttributeArray(filterRender.aTextureCoordinateLocation)

//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
//        GLES20.glViewport(0, 0, width, height)

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)

    }

    fun releaseFrameBufferTexture() {
        Log.d(TAG, "release: ")
        GLES20.glDeleteFramebuffers(1, frameBuffer, 0)
        GLES20.glDeleteTextures(1, frameTexture, 0)
    }

    private fun surfaceChange(width: Int, height: Int) {
        //
        Log.d(TAG, "onSurfaceChanged: width = $width, height = $height - $inputSurfaceTexture")
        if(inputSurfaceTexture == null) {
            inputSurfaceTexture = SurfaceTexture(textureId)
            inputSurfaceTexture?.setOnFrameAvailableListener(listener)
        }

        inputSurfaceTexture?.setDefaultBufferSize(height, width)
        synchronized(mLock) {
            mLock.notify()
        }

        // init  fbo
        GLES20.glViewport(0, 0, width, height)

        createFrameBufferAndTexture()

    }

    private var listener: SurfaceTexture.OnFrameAvailableListener = object :SurfaceTexture.OnFrameAvailableListener{
        override fun onFrameAvailable(p0: SurfaceTexture?) {
            mEGLHandler?.sendMessage(Message.obtain(mEGLHandler, M_DRAW))
        }
    }

    fun initEGL() {

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

        val configAttributes:IntArray =  intArrayOf(
            EGL14.EGL_BUFFER_SIZE, 32,   //颜色缓冲区中所有组成颜色的位数
            EGL14.EGL_ALPHA_SIZE, 8,     //颜色缓冲区中透明度位数
            EGL14.EGL_BLUE_SIZE, 8,      //颜色缓冲区中蓝色位数
            EGL14.EGL_GREEN_SIZE, 8,     //颜色缓冲区中绿色位数
            EGL14.EGL_RED_SIZE, 8,       //颜色缓冲区中红色位数
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,  //渲染窗口支持的布局组成
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,  //EGL 窗口支持的类型
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttributes, 0, configs, 0, configs.size, numConfigs, 0)
        eglConfig = configs[0]

        val surfaceAttributes = intArrayOf( EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, cameraSurfaceTexture, surfaceAttributes, 0)

        if(eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.d("zjy","initEGL = ${EGL14.eglGetError()}")
        }

        val contextAttributes = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )

        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttributes, 0)

        if(eglContext == EGL14.EGL_NO_CONTEXT) {
            throw Exception("eglcontext create fail")
        }

//        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttributes, 0)
        val b = EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        Log.d("zjy","eglMakeCurrent = $b")
        createTextureId()
        createProgram()
    }

    private fun createTextureId() {
        textureId = TextureUtil.createOESTextureObject()
        Log.d("zjy","createTextureId = $textureId")
    }

    private fun createProgram() {
        cameraRender.initProgram()
        filterRender.initProgram()
    }

    fun setPreViewSize(size: Size) {
        this.width = size.height
        this.height =  size.width

        mEGLHandler?.sendEmptyMessage(M_SIZE_CHANGE)


    }

    fun createFrameBufferAndTexture() {
        if(frameTexture[0] > 0) {
            releaseFrameBufferTexture()
        }


        GLES20.glGenFramebuffers(1, frameBuffer, 0)
        GLES20.glGenTextures(1, frameTexture, 0)
        Log.d("zjy","createFrameBufferAndTexture  =   ${frameBuffer[0]} - ${frameTexture[0]}")

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTexture[0])

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameTexture[0], 0)
        val code = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if(code != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.d("zjy","createFrameBufferAndTexture = $code")
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

    }

}