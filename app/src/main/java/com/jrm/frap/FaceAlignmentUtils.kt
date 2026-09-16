package com.jrm.frap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.sqrt

object FaceAlignmentUtils {

    private const val OUTPUT_SIZE = 112

    /*
     * Standard InsightFace / ArcFace reference landmarks.
     *
     * Order:
     * 1. Left eye
     * 2. Right eye
     * 3. Nose
     * 4. Left mouth
     * 5. Right mouth
     */
    private val referencePoints = arrayOf(
        PointF(38.2946f, 51.6963f),
        PointF(73.5318f, 51.5014f),
        PointF(56.0252f, 71.7366f),
        PointF(41.5493f, 92.3655f),
        PointF(70.7299f, 92.2041f)
    )

    fun alignFace(
        bitmap: Bitmap,
        leftEye: PointF,
        rightEye: PointF,
        nose: PointF,
        leftMouth: PointF,
        rightMouth: PointF
    ): Bitmap? {

        return try {

            val sourcePoints = arrayOf(
                leftEye,
                rightEye,
                nose,
                leftMouth,
                rightMouth
            )

            val matrix =
                createSimilarityMatrix(
                    sourcePoints,
                    referencePoints
                )

            val alignedBitmap =
                Bitmap.createBitmap(
                    OUTPUT_SIZE,
                    OUTPUT_SIZE,
                    Bitmap.Config.ARGB_8888
                )

            val canvas =
                Canvas(alignedBitmap)

            val paint =
                Paint(
                    Paint.ANTI_ALIAS_FLAG or
                            Paint.FILTER_BITMAP_FLAG
                )

            canvas.drawBitmap(
                bitmap,
                matrix,
                paint
            )

            alignedBitmap

        } catch (exception: Exception) {

            exception.printStackTrace()
            null
        }
    }

    /**
     * Creates a 2D similarity transform using
     * all five facial landmarks.
     *
     * Transformation:
     *
     * x' = a*x - b*y + tx
     * y' = b*x + a*y + ty
     */
    private fun createSimilarityMatrix(
        source: Array<PointF>,
        target: Array<PointF>
    ): Matrix {

        var sourceCenterX = 0.0
        var sourceCenterY = 0.0

        var targetCenterX = 0.0
        var targetCenterY = 0.0

        for (i in source.indices) {

            sourceCenterX += source[i].x
            sourceCenterY += source[i].y

            targetCenterX += target[i].x
            targetCenterY += target[i].y
        }

        sourceCenterX /= source.size
        sourceCenterY /= source.size

        targetCenterX /= target.size
        targetCenterY /= target.size

        var numeratorA = 0.0
        var numeratorB = 0.0
        var denominator = 0.0

        for (i in source.indices) {

            val sx =
                source[i].x - sourceCenterX

            val sy =
                source[i].y - sourceCenterY

            val tx =
                target[i].x - targetCenterX

            val ty =
                target[i].y - targetCenterY

            numeratorA +=
                sx * tx +
                        sy * ty

            numeratorB +=
                sx * ty -
                        sy * tx

            denominator +=
                sx * sx +
                        sy * sy
        }

        if (denominator <= 0.0) {
            return Matrix()
        }

        val a =
            numeratorA / denominator

        val b =
            numeratorB / denominator

        val translateX =
            targetCenterX -
                    a * sourceCenterX +
                    b * sourceCenterY

        val translateY =
            targetCenterY -
                    b * sourceCenterX -
                    a * sourceCenterY

        val values = floatArrayOf(
            a.toFloat(),
            (-b).toFloat(),
            translateX.toFloat(),

            b.toFloat(),
            a.toFloat(),
            translateY.toFloat(),

            0f,
            0f,
            1f
        )

        return Matrix().apply {
            setValues(values)
        }
    }
}