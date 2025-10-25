package com.book.rabyw.di

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.book.rabyw.data.api.TranslationApi
import com.book.rabyw.data.repository.TranslationRepository
import com.book.rabyw.domain.ICameraService
import com.book.rabyw.domain.IOcrService
import com.book.rabyw.domain.ITranslationService
import com.book.rabyw.platform.camera.AndroidCameraService
import com.book.rabyw.platform.ocr.AndroidOcrService
import com.book.rabyw.platform.translation.AndroidTranslationService

actual object AppModule {
    
    private var context: Context? = null
    private var lifecycleOwner: LifecycleOwner? = null
    
    fun initialize(context: Context, lifecycleOwner: LifecycleOwner) {
        this.context = context
        this.lifecycleOwner = lifecycleOwner
    }
    
    actual fun provideCameraService(): ICameraService {
        val ctx = context ?: throw IllegalStateException("AppModule not initialized")
        val lifecycle = lifecycleOwner ?: throw IllegalStateException("AppModule not initialized")
        return AndroidCameraService(ctx, lifecycle)
    }
    
    actual fun provideOcrService(): IOcrService {
        return AndroidOcrService()
    }
    
    actual fun provideTranslationService(): ITranslationService {
        val onDeviceService = AndroidTranslationService()
        val serverApi = TranslationApi()
        return TranslationRepository(onDeviceService, serverApi)
    }
}
