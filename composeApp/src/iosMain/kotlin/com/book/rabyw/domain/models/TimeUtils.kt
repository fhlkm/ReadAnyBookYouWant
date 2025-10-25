package com.book.rabyw.domain.models

actual fun getCurrentTimeMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
