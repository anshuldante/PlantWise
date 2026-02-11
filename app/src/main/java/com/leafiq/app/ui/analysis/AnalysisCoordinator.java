package com.leafiq.app.ui.analysis;

import com.leafiq.app.util.PhotoQualityChecker;

/**
 * Orchestrates multi-step analysis flows.
 * Handles decision logic that spans multiple user interactions:
 * - Quality check -> override -> analyze flow
 * - Re-analyze from failed parse -> choose photo -> analyze
 * - Quick vs Full mode branching
 * Keeps Activity as thin UI glue code.
 * All methods are static and pure (no state, no Android dependencies).
 * This makes flow decisions trivially testable without mocking.
 */
public class AnalysisCoordinator {

    /**
     * Result of a quality check decision.
     * Tells the Activity what to do next.
     */
    public enum QualityAction {
        PROCEED_TO_ANALYSIS,   // Quality passed, proceed
        SHOW_BORDERLINE_WARNING, // Show warning with override option
        SHOW_EGREGIOUS_REJECTION, // Show rejection without override
        SKIP_CHECK_ERROR       // Quality check itself failed, proceed anyway
    }

    /**
     * Determines what action to take based on quality check result.
     * Pure logic, no UI involvement.
     * Decision flow:
     * 1. If result is null (check failed) -> SKIP_CHECK_ERROR (proceed anyway, don't block on check failure)
     * 2. If passed -> PROCEED_TO_ANALYSIS
     * 3. If failed but overrideAllowed -> SHOW_BORDERLINE_WARNING (user can proceed with "Use Anyway")
     * 4. If failed and !overrideAllowed -> SHOW_EGREGIOUS_REJECTION (user must retake photo)
     *
     * @param result Quality check result from PhotoQualityChecker
     * @return QualityAction enum indicating what the Activity should do
     */
    public static QualityAction evaluateQuality(PhotoQualityChecker.QualityResult result) {
        if (result == null) return QualityAction.SKIP_CHECK_ERROR;
        if (result.passed) return QualityAction.PROCEED_TO_ANALYSIS;
        if (result.overrideAllowed) return QualityAction.SHOW_BORDERLINE_WARNING;
        return QualityAction.SHOW_EGREGIOUS_REJECTION;
    }

    /**
     * Result of a re-analyze decision.
     */
    public enum ReanalyzeAction {
        USE_ORIGINAL_PHOTO,    // Re-analyze with existing photo
        TAKE_NEW_PHOTO,        // Launch camera for new photo
        UPGRADE_TO_FULL,       // Quick -> Full with new photo
        CREATE_NEW_ENTRY       // Create new analysis entry (not replace)
    }

    /**
     * Determines re-analyze action based on user choice and context.
     * Per CONTEXT.md:
     * - Default focus on original photo
     * - Allow upgrade to Full with new photo
     * - Never auto-upgrade silently
     *
     * @param useOriginalPhoto Whether user chose to use original photo
     * @param userChoseUpgrade Whether user explicitly chose to upgrade to Full
     * @param userChoseAnalyzeAsNew Whether user chose "Analyze as new"
     * @return ReanalyzeAction enum indicating what flow to take
     */
    public static ReanalyzeAction evaluateReanalyze(
            boolean useOriginalPhoto,
            boolean userChoseUpgrade,
            boolean userChoseAnalyzeAsNew) {

        if (userChoseAnalyzeAsNew) return ReanalyzeAction.CREATE_NEW_ENTRY;
        if (userChoseUpgrade && !useOriginalPhoto) return ReanalyzeAction.UPGRADE_TO_FULL;
        if (useOriginalPhoto) return ReanalyzeAction.USE_ORIGINAL_PHOTO;
        return ReanalyzeAction.TAKE_NEW_PHOTO;
    }

    /**
     * Determines if Quick Diagnosis should be used for re-analysis.
     * Per CONTEXT.md: "If original was Quick Diagnosis: default to Quick with original"
     * Decision flow:
     * 1. If user explicitly chose upgrade to Full -> return false (Full mode)
     * 2. Otherwise, match original mode (Quick stays Quick, Full stays Full)
     *
     * @param originalWasQuick Whether the original analysis was Quick Diagnosis
     * @param userChoseUpgrade Whether user explicitly chose to upgrade to Full
     * @return true if Quick Diagnosis should be used, false for Full mode
     */
    public static boolean shouldUseQuickForReanalyze(
            boolean originalWasQuick,
            boolean userChoseUpgrade) {
        if (userChoseUpgrade) return false; // Explicit upgrade to Full
        return originalWasQuick; // Match original mode
    }
}
