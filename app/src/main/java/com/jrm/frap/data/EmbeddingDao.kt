package com.jrm.frap.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface EmbeddingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: EmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(embeddings: List<EmbeddingEntity>)

    @Update
    suspend fun updateEmbedding(embedding: EmbeddingEntity)

    @Delete
    suspend fun deleteEmbedding(embedding: EmbeddingEntity)

    @Query("SELECT * FROM embeddings")
    suspend fun getAllEmbeddings(): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE workerId = :workerId")
    suspend fun getEmbeddingsByWorkerId(workerId: Int): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE id = :id LIMIT 1")
    suspend fun getEmbeddingById(id: Int): EmbeddingEntity?

    @Query("DELETE FROM embeddings")
    suspend fun deleteAllEmbeddings()
}