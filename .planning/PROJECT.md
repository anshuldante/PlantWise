# PlantWise — Project Context

## What This Is

PlantWise is a native Android app (Java, min SDK 26, target SDK 34) that lets users photograph plants, identify them via AI vision APIs, analyze their health, and get actionable care advice. Uses a bring-your-own-API-key (BYOK) model with support for Claude, Gemini, OpenAI, and Perplexity providers.

## Core Value

**One-tap plant analysis**: Take a photo → get instant AI-powered identification, health assessment, and personalized care plan. All data stays on-device (local-first, no cloud).

## Current State

The app is functional but incomplete. Phase 1 (Core) is ~70% done. A comprehensive audit identified **24 issues** filed as GitHub Issues (#1-#24) in [anshuldante/PlantWise](https://github.com/anshuldante/PlantWise/issues):

- **10 bugs** (4 critical, 3 medium, 3 low)
- **9 enhancements** (3 high, 4 medium, 2 low priority)
- **5 features** not yet implemented (2 high, 2 medium, 1 low priority)

### What Works
- Camera capture and gallery picker
- AI analysis via 4 providers (Claude, Gemini, OpenAI, Perplexity)
- Plant library with RecyclerView grid
- Plant detail view with analysis history
- Settings for provider selection and API key entry
- Image compression/optimization before API calls
- Material Design 3 theming with dark mode support
- Room database with Plant, Analysis, CareItem entities
- Existing unit tests (JsonParser, ImageUtils, PromptBuilder, ClaudeProvider)
- Existing instrumented tests (PlantDao, AnalysisDao, SavePlantFlow, UI tests)

### What's Broken/Missing
- Race condition between image copy and analysis (critical)
- Re-analyze creates duplicate plants instead of linking to existing (critical)
- CareItems never persisted to database (critical, blocks reminders)
- Single API key slot shared across all providers (critical)
- Analysis rawResponse never saved
- Perplexity model doesn't support vision
- Missing manifest permissions (READ_MEDIA_IMAGES, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM)
- No Repository layer (MVVM violation)
- No AIProviderFactory (hardcoded if/else in Activity)
- No AnalysisViewModel (500+ lines of business logic in Activity)
- Timeline tab completely absent
- Care reminders/notifications not implemented
- No plant deletion, nickname editing, or location field in UI

## Technical Context

- **Language:** Java (NOT Kotlin)
- **Build:** Gradle with Groovy DSL
- **Architecture:** MVVM planned, partially implemented (missing Repository layer, AnalysisViewModel, CameraViewModel)
- **Database:** Room with Plant, Analysis, CareItem entities; using destructive migration
- **AI:** AIProvider interface with 4 implementations; no factory pattern yet
- **Security:** EncryptedSharedPreferences for API keys via KeystoreHelper
- **Testing:** JUnit 4 + Truth + MockWebServer; reasonable coverage for utils, gaps in Activity logic

## Constraints

- Must remain Java (not Kotlin) — developer's 14 years of experience
- Local-first: no backend server, no cloud sync
- BYOK model: users supply their own API keys
- Min SDK 26 (Android 8.0)
- Must follow Material Design 3 guidelines
- All user-facing text in strings.xml

## Requirements

### Validated

- ✓ Camera capture via CameraX — existing
- ✓ Gallery picker for existing photos — existing
- ✓ Image compression before API calls — existing
- ✓ AI analysis with structured JSON response — existing
- ✓ Multi-provider support (Claude, Gemini, OpenAI, Perplexity) — existing
- ✓ Plant library grid view — existing
- ✓ Plant detail with analysis history — existing
- ✓ Settings for provider/API key — existing
- ✓ Encrypted API key storage — existing
- ✓ Material Design 3 theming — existing
- ✓ Dark mode support — existing
- ✓ Room database persistence — existing

### Active

- [ ] BUG-01: Fix race condition between image copy and analysis (#1)
- [ ] BUG-02: Fix re-analyze to link to existing plant (#2)
- [ ] BUG-03: Persist CareItems to database (#3)
- [ ] BUG-04: Per-provider API key storage (#4)
- [ ] BUG-05: Save analysis rawResponse (#5)
- [ ] BUG-06: Fix Perplexity vision model (#6)
- [ ] BUG-07: Add missing manifest permissions (#7)
- [ ] BUG-08: Fix NPE in PlantCardAdapter (#8)
- [ ] BUG-09: Format timestamps in PromptBuilder (#9)
- [ ] BUG-10: Fix GridLayoutManager column count (#10)
- [ ] ARCH-01: Implement Repository layer (#11)
- [ ] ARCH-02: Implement AIProviderFactory (#12)
- [ ] ARCH-03: Extract AnalysisViewModel and CameraViewModel (#13)
- [ ] ENH-01: Add plant deletion from library (#14)
- [ ] ENH-02: Add plant nickname editing (#15)
- [ ] ENH-03: Add location field to plant UI (#16)
- [ ] ENH-04: Create PlantWiseApp application class (#17)
- [ ] ENH-05: Add photo quality/brightness check (#18)
- [ ] ENH-06: Add "Correct this" button for AI hallucination (#19)
- [ ] FEAT-01: Timeline tab and fragment (#20)
- [ ] FEAT-02: Care reminders and notifications (#21)
- [ ] FEAT-03: Quick Diagnosis Mode (#22)
- [ ] FEAT-04: Growth timeline per plant (#23)

### Out of Scope

- Freemium model / usage tracking (#24) — Phase 3, not needed for functional app
- Cloud sync / backend server — contradicts local-first principle
- Kotlin migration — Java is the chosen language

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Fix critical bugs before new features | Broken core flows undermine all other work | Phase 1 = bug fixes |
| Architecture refactor before features | Repository + ViewModel + Factory needed for clean feature development | Phase 2 = architecture |
| Local-first, BYOK model | Privacy, no server costs, user controls their data | Standing |
| Java, not Kotlin | Developer expertise, no framework magic | Standing |

## Reference Documents

- `docs/SPEC.md` — Full project specification with phase plans and code blueprints
- `docs/CLAUDE.md` — Architecture rules, code style, build instructions
- `docs/ISSUES.md` — All 24 issues with GitHub links
- `.planning/codebase/` — 7 codebase analysis documents

---
*Last updated: 2026-02-07 after initialization*
