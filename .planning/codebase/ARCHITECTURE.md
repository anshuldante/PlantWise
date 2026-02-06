# Architecture

**Analysis Date:** 2026-02-07

## Pattern Overview

**Overall:** MVVM (Model-View-ViewModel) with Repository pattern and abstraction layer for AI services

**Key Characteristics:**
- Activity/Fragment views are thin, observing LiveData from ViewModels
- ViewModels access data through Room DAOs and AIProvider abstraction
- Business logic isolated in service layers, not UI code
- All database operations run off the main thread via Room's async queries and ExecutorService
- AI provider selection abstracted through interface and factory pattern
- Local-first architecture with no backend/cloud sync
- Image optimization happens before all AI API calls

## Layers

**Presentation Layer (UI):**
- Purpose: Handle user interactions and display results via Material Design 3
- Location: `com/anshul/plantwise/ui/` containing Activities and Fragments
- Contains: Activities (`MainActivity`, `CameraActivity`, `AnalysisActivity`, `PlantDetailActivity`), Fragments (`LibraryFragment`, `SettingsFragment`), Adapters (`PlantCardAdapter`, `AnalysisHistoryAdapter`)
- Depends on: ViewModels, Intents for navigation
- Used by: Android framework

**ViewModel Layer:**
- Purpose: Manage UI state and coordinate data access for screens
- Location: `com/anshul/plantwise/ui/{feature}/*.java` (ViewModel classes)
- Contains: `LibraryViewModel`, `PlantDetailViewModel` extending AndroidViewModel
- Depends on: Database (via AppDatabase), LiveData for state emissions
- Used by: Activities and Fragments

**AI Service Layer:**
- Purpose: Abstract AI provider implementations and handle API communication
- Location: `com/anshul/plantwise/ai/`
- Contains: `AIProvider` interface, implementations (`ClaudeProvider`, `GeminiProvider`, `OpenAIProvider`, `PerplexityProvider`), `AIProviderException`, `PromptBuilder`
- Depends on: OkHttp for HTTP, JSON parsing, image processing
- Used by: `AnalysisActivity` (via provider selection), `PromptBuilder` for standardized prompts

**Data Layer:**
- Purpose: Manage all data persistence and retrieval
- Location: `com/anshul/plantwise/data/`
- Contains: Room entities (`Plant`, `Analysis`, `CareItem`), DAOs (`PlantDao`, `AnalysisDao`, `CareItemDao`), `AppDatabase`
- Depends on: SQLite (via Room)
- Used by: ViewModels

**Utility Layer:**
- Purpose: Provide cross-cutting services for image processing, JSON parsing, and secure credential storage
- Location: `com/anshul/plantwise/util/`
- Contains: `ImageUtils` (compression/resizing), `JsonParser` (AI response parsing), `KeystoreHelper` (encrypted preferences)
- Depends on: Android APIs, org.json, androidx.security
- Used by: `AnalysisActivity`, AI providers, overall app

## Data Flow

**Plant Analysis Flow (Camera → Analysis → Save):**

1. User launches `CameraActivity` from `MainActivity` bottom nav
2. `CameraActivity` captures image via CameraX or opens gallery, passes Uri to `AnalysisActivity`
3. `AnalysisActivity` receives Uri, shows loading state
4. Background executor thread calls `ImageUtils.prepareForApi()` to compress image to ~1MB, 1024px max
5. `AnalysisActivity` retrieves existing plant data (if known) and previous analyses from Room
6. `PromptBuilder.buildAnalysisPrompt()` constructs prompt with context (plant name, history, location)
7. `AnalysisActivity` creates appropriate AIProvider (Claude/Gemini/OpenAI/Perplexity) from settings
8. Provider calls remote API with base64 image + prompt, receives JSON response
9. `JsonParser.parsePlantAnalysis()` deserializes JSON into `PlantAnalysisResult` model
10. UI thread renders results in `AnalysisActivity` via `displayResults()`
11. User clicks "Save to Library" → `AnalysisActivity.savePlant()`
12. New/updated `Plant` entity inserted to Room via `PlantDao.insertPlant()`
13. `Analysis` record created with health score, summary, photo path via `AnalysisDao.insertAnalysis()`
14. Photos saved to device storage via `ImageUtils.savePhoto()` and `ImageUtils.saveThumbnail()`
15. `LibraryFragment` observes `PlantDao.getAllPlants()` LiveData, updates grid display

**Plant Library View Flow:**

1. `MainActivity` loads `LibraryFragment` by default
2. `LibraryFragment` instantiates `LibraryViewModel` via ViewModelProvider
3. ViewModel observes `PlantDao.getAllPlants()` (LiveData<List<Plant>>)
4. Room returns plant list ordered by `updated_at DESC`
5. Fragment sets up RecyclerView with `PlantCardAdapter`
6. Adapter renders plant cards with thumbnails (via Glide) and basic info
7. User clicks plant → navigates to `PlantDetailActivity` with plant ID
8. `PlantDetailActivity` uses `PlantDetailViewModel` to fetch plant and analysis history
9. Detail view shows plant info, latest health score, analysis history timeline

**API Key Management Flow:**

1. `AnalysisActivity` checks if API key exists via `KeystoreHelper.hasApiKey()`
2. If missing, shows `AlertDialog` prompting user to enter key
3. Entered key saved via `KeystoreHelper.saveApiKey()` to EncryptedSharedPreferences
4. User can change provider in `SettingsFragment`, saved via `KeystoreHelper.saveProvider()`
5. On analysis, `AnalysisActivity.createProvider()` reads provider and key from helper

**State Management:**

- UI state managed via LiveData in ViewModels (`LibraryViewModel.allPlants`, `PlantDetailViewModel` queries)
- Database state persists in Room SQLite (offline-first)
- API keys/provider choice encrypted in SharedPreferences (EncryptedSharedPreferences)
- Temporary image files stored in app cache directory, cleanup on Activity destroy
- Analysis photos stored permanently in `getFilesDir()/plant_photos/`

## Key Abstractions

**AIProvider Interface:**
- Purpose: Abstract different AI API implementations behind single interface
- Examples: `com/anshul/plantwise/ai/ClaudeProvider.java`, `com/anshul/plantwise/ai/GeminiProvider.java`
- Pattern: Strategy pattern — each provider implements `analyzePhoto(String, String)`, `isConfigured()`, `getDisplayName()`
- Selection logic in `AnalysisActivity.createProvider()` reads user preference from `KeystoreHelper`

**PlantAnalysisResult Data Model:**
- Purpose: Represent structured AI response with nested DTOs for different aspects
- Examples: `com/anshul/plantwise/data/model/PlantAnalysisResult.java` containing `Identification`, `HealthAssessment`, `ImmediateAction`, `CarePlan` nested classes
- Pattern: Jackson/Gson-style POJOs with public fields for Room/JSON deserialization

**Room Entity Hierarchy:**
- Purpose: Define database schema and relationships
- Examples: `Plant.java` (root), `Analysis.java` (child with foreign key to Plant), `CareItem.java`
- Pattern: JPA-style annotations (`@Entity`, `@PrimaryKey`, `@ForeignKey`)

**PromptBuilder:**
- Purpose: Construct standardized AI analysis prompt with context injection
- Examples: `com/anshul/plantwise/ai/PromptBuilder.java`
- Pattern: Builder-style string construction with conditional context (known plant name, previous analyses)

## Entry Points

**MainActivity:**
- Location: `com/anshul/plantwise/MainActivity.java`
- Triggers: App launch (LAUNCHER intent filter)
- Responsibilities: Host bottom navigation (Library/Camera/Settings), manage Fragment lifecycle, route navigation

**CameraActivity:**
- Location: `com/anshul/plantwise/ui/camera/CameraActivity.java`
- Triggers: Bottom nav Camera tab click
- Responsibilities: Initialize CameraX preview, capture photo, handle gallery picker, request camera permissions, pass image Uri to AnalysisActivity

**AnalysisActivity:**
- Location: `com/anshul/plantwise/ui/analysis/AnalysisActivity.java`
- Triggers: CameraActivity passes image Uri, can be re-opened from existing plant detail
- Responsibilities: Manage AI analysis workflow, prompt API key entry, handle provider selection, display/format results, save plant to library

**LibraryFragment:**
- Location: `com/anshul/plantwise/ui/library/LibraryFragment.java`
- Triggers: Bottom nav Library tab click, app default view
- Responsibilities: Display grid of saved plants, handle plant click navigation, observe ViewModel changes

## Error Handling

**Strategy:** Explicit try-catch with user-visible feedback via Toast/Snackbar and error containers

**Patterns:**

- **Network errors:** `AnalysisActivity` catches `AIProviderException`, displays via `showError()` with user-friendly message, shows retry button
- **Image processing:** `ImageUtils` throws `IOException` on decode/resize failure; `AnalysisActivity` catches and shows "Failed to process image" message
- **JSON parsing:** `ClaudeProvider` defensive parsing strips markdown backticks, extracts JSON between first `{` and last `}`; `JsonParser` uses `optString()`, `optInt()`, `optBoolean()` for safe default handling
- **Database:** Room async queries handle threading; DAOs accessed only on background executors in `AnalysisActivity` and ViewModels
- **Permission denial:** `CameraActivity` catches permission result, shows toast and finishes Activity if denied

## Cross-Cutting Concerns

**Logging:** Debug logging via `Log.d()`, `Log.e()` in `CameraActivity`, `AnalysisActivity`, image utility methods. Error cases logged before user-facing messages.

**Validation:**
- API key presence checked via `KeystoreHelper.hasApiKey()`
- Image URI validation before opening streams
- JSON response parsing wraps in try-catch, logs raw response for debugging
- Plant entity requires non-null `id` field

**Authentication:**
- API keys stored encrypted in EncryptedSharedPreferences via `KeystoreHelper`
- Each AI provider receives API key injected in constructor
- Keys never logged; only presence checked

**Threading:**
- Room queries return `LiveData<T>` for async delivery on main thread
- Sync queries (`getPlantByIdSync()`) called only from background ExecutorService threads
- Image operations run on background executors
- API calls run on background executors
- UI updates marshalled back to main thread via `runOnUiThread()`

---

*Architecture analysis: 2026-02-07*
