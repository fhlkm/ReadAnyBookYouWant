package com.book.rabyw.domain

import com.book.rabyw.domain.models.Language
import com.book.rabyw.domain.models.TranslationMode
import com.book.rabyw.domain.models.TranslationResult

interface ITranslationService {
    /**
     * Translates text from source language to target language
     * @param text The text to translate
     * @param sourceLanguage Source language (use AUTO_DETECT for automatic detection)
     * @param targetLanguage Target language
     * @param mode Translation mode (FAST for on-device, ACCURATE for server-side)
     * @return TranslationResult containing the translated text and metadata
     */
    suspend fun translateText(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language,
        mode: TranslationMode = TranslationMode.FAST
    ): Result<TranslationResult>
    
    /**
     * Downloads language models for offline translation
     * @param sourceLanguage Source language
     * @param targetLanguage Target language
     * @return true if download was successful, false otherwise
     */
    suspend fun downloadLanguageModels(
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<Boolean>
    
    /**
     * Checks if language models are downloaded for the given language pair
     */
    suspend fun areLanguageModelsDownloaded(
        sourceLanguage: Language,
        targetLanguage: Language
    ): Boolean
    
    /**
     * Checks if translation service is available
     */
    suspend fun isAvailable(): Boolean
}
