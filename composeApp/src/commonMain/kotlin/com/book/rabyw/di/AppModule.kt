package com.book.rabyw.di

import com.book.rabyw.data.api.TranslationApi
import com.book.rabyw.data.repository.TranslationRepository
import com.book.rabyw.domain.ICameraService
import com.book.rabyw.domain.IOcrService
import com.book.rabyw.domain.ITranslationService

expect object AppModule {
    fun provideCameraService(): ICameraService
    fun provideOcrService(): IOcrService
    fun provideTranslationService(): ITranslationService
}
