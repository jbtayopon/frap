package com.jrm.frap

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log

import com.jrm.frap.data.AppDatabase
import com.jrm.frap.data.AppSettingsRepository
import com.jrm.frap.network.SyncRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import java.util.concurrent.atomic.AtomicBoolean

object AutoSyncManager {

    private var started = false

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var connectivityManager: ConnectivityManager

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )

    private val syncRunning =
        AtomicBoolean(false)


    // ======================================================
    // START AUTOMATIC SYNC
    // ======================================================

    fun start(
        appContext: Context,
        appDatabase: AppDatabase
    ) {

        if (started) {
            return
        }

        started = true

        context = appContext.applicationContext
        database = appDatabase

        connectivityManager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        // ==================================================
        // APP START
        // ==================================================

        triggerSync(
            reason = "app_start"
        )

        // ==================================================
        // NETWORK RECONNECT
        // ==================================================

        try {

            connectivityManager.registerDefaultNetworkCallback(
                networkCallback
            )

            Log.d(
                "FRAP_AUTO_SYNC",
                "Network callback registered."
            )

        } catch (exception: Exception) {

            Log.e(
                "FRAP_AUTO_SYNC",
                "Failed to register network callback",
                exception
            )
        }
    }


    // ======================================================
    // NETWORK CALLBACK
    // ======================================================

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(
                network: Network
            ) {

                Log.d(
                    "FRAP_AUTO_SYNC",
                    "Network available. Checking PC/ngrok."
                )

                triggerSync(
                    reason = "network_available"
                )
            }
        }


    // ======================================================
    // MANUAL / SCREEN SYNC
    // ======================================================

    fun triggerSync(
        reason: String = "manual"
    ) {

        if (!started) {
            return
        }

        if (
            !syncRunning.compareAndSet(
                false,
                true
            )
        ) {

            Log.d(
                "FRAP_AUTO_SYNC",
                "Sync already running. Skip."
            )

            return
        }

        scope.launch {

            try {

                val settingsRepository =
                    AppSettingsRepository(
                        database.appSettingsDao()
                    )

                val serverUrl =
                    settingsRepository.getServerUrl()

                if (serverUrl.isNullOrBlank()) {

                    Log.d(
                        "FRAP_AUTO_SYNC",
                        "No server URL configured."
                    )

                    return@launch
                }

                Log.d(
                    "FRAP_AUTO_SYNC",
                    "Checking PC/ngrok before sync. reason=$reason"
                )

                val result =
                    SyncRepository(database).sync(serverUrl)

                if (result.isSuccess) {

                    val data = result.getOrThrow()

                    Log.d(
                        "FRAP_AUTO_SYNC",
                        "SYNC SUCCESS: " +
                                "workers=${data.workers}, " +
                                "embeddings=${data.embeddings}, " +
                                "designations=${data.designations}, " +
                                "schedules=${data.schedules}, " +
                                "attendance=${data.attendanceSynced}"
                    )

                } else {

                    Log.w(
                        "FRAP_AUTO_SYNC",
                        "SYNC SKIPPED/FAILED: " +
                                result.exceptionOrNull()?.message
                    )
                }

            } catch (exception: Exception) {

                Log.e(
                    "FRAP_AUTO_SYNC",
                    "Automatic sync error",
                    exception
                )

            } finally {

                syncRunning.set(false)
            }
        }
    }
}
