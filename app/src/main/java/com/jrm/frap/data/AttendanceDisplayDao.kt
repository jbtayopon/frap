package com.jrm.frap.data

import androidx.room.Dao
import androidx.room.Query

data class AttendanceDisplayRow(
    val attendanceId: Int,
    val workerId: Int,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val suffix: String?,
    val designationId: Int?,
    val designationName: String?,
    val timeIn: String
)

@Dao
interface AttendanceDisplayDao {

    @Query("""
        SELECT
            a.id AS attendanceId,
            a.workerId AS workerId,
            w.firstName AS firstName,
            w.middleName AS middleName,
            w.lastName AS lastName,
            w.suffix AS suffix,
            s.designationId AS designationId,
            COALESCE(
                d.designationType,
                'No Schedule'
            ) AS designationName,
            a.timeIn AS timeIn

        FROM attendance a

        INNER JOIN workers w
            ON w.id = a.workerId

        LEFT JOIN schedules s
            ON s.workerId = a.workerId
            AND s.scheduleDate = a.attendanceDate

        LEFT JOIN designations d
            ON d.id = s.designationId

        WHERE a.attendanceDate = :date
        AND a.isDeleted = 0

        ORDER BY a.timeIn DESC
    """)
    suspend fun getTodayAttendanceDisplay(
        date: String
    ): List<AttendanceDisplayRow>
}