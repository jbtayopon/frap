package com.jrm.frap.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey
    val id: Int,

    val memberId: Int,

    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val suffix: String?,

    val status: String = "active",

    val updatedAt: String?
)