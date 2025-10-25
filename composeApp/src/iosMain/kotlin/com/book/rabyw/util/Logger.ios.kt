package com.book.rabyw.util

import platform.Foundation.NSLog

actual object AppLogger {
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val logMessage = if (throwable != null) {
            "[$tag] ERROR: $message\n${throwable.stackTraceToString()}"
        } else {
            "[$tag] ERROR: $message"
        }
        NSLog(logMessage)
    }

    actual fun d(tag: String, message: String) {
        NSLog("[$tag] DEBUG: $message")
    }

    actual fun i(tag: String, message: String) {
        NSLog("[$tag] INFO: $message")
    }
}
