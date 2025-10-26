package com.book.rabyw.data.repository

import com.book.rabyw.data.api.TranslationApi
import com.book.rabyw.domain.ITranslationService
import com.book.rabyw.alignment.TranslateAlignResponse
import com.book.rabyw.domain.models.Language
import com.book.rabyw.domain.models.TranslationMode
import com.book.rabyw.domain.models.TranslationResult

class TranslationRepository(
    private val onDeviceService: ITranslationService,
    private val serverApi: TranslationApi
) : ITranslationService {
    
    override suspend fun translateText(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language,
        mode: TranslationMode
    ): Result<TranslationResult> {
        return when (mode) {
            TranslationMode.FAST -> {
                // Use on-device translation
                onDeviceService.translateText(text, sourceLanguage, targetLanguage, mode)
            }
            TranslationMode.ACCURATE -> {
                // Try server-side translation first, fallback to on-device
                val serverResult = serverApi.translateText(
                    text = text,
                    sourceLanguage = mapLanguageToCode(sourceLanguage),
                    targetLanguage = mapLanguageToCode(targetLanguage)
                )
                
                if (serverResult.isSuccess) {
                    val response = serverResult.getOrThrow()
                    Result.success(
                        TranslationResult(
                            originalText = text,
                            translatedText = response.translatedText,
                            sourceLanguage = sourceLanguage,
                            targetLanguage = targetLanguage,
                            mode = mode,
                            confidence = response.confidence,
                            translateAlign = response.translateAlign
                        )
                    )
                } else {
                    // Fallback to on-device translation
                    onDeviceService.translateText(text, sourceLanguage, targetLanguage, TranslationMode.FAST)
                }
            }
        }
    }
    
    override suspend fun downloadLanguageModels(
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<Boolean> {
        return onDeviceService.downloadLanguageModels(sourceLanguage, targetLanguage)
    }
    
    override suspend fun areLanguageModelsDownloaded(
        sourceLanguage: Language,
        targetLanguage: Language
    ): Boolean {
        return onDeviceService.areLanguageModelsDownloaded(sourceLanguage, targetLanguage)
    }
    
    override suspend fun isAvailable(): Boolean {
        return onDeviceService.isAvailable()
    }
    
    private fun mapLanguageToCode(language: Language): String {
        return when (language) {
            Language.ENGLISH -> "en"
            Language.CHINESE_SIMPLIFIED -> "zh"
            Language.CHINESE_TRADITIONAL -> "zh-TW"
            Language.AUTO_DETECT -> "auto"
        }
    }
}
