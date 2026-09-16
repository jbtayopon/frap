package com.jrm.frap

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import com.jrm.frap.data.AppDatabase
import com.jrm.frap.data.AppSettingsRepository
import com.jrm.frap.data.DesignationOption
import com.jrm.frap.data.DesignationTimeEntity
import com.jrm.frap.data.SecurityRepository

import kotlinx.coroutines.launch

import java.time.LocalTime
import java.time.format.DateTimeFormatter


@Composable
fun SettingsScreen(
    database: AppDatabase,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()


    // ======================================================
    // SETTINGS TAB
    // ======================================================

    var selectedTab by remember {
        mutableStateOf(0)
    }


    // ======================================================
    // SETTINGS REPOSITORY
    // ======================================================

    val settingsRepository =
        remember {

            AppSettingsRepository(
                database.appSettingsDao()
            )
        }


    // ======================================================
    // SECURITY REPOSITORY
    // ======================================================

    val securityRepository =
        remember {
            SecurityRepository(context)
        }


    var hasAdminPassword by remember {
        mutableStateOf(false)
    }

    var adminPassword by remember {
        mutableStateOf("")
    }

    var confirmAdminPassword by remember {
        mutableStateOf("")
    }

    var isSavingPassword by remember {
        mutableStateOf(false)
    }


    // ======================================================
    // SERVER URL
    // ======================================================

    var serverUrl by remember {
        mutableStateOf("")
    }


    // ======================================================
    // NO SCHEDULE TIME
    // ======================================================

    var noScheduleTime by remember {
        mutableStateOf(
            AppSettingsRepository
                .DEFAULT_NO_SCHEDULE_TIME
        )
    }


    // ======================================================
    // DESIGNATIONS
    // ======================================================

    var designations by remember {
        mutableStateOf(
            emptyList<DesignationOption>()
        )
    }


    // ======================================================
    // DESIGNATION TIMES
    // ======================================================

    val times =
        remember {
            mutableStateMapOf<Int, String>()
        }


    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }


    // ======================================================
    // LOAD SETTINGS
    // ======================================================

    LaunchedEffect(Unit) {

        isLoading = true

        try {

            serverUrl =
                settingsRepository
                    .getServerUrl()
                    ?: ""

            noScheduleTime =
                settingsRepository
                    .getNoScheduleTime()

            hasAdminPassword =
                securityRepository
                    .hasAdminPassword()


            designations =
                database
                    .designationTimeDao()
                    .getActiveDesignations()


            val savedTimes =
                database
                    .designationTimeDao()
                    .getAllDesignationTimes()


            times.clear()


            savedTimes.forEach { item ->

                times[item.designationId] =
                    item.timeIn
            }


        } catch (exception: Exception) {

            Toast.makeText(
                context,
                "Unable to load settings.",
                Toast.LENGTH_SHORT
            ).show()

            exception.printStackTrace()

        } finally {

            isLoading = false
        }
    }


    // ======================================================
    // MAIN UI
    // ======================================================

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {


        // ==================================================
        // HEADER
        // ==================================================

        Text(
            text = "SETTINGS",
            fontWeight = FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )


        // ==================================================
        // SETTINGS NAVIGATION
        // ==================================================

        Row(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            SettingsTabButton(
                title = "CONFIGURE",
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                },
                modifier =
                    Modifier.weight(1f)
            )


            SettingsTabButton(
                title = "DESIGNATION TIME",
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                },
                modifier =
                    Modifier.weight(1f)
            )


            SettingsTabButton(
                title = "SECURITY",
                selected = selectedTab == 2,
                onClick = {
                    selectedTab = 2
                },
                modifier =
                    Modifier.weight(1f)
            )
        }


        HorizontalDivider()


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )


        if (isLoading) {

            Text(
                text = "Loading settings..."
            )

        } else {

            when (selectedTab) {

                // ==========================================
                // CONFIGURE
                // ==========================================

                0 -> {

                    ConfigureSettings(

                        serverUrl =
                            serverUrl,

                        onServerUrlChange = {
                            serverUrl = it
                        },

                        noScheduleTime =
                            noScheduleTime,

                        onNoScheduleTimeClick = {

                            showTimePicker(
                                context =
                                    context,

                                currentTime =
                                    noScheduleTime

                            ) { selectedTime ->

                                noScheduleTime =
                                    selectedTime
                            }
                        },

                        isSaving =
                            isSaving,

                        onSave = {

                            isSaving = true

                            scope.launch {

                                try {

                                    settingsRepository
                                        .saveServerUrl(
                                            serverUrl
                                        )


                                    settingsRepository
                                        .saveNoScheduleTime(
                                            noScheduleTime
                                        )


                                    Toast.makeText(
                                        context,
                                        "Configuration saved.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } catch (exception: Exception) {

                                    Toast.makeText(
                                        context,
                                        "Unable to save configuration.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    exception.printStackTrace()

                                } finally {

                                    isSaving = false
                                }
                            }
                        },

                        onTestConnection = {

                            Toast.makeText(
                                context,
                                "Server URL saved. Connection test can use this URL.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }


                // ==========================================
                // DESIGNATION TIME
                // ==========================================

                1 -> {

                    DesignationTimeSettings(

                        designations =
                            designations,

                        times =
                            times,

                        context =
                            context,

                        isSaving =
                            isSaving,

                        onTimeSelected = {
                                id,
                                selectedTime ->

                            times[id] =
                                selectedTime
                        },

                        onSave = {

                            isSaving = true

                            scope.launch {

                                try {

                                    designations
                                        .forEach { designation ->

                                            val selectedTime =
                                                times[
                                                    designation.id
                                                ]


                                            if (
                                                !selectedTime
                                                    .isNullOrBlank()
                                            ) {

                                                database
                                                    .designationTimeDao()
                                                    .saveDesignationTime(

                                                        DesignationTimeEntity(

                                                            designationId =
                                                                designation.id,

                                                            timeIn =
                                                                selectedTime,

                                                            updatedAt =
                                                                java.time
                                                                    .LocalDateTime
                                                                    .now()
                                                                    .toString()
                                                        )
                                                    )
                                            }
                                        }


                                    Toast.makeText(
                                        context,
                                        "Designation times saved.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } catch (exception: Exception) {

                                    Toast.makeText(
                                        context,
                                        "Unable to save designation times.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    exception.printStackTrace()

                                } finally {

                                    isSaving = false
                                }
                            }
                        }
                    )
                }

                // ==========================================
                // SECURITY
                // ==========================================

                2 -> {

                    SecuritySettings(
                        hasAdminPassword = hasAdminPassword,
                        password = adminPassword,
                        confirmPassword = confirmAdminPassword,
                        isSaving = isSavingPassword,

                        onPasswordChange = {
                            adminPassword = it
                        },

                        onConfirmPasswordChange = {
                            confirmAdminPassword = it
                        },

                        onSave = {

                            if (adminPassword.length < 6) {

                                Toast.makeText(
                                    context,
                                    "Password must be at least 6 characters.",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else if (
                                adminPassword != confirmAdminPassword
                            ) {

                                Toast.makeText(
                                    context,
                                    "Passwords do not match.",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                isSavingPassword = true

                                scope.launch {

                                    try {

                                        securityRepository
                                            .setAdminPassword(
                                                adminPassword
                                            )

                                        hasAdminPassword = true
                                        adminPassword = ""
                                        confirmAdminPassword = ""

                                        Toast.makeText(
                                            context,
                                            if (hasAdminPassword)
                                                "Admin password saved."
                                            else
                                                "Admin password created.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    } catch (exception: Exception) {

                                        Toast.makeText(
                                            context,
                                            exception.message
                                                ?: "Unable to save admin password.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        exception.printStackTrace()

                                    } finally {

                                        isSavingPassword = false
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


// ==========================================================
// SETTINGS TAB
// ==========================================================

@Composable
private fun SettingsTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier
                .clickable {
                    onClick()
                }
                .padding(
                    vertical = 12.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = title,
            fontWeight =
                if (selected)
                    FontWeight.Bold
                else
                    FontWeight.Normal
        )


        if (selected) {

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Color(0xFF1976D2)
                        )
            )
        }
    }
}


// ==========================================================
// CONFIGURE
// ==========================================================

@Composable
private fun ConfigureSettings(

    serverUrl: String,

    onServerUrlChange: (String) -> Unit,

    noScheduleTime: String,

    onNoScheduleTimeClick: () -> Unit,

    isSaving: Boolean,

    onSave: () -> Unit,

    onTestConnection: () -> Unit

) {

    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
    ) {

        item {

            Text(
                text = "SERVER / API",
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            OutlinedTextField(

                value =
                    serverUrl,

                onValueChange =
                    onServerUrlChange,

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("API / ngrok URL")
                },

                placeholder = {
                    Text(
                        "https://xxxx.ngrok-free.app"
                    )
                },

                singleLine = true
            )


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            Text(
                text = "NO SCHEDULE",
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNoScheduleTimeClick()
                        }
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column {

                        Text(
                            text =
                                "Arrival Time"
                        )

                        Text(
                            text =
                                formatTime(
                                    noScheduleTime
                                ),

                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    OutlinedButton(
                        onClick =
                            onNoScheduleTimeClick
                    ) {

                        Text(
                            "SET TIME"
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Text(
                text =
                    "Default: 08:30 AM. " +
                            "This time is used when the worker has no schedule."
            )


            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )


            Button(
                onClick = onSave,

                modifier =
                    Modifier.fillMaxWidth(),

                enabled =
                    !isSaving
            ) {

                Text(
                    if (isSaving)
                        "SAVING..."
                    else
                        "SAVE CONFIGURATION"
                )
            }


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            OutlinedButton(

                onClick =
                    onTestConnection,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "TEST CONNECTION"
                )
            }
        }
    }
}


// ==========================================================
// DESIGNATION SETTINGS
// ==========================================================

@Composable
private fun DesignationTimeSettings(

    designations:
    List<DesignationOption>,

    times:
    Map<Int, String>,

    context:
    Context,

    isSaving:
    Boolean,

    onTimeSelected:
        (Int, String) -> Unit,

    onSave:
        () -> Unit

) {

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Text(
            text =
                "Configure arrival time per designation."
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        if (designations.isEmpty()) {

            Text(
                text =
                    "No designations available."
            )

        } else {

            LazyColumn(
                modifier =
                    Modifier.weight(1f)
            ) {

                items(
                    designations
                ) { designation ->

                    Card(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 5.dp
                                )
                    ) {

                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),

                            horizontalArrangement =
                                Arrangement.SpaceBetween,

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text =
                                        designation.name,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Text(
                                    text =
                                        times[
                                            designation.id
                                        ]?.let {
                                            formatTime(it)
                                        }
                                            ?: "Not configured"
                                )
                            }


                            OutlinedButton(

                                onClick = {

                                    showTimePicker(

                                        context =
                                            context,

                                        currentTime =
                                            times[
                                                designation.id
                                            ]

                                    ) { selectedTime ->

                                        onTimeSelected(
                                            designation.id,
                                            selectedTime
                                        )
                                    }
                                }
                            ) {

                                Text(
                                    "SET TIME"
                                )
                            }
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

            onClick =
                onSave,

            modifier =
                Modifier.fillMaxWidth(),

            enabled =
                !isSaving
        ) {

            Text(
                if (isSaving)
                    "SAVING..."
                else
                    "SAVE DESIGNATION TIMES"
            )
        }
    }
}


// ==========================================================
// SECURITY SETTINGS
// ==========================================================

@Composable
private fun SecuritySettings(

    hasAdminPassword: Boolean,

    password: String,

    confirmPassword: String,

    isSaving: Boolean,

    onPasswordChange: (String) -> Unit,

    onConfirmPasswordChange: (String) -> Unit,

    onSave: () -> Unit

) {

    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
    ) {

        item {

            Text(
                text = "ADMIN SECURITY",
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    if (hasAdminPassword)
                        "Admin password is configured."
                    else
                        "No admin password configured."
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        if (hasAdminPassword)
                            "New Admin Password"
                        else
                            "Admin Password"
                    )
                },
                singleLine = true,
                visualTransformation =
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Confirm Password")
                },
                singleLine = true,
                visualTransformation =
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = "Minimum 6 characters."
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Button(
                onClick = onSave,
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    !isSaving
            ) {

                Text(
                    if (isSaving)
                        "SAVING..."
                    else if (hasAdminPassword)
                        "CHANGE ADMIN PASSWORD"
                    else
                        "CREATE ADMIN PASSWORD"
                )
            }
        }
    }
}


// ==========================================================
// TIME PICKER
// ==========================================================

private fun showTimePicker(

    context: Context,

    currentTime: String?,

    onSelected: (String) -> Unit

) {

    val parsedTime =

        try {

            if (
                currentTime.isNullOrBlank()
            ) {

                LocalTime.of(
                    8,
                    30
                )

            } else {

                LocalTime.parse(
                    currentTime
                )
            }

        } catch (
            exception: Exception
        ) {

            LocalTime.of(
                8,
                30
            )
        }


    TimePickerDialog(

        context,

        { _, hour, minute ->

            onSelected(

                String.format(
                    "%02d:%02d",
                    hour,
                    minute
                )
            )
        },

        parsedTime.hour,

        parsedTime.minute,

        false

    ).show()
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
                DateTimeFormatter
                    .ofPattern("hh:mm a")
            )

    } catch (
        exception: Exception
    ) {

        value
    }
}