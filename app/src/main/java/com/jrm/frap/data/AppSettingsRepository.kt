package com.jrm.frap.data

class AppSettingsRepository(
    private val dao: AppSettingsDao
) {

    companion object {

        const val SERVER_URL_KEY = "server_url"

        const val NO_SCHEDULE_TIME_KEY =
            "no_schedule_time"

        const val DEFAULT_NO_SCHEDULE_TIME =
            "08:30"
    }


    // ======================================================
    // SERVER URL
    // ======================================================

    suspend fun saveServerUrl(
        url: String
    ) {

        dao.saveSetting(

            AppSettingsEntity(
                key = SERVER_URL_KEY,
                value = url
                    .trim()
                    .trimEnd('/')
            )
        )
    }


    suspend fun getServerUrl(): String? {

        return dao
            .getSetting(SERVER_URL_KEY)
            ?.value
    }


    suspend fun clearServerUrl() {

        dao.deleteSetting(
            SERVER_URL_KEY
        )
    }


    // ======================================================
    // NO SCHEDULE ARRIVAL TIME
    // ======================================================

    suspend fun saveNoScheduleTime(
        time: String
    ) {

        dao.saveSetting(

            AppSettingsEntity(
                key = NO_SCHEDULE_TIME_KEY,
                value = time
            )
        )
    }


    suspend fun getNoScheduleTime(): String {

        return dao
            .getSetting(NO_SCHEDULE_TIME_KEY)
            ?.value
            ?.takeIf {
                it.isNotBlank()
            }
            ?: DEFAULT_NO_SCHEDULE_TIME
    }
}