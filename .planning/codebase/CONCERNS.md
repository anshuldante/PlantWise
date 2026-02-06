# Codebase Concerns

**Analysis Date:** 2026-02-07

## Tech Debt

**Database Migration Strategy:**
- Issue: Using `fallbackToDestructiveMigration()` without `exportSchema` - data loss risk on schema changes
- Files: `app/src/main/java/com/anshul/plantwise/data/db/AppDatabase.java`
- Impact: Any schema change (adding columns, new entities) will drop all user data without warning. Users lose all plant history and photos permanently.
- Fix approach: Enable `exportSchema = true`, commit schema files to version control, implement proper migrations using `Migration` classes instead of destructive fallback. Test migration path before releasing.

**Missing Read Permissions:**
- Issue: AndroidManifest declares only CAMERA and INTERNET; missing READ_MEDIA_IMAGES (required for SDK 33+) and POST_NOTIFICATIONS (for care reminders)
- Files: `app/src/main/AndroidManifest.xml`
- Impact: Gallery picker will fail silently on Android 13+; notification features cannot be added without manifest declaration
- Fix approach: Add `<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />` and `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` to manifest

**Encryption Fallback to Unencrypted Storage:**
- Issue: KeystoreHelper catches encryption failures and silently falls back to regular SharedPreferences
- Files: `app/src/main/java/com/anshul/plantwise/util/KeystoreHelper.java` (lines 39-42)
- Impact: API keys stored unencrypted if encryption initialization fails - defeats security purpose without warning
- Fix approach: Crash loudly on encryption failure, log the exception, or provide user-facing error instead of silent fallback

## Known Bugs

**Image URI Access Loss:**
- Symptoms: Gallery-selected images may become inaccessible after app exits analysis flow
- Files: `app/src/main/java/com/anshul/plantwise/ui/analysis/AnalysisActivity.java` (lines 88-94, 108-139)
- Trigger: User picks image from gallery → AnalysisActivity copies to temp_images cache → on rotation/destroy, temp file deleted
- Workaround: Copy image to persistent location (plant_photos) immediately, not in background thread after URI access time expires
- Current impact: Race condition between URI permission expiration and copyImageToLocal() executor task

**Executor Not Handling Rejections:**
- Symptoms: Tasks submitted to executor after shutdown will be silently rejected
- Files: `app/src/main/java/com/anshul/plantwise/ui/analysis/AnalysisActivity.java` (lines 84, 109, 221, 435), `app/src/main/java/com/anshul/plantwise/ui/camera/CameraActivity.java` (line 70)
- Trigger: User navigates away during analysis/save operations
- Workaround: Check `executor.isShutdown()` before submitting tasks; ignore failures gracefully
- Current impact: "Analysis failed" error may not display if executor is dead

**JSON Parsing Handles String Boolean Values Incorrectly:**
- Symptoms: CLAUDE.md notes "true/false as strings" but JsonParser only handles boolean primitives with `optBoolean()`
- Files: `app/src/main/java/com/anshul/plantwise/util/JsonParser.java` (lines 97, 105)
- Trigger: AI provider returns `"needed": "true"` (string) instead of `"needed": true` (boolean)
- Workaround: JsonParser.optBoolean() should call optString() first, then parse "true"/"false" strings
- Current impact: Pruning/repotting recommendations ignored if AI returns string booleans

## Security Considerations

**API Key Exposure in Error Messages:**
- Risk: AIProviderException catches responses and may include partial API key in error messages
- Files: `app/src/main/java/com/anshul/plantwise/ai/ClaudeProvider.java` (line 85), OpenAI/Gemini/Perplexity providers
- Current mitigation: Error messages shown to user via Toast (only display name shown), raw responses logged via android.util.Log
- Recommendations: Ensure no API key appears in `response.body().string()` error logging; scrub sensitive headers before logging

**Unencrypted Photo Storage:**
- Risk: Plant photos stored in app's internal files directory (`plant_photos/`, `thumbnails/`) accessible to other apps on rooted devices or via adb
- Files: `app/src/main/java/com/anshul/plantwise/util/ImageUtils.java` (lines 49, 77), `app/src/main/java/com/anshul/plantwise/ui/analysis/AnalysisActivity.java` (lines 467-471)
- Current mitigation: Uses internal app storage (not world-readable by default), not external storage
- Recommendations: For sensitive plant identification, consider encrypting image files at rest using AndroidX Security

**API Key Stored in SharedPreferences:**
- Risk: SharedPreferences can be backed up and extracted on rooted devices
- Files: `app/src/main/java/com/anshul/plantwise/util/KeystoreHelper.java` (lines 32-37)
- Current mitigation: Uses EncryptedSharedPreferences with AES256_GCM
- Recommendations: Verify encryption is actually working (catch block fallback exists). Consider biometric authentication before API calls for extra safety.

## Performance Bottlenecks

**Image Processing on Background Thread Without Progress:**
- Problem: CameraActivity.takePhoto() shows progress bar but AnalysisActivity.copyImageToLocal() and startAnalysis() run without visibility to user
- Files: `app/src/main/java/com/anshul/plantwise/ui/analysis/AnalysisActivity.java` (lines 108-139, 218-250)
- Cause: copyImageToLocal() is fire-and-forget; startAnalysis() begins immediately even if copy hasn't completed
- Improvement path: Use CountDownLatch or combine image copy + API call into single background task. Show progress steps: "Preparing image..." → "Analyzing..." → "Saving results..."

**No Connection Timeout for Slow Networks:**
- Problem: OkHttp configured with 60-second read timeout but no request timeout
- Files: `app/src/main/java/com/anshul/plantwise/ai/ClaudeProvider.java` (lines 28-31)
- Cause: On very slow networks (2G, poor WiFi), user sees infinite loading
- Improvement path: Add request timeout of 90 seconds total; show "Network slow, retrying..." message after 30 seconds

**Base64 Image Encoding in Main Request Construction:**
- Problem: Building request with large base64 string (1MB uncompressed = ~1.3MB base64) in JSON object
- Files: `app/src/main/java/com/anshul/plantwise/ai/ClaudeProvider.java` (lines 38-66)
- Cause: JSONObject concatenates strings, allocating memory for entire request before OkHttp sends streaming chunks
- Improvement path: Use OkHttp RequestBody with streaming or chunked encoding for large images

## Fragile Areas

**AnalysisActivity is Monolithic (522 lines):**
- Files: `app/src/main/java/com/anshul/plantwise/ui/analysis/AnalysisActivity.java`
- Why fragile: Single Activity handles camera intent → image display → API calls → JSON parsing → UI rendering → database saves. Changes to any step risk breaking others.
- Safe modification: Extract image handling into ImageManager, API calls into AnalysisRepository, UI display into separate methods. Use ViewModel to coordinate.
- Test coverage: Only UI tests (LibraryFragmentTest, MainActivityTest) exist; AnalysisActivity business logic has zero unit test coverage

**JsonParser Tightly Coupled to PlantAnalysisResult Structure:**
- Files: `app/src/main/java/com/anshul/plantwise/util/JsonParser.java`
- Why fragile: Any change to AI prompt output format requires manual JSON parsing updates. No schema validation.
- Safe modification: Define JSON schema (JsonSchema or similar), validate responses before parsing. Add version field to AI response to handle format changes.
- Test coverage: Good - JsonParserTest covers nominal cases, edge cases (missing fields), and invalid JSON

**AI Provider Selection Hardcoded in Activity:**
- Files: `app/src/main/java/com/anshul/plantwise/ui/analysis/AnalysisActivity.java` (lines 203-216)
- Why fragile: Adding new provider requires editing AnalysisActivity directly. No factory or registry pattern.
- Safe modification: Implement AIProviderFactory (already mentioned in CLAUDE.md as requirement) that instantiates providers based on keystoreHelper.getProvider()
- Test coverage: ClaudeProviderTest exists but only tests configuration checks, not actual API responses (placeholder test at line 80)

**Temporary Image Files Not Cleaned Up on Errors:**
- Files: `app/src/main/java/com/anshul/plantwise/ui/analysis/AnalysisActivity.java` (lines 108-139, 509-521), `app/src/main/java/com/anshul/plantwise/ui/camera/CameraActivity.java` (lines 133-170)
- Why fragile: If error occurs during analysis, temp file in cache_dir remains. Repeated analyses create orphaned files.
- Safe modification: Use try-with-resources, wrap in ExecutorService that catches exceptions, or implement cleanup callback
- Test coverage: No tests for file cleanup

## Scaling Limits

**SQLite Database for Large Plant Libraries:**
- Current capacity: Room/SQLite on Android handles thousands of records efficiently, but QueryUtils.getAllPlants() loads entire list into memory
- Limit: With 10,000+ plants and large analysis history, LiveData.observe() causes ANR when loading list
- Scaling path: Implement pagination in PlantDao (LIMIT/OFFSET), use Paging3 library, lazy-load thumbnails with Glide

**Bitmap Memory for Large Photos:**
- Current capacity: MAX_DIMENSION=1024px with JPEG quality 80 = ~100-300KB per image, manageable for 50-100 plant photos
- Limit: Device with <2GB RAM may run out of heap if multiple bitmaps decoded simultaneously
- Scaling path: Use BitmapFactory.Options.inSampleSize or inJustDecodeBounds to pre-calculate optimal scale before decoding

**API Call Retry Without Backoff:**
- Current capacity: Single analysis takes 30-60 seconds (network + AI processing)
- Limit: No exponential backoff for rate-limited API responses (429). Retry button clicked repeatedly will hammer API.
- Scaling path: Implement RetryPolicy with exponential backoff (1s, 2s, 4s, 8s), honor Retry-After header, queue requests

## Dependencies at Risk

**No Explicit Version Pinning:**
- Risk: Using `libs.versions.toml` aliases (e.g., `libs.okhttp`) without visible versions; hard to track breaking changes
- Impact: Room 2.6.1 to 3.x may have migration issues; OkHttp 4.x to 5.x may change API
- Migration plan: Add version constraints to build.gradle. Document upgrade path for each major library. Test on Android 15+ periodically.

**OkHttp Client Reuse:**
- Risk: Creating new OkHttpClient in each AI provider (ClaudeProvider, GeminiProvider, etc.) instead of singleton
- Impact: 4 providers × new client = 4 separate connection pools, 4× memory overhead
- Migration plan: Create singleton OkHttpClient in application context or dependency injection

## Missing Critical Features

**No Retry Logic for Failed Analyses:**
- Problem: Analysis fails (network error, API error) → user must retake photo and restart
- Blocks: Users with poor connectivity cannot complete analysis workflow
- Recommendation: Save image + prompt immediately, implement background retry queue, notify when retry succeeds

**No Care Reminder Notifications:**
- Problem: CarePlan has watering frequency and seasonal notes, but no scheduled notifications
- Blocks: Users forget plant care schedule
- Recommendation: Use AlarmManager (mentioned in CLAUDE.md) to schedule notifications for watering, fertilizing, repotting

**No Plant Health Tracking Over Time:**
- Problem: Analysis history saved per analysis, but no trend visualization (health score chart)
- Blocks: Users can't see "plant was 4/10, now 8/10" improvement
- Recommendation: Add health history chart in PlantDetailActivity, fetch last 12 analyses

**No Export/Backup Feature:**
- Problem: Plant data lives only on device; no backup or export
- Blocks: Users lose all data on device loss/reset
- Recommendation: Add "Export to JSON" feature, cloud sync (optional), or device backup verification

## Test Coverage Gaps

**AnalysisActivity Business Logic Not Tested:**
- What's not tested: Image preparation, API call flow, result display, save to database, error handling
- Files: `app/src/main/java/com/anshul/plantwise/ui/analysis/AnalysisActivity.java`
- Risk: Refactoring without test coverage will break in production. Image handling bugs (race conditions) undetected.
- Priority: High - core feature

**AI Provider Network Responses Not Tested:**
- What's not tested: Actual analyzePhoto() method with mocked HTTP responses. ClaudeProviderTest line 80 is placeholder.
- Files: `app/src/main/java/com/anshul/plantwise/ai/ClaudeProvider.java`, OpenAI/Gemini/Perplexity variants
- Risk: API response parsing changes silently break all providers. Missing error handling (4xx, 5xx responses).
- Priority: High - all analyses depend on this

**ImageUtils Edge Cases Not Tested:**
- What's not tested: OOM on very large images, failed URI access, disk full when saving, bitmap recycling correctness
- Files: `app/src/main/java/com/anshul/plantwise/util/ImageUtils.java`
- Risk: Crashes on older devices with limited RAM. Temp file leaks.
- Priority: Medium

**Database DAO Queries Not Tested at Scale:**
- What's not tested: Performance with 10,000 plants. SQLite index efficiency. Transaction isolation.
- Files: `app/src/main/java/com/anshul/plantwise/data/db/PlantDao.java`, `AnalysisDao.java`
- Risk: Query N+1 problems. LiveData updates blocked on main thread.
- Priority: Medium

**SettingsFragment Provider Switching Not Tested:**
- What's not tested: Changing provider mid-analysis. Clearing API key. Provider display names.
- Files: `app/src/main/java/com/anshul/plantwise/ui/settings/SettingsFragment.java`
- Risk: Switching provider leaves old API key in keystoreHelper. UI shows wrong provider name.
- Priority: Medium

**CameraActivity Permissions Not Tested:**
- What's not tested: Camera permission denied flow. Gallery picker cancellation. Photo capture errors.
- Files: `app/src/main/java/com/anshul/plantwise/ui/camera/CameraActivity.java`
- Risk: Denying permission crashes app instead of showing message. Permission lifecycle edge cases.
- Priority: Medium

---

*Concerns audit: 2026-02-07*
