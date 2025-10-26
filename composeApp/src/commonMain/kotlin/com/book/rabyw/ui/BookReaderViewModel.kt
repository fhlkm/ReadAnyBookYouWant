package com.book.rabyw.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.book.rabyw.domain.ICameraService
import com.book.rabyw.domain.IOcrService
import com.book.rabyw.domain.ITranslationService
import com.book.rabyw.domain.models.CapturedImage
import com.book.rabyw.domain.models.Language
import com.book.rabyw.domain.models.RecognizedText
import com.book.rabyw.domain.models.TranslationMode
import com.book.rabyw.domain.models.TranslationResult
import com.book.rabyw.util.AppLogger
import io.ktor.util.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
val TAG="BookReaderViewModel"
data class BookReaderUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val capturedImage: CapturedImage? = null,
    val recognizedText: RecognizedText? = null,
    val translationResult: TranslationResult? = null,
    val translateAlignJsonError: String? = null,
    val sourceLanguage: Language = Language.ENGLISH,
    val targetLanguage: Language = Language.CHINESE_SIMPLIFIED,
    val translationMode: TranslationMode = TranslationMode.FAST,
    val isCameraPermissionGranted: Boolean = false,
    val isProcessingOcr: Boolean = false,
    val isProcessingTranslation: Boolean = false
)

class BookReaderViewModel(
    private val cameraService: ICameraService,
    private val ocrService: IOcrService,
    private val translationService: ITranslationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookReaderUiState())
    val uiState: StateFlow<BookReaderUiState> = _uiState.asStateFlow()

    init {
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        viewModelScope.launch {
            val hasPermission = cameraService.hasCameraPermission()
            _uiState.value = _uiState.value.copy(isCameraPermissionGranted = hasPermission)
        }
    }

    fun requestCameraPermission() {
        viewModelScope.launch {
            val granted = cameraService.requestCameraPermission()
            _uiState.value = _uiState.value.copy(isCameraPermissionGranted = granted)
        }
    }

    fun captureImage() {
        if (!_uiState.value.isCameraPermissionGranted) {
            requestCameraPermission()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            cameraService.captureImage().collect { image ->
                if (image != null) {
                    _uiState.value = _uiState.value.copy(
                        capturedImage = image,
                        isLoading = false
                    )
                    // Automatically process OCR after image capture
                    processOcr(image)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun processOcr(image: CapturedImage? = null) {
        val imageToProcess = image ?: _uiState.value.capturedImage
        if (imageToProcess == null) return

        AppLogger.i(TAG,"processOcr: $imageToProcess")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingOcr = true, error = null)
            
            ocrService.recognizeText(imageToProcess)
                .onSuccess { recognizedText ->
                    _uiState.value = _uiState.value.copy(
                        recognizedText = recognizedText,
                        isProcessingOcr = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "OCR failed: ${error.message}",
                        isProcessingOcr = false
                    )
                }
        }
    }

    fun translateText() {
        val recognizedText = _uiState.value.recognizedText ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingTranslation = true, error = null)
            
            // Ensure language models are downloaded for on-device translation
            if (_uiState.value.translationMode == TranslationMode.FAST) {
                val downloadResult = translationService.downloadLanguageModels(
                    _uiState.value.sourceLanguage,
                    _uiState.value.targetLanguage
                )
                
                if (downloadResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to download language models: ${downloadResult.exceptionOrNull()?.message}",
                        isProcessingTranslation = false
                    )
                    return@launch
                }
            }
            
            translationService.translateText(
                recognizedText.fullText,
                _uiState.value.sourceLanguage,
                _uiState.value.targetLanguage,
                _uiState.value.translationMode
            )
                .onSuccess { translationResult ->
                    _uiState.value = _uiState.value.copy(
                        translationResult = translationResult,
                        isProcessingTranslation = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Translation failed: ${error.message}",
                        isProcessingTranslation = false
                    )
                }
        }
    }

    fun setSourceLanguage(language: Language) {
        _uiState.value = _uiState.value.copy(sourceLanguage = language)
    }

    fun setTargetLanguage(language: Language) {
        _uiState.value = _uiState.value.copy(targetLanguage = language)
    }

    fun setTranslationMode(mode: TranslationMode) {
        _uiState.value = _uiState.value.copy(translationMode = mode)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            capturedImage = null,
            recognizedText = null,
            translationResult = null,
            error = null
        )
    }
}
