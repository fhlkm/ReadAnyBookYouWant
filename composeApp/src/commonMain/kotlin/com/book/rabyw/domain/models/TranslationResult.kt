package com.book.rabyw.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    CHINESE_SIMPLIFIED("zh", "简体中文"),
    CHINESE_TRADITIONAL("zh-TW", "繁體中文"),
    AUTO_DETECT("auto", "Auto Detect");

    companion object {
        fun fromCode(code: String): Language? {
            return values().find { it.code == code }
        }
    }
}

@Serializable
enum class TranslationMode {
    FAST, // On-device ML Kit
    ACCURATE // Server-side API
}

@Serializable
data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
    val mode: TranslationMode,
    val confidence: Float? = null,
    val timestamp: Long = getCurrentTimeMillis()
)
