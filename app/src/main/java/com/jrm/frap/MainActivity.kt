@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.jrm.frap

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.core.content.ContextCompat

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

import com.jrm.frap.data.AppDatabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape


// ==========================================================
// MAIN ACTIVITY
// ==========================================================

class MainActivity : ComponentActivity() {

    // ======================================================
    // ONNX
    // ======================================================

    private var ortEnvironment: OrtEnvironment? = null

    private var ortSession: OrtSession? = null

    private var faceEmbeddingEngine: FaceEmbeddingEngine? = null

    // ======================================================
    // ROOM
    // ======================================================

    private lateinit var database: AppDatabase

    // ======================================================
    // CURRENT NAVIGATION SCREEN
    //
    // 0 = Recognition
    // 1 = Attendance
    // 2 = Settings
    // 3 = Sync Data
    // ======================================================

    private var selectedScreen by mutableStateOf(0)

    // ======================================================
    // CAMERA PERMISSION
    // ======================================================

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                // Open Recognition after permission is granted.
                selectedScreen = 0

            } else {

                Log.w(
                    "FRAP_CAMERA",
                    "Camera permission denied"
                )
            }
        }


    // ======================================================
    // ON CREATE
    // ======================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        // ==================================================
        // ROOM DATABASE
        // ==================================================

        database =
            AppDatabase.getInstance(this)

        AutoSyncManager.start(
            appContext = applicationContext,
            appDatabase = database
        )

        // ==================================================
        // LOAD ONNX MODEL
        // ==================================================

        Log.e(
            "FRAP_ONNX",
            "=== ONNX TEST STARTED ==="
        )

        loadRecognitionModel()

        // ==================================================
        // MAIN NAVIGATION
        // ==================================================

        setContent {

            MainNavigationScreen(
                faceEmbeddingEngine = faceEmbeddingEngine,
                database = database,
                selectedScreen = selectedScreen,

                onScreenSelected = {
                    selectedScreen = it
                },

                onRequestRecognition = {
                    requestCameraPermission()
                }
            )
        }
    }


    // ======================================================
    // CAMERA PERMISSION
    // ======================================================

    private fun requestCameraPermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            // Camera already allowed.
            selectedScreen = 0

        } else {

            // Ask Android for permission.
            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }


    // ======================================================
    // LOAD RECOGNITION MODEL
    // ======================================================

    private fun loadRecognitionModel() {

        try {

            Log.d(
                "FRAP_ONNX",
                "Loading w600k_r50.onnx..."
            )

            // ==================================================
            // ORT ENVIRONMENT
            // ==================================================

            val environment =
                OrtEnvironment.getEnvironment()

            ortEnvironment =
                environment

            // ==================================================
            // LOAD MODEL FILE
            // ==================================================

            val modelBytes =
                assets
                    .open("w600k_r50.onnx")
                    .use { inputStream ->

                        inputStream.readBytes()
                    }

            Log.d(
                "FRAP_ONNX",
                "Model file loaded. Size: ${modelBytes.size} bytes"
            )

            // ==================================================
            // CREATE SESSION
            // ==================================================

            val session =
                environment.createSession(
                    modelBytes,
                    OrtSession.SessionOptions()
                )

            ortSession =
                session

            // ==================================================
            // FACE EMBEDDING ENGINE
            // ==================================================

            faceEmbeddingEngine =
                FaceEmbeddingEngine(
                    environment,
                    session
                )

            Log.d(
                "FRAP_ONNX",
                "FaceEmbeddingEngine initialized successfully"
            )

            Log.d(
                "FRAP_ONNX",
                "ONNX MODEL LOADED SUCCESSFULLY!"
            )

            // ==================================================
            // INPUT INFORMATION
            // ==================================================

            session.inputInfo.forEach { (name, info) ->

                Log.d(
                    "FRAP_ONNX",
                    "Input: $name -> $info"
                )
            }

            // ==================================================
            // OUTPUT INFORMATION
            // ==================================================

            session.outputInfo.forEach { (name, info) ->

                Log.d(
                    "FRAP_ONNX",
                    "Output: $name -> $info"
                )
            }

        } catch (exception: Exception) {

            Log.e(
                "FRAP_ONNX",
                "FAILED TO LOAD ONNX MODEL",
                exception
            )
        }
    }


    // ======================================================
    // DESTROY
    // ======================================================

    override fun onDestroy() {

        try {

            ortSession?.close()

        } catch (exception: Exception) {

            Log.e(
                "FRAP_ONNX",
                "Error closing ONNX session",
                exception
            )
        }

        super.onDestroy()
    }
}


// ==========================================================
// MAIN NAVIGATION SCREEN
// ==========================================================

@Composable
fun MainNavigationScreen(

    faceEmbeddingEngine: FaceEmbeddingEngine?,

    database: AppDatabase,

    selectedScreen: Int,

    onScreenSelected: (Int) -> Unit,

    onRequestRecognition: () -> Unit

) {

    Scaffold(

        // ==================================================
        // BOTTOM NAVIGATION
        // ==================================================

        bottomBar = {

            NavigationBar {

                // ==========================================
                // RECOGNITION
                // ==========================================

                NavigationBarItem(

                    selected =
                        selectedScreen == 0,

                    onClick = {

                        onRequestRecognition()
                    },

                    icon = {

                        Text(
                            text = "👤"
                        )
                    },

                    label = {

                        Text(
                            text = "Recognition"
                        )
                    }
                )


                // ==========================================
                // ATTENDANCE
                // ==========================================

                NavigationBarItem(

                    selected =
                        selectedScreen == 1,

                    onClick = {

                        onScreenSelected(1)
                    },

                    icon = {

                        Text(
                            text = "📋"
                        )
                    },

                    label = {

                        Text(
                            text = "Attendance"
                        )
                    }
                )


                // ==========================================
                // SETTINGS
                // ==========================================

                NavigationBarItem(

                    selected =
                        selectedScreen == 2,

                    onClick = {

                        onScreenSelected(2)
                    },

                    icon = {

                        Text(
                            text = "⚙"
                        )
                    },

                    label = {

                        Text(
                            text = "Settings"
                        )
                    }
                )


                // ==========================================
                // SYNC DATA
                // ==========================================

                NavigationBarItem(

                    selected =
                        selectedScreen == 3,

                    onClick = {

                        onScreenSelected(3)
                    },

                    icon = {

                        Text(
                            text = "🔄"
                        )
                    },

                    label = {

                        Text(
                            text = "Sync Data"
                        )
                    }
                )
            }
        }

    ) { paddingValues ->

        // ==================================================
        // CURRENT SCREEN
        // ==================================================

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
        ) {

            when (selectedScreen) {

                // ==================================================
                // RECOGNITION
                // ==================================================

                0 -> {

                    CameraScreen(
                        faceEmbeddingEngine =
                            faceEmbeddingEngine
                    )
                }


                // ==================================================
                // ATTENDANCE
                // ==================================================

                1 -> {

                    AttendanceScreen(

                        database = database,

                        onBack = {

                            onScreenSelected(0)
                        }
                    )
                }


                // ==================================================
                // SETTINGS
                // ==================================================

                2 -> {

                    SettingsScreen(

                        database = database,

                        onBack = {

                            onScreenSelected(0)
                        }
                    )
                }


                // ==================================================
                // SYNC DATA
                // ==================================================

                3 -> {

                    SyncDataScreen(

                        database = database,

                        onBack = {

                            onScreenSelected(0)
                        }
                    )
                }
            }
        }
    }
}


// ==========================================================
// CAMERA SCREEN
// ==========================================================

// ==========================================================
// CAMERA SCREEN
// ==========================================================

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen(
    faceEmbeddingEngine: FaceEmbeddingEngine?
) {

    val context =
        LocalContext.current

    // ======================================================
    // DATABASE
    // ======================================================

    val database =
        remember {
            AppDatabase.getInstance(context)
        }

    val recognitionRepository =
        remember {
            RecognitionRepository(database)
        }

    val scope =
        rememberCoroutineScope()


    // ======================================================
    // RECOGNITION DISPLAY
    // ======================================================

    var recognizedName by remember {
        mutableStateOf<String?>(null)
    }


    // ======================================================
    // RECOGNITION COLOR
    //
    // Green = recognized
    // Red   = unknown
    // Orange will be added when schedule/late calculation
    // is connected.
    // ======================================================

    var recognitionStatus by remember {
        mutableStateOf("none")
    }

    // ======================================================
    // SMART IDLE MODE
    //
    // The camera stays active, but the screen dims when
    // nobody has been detected for the configured idle time.
    // When a face is detected again, the screen returns to
    // normal brightness.
    // ======================================================

    val idleTimeoutMillis = 2 * 60 * 1000L

    var isIdle by remember {
        mutableStateOf(false)
    }

    val lastFaceDetectedTime =
        remember {
            AtomicLong(SystemClock.elapsedRealtime())
        }

    var overlayJob by remember {
        mutableStateOf<kotlinx.coroutines.Job?>(null)
    }


    // ======================================================
    // CAMERA EXECUTOR
    // ======================================================

    val cameraExecutor =
        remember {
            Executors.newSingleThreadExecutor()
        }


    // ======================================================
    // RECOGNITION EXECUTOR
    // ======================================================

    val recognitionExecutor =
        remember {
            Executors.newSingleThreadExecutor()
        }


    // ======================================================
    // PREVENT OVERLAPPING RECOGNITION
    // ======================================================

    val recognitionRunning =
        remember {
            AtomicBoolean(false)
        }


    // ======================================================
    // RECOGNITION INTERVAL
    // ======================================================

    val lastRecognitionTime =
        remember {
            AtomicLong(0L)
        }


    // ======================================================
    // SMART IDLE SCREEN
    //
    // Keep the activity awake so the camera can continue
    // detecting faces, but dim the activity window after
    // prolonged inactivity to reduce display power/heat.
    // ======================================================

    DisposableEffect(Unit) {

        val activity =
            context as? ComponentActivity

        val window =
            activity?.window

        if (window != null) {

            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        onDispose {

            if (window != null) {

                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )

                // Return control to the system/default brightness.
                val attributes =
                    window.attributes

                attributes.screenBrightness = -1f

                window.attributes = attributes
            }
        }
    }

    // ======================================================
    // IDLE CHECKER
    //
    // ML Kit keeps running. ArcFace is only reached when a
    // face is detected, so the expensive embedding step is
    // not performed while there is no face.
    // ======================================================

    androidx.compose.runtime.LaunchedEffect(Unit) {

        while (true) {

            val now =
                SystemClock.elapsedRealtime()

            val idle =
                now - lastFaceDetectedTime.get() >=
                        idleTimeoutMillis

            if (idle != isIdle) {

                isIdle = idle

                val activity =
                    context as? ComponentActivity

                val window =
                    activity?.window

                if (window != null) {

                    val attributes =
                        window.attributes

                    attributes.screenBrightness =
                        if (idle) {
                            0.08f
                        } else {
                            -1f
                        }

                    window.attributes =
                        attributes
                }
            }

            delay(1000L)
        }
    }

    // ======================================================
    // CLEANUP
    // ======================================================

    DisposableEffect(Unit) {

        onDispose {

            cameraExecutor.shutdown()

            recognitionExecutor.shutdown()
        }
    }


    // ======================================================
    // CAMERA + UI
    // ======================================================

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {


        // ==================================================
        // CAMERA PREVIEW
        // ==================================================

        AndroidView(

            factory = {

                val previewView =
                    PreviewView(context)

                previewView.scaleType =
                    PreviewView.ScaleType.FILL_CENTER


                // ==========================================
                // CAMERA PROVIDER
                // ==========================================

                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(
                        context
                    )


                cameraProviderFuture.addListener({

                    val cameraProvider =
                        cameraProviderFuture.get()


                    // ======================================
                    // PREVIEW
                    // ======================================

                    val preview =
                        Preview.Builder()
                            .build()

                    preview.setSurfaceProvider(
                        previewView.surfaceProvider
                    )


                    // ======================================
                    // ML KIT
                    // ======================================

                    val detectorOptions =
                        FaceDetectorOptions.Builder()

                            .setPerformanceMode(
                                FaceDetectorOptions
                                    .PERFORMANCE_MODE_FAST
                            )

                            .setLandmarkMode(
                                FaceDetectorOptions
                                    .LANDMARK_MODE_ALL
                            )

                            .build()


                    val faceDetector =
                        FaceDetection.getClient(
                            detectorOptions
                        )


                    // ======================================
                    // IMAGE ANALYSIS
                    // ======================================

                    val imageAnalysis =
                        ImageAnalysis.Builder()

                            .setBackpressureStrategy(
                                ImageAnalysis
                                    .STRATEGY_KEEP_ONLY_LATEST
                            )

                            .setImageQueueDepth(1)

                            .build()


                    imageAnalysis.setAnalyzer(

                        cameraExecutor

                    ) { imageProxy ->


                        // ==================================
                        // CURRENT TIME
                        // ==================================

                        val now =
                            SystemClock.elapsedRealtime()


                        // ==================================
                        // SKIP FRAME
                        // ==================================

                        if (

                            recognitionRunning.get() ||

                            now -
                            lastRecognitionTime.get() <
                            500L

                        ) {

                            imageProxy.close()

                            return@setAnalyzer
                        }


                        // ==================================
                        // CAMERA IMAGE
                        // ==================================

                        val mediaImage =
                            imageProxy.image


                        if (mediaImage == null) {

                            imageProxy.close()

                            return@setAnalyzer
                        }


                        // ==================================
                        // CREATE ML KIT IMAGE
                        // ==================================

                        val inputImage =
                            InputImage.fromMediaImage(

                                mediaImage,

                                imageProxy
                                    .imageInfo
                                    .rotationDegrees
                            )


                        // ==================================
                        // LOCK RECOGNITION
                        // ==================================

                        recognitionRunning.set(true)

                        lastRecognitionTime.set(now)


                        // ==================================
                        // ML KIT FACE DETECTION
                        // ==================================

                        faceDetector
                            .process(inputImage)

                            .addOnSuccessListener(

                                cameraExecutor

                            ) { faces ->


                                try {


                                    // ==================================
                                    // NO FACE
                                    // ==================================

                                    if (faces.isEmpty()) {

                                        recognitionRunning.set(false)

                                        imageProxy.close()

                                        return@addOnSuccessListener
                                    }


                                    // ==================================
                                    // FIRST FACE
                                    // ==================================

                                    val face =
                                        faces.first()

                                    // A face is present again. Wake the
                                    // activity window from smart idle mode.
                                    lastFaceDetectedTime.set(
                                        SystemClock.elapsedRealtime()
                                    )

                                    if (isIdle) {

                                        scope.launch(Dispatchers.Main) {

                                            isIdle = false

                                            val activity =
                                                context as? ComponentActivity

                                            val window =
                                                activity?.window

                                            if (window != null) {

                                                val attributes =
                                                    window.attributes

                                                attributes.screenBrightness =
                                                    -1f

                                                window.attributes =
                                                    attributes
                                            }
                                        }
                                    }


                                    // ==================================
                                    // COPY FRAME
                                    // ==================================

                                    val bitmap =
                                        ImageUtils
                                            .imageProxyToBitmap(
                                                imageProxy
                                            )


                                    // ==================================
                                    // RELEASE CAMERA FRAME
                                    // ==================================

                                    imageProxy.close()


                                    if (bitmap == null) {

                                        Log.e(
                                            "FRAP_IMAGE",
                                            "Bitmap conversion FAILED"
                                        )

                                        recognitionRunning.set(false)

                                        return@addOnSuccessListener
                                    }


                                    // ==================================
                                    // LANDMARKS
                                    // ==================================

                                    val leftEye =
                                        face
                                            .getLandmark(
                                                FaceLandmark.LEFT_EYE
                                            )
                                            ?.position


                                    val rightEye =
                                        face
                                            .getLandmark(
                                                FaceLandmark.RIGHT_EYE
                                            )
                                            ?.position


                                    val nose =
                                        face
                                            .getLandmark(
                                                FaceLandmark.NOSE_BASE
                                            )
                                            ?.position


                                    val leftMouth =
                                        face
                                            .getLandmark(
                                                FaceLandmark.MOUTH_LEFT
                                            )
                                            ?.position


                                    val rightMouth =
                                        face
                                            .getLandmark(
                                                FaceLandmark.MOUTH_RIGHT
                                            )
                                            ?.position


                                    // ==================================
                                    // LANDMARK VALIDATION
                                    // ==================================

                                    if (

                                        leftEye == null ||

                                        rightEye == null ||

                                        nose == null ||

                                        leftMouth == null ||

                                        rightMouth == null

                                    ) {

                                        bitmap.recycle()

                                        recognitionRunning.set(false)

                                        return@addOnSuccessListener
                                    }


                                    // ==================================
                                    // HEAVY RECOGNITION THREAD
                                    // ==================================

                                    recognitionExecutor.execute {

                                        try {


                                            // ==================================
                                            // FACE ALIGNMENT
                                            // ==================================

                                            val faceBitmap =
                                                FaceAlignmentUtils.alignFace(

                                                    bitmap =
                                                        bitmap,

                                                    leftEye =
                                                        leftEye,

                                                    rightEye =
                                                        rightEye,

                                                    nose =
                                                        nose,

                                                    leftMouth =
                                                        leftMouth,

                                                    rightMouth =
                                                        rightMouth
                                                )


                                            bitmap.recycle()


                                            if (faceBitmap == null) {

                                                Log.e(
                                                    "FRAP_FACE_ALIGN",
                                                    "Face alignment FAILED"
                                                )

                                                recognitionRunning.set(false)

                                                return@execute
                                            }


                                            // ==================================
                                            // ARC FACE
                                            // ==================================

                                            val embedding =
                                                faceEmbeddingEngine
                                                    ?.getEmbedding(
                                                        faceBitmap
                                                    )


                                            faceBitmap.recycle()


                                            if (embedding == null) {

                                                Log.e(
                                                    "FRAP_EMBEDDING",
                                                    "Embedding FAILED"
                                                )

                                                recognitionRunning.set(false)

                                                return@execute
                                            }


                                            Log.d(
                                                "FRAP_EMBEDDING",
                                                "Embedding generated: ${embedding.size} values"
                                            )


                                            // ==================================
                                            // ROOM RECOGNITION
                                            // ==================================

                                            scope.launch(
                                                Dispatchers.IO
                                            ) {

                                                try {


                                                    val result =
                                                        recognitionRepository
                                                            .findBestMatch(
                                                                embedding
                                                            )


                                                    // ==================================
                                                    // RECOGNIZED
                                                    // ==================================

                                                    if (result != null) {


                                                        val worker =
                                                            result.worker


                                                        val name =
                                                            listOfNotNull(

                                                                worker
                                                                    .firstName
                                                                    .takeIf {
                                                                        it.isNotBlank()
                                                                    },

                                                                worker
                                                                    .middleName
                                                                    ?.takeIf {
                                                                        it.isNotBlank()
                                                                    },

                                                                worker
                                                                    .lastName
                                                                    .takeIf {
                                                                        it.isNotBlank()
                                                                    },

                                                                worker
                                                                    .suffix
                                                                    ?.takeIf {
                                                                        it.isNotBlank()
                                                                    }

                                                            )
                                                                .joinToString(" ")


                                                        // ==================================
                                                        // UI
                                                        // ==================================

                                                        withContext(Dispatchers.Main) {

                                                            recognizedName = name
                                                            recognitionStatus = "recognized"

                                                            overlayJob?.cancel()

                                                            overlayJob =
                                                                scope.launch {

                                                                    kotlinx.coroutines.delay(
                                                                        1000L
                                                                    )

                                                                    recognizedName = null
                                                                    recognitionStatus = "none"
                                                                }
                                                        }


                                                        Log.d(
                                                            "FRAP_RECOGNITION",

                                                            "MATCH: $name " +
                                                                    "similarity=${result.similarity}"
                                                        )


                                                        // ==================================
                                                        // ATTENDANCE
                                                        // ==================================

                                                        saveAttendanceIfNeeded(
                                                            database =
                                                                database,

                                                            workerId =
                                                                worker.id
                                                        )


                                                    }


                                                    // ==================================
                                                    // UNKNOWN
                                                    // ==================================

                                                    else {


                                                        withContext(Dispatchers.Main) {

                                                            recognizedName = "Unknown"
                                                            recognitionStatus = "unknown"

                                                            overlayJob?.cancel()

                                                            overlayJob =
                                                                scope.launch {

                                                                    kotlinx.coroutines.delay(
                                                                        1000L
                                                                    )

                                                                    recognizedName = null
                                                                    recognitionStatus = "none"
                                                                }
                                                        }


                                                        Log.d(
                                                            "FRAP_RECOGNITION",
                                                            "UNKNOWN FACE"
                                                        )
                                                    }


                                                } catch (exception: Exception) {

                                                    Log.e(
                                                        "FRAP_RECOGNITION",
                                                        "Recognition / attendance failed",
                                                        exception
                                                    )

                                                } finally {

                                                    recognitionRunning.set(false)
                                                }
                                            }


                                        } catch (exception: Exception) {

                                            Log.e(
                                                "FRAP_RECOGNITION",
                                                "Recognition processing failed",
                                                exception
                                            )

                                            recognitionRunning.set(false)
                                        }
                                    }


                                } catch (exception: Exception) {

                                    Log.e(
                                        "FRAP_CAMERA",
                                        "Frame processing failed",
                                        exception
                                    )

                                    recognitionRunning.set(false)
                                }
                            }


                            // ==============================================
                            // ML KIT FAILURE
                            // ==============================================

                            .addOnFailureListener(

                                cameraExecutor

                            ) { exception ->

                                Log.e(
                                    "FRAP_FACE",
                                    "Face detection error",
                                    exception
                                )

                                recognitionRunning.set(false)

                                imageProxy.close()
                            }
                    }


                    // ==============================================
                    // FRONT CAMERA
                    // ==============================================

                    val cameraSelector =
                        CameraSelector.DEFAULT_FRONT_CAMERA


                    try {

                        cameraProvider.unbindAll()


                        cameraProvider.bindToLifecycle(

                            context as ComponentActivity,

                            cameraSelector,

                            preview,

                            imageAnalysis
                        )


                        Log.d(
                            "FRAP_CAMERA",
                            "Camera started successfully"
                        )

                    } catch (exception: Exception) {

                        Log.e(
                            "FRAP_CAMERA",
                            "Camera binding failed",
                            exception
                        )
                    }

                }, ContextCompat.getMainExecutor(context))


                previewView
            },

            modifier =
                Modifier.fillMaxSize()
        )


        // ==================================================
        // RECOGNITION OVERLAY
        // ==================================================

        if (recognizedName != null) {

            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(
                            top = 32.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                        .fillMaxWidth()
                        .background(

                            color =
                                if (
                                    recognitionStatus ==
                                    "recognized"
                                ) {

                                    androidx.compose.ui.graphics.Color(
                                        0xCC2E7D32
                                    )

                                } else {

                                    androidx.compose.ui.graphics.Color(
                                        0xCCB71C1C
                                    )
                                },

                            shape =
                                androidx.compose.foundation.shape
                                    .RoundedCornerShape(
                                        14.dp
                                    )
                        )
                        .padding(
                            vertical = 14.dp,
                            horizontal = 16.dp
                        )
            ) {

                Text(

                    text =
                        recognizedName
                            ?: "",

                    modifier =
                        Modifier.fillMaxWidth(),

                    textAlign =
                        androidx.compose.ui.text.style
                            .TextAlign.Center,

                    color =
                        androidx.compose.ui.graphics
                            .Color.White
                )
            }
        }
    }
}

// ==========================================================
// SAVE ATTENDANCE IF NOT YET RECORDED TODAY
// ==========================================================

// ==========================================================
// SAVE ATTENDANCE IF NOT YET RECORDED TODAY
// ==========================================================

suspend fun saveAttendanceIfNeeded(
    database: AppDatabase,
    workerId: Int
) {

    try {

        // ==================================================
        // CURRENT DATE / TIME
        // ==================================================

        val now =
            java.time.LocalDateTime.now()

        val attendanceDate =
            now.toLocalDate().toString()

        val actualTime =
            now.toLocalTime()

        val timeIn =
            actualTime.format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "HH:mm:ss"
                )
            )

        val createdAt =
            now.format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
                )
            )


        // ==================================================
        // CHECK IF ALREADY RECORDED TODAY
        // ==================================================

        val existing =
            database
                .attendanceDao()
                .getAttendanceForDate(
                    workerId = workerId,
                    attendanceDate = attendanceDate
                )

        if (existing != null) {

            Log.d(
                "FRAP_ATTENDANCE",
                "Already recorded today. workerId=$workerId"
            )

            return
        }


        // ==================================================
        // GET TODAY'S SCHEDULE
        // ==================================================

        val todaySchedule =
            database
                .scheduleDao()
                .getScheduleForDate(
                    workerId = workerId,
                    scheduleDate = attendanceDate
                )


        // ==================================================
        // DETERMINE ARRIVAL TIME
        // ==================================================

        val configuredArrivalTime: String

        if (todaySchedule != null) {

            // ==================================================
            // HAS SCHEDULE
            // ==================================================

            val designationTime =
                database
                    .designationTimeDao()
                    .getTimeForDesignation(
                        designationId =
                            todaySchedule.designationId
                    )


            if (designationTime != null) {

                configuredArrivalTime =
                    designationTime.timeIn

                Log.d(
                    "FRAP_ATTENDANCE",
                    "Schedule found. " +
                            "designationId=${todaySchedule.designationId} " +
                            "arrival=$configuredArrivalTime"
                )

            } else {

                // ==================================================
                // SCHEDULE EXISTS BUT DESIGNATION TIME
                // IS NOT CONFIGURED
                //
                // FALL BACK TO NO-SCHEDULE CONFIGURATION
                // ==================================================

                val settingsRepository =
                    com.jrm.frap.data.AppSettingsRepository(
                        database.appSettingsDao()
                    )

                configuredArrivalTime =
                    settingsRepository
                        .getNoScheduleTime()

                Log.d(
                    "FRAP_ATTENDANCE",
                    "Schedule exists but designation time " +
                            "is not configured. " +
                            "Using no-schedule time=$configuredArrivalTime"
                )
            }

        } else {

            // ==================================================
            // NO SCHEDULE
            // ==================================================

            val settingsRepository =
                com.jrm.frap.data.AppSettingsRepository(
                    database.appSettingsDao()
                )

            configuredArrivalTime =
                settingsRepository
                    .getNoScheduleTime()

            Log.d(
                "FRAP_ATTENDANCE",
                "NO SCHEDULE. " +
                        "Using configured arrival time=" +
                        configuredArrivalTime
            )
        }


        // ==================================================
        // PARSE ARRIVAL TIME
        // ==================================================

        val targetTime =
            try {

                java.time.LocalTime.parse(
                    configuredArrivalTime
                )

            } catch (exception: Exception) {

                Log.e(
                    "FRAP_ATTENDANCE",
                    "Invalid configured arrival time: " +
                            configuredArrivalTime +
                            ". Falling back to 08:30.",
                    exception
                )

                java.time.LocalTime.of(
                    8,
                    30
                )
            }


        // ==================================================
        // CALCULATE STATUS
        //
        // actual <= target = ON TIME
        // actual > target  = LATE
        // ==================================================

        val status =
            if (actualTime.isAfter(targetTime)) {

                "LATE"

            } else {

                "ON TIME"
            }


        // ==================================================
        // LOG ARRIVAL INFORMATION
        // ==================================================

        Log.d(
            "FRAP_ATTENDANCE",
            "================================"
        )

        Log.d(
            "FRAP_ATTENDANCE",
            "Worker ID       : $workerId"
        )

        Log.d(
            "FRAP_ATTENDANCE",
            "Date            : $attendanceDate"
        )

        Log.d(
            "FRAP_ATTENDANCE",
            "Actual Time     : $timeIn"
        )

        Log.d(
            "FRAP_ATTENDANCE",
            "Arrival Target  : $configuredArrivalTime"
        )

        Log.d(
            "FRAP_ATTENDANCE",
            "Status          : $status"
        )

        Log.d(
            "FRAP_ATTENDANCE",
            "================================"
        )


        // ==================================================
        // CREATE ATTENDANCE
        // ==================================================

        val attendance =
            com.jrm.frap.data.AttendanceEntity(

                workerId =
                    workerId,

                attendanceDate =
                    attendanceDate,

                timeIn =
                    timeIn,

                method =
                    "face",

                syncStatus =
                    "pending",

                serverId =
                    null,

                eventUuid =
                    java.util.UUID
                        .randomUUID()
                        .toString(),

                createdAt =
                    createdAt
            )


        // ==================================================
        // INSERT
        // ==================================================

        val insertedId =
            database
                .attendanceDao()
                .insertAttendance(
                    attendance
                )


        Log.d(
            "FRAP_ATTENDANCE",
            "ATTENDANCE SAVED"
        )

        Log.d(
            "FRAP_ATTENDANCE",
            "localId=$insertedId"
        )

    } catch (exception: Exception) {

        Log.e(
            "FRAP_ATTENDANCE",
            "Failed to save attendance",
            exception
        )
    }
}