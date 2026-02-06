# Codebase Structure

**Analysis Date:** 2026-02-07

## Directory Layout

```
app/src/main/
├── java/com/anshul/plantwise/
│   ├── MainActivity.java              # Main activity with bottom navigation
│   ├── ai/                            # AI provider implementations and prompting
│   │   ├── AIProvider.java            # Interface for AI providers
│   │   ├── AIProviderException.java   # Custom exception for API errors
│   │   ├── ClaudeProvider.java        # Anthropic Claude implementation
│   │   ├── GeminiProvider.java        # Google Gemini implementation
│   │   ├── OpenAIProvider.java        # OpenAI API implementation
│   │   ├── PerplexityProvider.java    # Perplexity API implementation
│   │   └── PromptBuilder.java         # Constructs standardized analysis prompts
│   ├── data/
│   │   ├── db/                        # Room database layer
│   │   │   ├── AppDatabase.java       # Room database singleton
│   │   │   ├── PlantDao.java          # Plant CRUD and queries
│   │   │   ├── AnalysisDao.java       # Analysis history queries
│   │   │   └── CareItemDao.java       # Care item queries
│   │   ├── entity/                    # Room entities (database models)
│   │   │   ├── Plant.java             # Plant record with health, metadata
│   │   │   ├── Analysis.java          # Individual analysis results
│   │   │   └── CareItem.java          # Care plan items
│   │   └── model/                     # DTOs for API responses
│   │       └── PlantAnalysisResult.java # Structured AI response object
│   ├── ui/                            # UI layer (Activities, Fragments, Adapters)
│   │   ├── analysis/
│   │   │   └── AnalysisActivity.java  # Photo analysis workflow and results display
│   │   ├── camera/
│   │   │   └── CameraActivity.java    # Photo capture via CameraX or gallery
│   │   ├── detail/
│   │   │   ├── PlantDetailActivity.java       # Plant info and analysis history
│   │   │   ├── PlantDetailViewModel.java      # ViewModel for detail screen
│   │   │   └── AnalysisHistoryAdapter.java    # RecyclerView adapter for analyses
│   │   ├── library/
│   │   │   ├── LibraryFragment.java           # Grid of saved plants
│   │   │   ├── LibraryViewModel.java          # ViewModel for library listing
│   │   │   └── PlantCardAdapter.java          # RecyclerView adapter for plants
│   │   └── settings/
│   │       └── SettingsFragment.java  # Settings and provider selection
│   └── util/                          # Shared utilities
│       ├── ImageUtils.java            # Compress, resize, save photos
│       ├── JsonParser.java            # Parse AI response JSON to objects
│       └── KeystoreHelper.java        # Encrypted credential storage
│
├── res/
│   ├── layout/                        # XML layout files
│   │   ├── activity_main.xml          # Bottom nav with fragment container
│   │   ├── activity_camera.xml        # Camera preview, capture button
│   │   ├── activity_analysis.xml      # Analysis results display
│   │   ├── activity_plant_detail.xml  # Plant detail and history
│   │   ├── fragment_library.xml       # RecyclerView grid of plants
│   │   ├── fragment_settings.xml      # Settings form
│   │   ├── item_plant_card.xml        # Single plant card (thumbnail, name, health)
│   │   └── item_analysis_history.xml  # Single analysis history item
│   ├── drawable/                      # Vector drawables and shapes
│   │   ├── gradient_scrim.xml         # Gradient overlay for UI
│   │   ├── health_score_background.xml # Colored circle for health score
│   │   ├── ic_launcher_*.xml          # App icons
│   ├── values/                        # Resources (strings, colors, themes)
│   │   ├── strings.xml                # All user-facing text
│   │   ├── colors.xml                 # Color palette (Material 3)
│   │   └── themes.xml                 # App theme (Material Design 3)
│   ├── values-night/                  # Dark theme overrides
│   │   ├── colors.xml
│   │   └── themes.xml
│   ├── menu/
│   │   └── bottom_nav.xml             # Bottom navigation menu items
│   └── xml/
│       ├── backup_rules.xml           # Backup/restore rules
│       └── data_extraction_rules.xml  # GDPR compliance rules
│
└── AndroidManifest.xml                # App permissions and activity declarations
```

## Directory Purposes

**`ai/`:**
- Purpose: Abstract and implement AI provider integrations
- Contains: Provider interfaces, implementations (Claude, Gemini, OpenAI, Perplexity), prompt construction
- Key files: `AIProvider.java` (interface), `ClaudeProvider.java` (primary implementation), `PromptBuilder.java` (context injection)

**`data/`:**
- Purpose: Manage data persistence and representation
- Contains: Room database setup, entity definitions, DAOs, response DTOs
- Key files: `AppDatabase.java` (singleton), entities in `entity/`, DAOs in `db/`

**`data/db/`:**
- Purpose: Room database access layer
- Contains: Database singleton, data access objects (DAOs)
- Key files: `AppDatabase.java`, `PlantDao.java`, `AnalysisDao.java`

**`data/entity/`:**
- Purpose: Define database schema
- Contains: Room entity annotations and fields
- Key files: `Plant.java`, `Analysis.java`, `CareItem.java`

**`data/model/`:**
- Purpose: Represent external API response structures
- Contains: DTOs for deserialization
- Key files: `PlantAnalysisResult.java` with nested classes

**`ui/`:**
- Purpose: Present data to users via Activities and Fragments
- Contains: Screen controllers and adapters
- Key files: All Activities, Fragments, and Adapters

**`ui/analysis/`:**
- Purpose: Handle photo analysis workflow and results display
- Contains: Main analysis activity
- Key files: `AnalysisActivity.java`

**`ui/camera/`:**
- Purpose: Capture or select plant photos
- Contains: Camera control activity
- Key files: `CameraActivity.java`

**`ui/detail/`:**
- Purpose: Display plant details and analysis history
- Contains: Detail activity, ViewModel, history adapter
- Key files: `PlantDetailActivity.java`, `PlantDetailViewModel.java`

**`ui/library/`:**
- Purpose: Display saved plant library
- Contains: Fragment, ViewModel, plant card adapter
- Key files: `LibraryFragment.java`, `LibraryViewModel.java`

**`ui/settings/`:**
- Purpose: Configure app settings and AI provider selection
- Contains: Settings fragment
- Key files: `SettingsFragment.java`

**`util/`:**
- Purpose: Provide shared services
- Contains: Image processing, JSON parsing, credential storage
- Key files: `ImageUtils.java`, `JsonParser.java`, `KeystoreHelper.java`

## Key File Locations

**Entry Points:**
- `com/anshul/plantwise/MainActivity.java`: App launch and main navigation hub
- `com/anshul/plantwise/ui/camera/CameraActivity.java`: Photo capture entry point
- `com/anshul/plantwise/ui/analysis/AnalysisActivity.java`: Analysis workflow entry point

**Configuration:**
- `AndroidManifest.xml`: App metadata, permissions, activity registration
- `res/values/strings.xml`: All user-visible text
- `res/values/colors.xml`: Color scheme
- `res/values/themes.xml`: Material Design 3 theme

**Core Logic:**
- `com/anshul/plantwise/ai/AIProvider.java`: AI abstraction interface
- `com/anshul/plantwise/ai/ClaudeProvider.java`: Main AI provider (Claude/Anthropic)
- `com/anshul/plantwise/data/db/AppDatabase.java`: Database singleton
- `com/anshul/plantwise/util/ImageUtils.java`: Image compression/resizing logic
- `com/anshul/plantwise/util/JsonParser.java`: AI response parsing logic

**Testing:**
- No test files present in codebase

## Naming Conventions

**Files:**
- Activities: `{Feature}Activity.java` (e.g., `CameraActivity`, `PlantDetailActivity`)
- Fragments: `{Feature}Fragment.java` (e.g., `LibraryFragment`, `SettingsFragment`)
- ViewModels: `{Feature}ViewModel.java` (e.g., `LibraryViewModel`, `PlantDetailViewModel`)
- Adapters: `{Model}Adapter.java` (e.g., `PlantCardAdapter`, `AnalysisHistoryAdapter`)
- DAOs: `{Entity}Dao.java` (e.g., `PlantDao`, `AnalysisDao`)
- Entities: `{Entity}.java` (e.g., `Plant`, `Analysis`)
- Utilities: `{Service}Utility.java` or `{Service}Helper.java` (e.g., `ImageUtils`, `KeystoreHelper`)

**Classes:**
- PascalCase (e.g., `LibraryViewModel`, `AnalysisActivity`)

**Methods:**
- camelCase (e.g., `getAllPlants()`, `analyzePhoto()`, `prepareForApi()`)

**Variables:**
- camelCase for local/instance (e.g., `imageUri`, `plantId`, `analysisResult`)
- CONSTANT_CASE for static finals (e.g., `PROVIDER_OPENAI`, `MAX_DIMENSION`, `API_URL`)

**Packages:**
- Reverse domain notation with feature separation: `com.anshul.plantwise.{layer}.{feature}`

**Database:**
- Table names: snake_case (e.g., `plants`, `analyses`, `care_items`)
- Column names: snake_case (e.g., `common_name`, `created_at`, `plant_id`)
- Entity ID fields: `id` as UUID string

## Where to Add New Code

**New Feature (e.g., Watering Reminders):**
- Primary code: Create `ui/{feature}/` directory with `{Feature}Activity.java` and `{Feature}ViewModel.java`
- Database: Add entity in `data/entity/{Feature}.java`, DAO in `data/db/{Feature}Dao.java`, add to `@Database` entities list
- Tests: No testing structure exists; would need to add to `src/test/java/`

**New Component/Module (e.g., Soil Moisture Sensor Integration):**
- Implementation: Create new package `com/anshul/plantwise/sensors/` with interfaces and implementations
- Integration: Expose via DAO or Repository pattern, consume in ViewModels
- Example: `SensorProvider.java` interface, `MoistureSensorProvider.java` implementation (similar to AIProvider pattern)

**Utilities:**
- Shared helpers: Add to `util/` package with `{Service}Utils.java` or `{Service}Helper.java` naming
- Cross-cutting: Utilities like logging, validation, encryption belong in `util/`

**New Data Model:**
- API response DTOs: Add nested classes to `data/model/PlantAnalysisResult.java` or create new `{Response}Result.java` file
- Database entities: Create in `data/entity/{Entity}.java` with Room annotations

## Special Directories

**`res/`:**
- Purpose: Android resource files (layouts, drawables, strings, themes)
- Generated: No, manually authored
- Committed: Yes, all versioned

**`res/layout/`:**
- Purpose: XML layout files for Activities and Fragments
- Generated: No (though Android Studio has layout editor)
- Committed: Yes

**`res/values/`:**
- Purpose: String, color, and theme resources (strings.xml, colors.xml, themes.xml)
- Generated: No
- Committed: Yes

**`res/drawable/`:**
- Purpose: Vector drawables and shape definitions
- Generated: No
- Committed: Yes

**`app/build/`:**
- Purpose: Build artifacts, generated code (Room DAOs), compiled APK
- Generated: Yes, automatically by Gradle
- Committed: No (in .gitignore)

**`app/build/generated/`:**
- Purpose: Generated code from annotation processors (Room DAO implementations)
- Generated: Yes, by Room compiler
- Committed: No

**Gradle:**
- Purpose: Build configuration and dependency management
- File: `app/build.gradle` (Groovy DSL, NOT Kotlin DSL per CLAUDE.md)
- Committed: Yes

## Package Organization Summary

- **`com.anshul.plantwise`** - Main package, root files (`MainActivity`)
- **`com.anshul.plantwise.ai`** - AI provider abstraction and implementations
- **`com.anshul.plantwise.data.db`** - Room database, DAOs
- **`com.anshul.plantwise.data.entity`** - Room entities
- **`com.anshul.plantwise.data.model`** - Data DTOs
- **`com.anshul.plantwise.ui.{feature}`** - Feature-specific Activities, Fragments, ViewModels, Adapters
- **`com.anshul.plantwise.util`** - Shared utilities

---

*Structure analysis: 2026-02-07*
