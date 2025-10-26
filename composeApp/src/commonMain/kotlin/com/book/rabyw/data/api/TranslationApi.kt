package com.book.rabyw.data.api

import com.book.rabyw.util.AppLogger
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
    val max_tokens: Int = 500
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
    val confidence: Float? = null
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
                model = "gpt-5-nano",
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
                val translatedText = response.choices.first().message.content.trim()
                Result.success(TranslationResponse(translatedText = translatedText))
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
        val sourceLangName = getLanguageName(sourceLanguage)
        val targetLangName = getLanguageName(targetLanguage)
        
        return """
            Translate the following text from $sourceLangName to $targetLangName. 
            Only return the translated text, nothing else. Do not include any explanations or additional text.
            
            Text to translate: $text
        """.trimIndent()
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
