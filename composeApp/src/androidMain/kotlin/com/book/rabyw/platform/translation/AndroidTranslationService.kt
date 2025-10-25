package com.book.rabyw.platform.translation

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.book.rabyw.domain.ITranslationService
import com.book.rabyw.domain.models.Language
import com.book.rabyw.domain.models.TranslationMode
import com.book.rabyw.domain.models.TranslationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidTranslationService : ITranslationService {

    private val translators = mutableMapOf<String, Translator>()

    override suspend fun translateText(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language,
        mode: TranslationMode
    ): Result<TranslationResult> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val translator = getOrCreateTranslator(sourceLanguage, targetLanguage)
                
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        val result = TranslationResult(
                            originalText = text,
                            translatedText = translatedText,
                            sourceLanguage = sourceLanguage,
                            targetLanguage = targetLanguage,
                            mode = mode,
                            confidence = 0.8f // ML Kit doesn't provide confidence scores
                        )
                        continuation.resume(Result.success(result))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    override suspend fun downloadLanguageModels(
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val translator = getOrCreateTranslator(sourceLanguage, targetLanguage)
                
                translator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        continuation.resume(Result.success(true))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    override suspend fun areLanguageModelsDownloaded(
        sourceLanguage: Language,
        targetLanguage: Language
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                val translator = getOrCreateTranslator(sourceLanguage, targetLanguage)
                
                translator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        continuation.resume(true)
                    }
                    .addOnFailureListener {
                        continuation.resume(false)
                    }
            } catch (e: Exception) {
                continuation.resume(false)
            }
        }
    }

    override suspend fun isAvailable(): Boolean = true

    private fun getOrCreateTranslator(sourceLanguage: Language, targetLanguage: Language): Translator {
        val key = "${sourceLanguage.code}_${targetLanguage.code}"
        
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(mapLanguageToMLKit(sourceLanguage))
                .setTargetLanguage(mapLanguageToMLKit(targetLanguage))
                .build()
            
            Translation.getClient(options)
        }
    }

    private fun mapLanguageToMLKit(language: Language): String {
        return when (language) {
            Language.ENGLISH -> TranslateLanguage.ENGLISH
            Language.CHINESE_SIMPLIFIED -> TranslateLanguage.CHINESE
            Language.CHINESE_TRADITIONAL -> TranslateLanguage.CHINESE
            Language.AUTO_DETECT -> TranslateLanguage.ENGLISH // Fallback to English
        }
    }
}
