package com.jrm.frap

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.jrm.frap.data.AppDatabase
import com.jrm.frap.data.AttendanceDisplayRow

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale


data class AttendanceDisplayItem(
    val row: AttendanceDisplayRow,
    val status: String,
    val designationTime: String?
)

@Composable
fun AttendanceScreen(
    database: AppDatabase,
    onBack: () -> Unit
) {

    val today =
        remember {
            LocalDate.now()
                .toString()
        }

    val displayDate =
        remember {

            LocalDate.now()
                .format(
                    DateTimeFormatter.ofPattern(
                        "MMMM dd, yyyy",
                        Locale.getDefault()
                    )
                )
        }

    var records by remember {
        mutableStateOf(
            emptyList<AttendanceDisplayItem>()
        )
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }


    // ======================================================
    // LOAD
    // ======================================================

    LaunchedEffect(refreshKey) {

        try {

            val rows =
                database
                    .attendanceDisplayDao()
                    .getTodayAttendanceDisplay(
                        today
                    )

            val designationTimes =
                database
                    .designationTimeDao()
                    .getAllDesignationTimes()
                    .associateBy {
                        it.designationId
                    }

            records =
                rows.map { row ->

                    val configured =
                        row.designationId
                            ?.let {
                                designationTimes[it]
                            }

                    val status =
                        calculateAttendanceStatus(
                            timeIn =
                                row.timeIn,

                            configuredTime =
                                configured?.timeIn
                        )

                    AttendanceDisplayItem(
                        row =
                            row,

                        status =
                            status,

                        designationTime =
                            configured?.timeIn
                    )
                }

        } catch (exception: Exception) {

            exception.printStackTrace()
        }
    }


    // ======================================================
    // UI
    // ======================================================

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(12.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column {

                Text(
                    text =
                        "Today's Attendance"
                )

                Text(
                    text =
                        "Saved locally on this device"
                )
            }

            Text(
                text =
                    "${records.size} record" +
                            if (
                                records.size != 1
                            )
                                "s"
                            else
                                ""
            )
        }


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Text(
            text =
                displayDate
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // ==================================================
        // TABLE
        // ==================================================

        val horizontalScroll =
            rememberScrollState()

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
        ) {

            Column(
                modifier =
                    Modifier
                        .horizontalScroll(
                            horizontalScroll
                        )
                        .padding(8.dp)
            ) {

                // ==========================================
                // HEADER
                // ==========================================

                Row(
                    modifier =
                        Modifier
                            .width(760.dp)
                            .background(
                                Color.LightGray
                            )
                            .padding(10.dp)
                ) {

                    TableHeader(
                        "#",
                        Modifier.width(40.dp)
                    )

                    TableHeader(
                        "NAME",
                        Modifier.width(210.dp)
                    )

                    TableHeader(
                        "DESIGNATION",
                        Modifier.width(160.dp)
                    )

                    TableHeader(
                        "TIME",
                        Modifier.width(110.dp)
                    )

                    TableHeader(
                        "STATUS",
                        Modifier.width(110.dp)
                    )

                    TableHeader(
                        "ACTION",
                        Modifier.width(100.dp)
                    )
                }


                // ==========================================
                // ROWS
                // ==========================================

                if (records.isEmpty()) {

                    Text(
                        text =
                            "No attendance records today.",

                        modifier =
                            Modifier.padding(
                                16.dp
                            )
                    )

                } else {

                    LazyColumn {

                        itemsIndexed(
                            records
                        ) { index, item ->

                            AttendanceTableRow(
                                number =
                                    index + 1,

                                item =
                                    item,

                                onDeleted = {

                                    refreshKey++
                                }
                            )
                        }
                    }
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Button(
            onClick = onBack,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = "BACK"
            )
        }
    }
}


// ==========================================================
// HEADER
// ==========================================================

@Composable
private fun TableHeader(
    text: String,
    modifier: Modifier
) {

    Text(
        text =
            text,

        modifier =
            modifier
    )
}


// ==========================================================
// ROW
// ==========================================================

@Composable
private fun AttendanceTableRow(
    number: Int,
    item: AttendanceDisplayItem,
    onDeleted: () -> Unit
) {

    val row =
        item.row

    val name =
        listOfNotNull(

            row.firstName
                .takeIf {
                    it.isNotBlank()
                },

            row.middleName
                ?.takeIf {
                    it.isNotBlank()
                },

            row.lastName
                .takeIf {
                    it.isNotBlank()
                },

            row.suffix
                ?.takeIf {
                    it.isNotBlank()
                }

        ).joinToString(" ")


    Row(
        modifier =
            Modifier
                .width(760.dp)
                .padding(
                    vertical = 8.dp
                )
    ) {

        Text(
            text =
                number.toString(),

            modifier =
                Modifier.width(
                    40.dp
                )
        )

        Text(
            text =
                name,

            modifier =
                Modifier.width(
                    210.dp
                )
        )

        Text(
            text =
                row.designationName
                    ?: "No Schedule",

            modifier =
                Modifier.width(
                    160.dp
                )
        )

        Text(
            text =
                formatTime(
                    row.timeIn
                ),

            modifier =
                Modifier.width(
                    110.dp
                )
        )

        Text(
            text =
                item.status,

            modifier =
                Modifier.width(
                    110.dp
                )
        )

        Button(
            onClick = {

                // Delete will be connected
                // to AttendanceDao next.
                onDeleted()
            },

            modifier =
                Modifier.width(
                    100.dp
                )
        ) {

            Text(
                text = "Delete"
            )
        }
    }
}


// ==========================================================
// STATUS CALCULATION
// ==========================================================

private fun calculateAttendanceStatus(

    timeIn: String,

    configuredTime: String?

): String {

    if (
        configuredTime.isNullOrBlank()
    ) {

        return "NO TIME SET"
    }


    return try {

        val actual =
            LocalTime.parse(
                timeIn
            )

        val required =
            LocalTime.parse(
                configuredTime
            )

        if (
            actual.isAfter(
                required
            )
        ) {

            "LATE"

        } else {

            "ON TIME"
        }

    } catch (
        exception: Exception
    ) {

        "NO TIME SET"
    }
}


// ==========================================================
// FORMAT TIME
// ==========================================================

private fun formatTime(
    value: String
): String {

    return try {

        LocalTime
            .parse(value)
            .format(
                DateTimeFormatter.ofPattern(
                    "hh:mm a",
                    Locale.getDefault()
                )
            )

    } catch (
        exception: Exception
    ) {

        value
    }
}