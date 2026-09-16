package com.jrm.frap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

@ExperimentalGetImage
object ImageUtils {

    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {

        return try {

            val image = imageProxy.image
                ?: return null

            val planes = image.planes

            if (planes.size < 3) {
                return null
            }

            // ---------------------------------
            // IMAGE INFORMATION
            // ---------------------------------

            val width = image.width
            val height = image.height

            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            // ---------------------------------
            // CREATE NV21 DATA
            // ---------------------------------

            val nv21 = ByteArray(
                width * height +
                        (width * height / 2)
            )

            var outputOffset = 0

            // ---------------------------------
            // COPY Y PLANE
            // ---------------------------------

            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride

            for (row in 0 until height) {

                val rowStart =
                    row * yRowStride

                for (col in 0 until width) {

                    val index =
                        rowStart + col * yPixelStride

                    nv21[outputOffset++] =
                        yBuffer.get(index)
                }
            }

            // ---------------------------------
            // COPY V + U PLANES
            //
            // NV21 format:
            //
            // YYYYYYYY
            // VUVUVUVU
            // ---------------------------------

            val chromaWidth = width / 2
            val chromaHeight = height / 2

            val uRowStride = uPlane.rowStride
            val uPixelStride = uPlane.pixelStride

            val vRowStride = vPlane.rowStride
            val vPixelStride = vPlane.pixelStride

            for (row in 0 until chromaHeight) {

                val uRowStart =
                    row * uRowStride

                val vRowStart =
                    row * vRowStride

                for (col in 0 until chromaWidth) {

                    val uIndex =
                        uRowStart +
                                col * uPixelStride

                    val vIndex =
                        vRowStart +
                                col * vPixelStride

                    // NV21 = V then U
                    nv21[outputOffset++] =
                        vBuffer.get(vIndex)

                    nv21[outputOffset++] =
                        uBuffer.get(uIndex)
                }
            }

            // ---------------------------------
            // CONVERT NV21 → JPEG
            // ---------------------------------

            val yuvImage =
                YuvImage(
                    nv21,
                    android.graphics.ImageFormat.NV21,
                    width,
                    height,
                    null
                )

            val outputStream =
                ByteArrayOutputStream()

            yuvImage.compressToJpeg(
                Rect(
                    0,
                    0,
                    width,
                    height
                ),
                100,
                outputStream
            )

            val jpegBytes =
                outputStream.toByteArray()

            val bitmap =
                BitmapFactory.decodeByteArray(
                    jpegBytes,
                    0,
                    jpegBytes.size
                )
                    ?: return null

            // ---------------------------------
            // CAMERA ROTATION
            // ---------------------------------

            val rotation =
                imageProxy.imageInfo.rotationDegrees

            if (rotation == 0) {

                bitmap

            } else {

                val matrix =
                    Matrix().apply {
                        postRotate(
                            rotation.toFloat()
                        )
                    }

                val rotatedBitmap =
                    Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height,
                        matrix,
                        true
                    )

                if (rotatedBitmap !== bitmap) {
                    bitmap.recycle()
                }

                rotatedBitmap
            }

        } catch (exception: Exception) {

            exception.printStackTrace()

            null
        }
    }
}