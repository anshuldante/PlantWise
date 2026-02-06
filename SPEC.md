# PlantWise — Project Spec

## Overview

A native Android app (Java) that lets you photograph plants, identify them via AI, analyze their health, and get actionable care advice. Built with a commercialization-friendly architecture (bring-your-own-API-key model).

---

## Architectural Decisions

### Why Native Android with Java
- 14 years of Java experience — can read, debug, and reason about every line
- No framework magic, no bridge layers, no mystery errors
- Android Studio's tooling catches issues at compile time

### AI Backend: Claude API (Swappable)
- Claude Sonnet's vision capabilities for plant ID and health analysis
- AI provider abstracted behind an interface — can swap in Gemini, GPT-4o later
- For personal use: API key in local encrypted storage
- For commercialization: users enter their own key (BYOK model)

### Storage: All Local
- Room (SQLite) for structured data (plants, analyses, care schedules)
- Internal app storage for photos
- EncryptedSharedPreferences for API keys
- No server. No cloud. Privacy-friendly.

### UI: XML Layouts + Material Design 3
- Battle-tested, well-documented, enormous example base
- Material 3 for polished modern look out of the box

### Architecture Pattern: MVVM
- ViewModels + LiveData for reactive UI
- Repository layer coordinates AI service + database
- Clean separation: UI → ViewModel → Repository → Data/AI

---

## Features

### Phase 1 — Core (Build This First)

#### 1. Camera / Photo Capture
- Open camera via CameraX for in-app camera
- Pick from gallery via photo picker
- Auto-compress and resize before AI analysis

#### 2. AI Analysis (the core feature)
- Send photo to Claude's vision API with a structured prompt
- Get back structured JSON with:
  - **Plant identification**: Common name, scientific name, confidence level
  - **Health assessment**: Overall health score (1-10), identified issues
  - **Care advice**: Immediate actions needed
  - **Ongoing care plan**: Watering, light, fertilizer, pruning, repotting, seasonal notes

#### 3. Plant Library
- RecyclerView grid of saved plants
- Each card: thumbnail, name, health score badge, last analyzed date
- Tap to see full detail + analysis history

#### 4. Re-analyze
- New photo of existing plant
- AI receives previous analyses as context
- Notes changes over time

### Phase 2 — Quality of Life

#### 5. Care Reminders
- AlarmManager + NotificationChannel for scheduled push notifications
- Based on AI-recommended care frequency

#### 6. Growth Timeline
- Horizontal photo timeline per plant
- Side-by-side comparison view

#### 7. Quick Diagnosis Mode
- Snap and ask "what's wrong?" without saving to library

### Phase 3 — Commercialization

#### 8. Bring Your Own Key (BYOK)
- Settings screen for API key entry
- Multiple providers: Claude, Gemini, OpenAI
- Key encrypted via Android Keystore

#### 9. Freemium Model
- Free: Limited analyses/month
- BYOK: Unlimited with own key
- Premium: Extra features

---

## Architecture Diagram

```
┌──────────────────────────────────────────────────┐
│                    UI Layer                        │
│  Activities / Fragments + XML Layouts              │
│                                                    │
│  ┌────────────┐ ┌────────────┐ ┌───────────────┐  │
│  │ Camera     │ │ Plant      │ │ Settings      │  │
│  │ Activity   │ │ Library    │ │ Activity      │  │
│  │            │ │ Fragment   │ │               │  │
│  └─────┬──────┘ └─────┬──────┘ └───────────────┘  │
│        │               │                           │
│  ┌─────▼───────────────▼─────────────────────┐     │
│  │           ViewModel Layer                  │     │
│  │  (PlantViewModel, AnalysisViewModel, etc.) │     │
│  │  LiveData for reactive UI updates          │     │
│  └─────────────────┬─────────────────────────┘     │
│                    │                               │
│  ┌─────────────────▼─────────────────────────┐     │
│  │           Repository Layer                 │     │
│  │  (PlantRepository, AnalysisRepository)     │     │
│  │  Coordinates between AI service + database │     │
│  └──────┬──────────────────┬─────────────────┘     │
│         │                  │                       │
│  ┌──────▼──────┐   ┌──────▼───────────────┐       │
│  │ AI Service  │   │ Local Data Layer      │       │
│  │ Layer       │   │                       │       │
│  │             │   │ Room DB (plants,      │       │
│  │ ┌────────┐  │   │   analyses, care)     │       │
│  │ │Claude  │  │   │ Internal Storage      │       │
│  │ │Adapter │  │   │   (photos)            │       │
│  │ ├────────┤  │   │ EncryptedSharedPrefs  │       │
│  │ │Gemini  │  │   │   (API keys)          │       │
│  │ │Adapter │  │   │                       │       │
│  │ ├────────┤  │   └───────────────────────┘       │
│  │ │OpenAI  │  │                                   │
│  │ │Adapter │  │                                   │
│  │ └────────┘  │                                   │
│  └─────────────┘                                   │
└──────────────────────────────────────────────────┘
```

---

## Project Structure

```
app/src/main/java/com/yourname/plantwise/
├── PlantWiseApp.java              # Application class
│
├── data/
│   ├── db/
│   │   ├── AppDatabase.java       # Room database
│   │   ├── PlantDao.java
│   │   ├── AnalysisDao.java
│   │   └── CareItemDao.java
│   ├── entity/
│   │   ├── Plant.java
│   │   ├── Analysis.java
│   │   └── CareItem.java
│   ├── repository/
│   │   ├── PlantRepository.java
│   │   └── AnalysisRepository.java
│   └── model/
│       ├── PlantAnalysisResult.java    # Parsed AI response POJO
│       ├── HealthAssessment.java
│       ├── CareAction.java
│       └── CarePlan.java
│
├── ai/
│   ├── AIProvider.java            # Interface
│   ├── AIProviderFactory.java     # Returns correct provider from settings
│   ├── AIProviderException.java   # Custom exception
│   ├── ClaudeProvider.java        # Anthropic Messages API adapter
│   ├── GeminiProvider.java        # (Phase 3)
│   ├── OpenAIProvider.java        # (Phase 3)
│   └── PromptBuilder.java         # Constructs the analysis prompt
│
├── ui/
│   ├── library/
│   │   ├── LibraryFragment.java
│   │   ├── LibraryViewModel.java
│   │   └── PlantCardAdapter.java
│   ├── camera/
│   │   ├── CameraActivity.java
│   │   └── CameraViewModel.java
│   ├── analysis/
│   │   ├── AnalysisActivity.java
│   │   └── AnalysisViewModel.java
│   ├── detail/
│   │   ├── PlantDetailActivity.java
│   │   └── PlantDetailViewModel.java
│   ├── timeline/
│   │   ├── TimelineFragment.java
│   │   └── TimelineViewModel.java
│   ├── settings/
│   │   ├── SettingsActivity.java
│   │   └── SettingsViewModel.java
│   └── MainActivity.java
│
└── util/
    ├── ImageUtils.java
    ├── JsonParser.java
    ├── KeystoreHelper.java
    └── NotificationHelper.java

app/src/main/res/
├── layout/
│   ├── activity_main.xml
│   ├── fragment_library.xml
│   ├── item_plant_card.xml
│   ├── activity_camera.xml
│   ├── activity_analysis.xml
│   ├── activity_plant_detail.xml
│   ├── fragment_timeline.xml
│   └── activity_settings.xml
├── values/
│   ├── themes.xml
│   ├── colors.xml
│   └── strings.xml
├── menu/
│   └── bottom_nav.xml
└── drawable/
```

---

## Code Blueprints

These are reference implementations. Claude Code should use these as the basis,
adapting as needed but following the patterns exactly.

### AI Provider Interface

```java
public interface AIProvider {
    PlantAnalysisResult analyzePhoto(String imageBase64, String prompt)
        throws AIProviderException;
    boolean isConfigured();
    String getDisplayName();
}
```

### Claude Provider

```java
public class ClaudeProvider implements AIProvider {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-20250514";
    private static final String API_VERSION = "2023-06-01";
    private final String apiKey;
    private final OkHttpClient client;

    public ClaudeProvider(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)  // AI responses can be slow
            .build();
    }

    @Override
    public PlantAnalysisResult analyzePhoto(String imageBase64, String prompt)
            throws AIProviderException {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);
            requestBody.put("max_tokens", 2048);

            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");

            JSONArray content = new JSONArray();

            // Image block
            JSONObject imageBlock = new JSONObject();
            imageBlock.put("type", "image");
            JSONObject source = new JSONObject();
            source.put("type", "base64");
            source.put("media_type", "image/jpeg");
            source.put("data", imageBase64);
            imageBlock.put("source", source);
            content.put(imageBlock);

            // Text block
            JSONObject textBlock = new JSONObject();
            textBlock.put("type", "text");
            textBlock.put("text", prompt);
            content.put(textBlock);

            userMessage.put("content", content);
            messages.put(userMessage);
            requestBody.put("messages", messages);

            RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", API_VERSION)
                .addHeader("content-type", "application/json")
                .post(body)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new AIProviderException(
                        "API error: " + response.code() + " " + response.message());
                }

                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                JSONArray contentArray = json.getJSONArray("content");
                String aiText = contentArray.getJSONObject(0).getString("text");

                // Defensive: strip markdown backticks if AI wrapped the JSON
                aiText = aiText.trim();
                if (aiText.startsWith("```")) {
                    aiText = aiText.replaceAll("^```json?\\s*", "")
                                   .replaceAll("\\s*```$", "");
                }

                return JsonParser.parsePlantAnalysis(aiText);
            }
        } catch (JSONException | IOException e) {
            throw new AIProviderException("Analysis failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String getDisplayName() {
        return "Claude (Anthropic)";
    }
}
```

### Prompt Builder

```java
public class PromptBuilder {

    public static String buildAnalysisPrompt(
            @Nullable String knownPlantName,
            @Nullable List<Analysis> previousAnalyses,
            @Nullable String location) {

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert botanist and plant care specialist. ");
        sb.append("Analyze this plant photo and provide a detailed assessment.\n\n");

        if (knownPlantName != null) {
            sb.append("This plant was previously identified as: ")
              .append(knownPlantName).append("\n");
        } else {
            sb.append("Identify this plant.\n");
        }

        if (location != null) {
            sb.append("Location: ").append(location).append("\n");
        }

        if (previousAnalyses != null && !previousAnalyses.isEmpty()) {
            sb.append("\nPrevious analyses for this plant:\n");
            for (Analysis a : previousAnalyses) {
                sb.append("- ").append(a.createdAt)
                  .append(": Health ").append(a.healthScore)
                  .append("/10, Summary: ").append(a.summary)
                  .append("\n");
            }
            sb.append("\nCompare the current photo with previous assessments ");
            sb.append("and note any improvements or deterioration.\n");
        }

        sb.append("\nRespond ONLY with valid JSON in this exact format ");
        sb.append("(no markdown, no backticks, no explanatory text before or after):\n");
        sb.append(getJsonTemplate());

        sb.append("\n\nBe specific and actionable. ");
        sb.append("If you can see the pot, soil, or surroundings, ");
        sb.append("factor those into your assessment. ");
        sb.append("If you're unsure about something, say so rather than guessing.");

        return sb.toString();
    }

    private static String getJsonTemplate() {
        return "{\n"
            + "  \"identification\": {\n"
            + "    \"commonName\": \"string\",\n"
            + "    \"scientificName\": \"string\",\n"
            + "    \"confidence\": \"high | medium | low\",\n"
            + "    \"notes\": \"string\"\n"
            + "  },\n"
            + "  \"healthAssessment\": {\n"
            + "    \"score\": 1-10,\n"
            + "    \"summary\": \"1-2 sentence overview\",\n"
            + "    \"issues\": [\n"
            + "      {\n"
            + "        \"name\": \"string\",\n"
            + "        \"severity\": \"low | medium | high\",\n"
            + "        \"description\": \"string\",\n"
            + "        \"affectedArea\": \"string\"\n"
            + "      }\n"
            + "    ]\n"
            + "  },\n"
            + "  \"immediateActions\": [\n"
            + "    {\n"
            + "      \"action\": \"string\",\n"
            + "      \"priority\": \"urgent | soon | when_convenient\",\n"
            + "      \"detail\": \"string\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"carePlan\": {\n"
            + "    \"watering\": {\n"
            + "      \"frequency\": \"string\",\n"
            + "      \"amount\": \"string\",\n"
            + "      \"notes\": \"string\"\n"
            + "    },\n"
            + "    \"light\": {\n"
            + "      \"ideal\": \"string\",\n"
            + "      \"current\": \"string\",\n"
            + "      \"adjustment\": \"string or null\"\n"
            + "    },\n"
            + "    \"fertilizer\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"frequency\": \"string\",\n"
            + "      \"nextApplication\": \"string\"\n"
            + "    },\n"
            + "    \"pruning\": {\n"
            + "      \"needed\": true/false,\n"
            + "      \"instructions\": \"string\",\n"
            + "      \"when\": \"string\"\n"
            + "    },\n"
            + "    \"repotting\": {\n"
            + "      \"needed\": true/false,\n"
            + "      \"signs\": \"string\",\n"
            + "      \"recommendedPotSize\": \"string or null\"\n"
            + "    },\n"
            + "    \"seasonal\": \"string\"\n"
            + "  },\n"
            + "  \"funFact\": \"string\"\n"
            + "}";
    }
}
```

### Room Entities

```java
// Plant.java
@Entity(tableName = "plants")
public class Plant {
    @PrimaryKey @NonNull
    public String id;

    @ColumnInfo(name = "common_name")
    public String commonName;

    @ColumnInfo(name = "scientific_name")
    public String scientificName;

    public String nickname;
    public String location;

    @ColumnInfo(name = "thumbnail_path")
    public String thumbnailPath;

    @ColumnInfo(name = "latest_health_score")
    public int latestHealthScore;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}

// Analysis.java
@Entity(tableName = "analyses",
        foreignKeys = @ForeignKey(
            entity = Plant.class,
            parentColumns = "id",
            childColumns = "plant_id",
            onDelete = ForeignKey.CASCADE))
public class Analysis {
    @PrimaryKey @NonNull
    public String id;

    @ColumnInfo(name = "plant_id", index = true)
    public String plantId;

    @ColumnInfo(name = "photo_path")
    public String photoPath;

    @ColumnInfo(name = "raw_response")
    public String rawResponse;

    @ColumnInfo(name = "health_score")
    public int healthScore;

    public String summary;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}

// CareItem.java
@Entity(tableName = "care_items",
        foreignKeys = @ForeignKey(
            entity = Plant.class,
            parentColumns = "id",
            childColumns = "plant_id",
            onDelete = ForeignKey.CASCADE))
public class CareItem {
    @PrimaryKey @NonNull
    public String id;

    @ColumnInfo(name = "plant_id", index = true)
    public String plantId;

    public String type;     // "water", "fertilize", "prune", "repot"

    @ColumnInfo(name = "frequency_days")
    public int frequencyDays;

    @ColumnInfo(name = "last_done")
    public long lastDone;

    @ColumnInfo(name = "next_due")
    public long nextDue;

    public String notes;
}
```

### Room DAOs

```java
// PlantDao.java
@Dao
public interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY updated_at DESC")
    LiveData<List<Plant>> getAllPlants();

    @Query("SELECT * FROM plants WHERE id = :id")
    LiveData<Plant> getPlantById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlant(Plant plant);

    @Update
    void updatePlant(Plant plant);

    @Delete
    void deletePlant(Plant plant);
}

// AnalysisDao.java
@Dao
public interface AnalysisDao {
    @Query("SELECT * FROM analyses WHERE plant_id = :plantId ORDER BY created_at DESC")
    LiveData<List<Analysis>> getAnalysesForPlant(String plantId);

    @Query("SELECT * FROM analyses WHERE plant_id = :plantId ORDER BY created_at DESC LIMIT 5")
    List<Analysis> getRecentAnalysesSync(String plantId);

    @Insert
    void insertAnalysis(Analysis analysis);
}

// CareItemDao.java
@Dao
public interface CareItemDao {
    @Query("SELECT * FROM care_items WHERE plant_id = :plantId")
    LiveData<List<CareItem>> getCareItemsForPlant(String plantId);

    @Query("SELECT * FROM care_items WHERE next_due < :timestamp ORDER BY next_due ASC")
    LiveData<List<CareItem>> getOverdueItems(long timestamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCareItem(CareItem item);

    @Update
    void updateCareItem(CareItem item);

    @Delete
    void deleteCareItem(CareItem item);
}
```

### Image Utils

```java
public class ImageUtils {

    private static final int MAX_DIMENSION = 1024;
    private static final int JPEG_QUALITY = 80;

    /**
     * Compress and resize a photo, return base64 string ready for API.
     */
    public static String prepareForApi(Context context, Uri imageUri) throws IOException {
        Bitmap original = MediaStore.Images.Media.getBitmap(
            context.getContentResolver(), imageUri);

        Bitmap resized = resizeBitmap(original, MAX_DIMENSION);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);

        if (original != resized) {
            original.recycle();
        }

        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * Save a photo to internal app storage, return the file path.
     */
    public static String savePhoto(Context context, Uri sourceUri, String plantId)
            throws IOException {
        File dir = new File(context.getFilesDir(), "plant_photos");
        if (!dir.exists()) dir.mkdirs();

        String filename = plantId + "_" + System.currentTimeMillis() + ".jpg";
        File dest = new File(dir, filename);

        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        return dest.getAbsolutePath();
    }

    private static Bitmap resizeBitmap(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap;
        }

        float scale = Math.min(
            (float) maxDimension / width,
            (float) maxDimension / height
        );

        return Bitmap.createScaledBitmap(
            bitmap,
            Math.round(width * scale),
            Math.round(height * scale),
            true
        );
    }
}
```

---

## Design

### Color Palette
```xml
<!-- res/values/colors.xml -->
<color name="seed">#2E7D32</color>           <!-- Forest green — primary -->
<color name="surface_light">#FEFBF6</color>  <!-- Warm cream background -->
<color name="health_good">#4CAF50</color>     <!-- Healthy green -->
<color name="health_warning">#FF9800</color>  <!-- Needs attention orange -->
<color name="health_bad">#F44336</color>      <!-- Urgent red -->
<color name="text_primary">#1B1B1B</color>
<color name="text_secondary">#5F6368</color>
<color name="card_background">#FFFFFF</color>
```

### UI Guidelines
- Material 3 with green seed color for dynamic theming
- Rounded cards (16dp corner radius) for plant library items
- Health score as circular progress indicator on each card
- Camera screen: full-bleed with minimal capture button
- Analysis results: card-based layout with sections (ID, Health, Actions, Care Plan)
- Bottom navigation: Library (home), Camera (center/prominent), Timeline, Settings
- Use Glide for all image loading

---

## Cost Estimate

Claude Sonnet for vision: ~$0.003/image input + ~$0.015/text output per analysis.
At 5 plants/week: **~$0.50/month**.

---

## Gotchas to Watch For

1. **AI hallucination**: Add a "correct this" button. Send multiple angles for better accuracy.
2. **Photo quality**: Check brightness before sending. Toast: "Try better lighting."
3. **API key security**: EncryptedSharedPreferences + Keystore. Never log the key.
4. **JSON parsing**: AI may wrap in backticks. Strip defensively — find first `{` and last `}`.
5. **Large images**: Always resize to 1024px max, JPEG 80. Cuts costs 80%.
6. **Offline**: Library and reminders work offline. Cache last analysis per plant.
7. **Permissions**: Android 13+ uses READ_MEDIA_IMAGES, not READ_EXTERNAL_STORAGE.
8. **Room migrations**: Use fallbackToDestructiveMigration() in Phase 1. Write proper migrations for release.
