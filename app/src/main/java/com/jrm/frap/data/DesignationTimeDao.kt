package com.jrm.frap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class DesignationOption(
    val id: Int,
    val name: String
)

@Dao
interface DesignationTimeDao {

    // ======================================================
    // DESIGNATIONS
    // ======================================================

    @Query("""
        SELECT
            id,
            designationType AS name
        FROM designations
        ORDER BY designationType ASC
    """)
    suspend fun getActiveDesignations():
            List<DesignationOption>


    // ======================================================
    // GET ONE DESIGNATION TIME
    // ======================================================

    @Query("""
        SELECT *
        FROM designation_time
        WHERE designationId = :designationId
        LIMIT 1
    """)
    suspend fun getTimeForDesignation(
        designationId: Int
    ): DesignationTimeEntity?


    // ======================================================
    // GET ALL TIMES
    // ======================================================

    @Query("""
        SELECT *
        FROM designation_time
        ORDER BY designationId ASC
    """)
    suspend fun getAllDesignationTimes():
            List<DesignationTimeEntity>


    // ======================================================
    // SAVE
    // ======================================================

    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun saveDesignationTime(
        item: DesignationTimeEntity
    )


    // ======================================================
    // DELETE
    // ======================================================

    @Query("""
        DELETE FROM designation_time
        WHERE designationId = :designationId
    """)
    suspend fun deleteDesignationTime(
        designationId: Int
    )
}