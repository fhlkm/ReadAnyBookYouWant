package com.book.rabyw.platform.permissions

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
}


