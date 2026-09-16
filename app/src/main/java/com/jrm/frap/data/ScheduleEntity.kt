package com.jrm.frap.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DesignationEntity::class,
            parentColumns = ["id"],
            childColumns = ["designationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["designationId"])
    ]
)
data class ScheduleEntity(

    @PrimaryKey
    val id: Int,

    val workerId: Int,

    val scheduleDate: String,

    val designationId: Int,

    val createdAt: String?,

    val updatedAt: String?
)