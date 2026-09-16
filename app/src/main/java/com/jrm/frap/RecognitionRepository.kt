package com.jrm.frap

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jrm.frap.data.AppDatabase
import com.jrm.frap.data.WorkerEntity
import kotlin.math.sqrt

data class RecognitionResult(
    val worker: WorkerEntity,
    val similarity: Float
)

class RecognitionRepository(
    private val database: AppDatabase
) {

    private val gson = Gson()

    // ======================================================
    // RECOGNITION THRESHOLD
    // ======================================================

    companion object {
        const val RECOGNITION_THRESHOLD = 0.45f
    }

    // ======================================================
    // FIND BEST MATCH
    // ======================================================

    suspend fun findBestMatch(
        faceEmbedding: FloatArray
    ): RecognitionResult? {

        val embeddings =
            database.embeddingDao()
                .getAllEmbeddings()

        if (embeddings.isEmpty()) {
            return null
        }

        var bestWorkerId: Int? = null
        var bestSimilarity = -1f

        for (storedEmbedding in embeddings) {

            val storedVector =
                parseEmbedding(
                    storedEmbedding.embeddingJson
                ) ?: continue

            if (storedVector.size != 512) {
                continue
            }

            val similarity =
                cosineSimilarity(
                    faceEmbedding,
                    storedVector
                )

            if (similarity > bestSimilarity) {

                bestSimilarity =
                    similarity

                bestWorkerId =
                    storedEmbedding.workerId
            }
        }

        if (
            bestWorkerId == null ||
            bestSimilarity < RECOGNITION_THRESHOLD
        ) {
            return null
        }

        val worker =
            database.workerDao()
                .getWorkerById(
                    bestWorkerId
                )
                ?: return null

        return RecognitionResult(
            worker = worker,
            similarity = bestSimilarity
        )
    }

    // ======================================================
    // PARSE STORED EMBEDDING
    // ======================================================

    private fun parseEmbedding(
        json: String
    ): FloatArray? {

        return try {

            val type =
                object :
                    TypeToken<List<Float>>() {}.type

            val values: List<Float> =
                gson.fromJson(
                    json,
                    type
                )

            FloatArray(values.size) { index ->
                values[index]
            }

        } catch (exception: Exception) {

            null
        }
    }

    // ======================================================
    // COSINE SIMILARITY
    // ======================================================

    private fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray
    ): Float {

        if (a.size != b.size) {
            return -1f
        }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {

            dotProduct +=
                a[i].toDouble() *
                        b[i].toDouble()

            normA +=
                a[i].toDouble() *
                        a[i].toDouble()

            normB +=
                b[i].toDouble() *
                        b[i].toDouble()
        }

        val denominator =
            sqrt(normA) *
                    sqrt(normB)

        if (denominator == 0.0) {
            return -1f
        }

        return (
                dotProduct / denominator
                ).toFloat()
    }
}