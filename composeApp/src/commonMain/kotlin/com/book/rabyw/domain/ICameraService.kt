package com.book.rabyw.domain

import com.book.rabyw.domain.models.CapturedImage
import kotlinx.coroutines.flow.Flow

interface ICameraService {
    /**
     * Captures an image from the device camera
     * @return Flow that emits the captured image or null if capture was cancelled
     */
    suspend fun captureImage(): Flow<CapturedImage?>
    
    /**
     * Checks if camera permission is granted
     */
    suspend fun hasCameraPermission(): Boolean
    
    /**
     * Requests camera permission from the user
     * @return true if permission is granted, false otherwise
     */
    suspend fun requestCameraPermission(): Boolean
}
