package com.book.rabyw.platform.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import com.book.rabyw.domain.ICameraService
import com.book.rabyw.domain.models.CapturedImage
import com.book.rabyw.platform.permissions.PermissionController
import com.book.rabyw.ui.TAG
import com.book.rabyw.util.AppLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.ByteArrayOutputStream
import java.io.File

class AndroidCameraService(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : ICameraService {

    override suspend fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun requestCameraPermission(): Boolean {
        if (hasCameraPermission()) return true
        val activity = lifecycleOwner as? ComponentActivity ?: return false
        return PermissionController.requestCamera(activity)
    }

    override suspend fun captureImage(): Flow<CapturedImage?> = callbackFlow {
        AppLogger.i(TAG, "captureImage: starting")
        
        if (!hasCameraPermission()) {
            AppLogger.i(TAG, "captureImage: no camera permission")
            trySend(null)
            close()
            return@callbackFlow
        }

        if (!CameraLauncher.isInitialized()) {
            AppLogger.i(TAG, "captureImage: camera launcher not initialized")
            trySend(null)
            close()
            return@callbackFlow
        }

        val activity = lifecycleOwner as? ComponentActivity
        if (activity == null) {
            AppLogger.i(TAG, "captureImage: activity is null")
            trySend(null)
            close()
            return@callbackFlow
        }

        try {
            // Create a temporary file for the photo
            val photoFile = File.createTempFile(
                "photo_${System.currentTimeMillis()}",
                ".jpg",
                context.cacheDir
            )

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            
            AppLogger.i(TAG, "captureImage: launching camera with URI: $uri")
            
            // Launch camera and wait for result
            val success = CameraLauncher.takePicture(uri)
            
            AppLogger.i(TAG, "captureImage: camera result: $success")
            
            if (success) {
                try {
                    AppLogger.i(TAG, "captureImage: processing captured image")
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        activity.contentResolver,
                        uri
                    )
                    // Fix orientation based on EXIF data
                    val rotatedBitmap = fixImageOrientation(bitmap, photoFile)
                    val capturedImage = convertBitmapToCapturedImage(rotatedBitmap)
                    AppLogger.i(TAG, "captureImage: image processed successfully")
                    trySend(capturedImage)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "captureImage: error processing image: ${e.message}")
                    e.printStackTrace()
                    trySend(null)
                }
            } else {
                AppLogger.i(TAG, "captureImage: camera capture failed")
                trySend(null)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "captureImage: error: ${e.message}")
            e.printStackTrace()
            trySend(null)
        } finally {
            AppLogger.i(TAG, "captureImage: closing flow")
            close()
        }

        awaitClose {
            AppLogger.i(TAG, "captureImage: flow closed")
        }
    }

    /**
     * Fixes the image orientation based on EXIF data
     */
    private fun fixImageOrientation(bitmap: Bitmap, photoFile: File): Bitmap {
        try {
            val exif = ExifInterface(photoFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap
            }

            return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            ).also {
                if (it != bitmap) {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            return bitmap
        }
    }

    private fun convertBitmapToCapturedImage(bitmap: Bitmap): CapturedImage {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val byteArray = stream.toByteArray()

        return CapturedImage(
            imageData = byteArray,
            width = bitmap.width,
            height = bitmap.height
        )
    }
}
