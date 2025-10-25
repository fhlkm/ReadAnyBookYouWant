package com.book.rabyw.domain

import com.book.rabyw.domain.models.CapturedImage
import com.book.rabyw.domain.models.RecognizedText

interface IOcrService {
    /**
     * Recognizes text from a captured image
     * @param image The captured image to process
     * @return RecognizedText containing the extracted text and metadata
     */
    suspend fun recognizeText(image: CapturedImage): Result<RecognizedText>
    
    /**
     * Checks if OCR is available on the current platform
     */
    suspend fun isAvailable(): Boolean
}
