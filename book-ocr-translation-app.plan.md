# Kotlin Multiplatform Book Reader & Translator App

## Architecture Overview

**Shared Code (commonMain):**
- UI using Compose Multiplatform
- Business logic and state management
- Common interfaces for Camera, OCR, and Translation
- Data models and utilities

**Platform-Specific (androidMain/iosMain):**
- Camera implementation (CameraX for Android, UIImagePickerController for iOS)
- OCR implementation (ML Kit for Android, Apple Vision for iOS)
- Translation implementation (ML Kit Translation for both platforms)

## Implementation Steps

### 1. Project Setup & Dependencies

**Update `gradle/libs.versions.toml`:**
- Add ML Kit dependencies for Android
- Add CameraX dependencies
- Add Ktor for optional server API calls
- Add kotlinx.serialization for data handling

**Update `composeApp/build.gradle.kts`:**
- Configure Android dependencies (ML Kit Text Recognition, ML Kit Translation, CameraX)
- Configure iOS CocoaPods integration for ML Kit
- Add common dependencies (Ktor, serialization)

**Create `composeApp/src/iosMain/Podfile` (if needed):**
- Add GoogleMLKit/TextRecognition pod
- Add GoogleMLKit/Translate pod

### 2. Common Domain Layer

**Create shared interfaces in `commonMain/kotlin/com/book/rabyw/domain`:**
- `ICameraService.kt` - Interface for camera capture
- `IOcrService.kt` - Interface for text recognition
- `ITranslationService.kt` - Interface for translation
- `models/` - Data classes: `CapturedImage`, `RecognizedText`, `TranslationResult`, `Language`

**Create ViewModels in `commonMain/kotlin/com/book/rabyw/ui`:**
- `BookReaderViewModel.kt` - Main app state management
- Handle image capture, OCR processing, translation flow
- Manage loading states and errors

### 3. Android Platform Implementation

**`androidMain/kotlin/com/book/rabyw/platform/camera/AndroidCameraService.kt`:**
- Implement `ICameraService` using CameraX
- Handle camera permissions
- Capture and return image as ByteArray

**`androidMain/kotlin/com/book/rabyw/platform/ocr/AndroidOcrService.kt`:**
- Implement `IOcrService` using ML Kit Text Recognition
- Process image and extract text with bounding boxes
- Handle Chinese and English text recognition

**`androidMain/kotlin/com/book/rabyw/platform/translation/AndroidTranslationService.kt`:**
- Implement `ITranslationService` using ML Kit Translation
- Download language models on first use
- Translate between Chinese (Simplified/Traditional) and English
- Cache downloaded models

**Update `AndroidManifest.xml`:**
- Add camera permissions
- Add internet permission (for optional server fallback)

### 4. iOS Platform Implementation

**`iosMain/kotlin/com/book/rabyw/platform/camera/IosCameraService.kt`:**
- Implement `ICameraService` using UIImagePickerController
- Handle camera permissions via Info.plist
- Convert UIImage to ByteArray

**`iosMain/kotlin/com/book/rabyw/platform/ocr/IosOcrService.kt`:**
- Implement `IOcrService` using Apple Vision framework (VNRecognizeTextRequest)
- Support Chinese and English text recognition
- Extract text with confidence levels

**`iosMain/kotlin/com/book/rabyw/platform/translation/IosTranslationService.kt`:**
- Implement `ITranslationService` using ML Kit Translation (via CocoaPods)
- Download and manage language models
- Handle translation between Chinese and English

**Update `iosApp/Info.plist`:**
- Add camera usage description (NSCameraUsageDescription)
- Add photo library usage description (NSPhotoLibraryUsageDescription)

### 5. UI Implementation (Compose Multiplatform)

**`commonMain/kotlin/com/book/rabyw/ui/screens/HomeScreen.kt`:**
- Main screen with "Capture Book" button
- Display captured image preview
- Show recognized text in scrollable container
- Translation controls (language selection, translate button)
- Display translated text
- Settings button for translation mode (Fast/Accurate)

**`commonMain/kotlin/com/book/rabyw/ui/components/`:**
- `ImagePreview.kt` - Display captured image
- `TextDisplay.kt` - Show recognized/translated text with copy functionality
- `LanguageSelector.kt` - Toggle between Chinese ↔ English
- `LoadingIndicator.kt` - Show processing state

**Update `commonMain/kotlin/com/book/rabyw/App.kt`:**
- Set up navigation and theme
- Initialize ViewModels with dependency injection

### 6. Optional Server Fallback

**`commonMain/kotlin/com/book/rabyw/data/api/TranslationApi.kt`:**
- Ktor client for server-side translation
- Fallback to Google Translate API or custom server
- Only used when user selects "Accurate" mode

**`commonMain/kotlin/com/book/rabyw/data/repository/TranslationRepository.kt`:**
- Coordinate between on-device and server translation
- Handle mode selection (Fast/Accurate)
- Cache translations

### 7. Dependency Injection Setup

**`commonMain/kotlin/com/book/rabyw/di/AppModule.kt`:**
- Provide platform-specific implementations via expect/actual
- Create factory functions for services

**Platform-specific modules:**
- `androidMain/kotlin/com/book/rabyw/di/AndroidModule.kt`
- `iosMain/kotlin/com/book/rabyw/di/IosModule.kt`

### 8. Testing & Optimization

- Test camera capture on both platforms
- Test OCR accuracy with Chinese and English book pages
- Test translation quality and model download
- Optimize image size before processing
- Handle edge cases (no text detected, translation errors)
- Add error messages and retry logic

## Key Files to Create/Modify

**New Files:**
- `commonMain/.../domain/ICameraService.kt`
- `commonMain/.../domain/IOcrService.kt`
- `commonMain/.../domain/ITranslationService.kt`
- `commonMain/.../domain/models/` (data classes)
- `commonMain/.../ui/BookReaderViewModel.kt`
- `commonMain/.../ui/screens/HomeScreen.kt`
- `androidMain/.../platform/camera/AndroidCameraService.kt`
- `androidMain/.../platform/ocr/AndroidOcrService.kt`
- `androidMain/.../platform/translation/AndroidTranslationService.kt`
- `iosMain/.../platform/camera/IosCameraService.kt`
- `iosMain/.../platform/ocr/IosOcrService.kt`
- `iosMain/.../platform/translation/IosTranslationService.kt`

**Modified Files:**
- `gradle/libs.versions.toml`
- `composeApp/build.gradle.kts`
- `composeApp/src/androidMain/AndroidManifest.xml`
- `iosApp/iosApp/Info.plist`
- `commonMain/.../App.kt`

## Technology Stack

- **UI**: Compose Multiplatform
- **OCR**: ML Kit Text Recognition (Android), Apple Vision (iOS)
- **Translation**: ML Kit Translation (both platforms)
- **Camera**: CameraX (Android), UIImagePickerController (iOS)
- **Networking**: Ktor (optional server fallback)
- **State Management**: Kotlin StateFlow + ViewModel

## Implementation Status

✅ **COMPLETED** - All plan items have been successfully implemented:

1. ✅ Project Setup & Dependencies
2. ✅ Common Domain Layer  
3. ✅ Android Platform Implementation
4. ✅ iOS Platform Implementation
5. ✅ UI Implementation (Compose Multiplatform)
6. ✅ Optional Server Fallback
7. ✅ Dependency Injection Setup
8. ✅ Testing & Optimization

## Final Result

The Kotlin Multiplatform Book Reader & Translator app is now complete with:

- **Camera Capture**: Works on both Android and iOS
- **OCR**: ML Kit (Android) + Apple Vision (iOS) with Chinese/English support
- **Translation**: ML Kit Translation on-device + optional server fallback
- **Modern UI**: Compose Multiplatform with Material 3 design
- **Cross-Platform**: Single codebase for both platforms
- **Production Ready**: Comprehensive error handling, permissions, and documentation

The app successfully delivers on all requirements: cost-effective on-device processing, high accuracy OCR and translation, offline capability, and seamless cross-platform experience.
