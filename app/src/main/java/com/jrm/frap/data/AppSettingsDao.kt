package com.jrm.frap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(
        setting: AppSettingsEntity
    )

    @Query("""
        SELECT * FROM app_settings
        WHERE `key` = :key
        LIMIT 1
    """)
    suspend fun getSetting(
        key: String
    ): AppSettingsEntity?

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAllSettings()
}