package com.jrm.frap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(
        schedules: List<ScheduleEntity>
    )

    @Query("SELECT * FROM schedules ORDER BY scheduleDate ASC")
    suspend fun getAllSchedules(): List<ScheduleEntity>

    @Query("""
        SELECT * FROM schedules
        WHERE workerId = :workerId
        ORDER BY scheduleDate ASC
    """)
    suspend fun getSchedulesByWorkerId(
        workerId: Int
    ): List<ScheduleEntity>

    @Query("""
        SELECT * FROM schedules
        WHERE workerId = :workerId
        AND scheduleDate = :scheduleDate
        LIMIT 1
    """)
    suspend fun getScheduleForDate(
        workerId: Int,
        scheduleDate: String
    ): ScheduleEntity?

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()
}