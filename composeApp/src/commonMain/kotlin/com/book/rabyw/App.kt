package com.book.rabyw

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.book.rabyw.di.AppModule
import com.book.rabyw.ui.BookReaderViewModel
import com.book.rabyw.ui.screens.HomeScreen

@Composable
fun App() {
    MaterialTheme {
        // Initialize dependency injection
        val viewModel: BookReaderViewModel = remember {
            BookReaderViewModel(
                cameraService = AppModule.provideCameraService(),
                ocrService = AppModule.provideOcrService(),
                translationService = AppModule.provideTranslationService()
            )
        }
        
        HomeScreen(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
        )
    }
}