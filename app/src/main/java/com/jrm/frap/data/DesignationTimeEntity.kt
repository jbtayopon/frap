package com.jrm.frap.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "designation_time")
data class DesignationTimeEntity(

    @PrimaryKey
    val designationId: Int,

    val timeIn: String,

    val updatedAt: String?
)