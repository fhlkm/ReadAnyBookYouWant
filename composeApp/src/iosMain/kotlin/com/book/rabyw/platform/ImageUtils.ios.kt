package com.book.rabyw.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        this.usePinned { pinned ->
            val nsData = NSData.create(
                bytes = pinned.addressOf(0),
                length = this.size.toULong()
            )
            val uiImage = UIImage.imageWithData(nsData)
            if (uiImage != null) {
                val skiaImage = Image.makeFromEncoded(this)
                skiaImage?.toComposeImageBitmap()
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}
