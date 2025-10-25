package com.book.rabyw.util

/**
 * Simple cross-platform logger for KMM
 */
expect object AppLogger {
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
}
