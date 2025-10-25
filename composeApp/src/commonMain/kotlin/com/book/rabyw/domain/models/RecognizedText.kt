package com.book.rabyw.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class TextBlock(
    val text: String,
    val boundingBox: BoundingBox,
    val confidence: Float
)

@Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Serializable
data class RecognizedText(
    val fullText: String,
    val textBlocks: List<TextBlock>,
    val detectedLanguage: String? = null,
    val confidence: Float
)
