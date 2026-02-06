# External Integrations

**Analysis Date:** 2026-02-07

## APIs & External Services

**AI Vision Providers (Pluggable):**
- Claude (Anthropic) - `com.anshul.plantwise.ai.ClaudeProvider`
  - API: `https://api.anthropic.com/v1/messages`
  - Model: claude-sonnet-4-20250514
  - Auth: x-api-key header
  - Format: Base64 JPEG images in image content blocks
  - Response: JSON with content.text field containing analysis result

- ChatGPT/GPT-4 (OpenAI) - `com.anshul.plantwise.ai.OpenAIProvider`
  - API: `https://api.openai.com/v1/chat/completions`
  - Model: gpt-4o
  - Auth: Bearer token in Authorization header
  - Format: data:image/jpeg;base64 URLs in image_url blocks
  - Response: JSON with choices[0].message.content containing analysis result

- Gemini (Google) - `com.anshul.plantwise.ai.GeminiProvider`
  - API: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent`
  - Auth: API key as query parameter (?key=...)
  - Format: Base64 JPEG images in inlineData blocks
  - Response: JSON with candidates[0].content.parts[0].text
  - Note: Free tier available at aistudio.google.com

- Perplexity - `com.anshul.plantwise.ai.PerplexityProvider`
  - API: `https://api.perplexity.ai/chat/completions`
  - Model: llama-3.1-sonar-large-128k-online
  - Auth: Bearer token in Authorization header
  - Format: data:image/jpeg;base64 URLs in image_url blocks
  - Response: JSON with choices[0].message.content containing analysis result
  - Warning: May have limited image support

**Network Configuration:**
- Timeouts: 30 seconds connect, 60 seconds read (for slow AI responses)
- All providers use OkHttp3 with custom timeout configuration in `com.anshul.plantwise.ai` implementations

## Data Storage

**Primary Database:**
- Room/SQLite
  - Database name: `plantwise_database`
  - Location: `com.anshul.plantwise.data.db.AppDatabase`
  - Entities: Plant, Analysis, CareItem
  - DAOs: PlantDao, AnalysisDao, CareItemDao
  - Strategy: Singleton instance with synchronized lazy initialization
  - Migration strategy: `fallbackToDestructiveMigration()` (clears data on schema changes)

**Local File Storage:**
- Device internal storage directory for plant photos: `getFilesDir()/plant_photos/`
- No external storage or cloud sync

**Encrypted Preferences:**
- EncryptedSharedPreferences via AndroidX Security
  - File name: `plantwise_secure_prefs`
  - Encryption: AES-256-GCM for keys and values
  - Storage location: `com.anshul.plantwise.util.KeystoreHelper`
  - Stores: API key, AI provider selection
  - Fallback: Regular SharedPreferences if encryption initialization fails

## Authentication & Identity

**Auth Provider:**
- Custom via encrypted API keys (user-supplied)
- No centralized authentication service
- Each AI provider has independent API key management
- Provider selection stored locally: `KeystoreHelper.getProvider()`
- Default provider: Gemini (free tier)

**Key Management:**
- API keys stored in EncryptedSharedPreferences with AES256_GCM encryption
- MasterKey created per device via AndroidX Security
- Keys never logged or exposed in stack traces
- Keys cleared on app data reset

## Monitoring & Observability

**Error Tracking:**
- Not detected - Errors logged to logcat and shown to user via Toast/Snackbar

**Logs:**
- Android logcat (runtime logs)
- JSON response parsing with defensive fallback for markdown-wrapped responses
- Raw response logged on parse failures for debugging

**Exceptions:**
- Custom `AIProviderException` for all provider errors
- Wrapped in try-catch with user-visible error messages

## CI/CD & Deployment

**Hosting:**
- Google Play Store (intended distribution)
- APK/App Bundle generation via Android Studio Build menu

**CI Pipeline:**
- Not detected - No automated CI/CD configuration found

**Build System:**
- Gradle with Android Gradle Plugin 9.0.0
- Debug and Release build types

## Environment Configuration

**Required env vars:**
- None (development/build-time)

**User-configured at Runtime:**
- `api_key` - AI provider API key (encrypted in SharedPreferences)
- `ai_provider` - Selected AI provider (Gemini, OpenAI, Claude, Perplexity)

**Settings UI Location:**
- `com.anshul.plantwise.ui.settings.SettingsFragment`

**Secrets location:**
- EncryptedSharedPreferences in `com.anshul.plantwise.util.KeystoreHelper`
- Android Keystore (managed by AndroidX Security)

## Webhooks & Callbacks

**Incoming:**
- Not applicable - Local-first architecture with no backend server

**Outgoing:**
- Not applicable - All API calls are request-response only to AI vision providers

## Photo Processing

**Image Optimization (before sending to AI):**
- Compression via `Bitmap.compress()` with JPEG quality 80
- Resize to max 1024px longest edge
- Target: ~1MB maximum file size
- Utilities: `com.anshul.plantwise.util.ImageUtils`

**Base64 Encoding:**
- All AI providers receive base64-encoded JPEG images
- Encoding done before network transmission

## Response Parsing

**JSON Parsing:**
- `org.json.JSONObject` for direct parsing
- Custom `com.anshul.plantwise.util.JsonParser` for PlantAnalysisResult deserialization
- Defensive parsing: Handles markdown-wrapped JSON (removes ``` backticks)
- Extracts first { and last } to handle extra text in responses
- Try-catch wrapping with `AIProviderException` on parse failure

---

*Integration audit: 2026-02-07*
