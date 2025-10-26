package com.book.rabyw.platform.camera

import com.book.rabyw.domain.ICameraService
import com.book.rabyw.domain.models.CapturedImage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.Foundation.NSData
import platform.UIKit.*
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

    override suspend fun captureImage(): Flow<CapturedImage?> =
        launchImagePicker(UIImagePickerControllerSourceTypeCamera)

    override suspend fun loadImage(): Flow<CapturedImage?> =
        launchImagePicker(UIImagePickerControllerSourceTypePhotoLibrary)
    
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
                    val byteArray = NSDataToByteArray(imageData)
                    
                    val capturedImage = CapturedImage(
                        imageData = byteArray,
                        width = image.size.width.toInt(),
                        height = image.size.height.toInt()
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
    
    private fun NSDataToByteArray(nsData: NSData?): ByteArray {
        if (nsData == null) return ByteArray(0)
        val length = nsData.length.toInt()
        val byteArray = ByteArray(length)
        val bytes = byteArray.usePinned { it.addressOf(0) }
        nsData.getBytes(bytes, platform.posix.nav.C.NS(length))
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
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerInfoOriginalImageKey] as? UIImage
        onImageSelected(image)
    }
    
    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onImageSelected(null)
    }
}
