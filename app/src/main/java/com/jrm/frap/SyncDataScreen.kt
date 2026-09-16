package com.jrm.frap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jrm.frap.data.AppDatabase
import com.jrm.frap.data.WorkerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun SyncDataScreen(
    database: AppDatabase,
    onBack: () -> Unit
) {

    // ======================================================
    // COROUTINE SCOPE
    // ======================================================

    val scope =
        rememberCoroutineScope()


    // ======================================================
    // DATA
    // ======================================================

    var workers by remember {
        mutableStateOf<List<WorkerEntity>>(
            emptyList()
        )
    }

    var embeddingCount by remember {
        mutableStateOf(0)
    }

    var designationCount by remember {
        mutableStateOf(0)
    }

    var scheduleCount by remember {
        mutableStateOf(0)
    }

    var pendingAttendance by remember {
        mutableStateOf(0)
    }


    // ======================================================
    // SYNC STATE
    // ======================================================

    var isSyncing by remember {
        mutableStateOf(false)
    }

    var syncMessage by remember {
        mutableStateOf(
            "Automatic sync is enabled."
        )
    }


    // ======================================================
    // LOAD LOCAL DATABASE DATA
    // ======================================================

    suspend fun loadLocalData() {

        val loadedWorkers =
            database
                .workerDao()
                .getAllWorkers()

        val loadedEmbeddingCount =
            database
                .embeddingDao()
                .getAllEmbeddings()
                .size

        val loadedDesignationCount =
            database
                .designationDao()
                .getAllDesignations()
                .size

        val loadedScheduleCount =
            database
                .scheduleDao()
                .getAllSchedules()
                .size

        val loadedPendingAttendance =
            database
                .attendanceDao()
                .getPendingAttendance()
                .size


        withContext(Dispatchers.Main) {

            workers =
                loadedWorkers

            embeddingCount =
                loadedEmbeddingCount

            designationCount =
                loadedDesignationCount

            scheduleCount =
                loadedScheduleCount

            pendingAttendance =
                loadedPendingAttendance
        }
    }


    // ======================================================
    // INITIAL LOAD
    // ======================================================

    LaunchedEffect(Unit) {

        loadLocalData()

        // ==================================================
        // TRIGGER AUTOMATIC SYNC
        // ==================================================

        syncMessage =
            "Checking for updates..."

        AutoSyncManager.triggerSync(
            reason = "sync_screen_open"
        )

        // ==================================================
        // WAIT A LITTLE FOR SYNC TO UPDATE ROOM
        // ==================================================

        delay(2500L)

        loadLocalData()

        syncMessage =
            "Automatic sync is active."
    }


    // ======================================================
    // UI
    // ======================================================

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp)
    ) {

        // ==================================================
        // TITLE
        // ==================================================

        Text(
            text = "Sync Data"
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )


        // ==================================================
        // STATUS MESSAGE
        // ==================================================

        Text(
            text = syncMessage
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )


        // ==================================================
        // SYNC STATUS CARD
        // ==================================================

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text =
                        if (isSyncing) {
                            "Syncing..."
                        } else {
                            "Automatic Sync Enabled"
                        }
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Pending Attendance: " +
                                pendingAttendance
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )


        // ==================================================
        // DATA COUNTS
        // ==================================================

        Text(
            text =
                "Workers: ${workers.size}"
        )

        Text(
            text =
                "Embeddings: $embeddingCount"
        )

        Text(
            text =
                "Designations: $designationCount"
        )

        Text(
            text =
                "Schedules: $scheduleCount"
        )


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        // ==================================================
        // MANUAL SYNC BUTTON
        // ==================================================

        Button(
            onClick = {

                if (isSyncing) {
                    return@Button
                }

                isSyncing = true

                syncMessage =
                    "Syncing data..."

                scope.launch {

                    try {

                        // ----------------------------------
                        // START AUTOMATIC SYNC PROCESS
                        // ----------------------------------

                        AutoSyncManager.triggerSync(
                            reason = "manual_button"
                        )

                        // ----------------------------------
                        // WAIT FOR ROOM TO BE UPDATED
                        // ----------------------------------

                        delay(2500L)

                        // ----------------------------------
                        // RELOAD LOCAL DATA
                        // ----------------------------------

                        loadLocalData()

                        syncMessage =
                            "Sync completed."

                    } catch (exception: Exception) {

                        syncMessage =
                            "Sync finished with an error."

                    } finally {

                        isSyncing = false
                    }
                }
            },

            modifier =
                Modifier.fillMaxWidth(),

            enabled =
                !isSyncing
        ) {

            if (isSyncing) {

                CircularProgressIndicator()

            } else {

                Text(
                    text = "SYNC NOW"
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        // ==================================================
        // WORKER LIST
        // ==================================================

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),

            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            items(
                items = workers,
                key = {
                    it.id
                }
            ) { worker ->

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        // ----------------------------------
                        // FULL NAME
                        // ----------------------------------

                        val fullName =
                            listOfNotNull(

                                worker.firstName
                                    .takeIf {
                                        it.isNotBlank()
                                    },

                                worker.middleName
                                    ?.takeIf {
                                        it.isNotBlank()
                                    },

                                worker.lastName
                                    .takeIf {
                                        it.isNotBlank()
                                    },

                                worker.suffix
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }

                            ).joinToString(" ")


                        Text(
                            text =
                                fullName
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )


                        // ----------------------------------
                        // WORKER ID
                        // ----------------------------------

                        Text(
                            text =
                                "Worker ID: ${worker.id}"
                        )


                        // ----------------------------------
                        // MEMBER ID
                        // ----------------------------------

                        Text(
                            text =
                                "Member ID: ${worker.memberId}"
                        )
                    }
                }
            }
        }
    }
}