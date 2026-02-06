# Testing Patterns

**Analysis Date:** 2026-02-07

## Test Framework

**Runner:**
- Unit Tests: JUnit 4 (via `testImplementation libs.junit`)
- Instrumented Tests: AndroidJUnit4 (via `androidTestImplementation libs.ext.junit` and `@RunWith(AndroidJUnit4.class)`)
- Config: `build.gradle` testOptions enable Android resources in unit tests: `unitTests { includeAndroidResources = true }`

**Assertion Library:**
- Google Truth (`testImplementation libs.truth`, `androidTestImplementation libs.truth`)
- Assertions use: `assertThat(value).isEqualTo()`, `assertThat(value).isNotNull()`, `assertThat(list).hasSize(n)`, `assertThat(value).isTrue()`
- JUnit assertions also available: `@Test(expected = JSONException.class)` for exception testing

**Run Commands:**
```bash
./gradlew test                    # Run all unit tests
./gradlew testDebug               # Run unit tests for debug variant
./gradlew connectedAndroidTest    # Run instrumented tests on device/emulator
```

## Test File Organization

**Location:**
- Unit tests: `src/test/java/com/anshul/plantwise/` (JVM tests, no Android framework)
- Instrumented tests: `src/androidTest/java/com/anshul/plantwise/` (require Android device/emulator)

**Naming:**
- Unit test files: `{Class}Test.java` (e.g., `JsonParserTest.java`, `ImageUtilsTest.java`, `PromptBuilderTest.java`, `ClaudeProviderTest.java`)
- Instrumented test files: Same naming convention (e.g., `PlantDaoTest.java`, `MainActivityTest.java`, `LibraryFragmentTest.java`)

**Structure:**
```
app/src/test/java/com/anshul/plantwise/
├── ai/
│   ├── ClaudeProviderTest.java
│   └── PromptBuilderTest.java
├── data/
│   └── model/
│       └── PlantAnalysisResultTest.java
└── util/
    ├── ImageUtilsTest.java
    └── JsonParserTest.java

app/src/androidTest/java/com/anshul/plantwise/
├── ui/
│   ├── MainActivityTest.java
│   ├── DarkModeTest.java
│   └── LibraryFragmentTest.java
├── util/
│   ├── ImageUtilsInstrumentedTest.java
│   └── UriPermissionTest.java
├── data/
│   └── db/
│       ├── PlantDaoTest.java
│       ├── AnalysisDaoTest.java
│       ├── SavePlantFlowTest.java
│       └── LiveDataTestUtil.java
└── ExampleInstrumentedTest.java
```

## Test Structure

**Suite Organization:**
```java
@RunWith(AndroidJUnit4.class)
public class PlantDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private PlantDao plantDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
            .allowMainThreadQueries()
            .build();
        plantDao = database.plantDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void testName_scenario_expectedBehavior() {
        // Arrange: Set up test data
        Plant plant = createTestPlant("1", "Monstera", "Monstera deliciosa");

        // Act: Perform operation
        plantDao.insertPlant(plant);
        Plant retrieved = plantDao.getPlantByIdSync("1");

        // Assert: Verify results
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.commonName).isEqualTo("Monstera");
    }
}
```

**Patterns:**
- Setup pattern: @Before annotation for test initialization
- Teardown pattern: @After annotation for cleanup (database.close(), executor.shutdown())
- Assertion pattern: Google Truth assertions with fluent API
- Test naming: `{methodName}_{scenario}_{expectedResult}` (e.g., `insertPlant_withSameId_replacesExisting()`)

## Mocking

**Framework:** Mockito (via `testImplementation libs.mockito.core`)

**Patterns:**
- @RunWith(MockitoJUnitRunner.class) for unit test classes using mocks
- Example from `ImageUtilsTest.java`:
  ```java
  @RunWith(MockitoJUnitRunner.class)
  public class ImageUtilsTest {
      @Test
      public void prepareForApi_withNullContext_shouldThrowException() {
          assertThrows(NullPointerException.class, () -> {
              ImageUtils.prepareForApi(null, null);
          });
      }
  }
  ```
- MockWebServer used for HTTP testing (see `ClaudeProviderTest.java`):
  ```java
  private MockWebServer mockWebServer;

  @Before
  public void setUp() throws IOException {
      mockWebServer = new MockWebServer();
      mockWebServer.start();
  }

  @After
  public void tearDown() throws IOException {
      mockWebServer.shutdown();
  }

  @Test
  public void analyzePhoto_parsesJsonResponse() throws Exception {
      String mockApiResponse = "{\"content\": [{\"type\": \"text\", \"text\": \"...\"}]}";
      mockWebServer.enqueue(new MockResponse()
          .setBody(mockApiResponse)
          .setResponseCode(200));
  }
  ```

**What to Mock:**
- External network calls via MockWebServer
- Context-dependent operations that require Android framework
- Live API providers in unit tests (ClaudeProvider would be tested with mock responses)

**What NOT to Mock:**
- Data parsing logic (JsonParser tested with actual JSON strings, no mocks)
- Database operations (use Room in-memory database: `Room.inMemoryDatabaseBuilder()`)
- LiveData operations (use InstantTaskExecutorRule to execute LiveData operations on main thread during tests)

## Fixtures and Factories

**Test Data:**
- Helper method pattern to create test entities:
  ```java
  private Plant createTestPlant(String id, String commonName, String scientificName) {
      Plant plant = new Plant();
      plant.id = id;
      plant.commonName = commonName;
      plant.scientificName = scientificName;
      plant.latestHealthScore = 7;
      plant.createdAt = System.currentTimeMillis();
      plant.updatedAt = System.currentTimeMillis();
      return plant;
  }
  ```
- Inline JSON strings for API response testing (see `JsonParserTest.java`):
  ```java
  String json = "{"
      + "\"identification\": {"
      + "  \"commonName\": \"Monstera Deliciosa\","
      + "  \"scientificName\": \"Monstera deliciosa\","
      + "  \"confidence\": \"high\","
      + "  \"notes\": \"A popular houseplant\""
      + "}"
      + "...}";
  PlantAnalysisResult result = JsonParser.parsePlantAnalysis(json);
  ```

**Location:**
- Test helper methods in same test class (private, no separate factory files)
- Test utilities in dedicated utility class: `LiveDataTestUtil.java` in `src/androidTest/java/com/anshul/plantwise/data/db/`

## Coverage

**Requirements:** No explicit coverage targets or reports configured

**View Coverage:**
```bash
./gradlew test --info    # Run with detailed output
# Coverage report generation not configured; can be added via JaCoCo plugin
```

## Test Types

**Unit Tests:**
- Scope: Individual functions/methods in isolation, no Android framework dependencies
- Approach: Test public API contracts, verify parsing logic, validate data transformations
- Examples:
  - `JsonParserTest`: Tests JSON parsing with various inputs (complete, missing fields, invalid)
  - `ImageUtilsTest`: Tests null handling and exception cases
  - `PromptBuilderTest`: Tests prompt string generation with different parameters
  - `ClaudeProviderTest`: Tests API key validation and JSON extraction from responses

**Instrumented Tests:**
- Scope: Android framework-dependent code, database operations, UI interactions
- Approach: Use real Room database (in-memory), real LiveData, real Activities/Fragments
- Examples:
  - `PlantDaoTest`: Database CRUD operations, LiveData observation with `LiveDataTestUtil`
  - `AnalysisDaoTest`: Analysis record management
  - `MainActivityTest`: Activity lifecycle and navigation
  - `LibraryFragmentTest`: Fragment UI interactions
  - `ImageUtilsInstrumentedTest`: File I/O with real Android context

**E2E Tests:**
- Not currently implemented
- Would test complete flows like "capture photo → analyze → save to library"

## Common Patterns

**Async Testing:**
- LiveData extraction utility: `LiveDataTestUtil.getValue(liveData)` wraps observer in CountDownLatch
  ```java
  public static <T> T getValue(final LiveData<T> liveData) throws InterruptedException {
      final AtomicReference<T> data = new AtomicReference<>();
      final CountDownLatch latch = new CountDownLatch(1);
      Observer<T> observer = new Observer<T>() {
          @Override
          public void onChanged(T value) {
              data.set(value);
              latch.countDown();
              liveData.removeObserver(this);
          }
      };
      liveData.observeForever(observer);
      if (!latch.await(2, TimeUnit.SECONDS)) {
          liveData.removeObserver(observer);
          throw new RuntimeException("LiveData value was never set within timeout");
      }
      return data.get();
  }
  ```
- InstantTaskExecutorRule suppresses background threading in LiveData: `@Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();`

**Error Testing:**
- Exception assertion via JUnit: `@Test(expected = JSONException.class) public void testThrowsException()`
- Exception assertion via Truth: `assertThrows(JSONException.class, () -> { ... })`
- Examples from tests:
  ```java
  @Test(expected = JSONException.class)
  public void parsePlantAnalysis_withInvalidJson_throwsException() throws JSONException {
      String json = "not valid json";
      JsonParser.parsePlantAnalysis(json);
  }

  @Test
  public void prepareForApi_withNullContext_shouldThrowException() {
      assertThrows(NullPointerException.class, () -> {
          ImageUtils.prepareForApi(null, null);
      });
  }
  ```

**Database Testing:**
- In-memory database for isolation: `Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build()`
- Allow main thread queries in tests: `.allowMainThreadQueries()` enables synchronous DAO calls
- Synchronous DAO methods for testing: `getPlantByIdSync()`, `getRecentAnalysesSync()`
- Example from `PlantDaoTest.java`:
  ```java
  @Before
  public void setUp() {
      Context context = ApplicationProvider.getApplicationContext();
      database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
          .allowMainThreadQueries()
          .build();
      plantDao = database.plantDao();
  }

  @Test
  public void getAllPlants_returnsAllPlants_orderedByUpdatedAt() throws InterruptedException {
      Plant plant1 = createTestPlant("1", "First", "First scientific");
      plant1.updatedAt = 1000L;
      plantDao.insertPlant(plant1);

      List<Plant> plants = LiveDataTestUtil.getValue(plantDao.getAllPlants());
      assertThat(plants).hasSize(1);
  }
  ```

**Test Organization for Complex Objects:**
- Comprehensive test class for JSON parsing with multiple test cases per field/section:
  ```java
  public class JsonParserTest {
      @Test
      public void parsePlantAnalysis_withValidJson_parsesIdentification() { ... }

      @Test
      public void parsePlantAnalysis_withValidJson_parsesHealthAssessment() { ... }

      @Test
      public void parsePlantAnalysis_withValidJson_parsesImmediateActions() { ... }

      @Test
      public void parsePlantAnalysis_withMissingFields_usesDefaults() { ... }

      @Test
      public void parsePlantAnalysis_withEmptyIssues_returnsEmptyList() { ... }

      @Test(expected = JSONException.class)
      public void parsePlantAnalysis_withInvalidJson_throwsException() { ... }
  }
  ```

---

*Testing analysis: 2026-02-07*
