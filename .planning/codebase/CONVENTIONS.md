# Coding Conventions

**Analysis Date:** 2026-02-07

## Naming Patterns

**Files:**
- Activities: `{Feature}Activity.java` (e.g., `MainActivity.java`, `CameraActivity.java`, `AnalysisActivity.java`)
- Fragments: `{Feature}Fragment.java` (e.g., `LibraryFragment.java`, `SettingsFragment.java`)
- ViewModels: `{Feature}ViewModel.java` (e.g., `LibraryViewModel.java`, `PlantDetailViewModel.java`)
- Adapters: `{Feature}Adapter.java` (e.g., `PlantCardAdapter.java`, `AnalysisHistoryAdapter.java`)
- Utility classes: Simple names (e.g., `ImageUtils.java`, `JsonParser.java`, `KeystoreHelper.java`)
- Interfaces: `{Feature}Provider.java` (e.g., `AIProvider.java`)
- Data DAOs: `{Entity}Dao.java` (e.g., `PlantDao.java`, `AnalysisDao.java`)
- Entities: `{Entity}.java` (e.g., `Plant.java`, `Analysis.java`)
- Models: `{Feature}Result.java` (e.g., `PlantAnalysisResult.java`)
- Exceptions: `{Feature}Exception.java` (e.g., `AIProviderException.java`)

**Functions:**
- camelCase (e.g., `buildAnalysisPrompt()`, `prepareForApi()`, `savePlant()`)
- Descriptive action verbs: `get`, `set`, `build`, `save`, `delete`, `parse`, `create`, `analyze`
- Boolean getters: `is{Property}()` (e.g., `isConfigured()`)
- Lifecycle methods: Android-standard names (e.g., `onCreate()`, `onDestroy()`, `onViewCreated()`)

**Variables:**
- Instance fields: private with underscore prefix or no prefix (e.g., `private TextView plantCommonName`, `private ExecutorService executor`)
- Parameters: camelCase (e.g., `imageUri`, `plantId`, `prompt`)
- Constants: UPPER_SNAKE_CASE (e.g., `MAX_DIMENSION`, `JPEG_QUALITY`, `API_URL`)
- Local variables: camelCase (e.g., `base64Image`, `knownPlantName`, `previousAnalyses`)

**Types:**
- Model classes: PascalCase inner classes for nested structures (e.g., `PlantAnalysisResult.Identification`, `PlantAnalysisResult.CarePlan.Watering`)
- Entity classes: PascalCase with @Entity annotation (e.g., `Plant`, `Analysis`, `CareItem`)
- Provider classes: `{Name}Provider` implementing `AIProvider` interface

## Code Style

**Formatting:**
- No automatic formatter configured; follows Android Studio defaults
- 4-space indentation (standard Java)
- Opening braces on same line: `if (condition) {`
- Method chaining formatted with line breaks for readability (e.g., in `ClaudeProvider.analyzePhoto()`)

**Linting:**
- No explicit linter configuration (gradle, .eslintrc, etc.) found
- Follows implicit Android conventions
- @NonNull and @Nullable annotations used throughout for null-safety (e.g., `@NonNull Application application`, `@Nullable Bundle savedInstanceState`)

## Import Organization

**Order:**
1. Android framework imports (e.g., `import android.content.Intent;`, `import android.view.View;`)
2. AndroidX imports (e.g., `import androidx.appcompat.app.AppCompatActivity;`, `import androidx.lifecycle.ViewModel;`)
3. Google Material imports (e.g., `import com.google.android.material.button.MaterialButton;`)
4. Third-party library imports (e.g., `import com.squareup.okhttp3.*;`, `import com.bumptech.glide.Glide;`)
5. Project package imports (e.g., `import com.anshul.plantwise.ui.camera.CameraActivity;`)
6. Java standard library imports (e.g., `import java.io.IOException;`, `import java.util.List;`)
7. org.json imports (e.g., `import org.json.JSONObject;`, `import org.json.JSONArray;`)

**Path Aliases:**
- No path aliases configured; full package paths used throughout

## Error Handling

**Patterns:**
- Custom exception class: `AIProviderException extends Exception` in `src/main/java/com/anshul/plantwise/ai/AIProviderException.java`
  - Constructor: `AIProviderException(String message)`
  - Constructor with cause: `AIProviderException(String message, Throwable cause)`
- Try-catch in activities/async tasks to handle exceptions from repositories (see `AnalysisActivity.startAnalysis()`)
- Errors shown to user via Toast: `Toast.makeText(this, "Error message", Toast.LENGTH_SHORT).show()`
- Errors logged: `android.util.Log.e("TAG", "message")` for error level, `android.util.Log.d("TAG", "message")` for debug
- JSON parsing wrapped in try-catch with JSONException handling (e.g., `JsonParserTest` tests JSONException scenarios)
- IOException handling for file and network operations (see `AnalysisActivity.copyImageToLocal()`)
- Network errors from OkHttp wrapped as `AIProviderException` with status code and message (see `ClaudeProvider.analyzePhoto()`)

## Logging

**Framework:** `android.util.Log` (not Logcat abstraction; direct Android API)

**Patterns:**
- Log.e() for errors: `Log.e("AnalysisActivity", "Failed to copy image locally: " + e.getMessage())`
- Log.d() for debug info: `Log.d("AnalysisActivity", "Image copied to: " + localImageUri)`
- Tag uses class/activity name as string literal
- Messages include context (what operation failed) and exception details
- Logged on background threads during file operations and database access

## Comments

**When to Comment:**
- Method documentation: JSDoc-style for public utility methods (e.g., `ImageUtils.prepareForApi()`, `ImageUtils.savePhoto()`)
- Inline comments for non-obvious logic (e.g., explaining defensive markdown stripping in `ClaudeProvider`)
- TODO/FIXME: Not actively used in codebase; focus is on working code

**JSDoc/TSDoc:**
- Used selectively for public utility methods: `/** Compress and resize a photo, return base64 string ready for API. */`
- Parameters and return types documented in javadoc style
- Example in `ImageUtils`:
  ```java
  /**
   * Compress and resize a photo, return base64 string ready for API.
   */
  public static String prepareForApi(Context context, Uri imageUri) throws IOException
  ```

## Function Design

**Size:**
- Utility functions: typically 10-40 lines (e.g., `resizeBitmap()` is 19 lines, `prepareForApi()` is 18 lines)
- Activity methods: range from 5 lines (simple navigation) to 100+ lines for complex UI setup (e.g., `AnalysisActivity.displayResults()` is 47 lines)
- Keep complex logic (data fetching, parsing) in separate functions off main thread

**Parameters:**
- Max 3 parameters typical; if more needed, consider object/model wrapping (e.g., `startAnalysis()` uses instance fields rather than many parameters)
- Use Objects.requireNonNull() or @NonNull annotations for required parameters
- Nullable parameters marked with @Nullable (e.g., `@Nullable Bundle savedInstanceState`)

**Return Values:**
- Single return value per function
- Void if no data returned (e.g., UI update methods)
- May return custom objects (`PlantAnalysisResult`) for complex data
- Use Optional-style checks: `if (value != null && !value.isEmpty())`

## Module Design

**Exports:**
- Public methods explicit: `public static String prepareForApi()`, `public PlantAnalysisResult analyzePhoto()`
- Private helpers for implementation details: `private static Bitmap resizeBitmap()`, `private void copyImageToLocal()`
- DAOs expose CRUD operations: `public void insertPlant()`, `public void deletePlant()`, `public LiveData<List<Plant>> getAllPlants()`

**Barrel Files:**
- No barrel files (index.ts) used; each class imported directly
- Example: `import com.anshul.plantwise.data.entity.Plant;` (not from index)

## ViewModels and LiveData

**Pattern:**
- All ViewModels extend `AndroidViewModel` to access Application context (e.g., `LibraryViewModel extends AndroidViewModel`)
- LiveData used for observable data: `LiveData<List<Plant>> allPlants`
- Observers set in fragments/activities via: `viewModel.getAllPlants().observe(getViewLifecycleOwner(), plants -> { ... })`
- Database queries return LiveData directly from DAO: `return plantDao.getAllPlants()`

**Example from `LibraryViewModel`:**
```java
public class LibraryViewModel extends AndroidViewModel {
    private final PlantDao plantDao;
    private final LiveData<List<Plant>> allPlants;

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        plantDao = db.plantDao();
        allPlants = plantDao.getAllPlants();
    }

    public LiveData<List<Plant>> getAllPlants() {
        return allPlants;
    }
}
```

## Threading

**Pattern:**
- Main thread: UI operations only
- Background thread: All database and network operations via ExecutorService
- Example from `AnalysisActivity`:
  ```java
  executor = Executors.newSingleThreadExecutor();
  executor.execute(() -> {
      try {
          String base64Image = ImageUtils.prepareForApi(this, imageUri);
          analysisResult = provider.analyzePhoto(base64Image, prompt);
          runOnUiThread(this::displayResults);
      } catch (Exception e) {
          runOnUiThread(() -> showError(e.getMessage()));
      }
  });
  ```
- Switch to UI thread with `runOnUiThread()` for result handling

## String Resources

**Pattern:**
- User-visible text stored in `strings.xml` resource file
- Referenced as `R.string.{resource_name}` in code
- Example: `setTitle(R.string.api_key)`, `Toast.makeText(this, R.string.api_key_saved, Toast.LENGTH_SHORT)`
- No hardcoded strings in source code for UI text

## Entity Design

**Public fields:** All entity fields are public for Room compatibility
- Example from `Plant.java`:
  ```java
  @PrimaryKey
  @NonNull
  public String id;

  @ColumnInfo(name = "common_name")
  public String commonName;
  ```
- Database column mappings via @ColumnInfo annotations
- No getter/setter wrappers; direct field access

---

*Convention analysis: 2026-02-07*
