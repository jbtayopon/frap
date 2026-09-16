package com.jrm.frap

import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jrm.frap.data.AppDatabase
import com.jrm.frap.data.AppSettingsRepository
import com.jrm.frap.data.SecurityRepository
import com.jrm.frap.data.AttendanceDisplayRow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

data class AttendanceDisplayItem(
    val row: AttendanceDisplayRow,
    val status: String,
    val designationTime: String?
)

@Composable
fun AttendanceScreen(database: AppDatabase, onBack: () -> Unit) {
    val today = remember { LocalDate.now().toString() }
    val displayDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault()))
    }
    var records by remember { mutableStateOf(emptyList<AttendanceDisplayItem>()) }
    var refreshKey by remember { mutableStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val securityRepository = remember { SecurityRepository(context) }

    var selectedDeleteItem by remember { mutableStateOf<AttendanceDisplayItem?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        try {
            val rows = database.attendanceDisplayDao().getTodayAttendanceDisplay(today)
            val designationTimes = database.designationTimeDao().getAllDesignationTimes()
                .associateBy { it.designationId }
            val noScheduleTime = AppSettingsRepository(database.appSettingsDao()).getNoScheduleTime()

            records = rows.map { row ->
                val configuredTime = row.designationId?.let { designationTimes[it]?.timeIn }
                    ?: noScheduleTime
                AttendanceDisplayItem(
                    row = row,
                    status = calculateAttendanceStatus(row.timeIn, configuredTime),
                    designationTime = configuredTime
                )
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Today's Attendance")
                Text("Saved locally on this device")
            }
            Text("${records.size} record${if (records.size != 1) "s" else ""}")
        }
        Spacer(Modifier.height(10.dp))
        Text(displayDate)
        Spacer(Modifier.height(12.dp))

        val horizontalScroll = rememberScrollState()
        Card(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.horizontalScroll(horizontalScroll).padding(8.dp)) {
                Row(
                    Modifier.width(760.dp).background(Color.LightGray).padding(10.dp)
                ) {
                    TableHeader("#", Modifier.width(40.dp))
                    TableHeader("NAME", Modifier.width(210.dp))
                    TableHeader("DESIGNATION", Modifier.width(160.dp))
                    TableHeader("TIME", Modifier.width(110.dp))
                    TableHeader("STATUS", Modifier.width(110.dp))
                    TableHeader("ACTION", Modifier.width(100.dp))
                }

                if (records.isEmpty()) {
                    Text("No attendance records today.", Modifier.padding(16.dp))
                } else {
                    LazyColumn {
                        itemsIndexed(records) { index, item ->
                            AttendanceTableRow(index + 1, item) {
                                selectedDeleteItem = item
                                deletePassword = ""
                                showPasswordDialog = true
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(onClick = onBack, Modifier.fillMaxWidth()) { Text("BACK") }
    }

    // ======================================================
    // DELETE PASSWORD DIALOG
    // ======================================================

    if (showPasswordDialog && selectedDeleteItem != null) {

        val itemToDelete = selectedDeleteItem!!

        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showPasswordDialog = false
                    deletePassword = ""
                }
            },
            title = {
                Text("Delete Attendance")
            },
            text = {
                Column {

                    Text(
                        "Enter the admin password to delete this attendance."
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = {
                            deletePassword = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Admin Password")
                        },
                        singleLine = true,
                        enabled = !isDeleting,
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {

                Button(
                    onClick = {

                        if (!securityRepository.hasAdminPassword()) {

                            Toast.makeText(
                                context,
                                "Please create an admin password in Settings first.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@Button
                        }

                        if (deletePassword.isBlank()) {

                            Toast.makeText(
                                context,
                                "Enter the admin password.",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        scope.launch {

                            isDeleting = true

                            try {

                                val valid =
                                    securityRepository
                                        .verifyAdminPassword(
                                            deletePassword
                                        )

                                if (!valid) {

                                    Toast.makeText(
                                        context,
                                        "Incorrect admin password.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else {

                                    val attendance =
                                        database
                                            .attendanceDao()
                                            .getAllAttendance()
                                            .firstOrNull {
                                                it.id ==
                                                        itemToDelete.row.attendanceId
                                            }

                                    if (attendance == null) {

                                        Toast.makeText(
                                            context,
                                            "Attendance record not found.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    } else {

                                        val deletedAt =
                                            LocalDateTime
                                                .now()
                                                .toString()

                                        when (attendance.syncStatus) {

                                            "pending" -> {

                                                database
                                                    .attendanceDao()
                                                    .deletePendingAttendance(
                                                        attendance.id
                                                    )
                                            }

                                            "synced" -> {

                                                database
                                                    .attendanceDao()
                                                    .markAttendanceForDeletion(
                                                        attendance.id,
                                                        deletedAt
                                                    )
                                            }

                                            "delete_pending" -> {
                                                // Already waiting for server deletion.
                                            }

                                            else -> {

                                                Toast.makeText(
                                                    context,
                                                    "Unable to delete this attendance.",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                return@launch
                                            }
                                        }

                                        Toast.makeText(
                                            context,
                                            "Attendance deleted.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        showPasswordDialog = false
                                        selectedDeleteItem = null
                                        deletePassword = ""

                                        refreshKey++
                                    }
                                }

                            } catch (exception: Exception) {

                                Toast.makeText(
                                    context,
                                    "Delete failed: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                exception.printStackTrace()

                            } finally {

                                isDeleting = false
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text(
                        if (isDeleting)
                            "DELETING..."
                        else
                            "DELETE"
                    )
                }
            },
            dismissButton = {

                Button(
                    onClick = {
                        if (!isDeleting) {
                            showPasswordDialog = false
                            deletePassword = ""
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier) {
    Text(text, modifier = modifier)
}

@Composable
private fun AttendanceTableRow(number: Int, item: AttendanceDisplayItem, onDeleted: () -> Unit) {
    val row = item.row
    val name = listOfNotNull(
        row.firstName.takeIf { it.isNotBlank() },
        row.middleName?.takeIf { it.isNotBlank() },
        row.lastName.takeIf { it.isNotBlank() },
        row.suffix?.takeIf { it.isNotBlank() }
    ).joinToString(" ")

    Row(Modifier.width(760.dp).padding(vertical = 8.dp)) {
        Text(number.toString(), Modifier.width(40.dp))
        Text(name, Modifier.width(210.dp))
        Text(row.designationName ?: "No Schedule", Modifier.width(160.dp))
        Text(formatTime(row.timeIn), Modifier.width(110.dp))
        Text(item.status, Modifier.width(110.dp))
        Button(onClick = onDeleted, Modifier.width(100.dp)) { Text("Delete") }
    }
}

private fun calculateAttendanceStatus(timeIn: String, configuredTime: String?): String {
    if (configuredTime.isNullOrBlank()) return "NO TIME SET"
    return try {
        val actual = LocalTime.parse(timeIn)
        val required = LocalTime.parse(configuredTime)
        if (actual.isAfter(required)) "LATE" else "ON TIME"
    } catch (exception: Exception) {
        "NO TIME SET"
    }
}

private fun formatTime(value: String): String {
    return try {
        LocalTime.parse(value).format(
            DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
        )
    } catch (exception: Exception) {
        value
    }
}
