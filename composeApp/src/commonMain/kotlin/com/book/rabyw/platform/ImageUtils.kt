package com.book.rabyw.platform

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Converts a ByteArray (JPEG/PNG data) to an ImageBitmap for display in Compose
 */
expect fun ByteArray.toImageBitmap(): ImageBitmap?
