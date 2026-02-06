# PlantWise - Bugs, Enhancements & Features

Tracked issues from the codebase audit (Feb 2026). All items are filed as GitHub Issues on [anshuldante/PlantWise](https://github.com/anshuldante/PlantWise/issues).

---

## Bugs

### Critical

| # | Title | Description |
|---|-------|-------------|
| [#1](https://github.com/anshuldante/PlantWise/issues/1) | Race condition between image copy and analysis | `copyImageToLocal()` and `startAnalysis()` both run on a background executor. If the gallery picker URI becomes invalid before `prepareForApi()` reads it, analysis fails. The local copy may not be ready in time either. |
| [#2](https://github.com/anshuldante/PlantWise/issues/2) | Re-analyze does not link to existing plant | PlantDetailActivity FAB opens CameraActivity without passing `plantId`. New analysis creates a duplicate plant instead of adding an analysis to the existing one. |
| [#3](https://github.com/anshuldante/PlantWise/issues/3) | CareItems are never persisted to database | AI care plan data is displayed but never saved to the `care_items` table. CareItem entity/DAO exist but are unused. Blocks the entire care reminders feature. |
| [#4](https://github.com/anshuldante/PlantWise/issues/4) | Single API key stored for all providers | Switching providers overwrites the previous key. User loses their Claude key when switching to Gemini and entering a new one. Each provider needs its own key slot. |

### Medium

| # | Title | Description |
|---|-------|-------------|
| [#5](https://github.com/anshuldante/PlantWise/issues/5) | Analysis rawResponse never saved | `Analysis.rawResponse` field exists but is never populated during save. Loses the raw AI response, breaking offline caching and debugging. |
| [#6](https://github.com/anshuldante/PlantWise/issues/6) | Perplexity provider model does not support vision | `llama-3.1-sonar-large-128k-online` is text-only. Sending images always fails. Need a vision-capable model or disable image sending. |
| [#7](https://github.com/anshuldante/PlantWise/issues/7) | Missing manifest permissions | Plan requires `READ_MEDIA_IMAGES`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`. Only `CAMERA` and `INTERNET` are declared. Blocks notifications and may affect gallery on older APIs. |

### Low

| # | Title | Description |
|---|-------|-------------|
| [#8](https://github.com/anshuldante/PlantWise/issues/8) | NPE in PlantCardAdapter.areContentsTheSame | `oldItem.commonName.equals(newItem.commonName)` throws NPE when `commonName` is null. Should use `Objects.equals()`. |
| [#9](https://github.com/anshuldante/PlantWise/issues/9) | PromptBuilder sends raw timestamps to AI | `a.createdAt` is a `long` epoch millis. AI sees `1707259200000` instead of a human-readable date. |
| [#10](https://github.com/anshuldante/PlantWise/issues/10) | GridLayoutManager(1) is effectively LinearLayoutManager | Single-column grid adds overhead for no benefit. Should be 2-column grid or plain LinearLayoutManager. |

---

## Enhancements

### High Priority

| # | Title | Description |
|---|-------|-------------|
| [#11](https://github.com/anshuldante/PlantWise/issues/11) | Implement Repository layer (MVVM compliance) | CLAUDE.md mandates "ViewModels call Repositories." Currently AnalysisActivity directly calls AppDatabase and AI providers. Need PlantRepository and AnalysisRepository. |
| [#12](https://github.com/anshuldante/PlantWise/issues/12) | Implement AIProviderFactory | CLAUDE.md rule #2: "Use AIProviderFactory to get the active provider." Currently AnalysisActivity has a hardcoded if/else chain importing all 4 providers directly. |
| [#13](https://github.com/anshuldante/PlantWise/issues/13) | Extract AnalysisViewModel and CameraViewModel | AnalysisActivity has 500+ lines of business logic (API calls, DB operations, image processing) directly in the Activity. Violates MVVM architecture. |

### Medium Priority

| # | Title | Description |
|---|-------|-------------|
| [#14](https://github.com/anshuldante/PlantWise/issues/14) | Add plant deletion from library | `PlantDao.deletePlant()` exists but no UI exposes it. Users cannot remove plants. |
| [#15](https://github.com/anshuldante/PlantWise/issues/15) | Add plant nickname editing | `Plant.nickname` field exists and is displayed when set, but no UI to set or edit it. |
| [#16](https://github.com/anshuldante/PlantWise/issues/16) | Add location field to plant UI | `Plant.location` exists, `PromptBuilder` accepts it, but no UI collects it. Always passed as null. |
| [#17](https://github.com/anshuldante/PlantWise/issues/17) | Create PlantWiseApp application class | Planned but never created. Useful for global initialization. |

### Low Priority

| # | Title | Description |
|---|-------|-------------|
| [#18](https://github.com/anshuldante/PlantWise/issues/18) | Add photo quality/brightness check | Plan gotcha #2: check brightness before sending, toast "Try better lighting." |
| [#19](https://github.com/anshuldante/PlantWise/issues/19) | Add "Correct this" button for AI hallucination | Plan gotcha #1: let users flag incorrect AI identification. |

---

## Features (Not Yet Implemented)

### High Priority

| # | Title | Description |
|---|-------|-------------|
| [#20](https://github.com/anshuldante/PlantWise/issues/20) | Timeline tab and fragment | Plan specifies 4 bottom nav tabs (Library, Camera, Timeline, Settings). Timeline is entirely absent. No TimelineFragment or TimelineViewModel. Should show care history across all plants. |
| [#21](https://github.com/anshuldante/PlantWise/issues/21) | Care reminders and notifications | NotificationHelper, AlarmManager + NotificationCompat for care schedule reminders. Core planned feature, completely missing. Depends on #3 (CareItems being saved). |

### Medium Priority

| # | Title | Description |
|---|-------|-------------|
| [#22](https://github.com/anshuldante/PlantWise/issues/22) | Quick Diagnosis Mode | Phase 2 feature: snap and ask "what's wrong?" without saving to library. |
| [#23](https://github.com/anshuldante/PlantWise/issues/23) | Growth timeline per plant | Phase 2 feature: horizontal photo timeline per plant with side-by-side comparison view. |

### Low Priority

| # | Title | Description |
|---|-------|-------------|
| [#24](https://github.com/anshuldante/PlantWise/issues/24) | Freemium model / usage tracking | Phase 3 feature: free tier with limited analyses, BYOK unlimited. |
