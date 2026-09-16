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
    // ALL ATTENDANCE
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        ORDER BY attendanceDate DESC, timeIn DESC
    """)
    suspend fun getAllAttendance(): List<AttendanceEntity>


    // ======================================================
    // TODAY'S ATTENDANCE
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        WHERE attendanceDate = :attendanceDate
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
        ORDER BY attendanceDate DESC, timeIn DESC
    """)
    suspend fun getAttendanceByWorkerId(
        workerId: Int
    ): List<AttendanceEntity>


    // ======================================================
    // PENDING SYNC
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        WHERE syncStatus = 'pending'
        ORDER BY id ASC
    """)
    suspend fun getPendingAttendance(): List<AttendanceEntity>


    // ======================================================
    // CHECK IF WORKER ALREADY ATTENDED TODAY
    // ======================================================

    @Query("""
        SELECT * FROM attendance
        WHERE workerId = :workerId
        AND attendanceDate = :attendanceDate
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
}