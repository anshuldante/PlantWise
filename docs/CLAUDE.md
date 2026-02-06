# CLAUDE.md — PlantWise

## Project
PlantWise is a native Android app (Java, min SDK 26, target SDK 34) for plant identification,
health analysis, and care management using AI vision APIs.

## Stack
- Language: Java (NOT Kotlin)
- Min SDK: 26 (Android 8.0)
- Target SDK: 34
- Build: Gradle with Groovy DSL (build.gradle, NOT build.gradle.kts)
- UI: XML layouts with Material Design 3
- Architecture: MVVM (ViewModel + LiveData + Repository pattern)
- Database: Room (SQLite)
- HTTP: OkHttp 4.x for API calls
- JSON: org.json (built-in) for parsing, Gson for complex deserialization
- Image loading: Glide
- Camera: CameraX or ACTION_IMAGE_CAPTURE intent
- Encryption: AndroidX Security (EncryptedSharedPreferences)
- Notifications: AlarmManager + NotificationCompat

## Architecture Rules
1. **MVVM everywhere**: Activities/Fragments observe LiveData from ViewModels.
   ViewModels call Repositories. Repositories coordinate between AI service and Room DB.
   Never put business logic in Activities.
2. **AI Provider Abstraction**: All AI calls go through the AIProvider interface.
   Never reference ClaudeProvider directly in UI code. Use AIProviderFactory to get
   the active provider based on user settings.
3. **Local-first**: All data lives on-device. No backend server. No cloud sync.
4. **Background threading**: All Room DB operations and API calls must run off the
   main thread. Use ExecutorService or AsyncTask alternatives (java.util.concurrent).
   Never call network or DB on the main thread.
5. **Image optimization**: Always compress/resize photos before sending to AI API.
   Target ~1MB max, 1024px longest edge. Use Bitmap.compress() with JPEG quality 80.

## Key Dependencies (add to app/build.gradle)
```groovy
dependencies {
    // AndroidX + Material
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'

    // Room
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'

    // Network
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'

    // Image loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'

    // CameraX
    implementation 'androidx.camera:camera-core:1.3.1'
    implementation 'androidx.camera:camera-camera2:1.3.1'
    implementation 'androidx.camera:camera-lifecycle:1.3.1'
    implementation 'androidx.camera:camera-view:1.3.1'

    // Security
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
}
```

## Android Manifest Permissions
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

## API Integration
The Anthropic Messages API expects:
- POST to https://api.anthropic.com/v1/messages
- Headers: x-api-key, anthropic-version: 2023-06-01, content-type: application/json
- Body: model, max_tokens, messages array with content blocks
- Image: sent as base64 content block with type "image", source.type "base64",
  source.media_type "image/jpeg", source.data "<base64string>"
- Model to use: claude-sonnet-4-20250514

The AI MUST return pure JSON (no markdown wrapping). The prompt explicitly instructs this.
Always wrap JSON parsing in try-catch. If parsing fails, show the user a friendly error
and log the raw response for debugging.

## Code Style
- Java, not Kotlin
- Standard Java naming conventions (camelCase methods, PascalCase classes)
- All fields in entities public (Room requirement for simplicity)
- Use @NonNull / @Nullable annotations
- Prefer explicit try-catch over unchecked exceptions
- Use string resources (strings.xml) for all user-facing text
- Handle all errors with user-visible feedback (Toast or Snackbar)

## Build & Run
- Open in Android Studio
- Sync Gradle
- Run on connected device or emulator (API 26+)
- For APK: Build > Build Bundle(s) / APK(s) > Build APK(s)

## Important Notes
- Photos stored in getFilesDir()/plant_photos/ — NOT external storage
- UUIDs for all entity IDs: UUID.randomUUID().toString()
- The AI response JSON may contain "true/false" as strings — handle both
- Always request runtime permissions before camera/gallery access
- Use Material 3 dynamic colors if available, fall back to custom theme
