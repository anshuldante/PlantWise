package com.leafiq.app.ui.analysis;

import com.leafiq.app.data.model.PlantAnalysisResult;

/**
 * Immutable UI state class for analysis screen.
 * Represents all possible states of the analysis flow.
 * <p>
 * States:
 * - IDLE: Initial state, no analysis in progress
 * - LOADING: Analysis in progress, show loading UI
 * - SUCCESS: Analysis complete, show results
 * - ERROR: Analysis failed, show error message
 * <p>
 * Use factory methods to create state instances (idle(), loading(), success(), error()).
 */
public class AnalysisUiState {

    /**
     * State enumeration for analysis flow.
     */
    public enum State {
        IDLE,       // No analysis in progress
        LOADING,    // Analysis running
        SUCCESS,    // Analysis complete with result
        ERROR       // Analysis failed with error
    }

    private final State state;
    private final PlantAnalysisResult result;
    private final String errorMessage;
    private final String visionUnsupportedProvider;
    private final String loadingMessage;
    private final String fallbackMessage;
    private final boolean showReanalyzeButton;
    private final boolean qualityOverridden;
    private final String quickDiagnosisDisclaimer;
    private final String reanalyzedDate;

    /**
     * Private constructor - use factory methods instead.
     */
    private AnalysisUiState(State state,
                           PlantAnalysisResult result,
                           String errorMessage,
                           String visionUnsupportedProvider,
                           String loadingMessage,
                           String fallbackMessage,
                           boolean showReanalyzeButton,
                           boolean qualityOverridden,
                           String quickDiagnosisDisclaimer,
                           String reanalyzedDate) {
        this.state = state;
        this.result = result;
        this.errorMessage = errorMessage;
        this.visionUnsupportedProvider = visionUnsupportedProvider;
        this.loadingMessage = loadingMessage;
        this.fallbackMessage = fallbackMessage;
        this.showReanalyzeButton = showReanalyzeButton;
        this.qualityOverridden = qualityOverridden;
        this.quickDiagnosisDisclaimer = quickDiagnosisDisclaimer;
        this.reanalyzedDate = reanalyzedDate;
    }

    // ==================== Factory Methods ====================

    /**
     * Creates an IDLE state.
     * Used as initial state before any analysis.
     */
    public static AnalysisUiState idle() {
        return new AnalysisUiState(State.IDLE, null, null, null, null, null, false, false, null, null);
    }

    /**
     * Creates a LOADING state.
     * Used while analysis is in progress.
     */
    public static AnalysisUiState loading() {
        return new AnalysisUiState(State.LOADING, null, null, null, null, null, false, false, null, null);
    }

    /**
     * Creates a LOADING state with a custom progress message.
     * Used for timeout warnings during long-running analysis.
     *
     * @param message Progress message to display (e.g., "Analysis is taking longer than usual...")
     */
    public static AnalysisUiState loadingWithMessage(String message) {
        return new AnalysisUiState(State.LOADING, null, null, null, message, null, false, false, null, null);
    }

    /**
     * Creates a SUCCESS state with analysis result.
     *
     * @param result The analysis result to display
     */
    public static AnalysisUiState success(PlantAnalysisResult result) {
        return new AnalysisUiState(State.SUCCESS, result, null, null, null, null, false, false, null, null);
    }

    /**
     * Creates a SUCCESS state with fallback information.
     * Used when parse_status is PARTIAL, FAILED, or EMPTY.
     *
     * @param result The analysis result (may be minimal/incomplete)
     * @param fallbackMessage Message explaining what's unavailable
     * @param showReanalyze Whether to show re-analyze button
     * @param reanalyzedDate Formatted re-analyzed date, null if never re-analyzed
     */
    public static AnalysisUiState successWithFallback(PlantAnalysisResult result,
                                                       String fallbackMessage,
                                                       boolean showReanalyze,
                                                       String reanalyzedDate) {
        return new AnalysisUiState(State.SUCCESS, result, null, null, null, fallbackMessage, showReanalyze, false, null, reanalyzedDate);
    }

    /**
     * Creates a SUCCESS state with additional metadata (quality override, quick diagnosis).
     * Used by AnalysisStateMapper for parse_status OK with extra context.
     *
     * @param result The analysis result to display
     * @param qualityOverridden Whether quality check was overridden
     * @param quickDiagnosisDisclaimer Disclaimer text for Quick mode, null for full analysis
     * @param reanalyzedDate Formatted re-analyzed date, null if never re-analyzed
     */
    public static AnalysisUiState successWithMetadata(PlantAnalysisResult result,
                                                       boolean qualityOverridden,
                                                       String quickDiagnosisDisclaimer,
                                                       String reanalyzedDate) {
        return new AnalysisUiState(State.SUCCESS, result, null, null, null, null, false, qualityOverridden, quickDiagnosisDisclaimer, reanalyzedDate);
    }

    /**
     * Creates an ERROR state with error message.
     *
     * @param message Human-readable error message
     */
    public static AnalysisUiState error(String message) {
        return new AnalysisUiState(State.ERROR, null, message, null, null, null, false, false, null, null);
    }

    /**
     * Creates an ERROR state for vision-not-supported scenario.
     * Provides specific error message guiding user to switch providers.
     *
     * @param providerName Name of the text-only provider
     */
    public static AnalysisUiState visionNotSupported(String providerName) {
        String message = providerName + " is text-only. Switch to Claude, ChatGPT, or Gemini in Settings for image analysis.";
        return new AnalysisUiState(State.ERROR, null, message, providerName, null, null, false, false, null, null);
    }

    // ==================== Getters ====================

    /**
     * Gets the current state.
     */
    public State getState() {
        return state;
    }

    /**
     * Gets the analysis result (only non-null when state is SUCCESS).
     */
    public PlantAnalysisResult getResult() {
        return result;
    }

    /**
     * Gets the error message (only non-null when state is ERROR).
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the vision-unsupported provider name (only non-null for vision errors).
     */
    public String getVisionUnsupportedProvider() {
        return visionUnsupportedProvider;
    }

    // ==================== Convenience Methods ====================

    /**
     * Checks if analysis is currently loading.
     */
    public boolean isLoading() {
        return state == State.LOADING;
    }

    /**
     * Checks if analysis completed successfully.
     */
    public boolean isSuccess() {
        return state == State.SUCCESS;
    }

    /**
     * Checks if analysis failed with error.
     */
    public boolean hasError() {
        return state == State.ERROR;
    }

    /**
     * Checks if error is specifically due to vision not being supported.
     */
    public boolean isVisionUnsupported() {
        return visionUnsupportedProvider != null;
    }

    /**
     * Gets the loading progress message (only non-null when state is LOADING with custom message).
     * Returns null for default loading state (show standard progress indicator).
     */
    public String getLoadingMessage() {
        return loadingMessage;
    }

    /**
     * Gets the fallback message explaining what couldn't be loaded.
     * Only non-null when parse_status is PARTIAL, FAILED, or EMPTY.
     */
    public String getFallbackMessage() {
        return fallbackMessage;
    }

    /**
     * Checks if re-analyze button should be shown.
     * True when parse failed and photo exists.
     */
    public boolean shouldShowReanalyzeButton() {
        return showReanalyzeButton;
    }

    /**
     * Checks if quality check was overridden by user.
     */
    public boolean isQualityOverridden() {
        return qualityOverridden;
    }

    /**
     * Gets the Quick Diagnosis disclaimer text.
     * Only non-null for Quick mode analyses.
     */
    public String getQuickDiagnosisDisclaimer() {
        return quickDiagnosisDisclaimer;
    }

    /**
     * Gets the formatted re-analyzed date.
     * Null if this analysis was never re-analyzed.
     */
    public String getReanalyzedDate() {
        return reanalyzedDate;
    }
}
