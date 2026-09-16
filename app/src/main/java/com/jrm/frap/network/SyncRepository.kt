package com.jrm.frap.network

import android.util.Log
import com.jrm.frap.data.AppDatabase
import com.jrm.frap.data.DesignationEntity
import com.jrm.frap.data.EmbeddingEntity
import com.jrm.frap.data.ScheduleEntity
import com.jrm.frap.data.WorkerEntity

class SyncRepository(
    private val database: AppDatabase
) {

    // ======================================================
    // FULL BIDIRECTIONAL SYNC
    // ======================================================

    suspend fun sync(
        serverUrl: String
    ): Result<SyncResult> {

        return try {

            Log.d(
                "FRAP_SYNC",
                "Starting bidirectional sync..."
            )

            val api = ApiClient.create(serverUrl)

            // ==================================================
            // 0. VERIFY PC / NGROK IS ACTUALLY REACHABLE
            // ==================================================

            val pingResponse = api.ping()

            if (!pingResponse.isSuccessful || pingResponse.body() == null) {
                return Result.failure(
                    Exception(
                        "PC/ngrok server unavailable. Sync skipped."
                    )
                )
            }

            // ==================================================
            // 1. PHONE → PC : NEW ATTENDANCE
            // ==================================================

            val attendanceResult =
                uploadPendingAttendance(api)

            if (attendanceResult.isFailure) {
                Log.e(
                    "FRAP_SYNC",
                    "Attendance upload failed",
                    attendanceResult.exceptionOrNull()
                )
            }

            // ==================================================
            // 2. PHONE → PC : DELETIONS
            // ==================================================

            val deletionResult =
                uploadPendingDeletions(api)

            if (deletionResult.isFailure) {
                Log.e(
                    "FRAP_SYNC",
                    "Attendance deletion sync failed",
                    deletionResult.exceptionOrNull()
                )
            }

            val attendanceSync =
                attendanceResult.getOrDefault(
                    AttendanceUploadResult(
                        attempted = 0,
                        synced = 0
                    )
                )

            // ==================================================
            // 3. PC → PHONE
            // ==================================================

            val serverResult =
                syncFromServer(serverUrl)

            if (serverResult.isFailure) {
                return Result.failure(
                    serverResult.exceptionOrNull()
                        ?: Exception("Server sync failed")
                )
            }

            val serverSync = serverResult.getOrThrow()

            Log.d(
                "FRAP_SYNC",
                "Bidirectional sync completed."
            )

            Result.success(
                SyncResult(
                    workers = serverSync.workers,
                    embeddings = serverSync.embeddings,
                    designations = serverSync.designations,
                    schedules = serverSync.schedules,
                    attendanceAttempted = attendanceSync.attempted,
                    attendanceSynced = attendanceSync.synced
                )
            )

        } catch (exception: Exception) {

            Log.e(
                "FRAP_SYNC",
                "Sync failed",
                exception
            )

            Result.failure(exception)
        }
    }


    // ======================================================
    // PC → PHONE
    // ======================================================

    suspend fun syncFromServer(
        serverUrl: String
    ): Result<SyncResult> {

        return try {

            val api = ApiClient.create(serverUrl)
            val response = api.sync()

            if (!response.isSuccessful) {
                return Result.failure(
                    Exception(
                        "Server returned HTTP ${response.code()}"
                    )
                )
            }

            val data = response.body()

            if (data == null) {
                return Result.failure(
                    Exception("Empty server response")
                )
            }

            if (!data.success) {
                return Result.failure(
                    Exception(
                        data.message
                            ?: data.error
                            ?: "Sync failed"
                    )
                )
            }

            // ==================================================
            // WORKERS
            // ==================================================

            val workers =
                data.workers.map { worker ->
                    WorkerEntity(
                        id = worker.id,
                        memberId = worker.memberId,
                        firstName = worker.firstName,
                        middleName = worker.middleName,
                        lastName = worker.lastName,
                        suffix = worker.suffix,
                        status = worker.status,
                        updatedAt = worker.updatedAt
                    )
                }

            // ==================================================
            // SAVE WORKERS SAFELY
            // ==================================================

            for (worker in workers) {

                val existing =
                    database.workerDao().getWorkerById(worker.id)

                if (existing == null) {
                    database.workerDao().insertWorker(worker)

                    Log.d(
                        "FRAP_SYNC",
                        "Inserted worker ${worker.id}"
                    )
                } else {
                    database.workerDao().updateWorker(worker)

                    Log.d(
                        "FRAP_SYNC",
                        "Updated worker ${worker.id}"
                    )
                }
            }

            // ==================================================
            // DESIGNATIONS
            // ==================================================

            val designations =
                data.designations.map { designation ->
                    DesignationEntity(
                        id = designation.id,
                        designationType = designation.designationType,
                        updatedAt = designation.updatedAt
                    )
                }

            database.designationDao().insertDesignations(designations)

            // ==================================================
            // EMBEDDINGS
            // ==================================================

            val embeddings =
                data.embeddings.map { embedding ->
                    EmbeddingEntity(
                        id = embedding.id,
                        workerId = embedding.workerId,
                        embeddingJson = embedding.embeddingJson,
                        updatedAt = embedding.updatedAt
                    )
                }

            database.embeddingDao().insertEmbeddings(embeddings)

            // ==================================================
            // SCHEDULES
            // ==================================================

            val schedules =
                data.schedules.map { schedule ->
                    ScheduleEntity(
                        id = schedule.id,
                        workerId = schedule.workerId,
                        scheduleDate = schedule.scheduleDate,
                        designationId = schedule.designationId,
                        createdAt = schedule.createdAt,
                        updatedAt = schedule.updatedAt
                    )
                }

            database.scheduleDao().insertSchedules(schedules)

            Log.d("FRAP_SYNC", "PC → Phone:")
            Log.d("FRAP_SYNC", "Workers = ${workers.size}")
            Log.d("FRAP_SYNC", "Embeddings = ${embeddings.size}")
            Log.d("FRAP_SYNC", "Designations = ${designations.size}")
            Log.d("FRAP_SYNC", "Schedules = ${schedules.size}")

            Result.success(
                SyncResult(
                    workers = workers.size,
                    embeddings = embeddings.size,
                    designations = designations.size,
                    schedules = schedules.size,
                    attendanceAttempted = 0,
                    attendanceSynced = 0
                )
            )

        } catch (exception: Exception) {

            Log.e(
                "FRAP_SYNC",
                "PC → Phone sync failed",
                exception
            )

            Result.failure(exception)
        }
    }


    // ======================================================
    // PHONE → PC : NEW ATTENDANCE
    // ======================================================

    private suspend fun uploadPendingAttendance(
        api: ApiService
    ): Result<AttendanceUploadResult> {

        return try {

            val pending =
                database.attendanceDao().getPendingAttendance()

            if (pending.isEmpty()) {

                Log.d(
                    "FRAP_SYNC",
                    "No pending attendance."
                )

                return Result.success(
                    AttendanceUploadResult(
                        attempted = 0,
                        synced = 0
                    )
                )
            }

            Log.d(
                "FRAP_SYNC",
                "Pending attendance: ${pending.size}"
            )

            val records =
                pending.map { attendance ->
                    AttendanceSyncDto(
                        localId = attendance.id,
                        workerId = attendance.workerId,
                        attendanceDate = attendance.attendanceDate,
                        timeIn = attendance.timeIn,
                        method = attendance.method,
                        eventUuid = attendance.eventUuid,
                        createdAt = attendance.createdAt
                    )
                }

            val response =
                api.syncAttendance(
                    AttendanceSyncRequest(records = records)
                )

            if (!response.isSuccessful) {
                return Result.failure(
                    Exception(
                        "Attendance sync HTTP ${response.code()}"
                    )
                )
            }

            val data =
                response.body()
                    ?: return Result.failure(
                        Exception("Empty attendance sync response")
                    )

            if (!data.success) {
                return Result.failure(
                    Exception(
                        data.message
                            ?: data.error
                            ?: "Attendance sync failed"
                    )
                )
            }

            var syncedCount = 0

            data.records.forEach { result ->

                if (
                    result.status == "synced" ||
                    result.status == "already_exists"
                ) {

                    val localRecord =
                        pending.firstOrNull {
                            it.id == result.localId
                        }

                    if (localRecord != null) {

                        database.attendanceDao().markAsSynced(
                            eventUuid = localRecord.eventUuid,
                            status = "synced",
                            serverId = result.serverId
                        )

                        syncedCount++
                    }
                }
            }

            Log.d(
                "FRAP_SYNC",
                "Attendance uploaded: $syncedCount"
            )

            Result.success(
                AttendanceUploadResult(
                    attempted = pending.size,
                    synced = syncedCount
                )
            )

        } catch (exception: Exception) {

            Log.e(
                "FRAP_SYNC",
                "Phone → PC attendance upload failed",
                exception
            )

            Result.failure(exception)
        }
    }


    // ======================================================
    // PHONE → PC : DELETED ATTENDANCE
    // ======================================================

    private suspend fun uploadPendingDeletions(
        api: ApiService
    ): Result<Int> {

        return try {

            val pending =
                database.attendanceDao().getPendingDeletions()

            if (pending.isEmpty()) {

                Log.d(
                    "FRAP_SYNC",
                    "No pending attendance deletions."
                )

                return Result.success(0)
            }

            var deletedCount = 0

            for (attendance in pending) {

                val serverId = attendance.serverId

                // A delete_pending record without a serverId should not
                // be sent to the server. It can be safely finalized
                // locally because it never had a known server record.
                if (serverId == null) {

                    database.attendanceDao()
                        .permanentlyDeleteAttendance(attendance.id)

                    continue
                }

                val response =
                    api.deleteAttendance(
                        AttendanceDeleteRequest(
                            serverId = serverId,
                            eventUuid = attendance.eventUuid
                        )
                    )

                if (!response.isSuccessful) {
                    Log.w(
                        "FRAP_SYNC",
                        "Delete failed for serverId=$serverId " +
                                "HTTP ${response.code()}"
                    )
                    continue
                }

                val data = response.body()

                if (data?.success == true) {

                    database.attendanceDao()
                        .permanentlyDeleteAttendance(attendance.id)

                    deletedCount++

                    Log.d(
                        "FRAP_SYNC",
                        "Deleted server attendance " +
                                "serverId=$serverId"
                    )
                } else {

                    Log.w(
                        "FRAP_SYNC",
                        "Server rejected deletion " +
                                "serverId=$serverId"
                    )
                }
            }

            Result.success(deletedCount)

        } catch (exception: Exception) {

            Log.e(
                "FRAP_SYNC",
                "Pending deletion upload failed",
                exception
            )

            Result.failure(exception)
        }
    }
}


// ==========================================================
// RESULT
// ==========================================================

data class SyncResult(
    val workers: Int,
    val embeddings: Int,
    val designations: Int,
    val schedules: Int,
    val attendanceAttempted: Int,
    val attendanceSynced: Int
)


data class AttendanceUploadResult(
    val attempted: Int,
    val synced: Int
)
