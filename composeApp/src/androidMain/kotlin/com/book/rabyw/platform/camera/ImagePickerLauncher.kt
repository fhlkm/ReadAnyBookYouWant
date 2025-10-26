package com.book.rabyw.platform.camera

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

object ImagePickerLauncher {
    private var launcher: ActivityResultLauncher<String>? = null
    private var currentDeferred: CompletableDeferred<Uri?>? = null

    fun initialize(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            currentDeferred?.complete(uri)
            currentDeferred = null
        }
    }

    suspend fun pickImage(mimeType: String = "image/*"): Uri? {
        return try {
            val deferred = CompletableDeferred<Uri?>()
            currentDeferred = deferred
            launcher?.launch(mimeType) ?: run {
                deferred.complete(null)
                return null
            }
            deferred.await()
        } catch (e: Exception) {
            e.printStackTrace()
            currentDeferred?.complete(null)
            currentDeferred = null
            null
        }
    }

    fun isInitialized(): Boolean = launcher != null
}
