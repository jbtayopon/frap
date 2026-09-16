package com.jrm.frap

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.util.Log
import java.nio.FloatBuffer

class FaceEmbeddingEngine(
    private val environment: OrtEnvironment,
    private val session: OrtSession
) {

    companion object {
        private const val TAG = "FRAP_EMBEDDING"

        private const val INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 512
    }

    /**
     * Converts a 112x112 face bitmap into a 512-dimensional
     * face embedding using w600k_mbf.onnx.
     */
    fun getEmbedding(bitmap: Bitmap): FloatArray? {

        return try {

            // ---------------------------------
            // RESIZE TO 112 x 112
            // ---------------------------------

            val resizedBitmap =
                Bitmap.createScaledBitmap(
                    bitmap,
                    INPUT_SIZE,
                    INPUT_SIZE,
                    true
                )

            // ---------------------------------
            // CREATE MODEL INPUT
            //
            // Model expects:
            // [1, 3, 112, 112]
            //
            // RGB channels
            // ---------------------------------

            val inputData =
                FloatArray(
                    1 *
                            3 *
                            INPUT_SIZE *
                            INPUT_SIZE
                )

            var index = 0

            // Channel order: R, G, B
            for (channel in 0 until 3) {

                for (y in 0 until INPUT_SIZE) {

                    for (x in 0 until INPUT_SIZE) {

                        val pixel =
                            resizedBitmap.getPixel(
                                x,
                                y
                            )

                        val red =
                            (pixel shr 16) and 0xFF

                        val green =
                            (pixel shr 8) and 0xFF

                        val blue =
                            pixel and 0xFF

                        val value =
                            when (channel) {

                                0 -> red
                                1 -> green
                                else -> blue
                            }

                        // ArcFace normalization:
                        // [0,255] -> [-1,1]
                        inputData[index++] =
                            (value - 127.5f) / 127.5f
                    }
                }
            }

            // ---------------------------------
            // ONNX TENSOR
            // ---------------------------------

            val inputTensor =
                OnnxTensor.createTensor(
                    environment,
                    FloatBuffer.wrap(inputData),
                    longArrayOf(
                        1,
                        3,
                        INPUT_SIZE.toLong(),
                        INPUT_SIZE.toLong()
                    )
                )

            // ---------------------------------
            // RUN MODEL
            // ---------------------------------

            val inputName =
                session.inputNames.first()

            val inputs =
                mapOf(
                    inputName to inputTensor
                )

            val result =
                session.run(inputs)

            // ---------------------------------
            // GET OUTPUT
            // ---------------------------------

            val output =
                result[0].value

            val embedding =
                when (output) {

                    is Array<*> -> {

                        val first =
                            output[0]

                        if (first is FloatArray) {
                            first
                        } else {
                            null
                        }
                    }

                    is FloatArray -> {
                        output
                    }

                    else -> {
                        null
                    }
                }

            inputTensor.close()
            result.close()

            resizedBitmap.recycle()

            if (
                embedding != null &&
                embedding.size == EMBEDDING_SIZE
            ) {

                var sum = 0.0

                for (value in embedding) {
                    sum += value * value
                }

                val norm = kotlin.math.sqrt(sum)

                if (norm > 0.0) {

                    for (i in embedding.indices) {
                        embedding[i] =
                            (embedding[i] / norm).toFloat()
                    }
                }

                Log.d(
                    TAG,
                    "Embedding L2 norm before normalization: $norm"
                )

                // Verify normalized vector
                var normalizedSum = 0.0

                for (value in embedding) {
                    normalizedSum += value * value
                }

                val normalizedNorm =
                    kotlin.math.sqrt(normalizedSum)

                Log.d(
                    TAG,
                    "Embedding L2 norm after normalization: $normalizedNorm"
                )

                Log.d(
                    TAG,
                    "Embedding generated: ${embedding.size} values"
                )

                Log.d(
                    TAG,
                    "Embedding first 10: ${
                        embedding.take(10).joinToString(", ")
                    }"
                )

                embedding

            } else {

                Log.e(
                    TAG,
                    "Invalid embedding output"
                )

                null
            }

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Embedding generation failed",
                exception
            )

            null
        }
    }
}