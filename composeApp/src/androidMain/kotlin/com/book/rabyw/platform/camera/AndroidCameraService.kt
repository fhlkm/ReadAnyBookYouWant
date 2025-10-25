package com.book.rabyw.platform.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.book.rabyw.domain.ICameraService
import com.book.rabyw.domain.models.CapturedImage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import com.book.rabyw.platform.permissions.PermissionController
import androidx.activity.ComponentActivity
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class AndroidCameraService(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : ICameraService {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override suspend fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun requestCameraPermission(): Boolean {
        // If already granted, short-circuit
        if (hasCameraPermission()) return true
        val activity = lifecycleOwner as? ComponentActivity ?: return false
        return PermissionController.requestCamera(activity)
    }

    override suspend fun captureImage(): Flow<CapturedImage?> = callbackFlow {
        if (!hasCameraPermission()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageCapture
            )

            this@AndroidCameraService.imageCapture = imageCapture
            this@AndroidCameraService.cameraProvider = cameraProvider

            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                        try {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)

                            val capturedImage = CapturedImage(
                                imageData = bytes,
                                width = image.width,
                                height = image.height
                            )

                            trySend(capturedImage)
                            close()
                        } catch (e: Exception) {
                            trySend(null)
                            close()
                        } finally {
                            image.close()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        trySend(null)
                        close()
                    }
                }
            )

        } catch (e: Exception) {
            trySend(null)
            close()
        }

        awaitClose {
            cameraProvider?.unbindAll()
        }
    }
}
