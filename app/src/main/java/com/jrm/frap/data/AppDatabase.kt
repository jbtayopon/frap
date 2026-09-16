package com.jrm.frap.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkerEntity::class,
        EmbeddingEntity::class,
        DesignationEntity::class,
        ScheduleEntity::class,
        AttendanceEntity::class,
        AppSettingsEntity::class,
        DesignationTimeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workerDao(): WorkerDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun designationDao(): DesignationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun designationTimeDao(): DesignationTimeDao
    abstract fun attendanceDisplayDao(): AttendanceDisplayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS designation_time (
                            designationId INTEGER NOT NULL,
                            timeIn TEXT NOT NULL,
                            updatedAt TEXT,
                            PRIMARY KEY(designationId)
                        )
                    """.trimIndent())
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("""
                        ALTER TABLE attendance
                        ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0
                    """.trimIndent())

                    database.execSQL("""
                        ALTER TABLE attendance
                        ADD COLUMN deletedAt TEXT
                    """.trimIndent())
                }
            }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jrm_frap.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
