package com.book.rabyw.alignment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlignmentSpan(
    @SerialName("start_char") val start: Int,
    @SerialName("end_char") val end: Int,
    val text: String
)

@Serializable
data class AlignmentPair(
    val source: AlignmentSpan,
    val target: AlignmentSpan,
    val confidence: Float
)

@Serializable
data class TranslateAlignResponse(
    @SerialName("source_language") val sourceLanguage: String,
    @SerialName("target_language") val targetLanguage: String,
    val translation: String,
    val alignment: List<AlignmentPair>
)


