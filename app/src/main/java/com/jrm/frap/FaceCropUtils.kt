package com.jrm.frap

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.face.Face

object FaceCropUtils {

    fun cropFace(
        bitmap: Bitmap,
        face: Face
    ): Bitmap? {

        return try {

            val box = face.boundingBox

            // ---------------------------------
            // ADD PADDING AROUND FACE
            // ---------------------------------

            val paddingX =
                (box.width() * 0.20f).toInt()

            val paddingY =
                (box.height() * 0.20f).toInt()

            val left =
                (box.left - paddingX)
                    .coerceAtLeast(0)

            val top =
                (box.top - paddingY)
                    .coerceAtLeast(0)

            val right =
                (box.right + paddingX)
                    .coerceAtMost(bitmap.width)

            val bottom =
                (box.bottom + paddingY)
                    .coerceAtMost(bitmap.height)

            val cropWidth =
                right - left

            val cropHeight =
                bottom - top

            if (
                cropWidth <= 0 ||
                cropHeight <= 0
            ) {
                return null
            }

            Bitmap.createBitmap(
                bitmap,
                left,
                top,
                cropWidth,
                cropHeight
            )

        } catch (exception: Exception) {

            exception.printStackTrace()

            null
        }
    }
}