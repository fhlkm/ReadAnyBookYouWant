package com.book.rabyw.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CapturedImage(
    val imageData: ByteArray,
    val width: Int,
    val height: Int,
    val timestamp: Long = getCurrentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CapturedImage

        if (!imageData.contentEquals(other.imageData)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageData.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

expect fun getCurrentTimeMillis(): Long
