package com.jrm.frap.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "embeddings",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workerId"])
    ]
)
data class EmbeddingEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val workerId: Int,

    val embeddingJson: String,

    val updatedAt: String?
)