package com.jrm.frap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkerDao {

    // Insert a completely new worker.
    @Insert
    suspend fun insertWorker(worker: WorkerEntity)

    // IMPORTANT:
    // Do NOT use REPLACE here.
    // REPLACE can delete the existing parent worker row,
    // which can cascade-delete AttendanceEntity records.
    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("""
        SELECT * 
        FROM workers
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getWorkerById(
        id: Int
    ): WorkerEntity?

    @Query("""
        SELECT *
        FROM workers
        WHERE memberId = :memberId
        LIMIT 1
    """)
    suspend fun getWorkerByMemberId(
        memberId: Int
    ): WorkerEntity?

    @Query("""
        SELECT *
        FROM workers
        ORDER BY lastName ASC, firstName ASC
    """)
    suspend fun getAllWorkers(): List<WorkerEntity>

    @Query("""
        DELETE FROM workers
    """)
    suspend fun deleteAllWorkers()
}