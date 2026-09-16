package com.jrm.frap.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "designations")
data class DesignationEntity(

    @PrimaryKey
    val id: Int,

    val designationType: String,

    val updatedAt: String?
)