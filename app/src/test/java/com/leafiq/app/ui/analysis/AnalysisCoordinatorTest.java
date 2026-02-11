package com.leafiq.app.ui.analysis;

import com.leafiq.app.util.PhotoQualityChecker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for AnalysisCoordinator.
 * Tests pure static flow decision methods without Android dependencies.
 */
public class AnalysisCoordinatorTest {

    // ========================================
    // evaluateQuality tests
    // ========================================

    @Test
    public void evaluateQuality_passed_proceedToAnalysis() {
        PhotoQualityChecker.QualityResult result = PhotoQualityChecker.QualityResult.ok(50f, 0.5f);

        AnalysisCoordinator.QualityAction action = AnalysisCoordinator.evaluateQuality(result);

        assertEquals(AnalysisCoordinator.QualityAction.PROCEED_TO_ANALYSIS, action);
    }

    @Test
    public void evaluateQuality_borderline_showWarning() {
        PhotoQualityChecker.QualityResult result =
            PhotoQualityChecker.QualityResult.fail("Photo appears blurry", "blur", true, "borderline", 40f, 0.5f);

        AnalysisCoordinator.QualityAction action = AnalysisCoordinator.evaluateQuality(result);

        assertEquals(AnalysisCoordinator.QualityAction.SHOW_BORDERLINE_WARNING, action);
    }

    @Test
    public void evaluateQuality_egregious_showRejection() {
        PhotoQualityChecker.QualityResult result =
            PhotoQualityChecker.QualityResult.fail("Photo is extremely blurry", "blur", false, "egregious", 10f, 0.5f);

        AnalysisCoordinator.QualityAction action = AnalysisCoordinator.evaluateQuality(result);

        assertEquals(AnalysisCoordinator.QualityAction.SHOW_EGREGIOUS_REJECTION, action);
    }

    @Test
    public void evaluateQuality_null_skipCheckError() {
        AnalysisCoordinator.QualityAction action = AnalysisCoordinator.evaluateQuality(null);

        assertEquals(AnalysisCoordinator.QualityAction.SKIP_CHECK_ERROR, action);
    }

    @Test
    public void evaluateQuality_egregiousBlur_showRejection() {
        PhotoQualityChecker.QualityResult result =
            PhotoQualityChecker.QualityResult.egregiousFail("Extremely blurry", "blur", 10f, 0.5f);

        AnalysisCoordinator.QualityAction action = AnalysisCoordinator.evaluateQuality(result);

        assertEquals(AnalysisCoordinator.QualityAction.SHOW_EGREGIOUS_REJECTION, action);
    }

    // ========================================
    // evaluateReanalyze tests
    // ========================================

    @Test
    public void evaluateReanalyze_useOriginal_returnsUseOriginal() {
        AnalysisCoordinator.ReanalyzeAction action = AnalysisCoordinator.evaluateReanalyze(
            /* useOriginalPhoto */ true,
            /* originalWasQuick */ false,
            /* userChoseUpgrade */ false,
            /* userChoseAnalyzeAsNew */ false
        );

        assertEquals(AnalysisCoordinator.ReanalyzeAction.USE_ORIGINAL_PHOTO, action);
    }

    @Test
    public void evaluateReanalyze_takeNewPhoto_returnsTakeNew() {
        AnalysisCoordinator.ReanalyzeAction action = AnalysisCoordinator.evaluateReanalyze(
            /* useOriginalPhoto */ false,
            /* originalWasQuick */ false,
            /* userChoseUpgrade */ false,
            /* userChoseAnalyzeAsNew */ false
        );

        assertEquals(AnalysisCoordinator.ReanalyzeAction.TAKE_NEW_PHOTO, action);
    }

    @Test
    public void evaluateReanalyze_upgradeToFull_returnsUpgrade() {
        AnalysisCoordinator.ReanalyzeAction action = AnalysisCoordinator.evaluateReanalyze(
            /* useOriginalPhoto */ false, // New photo
            /* originalWasQuick */ true,
            /* userChoseUpgrade */ true,
            /* userChoseAnalyzeAsNew */ false
        );

        assertEquals(AnalysisCoordinator.ReanalyzeAction.UPGRADE_TO_FULL, action);
    }

    @Test
    public void evaluateReanalyze_analyzeAsNew_returnsCreateNew() {
        // "Analyze as new" takes precedence over all other options
        AnalysisCoordinator.ReanalyzeAction action = AnalysisCoordinator.evaluateReanalyze(
            /* useOriginalPhoto */ true,
            /* originalWasQuick */ false,
            /* userChoseUpgrade */ false,
            /* userChoseAnalyzeAsNew */ true
        );

        assertEquals(AnalysisCoordinator.ReanalyzeAction.CREATE_NEW_ENTRY, action);
    }

    @Test
    public void evaluateReanalyze_quickOriginal_defaultUseOriginal() {
        // Original was Quick Diagnosis, user chose to use original photo
        AnalysisCoordinator.ReanalyzeAction action = AnalysisCoordinator.evaluateReanalyze(
            /* useOriginalPhoto */ true,
            /* originalWasQuick */ true,
            /* userChoseUpgrade */ false,
            /* userChoseAnalyzeAsNew */ false
        );

        assertEquals(AnalysisCoordinator.ReanalyzeAction.USE_ORIGINAL_PHOTO, action);
    }

    @Test
    public void evaluateReanalyze_upgradeWithOriginal_returnsUseOriginal() {
        // Edge case: User chose upgrade but also chose original photo
        // useOriginalPhoto takes precedence (checked first in method)
        AnalysisCoordinator.ReanalyzeAction action = AnalysisCoordinator.evaluateReanalyze(
            /* useOriginalPhoto */ true,
            /* originalWasQuick */ true,
            /* userChoseUpgrade */ true,
            /* userChoseAnalyzeAsNew */ false
        );

        assertEquals(AnalysisCoordinator.ReanalyzeAction.USE_ORIGINAL_PHOTO, action);
    }

    // ========================================
    // shouldUseQuickForReanalyze tests
    // ========================================

    @Test
    public void shouldUseQuick_originalQuick_noUpgrade_returnsTrue() {
        // Original was Quick, no upgrade -> stay in Quick mode
        boolean result = AnalysisCoordinator.shouldUseQuickForReanalyze(
            /* originalWasQuick */ true,
            /* userChoseUpgrade */ false
        );

        assertEquals(true, result);
    }

    @Test
    public void shouldUseQuick_originalQuick_withUpgrade_returnsFalse() {
        // Original was Quick, explicit upgrade -> Full mode
        boolean result = AnalysisCoordinator.shouldUseQuickForReanalyze(
            /* originalWasQuick */ true,
            /* userChoseUpgrade */ true
        );

        assertEquals(false, result);
    }

    @Test
    public void shouldUseQuick_originalFull_returnsFalse() {
        // Original was Full, no upgrade -> stay in Full mode
        boolean result = AnalysisCoordinator.shouldUseQuickForReanalyze(
            /* originalWasQuick */ false,
            /* userChoseUpgrade */ false
        );

        assertEquals(false, result);
    }

    @Test
    public void shouldUseQuick_originalFull_withUpgrade_returnsFalse() {
        // Original was Full, upgrade flag doesn't change anything (already Full)
        boolean result = AnalysisCoordinator.shouldUseQuickForReanalyze(
            /* originalWasQuick */ false,
            /* userChoseUpgrade */ true
        );

        assertEquals(false, result);
    }
}
