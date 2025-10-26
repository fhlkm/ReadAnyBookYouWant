package com.book.rabyw.platform.camera

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

/**
 * Singleton to manage camera launch and result handling.
 * Must be initialized early in the activity lifecycle (during onCreate).
 */
object CameraLauncher {
    private var launcher: ActivityResultLauncher<Uri>? = null
    private var currentDeferred: CompletableDeferred<Boolean>? = null

    /**
     * Initialize the launcher. Must be called during onCreate, before STARTED state.
     */
    fun initialize(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            currentDeferred?.complete(success)
            currentDeferred = null
        }
    }

    /**
     * Launch the camera to take a picture.
     * @param uri The URI where the photo will be saved
     * @return true if photo was taken successfully, false otherwise
     */
    suspend fun takePicture(uri: Uri): Boolean {
        return try {
            val deferred = CompletableDeferred<Boolean>()
            currentDeferred = deferred

            launcher?.launch(uri) ?: run {
                deferred.complete(false)
                return false
            }

            deferred.await()
        } catch (e: Exception) {
            e.printStackTrace()
            currentDeferred?.complete(false)
            currentDeferred = null
            false
        }
    }

    fun isInitialized(): Boolean = launcher != null
}
