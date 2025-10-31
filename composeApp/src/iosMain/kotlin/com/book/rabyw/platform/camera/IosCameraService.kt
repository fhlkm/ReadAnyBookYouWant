package com.book.rabyw.platform.camera

import com.book.rabyw.domain.ICameraService
import com.book.rabyw.domain.models.CapturedImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.Foundation.NSData
import platform.CoreGraphics.CGSize
import platform.darwin.NSObject
import platform.UIKit.*
import platform.posix.memcpy
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

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun captureImage(): Flow<CapturedImage?> =
        launchImagePicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun loadImage(): Flow<CapturedImage?> =
        launchImagePicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
    
    @OptIn(ExperimentalForeignApi::class)
    private fun launchImagePicker(sourceType: UIImagePickerControllerSourceType): Flow<CapturedImage?> = callbackFlow {
        // Check if source type is available
        if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val pickerController = UIImagePickerController()
        pickerController.sourceType = sourceType
        pickerController.allowsEditing = false
        
        val delegate = ImagePickerDelegate { image ->
            if (image != null) {
                try {
                    val imageData = UIImageJPEGRepresentation(image, 0.9)
                    val byteArray = imageData.toByteArray()
                    
                    val size: CGSize = image.size.useContents { this }

                    val capturedImage = CapturedImage(
                        imageData = byteArray,
                        width = size.width.toInt(),
                        height = size.height.toInt()
                    )
                    
                    trySend(capturedImage)
                } catch (e: Exception) {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
            close()
        }
        
        pickerController.delegate = delegate
        
        platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
            viewController.presentViewController(pickerController, animated = true, completion = null)
        }
        
        awaitClose {
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                pickerController.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private fun NSData?.toByteArray(): ByteArray {
        val data = this ?: return ByteArray(0)
        val length = data.length.toInt()
        val byteArray = ByteArray(length)
        val source = data.bytes?.reinterpret<ByteVar>()
        if (source != null) {
            byteArray.usePinned { pinned ->
                memcpy(pinned.addressOf(0), source, data.length)
            }
        }
        return byteArray
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onImageSelected: (UIImage?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        onImageSelected(image)
    }
    
    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onImageSelected(null)
    }
}
