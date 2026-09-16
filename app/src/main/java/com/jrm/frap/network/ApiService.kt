package com.jrm.frap.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // ======================================================
    // CONNECTION TEST
    // ======================================================

    @GET("api/mobile/ping")
    suspend fun ping(): Response<Map<String, Any>>

    // ======================================================
    // PC → PHONE
    // ======================================================

    @GET("api/mobile/sync")
    suspend fun sync(): Response<SyncResponse>

    // ======================================================
    // PHONE → PC
    // ======================================================

    @POST("api/mobile/attendance/sync")
    suspend fun syncAttendance(
        @Body request: AttendanceSyncRequest
    ): Response<AttendanceSyncResponse>
}