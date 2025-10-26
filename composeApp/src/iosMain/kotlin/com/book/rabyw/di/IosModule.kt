package com.book.rabyw.di

import com.book.rabyw.config.AppConfig
import com.book.rabyw.data.api.TranslationApi
import com.book.rabyw.data.repository.TranslationRepository
import com.book.rabyw.domain.ICameraService
import com.book.rabyw.domain.IOcrService
import com.book.rabyw.domain.ITranslationService
import com.book.rabyw.platform.camera.IosCameraService
import com.book.rabyw.platform.ocr.IosOcrService
import com.book.rabyw.platform.translation.IosTranslationService
import platform.UIKit.UIViewController

actual object AppModule {
    
    private var viewController: UIViewController? = null
    
    fun initialize(viewController: UIViewController) {
        this.viewController = viewController
    }
    
    actual fun provideCameraService(): ICameraService {
        val vc = viewController ?: throw IllegalStateException("AppModule not initialized")
        return IosCameraService(vc)
    }
    
    actual fun provideOcrService(): IOcrService {
        return IosOcrService()
    }
    
    actual fun provideTranslationService(): ITranslationService {
        val onDeviceService = IosTranslationService()
        val serverApi = TranslationApi(AppConfig.OPENAI_API_KEY)
        return TranslationRepository(onDeviceService, serverApi)
    }
}
