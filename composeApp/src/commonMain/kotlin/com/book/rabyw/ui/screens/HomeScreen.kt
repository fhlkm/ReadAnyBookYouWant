package com.book.rabyw.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.book.rabyw.ui.BookReaderViewModel
import com.book.rabyw.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BookReaderViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Book Reader & Translator",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Camera Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Capture Book Page",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (!uiState.isCameraPermissionGranted) {
                        Text(
                            text = "Camera permission is required to capture book pages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Button(
                            onClick = { viewModel.requestCameraPermission() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Grant Camera Permission")
                        }
                    }
                    
                    Button(
                        onClick = { viewModel.captureImage() },
                        enabled = uiState.isCameraPermissionGranted && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Capture Image")
                    }
                }
            }
        }
        
        // Image Preview
        uiState.capturedImage?.let { image ->
            item {
                ImagePreview(
                    image = image,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // OCR Section
        if (uiState.isProcessingOcr) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Processing OCR...")
                    }
                }
            }
        }
        
        // Recognized Text
        uiState.recognizedText?.let { recognizedText ->
            item {
                TextDisplay(
                    title = "Recognized Text",
                    text = recognizedText.fullText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Translation Controls
            item {
                TranslationControls(
                    sourceLanguage = uiState.sourceLanguage,
                    targetLanguage = uiState.targetLanguage,
                    translationMode = uiState.translationMode,
                    onSourceLanguageChanged = viewModel::setSourceLanguage,
                    onTargetLanguageChanged = viewModel::setTargetLanguage,
                    onTranslationModeChanged = viewModel::setTranslationMode,
                    onTranslate = { viewModel.translateText() },
                    isTranslating = uiState.isProcessingTranslation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Translation Result
        if (uiState.isProcessingTranslation) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Translating...")
                    }
                }
            }
        }
        
        uiState.translationResult?.let { translationResult ->
            item {
                TextDisplay(
                    title = "Translation",
                    text = translationResult.translatedText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Error Display
        uiState.error?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Text("✕")
                        }
                    }
                }
            }
        }
        
        // Clear Results Button
        if (uiState.capturedImage != null || uiState.recognizedText != null || uiState.translationResult != null) {
            item {
                OutlinedButton(
                    onClick = { viewModel.clearResults() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Results")
                }
            }
        }
    }
}
