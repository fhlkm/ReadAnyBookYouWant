package com.book.rabyw

import androidx.compose.ui.window.ComposeUIViewController
import com.book.rabyw.di.AppModule
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val viewController = ComposeUIViewController { App() }
    
    // Initialize dependency injection
    AppModule.initialize(viewController)
    
    return viewController
}