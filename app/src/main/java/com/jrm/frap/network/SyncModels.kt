package com.jrm.frap.network

data class SyncResponse(
    val success: Boolean,
    val workers: List<WorkerDto> = emptyList(),
    val embeddings: List<EmbeddingDto> = emptyList(),
    val designations: List<DesignationDto> = emptyList(),
    val schedules: List<ScheduleDto> = emptyList(),
    val counts: SyncCounts? = null,
    val message: String? = null,
    val error: String? = null
)

data class SyncCounts(
    val workers: Int = 0,
    val embeddings: Int = 0,
    val designations: Int = 0,
    val schedules: Int = 0
)

data class WorkerDto(
    val id: Int,
    val memberId: Int,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val suffix: String?,
    val status: String = "active",
    val updatedAt: String?
)

data class EmbeddingDto(
    val id: Int,
    val workerId: Int,
    val embeddingJson: String,
    val updatedAt: String?
)

data class DesignationDto(
    val id: Int,
    val designationType: String,
    val updatedAt: String?
)

data class ScheduleDto(
    val id: Int,
    val workerId: Int,
    val scheduleDate: String,
    val designationId: Int,
    val createdAt: String?,
    val updatedAt: String?
)

// ==========================================================
// ATTENDANCE SYNC
// ==========================================================

data class AttendanceSyncRequest(
    val records: List<AttendanceSyncDto>
)

data class AttendanceSyncDto(
    val localId: Int,
    val workerId: Int,
    val attendanceDate: String,
    val timeIn: String,
    val method: String,
    val eventUuid: String,
    val createdAt: String
)

data class AttendanceSyncResponse(
    val success: Boolean,
    val records: List<AttendanceSyncResult> = emptyList(),
    val syncedCount: Int = 0,
    val message: String? = null,
    val error: String? = null
)

data class AttendanceSyncResult(
    val localId: Int,
    val serverId: Int?,
    val status: String,
    val message: String? = null
)