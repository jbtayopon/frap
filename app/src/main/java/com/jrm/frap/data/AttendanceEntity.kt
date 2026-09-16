package com.jrm.frap.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
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
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workerId: Int,
    val attendanceDate: String,
    val timeIn: String,
    val method: String = "mobile",
    val syncStatus: String = "pending",
    val serverId: Int? = null,
    val eventUuid: String,
    val createdAt: String,

    // 0 = normal, 1 = marked for deletion
    val isDeleted: Boolean = false,

    // Set when this attendance is marked for deletion
    val deletedAt: String? = null
)
