package com.book.rabyw

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform