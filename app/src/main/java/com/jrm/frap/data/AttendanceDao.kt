package com.jrm.frap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AttendanceDao {

    // ======================================================
    // INSERT ATTENDANCE
    // ======================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(
        attendance: AttendanceEntity
    ): Long


    // ======================================================
    // UPDATE ATTENDANCE
    // ======================================================

    @Update
    suspend fun updateAttendance(
        attendance: AttendanceEntity
    )


    // ======================================================
    // ALL ACTIVE ATTENDANCE
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        WHERE isDeleted = 0
        ORDER BY attendanceDate DESC, timeIn DESC
    """)
    suspend fun getAllAttendance(): List<AttendanceEntity>


    // ======================================================
    // TODAY'S ACTIVE ATTENDANCE
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        WHERE attendanceDate = :attendanceDate
        AND isDeleted = 0
        ORDER BY timeIn DESC
    """)
    suspend fun getTodayAttendance(
        attendanceDate: String
    ): List<AttendanceEntity>


    // ======================================================
    // ATTENDANCE BY WORKER
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        WHERE workerId = :workerId
        AND isDeleted = 0
        ORDER BY attendanceDate DESC, timeIn DESC
    """)
    suspend fun getAttendanceByWorkerId(
        workerId: Int
    ): List<AttendanceEntity>


    // ======================================================
    // PENDING NEW ATTENDANCE
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        WHERE syncStatus = 'pending'
        AND isDeleted = 0
        ORDER BY id ASC
    """)
    suspend fun getPendingAttendance(): List<AttendanceEntity>


    // ======================================================
    // PENDING DELETIONS
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        WHERE syncStatus = 'delete_pending'
        AND isDeleted = 1
        ORDER BY id ASC
    """)
    suspend fun getPendingDeletions(): List<AttendanceEntity>


    // ======================================================
    // CHECK IF WORKER ALREADY ATTENDED TODAY
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        WHERE workerId = :workerId
        AND attendanceDate = :attendanceDate
        AND isDeleted = 0
        LIMIT 1
    """)
    suspend fun getAttendanceForDate(
        workerId: Int,
        attendanceDate: String
    ): AttendanceEntity?


    // ======================================================
    // MARK AS SYNCED
    // ======================================================

    @Query("""
        UPDATE attendance
        SET syncStatus = :status,
            serverId = :serverId
        WHERE eventUuid = :eventUuid
    """)
    suspend fun markAsSynced(
        eventUuid: String,
        status: String,
        serverId: Int?
    )


    // ======================================================
    // DELETE PENDING ATTENDANCE
    // ======================================================
    // Used when attendance has NOT yet reached the server.
    // It can safely be removed locally.

    @Query("""
        DELETE FROM attendance
        WHERE id = :attendanceId
        AND syncStatus = 'pending'
    """)
    suspend fun deletePendingAttendance(
        attendanceId: Int
    )


    // ======================================================
    // MARK SYNCED ATTENDANCE FOR DELETION
    // ======================================================
    // Used when attendance already exists on the PC/server.
    //
    // We DO NOT immediately delete the local row.
    // This keeps the serverId available so the deletion
    // can still be synchronized when the connection returns.

    @Query("""
        UPDATE attendance
        SET isDeleted = 1,
            syncStatus = 'delete_pending',
            deletedAt = :deletedAt
        WHERE id = :attendanceId
        AND syncStatus = 'synced'
    """)
    suspend fun markAttendanceForDeletion(
        attendanceId: Int,
        deletedAt: String
    )


    // ======================================================
    // FINALIZE LOCAL DELETION
    // ======================================================
    // Called only after the server confirms the deletion.

    @Query("""
        DELETE FROM attendance
        WHERE id = :attendanceId
    """)
    suspend fun permanentlyDeleteAttendance(
        attendanceId: Int
    )
}