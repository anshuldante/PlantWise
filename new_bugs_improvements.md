# Bugs and Improvements

## From 11-02-2028 Onwards

- ~~The colors for the timeline icon at the top and the settings need to be changed, barely visible currently.~~ **DONE (15-01)** — Added `app:iconTint="@color/md_theme_onPrimary"` to toolbar menu items
- ~~Remove perplexity as a provider, since there's no plant analysis without an image.~~ **DONE (15-02)** — Completely removed PerplexityProvider, factory case, settings UI, keystore constants, tests
- ~~I see no point in showing the scientific name for a plant on the my garden page, it's perfectly fine in the details page though.~~ **DONE (15-01)** — Removed scientific name from plant card layout and adapter
- ~~The hint and value merge is happening in settings now, gone from the details page where user can add location and name.~~ **DONE (15-03)** — Moved masked key display from hint to status text
- ~~If the image is rejected for some reason, I'm taken back to the home screen. I should be taken back to the camera instead.~~ **DONE (15-03)** — Image rejection dialogs now navigate to CameraActivity
- ~~Is there a way to allow Flash enablement on the camera activity?~~ **DONE (15-04)** — Added 3-state flash toggle (Off/On/Auto) with torch preview

## Code Cleanup

- ~~Why is markReAnalyzed never used? Check and find out if it's needed, if not explain why or why not and remove it if approved by developer.~~ **DONE (16-01)** — Confirmed unused, removed from AnalysisDao
  - ~~getCompletionsForSchedule in CareCompletionDao.~~ **DONE (16-01)** — Confirmed unused, removed
  - ~~updateCareItem and deleteCareItem in CareItemDao~~ **DONE (16-01)** — Confirmed unused (superseded by CareSchedule), removed
- ~~Unused param originalWasQuick in method evaluateReanalyze of AnalysisCoordinator~~ **DONE (16-02)** — Removed param from method signature and all test calls
- ~~Unused methods reanalyzeExisting, saveFieldCorrections in AnalysisViewModel~~ **DONE (16-01)** — Removed both methods + updateAnalysisIfNeeded helper (~140 lines)
- ~~Unused field careScheduleManager in CareOverviewViewModel.~~ **DONE (16-02)** — Removed field, import, and initialization
