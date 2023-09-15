package com.zjy.camera2opengl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.*
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.activity.ComponentActivity
import com.zjy.camera2opengl.gl.EglHelper
import com.zjy.camera2opengl.utils.CameraUtil
import com.zjy.camera2opengl.view.AutoFitTextureView
import java.util.Arrays

class MainActivity : ComponentActivity() {

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var previewSize: Size? = null
    private lateinit var textureView: AutoFitTextureView
    private var cameraDevice: CameraDevice? = null

    private var eglHelper: EglHelper? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_activity)

        startCameraThread()

        textureView = findViewById(R.id.textureview)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, width: Int, height: Int) {
                setupCamera(width, height)
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
//                configureTransform(p1, p2)
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            }
        }


    }
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = windowManager.defaultDisplay.rotation
        Log.d("zjy", "configureTransform: rotation = $rotation")
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize!!.height,
                viewWidth.toFloat() / previewSize!!.width)
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    private fun startCameraThread() {
        cameraThread = HandlerThread("cameraThread")
        cameraThread.start()
        cameraHandler = Handler(cameraThread.looper)
    }


    @SuppressLint("MissingPermission")
    private fun setupCamera(width:Int, height:Int) {
        val cameraManager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.cameraIdList.forEach { id ->
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)

            if (cameraCharacteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT) {
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.let {
                        previewSize = CameraUtil.getOptimalSize(
                            it.getOutputSizes(SurfaceTexture::class.java),
                            width,
                            height
                        )

                        previewSize?.let {
                            textureView.setAspectRatio(it.height, it.width)
                        }

                        Log.d("zjy", "selectcamera")
                        cameraManager.openCamera(id, cameraCallback, cameraHandler)

                        return@forEach
                    }
            }
        }

    }

    private var cameraCallback = object: CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            Log.d("zjy","open camera")
            this@MainActivity.cameraDevice = cameraDevice
            previewCamera(cameraDevice)
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, p1: Int) {
            cameraDevice.close()
        }

    }

    private fun previewCamera(cameraDevice: CameraDevice) {

        val surfaceTexture = textureView.surfaceTexture
        eglHelper = EglHelper(surfaceTexture)


        previewSize?.let {
            Log.d("previewSize","width = ${it.width} - ${it.height}")
            surfaceTexture?.setDefaultBufferSize(it.height, it.width)
            eglHelper?.setPreViewSize(it)
        }

        val xuanranSurfaceTexture = eglHelper?.getXuanranSurfaceTexture()
        Log.d("zjy","getXuanranSurfaceTexture = $xuanranSurfaceTexture")
        val surface = Surface(xuanranSurfaceTexture)
//        val surface = Surface(surfaceTexture)


        val createCaptureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        createCaptureRequest.addTarget(surface)
        cameraDevice.createCaptureSession(Arrays.asList(surface), object: StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                session.setRepeatingRequest(createCaptureRequest.build(), null, cameraHandler)
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {}

        }, cameraHandler)

//        val sessionConfiguration = SessionConfiguration(SessionConfiguration.SESSION_REGULAR, Arrays.asList(
//            OutputConfiguration(surface)
//        ),
//        Executors.newSingleThreadExecutor(),
//        object: CameraCaptureSession.StateCallback(){
//            override fun onConfigured(p0: CameraCaptureSession) {
//            }
//
//            override fun onConfigureFailed(p0: CameraCaptureSession) {
//            }
//
//        })

//        cameraDevice.createCaptureSession(sessionConfiguration)

    }


}
