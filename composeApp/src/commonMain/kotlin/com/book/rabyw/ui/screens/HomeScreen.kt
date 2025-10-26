package com.book.rabyw.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.book.rabyw.ui.BookReaderViewModel
import com.book.rabyw.ui.components.*
import com.book.rabyw.alignment.TranslateAlignResponse
import com.book.rabyw.alignment.AlignmentPair
import com.book.rabyw.util.AppLogger

val TAG="HomeScreen"
private data class ColoredSpan(val start: Int, val end: Int, val color: Color)

private data class AlignmentDisplayRow(
    val sourceText: String,
    val targetText: String,
    val sourceSpans: List<ColoredSpan>,
    val targetSpans: List<ColoredSpan>
)

private fun palette(): List<Color> = listOf(
    Color(0xFFCCE5FF), Color(0xFFFFE5CC), Color(0xFFD5F5E3), Color(0xFFF9E79F),
    Color(0xFFE8DAEF), Color(0xFFFADBD8), Color(0xFFD6EAF8), Color(0xFFFDEBD0)
)

private fun renderRows(ta: TranslateAlignResponse, fullSource: String, fullTarget: String): List<AlignmentDisplayRow> {
    // Segment source into sentences by simple punctuation heuristics
    val sentenceRanges = run {
        val rs = mutableListOf<IntRange>()
        var start = 0
        val text = fullSource
        val delimiters = setOf('。','！','？','；','!','?',';','\n')
        for (i in text.indices) {
            val ch = text[i]
            if (delimiters.contains(ch)) {
                val end = i + 1
                if (end > start) rs.add(IntRange(start, end))
                start = end
            }
        }
        if (start < text.length) rs.add(IntRange(start, text.length))
        rs
    }

    val rows = mutableListOf<AlignmentDisplayRow>()
    val colors = palette()
    for (range in sentenceRanges) {
        val pairs = ta.alignment.filter { p ->
            val s0 = p.source.start
            val s1 = p.source.end
            s1 > range.first && s0 < range.last
        }.sortedBy { it.source.start }
        if (pairs.isEmpty()) continue
        val sourceText = fullSource.substring(range.first, range.last)
        val sourceSpans = pairs.mapIndexed { idx, p ->
            val startLocal = (p.source.start - range.first).coerceAtLeast(0)
            val endLocal = (p.source.end - range.first).coerceAtLeast(startLocal)
            ColoredSpan(startLocal, endLocal, colors[idx % colors.size])
        }
        val tgtMin = pairs.minOf { it.target.start }
        val tgtMax = pairs.maxOf { it.target.end }
        val targetText = if (tgtMax > tgtMin && tgtMax <= fullTarget.length && tgtMin >= 0) {
            fullTarget.substring(tgtMin, tgtMax)
        } else fullTarget
        val targetSpans = pairs.mapIndexed { idx, p ->
            val startLocal = (p.target.start - tgtMin).coerceAtLeast(0)
            val endLocal = (p.target.end - tgtMin).coerceAtLeast(startLocal)
            ColoredSpan(startLocal, endLocal, colors[idx % colors.size])
        }
        rows.add(AlignmentDisplayRow(sourceText, targetText, sourceSpans, targetSpans))
    }
    if (rows.isEmpty()) {
        // Fallback single row: highlight nothing
        rows.add(AlignmentDisplayRow(fullSource, fullTarget, emptyList(), emptyList()))
    }
    return rows
}

private fun buildColoredText(text: String, spans: List<ColoredSpan>): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    spans.forEach { s ->
        val safeStart = s.start.coerceIn(0, text.length)
        val safeEnd = s.end.coerceIn(0, text.length)
        if (safeEnd > safeStart) {
            builder.addStyle(SpanStyle(background = s.color.copy(alpha = 0.35f)), safeStart, safeEnd)
        }
    }
    return builder.toAnnotatedString()
}

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
            val ta = translationResult.translateAlign
            if (ta == null) {
                item {
                    TextDisplay(
                        title = "Translation",
                        text = translationResult.translatedText,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    Text(
                        text = "Translation + Alignment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(ta.alignment) { row ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppLogger.i(TAG,"******************************")

                            Text(row.source.text +"\n"+ row.target.text)

                        }
                    }
                }
//                items(renderRows(ta, uiState.recognizedText?.fullText ?: "", translationResult.translatedText)) { row ->
//                    Card(
//                        modifier = Modifier.fillMaxWidth(),
//                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                    ) {
//                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
//                            AppLogger.i(TAG,"******************************")
//                            AppLogger.i(TAG,"sourceText: ${row.sourceText}")
//                            AppLogger.i(TAG,"targetText: ${row.targetText}")
//                            Text(buildColoredText(row.sourceText, row.sourceSpans))
//                            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
//                            Text(buildColoredText(row.targetText, row.targetSpans))
//                        }
//                    }
//                }
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
