# Technology Stack

**Analysis Date:** 2026-02-07

## Languages

**Primary:**
- Java 11 - Native Android app code (not Kotlin)

**Secondary:**
- XML - Android resource layouts, manifests, strings
- Groovy - Build configuration (build.gradle)

## Runtime

**Environment:**
- Android SDK (compileSdk 35, targetSdk 35)
- Minimum SDK 26 (Android 8.0)

**Platform:**
- Android OS with native Java runtime

## Frameworks

**Core UI & Lifecycle:**
- AndroidX AppCompat 1.6.1 - Backward compatibility and Material Design 3
- AndroidX Material 1.11.0 - Material Design 3 components
- AndroidX Activity 1.8.2 - Activity lifecycle management
- AndroidX Fragment 1.6.2 - Fragment support library
- AndroidX ConstraintLayout 2.1.4 - Layout constraints for UI

**Architecture:**
- AndroidX Lifecycle (ViewModel 2.7.0, LiveData 2.7.0) - MVVM pattern
- AndroidX Navigation 2.7.7 - Fragment navigation and deep linking

**Database:**
- AndroidX Room 2.6.1 - SQLite ORM with compile-time entity checking
- SQLite - Underlying database engine

**Camera & Imaging:**
- AndroidX CameraX 1.3.1 - Modern camera API (core, camera2, lifecycle, view)
- Glide 4.16.0 - Image loading and caching library

**Security:**
- AndroidX Security (Crypto) 1.1.0-alpha06 - EncryptedSharedPreferences with AES-256

**Testing:**
- JUnit 4.13.2 - Unit test framework
- Mockito 5.11.0 - Mocking framework for unit tests
- Google Truth 1.4.2 - Assertion library for fluent test assertions
- AndroidX Arch Core Testing 2.2.0 - LiveData and ViewModel testing
- MockWebServer 4.12.0 - Mock HTTP server for testing API calls
- AndroidX Test Runner 1.5.2 - Instrumented test runner
- AndroidX Test Rules 1.5.0 - Test rule annotations
- Espresso 3.5.1 - UI testing framework with contrib and intents support

## Key Dependencies

**HTTP & Networking:**
- OkHttp3 4.12.0 - HTTP client with interceptors, timeouts (30s connect, 60s read)
- org.json 20231013 - JSON parsing (JVM compatibility for tests)

**Build System:**
- Android Gradle Plugin 9.0.0 - Gradle build system for Android
- Gradle 8.0+ - Build automation

## Configuration

**Environment:**
- No external .env files detected - API keys stored in EncryptedSharedPreferences at runtime
- User-provided configuration via Settings UI for API provider selection and API key

**Build:**
- `build.gradle` - Gradle build configuration (Groovy DSL, not Kotlin DSL)
- `gradle/libs.versions.toml` - Centralized dependency version management
- `gradle.properties` - Gradle project properties
- `local.properties` - Local SDK path (auto-generated, not version controlled)

**ProGuard/R8:**
- Minification disabled in debug builds
- ProGuard rules available in `proguard-rules.pro` for release builds

## Platform Requirements

**Development:**
- Android Studio (AGP 9.0.0 compatible)
- Java 11 (JDK 11+)
- Android SDK API 35

**Target Devices:**
- Android 8.0 (API 26) and higher
- Devices with camera support (optional but recommended)

**Production:**
- Google Play Store distribution
- APK/Bundle generation via Android Studio

---

*Stack analysis: 2026-02-07*
