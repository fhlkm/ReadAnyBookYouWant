package com.book.rabyw.platform.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PermissionController {
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var pendingContinuation: CancellableContinuation<Boolean>? = null

    fun init(activity: ComponentActivity) {
        if (requestPermissionLauncher != null) return
        requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            pendingContinuation?.resume(granted)
            pendingContinuation = null
        }
    }

    suspend fun requestCamera(activity: ComponentActivity): Boolean = suspendCancellableCoroutine { cont ->
        pendingContinuation = cont
        requestPermissionLauncher?.launch(Manifest.permission.CAMERA)
            ?: run {
                // Not initialized; fail safe
                pendingContinuation = null
                cont.resume(false)
            }
    }

    /**
     * Checks if storage permission is granted
     */
    fun hasStoragePermission(activity: ComponentActivity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 and below
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Requests storage permission based on Android version
     */
    suspend fun requestStoragePermission(activity: ComponentActivity): Boolean = suspendCancellableCoroutine { cont ->
        pendingContinuation = cont
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissionLauncher?.launch(permission)
            ?: run {
                pendingContinuation = null
                cont.resume(false)
            }
    }
}


