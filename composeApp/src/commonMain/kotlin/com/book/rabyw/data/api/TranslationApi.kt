package com.book.rabyw.data.api

import com.book.rabyw.util.AppLogger
import com.book.rabyw.alignment.TranslateAlignResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OpenAITranslationRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.1,
    val max_tokens: Int = 2000
)

@Serializable
data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAIResponse(
    val choices: List<OpenAIChoice>? = null,
    val error: OpenAIError? = null
)

@Serializable
data class OpenAIChoice(
    val message: OpenAIMessage
)

@Serializable
data class OpenAIError(
    val message: String,
    val type: String? = null,
    val code: String? = null
)

@Serializable
data class TranslationRequest(
    val text: String,
    val sourceLanguage: String,
    val targetLanguage: String
)

@Serializable
data class TranslationResponse(
    val translatedText: String,
    val confidence: Float? = null,
    val translateAlign: TranslateAlignResponse? = null
)

class TranslationApi(
    private val apiKey: String? = null
) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    suspend fun translateText(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        apiKey: String? = null
    ): Result<TranslationResponse> {
        return try {
            val apiKeyToUse = apiKey ?: this.apiKey ?: throw IllegalArgumentException("OpenAI API key is required")
            
            val prompt = buildTranslationPrompt(text, sourceLanguage, targetLanguage)
            val request = OpenAITranslationRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    OpenAIMessage(role = "user", content = prompt)
                )
            )
            
            // Debug: Log the request being sent
            val requestJson = Json.encodeToString(OpenAITranslationRequest.serializer(), request)
            AppLogger.d("TranslationApi", "Sending request: $requestJson")
            
            val response = try {
                val rawResponse = client.post("https://api.openai.com/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    header("Authorization", "Bearer $apiKeyToUse")
                }
                
                val responseText = rawResponse.body<String>()
                AppLogger.d("TranslationApi", "Raw OpenAI response: $responseText")
                
                // Try to parse the response manually first
                try {
                    rawResponse.body<OpenAIResponse>()
                } catch (parseError: Exception) {
                    AppLogger.e("TranslationApi", "Failed to parse response: ${parseError.message}")
                    AppLogger.e("TranslationApi", "Response was: $responseText")
                    throw parseError
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to GPT-4o-mini if GPT-5-nano is not available
                if (request.model == "gpt-5-nano") {
                    AppLogger.d("TranslationApi", "GPT-5-nano not available, falling back to GPT-4o-mini")
                    val fallbackRequest = OpenAITranslationRequest(
                        model = "gpt-4o-mini",
                        messages = request.messages,
                        temperature = request.temperature,
                        max_tokens = request.max_tokens
                    )
                    val rawResponse = client.post("https://api.openai.com/v1/chat/completions") {
                        contentType(ContentType.Application.Json)
                        setBody(fallbackRequest)
                        header("Authorization", "Bearer $apiKeyToUse")
                    }
                    
                    val responseText = rawResponse.body<String>()
                    AppLogger.d("TranslationApi", "Raw OpenAI fallback response: $responseText")
                    
                    rawResponse.body<OpenAIResponse>()
                } else {
                    throw e
                }
            }
            
            // Check for API errors first
            if (response.error != null) {
                AppLogger.e("TranslationApi", "OpenAI API error: ${response.error.message}")
                return Result.failure(Exception("OpenAI API error: ${response.error.message}"))
            }
            
            // Check for successful response
            if (response.choices != null && response.choices.isNotEmpty()) {
                val content = response.choices.first().message.content.trim()
                // Try to parse translate+align JSON first
                val parsed = runCatching {
                    Json { ignoreUnknownKeys = true }.decodeFromString(
                        TranslateAlignResponse.serializer(), content
                    )
                }
                val ta = parsed.getOrThrow()

                return Result.success(
                    TranslationResponse(
                        translatedText = ta.translation,
                        confidence = null,
                        translateAlign = ta
                    )
                )
            } else {
                AppLogger.e("TranslationApi", "No choices in OpenAI response")
                Result.failure(Exception("No translation response from OpenAI"))
            }
        } catch (e: Exception) {
            AppLogger.e("TranslationApi", "OpenAI Translation API error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun buildTranslationPrompt(text: String, sourceLanguage: String, targetLanguage: String): String {
        return (
            """
            You are a bilingual alignment expert.
            Task: Translate the source text into the target language AND produce phrase-level alignment between the source and your translation.
            Return ONLY valid minified JSON exactly matching this schema (no prose):
            {"source_language":string,"target_language":string,"translation":string,"alignment":[{"source":{"start_char":number,"end_char":number,"text":string},"target":{"start_char":number,"end_char":number,"text":string},"confidence":number}]}
            
            Constraints:
            - Character indices are 0-based, end-exclusive, using UTF-16 code units (Kotlin) for BOTH source and target.
            - Phrases must be contiguous, ordered, and non-overlapping; merge function words with adjacent content.
            - Omit punctuation-only groups. All span texts must be exact substrings of the provided texts.
            - Use the given language identifiers verbatim.
            - Granularity: Do NOT output a single alignment covering the whole text. Create fine-grained phrase pairs.
              • For short sentences: at least 3–6 pairs per sentence when possible.
              • For long passages (>300 chars): aim for 12–60 pairs overall.
              • Max span size: CJK ≤ 16 characters; space-delimited languages ≤ 6 words per span.
              • No single pair may cover >25% of the source characters; split it.
              • Cover ~90% of content characters across pairs.
            - Guidance: split by clauses, noun/verb phrases, named entities, numbers, units, dates, idioms; keep pairs semantically coherent.
            
            Source language: $sourceLanguage
            Target language: $targetLanguage
            
            Source text:
            $text
            """
        ).trimIndent()
    }

    private fun buildAlignmentOnlyPrompt(sourceText: String, targetText: String, sourceLanguage: String, targetLanguage: String): String {
        return (
            """
            You are a bilingual alignment expert.
            Task: Produce phrase-level alignment ONLY for the given source and target texts (do not re-translate). Return ONLY valid minified JSON exactly matching this schema (no prose):
            {"source_language":string,"target_language":string,"translation":string,"alignment":[{"source":{"start_char":number,"end_char":number,"text":string},"target":{"start_char":number,"end_char":number,"text":string},"confidence":number}]}
            
            Constraints:
            - Use the provided source and target texts verbatim; do not alter them.
            - Character indices are 0-based, end-exclusive, UTF-16 (Kotlin).
            - Fine-grained alignment required; do not output one giant span. Max CJK span 12 chars; max space-delimited span 5 words; no span covers >15% of the source.
            - Aim to cover ~90% of content characters. Phrases are contiguous, ordered, non-overlapping. Omit punctuation-only groups. Do not cross major punctuation.
            
            Source language: $sourceLanguage
            Target language: $targetLanguage
            
            Source text:
            $sourceText
            
            Target text:
            $targetText
            """
        ).trimIndent()
    }

    private fun buildAlignmentOnlyPromptGranular(sourceText: String, targetText: String, sourceLanguage: String, targetLanguage: String): String {
        return (
            """
            You are a bilingual alignment expert.
            Task: Produce FINE-GRAINED phrase-level alignment ONLY. Return ONLY minified JSON EXACTLY in this schema (no prose):
            {"source_language":string,"target_language":string,"translation":string,"alignment":[{"source":{"start_char":number,"end_char":number,"text":string},"target":{"start_char":number,"end_char":number,"text":string},"confidence":number}]}
            
            Hard constraints (must satisfy):
            - Character indices are 0-based, end-exclusive, UTF-16 (Kotlin).
            - Do not output sentence-level spans. Max CJK span 10 chars; max space-delimited span 4 words.
            - Each sentence should have 4–10 pairs when possible; entire text 24–120 pairs depending on length.
            - No single pair covers >10% of source characters. Avoid crossing major punctuation (.,!?; 。！？；).
            - Cover ~90% of content characters; omit punctuation-only.
            
            Source language: $sourceLanguage
            Target language: $targetLanguage
            
            Source text:
            $sourceText
            
            Target text:
            $targetText
            """
        ).trimIndent()
    }
    
    private fun getLanguageName(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "en" -> "English"
            "zh" -> "Chinese (Simplified)"
            "zh-tw" -> "Chinese (Traditional)"
            "auto" -> "the detected language"
            else -> languageCode
        }
    }
    
    fun close() {
        client.close()
    }
}
