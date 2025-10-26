package com.book.rabyw.platform.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.book.rabyw.domain.IOcrService
import com.book.rabyw.domain.models.BoundingBox
import com.book.rabyw.domain.models.CapturedImage
import com.book.rabyw.domain.models.RecognizedText
import com.book.rabyw.domain.models.TextBlock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidOcrService : IOcrService {

    private val chineseTextRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

//    private val englishTextRecognizer = TextRecognition.getClient(EnglishTextRecognizerOptions.Builder().build())
    private val latinTextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(image: CapturedImage): Result<RecognizedText> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val bitmap = BitmapFactory.decodeByteArray(image.imageData, 0, image.imageData.size)
                Log.d("AndroidOcrService", "before fromBitmap Bitmap size: ${bitmap.width}x${bitmap.height}")

                val inputImage = InputImage.fromBitmap(bitmap, 0)
                Log.d("AndroidOcrService", "after fromBitmap ")
                // Try Chinese recognition first, then fallback to Latin
                chineseTextRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val recognizedText = processVisionText(visionText, image)
                        continuation.resume(Result.success(recognizedText))
                        Log.d("AndroidOcrService", "chineseTextRecognizer is:  $recognizedText")
                    }
                    .addOnFailureListener { chineseError ->
                        // Fallback to Latin recognition
                        latinTextRecognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                val recognizedText = processVisionText(visionText, image)
                                Log.d("AndroidOcrService", "latinTextRecognizer success  is:  $recognizedText" )

                                continuation.resume(Result.success(recognizedText))
                            }
                            .addOnFailureListener { latinError ->
                                Log.d("AndroidOcrService", "latinTextRecognizer failed ")
                                continuation.resume(Result.failure(Exception("OCR failed: Chinese=${chineseError.message}, Latin=${latinError.message}")))
                            }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun processVisionText(visionText: com.google.mlkit.vision.text.Text, image: CapturedImage): RecognizedText {
        val textBlocks = mutableListOf<TextBlock>()
        
        for (block in visionText.textBlocks) {
            val boundingBox = block.boundingBox?.let { rect ->
                BoundingBox(
                    left = rect.left.toFloat(),
                    top = rect.top.toFloat(),
                    right = rect.right.toFloat(),
                    bottom = rect.bottom.toFloat()
                )
            } ?: BoundingBox(0f, 0f, 0f, 0f)

            textBlocks.add(
                TextBlock(
                    text = block.text,
                    boundingBox = boundingBox,
                    confidence = 0.8f // ML Kit doesn't provide confidence scores directly
                )
            )
        }

        return RecognizedText(
            fullText = visionText.text,
            textBlocks = textBlocks,
            detectedLanguage = detectLanguage(visionText.text),
            confidence = calculateOverallConfidence(textBlocks)
        )
    }

    private fun detectLanguage(text: String): String? {
        // Simple language detection based on character patterns
        val chinesePattern = Regex("[\\u4e00-\\u9fff]")
        val englishPattern = Regex("[a-zA-Z]")
        
        val chineseCount = chinesePattern.findAll(text).count()
        val englishCount = englishPattern.findAll(text).count()
        
        return when {
            chineseCount > englishCount -> "zh"
            englishCount > chineseCount -> "en"
            else -> null
        }
    }

    private fun calculateOverallConfidence(textBlocks: List<TextBlock>): Float {
        if (textBlocks.isEmpty()) return 0f
        return textBlocks.map { it.confidence }.average().toFloat()
    }

    override suspend fun isAvailable(): Boolean = true
}
