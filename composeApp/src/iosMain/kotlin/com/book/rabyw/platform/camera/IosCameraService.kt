package com.book.rabyw.platform.camera

import com.book.rabyw.domain.ICameraService
import com.book.rabyw.domain.models.CapturedImage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.UIKit.UIViewController
import kotlin.coroutines.resume

class IosCameraService(
    private val viewController: UIViewController
) : ICameraService {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun hasCameraPermission(): Boolean {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        return status == AVAuthorizationStatusAuthorized
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun requestCameraPermission(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
            when (status) {
                AVAuthorizationStatusAuthorized -> continuation.resume(true)
                AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> continuation.resume(false)
                AVAuthorizationStatusNotDetermined -> {
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        continuation.resume(granted)
                    }
                }
                else -> continuation.resume(false)
            }
        }
    }

    override suspend fun captureImage(): Flow<CapturedImage?> = callbackFlow {
        // Placeholder implementation - in real app, implement actual camera capture
        // For now, create a dummy image
        val dummyImageData = ByteArray(1024) { it.toByte() }
        val capturedImage = CapturedImage(
            imageData = dummyImageData,
            width = 800,
            height = 600
        )
        
        trySend(capturedImage)
        close()
        
        awaitClose {
            // Cleanup if needed
        }
    }
}
