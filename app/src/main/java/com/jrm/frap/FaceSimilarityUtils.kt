package com.jrm.frap

import kotlin.math.sqrt

object FaceSimilarityUtils {

    fun cosineSimilarity(
        embeddingA: FloatArray,
        embeddingB: FloatArray
    ): Float {

        if (embeddingA.size != embeddingB.size) {
            return -1f
        }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in embeddingA.indices) {

            val a = embeddingA[i].toDouble()
            val b = embeddingB[i].toDouble()

            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        if (normA == 0.0 || normB == 0.0) {
            return -1f
        }

        return (
                dotProduct /
                        (sqrt(normA) * sqrt(normB))
                ).toFloat()
    }
}