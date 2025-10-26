# Book Reader & Translator App

A Kotlin Multiplatform mobile application that captures book pages, extracts text using OCR, and translates between Chinese and English using on-device ML models.

## Features

- 📸 **Camera Capture**: Take photos of book pages using device camera
- 🔍 **Text Recognition**: Extract text from images using on-device OCR
  - Android: ML Kit Text Recognition (supports Chinese and English)
  - iOS: Apple Vision framework (supports Chinese and English)
- 🌐 **Translation**: Translate text between Chinese and English
  - On-device: ML Kit Translation (both platforms)
  - Server-side: OpenAI GPT API for high-accuracy translation
- 🎨 **Modern UI**: Built with Compose Multiplatform for consistent experience
- 📱 **Cross-Platform**: Single codebase for Android and iOS

## Architecture

### Shared Code (commonMain)
- **UI**: Compose Multiplatform screens and components
- **Domain**: Interfaces and data models for Camera, OCR, and Translation
- **ViewModel**: State management and business logic
- **Repository**: Translation service coordination

### Platform-Specific (androidMain/iosMain)
- **Camera**: CameraX (Android) / UIImagePickerController (iOS)
- **OCR**: ML Kit Text Recognition (Android) / Apple Vision (iOS)
- **Translation**: ML Kit Translation (both platforms)

## Technology Stack

- **UI Framework**: Compose Multiplatform
- **OCR**: ML Kit Text Recognition (Android), Apple Vision (iOS)
- **Translation**: ML Kit Translation (both platforms) + OpenAI GPT-5-nano API
- **Camera**: CameraX (Android), UIImagePickerController (iOS)
- **Networking**: Ktor (OpenAI API integration)
- **State Management**: Kotlin StateFlow + ViewModel

## Setup Instructions

### Prerequisites
- Android Studio or IntelliJ IDEA
- Xcode (for iOS development)
- Kotlin Multiplatform Mobile plugin
- Android SDK (API level 24+)
- iOS 15.0+ deployment target

### Building the App

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ReadAnyBookYouWant
   ```

2. **Open in Android Studio**
   - Open the project root directory
   - Sync Gradle files
   - Wait for dependencies to download

3. **Android Build**
   ```bash
   ./gradlew :composeApp:assembleDebug
   ```

4. **iOS Build**
   ```bash
   cd iosApp
   pod install
   open iosApp.xcworkspace
   ```
   Then build and run in Xcode.

### Running the App

#### Android
1. Connect Android device or start emulator
2. Run the app from Android Studio
3. Grant camera permission when prompted

#### iOS
1. Open `iosApp/iosApp.xcworkspace` in Xcode
2. Select target device or simulator
3. Build and run the project
4. Grant camera permission when prompted

## Usage

1. **Capture Image**: Tap "Capture Image" to take a photo of a book page
2. **OCR Processing**: The app automatically extracts text from the image
3. **Translation Settings**: Choose source/target languages and translation mode
4. **Translate**: Tap "Translate" to convert text between Chinese and English
5. **View Results**: See both original and translated text with copy functionality

## Translation Modes

- **Fast (Offline)**: Uses on-device ML Kit Translation
  - ✅ Free and works offline
  - ✅ Good quality for general text
  - ⚠️ Lower accuracy for complex/specialized content

- **Accurate (Online)**: Uses server-side translation with on-device fallback
  - ✅ Highest accuracy
  - ⚠️ Requires internet connection
  - ⚠️ May incur API costs

## Permissions

### Android
- `CAMERA`: Required for capturing book pages
- `INTERNET`: Required for server-side translation fallback
- `ACCESS_NETWORK_STATE`: Required for network status checking

### iOS
- `NSCameraUsageDescription`: Required for camera access
- `NSPhotoLibraryUsageDescription`: Required for photo library access

## Configuration

### OpenAI Translation API Setup

The app now uses OpenAI's GPT API for high-accuracy server-side translation. To enable this feature:

1. **Get an OpenAI API Key**
   - Visit [OpenAI Platform](https://platform.openai.com/)
   - Create an account and generate an API key
   - Ensure you have sufficient credits for API usage

2. **Configure the API Key**
   
   **Option A: Direct Configuration (Quick Setup)**
   - Open `composeApp/src/androidMain/kotlin/com/book/rabyw/di/AndroidModule.kt`
   - Replace `"YOUR_OPENAI_API_KEY_HERE"` with your actual API key
   - Open `composeApp/src/iosMain/kotlin/com/book/rabyw/di/IosModule.kt`
   - Replace `"YOUR_OPENAI_API_KEY_HERE"` with your actual API key

   **Option B: Configuration File (Recommended for Production)**
   - Copy `api-keys.properties.template` to `api-keys.properties`
   - Replace `your_openai_api_key_here` with your actual API key
   - Update the dependency injection modules to load the key from the configuration file

3. **Usage**
   - Select "Accurate" translation mode in the app
   - The app will use OpenAI GPT-5-nano (cheapest model) with automatic fallback to GPT-4o-mini
   - Translation quality is significantly higher than on-device models
   - Cost-optimized with minimal token usage and low temperature settings

### Server-Side Translation
The app supports multiple translation services. Currently implemented:
- **OpenAI GPT**: High-accuracy translation using GPT-5-nano (cheapest model) with GPT-4o-mini fallback
- **On-device ML Kit**: Fast, offline translation (fallback)

To add other translation services, update the `TranslationApi` class:

```kotlin
// Example: Google Translate API
suspend fun translateText(text: String, sourceLanguage: String, targetLanguage: String): Result<TranslationResponse> {
    // Implement your preferred translation API
    // Options: Google Translate API, Azure Translator, AWS Translate, etc.
}
```

### Language Models
ML Kit Translation automatically downloads language models on first use. Models are cached locally for offline use.

## Performance Considerations

- **Image Size**: Large images may slow down OCR processing
- **Model Downloads**: First-time translation may require downloading language models
- **Memory Usage**: OCR and translation models consume device memory
- **Battery**: On-device processing may impact battery life

## Troubleshooting

### Common Issues

1. **Camera Permission Denied**
   - Go to device settings and manually grant camera permission
   - Restart the app after granting permission

2. **OCR Not Working**
   - Ensure image has clear, readable text
   - Try capturing image in better lighting
   - Check if text is in supported languages (Chinese/English)

3. **Translation Fails**
   - Check internet connection for "Accurate" mode
   - Ensure language models are downloaded for "Fast" mode
   - Try shorter text segments for better results

4. **Build Errors**
   - Clean and rebuild project
   - Ensure all dependencies are properly synced
   - Check platform-specific requirements

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test on both Android and iOS
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Google ML Kit for on-device OCR and translation
- Apple Vision framework for iOS text recognition
- JetBrains Compose Multiplatform team
- Kotlin Multiplatform Mobile community