package com.zjy.camera2opengl.utils

import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import android.util.Size
import android.view.TextureView
import android.view.ViewGroup
import java.util.Collections


/**
 * @Author yocn
 * @Date 2019/8/2 11:26 AM
 * @ClassName CameraUtil
 */
object CameraUtil {
    private const val TAG = "CameraUtil"
    private const val VERBOSE = false

    //选择sizeMap中大于并且最接近width和height的size
    fun getOptimalSize(sizeMap: Array<Size>, width: Int, height: Int): Size {
        val sizeList: MutableList<Size> = ArrayList()
        for (option in sizeMap) {
            Log.d(TAG, "getOptimalSize = $option")
            if (width > height) {
                if (option.width > width && option.height > height) {
                    sizeList.add(option)
                }
            } else {
                if (option.width > height && option.height > width) {
                    sizeList.add(option)
                }
            }
        }
        return if (sizeList.size > 0) {
            Collections.min(sizeList, object : Comparator<Size> {
                override fun compare(lhs: Size, rhs: Size): Int {
                    return java.lang.Long.signum((lhs.width * lhs.height - rhs.width * rhs.height).toLong())
                }

            })
        } else sizeMap[0]
    }

    fun transTextureView(parentViewGroup: ViewGroup, mPreviewView: TextureView) {
        val parentHeight = parentViewGroup.height
        val parentWidth = parentViewGroup.width
        val tarHeight = mPreviewView.height
        val tarWidth = mPreviewView.width
        Log.d(
            "yocn",
            "parentHeight::$parentHeight parentWidth::$parentWidth tarHeight::$tarHeight tarWidth::$tarWidth"
        )
        if (parentWidth * 1.0f / parentHeight > tarWidth * 1.0f / tarHeight) {
            // parent的宽高比 比 预览的宽高比大, 也就是parent比较宽，预览比较细长，需要移动x轴
            val deltaX = (parentWidth - parentHeight * 1.0f * tarWidth / tarHeight).toInt()
            Log.d("yocn", "deltaX::$deltaX")
        } else {
            val deltaY = (parentHeight - parentWidth * 1.0f * tarHeight / tarWidth).toInt()
            Log.d("yocn", "deltaY::$deltaY")
        }
    }

//    fun transTextureView(mPreviewView: TextureView) {
//        val minus: Int =
//            BaseCameraProvider.TextureViewSize.getWidth() - BaseCameraProvider.ScreenSize.getWidth()
//        mPreviewView.translationX = (-minus / 2).toFloat()
//    }

    const val COLOR_FormatI420 = 1
    const val COLOR_FormatNV21 = 2
    private fun isImageFormatSupported(image: Image): Boolean {
        val format = image.format
        when (format) {
            ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> return true
            else -> {}
        }
        return false
    }

//    fun getYUV(image: Image): YUVData {
//        val data = YUVData()
//        val w = image.width
//        val h = image.height
//        val i420Size = w * h * 3 / 2
//        val planes = image.planes
//        //
//        val i420bytes = ByteArray(i420Size)
//        val ySrcBytes = ByteArray(w * h)
//        val uSrcBytes = ByteArray(w * h / 4)
//        val vSrcBytes = ByteArray(w * h / 4)
//
//        //y分量
//        planes[0].buffer[ySrcBytes]
//        System.arraycopy(ySrcBytes, 0, i420bytes, 0, w * h)
//        //uv分量
//        val pixelStride = planes[1].pixelStride
//        if (pixelStride == 1) {
//            //YYYYYYYYUUVV
//            planes[1].buffer[uSrcBytes]
//            planes[2].buffer[vSrcBytes]
//        } else {
//            //YYYYYYYYUVUV
//            val uvBytes = ByteArray(w * h / 2)
//        }
//        Log.d("-----------------wh->$w/$h")
//        //        System.arraycopy(uSrcBytes, 0, i420bytes1, w * h, w * h / 4);
////        System.arraycopy(vSrcBytes, 0, i420bytes1, w * h * 5 / 4, w * h / 4);
////        BitmapUtil.dumpFile("mnt/sdcard/2.yuv", i420bytes1);
//        return data
//    }

    fun getDataFromImage(image: Image, colorFormat: Int): ByteArray {
        require(!(colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21)) { "only support COLOR_FormatI420 " + "and COLOR_FormatNV21" }
        if (!isImageFormatSupported(image)) {
            throw RuntimeException("can't convert Image to byte array, format " + image.format)
        }
        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        if (VERBOSE) {
//            Log.d("get data from " + planes.size + " planes")
        }
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }

                1 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = width * height
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height + 1
                    outputStride = 2
                }

                2 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = (width * height * 1.25).toInt()
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height
                    outputStride = 2
                }

                else -> {}
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            if (VERBOSE) {
                Log.v(TAG, "pixelStride $pixelStride")
                Log.v(TAG, "rowStride $rowStride")
                Log.v(TAG, "width $width")
                Log.v(TAG, "height $height")
                Log.v(TAG, "buffer size " + buffer.remaining())
            }
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer[data, channelOffset, length]
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
            if (VERBOSE) {
                Log.v(TAG, "Finished reading data from plane $i")
            }
        }
        return data
    }


}