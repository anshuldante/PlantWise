package com.leafiq.app.ui.analysis;

import com.leafiq.app.data.entity.Analysis;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.PlantAnalysisResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Pure function that maps domain state to UI state.
 * No Android dependencies, no side effects, trivially testable.
 * Answers: "Given this analysis data, parse status, and context,
 * what should the UI display?"
 * Decision tree:
 * - OK parse status: success with optional disclaimers
 * - PARTIAL parse status: successWithFallback with "Some details couldn't be loaded"
 * - FAILED/EMPTY parse status: successWithFallback with "Full details unavailable"
 * - No photo + no metadata: unrecoverable error state
 */
public class AnalysisStateMapper {

    /**
     * Maps analysis domain state to UI display state.
     *
     * @param analysis The analysis entity from database
     * @param plant The plant entity (may be null)
     * @param parsedResult The parsed analysis result (may be incomplete or null)
     * @param parseStatus Parse status: "OK", "PARTIAL", "FAILED", "EMPTY"
     * @param isQuickDiagnosis Whether this was a Quick Diagnosis
     * @param qualityOverridden Whether quality check was overridden
     * @return UI state with appropriate success/fallback/error configuration
     */
    public static AnalysisUiState mapForDisplay(
            Analysis analysis,
            Plant plant,
            PlantAnalysisResult parsedResult,
            String parseStatus,
            boolean isQuickDiagnosis,
            boolean qualityOverridden) {

        // Unrecoverable: no photo + no metadata = can't display anything useful
        if (isUnrecoverable(analysis, plant)) {
            return AnalysisUiState.error("Unable to load analysis data");
        }

        String reanalyzedDate = formatReanalyzedDate(analysis.reAnalyzedAt);

        // OK parse status: full success
        if ("OK".equals(parseStatus)) {
            String disclaimer = getQuickDiagnosisDisclaimer(isQuickDiagnosis);
            return AnalysisUiState.successWithMetadata(
                parsedResult,
                qualityOverridden,
                disclaimer,
                reanalyzedDate
            );
        }

        // PARTIAL, FAILED, or EMPTY: show fallback with minimal data
        PlantAnalysisResult minimalResult = parsedResult != null
            ? parsedResult
            : buildMinimalResult(analysis, plant);

        String fallbackMessage = getFallbackMessage(parseStatus);
        boolean showReanalyze = shouldShowReanalyze(parseStatus, analysis.photoPath);

        return AnalysisUiState.successWithFallback(
            minimalResult,
            fallbackMessage,
            showReanalyze,
            reanalyzedDate
        );
    }

    /**
     * Determines if re-analyze button should be shown.
     * Show button only if parse failed AND photo exists (can retry).
     * @param parseStatus Parse status: "OK", "PARTIAL", "FAILED", "EMPTY"
     * @param photoPath Path to analysis photo
     * @return true if re-analyze button should be shown
     */
    public static boolean shouldShowReanalyze(String parseStatus, String photoPath) {
      if ("OK".equals(parseStatus)) return false;
      return photoPath != null && !photoPath.isEmpty();
    }

    /**
     * Gets fallback message based on parse status.
     *
     * @param parseStatus Parse status: "OK", "PARTIAL", "FAILED", "EMPTY"
     * @return Human-readable fallback message, or null for OK status
     */
    public static String getFallbackMessage(String parseStatus) {
        if (parseStatus == null) return null;

        switch (parseStatus) {
            case "PARTIAL":
                return "Some details couldn't be loaded";
            case "FAILED":
            case "EMPTY":
                return "Full details unavailable";
            default:
                return null;
        }
    }

    /**
     * Builds minimal result from analysis entity fields.
     * Used as fallback when parse failed but we have some metadata.
     *
     * @param analysis The analysis entity
     * @param plant The plant entity (may be null)
     * @return Minimal PlantAnalysisResult with available data
     */
    public static PlantAnalysisResult buildMinimalResult(Analysis analysis, Plant plant) {
        PlantAnalysisResult result = new PlantAnalysisResult();

        // Identification: use plant name or "Unknown Plant"
        result.identification = new PlantAnalysisResult.Identification();
        result.identification.commonName = plant != null && plant.commonName != null
            ? plant.commonName
            : "Unknown Plant";
        result.identification.scientificName = plant != null && plant.scientificName != null
            ? plant.scientificName
            : "";
        result.identification.confidence = "unknown";

        // Health assessment: use analysis fields
        result.healthAssessment = new PlantAnalysisResult.HealthAssessment();
        result.healthAssessment.score = analysis.healthScore;
        result.healthAssessment.summary = analysis.summary != null ? analysis.summary : "";

        return result;
    }

    /**
     * Gets Quick Diagnosis disclaimer text.
     *
     * @param isQuickDiagnosis Whether this was a Quick Diagnosis
     * @return Disclaimer text, or null for full analysis
     */
    public static String getQuickDiagnosisDisclaimer(boolean isQuickDiagnosis) {
        if (!isQuickDiagnosis) return null;
        return "Quick assessment — results may be less precise. If this doesn't look right, try full analysis with a clearer photo.";
    }

    /**
     * Checks if analysis is unrecoverable.
     * Unrecoverable = no photo AND no useful metadata (can't display anything).
     *
     * @param analysis The analysis entity
     * @param plant The plant entity (may be null)
     * @return true if no useful data to display
     */
    public static boolean isUnrecoverable(Analysis analysis, Plant plant) {
        boolean hasPhoto = analysis.photoPath != null && !analysis.photoPath.isEmpty();
        boolean hasName = (plant != null && plant.commonName != null && !plant.commonName.isEmpty());
        boolean hasAnyMeta = hasName
            || analysis.healthScore > 0
            || (analysis.summary != null && !analysis.summary.isEmpty());

        return !hasPhoto && !hasAnyMeta;
    }

    /**
     * Formats re-analyzed timestamp to human-readable date.
     *
     * @param reAnalyzedAt Timestamp in milliseconds, or null
     * @return Formatted date string like "Jan 15, 2026", or null
     */
    private static String formatReanalyzedDate(Long reAnalyzedAt) {
        if (reAnalyzedAt == null) return null;

        SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return format.format(new Date(reAnalyzedAt));
    }
}
