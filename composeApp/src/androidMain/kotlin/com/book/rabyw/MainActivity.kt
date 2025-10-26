package com.book.rabyw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LifecycleOwner
import com.book.rabyw.di.AppModule
import com.book.rabyw.platform.permissions.PermissionController

class MainActivity : ComponentActivity(), LifecycleOwner {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize permission controller early (before STARTED)
        PermissionController.init(this)

        // Initialize camera launcher early (before STARTED)
        com.book.rabyw.platform.camera.CameraLauncher.initialize(this)
        
        // Initialize image picker launcher early (before STARTED)
        com.book.rabyw.platform.camera.ImagePickerLauncher.initialize(this)

        // Initialize dependency injection
        AppModule.initialize(this, this)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}