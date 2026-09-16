package com.jrm.frap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DesignationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesignation(designation: DesignationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesignations(
        designations: List<DesignationEntity>
    )

    @Query("SELECT * FROM designations ORDER BY designationType ASC")
    suspend fun getAllDesignations(): List<DesignationEntity>

    @Query("SELECT * FROM designations WHERE id = :id LIMIT 1")
    suspend fun getDesignationById(id: Int): DesignationEntity?

    @Query("DELETE FROM designations")
    suspend fun deleteAllDesignations()
}