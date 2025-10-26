package com.book.rabyw.alignment

fun segmentSentences(text: String, language: String): List<IntRange> {
    if (text.isEmpty()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var start = 0
    val delimiters = setOf('。','！','？','；','!','?',';','\n')
    for (i in text.indices) {
        val ch = text[i]
        if (delimiters.contains(ch)) {
            val end = i + 1
            if (end > start) ranges.add(IntRange(start, end))
            start = end
        }
    }
    if (start < text.length) {
        ranges.add(IntRange(start, text.length))
    }
    return ranges
}


